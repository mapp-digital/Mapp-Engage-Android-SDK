package com.appoxee.internal.provider

import com.appoxee.internal.model.request.RegisterDevice

internal interface DeviceProvider {

    fun generateRegistrationDevice(): RegisterDevice

    fun getUniqueDeviceId(): String

    fun getOSName(): String

    fun getOSNumber(): String

    fun getClientVersion(): String

    fun getAppVersion(): String

    fun getLocale(): String

    fun getTimeZone(): String

    fun getHardwareType(): String

    fun getDensity(): String

    fun getVendorId(): String

    fun getResolution(): String
}