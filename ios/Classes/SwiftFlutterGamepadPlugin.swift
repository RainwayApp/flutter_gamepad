import Flutter
import UIKit
import GameController

enum Button : Int {
  case a = 0
  case b
  case x
  case y
  case dpadUp
  case dpadDown
  case dpadLeft
  case dpadRight
  case menu
  case options
  case leftThumbstickButton
  case rightThumbstickButton
  case leftShoulder
  case rightShoulder
  case leftTrigger
  case rightTrigger
}

enum Thumbstick : Int {
  case left = 0
  case right
}

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
    result["id"] = gamepad.hash;
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
    func buttonHandler(_ button: Button) -> GCControllerButtonValueChangedHandler {
      return { (_, value, pressed) in eventSink(["event": "button", "gamepadId": gamepad.hash, "button": button.rawValue, "value": value, "pressed": pressed]) }
    }
    func thumbstickHandler(_ thumbstick: Thumbstick) -> GCControllerDirectionPadValueChangedHandler {
      // We flip the Y-axis direction here: our library has Y+ = down in line with Flutter's coordinate system, but iOS has Y+ = up.
      return { (_, x, y) in eventSink(["event": "thumbstick", "gamepadId": gamepad.hash, "thumbstick": thumbstick.rawValue, "x": x, "y": -y]) }
    }
    eventSink(["event": "gamepadConnected", "gamepadId": gamepad.hash, "gamepadInfo": gamepadInfoDictionary(gamepad: gamepad)])
    gamepad.buttonA.valueChangedHandler = buttonHandler(.a)
    gamepad.buttonB.valueChangedHandler = buttonHandler(.b)
    gamepad.buttonX.valueChangedHandler = buttonHandler(.x)
    gamepad.buttonY.valueChangedHandler = buttonHandler(.y)
    if #available(iOS 13.0, *) {
      gamepad.buttonOptions?.valueChangedHandler = buttonHandler(.options)
        gamepad.buttonMenu.valueChangedHandler = buttonHandler(.menu)
    } else {
      gamepad.controller?.controllerPausedHandler = { (_) in
        eventSink(["event": "button", "gamepadId": gamepad.hash, "button": Button.menu.rawValue, "value": 1.0])
        eventSink(["event": "button", "gamepadId": gamepad.hash, "button": Button.menu.rawValue, "value": 0.0])
      }
    }
          gamepad.leftShoulder.valueChangedHandler = buttonHandler(.leftShoulder)
          gamepad.leftTrigger.valueChangedHandler = buttonHandler(.leftTrigger)
          gamepad.rightShoulder.valueChangedHandler = buttonHandler(.rightShoulder)
          gamepad.rightTrigger.valueChangedHandler = buttonHandler(.rightTrigger)
          gamepad.dpad.up.valueChangedHandler = buttonHandler(.dpadUp)
          gamepad.dpad.down.valueChangedHandler = buttonHandler(.dpadDown)

          gamepad.dpad.left.valueChangedHandler = buttonHandler(.dpadLeft)
          gamepad.dpad.right.valueChangedHandler = buttonHandler(.dpadRight)
    if #available(iOS 12.1, *) {
      gamepad.leftThumbstickButton?.valueChangedHandler = buttonHandler(.leftThumbstickButton)
      gamepad.rightThumbstickButton?.valueChangedHandler = buttonHandler(.rightThumbstickButton)
    }
    gamepad.leftThumbstick.valueChangedHandler = thumbstickHandler(.left)
    gamepad.rightThumbstick.valueChangedHandler = thumbstickHandler(.right)
    gamepads.append(gamepad);
  }

  private func gamepadDisconnected(gamepad: GCExtendedGamepad) {
    guard let eventSink = eventSink else { return }
    eventSink(["event": "gamepadDisconnected", "gamepadId": gamepad.hash, "gamepadInfo": gamepadInfoDictionary(gamepad: gamepad)])
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
