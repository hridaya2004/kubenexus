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
	var namespace, podName, container, execCmd string

	if home := homedir.HomeDir(); home != "" {
		flag.StringVar(&kubeconfig, "kubeconfig", filepath.Join(home, ".kube", "config"), "path to the kubeconfig file")
	} else {
		flag.StringVar(&kubeconfig, "kubeconfig", "", "absolute path to the kubeconfig file")
	}
	flag.BoolVar(&useProtobuf, "protobuf", false, "use protobuf wire format instead of JSON")
	flag.StringVar(&namespace, "namespace", "default", "kubernetes namespace")
	flag.StringVar(&podName, "pod", "", "pod name")
	flag.StringVar(&container, "container", "", "container name")
	flag.StringVar(&execCmd, "exec", "", "command to execute inside pod")
	flag.Parse()

	var opts []client.Option
	if useProtobuf {
		opts = append(opts, client.WithProtobuf())
	}

	c, err := client.New(kubeconfig, opts...)
	if err != nil {
		return fmt.Errorf("initializing client: %w", err)
	}

	if execCmd != "" && podName != "" {
		res, err := c.Exec(context.Background(), namespace, podName, container, []string{"/bin/sh", "-c", execCmd}, "")
		if err != nil {
			return fmt.Errorf("executing in pod %s: %w", podName, err)
		}
		fmt.Print(res.Stdout)
		if res.Stderr != "" {
			fmt.Print(res.Stderr)
		}
		return nil
	}

	pods, err := c.ListPods(context.Background(), "")
	if err != nil {
		return fmt.Errorf("listing pods: %w", err)
	}

	fmt.Printf("There are %d pods in the cluster\n", len(pods))
	return nil
}
