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
	
	public LocationHub(Context context) {
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
		if (Utility.isValidLocation(location)) {
			long DiffTime = System.currentTimeMillis() - previousTimestamp;
			if ((previousLocation == null) || 
					!Utility.isWithinAccuracyBound(previousLocation, location) ||
					Utility.isWithinAccelerationBound(previousLocation, location, previousVelocity, DiffTime)) {
				result = true;
			}
			previousLocation = location;
			previousVelocity = Utility.getDistance(previousLocation, location) / Constants.LOCATION_INTERVAL;
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
