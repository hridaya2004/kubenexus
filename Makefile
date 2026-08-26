ANDROID_DIR := android
CORE_DIR := core
TERMINAL_DIR := terminal-native
GRADLEW := ./gradlew

ANDROID_NDK_HOME ?= $(HOME)/Android/Sdk/ndk/27.0.12077973
export ANDROID_NDK_HOME
export ANDROID_NDK_ROOT ?= $(ANDROID_NDK_HOME)
export ANDROID_HOME ?= $(HOME)/Android/Sdk
export ANDROID_SDK_ROOT ?= $(ANDROID_HOME)

# Module Commit SHAs
export KUBENEXUS_APP_COMMIT_SHA ?= $(shell git rev-parse --short HEAD 2>/dev/null || echo unknown)
export KUBENEXUS_LIBGHOSTTY_COMMIT_SHA ?= $(shell grep -oP 'ghostty#[a-f0-9]+' $(TERMINAL_DIR)/build.zig.zon 2>/dev/null | cut -d'#' -f2 | cut -c1-7 || echo a746d0f)
export KUBENEXUS_GHOSTTY_BRIDGE_COMMIT_SHA ?= $(shell git log -n 1 --format=%h -- $(TERMINAL_DIR) 2>/dev/null || echo unknown)
export KUBENEXUS_GO_CORE_COMMIT_SHA ?= $(shell git log -n 1 --format=%h -- $(CORE_DIR) 2>/dev/null || echo unknown)
export KUBENEXUS_CLIENT_GO_COMMIT_SHA ?= 44a8af2

AAR_TARGET := $(ANDROID_DIR)/app/libs/kubenexus.aar
GHOSTTY_SO_TARGET := $(ANDROID_DIR)/app/src/main/jniLibs/arm64-v8a/libghostty_jni.so

.DEFAULT_GOAL := help

.PHONY: help jni go-core ghostty debug release build bundle lint fmt test clean clean-jni install install-debug install-release go-clean go-test go-lint go-fmt ghostty-fmt generate-kube-openapi-spec

help: ## Display this help message
	@echo "KubeNexus Build & Development Commands"
	@echo ""
	@echo "Usage:"
	@printf "  make \033[36m<target>\033[0m\n"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

# Rebuild whenever any Go source changes - without these prerequisites Make
# happily serves a stale AAR after core edits (bit us 2026-08-26).
GO_CORE_SOURCES := $(shell find $(CORE_DIR) -name '*.go' -not -name '*_test.go' 2>/dev/null)

$(AAR_TARGET): $(GO_CORE_SOURCES)
	@echo "kubenexus.aar missing or Go core changed. Building Go core native bridge..."
	$(MAKE) go-core

$(GHOSTTY_SO_TARGET):
	@echo "libghostty_jni.so not found. Building Ghostty JNI library with Zig..."
	$(MAKE) ghostty

jni: $(AAR_TARGET) $(GHOSTTY_SO_TARGET) ## Ensure native JNI libraries (Go core and Ghostty) are built

go-core: ## Build kubenexus.aar from core Go source and copy to android libs
	$(MAKE) -C $(CORE_DIR) build-android
	mkdir -p $(ANDROID_DIR)/app/libs
	cp $(CORE_DIR)/kubenexus.aar $(ANDROID_DIR)/app/libs/kubenexus.aar
	@# gomobile also emits a sources jar. Keeping it next to the AAR lets Android
	@# Studio show Go doc comments and real parameter names instead of p0, p1.
	@# The Gradle fileTree excludes *-sources.jar from the compile classpath.
	@if [ -f $(CORE_DIR)/kubenexus-sources.jar ]; then \
		cp $(CORE_DIR)/kubenexus-sources.jar $(ANDROID_DIR)/app/libs/kubenexus-sources.jar; \
		echo "Updated $(ANDROID_DIR)/app/libs/kubenexus-sources.jar"; \
	fi
	@echo "Updated $(ANDROID_DIR)/app/libs/kubenexus.aar"

ghostty: ## Cross-compile libghostty_jni.so for Android (arm64-v8a) using Zig
	cd $(TERMINAL_DIR) && zig build -Doptimize=ReleaseSmall jni
	@echo "Built libghostty_jni.so in $(ANDROID_DIR)/app/src/main/jniLibs"

ghostty-fmt: ## Format terminal native Zig source code
	cd $(TERMINAL_DIR) && zig fmt build.zig src/

go-clean: ## Clean core Go build artifacts and gomobile cache
	$(MAKE) -C $(CORE_DIR) clean

go-test: ## Run core Go unit tests
	$(MAKE) -C $(CORE_DIR) test

generate-kube-openapi-spec: ## Re-record live cluster payloads into core/pkg/client/testdata (kubectl + jq required)
	$(MAKE) -C $(CORE_DIR) generate-kube-openapi-spec

go-lint: ## Run golangci-lint on core Go source
	$(MAKE) -C $(CORE_DIR) lint

go-fmt: ## Format core Go source code
	$(MAKE) -C $(CORE_DIR) fmt

debug: jni ## Build debug APK
	cd $(ANDROID_DIR) && $(GRADLEW) assembleDebug

release: jni ## Build release APK
	cd $(ANDROID_DIR) && $(GRADLEW) assembleRelease

build: jni ## Build both debug and release APKs
	cd $(ANDROID_DIR) && $(GRADLEW) assembleDebug assembleRelease

bundle: jni ## Build release Android App Bundle (AAB)
	cd $(ANDROID_DIR) && $(GRADLEW) bundleRelease

lint: ## Run Android Lint checks
	cd $(ANDROID_DIR) && $(GRADLEW) lint

fmt: go-fmt ghostty-fmt ## Apply formatting across Go, Zig, and Android
	cd $(ANDROID_DIR) && $(GRADLEW) lintFix

test: jni ## Run unit tests
	cd $(ANDROID_DIR) && $(GRADLEW) test

install: install-debug ## Install debug APK on connected device (alias)

install-debug: jni ## Install debug APK on connected Android device/emulator
	cd $(ANDROID_DIR) && $(GRADLEW) installDebug

install-release: jni ## Install release APK on connected Android device/emulator
	cd $(ANDROID_DIR) && $(GRADLEW) installRelease

clean-jni: ## Remove compiled native JNI libraries and Zig artifacts
	rm -rf $(ANDROID_DIR)/app/src/main/jniLibs
	rm -f $(ANDROID_DIR)/app/libs/kubenexus.aar $(ANDROID_DIR)/app/libs/kubenexus-sources.jar
	rm -rf $(TERMINAL_DIR)/.zig-cache $(TERMINAL_DIR)/zig-out

clean: go-clean clean-jni ## Clean build cache, generated artifacts, and JNI libraries
	cd $(ANDROID_DIR) && $(GRADLEW) clean
