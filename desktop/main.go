package main

import (
	"log"

	"fyne.io/systray"
	"entangle/desktop/icon"
)

func main() {
	systray.Run(onReady, onExit)
}

func onReady() {
	systray.SetIcon(icon.Data)
	systray.SetTitle("Entangle")
	systray.SetTooltip("Entangle — Ready to receive")

	mStatus := systray.AddMenuItem("● Ready", "")
	mStatus.Disable() // informational only

	systray.AddSeparator()

	mOpen := systray.AddMenuItem("Open Downloads", "")
	mQuit := systray.AddMenuItem("Quit Entangle", "")

	// TODO: Start WebSocket server in background
	// go startServer()

	// TODO: Start mDNS announcement in background
	// go announceOnLAN()

	// Handle menu clicks
	go func() {
		for {
			select {
			case <-mOpen.ClickedCh:
				// TODO: openDownloadsFolder()
				log.Println("Open Downloads clicked")
			case <-mQuit.ClickedCh:
				systray.Quit()
			}
		}
	}()
}

func onExit() {
	// cleanup
}
