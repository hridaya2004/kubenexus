package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"path/filepath"

	"github.com/hridaya2004/kubenexus-go-client/pkg/client"

	"k8s.io/client-go/util/homedir"
)

func main() {
	if err := run(); err != nil {
		log.Fatal(err)
	}
}

func run() error {
	var kubeconfig string
	var useProtobuf bool
	if home := homedir.HomeDir(); home != "" {
		flag.StringVar(&kubeconfig, "kubeconfig", filepath.Join(home, ".kube", "config"), "path to the kubeconfig file")
	} else {
		flag.StringVar(&kubeconfig, "kubeconfig", "", "absolute path to the kubeconfig file")
	}
	flag.BoolVar(&useProtobuf, "protobuf", false, "use protobuf wire format instead of JSON")
	flag.Parse()

	var opts []client.Option
	if useProtobuf {
		opts = append(opts, client.WithProtobuf())
	}

	c, err := client.New(kubeconfig, opts...)
	if err != nil {
		return fmt.Errorf("initializing client: %w", err)
	}

	pods, err := c.ListPods(context.Background(), "")
	if err != nil {
		return fmt.Errorf("listing pods: %w", err)
	}

	fmt.Printf("There are %d pods in the cluster\n", len(pods))
	return nil
}
