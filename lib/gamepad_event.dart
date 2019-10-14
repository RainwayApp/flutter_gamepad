import 'package:flutter/foundation.dart';

import 'gamepad_info.dart';

abstract class GamepadEvent {
  factory GamepadEvent.decode(dynamic message) {
    if (message['event'] == 'gamepadConnected') {
      return GamepadConnectedEvent(
        gamepadInfo: GamepadInfo.decode(message['gamepadInfo']),
      );
    } else if (message['event'] == 'gamepadDisconnected') {
      return GamepadDisconnectedEvent(
        gamepadInfo: GamepadInfo.decode(message['gamepadInfo']),
      );
    } else {
      throw ArgumentError("Unknown flutter_gamepad event ${message['event']}");
    }
  }
}

class GamepadConnectedEvent implements GamepadEvent {
  final GamepadInfo gamepadInfo;
  const GamepadConnectedEvent({@required this.gamepadInfo});
}

class GamepadDisconnectedEvent implements GamepadEvent {
  final GamepadInfo gamepadInfo;
  const GamepadDisconnectedEvent({@required this.gamepadInfo});
}
