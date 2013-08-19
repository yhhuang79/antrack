package tw.plash.antrack;

import java.io.Serializable;

import android.location.Location;

public class TripStatictics implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3018274649384063742L;

	private final double radius = 6371008.7714;
	
	private double totalDistance;
	private long totalDuration;
	private double averageAccuracy;
	private double averageSpeed;
	private int pointCounter;
	
	private int numberOfWatcher;
	
	private Location previousLocation;
	
	public TripStatictics() {
		totalDistance = 0;
		totalDuration = 0;
		averageAccuracy = 0;
		averageSpeed = 0;
		pointCounter = 0;
		numberOfWatcher = 0;
		
		previousLocation = null;
	}
	
	public void resetStats(){
		totalDistance = 0;
		totalDuration = 0;
		averageAccuracy = 0;
		averageSpeed = 0;
		pointCounter = 0;
		numberOfWatcher = 0;
		
		previousLocation = null;
	}
	
	public void addLocation(Location location){
		pointCounter += 1;
		if(previousLocation != null){
			totalDistance += getGreatCircleDistance(location);
			totalDuration += (location.getTime() - previousLocation.getTime());
			
			double accuracySum = averageAccuracy * (pointCounter - 1);
			averageAccuracy = (accuracySum + location.getAccuracy()) / pointCounter;
			
			double speedSum = averageSpeed * (pointCounter - 1);
			averageSpeed = (speedSum + location.getSpeed()) / pointCounter;
		}
		previousLocation = location;
	}
	
	private double getGreatCircleDistance(Location location){
		double dlat = toRad(location.getLatitude() - previousLocation.getLatitude());
		double dlon = toRad(location.getLongitude() - previousLocation.getLongitude());
		double latone = toRad(previousLocation.getLatitude());
		double lattwo = toRad(location.getLatitude());
		double a = Math.sin(dlat/2) * Math.sin(dlat/2) + Math.sin(dlon/2) * Math.sin(dlon/2) * Math.cos(latone) * Math.cos(lattwo);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return radius * c;
	}
	
	private double toRad(Double degree){
		return degree/180*Math.PI;
	}
	
	public String getDuration(){
		//milliseconds -> seconds -> minutes -> hours
		Double hours = ((double) totalDuration) / 1000 / 60 / 60;
		Double minutes = (hours - hours.intValue()) * 60;
		Double seconds = (minutes - minutes.intValue()) * 60;
		return String.format("%d:%02d:%04.1f", hours.intValue(), minutes.intValue(), seconds);
	}
	
	public String getDistance(){
		return String.format("%.3f", totalDistance / 1000);
	}
	
	public String getAverageAccuracy(){
		return String.format("%.3f", averageAccuracy);
	}
	
	public String getAverageSpeed(){
		return String.format("%.3f", averageSpeed);
	}
	
	public void setNumberOfWatcher(int number){
		numberOfWatcher = number;
	}
	
	public String getNumberOfWatcher(){
		return numberOfWatcher + " people are following you";
	}
}
