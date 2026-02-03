package com.neko.hostpotproxy.core

import android.util.Log
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class ChannelPair : ChannelListener {

    private var requestChannel: Channel? = null
    private var responseChannel: Channel? = null

    companion object {
        const val CONNECT_OK = "HTTP/1.1 200 Connection Established\r\nProxy-agent: NekoProxy\r\n\r\n"
    }

    fun handleKey(key: SelectionKey?) {
        if (key == null || !key.isValid) {
            close()
            return
        }

        try {
            if (key.isAcceptable) {
                acceptConnection(key)
            } else {
                val req = requestChannel
                val res = responseChannel
                
                if (req != null && key == req.selectionKey) {
                    req.read()
                } else if (res != null && key == res.selectionKey) {
                    res.read()
                }
            }
        } catch (e: Exception) {
            close()
        }
    }

    private fun acceptConnection(key: SelectionKey) {
        try {
            val serverSocket = key.channel() as ServerSocketChannel
            val clientSocket = serverSocket.accept() ?: return

            clientSocket.configureBlocking(false)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                clientSocket.setOption(StandardSocketOptions.TCP_NODELAY, true)
            }
            
            requestChannel = Channel(true).apply {
                this.socket = clientSocket
                this.listener = this@ChannelPair
                this.selectionKey = clientSocket.register(
                    ProxyServer.getInstance().getSelector(), SelectionKey.OP_READ, this@ChannelPair
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        }
    }

    override fun onHeaders(channel: Channel) {
        if (channel.isRequest) {
            if (!connectToInternet(channel)) close()
        } else {
            val raw = "${channel.statusLine}\r\n${channel.buildHeaderString()}\r\n"
            writeToChannel(requestChannel, ByteBuffer.wrap(raw.toByteArray()))
        }
    }

    private fun connectToInternet(channel: Channel): Boolean {
        try {
            val host = channel.resolveHost() ?: return false
            
            if (responseChannel == null) {
                val socket = SocketChannel.open()
                socket.configureBlocking(false)
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    socket.setOption(StandardSocketOptions.TCP_NODELAY, true)
                }

                val remoteAddress = InetSocketAddress(InetAddress.getByName(host), channel.port)
                val connected = socket.connect(remoteAddress)
                
                if (!connected) {
                    var retry = 0
                    while (!socket.finishConnect()) {
                        if (retry++ > 50) {
                            socket.close()
                            return false 
                        }
                        Thread.sleep(10)
                    }
                }

                responseChannel = Channel(false).apply {
                    this.socket = socket
                    this.listener = this@ChannelPair
                    this.selectionKey = socket.register(
                        ProxyServer.getInstance().getSelector(), SelectionKey.OP_READ, this@ChannelPair
                    )
                }
            } else {
                responseChannel!!.reset()
            }

            if (channel.method == "CONNECT") {
                writeToChannel(requestChannel, ByteBuffer.wrap(CONNECT_OK.toByteArray()))
                
                responseChannel?.status = ChannelStatus.CONTENT
                requestChannel?.status = ChannelStatus.CONTENT // PENTING: Force request jadi content mode
                
                onContent(channel)
                
            } else {
                val raw = "${channel.method} ${channel.getUrlPath()} ${channel.protocol}\r\n${channel.buildHeaderString()}\r\n"
                writeToChannel(responseChannel, ByteBuffer.wrap(raw.toByteArray()))
                
                onContent(channel)
            }
            return true
        } catch (e: Exception) {
            Log.e("ChannelPair", "Connect Fail: ${channel.host}", e)
            return false
        }
    }

    override fun onContent(channel: Channel) {
        val target = if (channel.isRequest) responseChannel else requestChannel
        
        if (target != null && target.socket?.isConnected == true) {
            try {
                val buffer = channel.getSocketBuffer()
                if (buffer.position() > 0) {
                     writeToChannel(target, buffer)
                }
            } catch (e: Exception) {
                close()
            }
        }
    }

    private fun writeToChannel(channel: Channel?, buffer: ByteBuffer) {
        try {
            channel?.write(buffer)
        } catch (e: Exception) {
            close()
        }
    }

    override fun onClose(channel: Channel) = close()
    override fun onStatusLine(channel: Channel) {}

    fun close() {
        try { requestChannel?.close() } catch (e: Exception) {}
        try { responseChannel?.close() } catch (e: Exception) {}
    }
}
