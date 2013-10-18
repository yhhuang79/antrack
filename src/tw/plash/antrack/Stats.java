package tw.plash.antrack;

public class Stats {
	
	private String starttime;
	private String duration;
	private long durationbase;
	private String distance;
	
	public Stats() {
		starttime = "00:00:00";
		duration = "00:00";
		durationbase = 0L;
		distance = "0km";
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

	public String getDistance() {
		return distance;
	}

	public void setDistance(String distance) {
		this.distance = distance;
	}
}
