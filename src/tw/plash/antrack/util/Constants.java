package tw.plash.antrack.util;

public interface Constants {
	public String packageName = "tw.plash.antrack";
	public String PREF_FIXTOLOCATION = packageName + ".pref.fixtolocation";
	public String PREF_LASTZOOMLEVEL = packageName + ".pref.lastzoomlevel";
	
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
	
	/* character     grid size
	 * precision    w    x    h
	 *     1 -> 5003778m x 5003778m
	 *     2 -> 1155449m x  625472m
	 *     3 ->  142952m x  156368m
	 *     4 ->   35440m x   19546m
	 *     5 ->    4428m x    4886m
	 *     6 ->    1106m x     610m
	 *     7 ->     138m x     152m
	 *     8 ->      34m x      19m
	 *     9 ->       4m x       4m
	 *    10 ->       1m x     0.6m
	 *    11 ->    0.13m x    0.15m
	 *    12 ->    0.03m x    0.02m
	 */
	public final int GEOHASH_CHAR_PRECISION = 9;
}
