package com.neko.hostpotproxy.core

import android.text.TextUtils
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.regex.Pattern

enum class ChannelStatus { STATUS_LINE, HEADERS, CONTENT }

interface ChannelListener {
    fun onClose(c: Channel); fun onContent(c: Channel); fun onHeaders(c: Channel); fun onStatusLine(c: Channel)
}

class Channel(val isRequest: Boolean) {
    var socket: SocketChannel? = null
    var selectionKey: SelectionKey? = null
    var listener: ChannelListener? = null
    var status: ChannelStatus = ChannelStatus.STATUS_LINE
    
    private var headers = HashMap<String, String>()
    private var buffer = ByteBuffer.allocate(16384) 
    private var readBuf = CharArray(4096) 
    private var readOffset = 0
    
    var statusLine: String? = null
    var method: String? = null
    var protocol: String? = "HTTP/1.1"
    var host: String? = null
    var port: Int = 80

    fun reset() {
        status = if (method == "CONNECT") ChannelStatus.CONTENT else ChannelStatus.STATUS_LINE
        headers.clear()
        readOffset = 0
    }

    fun getSocketBuffer() = buffer

    fun read() {
        buffer.clear()
        val count = try { socket?.read(buffer) ?: -1 } catch (e: Exception) { -1 }

        if (count == -1) {
            listener?.onClose(this)
            return
        }
        
        buffer.flip()
        if (buffer.limit() == 0) return

        if (status == ChannelStatus.CONTENT) {
            listener?.onContent(this)
            return
        }

        while (buffer.hasRemaining()) {
            val line = readLine() ?: break
            
            when (status) {
                ChannelStatus.STATUS_LINE -> {
                    parseStatusLine(line)
                    status = ChannelStatus.HEADERS
                    listener?.onStatusLine(this)
                }
                ChannelStatus.HEADERS -> {
                    if (TextUtils.isEmpty(line)) {
                        status = ChannelStatus.CONTENT
                        listener?.onHeaders(this)
                        if (buffer.hasRemaining()) {
                            listener?.onContent(this)
                        }
                        return 
                    }
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) headers[parts[0].trim()] = parts[1].trim()
                }
                else -> {}
            }
        }
    }

    private fun readLine(): String? {
        while (buffer.hasRemaining()) {
            val c = buffer.get().toInt().toChar()
            if (c == '\n') {
                val line = String(readBuf, 0, readOffset).trim()
                readOffset = 0
                return line
            } else if (c != '\r') {
                if (readOffset < readBuf.size) {
                    readBuf[readOffset++] = c
                }
            }
        }
        return null
    }

    fun write(buf: ByteBuffer): Int {
        if (!buf.hasRemaining()) return 0
        return try { socket?.write(buf) ?: 0 } catch (e: Exception) { 0 }
    }

    fun close() {
        try { selectionKey?.cancel(); socket?.close() } catch (e: Exception) {}
    }
    
    private fun parseStatusLine(line: String) {
        statusLine = line
        val parts = line.split(" ")
        if (isRequest && parts.size >= 2) {
            method = parts[0]
            val uri = parts[1]
            protocol = if (parts.size > 2) parts[2] else "HTTP/1.1"
            
            if (method == "CONNECT") {
                val hp = uri.split(":")
                host = hp[0]
                port = hp.getOrNull(1)?.toIntOrNull() ?: 443
            }
        }
    }
    
    fun resolveHost(): String? {
        if (host != null && method == "CONNECT") return host

        val uri = statusLine?.split(" ")?.getOrNull(1) ?: return null
        
        if (method != "CONNECT") {
            val hostHeader = headers["Host"]
            if (hostHeader != null) {
                val parts = hostHeader.split(":")
                host = parts[0]
                port = parts.getOrNull(1)?.toIntOrNull() ?: 80
            } else {
                val matcher = Pattern.compile("http://([^/:]+)(:(\\d+))?.*").matcher(uri)
                if (matcher.find()) {
                    host = matcher.group(1)
                    port = matcher.group(3)?.toIntOrNull() ?: 80
                }
            }
        }
        return host
    }

    fun getUrlPath(): String {
        val uri = statusLine?.split(" ")?.getOrNull(1) ?: "/"
        if (uri.startsWith("http")) {
             val idx = uri.indexOf("/", 8)
             return if (idx != -1) uri.substring(idx) else "/"
        }
        return uri
    }
    
    fun buildHeaderString(): String {
        return headers.entries.joinToString("\r\n") { "${it.key}: ${it.value}" }
    }
}
