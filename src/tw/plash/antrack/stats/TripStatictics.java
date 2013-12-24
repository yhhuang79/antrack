package tw.plash.antrack.stats;

import java.io.Serializable;

import tw.plash.antrack.util.Utility;
import android.location.Location;
import android.os.SystemClock;

public class TripStatictics implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 343414643452262890L;

	private final double radius = 6371008.7714;
	
	private double totalDistance;
	private double averageAccuracy;
	private double averageSpeed;
	private int pointCounter;
	
	private long baseTimeFromChorometerInMilliseconds;
	private int numberOfWatcher;
	
	private String startTimeAsString;
	
	private Location previousLocation;
	
	public TripStatictics() {
		totalDistance = 0;
		baseTimeFromChorometerInMilliseconds = 0;
		averageAccuracy = 0;
		averageSpeed = 0;
		pointCounter = 0;
		numberOfWatcher = 0;
		startTimeAsString = null;
		
		previousLocation = null;
	}
	
	public void resetStats(){
		totalDistance = 0;
		baseTimeFromChorometerInMilliseconds = 0;
		averageAccuracy = 0;
		averageSpeed = 0;
		pointCounter = 0;
		numberOfWatcher = 0;
		startTimeAsString = null;
		
		previousLocation = null;
	}
	
	public void addLocation(Location location){
		pointCounter += 1;
		if(previousLocation != null){
			totalDistance += Utility.getDistance(previousLocation, location);
			
			double accuracySum = averageAccuracy * (pointCounter - 1);
			averageAccuracy = (accuracySum + location.getAccuracy()) / pointCounter;
			
			double speedSum = averageSpeed * (pointCounter - 1);
			averageSpeed = (speedSum + location.getSpeed()) / pointCounter;
		}
		previousLocation = location;
	}
	
	public void setBaseTimeFromChronometer(long baseTimeFromChronometer){
		baseTimeFromChorometerInMilliseconds = baseTimeFromChronometer;
	}
	
	public String getFormattedElapsedTime(){
		long elapsedTimeInMilliseconds = SystemClock.elapsedRealtime() - baseTimeFromChorometerInMilliseconds;
		return Utility.getDurationInSecondsAsFormattedString((elapsedTimeInMilliseconds / 1000));
	}
	
	public String getDistanceString(){
		return String.format("%.3f", totalDistance / 1000) + "km";
	}
	
	public String getAverageAccuracyString(){
		return String.format("%.3f", averageAccuracy) + " m";
	}
	
	public String getAverageSpeedString(){
		return String.format("%.3f", averageSpeed) + "m/s";
	}
	
	public void setNumberOfWatcher(int number){
		numberOfWatcher = number;
	}
	
	public String getNumberOfFollowersString(){
		return numberOfWatcher + " followers";
	}
	
	public void restoreFromString(String toString){
		
	}
	
	@Override
	public String toString() {
		
		return super.toString();
	}
}
