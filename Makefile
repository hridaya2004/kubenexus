# ------------------------------------------------------------------------------
# Configuration & Paths
# ------------------------------------------------------------------------------

ANDROID_DIR  := android
CORE_DIR     := k8s-engine
TERMINAL_DIR := terminal-native
GRADLE       := ./$(ANDROID_DIR)/gradlew -p $(ANDROID_DIR)

ANDROID_NDK_HOME ?= $(HOME)/Android/Sdk/ndk/30.0.16138531
export ANDROID_NDK_HOME
export ANDROID_NDK_ROOT ?= $(ANDROID_NDK_HOME)
export ANDROID_HOME     ?= $(HOME)/Android/Sdk
export ANDROID_SDK_ROOT ?= $(ANDROID_HOME)

AAR_TARGET        := $(ANDROID_DIR)/data/libs/kubenexus.aar
GHOSTTY_ABIS      := arm64-v8a armeabi-v7a x86_64 x86
GHOSTTY_SO_TARGET := $(foreach abi,$(GHOSTTY_ABIS),$(ANDROID_DIR)/app/src/main/jniLibs/$(abi)/libghostty_jni.so)

GO_CORE_SOURCES   := $(shell find $(CORE_DIR) -type f \( -name '*.go' -o -name 'go.mod' -o -name 'go.sum' -o -name '*.sh' \) -not -name '*_test.go' 2>/dev/null)
GHOSTTY_SOURCES   := $(shell find $(TERMINAL_DIR)/src -type f 2>/dev/null) $(TERMINAL_DIR)/build.zig $(TERMINAL_DIR)/build.zig.zon

.DEFAULT_GOAL := help
.PHONY: help jni k8s-engine ghostty debug release build bundle bundle-debug lint fmt test \
        clean clean-jni install install-debug install-release \
        k8s-clean k8s-test k8s-lint k8s-fmt ghostty-fmt generate-kube-openapi-spec

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

jni: $(AAR_TARGET) $(GHOSTTY_SO_TARGET) ## Ensure native JNI libraries (k8s-engine and Ghostty) are built

verify-jni: ## Fail unless every ABI has its native library in jniLibs and in the AAR
	@missing=""; \
	for abi in $(GHOSTTY_ABIS); do \
	  [ -f "$(ANDROID_DIR)/app/src/main/jniLibs/$$abi/libghostty_jni.so" ] || missing="$$missing jniLibs/$$abi"; \
	done; \
	for abi in $(GHOSTTY_ABIS); do \
	  unzip -l "$(AAR_TARGET)" 2>/dev/null | grep -q "jni/$$abi/libkubenexus_client.so" || missing="$$missing aar/$$abi"; \
	done; \
	if [ -n "$$missing" ]; then echo "ERROR: missing native libs for:$$missing"; exit 1; fi; \
	echo "OK: native libs present for $(GHOSTTY_ABIS) in jniLibs and $(notdir $(AAR_TARGET))"

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
# Cleanup
# ------------------------------------------------------------------------------

clean-jni: ## Remove compiled native JNI libraries and Zig artifacts
	rm -rf $(ANDROID_DIR)/app/src/main/jniLibs
	rm -f $(ANDROID_DIR)/data/libs/kubenexus.aar $(ANDROID_DIR)/data/libs/kubenexus-sources.jar
	rm -rf $(TERMINAL_DIR)/.zig-cache $(TERMINAL_DIR)/zig-out

clean: k8s-clean clean-jni ## Clean build cache, generated artifacts, and JNI libraries
	$(GRADLE) clean
