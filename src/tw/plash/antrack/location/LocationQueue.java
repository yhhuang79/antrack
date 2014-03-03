package tw.plash.antrack.location;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tw.plash.antrack.util.Constants;
import tw.plash.antrack.util.Utility;

import android.location.Location;
import android.util.Log;

public class LocationQueue {
	private List<GPSPoint> locationQueue = new ArrayList<GPSPoint>();
	private int queueSize = 6;
	
	private static final double velocityLimit = 0.088;
		
	public int getQueueSize() {
		return locationQueue.size();
	}
	
	public void clear() {
		locationQueue.clear();
	}	
	
	public void offer(GPSPoint p){
		locationQueue.add(p);
		if (locationQueue.size() > queueSize)
			locationQueue.remove(0);
	}
	
	public void offer(Location location, long timestamp){
		GPSPoint p = new GPSPoint();
		p.setLocation(location);
		p.setTimestamp(timestamp);
		this.offer(p);
	}

	public boolean isAvailablePoint(Location location, double previousVelocity, long diffTime) {
		Location predictLocation = predictLocation();
		double res = Utility.getDistance(location, predictLocation);
		double deltaOutlier = velocityLimit * diffTime;
		double deltaSmooth = previousVelocity * diffTime * 1.2;
		Log.d("is Available Point", "res, outlier, smooth :  " + res + " : " + deltaOutlier  + " : " + deltaSmooth);
		if ((res > deltaSmooth) && (res < deltaOutlier)){
			return true;
		} else{
			return false;
		}
	}
	
	private Location predictLocation() {
		double latitude = 0, longitude = 0, kt = 0;
		Iterator<GPSPoint> iterator = locationQueue.iterator();
		while (iterator.hasNext()) {
			GPSPoint p = iterator.next();
			double kti = kernal(p.getTimestamp());
			latitude = latitude + p.getLocation().getLatitude() * kti;
			longitude = longitude + p.getLocation().getLongitude() * kti;
			kt = kt + kti;
		}
		latitude = latitude / kt;
		longitude = longitude /kt;
		Location l = new Location("dummyprovider");
		l.setLatitude(latitude);
		l.setLongitude(longitude);
		Log.d("Predict Location", "Lat, Lng :  " + latitude + ":" + longitude + " : " + locationQueue.size());
		return l;
	}
	
	private double kernal(long timestamp) {		
		return Math.exp(-1*((timestamp - System.currentTimeMillis())^2)/500); //(2*(10000/Constants.LOCATION_INTERVAL)^2));
	}
	
}
