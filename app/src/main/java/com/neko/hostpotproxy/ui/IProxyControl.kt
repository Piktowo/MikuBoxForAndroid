package com.neko.hostpotproxy.ui

import android.os.Binder
import android.os.IBinder
import android.os.IInterface

interface IProxyControl : IInterface {
    fun getPort(): Int
    fun isRunning(): Boolean
    fun start(): Boolean
    fun stop(): Boolean

    abstract class Stub : Binder(), IProxyControl {

        init {
            this.attachInterface(this, DESCRIPTOR)
        }

        override fun asBinder(): IBinder {
            return this
        }

        companion object {
            private const val DESCRIPTOR = "com.neko.hostpotproxy.ui.IProxyControl"

            fun asInterface(obj: IBinder?): IProxyControl? {
                if (obj == null) return null
                
                val iin = obj.queryLocalInterface(DESCRIPTOR)
                if (iin != null && iin is IProxyControl) {
                    return iin
                }
                
                return null 
            }
        }
    }
}
