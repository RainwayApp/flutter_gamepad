# flutter_gamepad

A platform library for listening to hardware gamepads (game controllers) from Flutter.

**Currently supports iOS only. Android coming soon!**

## Features

* Any widget can listen to a GamepadEvent broadcast stream. It fires "connect", "disconnect", "button input" and "stick input" events.
* Supports fractional button values, such as those reported by the left and right trigger buttons on most gamepads.
* Supports iOS 13+, as well as older versions of iOS.
* Supports multiple simultaneous gamepads. Events are tagged with an ID you can tell gamepads apart by.