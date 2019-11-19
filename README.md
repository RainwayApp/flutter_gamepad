# flutter_gamepad

A platform library for listening to hardware gamepads (game controllers) from Flutter.

## Features

* `FlutterGamepad.gamepads()` returns info about all currently connected gamepads.
* `FlutterGamepad.eventStream` reports gamepad events.
* Fractional button values, such as those reported by the left and right trigger buttons on most gamepads, are supported.
* Supports iOS 13+, as well as older versions of iOS.
* Supports Android.
* Supports multiple simultaneous gamepads. Events are tagged with an ID you can tell gamepads apart by.

## Caveats

* On Android, the B button seems to trigger a "back" action. You'll want to have a `WillPopScope` on your game scaffold to prevent this.
* On Android, "disconnect" events are never fired. "Connect" events are fired initially when listening to the stream, and thereafter on the first input of any new gamepads. This differs from the iOS behavior, where a "connect" event is sent when the connection is established even if no buttons have been pressed.

## Example

```dart
import 'package:flutter_gamepad/flutter_gamepad.dart';

class _MyWidgetState extends State<MyWidget> {
  StreamSubscription<GamepadEvent> _gamepadEventSubscription;

  @override
  void initState() {
    super.initState();
    _gamepadEventSubscription = FlutterGamepad.eventStream.listen(onGamepadEvent);
  }

  void dispose() { _gamepadEventSubscription.cancel(); super.dispose(); }

  void onGamepadEvent(GamepadEvent e) {
    if (e is GamepadConnectedEvent) {
      // ...
    } else if (e is GamepadDisconnectedEvent) {
      // ...
    } else if (e is GamepadButtonEvent) {
      // ...
    } else if (e is GamepadThumbstickEvent) {
      // ...
    } else throw ArgumentError('Unknown event: $e');
  }
}
```
