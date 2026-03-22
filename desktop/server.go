package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true // allow all origins — LAN only
	},
}

type TransferHeader struct {
	Type     string `json:"type"`
	Name     string `json:"name"`
	Size     int64  `json:"size"`
	MIME     string `json:"mime"`
	Content  string `json:"content"` // for text/links
	Checksum string `json:"checksum"`
}

func startServer() {
	http.HandleFunc("/transfer", handleTransfer)
	http.HandleFunc("/ping", handlePing) // for Android to verify connection

	addr := fmt.Sprintf("0.0.0.0:%d", 7297)
	log.Printf("Entangle listening on %s\n", addr)

	if err := http.ListenAndServe(addr, nil); err != nil {
		log.Printf("Server error: %v\n", err)
	}
}

func handlePing(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte(`{"status":"entangle","version":"1.0"}`))
}

func handleTransfer(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		return
	}
	defer conn.Close()

	// First message is always the JSON header
	_, headerBytes, err := conn.ReadMessage()
	if err != nil {
		return
	}

	var header TransferHeader
	if err := json.Unmarshal(headerBytes, &header); err != nil {
		return
	}

	switch header.Type {
	case "file":
		receiveFile(conn, header)
	case "text", "link":
		receiveText(conn, header)
	}
}

