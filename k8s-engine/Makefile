.PHONY: build-android clean test lint fmt generate-kube-openapi-spec

ANDROID_API ?= 35
LIB_NAME ?= kubenexus_client

# All four Android ABIs so the release AAB can be split by Play per device.
ANDROID_TARGETS ?= android/arm64,android/arm,android/amd64,android/386

export CGO_LDFLAGS := -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384

# No -s -w: AGP strips the shipped copy and keeps the symbols for Play.
build-android:
	gomobile bind -androidapi $(ANDROID_API) -trimpath -ldflags="-extldflags '-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384'" -target $(ANDROID_TARGETS) -o kubenexus.aar ./pkg/client
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

generate-kube-openapi-spec:
	./scripts/record-testdata.sh
