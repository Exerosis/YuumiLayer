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
import java.awt.event.KeyEvent.*
import java.net.StandardSocketOptions.SO_KEEPALIVE
import java.net.StandardSocketOptions.TCP_NODELAY
import java.nio.channels.AsynchronousChannelGroup.withThreadPool
import java.util.concurrent.Executors

const val OP_MOUSE_MOVE = 2.toByte()
const val OP_MOUSE_PRESS = 3.toByte()
const val OP_MOUSE_RELEASE = 4.toByte()


const val OP_MOUSE = 0.toByte()

const val OP_MISSILE = 1.toByte()
const val OP_ATTACH = 2.toByte()
const val OP_HEAL = 3.toByte()
const val OP_ULT = 4.toByte()
const val OP_ACTIVE = 5.toByte()
const val OP_D = 6.toByte()
const val OP_F = 7.toByte()
const val OP_WARD = 8.toByte()
const val OP_CONTROL = 9.toByte()
const val OP_BACK = 10.toByte()
const val OP_LEVEL_MISSILE = 11.toByte()
const val OP_LEVEL_ATTACH = 12.toByte()
const val OP_LEVEL_HEAL = 13.toByte()
const val OP_LEVEL_ULT = 14.toByte()
const val OP_CENTER =  15.toByte()


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
            var shift = false; var ctrl = false
            override fun nativeKeyPressed(event: NativeKeyEvent) {
                if (event.keyCode == 42) {
                    shift = true
                }
                else if (event.keyCode == 29) ctrl = true
                else if (!shift) return
                println("Control: $ctrl")
                val operation = when (event.keyCode) {
                    VC_Q -> if (ctrl) OP_LEVEL_MISSILE else OP_MISSILE
                    VC_W -> if (ctrl) OP_LEVEL_ATTACH else OP_ATTACH
                    VC_E -> if (ctrl) OP_LEVEL_HEAL else OP_HEAL
                    VC_R -> if (ctrl) OP_LEVEL_ULT else OP_ULT
                    VC_B -> OP_BACK; VC_4 -> OP_WARD
                    VC_1 -> OP_ACTIVE; VC_2 -> OP_CENTER
                    VC_D -> OP_D; VC_F -> OP_F
                    VC_5 -> OP_CONTROL; else -> return
                }; runBlocking { write.byte(operation) }
            }
            override fun nativeKeyReleased(event: NativeKeyEvent) {
                if (event.keyCode == 42) shift = false
                else if (event.keyCode == 29) ctrl = false
            }
            override fun nativeMouseMoved(event: NativeMouseEvent) = runBlocking {
                write.byte(OP_MOUSE); write.int(event.x); write.int(event.y)
            }
        }
        GlobalScreen.registerNativeHook()
        GlobalScreen.addNativeKeyListener(listener)
//        GlobalScreen.addNativeMouseListener(listener)
        GlobalScreen.addNativeMouseMotionListener(listener)
    } else while (provider.isOpen) runBlocking {
        try {
            val robot = Robot(); val scale = 1 / args[3].toDouble()
            var pressing = false
            fun press(key: Int, block: () -> (Unit) = {}) {
                pressing = true
                robot.keyPress(key); block(); robot.keyRelease(key)
                pressing = false
            }
            fun move(x: Int, y: Int) {
                if (!pressing) robot.mouseMove(
                    (x * scale).toInt(), (y * scale).toInt()
                )
            }
            println("Waiting for connections...")
            provider.accept(address).apply {
                println("Got connection to Yuumi!")
                while (isOpen) when (read.byte()) {
                    OP_MOUSE -> move(read.int(), read.int())
                    OP_MISSILE -> press(VK_Q); OP_ULT -> press(VK_R)
                    OP_HEAL -> press(VK_E); OP_CONTROL -> press(VK_3)
                    OP_ACTIVE -> press(VK_1); OP_D -> press(VK_D)
                    OP_F -> press(VK_F); OP_WARD -> press(VK_4)
                    OP_BACK -> press(VK_B); OP_CENTER -> press(VK_F1)
                    OP_ATTACH -> { move(3742, 1209); press(VK_W) }
                    OP_LEVEL_MISSILE -> press(VK_CONTROL) { press(VK_Q) }
                    OP_LEVEL_ATTACH -> press(VK_CONTROL) { press(VK_W) }
                    OP_LEVEL_HEAL -> press(VK_CONTROL) { press(VK_E) }
                    OP_LEVEL_ULT -> press(VK_CONTROL) { press(VK_R) }
                }
            }
        } catch (reason: Throwable) {
            println("Connection lost: $reason")
        }
    }
}