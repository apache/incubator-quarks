package quarks.test.svt.utils.sensor.gps;

public class SimulatedGeofence {
	protected static double GEOFENCE_LATITUDE_MAX = 37.21;
	protected static double GEOFENCE_LATITUDE_MIN = 37.0;
	protected static double GEOFENCE_LONGITUDE_MAX = -121.75;
	protected static double GEOFENCE_LONGITUDE_MIN = -122.0 ;
	
	// Simple Geofence test
    public static boolean outsideGeofence(double latitude, double longitude) {
    	
    	if (latitude < GEOFENCE_LATITUDE_MIN || latitude > GEOFENCE_LATITUDE_MAX ||
    		longitude < GEOFENCE_LONGITUDE_MIN || longitude > GEOFENCE_LONGITUDE_MAX
    	) return true;
    	else return false;
    }
}
