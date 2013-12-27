package tw.plash.antrack.location;

import tw.plash.antrack.util.Constants;
import android.location.Location;

public class AntrackLocation {
	
	private Location location;
	private int toDisplay;
	
	public AntrackLocation(Location location, int toDisplay){
		this.location = location;
		this.toDisplay = toDisplay;
	}
	
	public AntrackLocation(Location location, boolean toDisplay) {
		this(location, toDisplay? Constants.VALID_LOCATION : Constants.INVALID_LOCATION);
	}
	
	public Location getLocation(){
		return this.location;
	}
	
	public int getToDisplay(){
		return this.toDisplay;
	}
}
