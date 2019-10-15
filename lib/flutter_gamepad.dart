import 'dart:async';

import 'package:flutter/services.dart';
import 'gamepad_event.dart';

export 'gamepad_event.dart';

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
  static const EventChannel _eventChannel =
      const EventChannel('com.rainway.flutter_gamepad/events');

  static Stream<GamepadEvent> _eventStream;

  /// A stream of [GamepadEvent]s.
  static Stream<GamepadEvent> get eventStream {
    _eventStream ??= _eventChannel.receiveBroadcastStream().map((x) => GamepadEvent.decode(x));
    return _eventStream;
  }
}
