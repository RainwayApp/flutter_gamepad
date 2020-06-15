package com.example.flutter_gamepad

import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import io.flutter.plugin.common.EventChannel

val InputDevice.isGamepad: Boolean
    get() = sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD

val InputEvent.deviceIsGamepad: Boolean
    get() = InputDevice.getDevice(deviceId)?.isGamepad ?: false

/**
 * A singleton object that manages the gamepad event stream. It is fed Android events and
 * redirects the gamepad ones into a Flutter EventSink via its GamepadCache.
 */
object GamepadStreamHandler : EventChannel.StreamHandler {
    private var gamepadCache: GamepadCache? = null
    override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink?) {
        this.gamepadCache = GamepadCache(eventSink!!)
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id)
            if (device.isGamepad) {
                gamepadCache!!.touchGamepad(id)
            }
        }
    }

    override fun onCancel(arguments: Any?) {
        this.gamepadCache = null
    }

    // Returns true if the cache handled the event, or false if it should be bubbled up.
    fun processMotionEvent(event: MotionEvent): Boolean {
        return event.deviceIsGamepad && gamepadCache?.processMotionEvent(event) ?: false
    }

    // Returns true if the cache handled the event, or false if it should be bubbled up.
    fun processKeyEvent(event: KeyEvent): Boolean {
        Log.d("GSH", "$event")
        return event.deviceIsGamepad && gamepadCache?.processKeyEvent(event) ?: false
    }
}
