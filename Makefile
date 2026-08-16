.PHONY: build-android clean test lint

ANDROID_HOME ?= $(HOME)/Android/Sdk
ANDROID_NDK_HOME ?= $(ANDROID_HOME)/ndk/27.0.12077973
ANDROID_API ?= 25

build-android:
	gomobile bind -androidapi $(ANDROID_API) -target android/arm,android/arm64 -o kubenexus.aar ./pkg/client

clean:
	rm -f kubenexus.aar kubenexus-sources.jar
	gomobile clean

test:
	go test ./...

lint:
	golangci-lint run ./...
