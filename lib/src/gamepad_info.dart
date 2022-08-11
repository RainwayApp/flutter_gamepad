import 'package:flutter/foundation.dart';

/// Info about a gamepad that was connected or disconnected to the app.
class GamepadInfo {
  /// A vendor supplied name. May be nil, and is not guaranteed to be unique. This should not be used as a key in a dictionary,
  /// but simply as a way to present some basic information about the controller in testing or to the user.
  /// For telling gamepads apart, prefer [id] (and the gamepadId field on GamepadEvents).
  final String vendorName;

  /// (iOS 13+ only.) The product category the controller belongs to. This is useful for setting appropriate UI elements based
  /// on what type of controller is connected.
  /// For telling gamepads apart, prefer [id] (and the gamepadId field on GamepadEvents).
  final String productCategory;

  /// A controller may be form fitting or otherwise closely attached to the device. This closeness to other inputs on the device
  /// may suggest that interaction with the device may use other inputs easily. This is presented to developers to allow them to
  /// make informed decisions about UI and interactions to choose for their game in this situation.
  final bool isAttachedToDevice;

  /// This gamepad's unique identifying number.
  ///
  /// Note that this value does not persist beyond disconnect and reconnect events: when the controller reconnects, its [id] will change.
  /// The only guarantee is that the number is unique among all controllers currently connected.
  final int id;

  const GamepadInfo({
    required this.vendorName,
    required this.productCategory,
    required this.isAttachedToDevice,
    required this.id,
  });

  /// Decode a GamepadInfo object from a platform channel message.
  factory GamepadInfo.decode(dynamic message) {
    return GamepadInfo(
      vendorName: message['vendorName'],
      productCategory: message['productCategory'],
      isAttachedToDevice: message['isAttachedToDevice'],
      id: message['id'],
    );
  }
}
