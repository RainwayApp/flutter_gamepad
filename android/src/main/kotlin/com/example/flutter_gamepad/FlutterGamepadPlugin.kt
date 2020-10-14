package com.example.flutter_gamepad

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
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
class GamepadAndroidTouchProcessor(renderer: FlutterRenderer) : AndroidTouchProcessor(renderer, true) {
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return GamepadStreamHandler.processMotionEvent(event) || super.onGenericMotionEvent(event)
    }
}


/**
 * An extension of Flutter's AndroidKeyProcessor that delegates KeyEvents to the GamepadStreamHandler.
 * (On Android, button events from a gamepad are a kind of KeyEvent.)
 */
class GamepadAndroidKeyProcessor(flutterView: FlutterView, keyEventChannel: KeyEventChannel, textInputPlugin: TextInputPlugin) : AndroidKeyProcessor(flutterView, keyEventChannel, textInputPlugin) {
    override fun onKeyDown(keyEvent: KeyEvent): Boolean {
        val handled = GamepadStreamHandler.processKeyEvent(keyEvent)
        if (!handled) {
            return super.onKeyDown(keyEvent)
        }
        return handled;
    }

    override fun onKeyUp(keyEvent: KeyEvent): Boolean {
        val handled = GamepadStreamHandler.processKeyEvent(keyEvent)
        if (!handled) {
            return super.onKeyUp(keyEvent)
        }
        return handled;
    }
}

/**
 * The flutter_gamepad plugin class that is registered with the framework.
 */
class FlutterGamepadPlugin : MethodCallHandler {
    companion object {
        var isTv: Boolean? = null
        var gamepadAndroidKeyProcessor: GamepadAndroidKeyProcessor? = null

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            // Detect if we are running on a TV.
            val context = registrar.context()
            val manager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            isTv = manager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

            // Set up Flutter platform channels.
            val methodChannel = MethodChannel(registrar.messenger(), "com.rainway.flutter_gamepad/methods")
            methodChannel.setMethodCallHandler(FlutterGamepadPlugin())
            val eventChannel = EventChannel(registrar.messenger(), "com.rainway.flutter_gamepad/events")
            eventChannel.setStreamHandler(GamepadStreamHandler)

            // Patch the FlutterView's touch and keypress processors:
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
            gamepadAndroidKeyProcessor = GamepadAndroidKeyProcessor(view, keyEventChannel, textInputPlugin)
            keyProcessorField.set(view, gamepadAndroidKeyProcessor)
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "gamepads") {
            result.success(allGamepadInfoDictionaries())
        } else if (call.method == "enableDebugMode") {
            GamepadStreamHandler.enableDebugMode(true)
        } else if (call.method == "disableDebugMode") {
            GamepadStreamHandler.enableDebugMode(false)
        } else {
            result.notImplemented()
        }
    }
}
