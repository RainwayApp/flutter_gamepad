import 'dart:async';

import 'package:flutter/services.dart';

class GamepadEvent {
  dynamic underlying;
  GamepadEvent(this.underlying);
}

GamepadEvent _decodeGamepadEvent(dynamic d) {
  return GamepadEvent(d);
}

class FlutterGamepad {
  static const MethodChannel _methodChannel =
      const MethodChannel('com.rainway.flutter_gamepad/methods');

  static const EventChannel _eventChannel =
      const EventChannel('com.rainway.flutter_gamepad/events');

  static Stream<GamepadEvent> _eventStream;

  static Stream<GamepadEvent> get eventStream {
    _eventStream ??= _eventChannel.receiveBroadcastStream().map(_decodeGamepadEvent);
    return _eventStream;
  }

  static Future<String> get platformVersion async {
    final String version = await _methodChannel.invokeMethod('getPlatformVersion');
    return version;
  }
}
