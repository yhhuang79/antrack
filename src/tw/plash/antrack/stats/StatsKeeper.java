package tw.plash.antrack.stats;

import java.util.Observable;
import java.util.Observer;

import tw.plash.antrack.location.AntrackLocation;
import tw.plash.antrack.util.Utility;
import android.location.Location;

public class StatsKeeper extends Observable implements Observer{
	
	private Stats stats;
	private Location previousLocation;
	private double distance;
	
	public StatsKeeper() {
		stats = new Stats();
		previousLocation = null;
		distance = 0.0;
		publishUpdate(stats); //XXX this line is probably useless
	}
	
	public void initStats(){
		stats = new Stats();
		stats.setStartTime(System.currentTimeMillis());
		publishUpdate(stats);
	}
	
	public void finalizeStats(){
		stats.setStopTime(System.currentTimeMillis());
		publishUpdate(stats);
	}
	
	public void sync(){
		publishUpdate(stats);
	}
	
	public Stats getStats(){
		return stats;
	}
	
	/**
	 * receives location update from location hub
	 */
	@Override
	public void update(Observable observable, Object data) {
		AntrackLocation antrackLocation = (AntrackLocation) data;
		if(previousLocation != null){
			distance = distance + Utility.getDistance(previousLocation, antrackLocation.getLocation());
			stats.setDistance(distance);
			publishUpdate(stats);
		}
		previousLocation = antrackLocation.getLocation();
	}
	
	/**
	 * publishes stats update to stats fragment
	 */
	private void publishUpdate(Stats stats){
		setChanged();
		notifyObservers(stats);
	}
}