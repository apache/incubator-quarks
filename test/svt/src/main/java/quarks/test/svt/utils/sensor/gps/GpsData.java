package quarks.test.svt.utils.sensor.gps;

public class GpsData {
	private double latitude;
	private double longitude;
	private double speedMph;
	private long time;
	private String driverId;
	
	public GpsData(double latitude, double longitude, double speedMph, long time, String driverId) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.speedMph = speedMph;
		this.time = time;
		this.driverId = driverId;
	}
	
	public double getSpeedMph() {
		return speedMph;
	}
	
	public double getLatitude() {
		return latitude; 
	}
	
	public double getLongitude() {
		return longitude; 
	}
	
	@Override
	public String toString() {
		return  driverId
				+ ", " + latitude 			
				+ ", " + longitude 
				+ ", " + speedMph
				+ ", " + time
				;
	}
}
