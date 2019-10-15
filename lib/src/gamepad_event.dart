import 'package:flutter/foundation.dart';

import 'gamepad_info.dart';

/// A button on a gamepad.
enum Button {
  a,
  b,
  x,
  y,
  dpadUp,
  dpadDown,
  dpadLeft,
  dpadRight,
  menu,
  options,
  leftThumbstickButton,
  rightThumbstickButton,
  leftShoulder,
  rightShoulder,
  leftTrigger,
  rightTrigger,
}

/// A thumbstick on a gamepad.
enum Thumbstick {
  left,
  right,
}

/// An event originating from a gamepad.
///
/// This is an abstract class with the following concrete implementing classes:
///
/// * [GamepadConnectedEvent], when a gamepad is connected to the device.
/// * [GamepadDisconnectedEvent], when a gamepad is disconnected from the device.
/// * [GamepadButtonEvent], when a button or analog trigger's state changes.
/// * [GamepadThumbstickEvent], when a 2D analog thumbstick's state changes.
///
/// To consume one of these events, use code like:
///
/// ```
/// void onGamepadEvent(GamepadEvent e) {
///   if (e is GamepadConnectedEvent) {
///     ...
///   } else if (e is GamepadDisconnectedEvent) {
///     ...
///   } else if (e is GamepadButtonEvent) {
///     ...
///   } else if (e is GamepadThumbstickEvent) {
///     ...
///   } else {
///     throw ArgumentError('Unknown event: $e');
///   }
/// }
/// ```
///
abstract class GamepadEvent {
  factory GamepadEvent.decode(dynamic message) {
    print('decode: $message');
    if (message['event'] == 'gamepadConnected') {
      return GamepadConnectedEvent(
        gamepadId: message['gamepadId'],
        gamepadInfo: GamepadInfo.decode(message['gamepadInfo']),
      );
    } else if (message['event'] == 'gamepadDisconnected') {
      return GamepadDisconnectedEvent(
        gamepadId: message['gamepadId'],
        gamepadInfo: GamepadInfo.decode(message['gamepadInfo']),
      );
    } else if (message['event'] == 'button') {
      return GamepadButtonEvent(
        gamepadId: message['gamepadId'],
        button: Button.values[message['button']],
        value: message['value'],
        pressed: message['pressed'],
      );
    } else if (message['event'] == 'thumbstick') {
      return GamepadThumbstickEvent(
        gamepadId: message['gamepadId'],
        thumbstick: Thumbstick.values[message['thumbstick']],
        x: message['x'],
        y: message['y'],
      );
    } else {
      throw ArgumentError("Unknown flutter_gamepad event ${message['event']}");
    }
  }
}

/// A gamepad event that fires when a controller is connected.
class GamepadConnectedEvent implements GamepadEvent {
  /// A number uniquely identifying the gamepad that was newly connected.
  ///
  /// Note that this value does not persist beyond disconnect and reconnect events: when the controller reconnects, its [id] will change.
  /// The only guarantee is that the number is unique among all controllers currently connected.
  final int gamepadId;

  /// A record containing further information about this gamepad.
  final GamepadInfo gamepadInfo;

  const GamepadConnectedEvent(
      {@required this.gamepadId, @required this.gamepadInfo});
}

/// A gamepad event that fires when a controller is disconnected.
class GamepadDisconnectedEvent implements GamepadEvent {
  /// A number uniquely identifying the gamepad that was disconnected.
  ///
  /// Note that this value does not persist beyond disconnect and reconnect events: when the controller reconnects, its [id] will change.
  /// The only guarantee is that the number is unique among all controllers currently connected.
  final int gamepadId;

  /// A record containing further information about this gamepad.
  final GamepadInfo gamepadInfo;

  const GamepadDisconnectedEvent(
      {@required this.gamepadId, @required this.gamepadInfo});
}

/// A gamepad event describing a button state change.
class GamepadButtonEvent implements GamepadEvent {
  /// A number uniquely identifying the gamepad this button state change pertains to.
  final int gamepadId;

  /// Which button's state changed?
  final Button button;

  /// How far is the button depressed? (0.0 for not at all, 1.0 for maximally.)
  /// Only analog buttons like triggers will return values other than 0.0 and 1.0.
  final double value;

  /// Is the button pressed? (This corresponds more or less with a [value] greater than 0.0 -- not accounting for possible noise.)
  final bool pressed;

  const GamepadButtonEvent({
    @required this.gamepadId,
    @required this.button,
    @required this.value,
    @required this.pressed,
  });
}

/// A gamepad event describing a thumbstick state change.
class GamepadThumbstickEvent implements GamepadEvent {
  /// A number uniquely identifying the gamepad this thumbstick state change pertains to.
  final int gamepadId;

  /// Which thumbstick was moved?
  final Thumbstick thumbstick;

  /// The thumbstick's new X coordinate, between -1 and +1 (inclusive).
  /// (-1 is left, +1 is right.)
  final double x;

  /// The thumbstick's new Y coordinate, between -1 and +1 (inclusive).
  /// (-1 is up, +1 is down.)
  final double y;

  const GamepadThumbstickEvent({
    @required this.gamepadId,
    @required this.thumbstick,
    @required this.x,
    @required this.y,
  });
}
