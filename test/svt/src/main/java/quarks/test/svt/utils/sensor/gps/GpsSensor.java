package quarks.test.svt.utils.sensor.gps;

public class GpsSensor {

	private double latitude;
	private double longitude;
	private double altitude;
	private double speedMetersPerSec; // meters per sec
	private long time;
	private double course; 
	
	public GpsSensor(double latitude, double longitude, double altitude, double speedMetersPerSec, long time, double course) {

		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.speedMetersPerSec = speedMetersPerSec;
		this.time = time;	
		this.course = course;
	}
	
	public double getLatitude() {
		return latitude; 
	}
	
	public double getLongitude() {
		return longitude; 
	}
	
	public double geAltitude() {
		return altitude; 
	}
	
	public double getSpeedMetersPerSec() {
		return speedMetersPerSec; 
	}
	
	public long getTime() {
		return time; 
	}
	
	public double course() {
		return course;
	}
	
	@Override
	public String toString() {
		return  latitude 			
				+ ", " + longitude 
				+ ", " + altitude
				+ ", " + speedMetersPerSec
				+ ", " + time
				+ ", " + course
				;
	
	}
	
}
