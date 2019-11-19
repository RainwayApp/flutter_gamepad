package com.example.flutter_gamepad

import android.os.Build
import android.view.InputDevice

fun gamepadInfoDictionary(device: InputDevice): HashMap<String, Any?> {
    val isAttached = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        !device.isExternal
    } else {
        null
    }

    return hashMapOf(
            "isAttachedToDevice" to isAttached,
            "vendorName" to device.name,
            "productCategory" to "Android Gamepad",
            "id" to device.id
    )
}
