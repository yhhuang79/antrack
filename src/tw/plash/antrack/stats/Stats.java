package tw.plash.antrack.stats;

public class Stats {
	
	private String starttime;
	private String duration;
	private long durationbase;
	private String distanceString;
	private double distance;
	private int numberOfFollowers;
	private int numberOfPhotos;
	
	public Stats() {
		starttime = "00:00:00";
		duration = "00:00";
		durationbase = 0L;
		distanceString = "0km";
		distance = 0.0;
		numberOfFollowers = 0;
		numberOfPhotos = 0;
	}
	
	public String getStarttime() {
		return starttime;
	}

	public void setStarttime(String starttime) {
		this.starttime = starttime;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public long getDurationbase() {
		return durationbase;
	}

	public void setDurationbase(long durationbase) {
		this.durationbase = durationbase;
	}
	
	public void setDistance(double distance){
		this.distance = distance;
		setDistanceString(String.format("%.3f", this.distance / 1000) + "km");
	}
	
	public String getDistanceString() {
		return distanceString;
	}

	public void setDistanceString(String distanceString) {
		this.distanceString = distanceString;
	}
	
	public int getNumberOfFollowers(){
		return this.numberOfFollowers;
	}
	
	public void setNumberOfFollowers(int number){
		this.numberOfFollowers = number;
	}
	
	public int getNumberOfPhotos(){
		return this.numberOfPhotos;
	}
	
	public void setNumberOfPhotos(int photos){
		this.numberOfPhotos = photos;
	}
}
