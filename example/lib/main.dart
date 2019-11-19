import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter_gamepad/flutter_gamepad.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<GamepadEvent> _eventLog = [];
  StreamSubscription<GamepadEvent> _subscription;
  List<GamepadInfo> _gamepads;

  @override
  void initState() {
    super.initState();
    _subscription = FlutterGamepad.eventStream.listen(onGamepadEvent);
  }

  void dispose() {
    _subscription.cancel();
    super.dispose();
  }

  String _describeEvent(GamepadEvent e) {
    if (e is GamepadConnectedEvent) {
      return 'Gamepad connected: ${e.gamepadInfo.vendorName} = ${e.gamepadId}';
    } else if (e is GamepadDisconnectedEvent) {
      return 'Gamepad disconnected: ${e.gamepadInfo.vendorName} = ${e.gamepadId}';
    } else if (e is GamepadButtonEvent) {
      return '[${e.gamepadId}] ${e.button} -> ${e.value.toStringAsFixed(3)}';
    } else if (e is GamepadThumbstickEvent) {
      return '[${e.gamepadId}] ${e.thumbstick} -> ${e.x.toStringAsFixed(3)}, ${e.y.toStringAsFixed(3)}';
    } else {
      return 'Unknown event: $e';
    }
  }

  String _eventLogBin(GamepadEvent e) {
    return e.runtimeType.toString() +
        (e is GamepadButtonEvent ? e.button.toString() : '') +
        (e is GamepadThumbstickEvent ? e.thumbstick.toString() : '');
  }

  void onGamepadEvent(GamepadEvent e) {
    setState(() {
      if (_eventLog.isNotEmpty && _eventLogBin(e) == _eventLogBin(_eventLog.last)) {
        _eventLog.removeLast();
      }
      _eventLog.add(e);
      if (_eventLog.length >= 20) _eventLog.removeAt(0);
    });
  }

  Future<void> _fetchGamepads() async {
    final gamepads = await FlutterGamepad.gamepads();
    setState(() {
      _gamepads = gamepads;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Running on: ${Theme.of(context).platform}\n' +
                  _eventLog.map(_describeEvent).toList().join('\n')),
              RaisedButton(onPressed: _fetchGamepads, child: Text('Call gamepads()')),
              Text('gamepads = ${_gamepads?.map((pad) => pad.vendorName)}'),
            ],
          ),
        ),
      ),
    );
  }
}
