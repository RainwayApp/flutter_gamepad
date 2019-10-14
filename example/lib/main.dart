import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_gamepad/flutter_gamepad.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  List<String> _eventLog = [];
  StreamSubscription<GamepadEvent> _subscription;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    _subscription = FlutterGamepad.eventStream.listen(onGamepadEvent);

    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await FlutterGamepad.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  void dispose() {
    _subscription.cancel();
    super.dispose();
  }

  String _describeEvent(GamepadEvent e) {
    if (e is GamepadConnectedEvent) {
      return 'Gamepad connected: ${e.gamepadInfo.vendorName}';
    } else if (e is GamepadDisconnectedEvent) {
      return 'Gamepad disconnected: ${e.gamepadInfo.vendorName}';
    } else {
      return 'Unknown event: $e';
    }
  }

  void onGamepadEvent(GamepadEvent e) {
    if (_eventLog.length >= 10) _eventLog.removeAt(0);
    _eventLog.add(_describeEvent(e));
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('Running on: $_platformVersion\n${_eventLog.join('\n')}'),
        ),
      ),
    );
  }
}
