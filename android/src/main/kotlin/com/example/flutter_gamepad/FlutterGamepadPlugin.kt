package com.example.flutter_gamepad

import android.os.Build
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import io.flutter.embedding.android.AndroidKeyProcessor
import io.flutter.embedding.android.AndroidTouchProcessor
import io.flutter.view.FlutterView
import io.flutter.embedding.engine.renderer.FlutterRenderer
import io.flutter.embedding.engine.systemchannels.KeyEventChannel
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.editing.TextInputPlugin

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

val buttonMap = hashMapOf(
        KeyEvent.KEYCODE_BUTTON_A to Button.A,
        KeyEvent.KEYCODE_BUTTON_B to Button.B,
        KeyEvent.KEYCODE_BUTTON_X to Button.X,
        KeyEvent.KEYCODE_BUTTON_Y to Button.Y,
        // KeyEvent.KEYCODE_DPAD_UP to Button.DpadUp,
        // KeyEvent.KEYCODE_DPAD_DOWN to Button.DpadDown,
        // KeyEvent.KEYCODE_DPAD_LEFT to Button.DpadLeft,
        // KeyEvent.KEYCODE_DPAD_RIGHT to Button.DpadRight,
        KeyEvent.KEYCODE_BUTTON_START to Button.Menu,
        KeyEvent.KEYCODE_BUTTON_SELECT to Button.Options,
        KeyEvent.KEYCODE_BUTTON_THUMBL to Button.LeftThumbstickButton,
        KeyEvent.KEYCODE_BUTTON_THUMBR to Button.RightThumbstickButton,
        KeyEvent.KEYCODE_BUTTON_L1 to Button.LeftShoulder,
        KeyEvent.KEYCODE_BUTTON_R1 to Button.RightShoulder,
        KeyEvent.KEYCODE_BUTTON_L2 to Button.LeftTrigger,
        KeyEvent.KEYCODE_BUTTON_R2 to Button.RightTrigger
)

enum class Thumbstick {
    Left,
    Right,
}

val InputDevice.isGamepad: Boolean
    get() = sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD

/*
class GamepadState {
    var axisValues: HashMap<Int, Float> = hashMapOf()
    fun getAxis(axis: Int) {
    }
}

class GamepadCache {
    var gamepads: HashMap<Int, GamepadState> = hashMapOf()
    fun axis(gamepadId: Int, axis: Int): Float {
        gamepads.getOrPut(gamepadId) { GamepadState() }.getAxis(axis)
    }
}
 */

object GamepadStreamHandler : EventChannel.StreamHandler {
    var eventSink: EventChannel.EventSink? = null
    // var gamepadCache = GamepadCache()
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

    fun allGamepadInfoDictionaries(): List<HashMap<String, Any>> =
            InputDevice.getDeviceIds()
                    .map(InputDevice::getDevice)
                    .filter { it.isGamepad }
                    .map { gamepadInfoDictionary(it) }

    private fun reportThumbstick(event: MotionEvent, thumbstick: Thumbstick, xAxis: Int, yAxis: Int) {
        val x = event.getAxisValue(xAxis)
        val y = event.getAxisValue(yAxis)
        eventSink?.success(
                hashMapOf("event" to "thumbstick", "gamepadId" to event.deviceId,
                        "thumbstick" to thumbstick.ordinal, "x" to x, "y" to y))
    }

    private fun reportAxisButton(event: MotionEvent, button: Button, axis: Int) {
        val value = event.getAxisValue(axis)
        eventSink?.success(
                hashMapOf("event" to "button", "gamepadId" to event.deviceId,
                        "button" to button.ordinal, "value" to value))
    }

//    private fun reportHatAxis(event: MotionEvent, buttonMinus: Button, buttonPlus: Button, axis: Int) {
//        val value = event.getAxisValue(axis)
//        eventSink?.success(
//                hashMapOf("event" to "button", "gamepadId" to event.deviceId,
//                        "button" to button.ordinal, "value" to value))
//        )
//    }

    fun processEvent(event: MotionEvent) {
        reportThumbstick(event, Thumbstick.Left, MotionEvent.AXIS_X, MotionEvent.AXIS_Y)
        reportThumbstick(event, Thumbstick.Right, MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ)
        reportAxisButton(event, Button.LeftTrigger, MotionEvent.AXIS_BRAKE)
        reportAxisButton(event, Button.RightTrigger, MotionEvent.AXIS_GAS)
//        reportHatAxis(event, Button.DpadLeft, Button.DpadRight, MotionEvent.AXIS_HAT_X)
//        reportHatAxis(event, Button.DpadUp, Button.DpadDown, MotionEvent.AXIS_HAT_Y)
        reportAxisButton(event, Button.A, MotionEvent.AXIS_HAT_X)
        reportAxisButton(event, Button.B, MotionEvent.AXIS_HAT_Y)
    }
}

class GamepadAndroidTouchProcessor(renderer: FlutterRenderer) : AndroidTouchProcessor(renderer) {
    companion object {
        const val TAG: String = "GamepadAndroidTouchPro"
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return if (Build.VERSION.SDK_INT >= 18 && event.isFromSource(InputDevice.SOURCE_CLASS_JOYSTICK) && event.action == MotionEvent.ACTION_MOVE) {
            GamepadStreamHandler.processEvent(event)
            true
        } else {
            super.onGenericMotionEvent(event)
        }
    }
}

class GamepadAndroidKeyProcessor(keyEventChannel: KeyEventChannel, textInputPlugin: TextInputPlugin) : AndroidKeyProcessor(keyEventChannel, textInputPlugin) {
    override fun onKeyDown(keyEvent: KeyEvent) {
        val button = buttonMap[keyEvent.keyCode]
        if (button != null) {
            GamepadStreamHandler.eventSink?.success(hashMapOf("event" to "button", "gamepadId" to keyEvent.deviceId, "button" to button.ordinal, "value" to 1.0))
        } else {
            return super.onKeyDown(keyEvent)
        }
    }

    override fun onKeyUp(keyEvent: KeyEvent) {
        val button = buttonMap[keyEvent.keyCode]
        if (button != null) {
            GamepadStreamHandler.eventSink?.success(hashMapOf("event" to "button", "gamepadId" to keyEvent.deviceId, "button" to button.ordinal, "value" to 0.0))
        } else {
            return super.onKeyDown(keyEvent)
        }
    }
}

class FlutterGamepadPlugin : MethodCallHandler {
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val methodChannel = MethodChannel(registrar.messenger(), "com.rainway.flutter_gamepad/methods")
            methodChannel.setMethodCallHandler(FlutterGamepadPlugin())
            val eventChannel = EventChannel(registrar.messenger(), "com.rainway.flutter_gamepad/events")
            eventChannel.setStreamHandler(GamepadStreamHandler)

            val view: FlutterView = registrar.view()

            // Hack: swap in a new AndroidTouchProcessor.
            val touchProcessorField = FlutterView::class.java.getDeclaredField("androidTouchProcessor")
            touchProcessorField.isAccessible = true
            val rendererField = FlutterView::class.java.getDeclaredField("flutterRenderer")
            rendererField.isAccessible = true
            val renderer = rendererField.get(view) as FlutterRenderer
            val touchProcessor = GamepadAndroidTouchProcessor(renderer)
            touchProcessorField.set(view, touchProcessor)

            // Hack: swap in a new AndroidKeyProcessor.
            val keyProcessorField = FlutterView::class.java.getDeclaredField("androidKeyProcessor")
            keyProcessorField.isAccessible = true
            val keyEventChannelField = FlutterView::class.java.getDeclaredField("keyEventChannel")
            keyEventChannelField.isAccessible = true
            val textInputPluginField = FlutterView::class.java.getDeclaredField("mTextInputPlugin")
            textInputPluginField.isAccessible = true
            val keyEventChannel = keyEventChannelField.get(view) as KeyEventChannel
            val textInputPlugin = textInputPluginField.get(view) as TextInputPlugin
            val keyProcessor = GamepadAndroidKeyProcessor(keyEventChannel, textInputPlugin)
            keyProcessorField.set(view, keyProcessor)
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "gamepads") {
            result.success(GamepadStreamHandler.allGamepadInfoDictionaries())
        } else {
            result.notImplemented()
        }
    }
}
