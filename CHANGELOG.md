## 0.2.1

- Add "pressed" key to button events

## 0.2.0

- Bump Kotlin version

## 0.1.0

Add a `gamepads` method for retrieving current gamepad info.

## 0.0.5

Remove some stray debugging code.

## 0.0.4

Run flutter format and add a stripped-down example to the library README.

## 0.0.3

Hide some internals and write a README for the example.

## 0.0.2

Write a README.

## 0.0.1

Initial release.

* Basic implementation for iOS.
* Events are sent to Flutter over `FlutterGamepad.eventStream` whenever controllers are connected and disconnected, and whenever their buttons or sticks change state.
* Supports MFi, and (in iOS 13+) Xbox and DualShock controllers.
