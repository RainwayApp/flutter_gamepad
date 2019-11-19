package com.example.flutter_gamepad

import android.view.KeyEvent

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
