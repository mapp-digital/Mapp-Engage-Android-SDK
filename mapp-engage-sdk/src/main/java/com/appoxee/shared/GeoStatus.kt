package com.appoxee.shared

open class GeoStatus(val status: String) {
    class GeoSdkNotReady : GeoStatus("GEOFENCE_SDK_NOT_READY")
    class GeoLocationPermissionsNotGranted : GeoStatus("GEOFENCE_LOCATION_PERMISSIONS_NOT_GRANTED")
    class GeoLocationNotAccurate : GeoStatus("GEOFENCE_LOCATION_NOT_ACCURATE")
    class GeoLocationNotAvailable : GeoStatus("GEOFENCE_LOCATION_NOT_AVAILABLE")
    class GeoNoInternetConnection : GeoStatus("GEOFENCE_NO_INTERNET_CONNECTION")
    class GeoFailedGettingRegions : GeoStatus("GEOFENCE_FAILED_GETTING_REGIONS")
    class GeoStartedOk : GeoStatus("GEOFENCE_STARTED_OK")
    class GeoStoppedOk : GeoStatus("GEOFENCE_STOPPED_OK")
    class GeoGeneralError : GeoStatus("GEOFENCE_GENERAL_ERROR")
    class GeoTooManyGeofenceCalls : GeoStatus("GEOFENCE_TOO_MANY_GEOFENCE_CALLS")
    class GeoTooManyGeofences : GeoStatus("GEOFENCE_TOO_MANY_GEOFENCES")
    class GeoTooManyPendingIntents : GeoStatus("GEOFENCE_TOO_MANY_PENDING_INTENTS")
    class GeoErrorStopping : GeoStatus("GEOFENCE_ERROR_STOPPING")

}
