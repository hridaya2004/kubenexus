# Makefile for KubeNexus

ANDROID_DIR := android
CORE_DIR := core
GRADLEW := ./gradlew

.DEFAULT_GOAL := help

.PHONY: help aar debug release build bundle lint fmt test clean install-debug go-clean go-test go-lint go-fmt

help: ## Display this help message
	@echo "KubeNexus Build & Development Commands"
	@echo ""
	@echo "Usage:"
	@printf "  make \033[36m<target>\033[0m\n"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

aar: ## Build kubenexus.aar from core Go source and copy to android libs
	cd $(CORE_DIR) && $(MAKE) build-android
	cp $(CORE_DIR)/kubenexus.aar $(ANDROID_DIR)/app/libs/kubenexus.aar
	@echo "Updated $(ANDROID_DIR)/app/libs/kubenexus.aar"

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

fmt: go-fmt ## Apply safe formatting and lint quickfixes across Go and Android
	cd $(ANDROID_DIR) && $(GRADLEW) lintFix

test: ## Run unit tests
	cd $(ANDROID_DIR) && $(GRADLEW) test

clean: go-clean ## Clean build cache and generated artifacts
	cd $(ANDROID_DIR) && $(GRADLEW) clean

install-debug: ## Install debug APK on connected Android device/emulator
	cd $(ANDROID_DIR) && $(GRADLEW) installDebug
