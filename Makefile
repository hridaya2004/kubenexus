# Makefile for KubeNexus

ANDROID_DIR := android
CORE_DIR := core
GRADLEW := ./gradlew

.DEFAULT_GOAL := help

.PHONY: help build debug release build-debug build-release bundle bundle-release aar build-aar lint fmt format test clean install-debug

help: ## Display this help message
	@echo "KubeNexus Build & Development Commands"
	@echo ""
	@echo "Usage:"
	@echo "  make \033[36m<target>\033[0m"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

aar: ## Build kubenexus.aar from core Go source and copy to android libs
	cd $(CORE_DIR) && $(MAKE) build-android
	cp $(CORE_DIR)/kubenexus.aar $(ANDROID_DIR)/app/libs/kubenexus.aar
	@echo "Updated $(ANDROID_DIR)/app/libs/kubenexus.aar"

build-aar: aar ## Alias for aar

debug: ## Build debug APK
	cd $(ANDROID_DIR) && $(GRADLEW) assembleDebug

build-debug: debug ## Alias for debug build

release: ## Build release APK
	cd $(ANDROID_DIR) && $(GRADLEW) assembleRelease

build-release: release ## Alias for release build

build: ## Build both debug and release APKs
	cd $(ANDROID_DIR) && $(GRADLEW) assembleDebug assembleRelease

bundle: ## Build release Android App Bundle (AAB)
	cd $(ANDROID_DIR) && $(GRADLEW) bundleRelease

bundle-release: bundle ## Alias for bundle

lint: ## Run Android Lint checks
	cd $(ANDROID_DIR) && $(GRADLEW) lint

fmt: ## Apply Android Lint quickfixes and safe formatting
	cd $(ANDROID_DIR) && $(GRADLEW) lintFix

format: fmt ## Alias for fmt

test: ## Run unit tests
	cd $(ANDROID_DIR) && $(GRADLEW) test

clean: ## Clean build cache and generated artifacts
	cd $(ANDROID_DIR) && $(GRADLEW) clean

install-debug: ## Install debug APK on connected Android device/emulator
	cd $(ANDROID_DIR) && $(GRADLEW) installDebug
