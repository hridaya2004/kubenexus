const std = @import("std");
const builtin = @import("builtin");
const ndk = @import("src/ndk.zig");

const default_build_targets: []const std.Target.Query = &.{
    .{ .cpu_arch = .aarch64, .os_tag = .linux, .abi = .android, .android_api_level = 24 },
};

fn resolveBuildTargets(b: *std.Build) []const std.Target.Query {
    const maybe_target = b.option(
        []const u8,
        "target",
        "Android target triple (default: all supported ABIs)",
    ) orelse return default_build_targets;

    var query = std.Target.Query.parse(.{ .arch_os_abi = maybe_target }) catch |err| {
        std.debug.panic("invalid -Dtarget '{s}': {s}", .{ maybe_target, @errorName(err) });
    };
    if (query.android_api_level == null) query.android_api_level = 24;

    const targets = b.allocator.alloc(std.Target.Query, 1) catch @panic("OOM");
    targets[0] = query;
    return targets;
}

fn ndkPrebuiltTag() []const u8 {
    const os_part = switch (builtin.os.tag) {
        .macos => "darwin",
        .linux => "linux",
        .windows => "windows",
        else => @panic("Unsupported host OS for Android NDK prebuilt toolchain"),
    };

    const arch_part = switch (builtin.cpu.arch) {
        .x86_64 => "x86_64",
        .aarch64 => if (builtin.os.tag == .macos) "x86_64" else "aarch64",
        else => @panic("Unsupported host architecture for Android NDK prebuilt toolchain"),
    };

    return std.fmt.comptimePrint("{s}-{s}", .{ os_part, arch_part });
}

fn resolveNdkHome(b: *std.Build, ndk_root: []const u8) []const u8 {
    if (ndk_root.len == 0) return ndk_root;

    const toolchains_path = b.pathJoin(&.{ ndk_root, "toolchains", "llvm" });
    std.fs.cwd().access(toolchains_path, .{}) catch {
        var dir = std.fs.cwd().openDir(ndk_root, .{ .iterate = true }) catch return ndk_root;
        defer dir.close();

        var iter = dir.iterate();
        while (iter.next() catch null) |entry| {
            if (entry.kind != .directory) continue;
            return b.pathJoin(&.{ ndk_root, entry.name });
        }
        return ndk_root;
    };

    return ndk_root;
}

fn buildNativeLibrary(
    b: *std.Build,
    target: std.Build.ResolvedTarget,
    optimize: std.builtin.OptimizeMode,
) *std.Build.Step.Compile {
    const ghostty_dep = b.dependency("ghostty", .{
        .target = target,
        .optimize = optimize,
        .simd = false,
    });

    const ndk_root = b.graph.env_map.get("ANDROID_NDK_HOME") orelse b.graph.env_map.get("ANDROID_NDK_ROOT") orelse "/home/hridaya/Android/Sdk/ndk/27.0.12077973";
    const ndk_home = resolveNdkHome(b, ndk_root);

    const android_target = ndk.getAndroidTriple(target.result) catch {
        std.debug.panic("target must be Android", .{});
    };
    std.debug.assert(target.result.os.tag == .linux);
    const android_api_version: u32 = target.result.os.version_range.linux.android;

    const ndk_sysroot = b.pathJoin(&.{
        ndk_home,
        "toolchains",
        "llvm",
        "prebuilt",
        ndkPrebuiltTag(),
        "sysroot",
    });

    const libc_config = ndk.createLibC(
        b,
        android_target,
        android_api_version,
        ndk_sysroot,
    );

    const include_dir = b.pathJoin(&.{ ndk_sysroot, "usr", "include" });
    const target_include_dir = b.pathJoin(&.{ include_dir, android_target });

    const root_module = b.createModule(.{
        .root_source_file = b.path("src/ghostty_bridge.zig"),
        .target = target,
        .optimize = optimize,
        .link_libc = true,
        .strip = true,
        .unwind_tables = .none,
        .omit_frame_pointer = true,
    });
    root_module.addIncludePath(b.path("src"));
    root_module.addImport("ghostty-vt", ghostty_dep.module("ghostty-vt"));

    const lib = b.addLibrary(.{
        .linkage = .dynamic,
        .name = "ghostty_jni",
        .root_module = root_module,
    });
    lib.link_function_sections = true;
    lib.link_data_sections = true;
    lib.link_gc_sections = true;
    lib.link_eh_frame_hdr = false;
    lib.lto = .thin;
    root_module.strip = true;
    root_module.unwind_tables = .none;
    root_module.omit_frame_pointer = true;

    lib.addIncludePath(.{ .cwd_relative = include_dir });
    lib.addIncludePath(.{ .cwd_relative = target_include_dir });

    const api_dir = b.fmt("{d}", .{android_api_version});
    const lib_dir = b.pathJoin(&.{ ndk_sysroot, "usr", "lib", android_target, api_dir });
    lib.addLibraryPath(.{ .cwd_relative = lib_dir });

    lib.setLibCFile(libc_config);
    lib.linkSystemLibrary("log");
    lib.linkSystemLibrary("z");
    lib.linkLibC();
    lib.version_script = b.path("src/version-script.map");

    b.installArtifact(lib);
    return lib;
}

pub fn build(b: *std.Build) void {
    const optimize = b.standardOptimizeOption(.{});
    const build_targets = resolveBuildTargets(b);

    const native_step = b.step("native", "Build native JNI library");
    const jni_step = b.step("jni", "Build native library and copy to jniLibs");

    for (build_targets) |target_query| {
        const resolved_target = b.resolveTargetQuery(target_query);
        const native_lib = buildNativeLibrary(b, resolved_target, optimize);
        native_step.dependOn(&native_lib.step);

        const abi_name = ndk.getOutputDir(resolved_target.result) catch unreachable;
        const jni_lib_dir = b.fmt("../android/app/src/main/jniLibs/{s}", .{abi_name});

        const mkdir_jni_libs = b.addSystemCommand(&.{ "mkdir", "-p", jni_lib_dir });
        mkdir_jni_libs.step.dependOn(&native_lib.step);

        const copy_to_jni_libs = b.addSystemCommand(&.{"cp"});
        copy_to_jni_libs.step.dependOn(&mkdir_jni_libs.step);
        copy_to_jni_libs.addFileArg(native_lib.getEmittedBin());
        _ = copy_to_jni_libs.addArg(b.fmt("{s}/libghostty_jni.so", .{jni_lib_dir}));

        jni_step.dependOn(&copy_to_jni_libs.step);
    }
}
