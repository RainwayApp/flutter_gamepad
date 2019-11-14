package com.example.flutter_gamepad

import android.os.Build
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import io.flutter.embedding.android.AndroidTouchProcessor
import io.flutter.view.FlutterView
import io.flutter.embedding.engine.renderer.FlutterRenderer
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

enum class Button {
    A,
    B,
    X,
    Y,
    DpadUp,
    DpadDown,
    DpadLeft,
    DpadRight,
    Menu,
    Options,
    LeftThumbstickButton,
    RightThumbstickButton,
    LeftShoulder,
    RightShoulder,
    LeftTrigger,
    RightTrigger,
}

enum class Thumbstick {
    Left,
    Right,
}

val InputDevice.isGamepad: Boolean
    get() = sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD


class GamepadStreamHandler : EventChannel.StreamHandler {
    var eventSink: EventChannel.EventSink? = null
    override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink?) {
        this.eventSink = eventSink
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id)
            if (device.isGamepad) {
                this.gamepadConnected(device)
            }
        }
    }

    override fun onCancel(p0: Any?) {
        this.eventSink = null //To change body of created functions use File | Settings | File Templates.
    }

    private fun gamepadInfoDictionary(device: InputDevice): HashMap<String, Any> {
        val gamepadInfo: HashMap<String, Any> = HashMap()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            gamepadInfo["isAttachedToDevice"] = !device.isExternal
        }
        gamepadInfo["vendorName"] = device.name
        gamepadInfo["productCategory"] = "Android Gamepad"
        gamepadInfo["id"] = device.id
        return gamepadInfo
    }

    private fun gamepadConnected(device: InputDevice) {
        eventSink?.success(hashMapOf(
                "event" to "gamepadConnected",
                "gamepadId" to device.id,
                "gamepadInfo" to gamepadInfoDictionary(device)
        ))
    }

    private fun gamepadDisconnected(device: InputDevice) {
        // TODO
    }

    fun allGamepadInfoDictionaries(): List<HashMap<String, Any>> =
            InputDevice.getDeviceIds()
                    .map(InputDevice::getDevice)
                    .filter { it.isGamepad }
                    .map { gamepadInfoDictionary(it) }
}

class GamepadAndroidTouchProcessor(renderer: FlutterRenderer) : AndroidTouchProcessor(renderer) {
    companion object {
        const val TAG: String = "GamepadAndroidTouchProc";
    }
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        Log.d(TAG, "${event.action}, ${event.source}, ${Build.VERSION.SDK_INT >= 18 && event.isFromSource(InputDevice.SOURCE_CLASS_JOYSTICK)}")
        return super.onGenericMotionEvent(event)
    }
}

class FlutterGamepadPlugin : MethodCallHandler {
    companion object {
        val gamepadStreamHandler: GamepadStreamHandler = GamepadStreamHandler()

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val methodChannel = MethodChannel(registrar.messenger(), "com.rainway.flutter_gamepad/methods")
            methodChannel.setMethodCallHandler(FlutterGamepadPlugin())
            val eventChannel = EventChannel(registrar.messenger(), "com.rainway.flutter_gamepad/events")
            eventChannel.setStreamHandler(gamepadStreamHandler)

            // Hack: swap in a new AndroidTouchProcessor.
            val view: FlutterView = registrar.view()
            val touchProcessorField = FlutterView::class.java.getDeclaredField("androidTouchProcessor")
            val rendererField = FlutterView::class.java.getDeclaredField("flutterRenderer")
            rendererField.isAccessible = true
            val renderer = rendererField.get(view) as FlutterRenderer
            touchProcessorField.isAccessible = true
            val touchProcessor = GamepadAndroidTouchProcessor(renderer)
            touchProcessorField.set(registrar.view(), touchProcessor)
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "gamepads") {
            result.success(gamepadStreamHandler.allGamepadInfoDictionaries())
        } else {
            result.notImplemented()
        }
    }
}
