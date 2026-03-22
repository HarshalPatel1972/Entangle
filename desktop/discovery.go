package main

import (
	"context"

	"github.com/grandcat/zeroconf"
)

const ServiceType = "_entangle._tcp"
const ServiceDomain = "local."

func announceOnLAN() {
	server, err := zeroconf.Register(
		"Entangle",     // instance name
		ServiceType,    // service type
		ServiceDomain,  // domain
		7297,           // port
		[]string{"version=1.0"}, // TXT records
		nil,            // interfaces (nil = all)
	)
	if err != nil {
		// mDNS failed — Android will fall back to manual IP
		return
	}
	defer server.Shutdown()

	// Keep announcing until shutdown
	ctx := context.Background()
	<-ctx.Done()
}
