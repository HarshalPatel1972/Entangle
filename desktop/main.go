package main

import (
	"fmt"

	"fyne.io/systray"
	"entangle/desktop/icon"
)

func main() {
	systray.Run(onReady, onExit)
}

func onReady() {
	systray.SetIcon(icon.Data)
	systray.SetTitle("Entangle")
	ip := getLocalIP()
	if ip == "" {
		ip = "Unknown IP"
	}

	systray.SetTooltip(fmt.Sprintf("Entangle — %s:7297", ip))

	mStatus := systray.AddMenuItem(fmt.Sprintf("● Ready — %s", ip), "")
	mStatus.Disable() // informational only

	systray.AddSeparator()

	mOpen := systray.AddMenuItem("Open Downloads", "")
	mQuit := systray.AddMenuItem("Quit Entangle", "")

	// Start WebSocket server in background
	go startServer()

	// Start mDNS announcement in background
	go announceOnLAN()

	// Handle menu clicks
	go func() {
		for {
			select {
			case <-mOpen.ClickedCh:
				openDownloadsFolder()
			case <-mQuit.ClickedCh:
				systray.Quit()
			}
		}
	}()
}

func onExit() {
	// cleanup
}
