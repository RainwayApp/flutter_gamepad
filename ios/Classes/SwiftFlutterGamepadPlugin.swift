import Flutter
import UIKit
import GameController

@objc class GamepadStreamHandler: NSObject, FlutterStreamHandler {
  private var eventSink: FlutterEventSink?
  private var gamepads: [GCExtendedGamepad] = []

  private func gamepadInfoDictionary(gamepad: GCExtendedGamepad) -> [String: Any] {
    var result: [String: Any] = [:];
    result["isAttachedToDevice"] = gamepad.controller?.isAttachedToDevice as Any;
    // result["playerIndex"] = gamepad.controller?.playerIndex.rawValue as Any;
    result["vendorName"] = gamepad.controller?.vendorName as Any;
    if #available(iOS 13.0, *) {
      result["productCategory"] = gamepad.controller?.productCategory as Any;
      // result["isSnapshot"] = gamepad.controller?.isSnapshot as Any;
    }
    return result
  }

  public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
    eventSink = events

    // Initial read:
    for gc in GCController.controllers() {
      if let eg = gc.extendedGamepad { self.gamepadConnected(gamepad: eg) }
    }

    // Then start listening:
    NotificationCenter.default.addObserver(forName: .GCControllerDidConnect, object: nil, queue: .main) { (note) in
      guard let gc = note.object as? GCController else {
        print("GCControllerDidConnect: non-controller object?")
        return
      }
      guard let eg = gc.extendedGamepad else {
        print("GCControllerDidConnect: Ignoring non-extendedGamepad controller; vendorName is \(String(describing: gc.vendorName)).")
        return
      }
      print("GCControllerDidConnect: Adding extendedGamepad with vendorName \(String(describing: gc.vendorName)).")
      self.gamepadConnected(gamepad: eg)
    }

    NotificationCenter.default.addObserver(forName: .GCControllerDidDisconnect, object: nil, queue: .main) { (note) in
      guard let gc = note.object as? GCController else {
        print("GCControllerDidDisconnect: non-controller object?")
        return
      }
      guard let eg = gc.extendedGamepad else {
        print("GCControllerDidDisconnect: Ignoring non-extendedGamepad controller; vendorName is \(String(describing: gc.vendorName)).")
        return
      }
      print("GCControllerDidDisconnect: Removing extendedGamepad with vendorName \(String(describing: gc.vendorName)).")
      self.gamepadDisconnected(gamepad: eg)
    }

    GCController.startWirelessControllerDiscovery(completionHandler: {})
    print("Started wireless controller discovery")
    return nil
  }

  public func onCancel(withArguments arguments: Any?) -> FlutterError? {
    GCController.stopWirelessControllerDiscovery()
    print("Stopped wireless controller discovery")
    NotificationCenter.default.removeObserver(self)
    eventSink = nil
    return nil
  }

  private func gamepadConnected(gamepad: GCExtendedGamepad) {
    guard let eventSink = eventSink else { return }
    eventSink(["event": "gamepadConnected", "gamepadInfo": gamepadInfoDictionary(gamepad: gamepad)])
    gamepad.buttonA.valueChangedHandler = { (_, _, b) in eventSink(["event": "button", "button": "buttonA", "value": b]) }
    gamepads.append(gamepad);
  }

  private func gamepadDisconnected(gamepad: GCExtendedGamepad) {
    guard let eventSink = eventSink else { return }
    eventSink(["event": "gamepadDisconnected", "gamepadInfo": gamepadInfoDictionary(gamepad: gamepad)])
    gamepads.removeAll { $0 == gamepad };
  }
}

public class SwiftFlutterGamepadPlugin: NSObject, FlutterPlugin {
  var gamepadStreamHandler: GamepadStreamHandler = GamepadStreamHandler()

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "com.rainway.flutter_gamepad/methods", binaryMessenger: registrar.messenger())
    let instance = SwiftFlutterGamepadPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)

    let eventChannel = FlutterEventChannel(name: "com.rainway.flutter_gamepad/events", binaryMessenger: registrar.messenger())
    eventChannel.setStreamHandler(instance.gamepadStreamHandler)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {

    result("iOS " + UIDevice.current.systemVersion)
  }
}
