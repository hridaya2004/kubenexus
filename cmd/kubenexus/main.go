package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"path/filepath"

	"github.com/hridaya2004/kubenexus-go-client/internal/client"

	"k8s.io/client-go/util/homedir"
)

func main() {
	if err := run(); err != nil {
		log.Fatal(err)
	}
}

func run() error {
	var kubeconfig string
	if home := homedir.HomeDir(); home != "" {
		flag.StringVar(&kubeconfig, "kubeconfig", filepath.Join(home, ".kube", "config"), "path to the kubeconfig file")
	} else {
		flag.StringVar(&kubeconfig, "kubeconfig", "", "absolute path to the kubeconfig file")
	}
	flag.Parse()

	c, err := client.New(kubeconfig)
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
