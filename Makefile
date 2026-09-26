# ------------------------------------------------------------------------------
# Configuration & Paths
# ------------------------------------------------------------------------------

ANDROID_DIR  := android
CORE_DIR     := k8s-engine
TERMINAL_DIR := terminal-native
GRADLE       := ./$(ANDROID_DIR)/gradlew -p $(ANDROID_DIR)

ANDROID_NDK_HOME ?= $(HOME)/Android/Sdk/ndk/30.0.16248370
export ANDROID_NDK_HOME
export ANDROID_NDK_ROOT ?= $(ANDROID_NDK_HOME)
export ANDROID_HOME     ?= $(HOME)/Android/Sdk
export ANDROID_SDK_ROOT ?= $(ANDROID_HOME)

AAR_TARGET        := $(ANDROID_DIR)/data/libs/kubenexus.aar
GHOSTTY_ABIS      := arm64-v8a armeabi-v7a x86_64 x86
GHOSTTY_SO_TARGET := $(foreach abi,$(GHOSTTY_ABIS),$(ANDROID_DIR)/app/src/main/jniLibs/$(abi)/libghostty_jni.so)

GO_CORE_SOURCES   := $(shell find $(CORE_DIR) -type f \( -name '*.go' -o -name 'go.mod' -o -name 'go.sum' -o -name '*.sh' \) -not -name '*_test.go' 2>/dev/null)
GHOSTTY_SOURCES   := $(shell find $(TERMINAL_DIR)/src -type f 2>/dev/null) $(TERMINAL_DIR)/build.zig $(TERMINAL_DIR)/build.zig.zon

LICENSES_DIR      ?= legal/compliance
# Gradle resolves a relative exportPath against its own project dir (android/), not the repo root,
# so anchor it here. An absolute LICENSES_DIR is used as given.
LICENSES_OUT       := $(if $(filter /%,$(LICENSES_DIR)),$(LICENSES_DIR),$(CURDIR)/$(LICENSES_DIR))
# The plugin names its output export.txt/export.csv, which says nothing about what it is if the
# file travels on its own. Rename to something self-describing.
LICENSES_REPORT    := $(LICENSES_OUT)/kubenexus-licenses.txt

# The NDK version android/app/build.gradle.kts asks for, read from there so the two can only
# disagree if that file itself is wrong.
GRADLE_NDK_VERSION  := $(shell sed -n 's/^ *ndkVersion = "\(.*\)"$$/\1/p' $(ANDROID_DIR)/app/build.gradle.kts)

.DEFAULT_GOAL := help
.PHONY: help jni k8s-engine ghostty debug release build bundle bundle-debug lint fmt test \
        clean clean-jni install install-debug install-release \
        k8s-clean k8s-test k8s-lint k8s-fmt ghostty-fmt generate-kube-openapi-spec \
        licenses licenses-clean \
        verify-jni verify-ndk

# ------------------------------------------------------------------------------
# Help
# ------------------------------------------------------------------------------

help: ## Display this help message
	@echo "KubeNexus Build & Development Commands"
	@echo ""
	@echo "Usage:"
	@printf "  make \033[36m<target>\033[0m\n"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-26s\033[0m %s\n", $$1, $$2}'

# ------------------------------------------------------------------------------
# Native Bridges & JNI
# ------------------------------------------------------------------------------

$(AAR_TARGET): $(GO_CORE_SOURCES)
	@echo "kubenexus.aar missing or k8s-engine changed. Rebuilding k8s-engine native bridge..."
	$(MAKE) k8s-engine

# Grouped target (&:), so a single zig build satisfies all four ABIs while any one of them going
# missing or stale re-triggers the build. A plain ':' rule over a list would either run zig once
# per ABI or, worse, treat the whole set as satisfied from arm64-v8a alone.
$(GHOSTTY_SO_TARGET) &: $(GHOSTTY_SOURCES)
	@echo "libghostty_jni.so missing or stale for some ABI. Rebuilding Ghostty JNI library with Zig..."
	$(MAKE) ghostty

jni: verify-ndk $(AAR_TARGET) $(GHOSTTY_SO_TARGET) ## Ensure native JNI libraries (k8s-engine and Ghostty) are built

verify-jni: ## Fail unless every ABI has its native library in jniLibs and the AAR, all 16 KB aligned
	@missing=""; \
	for abi in $(GHOSTTY_ABIS); do \
	  [ -f "$(ANDROID_DIR)/app/src/main/jniLibs/$$abi/libghostty_jni.so" ] || missing="$$missing jniLibs/$$abi"; \
	done; \
	for abi in $(GHOSTTY_ABIS); do \
	  unzip -l "$(AAR_TARGET)" 2>/dev/null | grep -q "jni/$$abi/libkubenexus_client.so" || missing="$$missing aar/$$abi"; \
	done; \
	if [ -n "$$missing" ]; then echo "ERROR: missing native libs for:$$missing"; exit 1; fi; \
	echo "OK: native libs present for $(GHOSTTY_ABIS) in jniLibs and $(notdir $(AAR_TARGET))"
	@# Google Play rejects apps targeting Android 15+ whose native libs have LOAD segments
	@# aligned below 16 KB. Check every shipped .so, including the ones inside the AAR.
	@readelf="$(firstword $(wildcard $(ANDROID_NDK_ROOT)/toolchains/llvm/prebuilt/*/bin/llvm-readelf))"; \
	if [ -z "$$readelf" ]; then echo "ERROR: llvm-readelf not found under $(ANDROID_NDK_ROOT)"; exit 1; fi; \
	tmp=$$(mktemp -d); trap 'rm -rf "$$tmp"' EXIT; \
	unzip -q -o "$(AAR_TARGET)" 'jni/*' -d "$$tmp"; \
	misaligned=""; \
	for so in $(ANDROID_DIR)/app/src/main/jniLibs/*/*.so "$$tmp"/jni/*/*.so; do \
	  for align in $$("$$readelf" -lW "$$so" | awk '$$1 == "LOAD" { print $$NF }'); do \
	    if [ "$$(printf '%d' "$$align")" -lt 16384 ]; then misaligned="$$misaligned $${so#$$tmp/}($$align)"; break; fi; \
	  done; \
	done; \
	if [ -n "$$misaligned" ]; then echo "ERROR: LOAD segments aligned below 16 KB:$$misaligned"; exit 1; fi; \
	echo "OK: every native lib has 16 KB-aligned LOAD segments"

verify-ndk: ## Fail unless the NDK AGP was told to use is the one installed here
	@if [ -z "$(GRADLE_NDK_VERSION)" ]; then \
		echo "ERROR: could not read ndkVersion from $(ANDROID_DIR)/app/build.gradle.kts"; exit 1; \
	fi
	@if [ ! -d "$(ANDROID_NDK_ROOT)" ]; then \
		echo "ERROR: NDK not installed at $(ANDROID_NDK_ROOT)"; exit 1; \
	fi
	@if [ "$(notdir $(ANDROID_NDK_ROOT))" != "$(GRADLE_NDK_VERSION)" ]; then \
		echo "ERROR: $(ANDROID_DIR)/app/build.gradle.kts asks for NDK $(GRADLE_NDK_VERSION) but ANDROID_NDK_ROOT is $(ANDROID_NDK_ROOT)"; \
		echo "       Without a matching NDK, AGP cannot strip native libs, so it ships them as-is and emits no debug symbols."; \
		exit 1; \
	fi
	@echo "OK: NDK $(GRADLE_NDK_VERSION) matches $(ANDROID_DIR)/app/build.gradle.kts"

k8s-engine: ## Build kubenexus.aar from k8s-engine Go source and copy to android libs
	$(MAKE) -C $(CORE_DIR) build-android
	mkdir -p $(ANDROID_DIR)/data/libs
	cp -f $(CORE_DIR)/kubenexus.aar $(ANDROID_DIR)/data/libs/
	@if [ -f $(CORE_DIR)/kubenexus-sources.jar ]; then \
		cp -f $(CORE_DIR)/kubenexus-sources.jar $(ANDROID_DIR)/data/libs/; \
		echo "Updated $(ANDROID_DIR)/data/libs/kubenexus-sources.jar"; \
	fi
	@touch -c $(AAR_TARGET)
	@echo "Updated $(ANDROID_DIR)/data/libs/kubenexus.aar"

ghostty: ## Cross-compile libghostty_jni.so for all Android ABIs using Zig
	cd $(TERMINAL_DIR) && zig build -Doptimize=ReleaseSmall jni
	@touch -c $(GHOSTTY_SO_TARGET)
	@echo "Built libghostty_jni.so for $(GHOSTTY_ABIS) in $(ANDROID_DIR)/app/src/main/jniLibs"

ghostty-fmt: ## Format terminal native Zig source code
	cd $(TERMINAL_DIR) && zig fmt build.zig src/

# ------------------------------------------------------------------------------
# Kubernetes Engine (Delegated to k8s-engine)
# ------------------------------------------------------------------------------

k8s-test: ## Run k8s-engine unit tests
	$(MAKE) -C $(CORE_DIR) test

k8s-lint: ## Run golangci-lint on k8s-engine source
	$(MAKE) -C $(CORE_DIR) lint

k8s-fmt: ## Format k8s-engine source code
	$(MAKE) -C $(CORE_DIR) fmt

k8s-clean: ## Clean k8s-engine build artifacts and gomobile cache
	$(MAKE) -C $(CORE_DIR) clean

generate-kube-openapi-spec: ## Re-record live cluster payloads into k8s-engine (kubectl + jq required)
	$(MAKE) -C $(CORE_DIR) generate-kube-openapi-spec

# ------------------------------------------------------------------------------
# Android Build & Install
# ------------------------------------------------------------------------------

debug: jni ## Build debug APK
	$(GRADLE) assembleDebug

release: jni ## Build release APK (all ABIs, universal)
	$(GRADLE) assembleRelease

bundle: jni ## Build signed release Android App Bundle (AAB) for Play
	$(GRADLE) bundleRelease

bundle-debug: jni ## Build debug Android App Bundle (AAB)
	$(GRADLE) bundleDebug

build: bundle ## Build the signed release AAB (default CI build)

install: install-debug ## Install debug APK on connected device (alias)

install-debug: jni ## Install debug APK on connected Android device/emulator
	$(GRADLE) installDebug

install-release: jni ## Install release APK on connected Android device/emulator
	$(GRADLE) installRelease

# ------------------------------------------------------------------------------
# Code Quality, Formatting & Testing
# ------------------------------------------------------------------------------

test: jni ## Run Android unit tests
	$(GRADLE) test

lint: ## Run Android Lint checks
	$(GRADLE) lint

fmt: k8s-fmt ghostty-fmt ## Apply formatting across Go, Zig, and Android
	$(GRADLE) lintFix

# ------------------------------------------------------------------------------
# Licences
# ------------------------------------------------------------------------------

licenses: ## Generate the licence compliance report (override with LICENSES_DIR=...)
	@echo "Generating licence report into $(LICENSES_DIR)/ ..."
	$(GRADLE) :app:exportComplianceLibrariesRelease -PaboutLibraries.exportPath=$(LICENSES_OUT)
	@test -f $(LICENSES_OUT)/export.txt || { echo "ERROR: nothing generated in $(LICENSES_OUT)"; exit 1; }
	@mv $(LICENSES_OUT)/export.txt $(LICENSES_REPORT)
	@mv $(LICENSES_OUT)/export.csv $(LICENSES_OUT)/kubenexus-licenses.csv
	@echo ""
	@echo "Libraries: $$(sed -n '/^LIBRARIES:/,/^LICENSES:/p' $(LICENSES_REPORT) | grep -c ';')"
	@echo ""
	@echo "Licences in use:"
	@sed -n '/^LIBRARIES:/,/^LICENSES:/p' $(LICENSES_REPORT) | grep ';' \
		| cut -d';' -f3 | tr ',' '\n' | sed 's/^ *//' | sort -u | sed 's/^/  /'
	@echo ""
	@echo "Native components (not visible to Gradle, declared in android/config):"
	@sed -n '/^LIBRARIES:/,/^LICENSES:/p' $(LICENSES_REPORT) \
		| grep 'dev.hridaya.kubenexus' | cut -d';' -f1,3 | sed 's/^/  /'
	@for section in "ARTIFACTS WITHOUT LICENSE" "UNKNOWN LICENSES"; do \
		entries=$$(awk -v want="$$section:" '$$0 == want {body=1; next} \
			body && /^[A-Z][A-Z /-]*:$$/ {exit} body {print}' $(LICENSES_REPORT) \
			| sed '/^$$/d'); \
		if [ -z "$$entries" ]; then \
			echo ""; echo "$$section: none"; \
		else \
			echo ""; echo "$$section — review these:"; echo "$$entries" | sed 's/^/  /'; \
		fi; \
	done
	@echo ""
	@echo "Report: $(LICENSES_REPORT)  (also kubenexus-licenses.csv, and per-dependency copies under dependencies/)"

licenses-clean: ## Remove the generated licence report
	rm -rf $(LICENSES_DIR)

# ------------------------------------------------------------------------------
# Cleanup
# ------------------------------------------------------------------------------

clean-jni: ## Remove compiled native JNI libraries and Zig artifacts
	rm -rf $(ANDROID_DIR)/app/src/main/jniLibs
	rm -f $(ANDROID_DIR)/data/libs/kubenexus.aar $(ANDROID_DIR)/data/libs/kubenexus-sources.jar
	rm -rf $(TERMINAL_DIR)/.zig-cache $(TERMINAL_DIR)/zig-out

clean: k8s-clean clean-jni ## Clean build cache, generated artifacts, and JNI libraries
	$(GRADLE) clean
