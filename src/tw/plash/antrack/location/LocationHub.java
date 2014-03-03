package tw.plash.antrack.location;

import java.util.Observable;

import tw.plash.antrack.util.Constants;
import tw.plash.antrack.util.Utility;
import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.LocationListener;

public class LocationHub extends Observable implements LocationListener {
	
	private Location previousLocation;
	private double previousVelocity;
	private long previousTimestamp;
	public LocationQueue locationQueue;
	
	public LocationHub(Context context) {
		locationQueue = new LocationQueue();
		previousLocation = null;
		previousVelocity = 0;
		previousTimestamp = System.currentTimeMillis();
	}
	
	@Override
	public void onLocationChanged(Location location) {
		//received a new location
		//do filter first
		boolean todisplay = shouldDisplayThisLocation(location);
		//
		setChanged();
		/**
		 * observers might include
		 * 1. map controller
		 * 2. stats keeper
		 * 3. DBHelper
		 * 4. location uploader
		 */
		notifyObservers(new AntrackLocation(location, todisplay));
	}
	
	private boolean shouldDisplayThisLocation(Location location) {
		boolean result = false;
		long DiffTime = System.currentTimeMillis() - previousTimestamp;
//		if (Utility.isValidLocation(location)) {
//			if ((previousLocation == null) || 
//					!Utility.isWithinAccuracyBound(previousLocation, location)) {
//				result = true;
//				previousTimestamp = System.currentTimeMillis();
//				locationQueue.offer(location, previousTimestamp);
//				if (previousLocation != null)
//					previousVelocity = Utility.getDistance(previousLocation, location) / DiffTime;
//			}
//			previousLocation = location;
//		}
//		if (locationQueue.getQueueSize() > 1)
//			result = locationQueue.isAvailablePoint(location, previousVelocity, DiffTime);
		if (Utility.isValidLocation(location)) {
			if (previousLocation == null) {
				result = true;
				previousTimestamp = System.currentTimeMillis();
				previousLocation = location;
				locationQueue.offer(previousLocation, previousTimestamp);								
			} else {
				if ((locationQueue.isAvailablePoint(location, previousVelocity, DiffTime) ||
						Utility.isWithinAccelerationBound(previousLocation, location, previousVelocity, DiffTime)) &&
						!Utility.isWithinAccuracyBound(previousLocation, location)) {
					result = true;
					previousTimestamp = System.currentTimeMillis();
					previousLocation = location;
					previousVelocity = Utility.getDistance(previousLocation, location) / DiffTime;
					locationQueue.offer(previousLocation, previousTimestamp);
				}
			}
		}
		return result;
	}
	
	public Location getLatestLocation(){
		return previousLocation;
	}
	
	public void clearPreviousLocation(){
		previousLocation = null;
	}
}
