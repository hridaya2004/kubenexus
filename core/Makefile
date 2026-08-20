.PHONY: build-android clean test lint fmt

ANDROID_API ?= 35
LIB_NAME ?= kubenexus_client

export CGO_LDFLAGS := -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384

build-android:
	gomobile bind -androidapi $(ANDROID_API) -trimpath -ldflags="-extldflags '-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384' -s -w" -target android/arm64 -o kubenexus.aar ./pkg/client
	./scripts/rename_jni.sh kubenexus.aar $(LIB_NAME)

clean:
	rm -f kubenexus.aar kubenexus-sources.jar
	gomobile clean

test:
	go test ./...

lint:
	golangci-lint run ./...

fmt:
	go fmt ./...
