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
import com.sun.javafx.scene.control.skin.FXVK.attach
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.awt.MouseInfo.getPointerInfo
import java.awt.PointerInfo
import java.awt.Robot
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import java.awt.event.MouseEvent
import java.net.StandardSocketOptions.SO_KEEPALIVE
import java.net.StandardSocketOptions.TCP_NODELAY
import java.nio.channels.AsynchronousChannelGroup.withThreadPool
import java.util.concurrent.Executors
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.CTRL_MASK as MASK_CTRL
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.SHIFT_MASK as MAKS_SHIFT

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
            override fun nativeKeyPressed(event: NativeKeyEvent) {
                if (event.modifiers and MAKS_SHIFT != 0) return
                val ctrl = event.modifiers and MASK_CTRL != 0
                println("Shift: true Ctrl: $ctrl")
                val operation = when (event.keyCode) {
                    VC_Q -> if (ctrl) OP_LEVEL_MISSILE else OP_MISSILE
                    VC_W -> if (ctrl) OP_LEVEL_ATTACH else OP_ATTACH
                    VC_E -> if (ctrl) OP_LEVEL_HEAL else OP_HEAL
                    VC_R -> if (ctrl) OP_LEVEL_ULT else OP_ULT
                    VC_B -> OP_BACK; VC_4 -> OP_WARD
                    VC_1 -> OP_ACTIVE; VC_2 -> OP_CENTER
                    VC_D -> OP_D; VC_F -> OP_F
                    VC_5 -> OP_CONTROL; else -> return
                }; runBlocking {
                    write.byte(OP_MOUSE)
                    val point = getPointerInfo().location
                    println(point)
                    write.int(point.x)
                    write.int(point.y)
                    write.byte(operation)
                }
            }
            override fun nativeMouseMoved(event: NativeMouseEvent) = runBlocking {
                write.byte(OP_MOUSE); write.int(event.x); write.int(event.y)
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
                    write.byte(operation)
                    write.byte(button.toByte())
                }
            }
            override fun nativeMousePressed(event: NativeMouseEvent) = handle(OP_MOUSE_PRESS, event)
            override fun nativeMouseReleased(event: NativeMouseEvent) = handle(OP_MOUSE_RELEASE, event)
        }
        GlobalScreen.registerNativeHook()
        GlobalScreen.addNativeKeyListener(listener)
//        GlobalScreen.addNativeMouseListener(listener)
        GlobalScreen.addNativeMouseMotionListener(listener)
    } else while (provider.isOpen) runBlocking {
        try {
            val robot = Robot(); val scale = 1 / args[3].toDouble()
            fun press(key: Int, block: () -> (Unit) = {}) {
                robot.keyPress(key); block(); robot.keyRelease(key)
            }
            fun move(x: Int, y: Int) = robot.mouseMove(
                (x * scale).toInt(), (y * scale).toInt()
            )
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