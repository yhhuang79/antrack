package tw.plash.antrack.stats;

import java.util.Observable;
import java.util.Observer;

import tw.plash.antrack.location.AntrackLocation;
import tw.plash.antrack.util.Utility;
import android.location.Location;

/**
 * Stats keeper observers location hub for location update,
 * when a location update is received, stats keeper calculates
 * new stats (e.g. new distance, etc.)
 * 
 * Stats fragment observes stats keeper for stats update
 * 
 * @author cszu
 *
 */
public class StatsKeeper extends Observable implements Observer{
	
	private Stats stats;
	private Location previousLocation;
	private double distance;
	
	public StatsKeeper() {
		this.stats = new Stats();
		this.previousLocation = null;
		this.distance = 0.0;
	}
	
	public void addLocation(Location location){
		distance = distance + Utility.getDistance(previousLocation, location);
		stats.setDistance(distance);
		setChanged();
		notifyObservers(stats);
	}
	
	public void resetStats(){
		this.stats = new Stats();
		setChanged();
		notifyObservers();
	}
	
	@Override
	public void update(Observable observable, Object data) {
		AntrackLocation antrackLocation = (AntrackLocation) data;
		if(previousLocation != null){
			distance = distance + Utility.getDistance(previousLocation, antrackLocation.getLocation());
			stats.setDistance(distance);
			setChanged();
			notifyObservers(stats);
		}
		previousLocation = antrackLocation.getLocation();
	}
}
