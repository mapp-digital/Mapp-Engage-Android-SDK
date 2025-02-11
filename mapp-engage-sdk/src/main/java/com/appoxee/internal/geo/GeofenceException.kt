package com.appoxee.internal.geo

import com.appoxee.shared.GeoStatus

class GeofenceException(val geoStatus: GeoStatus) : Exception(geoStatus.status) {
}