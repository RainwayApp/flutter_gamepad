## 0.0.1

Initial release.

* Basic implementation for iOS.
* Events are sent to Flutter over `FlutterGamepad.eventStream` whenever controllers are connected and disconnected, and whenever their buttons or sticks change state.
* Supports MFi, and (in iOS 13+) Xbox and DualShock controllers.
