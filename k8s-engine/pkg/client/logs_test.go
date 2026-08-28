package client

import "testing"

type mockLogCallback struct {
	lines  []string
	errors []string
	done   bool
}

func (m *mockLogCallback) OnLogLine(line string) {
	m.lines = append(m.lines, line)
}

func (m *mockLogCallback) OnError(err string) {
	m.errors = append(m.errors, err)
}

func (m *mockLogCallback) OnDone() {
	m.done = true
}

func TestMockLogCallback(t *testing.T) {
	cb := &mockLogCallback{}
	cb.OnLogLine("line1")
	cb.OnLogLine("line2")
	cb.OnError("something went wrong")
	cb.OnDone()

	if len(cb.lines) != 2 || cb.lines[0] != "line1" || cb.lines[1] != "line2" {
		t.Errorf("lines = %v", cb.lines)
	}
	if len(cb.errors) != 1 || cb.errors[0] != "something went wrong" {
		t.Errorf("errors = %v", cb.errors)
	}
	if !cb.done {
		t.Error("done should be true")
	}
}

// A nil callback must be ignored rather than panic, since the Kotlin side can
// pass null across the JNI boundary.
func TestStreamLogs_NilCallbackIsIgnored(t *testing.T) {
	c := &Client{timeout: defaultTimeout}
	c.StreamLogsWithTail("default", "pod-1", "container-1", 10, nil)
}
