import 'dart:async';

import 'package:flutter/services.dart';
import 'src/gamepad_event.dart';
import 'src/gamepad_info.dart';

export 'src/gamepad_event.dart';
export 'src/gamepad_info.dart';

/// The public API for the flutter_gamepad library.
///
/// Usage example:
/// ```
/// class _MyWidgetState extends State<MyWidget> {
///   StreamSubscription<GamepadEvent> _gamepadEventSubscription;
///
///   @override
///   void initState() {
///     super.initState();
///     _gamepadEventSubscription = FlutterGamepad.eventStream.listen(onGamepadEvent);
///   }
///
///   void dispose() {
///     _gamepadEventSubscription.cancel();
///     super.dispose();
///   }
///
///   /// Handle a flutter_gamepad [GamepadEvent].
///   void onGamepadEvent(GamepadEvent e) {
///     if (e is GamepadConnectedEvent) {
///       ...
///     } else if (e is GamepadDisconnectedEvent) {
///       ...
///     } else if (e is GamepadButtonEvent) {
///       ...
///     } else if (e is GamepadThumbstickEvent) {
///       ...
///     } else {
///       throw ArgumentError('Unknown event: $e');
///     }
///   }
/// }
/// ```
class FlutterGamepad {
  static const MethodChannel _methodChannel =
      const MethodChannel('com.rainway.flutter_gamepad/methods');

  static const EventChannel _eventChannel =
      const EventChannel('com.rainway.flutter_gamepad/events');

  static Stream<GamepadEvent> _eventStream;

  /// A stream of [GamepadEvent]s.
  static Stream<GamepadEvent> get eventStream {
    _eventStream ??= _eventChannel.receiveBroadcastStream().map((x) => GamepadEvent.decode(x));
    return _eventStream;
  }

  /// Return info about all currently connected controllers.
  static Future<List<GamepadInfo>> gamepads() async {
    final result = await _methodChannel.invokeListMethod<dynamic>('gamepads');
    return result.map((x) => GamepadInfo.decode(x)).toList();
  }

  /// Older versions of FlutterGamepad would generate "extra" events
  /// in addition to the gamepad events (for example pressing the B-button
  /// would also generate a BACK event). This newer version of the plugin
  /// does not have that issue, but for apps that require this, use this
  /// function to restore that "extra event" behavior.
  static void enableAndroidBackwardsCompatibilityMode() {
    _methodChannel.invokeListMethod<dynamic>('enableAndroidBackwardsCompatibilityMode');
  }

  static void disableAndroidBackwardsCompatibilityMode() {
    _methodChannel.invokeListMethod<dynamic>('disableAndroidBackwardsCompatibilityMode');
  }

  static void enableDebugMode() {
    _methodChannel.invokeListMethod<dynamic>('enableDebugMode');
  }

  static void disableDebugMode() {
    _methodChannel.invokeListMethod<dynamic>('disableDebugMode');
  }
}
