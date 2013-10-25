package tw.plash.antrack;

public interface Constants {
	public String packageName = "tw.plash.antrack";
	public String PREF_FIXTOLOCATION = packageName + ".pref.fixtolocation";
	
	public enum IMAGE_MARKER_STATE{
		INCOMPLETE,
		PENDING_UPLOAD,
		UPLOAD_IN_PROGRESS,
		DONE
	};
	
	public final String API_RES_KEY_STATUS_CODE = "status_code";
	
	public final int MAX_INT = (int) (Math.pow(2, 16) - 1);
	
	public final int LOCATION_INTERVAL = 10000; //10 seconds
	public final int LOCATION_FASTEST_INTERVAL = 5000; //5 seconds
}
