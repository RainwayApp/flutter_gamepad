package com.example.flutter_gamepad

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import io.flutter.plugin.common.EventChannel
import kotlin.math.absoluteValue
import android.content.res.Configuration
import android.os.Handler
import com.example.flutter_gamepad.GamepadState
import java.util.Date
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.timer


val KeyEvent.isFromGamepad: Boolean
    get() = (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD

val MotionEvent.isJoystickMove: Boolean
    get() = Build.VERSION.SDK_INT >= 18 && isFromSource(InputDevice.SOURCE_CLASS_JOYSTICK) && action == MotionEvent.ACTION_MOVE

/**
 * Axis values with absolute value less than this constant are mapped to 0.
 */
const val AXIS_THRESHOLD: Float = 0.01f

/**
 * A "smart" snapshot of the current state (axis positions) for one gamepad.
 *
 * It knows how to send events to Flutter, over the given eventSink.
 *
 * This class is also in charge of "forgetting" the exact position of an axis that's sufficiently
 * close to 0. Such positions are snapped exactly to 0, so that not every slight spurious movement
 * of a controller stick causes events to be fired.
 *
 * Sometimes, the OS might send repeating "down" events when a gamepad button is held down.
 * We use a cache of button states to filter such repeat events out.
 *
 * If a KEYCODE_DPAD_LEFT key-down event comes in, we obey it.
 * But some gamepads report D-pad inputs on the AXIS_HAT_X and AXIS_HAT_Y axes.
 * We treat HAT_X and HAT_Y as "axes that can also be steered by key events."
 *
 * ---
 *
 * This class further contains a piece of Android TV specific behavior called the "BACK hack":
 *
 * On Android TV, SELECT events (as created by the "options/select/back" button on the gamepad)
 * get "helpfully" transformed into BACK by some layer of the OS. Meanwhile, BACK
 * is already triggered as the "fallback" key event of the B button, as defined here:
 * https://android.googlesource.com/platform/frameworks/base/+/master/data/keyboards/Generic.kcm
 *
 * This means B button presses look like BUTTON_B + BACK occurring "simultaneously",
 * and "options" button presses look like just BACK events (with a different scan-code).
 *
 * This class does its best to figure out whether a BACK event occurred as part of a B press,
 * and should be ignored, or if it actually represents an "options" button press.
 * Doing so, it undoes this "helpful" transformation: an Android TV app will see events as normal.
 */
class GamepadState(val eventSink: EventChannel.EventSink, val deviceId: Int) {
    /** A map from axis numbers (e.g. MotionEvent.AXIS_HAT_X) to their floating-point readings. */
    private var axisValues = HashMap<Int, Float>()
    private var buttonValues = HashMap<Button, Float>()

    /** A scanCode that we know to represent this gamepad's B button. */
    var knownButtonBScanCode: Int? = null
    private var backHackTimer: TimerTask? = null
    private var lastButtonBEventTime: Date? = null

    private fun axisValue(axis: Int): Float {
        val value = axisValues[axis] ?: 0.0f
        axisValues[axis] = value
        return value
    }

    private fun buttonValue(button: Button): Float {
        val value = buttonValues[button] ?: 0.0f
        buttonValues[button] = value
        return value
    }

    /** When a GamepadState is created, send a `gamepadConnected` event to Flutter.
     *
     * Android doesn't have an event for "a new controller was connected", but the lazy and
     * memoized nature of `GamepadCache` means this will happen once, the first time any gamepad
     * event for a new controller is seen.
     */
    init {
        sendEvent("event" to "gamepadConnected",
                "gamepadInfo" to gamepadInfoDictionary(InputDevice.getDevice(deviceId)))
    }

    private fun sendEvent(vararg pairs: Pair<String, Any>) {
        eventSink.success(hashMapOf("gamepadId" to deviceId, *pairs))
    }

    private fun normalize(value: Float): Float {
        return if (value.absoluteValue < AXIS_THRESHOLD) 0.0f else value

    }

    fun handleThumbstick(event: MotionEvent, thumbstick: Thumbstick, xAxis: Int, yAxis: Int) {
        val x = normalize(event.getAxisValue(xAxis))
        val y = normalize(event.getAxisValue(yAxis))
        if (axisValue(xAxis) != x || axisValue(yAxis) != y) {
            sendEvent("event" to "thumbstick", "thumbstick" to thumbstick.ordinal, "x" to x, "y" to y)
        }
        axisValues[xAxis] = x
        axisValues[yAxis] = y
    }

    fun handleAxisButton(event: MotionEvent, button: Button, axis: Int) {
        val value = normalize(event.getAxisValue(axis))
        if (axisValue(axis) != value) {
            sendButtonEvent(button.ordinal, value)
        }
        axisValues[axis] = value
    }

    fun handleHatAxis(event: MotionEvent, buttonMinus: Button, buttonPlus: Button, axis: Int) {
        val new = normalize(event.getAxisValue(axis))
        val old = axisValue(axis)

        val wasPlus = old > 0.0f
        val nowPlus = new > 0.0f
        if (!wasPlus && nowPlus) {
            sendButtonEvent(buttonPlus.ordinal, 1.0f)
        } else if (wasPlus && !nowPlus) {
            sendButtonEvent(buttonPlus.ordinal, 0.0f)
        }

        val wasMinus = old < 0.0f
        val nowMinus = new < 0.0f
        if (!wasMinus && nowMinus) {
            sendButtonEvent(buttonMinus.ordinal, 1.0f)
        } else if (wasMinus && !nowMinus) {
            sendButtonEvent(buttonMinus.ordinal, 0.0f)
        }

        axisValues[axis] = new
    }

    private fun sendButtonEvent(button: Int, value: Float) {
        val pressed = value > AXIS_THRESHOLD
        sendEvent("event" to "button", "button" to button, "value" to value, "pressed" to pressed)
    }

    fun buttonDown(button: Button) {
        // D-pad buttons affect the hat axis values:
        if (button == Button.DpadLeft) axisValues[MotionEvent.AXIS_HAT_X] = -1.0f
        if (button == Button.DpadRight) axisValues[MotionEvent.AXIS_HAT_X] = +1.0f
        if (button == Button.DpadUp) axisValues[MotionEvent.AXIS_HAT_Y] = -1.0f
        if (button == Button.DpadDown) axisValues[MotionEvent.AXIS_HAT_Y] = +1.0f

        if (buttonValue(button) != 1.0f) {
            sendButtonEvent(button.ordinal, 1.0f)
        }
        buttonValues[button] = 1.0f
    }

    fun buttonUp(button: Button) {
        // D-pad buttons affect the hat axis values:
        if (button == Button.DpadLeft || button == Button.DpadRight) axisValues[MotionEvent.AXIS_HAT_X] = 0.0f
        if (button == Button.DpadUp || button == Button.DpadDown) axisValues[MotionEvent.AXIS_HAT_Y] = 0.0f

        if (buttonValue(button) != 0.0f) {
            sendButtonEvent(button.ordinal, 0.0f)
        }
        buttonValues[button] = 0.0f
    }

    fun processButtonAction(action: Int, button: Button) {
        if (action == KeyEvent.ACTION_DOWN) {
            buttonDown(button)
        } else if (action == KeyEvent.ACTION_UP) {
            buttonUp(button)
        }
    }

    fun processButtonEvent(keyEvent: KeyEvent, button: Button) {
        if (button == Button.B) {
            knownButtonBScanCode = knownButtonBScanCode ?: keyEvent.scanCode
            lastButtonBEventTime = Date()
        }
        processButtonAction(keyEvent.action, button)
    }

    fun investigateBackEvent(action: Int) {
        // We got a BACK button press on Android TV, and we'd like to figure out
        // if it occurred or is about to occur simultaneously with a B button press.

        // We consider presses within this millisecond threshold to be "simultaneous":
        val threshold = 50L

        // We wait that long, and then check if B was pressed in the [now - threshold, now + threshold] interval.
        backHackTimer?.cancel()
        Handler().postDelayed({
            val lastB = lastButtonBEventTime
            if (lastB != null && lastB.time > Date().time - 2L * threshold) {
                // OK, a B press happened simultaneously with this BACK press.
                // That means the BACK is just the "fallback" part of the B press
                // and we shouldn't pretend the options button was pressed.
                // We do nothing at all.
            } else {
                // This BACK press is not from the B button.
                // We deduce that Android TV must've turned SELECT into BACK here,
                // and we trigger an "options" button press to undo this mapping.
                processButtonAction(action, Button.Options)
            }
        }, threshold)
    }
}

/**
 * A GamepadCache is fed gamepad data from the OS. It remembers the current position
 * of each gamepad's joysticks and handles changes to the EventSink passed in on creation.
 */
class GamepadCache(val eventSink: EventChannel.EventSink) {
    val gamepadStates = HashMap<Int, GamepadState>()

    private fun gamepadState(deviceId: Int): GamepadState {
        val state = gamepadStates[deviceId] ?: GamepadState(eventSink, deviceId)
        gamepadStates[deviceId] = state
        return state
    }

    /**
     * Process a generic motion Android event. Returns true if the event was handled successfully,
     * or false if it should be bubbled up.
     */
    fun processMotionEvent(event: MotionEvent): Boolean {
        if (event.isJoystickMove) {
            val state = gamepadState(event.deviceId)
            state.handleThumbstick(event, Thumbstick.Left, MotionEvent.AXIS_X, MotionEvent.AXIS_Y)
            state.handleThumbstick(event, Thumbstick.Right, MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ)
            state.handleAxisButton(event, Button.LeftTrigger, MotionEvent.AXIS_BRAKE)
            state.handleAxisButton(event, Button.RightTrigger, MotionEvent.AXIS_GAS)
            state.handleHatAxis(event, Button.DpadLeft, Button.DpadRight, MotionEvent.AXIS_HAT_X)
            state.handleHatAxis(event, Button.DpadUp, Button.DpadDown, MotionEvent.AXIS_HAT_Y)
            return true
        }
        return false
    }

    /**
     * Process an Android key event. Returns true if the event was handled successfully,
     * or false if it should be bubbled up.
     */
    fun processKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isFromGamepad) return false
        if (keyEvent.repeatCount > 1) return true
        val button = buttonMap[keyEvent.keyCode]
        val pad = gamepadState(keyEvent.deviceId)

        // Treat BACK keycodes specially on Android. See this class's documentation.
        if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK && FlutterGamepadPlugin.isTv == true) {
            if (pad.knownButtonBScanCode == null) {
                // If we don't yet know the B button scan-code, use a timer-based method
                // to tell B presses and "options" presses apart.
                pad.investigateBackEvent(keyEvent.action)
            } else if (keyEvent.scanCode != pad.knownButtonBScanCode) {
                // If we do know it, and this one isn't that, then it's "options" for sure.
                pad.processButtonAction(keyEvent.action, Button.Options)
            }
        }

        // If we don't recognize this as a gamepad button, it must be something like
        // KEYCODE_DEL or KEYCODE_SELECT that gets sent as a "fallback" along with gamepad events.
        // We intentionally silence these and trust the app to properly handle gamepad events.
        // (By "silence", I mean "return `true` so they don't propagate and reach Flutter".)
        if (button == null) {
            return true
        }

        // Process a normal gamepad button and return true to say we processed it.
        pad.processButtonEvent(keyEvent, button)
        return true
    }

    /**
     * Create a gamepad state object if necessary, but do nothing with it.
     * If a new gamepad state object is created, this sends a "gamepad connected" event to Flutter.
     */
    fun touchGamepad(deviceId: Int) {
        gamepadState(deviceId)
    }
}