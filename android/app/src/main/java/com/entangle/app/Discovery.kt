package com.entangle.app

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

const val SERVICE_TYPE = "_entangle._tcp."
const val DESKTOP_PORT = 7297

data class DesktopInfo(val host: String, val port: Int)

suspend fun findDesktop(context: Context): DesktopInfo? {
    return suspendCancellableCoroutine { continuation ->
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                if (continuation.isActive) continuation.resume(null)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}

            override fun onDiscoveryStarted(serviceType: String) {}

            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // Found Entangle desktop — resolve to get IP
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        if (continuation.isActive) {
                            val host = serviceInfo.host.hostAddress ?: return
                            val port = serviceInfo.port
                            continuation.resume(DesktopInfo(host, port))
                        }
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)

        continuation.invokeOnCancellation {
            try { nsdManager.stopServiceDiscovery(listener) } catch (e: Exception) {}
        }
    }
}

// Verify connection before attempting transfer
suspend fun verifyDesktop(host: String, port: Int): Boolean {
    return try {
        val url = java.net.URL("http://$host:$port/ping")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 2000
        connection.readTimeout = 2000
        val response = connection.inputStream.bufferedReader().readText()
        response.contains("entangle")
    } catch (e: Exception) {
        false
    }
}
