package tw.plash.antrack;

import android.location.Location;

public class AntsLocation extends Location {
	
	private int toDisplay;
	
	public AntsLocation(Location location, boolean toDisplay){
		this(location, toDisplay? 1 : 0);
	}
	
	public AntsLocation(Location location, int toDisplay){
		super(location);
		this.toDisplay = toDisplay;
	}
	
	public void setToDisplay(boolean toDisplay){
		if(toDisplay){
			setToDisplay(1);
		} else{
			setToDisplay(0);
		}
	}
	
	public void setToDisplay(int toDisplay){
		this.toDisplay = toDisplay;
	}
	
	public int getToDisplay(){
		return toDisplay;
	}
}
