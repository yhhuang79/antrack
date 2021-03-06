package tw.plash.antrack.util;

public interface IPCMessages {
	// messenger identification
	final int MAP_ACTIVITY = 0;
	final int STATS_FRAGMENT = 1;
	
	/*
	 *  activity/fragment -> service
	 */
	final int REGISTER = 10;
	final int DEREGISTER = 11;
	final int START_SHARING = 12;
	final int STOP_SHARING = 13;
	
	/*
	 * activity/fragment <- service
	 */
	
	//when a new location is received, ew stats will be generated too
	final int UPDATE_STATS = 14;
	//when sharing is finished, summary stats of the last sharing will be calculated
//	final int UPDATE_STATS_SUMMARY = 16;
	//when ever a new location is received, sharing or not
	final int UPDATE_NEW_LOCATION = 17;
	//during sharing, when map activity is destroyed and re-created, current trajectory will be redrawn
	final int SYNC_CURRENT_TRAJECTORY = 18;
	final int SYNC_CURRENT_IMAGE_MARKERS = 19;
	
	/*
	 * local broadcast
	 */
	final String LB_START_SHARING = "tw.plash.antrack.start.sharing";
	final String LB_STOP_SHARING = "tw.plash.antrack.stop.sharing";
	
	final String LB_IMAGE_CREATE = "tw.plash.antrack.image.create";
	final String LB_IMAGE_CONFIRM = "tw.plash.antrack.image.confirm";
	final String LB_IMAGE_CANCEL = "tw.plash.antrack.image.cancel";
	
	final String LB_EXTRA_IMAGE_PATH = "tw.plash.antrack.image.path";
	final String LB_EXTRA_REQUEST_CODE = "tw.plash.antrack.request.code";
}