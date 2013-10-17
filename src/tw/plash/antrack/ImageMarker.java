package tw.plash.antrack;

public class ImageMarker {
	
	private double latitude;
	private double longitude;
	private long time;
	private String path;
	
	public ImageMarker() {
		latitude = 0;
		longitude = 0;
		time = 0;
		path = null;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
