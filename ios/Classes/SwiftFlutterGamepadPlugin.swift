import Flutter
import UIKit
import GameController

@objc class GamepadStreamHandler: NSObject, FlutterStreamHandler {
  private var eventSink: FlutterEventSink?
  private var gamepads: [GCExtendedGamepad] = []

  public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
    eventSink = events

    // Initial read:
    for gc in GCController.controllers() {
      if let eg = gc.extendedGamepad { self.addGamepad(gamepad: eg) }
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
      self.addGamepad(gamepad: eg)
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
      self.removeGamepad(gamepad: eg)
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

  private func addGamepad(gamepad: GCExtendedGamepad) {
    guard let eventSink = eventSink else { return }
    eventSink(["event": "addGamepad", "vendorName": gamepad.controller?.vendorName])
    gamepad.buttonA.valueChangedHandler = { (_, _, b) in eventSink(b) }
    gamepads.append(gamepad);
  }

  private func removeGamepad(gamepad: GCExtendedGamepad) {
    guard let eventSink = eventSink else { return }
    eventSink(["event": "removeGamepad", "vendorName": gamepad.controller?.vendorName])
    gamepads.removeAll { $0 == gamepad };
  }

  @objc
  private func controllerDidDisconnect() {

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
