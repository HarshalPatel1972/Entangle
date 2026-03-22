package main

import "github.com/gen2brain/beeep"

func showNotification(title, message string) {
	beeep.Notify(title, message, "")
}
