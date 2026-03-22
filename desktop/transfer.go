package main

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/gorilla/websocket"
	"golang.design/x/clipboard"
)

func receiveFile(conn *websocket.Conn, header TransferHeader) {
	downloadsDir := getDownloadsDir()
	os.MkdirAll(downloadsDir, 0755)

	filePath := filepath.Join(downloadsDir, header.Name)
	filePath = deduplicateFilename(filePath)

	file, err := os.Create(filePath)
	if err != nil {
		conn.WriteMessage(websocket.TextMessage,
			[]byte(`{"status":"error","message":"could not create file"}`))
		return
	}
	defer file.Close()

	// Send acknowledgment — ready to receive
	conn.WriteMessage(websocket.TextMessage, []byte(`{"status":"ready"}`))

	for {
		_, chunk, err := conn.ReadMessage()
		if err != nil {
			break
		}

		// Check for end signal
		if string(chunk) == `{"status":"done"}` {
			break
		}

		file.Write(chunk)
	}

	// Send completion acknowledgment
	conn.WriteMessage(websocket.TextMessage, []byte(`{"status":"complete"}`))

	showNotification("Entangle", fmt.Sprintf("Received: %s", header.Name))
}

func receiveText(conn *websocket.Conn, header TransferHeader) {
	clipboard.Init()
	clipboard.Write(clipboard.FmtText, []byte(header.Content))

	conn.WriteMessage(websocket.TextMessage, []byte(`{"status":"complete"}`))

	showNotification("Entangle", fmt.Sprintf("Text copied to clipboard: %s", truncate(header.Content, 50)))
}

func getDownloadsDir() string {
	home, _ := os.UserHomeDir()
	return filepath.Join(home, "Downloads", "Entangle")
}

func deduplicateFilename(path string) string {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return path
	}

	ext := filepath.Ext(path)
	base := strings.TrimSuffix(path, ext)

	for i := 1; ; i++ {
		newPath := fmt.Sprintf("%s (%d)%s", base, i, ext)
		if _, err := os.Stat(newPath); os.IsNotExist(err) {
			return newPath
		}
	}
}

func truncate(s string, n int) string {
	if len(s) > n {
		return s[:n-3] + "..."
	}
	return s
}
