package client

import (
	"testing"
	"time"
)

func TestWithTimeout(t *testing.T) {
	tests := []struct {
		name    string
		timeout time.Duration
		wantErr bool
	}{
		{
			name:    "valid timeout",
			timeout: 10 * time.Second,
			wantErr: false,
		},
		{
			name:    "zero timeout",
			timeout: 0,
			wantErr: true,
		},
		{
			name:    "negative timeout",
			timeout: -1 * time.Second,
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &Client{timeout: defaultTimeout}
			err := WithTimeout(tt.timeout)(c)
			if (err != nil) != tt.wantErr {
				t.Errorf("WithTimeout(%v) error = %v, wantErr %v", tt.timeout, err, tt.wantErr)
				return
			}
			if !tt.wantErr && c.timeout != tt.timeout {
				t.Errorf("timeout = %v, want %v", c.timeout, tt.timeout)
			}
		})
	}
}

func TestDefaultTimeout(t *testing.T) {
	c := &Client{}
	if c.timeout != 0 {
		t.Errorf("zero-value Client timeout = %v, want 0", c.timeout)
	}
}

func TestNewFromData(t *testing.T) {
	tests := []struct {
		name    string
		data    []byte
		wantErr bool
	}{
		{
			name:    "invalid yaml",
			data:    []byte("not valid kubeconfig"),
			wantErr: true,
		},
		{
			name:    "empty data",
			data:    []byte{},
			wantErr: true,
		},
		{
			name:    "nil data",
			data:    nil,
			wantErr: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := NewFromData(tt.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("NewFromData() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
