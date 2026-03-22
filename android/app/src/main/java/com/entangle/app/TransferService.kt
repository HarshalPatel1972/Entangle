package com.entangle.app

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import org.json.JSONObject

class TransferService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS) // no timeout for ws
        .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bundle = intent?.getBundleExtra("transfer_data") ?: return START_NOT_STICKY

        scope.launch {
            val desktop = withTimeoutOrNull(8000) { findDesktop(applicationContext) }

            if (desktop == null) {
                showNotification("Entangle", "Could not find laptop on network")
                stopSelf(startId)
                return@launch
            }

            val verified = verifyDesktop(desktop.host, desktop.port)
            if (!verified) {
                showNotification("Entangle", "Could not connect to laptop")
                stopSelf(startId)
                return@launch
            }

            val type = bundle.getString("type")
            when (type) {
                "file" -> {
                    val uri = bundle.getParcelable<Uri>("uri") ?: return@launch
                    val mimeType = bundle.getString("mimeType") ?: "*/*"
                    transferFile(desktop, uri, mimeType)
                }
                "text" -> {
                    val content = bundle.getString("content") ?: return@launch
                    transferText(desktop, content)
                }
            }

            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private suspend fun transferFile(desktop: DesktopInfo, uri: Uri, mimeType: String) {
        val contentResolver = applicationContext.contentResolver

        // Get file info
        val fileName = getFileName(uri)
        val fileSize = getFileSize(uri)

        val wsUrl = "ws://${desktop.host}:${desktop.port}/transfer"
        val request = Request.Builder().url(wsUrl).build()

        val transferred = CompletableDeferred<Boolean>()

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Send header
                val header = JSONObject().apply {
                    put("type", "file")
                    put("name", fileName)
                    put("size", fileSize)
                    put("mime", mimeType)
                }.toString()

                webSocket.send(header)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                when (json.getString("status")) {
                    "ready" -> {
                        // Desktop is ready — send file in chunks
                        scope.launch {
                            val inputStream = contentResolver.openInputStream(uri)
                            val buffer = ByteArray(64 * 1024) // 64KB chunks
                            var bytesRead: Int

                            while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                                webSocket.send(ByteString.of(*buffer.copyOf(bytesRead)))
                            }

                            inputStream?.close()
                            webSocket.send(JSONObject().put("status", "done").toString())
                        }
                    }
                    "complete" -> {
                        showNotification("Entangle", "✓ $fileName sent to laptop")
                        transferred.complete(true)
                        webSocket.close(1000, null)
                    }
                    "error" -> {
                        transferred.complete(false)
                        webSocket.close(1000, null)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                transferred.complete(false)
            }
        })

        transferred.await()
    }

    private suspend fun transferText(desktop: DesktopInfo, content: String) {
        val wsUrl = "ws://${desktop.host}:${desktop.port}/transfer"
        val request = Request.Builder().url(wsUrl).build()

        val transferred = CompletableDeferred<Boolean>()

        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val header = JSONObject().apply {
                    put("type", "text")
                    put("content", content)
                    put("mime", "text/plain")
                }.toString()
                webSocket.send(header)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.getString("status") == "complete") {
                    showNotification("Entangle", "✓ Text copied to laptop clipboard")
                    transferred.complete(true)
                    webSocket.close(1000, null)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                transferred.complete(false)
            }
        })

        transferred.await()
    }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        } ?: "entangle_file_${System.currentTimeMillis()}"
    }

    private fun getFileSize(uri: Uri): Long {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
            it.moveToFirst()
            it.getLong(sizeIndex)
        } ?: 0L
    }

    private suspend fun showNotification(title: String, message: String) {
        withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(applicationContext, message,
                android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        client.dispatcher.executorService.shutdown()
        super.onDestroy()
    }
}
