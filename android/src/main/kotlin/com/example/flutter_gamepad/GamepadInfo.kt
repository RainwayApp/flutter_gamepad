package com.example.flutter_gamepad

import android.os.Build
import android.view.InputDevice

typealias GamepadInfo = HashMap<String, Any?>

fun gamepadInfoDictionary(device: InputDevice): GamepadInfo {
    val isAttached = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) !device.isExternal else null

    return hashMapOf(
            "isAttachedToDevice" to isAttached,
            "vendorName" to device.name,
            "productCategory" to "Android Gamepad",
            "id" to device.id
    )
}

fun allGamepadInfoDictionaries(): List<GamepadInfo> {
    return InputDevice.getDeviceIds()
            .map(InputDevice::getDevice)
            .filter { it.isGamepad }
            .map { gamepadInfoDictionary(it) }
}