package com.example.flutter_gamepad

import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import io.flutter.plugin.common.EventChannel
import kotlin.math.absoluteValue

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
 * ## Why only keep axis values?
 *
 * We actually don't need to remember button positions, because we get e.g. "A down" and "A up"
 * events that we can always trust for those. But a motion event doesn't tell us which sticks
 * or axes have changed, and this class handles that.
 *
 * The exception is the D-pad. If a KEYCODE_DPAD_LEFT key-down event comes in, we obey it.
 * But some gamepads report D-pad inputs on the AXIS_HAT_X and AXIS_HAT_Y axes.
 * We treat HAT_X and HAT_Y as "axes that can also be steered by key events."
 */
class GamepadState(val eventSink: EventChannel.EventSink, val deviceId: Int) {
    /** A map from axis numbers (e.g. MotionEvent.AXIS_HAT_X) to their floating-point readings. */
    private var axisValues = HashMap<Int, Float>()

    private fun axisValue(axis: Int): Float {
        val value = axisValues[axis] ?: 0.0f
        axisValues[axis] = value
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

        sendButtonEvent(button.ordinal, 1.0f)
    }

    fun buttonUp(button: Button) {
        // D-pad buttons affect the hat axis values:
        if (button == Button.DpadLeft || button == Button.DpadRight) axisValues[MotionEvent.AXIS_HAT_X] = 0.0f
        if (button == Button.DpadUp || button == Button.DpadDown) axisValues[MotionEvent.AXIS_HAT_Y] = 0.0f

        sendButtonEvent(button.ordinal, 0.0f)
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
     * Process a "key down" Android event. Returns true if the event was handled successfully,
     * or false if it should be bubbled up.
     */
    fun processKeyDownEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isFromGamepad) return false
        val button = buttonMap[keyEvent.keyCode] ?: return false
        gamepadState(keyEvent.deviceId).buttonDown(button)
        return true
    }

    /**
     * Process a "key up" Android event. Returns true if the event was handled successfully,
     * or false if it should be bubbled up.
     */
    fun processKeyUpEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isFromGamepad) return false
        val button = buttonMap[keyEvent.keyCode] ?: return false
        gamepadState(keyEvent.deviceId).buttonUp(button)
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