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

.DEFAULT_GOAL := help

.PHONY: help go-core ghostty debug release build bundle lint fmt test clean install-debug go-clean go-test go-lint go-fmt ghostty-fmt

help: ## Display this help message
	@echo "KubeNexus Build & Development Commands"
	@echo ""
	@echo "Usage:"
	@printf "  make \033[36m<target>\033[0m\n"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

go-core: ## Build kubenexus.aar from core Go source and copy to android libs
	cd $(CORE_DIR) && $(MAKE) build-android
	cp $(CORE_DIR)/kubenexus.aar $(ANDROID_DIR)/app/libs/kubenexus.aar
	@echo "Updated $(ANDROID_DIR)/app/libs/kubenexus.aar"

ghostty: ## Cross-compile libghostty_jni.so for Android (arm64-v8a) using Zig
	cd $(TERMINAL_DIR) && zig build -Doptimize=ReleaseSmall jni
	@echo "Built libghostty_jni.so in $(ANDROID_DIR)/app/src/main/jniLibs"

ghostty-fmt: ## Format terminal native Zig source code
	cd $(TERMINAL_DIR) && zig fmt build.zig src/

go-clean: ## Clean core Go build artifacts and gomobile cache
	cd $(CORE_DIR) && $(MAKE) clean

go-test: ## Run core Go unit tests
	cd $(CORE_DIR) && $(MAKE) test

go-lint: ## Run golangci-lint on core Go source
	cd $(CORE_DIR) && $(MAKE) lint

go-fmt: ## Format core Go source code
	cd $(CORE_DIR) && $(MAKE) fmt

debug: ## Build debug APK
	cd $(ANDROID_DIR) && $(GRADLEW) assembleDebug

release: ## Build release APK
	cd $(ANDROID_DIR) && $(GRADLEW) assembleRelease

build: ## Build both debug and release APKs
	cd $(ANDROID_DIR) && $(GRADLEW) assembleDebug assembleRelease

bundle: ## Build release Android App Bundle (AAB)
	cd $(ANDROID_DIR) && $(GRADLEW) bundleRelease

lint: ## Run Android Lint checks
	cd $(ANDROID_DIR) && $(GRADLEW) lint

fmt: go-fmt ghostty-fmt ## Apply formatting across Go, Zig, and Android
	cd $(ANDROID_DIR) && $(GRADLEW) lintFix

test: ## Run unit tests
	cd $(ANDROID_DIR) && $(GRADLEW) test

clean: go-clean ## Clean build cache and generated artifacts
	cd $(ANDROID_DIR) && $(GRADLEW) clean

install-debug: ## Install debug APK on connected Android device/emulator
	cd $(ANDROID_DIR) && $(GRADLEW) installDebug
