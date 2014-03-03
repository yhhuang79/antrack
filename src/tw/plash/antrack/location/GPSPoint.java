package tw.plash.antrack.location;

import android.location.Location;

public class GPSPoint {
	private Location location;
	private long timestamp;
	
	public Location getLocation() {
		return location;
	}
	public void setLocation(Location location) {
		this.location = location;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
