package com.example.flutter_gamepad

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
import java.lang.reflect.Field

/**
 * An extension of Flutter's AndroidTouchProcessor that delegates MotionEvents to the GamepadStreamHandler.
 * (On Android, thumbstick events from a gamepad are a kind of MotionEvent.)
 */
class GamepadAndroidTouchProcessor(renderer: FlutterRenderer) : AndroidTouchProcessor(renderer) {
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return GamepadStreamHandler.processMotionEvent(event) || super.onGenericMotionEvent(event)
    }
}

/**
 * An extension of Flutter's AndroidKeyProcessor that delegates KeyEvents to the GamepadStreamHandler.
 * (On Android, button events from a gamepad are a kind of KeyEvent.)
 */
class GamepadAndroidKeyProcessor(keyEventChannel: KeyEventChannel, textInputPlugin: TextInputPlugin) : AndroidKeyProcessor(keyEventChannel, textInputPlugin) {
    override fun onKeyDown(keyEvent: KeyEvent) {
        val handled = GamepadStreamHandler.processKeyDownEvent(keyEvent)
        if (!handled) {
            super.onKeyDown(keyEvent)
        }
    }

    override fun onKeyUp(keyEvent: KeyEvent) {
        val handled = GamepadStreamHandler.processKeyUpEvent(keyEvent)
        if (!handled) {
            super.onKeyUp(keyEvent)
        }
    }
}

/**
 * The flutter_gamepad plugin class that is registered with the framework.
 */
class FlutterGamepadPlugin : MethodCallHandler {
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val methodChannel = MethodChannel(registrar.messenger(), "com.rainway.flutter_gamepad/methods")
            methodChannel.setMethodCallHandler(FlutterGamepadPlugin())
            val eventChannel = EventChannel(registrar.messenger(), "com.rainway.flutter_gamepad/events")
            eventChannel.setStreamHandler(GamepadStreamHandler)

            val view: FlutterView = registrar.view()
            fun viewField(name: String): Field {
                val field = FlutterView::class.java.getDeclaredField(name)
                field.isAccessible = true
                return field
            }

            // Hack: swap in a new AndroidTouchProcessor.
            val touchProcessorField = viewField("androidTouchProcessor")
            val rendererField = viewField("flutterRenderer")
            val renderer = rendererField.get(view) as FlutterRenderer
            val touchProcessor = GamepadAndroidTouchProcessor(renderer)
            touchProcessorField.set(view, touchProcessor)

            // Hack: swap in a new AndroidKeyProcessor.
            val keyProcessorField = viewField("androidKeyProcessor")
            val keyEventChannelField = viewField("keyEventChannel")
            val textInputPluginField = viewField("mTextInputPlugin")
            val keyEventChannel = keyEventChannelField.get(view) as KeyEventChannel
            val textInputPlugin = textInputPluginField.get(view) as TextInputPlugin
            val keyProcessor = GamepadAndroidKeyProcessor(keyEventChannel, textInputPlugin)
            keyProcessorField.set(view, keyProcessor)
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "gamepads") {
            result.success(allGamepadInfoDictionaries())
        } else {
            result.notImplemented()
        }
    }
}
