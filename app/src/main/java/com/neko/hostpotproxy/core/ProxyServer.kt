package com.neko.hostpotproxy.core

import android.util.Log
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel

class ProxyServer private constructor() {

    private var port = DEFAULT_PORT
    private var running = false
    private var selector: Selector? = null
    private var server: ServerSocketChannel? = null
    private var serverThread: Thread? = null

    companion object {
        const val TAG = "ProxyServer"
        private const val DEFAULT_PORT = 7071
        private const val MAX_PORT = 50146
        
        @Volatile
        private var instance: ProxyServer? = null

        fun getInstance(): ProxyServer {
            return instance ?: synchronized(this) {
                instance ?: ProxyServer().also { instance = it }
            }
        }
    }

    @Synchronized
    fun start(): Boolean {
        if (running) return true
        
        Log.d(TAG, "Start proxy server...")
        try {
            selector = Selector.open()
            server = ServerSocketChannel.open()
            server?.configureBlocking(false)
            
            port = DEFAULT_PORT
            while (port < MAX_PORT) {
                try {
                    server?.socket()?.bind(InetSocketAddress("0.0.0.0", port))
                    Log.d(TAG, "Proxy bound to 0.0.0.0:$port")
                    break
                } catch (e: Exception) {
                    port++
                }
            }

            if (port >= MAX_PORT) return false

            server?.register(selector, SelectionKey.OP_ACCEPT)
            running = true
            
            serverThread = Thread { doProxy() }.apply { 
                name = "NekoProxyThread"
                start() 
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Start failed", e)
            running = false
            return false
        }
    }

    @Synchronized
    fun stop(): Boolean {
        if (!running) return false
        
        Log.d(TAG, "Stop proxy server...")
        running = false
        try {
            selector?.wakeup()
            serverThread?.join(500)
            selector?.close()
        } catch (e: Exception) { e.printStackTrace() }
        
        try { server?.close() } catch (e: Exception) { e.printStackTrace() }
        
        selector = null
        server = null
        serverThread = null
        return true
    }

    private fun doProxy() {
        while (running) {
            try {
                val sel = selector ?: break
                if (sel.select(1000) == 0) continue
                if (!sel.isOpen) break

                val selectedKeys = sel.selectedKeys()
                val iterator = selectedKeys.iterator()

                while (iterator.hasNext()) {
                    val key = iterator.next()
                    iterator.remove()

                    if (!key.isValid) continue

                    val attachment = key.attachment()

                    try {
                        if (attachment is ChannelPair) {
                            attachment.handleKey(key)
                        } else if (key.isAcceptable) {
                            val newPair = ChannelPair()
                            newPair.handleKey(key)
                        }
                    } catch (e: Exception) {
                        key.cancel()
                        if (attachment is ChannelPair) attachment.close()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Selector loop error", e)
            }
        }
    }
    
    fun getSelector() = selector
    fun getPort() = port
    val isRunning get() = running
}
