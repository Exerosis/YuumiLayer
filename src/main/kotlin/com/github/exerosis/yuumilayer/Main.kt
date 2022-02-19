package com.github.exerosis.yuumilayer

import com.github.exerosis.mynt.SocketProvider
import com.github.exerosis.mynt.base.Address
import com.github.exerosis.mynt.component1
import com.github.exerosis.mynt.component2
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.*
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseListener
import com.github.kwhat.jnativehook.mouse.NativeMouseMotionListener
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.awt.Robot
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.net.StandardSocketOptions.SO_KEEPALIVE
import java.net.StandardSocketOptions.TCP_NODELAY
import java.nio.channels.AsynchronousChannelGroup.withThreadPool
import java.util.concurrent.Executors

const val OP_KEY_PRESS = 0.toByte()
const val OP_KEY_RELEASE = 1.toByte()
const val OP_MOUSE_MOVE = 2.toByte()
const val OP_MOUSE_PRESS = 3.toByte()
const val OP_MOUSE_RELEASE = 4.toByte()

fun main(args: Array<String>) {
    val executor = Executors.newCachedThreadPool()
    val dispatcher = executor.asCoroutineDispatcher()
    val provider = SocketProvider(1048576, withThreadPool(executor)) {
        it.setOption(TCP_NODELAY, true)
        it.setOption(SO_KEEPALIVE, false)
    }
    val address = Address(args[1], args[2].toInt())
    if (args[0].startsWith("c", ignoreCase = true)) {
        val (_, write) = runBlocking(dispatcher) { provider.connect(address) }
        println("Got connection to Yuumi!")
        val listener = object : NativeKeyListener, NativeMouseListener, NativeMouseMotionListener {
            fun handle(operation: Byte, event: NativeKeyEvent) {
                if (event.modifiers and SHIFT_MASK == 0) return
                val code = when (event.keyCode) {
                    VC_Q -> KeyEvent.VK_Q
                    VC_W -> KeyEvent.VK_W
                    VC_E -> KeyEvent.VK_E
                    VC_R -> KeyEvent.VK_R
                    else -> return
                }; runBlocking {
                    write.byte(operation)
                    write.int(code)
                }
            }
            fun handle(operation: Byte, event: NativeMouseEvent) {
                if (event.modifiers and SHIFT_MASK == 0) return
                val button = when (event.button) {
                    NativeMouseEvent.BUTTON1 -> MouseEvent.BUTTON1_MASK
                    NativeMouseEvent.BUTTON2 -> MouseEvent.BUTTON2_MASK
                    NativeMouseEvent.BUTTON3 -> MouseEvent.BUTTON3_MASK
                    else -> return
                }; runBlocking {
                    write.byte(OP_MOUSE_MOVE)
                    write.int(event.x)
                    write.int(event.y)
                    if (operation != OP_MOUSE_MOVE) {
                        write.byte(operation)
                        write.byte(button.toByte())
                    }
                }
            }
            override fun nativeKeyPressed(event: NativeKeyEvent) = handle(OP_KEY_PRESS, event)
            override fun nativeKeyReleased(event: NativeKeyEvent) = handle(OP_KEY_RELEASE, event)
            override fun nativeMousePressed(event: NativeMouseEvent) = handle(OP_MOUSE_PRESS, event)
            override fun nativeMouseReleased(event: NativeMouseEvent) = handle(OP_MOUSE_RELEASE, event)
            override fun nativeMouseMoved(event: NativeMouseEvent) = handle(OP_MOUSE_MOVE, event)
        }
        GlobalScreen.registerNativeHook()
        GlobalScreen.addNativeKeyListener(listener)
        GlobalScreen.addNativeMouseListener(listener)
        GlobalScreen.addNativeMouseMotionListener(listener)
    } else while (provider.isOpen) runBlocking { try {
        val robot = Robot()
        println("Waiting for connections...")
        provider.accept(address).apply {
            println("Got connection to Yuumi!")
            while (isOpen) {
                when (read.byte()) {
                    OP_KEY_PRESS -> robot.keyPress(read.int())
                    OP_KEY_RELEASE -> robot.keyRelease(read.int())
                    OP_MOUSE_MOVE -> robot.mouseMove(read.int(), read.int())
                    OP_MOUSE_PRESS -> robot.mousePress(read.byte().toInt())
                    OP_MOUSE_RELEASE -> robot.mouseRelease(read.byte().toInt())
                }
            }
        }
    } catch (reason: Throwable) {
        reason.printStackTrace()
    } }
}