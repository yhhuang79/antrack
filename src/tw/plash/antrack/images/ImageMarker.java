package tw.plash.antrack.images;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class ImageMarker {
	
	private double latitude;
	private double longitude;
	private long time;
	private String path;
	
	private int code;
	
	public ImageMarker() {
		latitude = 0;
		longitude = 0;
		time = 0;
		path = null;
		
		code = -1;
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
	
	public void setCode(int code){
		this.code = code;
	}
	
	public int getCode(){
		return code;
	}
	
	public void setLocation(Location location){
		setLatitude(location.getLatitude());
		setLongitude(location.getLongitude());
	}
	
	public void setLatLng(LatLng latlng){
		setLatitude(latlng.latitude);
		setLongitude(latlng.longitude);
	}
}
