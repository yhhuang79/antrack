package tw.plash.antrack;

public interface IPCMessages {
	//all message types pass between service and activity
	public static final int ACTIVITY_REGISTER = 0;
	public static final int ACTIVITY_DEREGISTER = 1;
	public static final int START_SHARING = 2;
	public static final int STOP_SHARING = 3;
	public static final int SHARING_SUMMARY_REQUEST = 4;
	public static final int SHARING_SUMMARY_REPLY = 5;
	public static final int NEW_LOCATION_UPDATE = 6;
	public static final int SYNC_CURRENT_TRAJECTORY = 7;
	public static final int SHARING_DURATION_UPDATE = 8;
	
	
}
