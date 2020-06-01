# flutter_gamepad

A platform library for listening to hardware gamepads (game controllers) from Flutter.

## Features

* `FlutterGamepad.gamepads()` returns info about all currently connected gamepads.
* `FlutterGamepad.eventStream` reports gamepad events. (See example)
* Fractional button values, such as those reported by the left and right trigger buttons on most gamepads, are supported.
* Supports iOS 13+, as well as older versions of iOS.
* Supports Android.
* Supports multiple simultaneous gamepads. Events are tagged with an ID you can tell gamepads apart by.

## Caveats

### Android

* "Disconnect" events are never fired on Android. "Connect" events are fired initially for already-connected gamepads when listening to the stream, and thereafter on the first input of any _new_ gamepads. This differs from the iOS behavior for non-inital "connect" events: on that platform, a "connect" event can arrive for a gamepad before any button has been pressed.
* Gamepad button events are just a special kind of key event. This plugin currently patches the FlutterViewController to prevent key-events that originate from a gamepad from reaching the default Flutter handler for key events. This allows you to handle gamepad input all in one place and not getting duplicate key events in your keyboard handlers.

  This has a notable consequence: normally, Android sends "SELECT" events along with "BUTTON_A" events that you might expect on the Flutter side to implement key-based focus navigation. This plugin silences those, so you'll have to listen to the FlutterGamepad event stream for A presses and make navigation work based off of those.

* Despite all that, the B button still seems to trigger a "back" action in Flutter. You'll want to have a `WillPopScope` on your game scaffold to prevent this.
* Android TV exhibits a weird behavior where it transforms presses of the "select" button (the leftmost small button on the face of the gamepad) into `KEYCODE_BACK` key events. (Normally they are `KEYCODE_BUTTON_SELECT`.) This plugin does some work to undo this transformation.

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
