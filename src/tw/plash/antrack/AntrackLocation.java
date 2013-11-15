package tw.plash.antrack;

import android.location.Location;

public class AntrackLocation {
	
	private Location location;
	private int toDisplay;
	
	public AntrackLocation(Location location, int toDisplay){
		this.location = location;
		this.toDisplay = toDisplay;
	}
	
	public AntrackLocation(Location location, boolean toDisplay) {
		this(location, toDisplay? 1 : 0);
	}
	
	public Location getLocation(){
		return this.location;
	}
	
	public int getToDisplay(){
		return this.toDisplay;
	}
}
