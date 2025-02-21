package com.appoxee.shared

class GeofenceException(val geoStatus: GeoStatus) : Exception(geoStatus.status) {
}