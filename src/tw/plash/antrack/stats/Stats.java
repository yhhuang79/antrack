package tw.plash.antrack.stats;

import tw.plash.antrack.util.Utility;

public class Stats {
	
	private long startTime;
	private long stopTime;
	private double distance;
	private int numberOfFollowers;
	private int numberOfPhotos;
	
	public Stats() {
		startTime = -1L;
		stopTime = -1L;
		distance = 0.0;
		numberOfFollowers = 0;
		numberOfPhotos = 0;
	}
	
	public void setStartTime(long startTime){
		this.startTime = startTime;
	}
	
	public long getStartTime(){
		return this.startTime;
	}
	
	public String getStartTimeAsString(){
		if(startTime > -1L){
			return Utility.getHHMMSSTimeString(this.startTime);
		}
		return "00:00:00";
	}
	
	public void setStopTime(long stopTime){
		this.stopTime = stopTime;
	}
	
	public long getDuration(){
		if(stopTime > -1L){
			return this.stopTime - this.startTime;
		}
		return -1L;
	}
	
	public void setDistance(double distance){
		this.distance = distance;
	}
	
	public String getDistanceAsString(){
		return String.format("%.3f", this.distance / 1000) + "km";
	}
	
	public String getNumberOfFollowers(){
		return String.valueOf(this.numberOfFollowers);
	}
	
	public void setNumberOfFollowers(int number){
		this.numberOfFollowers = number;
	}
	
	public String getNumberOfPhotos(){
		return String.valueOf(this.numberOfPhotos);
	}
	
	public void setNumberOfPhotos(int photos){
		this.numberOfPhotos = photos;
	}
}
