import 'dart:async';

import 'package:flutter/services.dart';
import 'gamepad_event.dart';

export 'gamepad_event.dart';

class FlutterGamepad {
  static const MethodChannel _methodChannel =
      const MethodChannel('com.rainway.flutter_gamepad/methods');

  static const EventChannel _eventChannel =
      const EventChannel('com.rainway.flutter_gamepad/events');

  static Stream<GamepadEvent> _eventStream;

  static Stream<GamepadEvent> get eventStream {
    _eventStream ??= _eventChannel.receiveBroadcastStream().map((x) => GamepadEvent.decode(x));
    return _eventStream;
  }

  static Future<String> get platformVersion async {
    final String version = await _methodChannel.invokeMethod('getPlatformVersion');
    return version;
  }
}
