package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;

import tw.plash.antrack.images.ImageMarker;
import tw.plash.antrack.location.AntrackLocation;
import tw.plash.antrack.stats.Stats;
import tw.plash.antrack.util.Constants;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.location.Location;
import ch.hsr.geohash.GeoHash;

public class DBHelper {
	
	private static final String DATABASE_NAME = "antrack";
	private static final int DATABASE_VERSION = 15;
	private static final String CURRENT_TRIP_TABLE = "currenttriptable";
	private static final String PENDING_UPLOAD_LOCATION_TABLE = "pendinguploadlocation";
	private static final String IMAGES_TABLE = "imagestable";
	private static final String STATS_TABLE = "statstable";
	private static final int ERROR_DB_IS_CLOSED = -2;
	
	private SQLiteDatabase db;
	
	private static final String CREATE_TABLE_CURRENT_TRIP = "CREATE TABLE " + CURRENT_TRIP_TABLE
			+ "(id INTEGER PRIMARY KEY, "
			+ "latitude REAL, "
			+ "longitude REAL, "
			+ "altitude REAL, "
			+ "accuracy REAL, "
			+ "speed REAL, "
			+ "bearing REAL, "
			+ "locationsource TEXT, "
			+ "time INTEGER, "
			+ "todisplay INTEGER)";
	
	private static final String CREATE_TABLE_PENDING_UPLOAD_LOCATION = "CREATE TABLE " + PENDING_UPLOAD_LOCATION_TABLE
			+ "(id INTEGER PRIMARY KEY, "
			+ "latitude REAL, "
			+ "longitude REAL, "
			+ "altitude REAL, "
			+ "accuracy REAL, "
			+ "speed REAL, "
			+ "bearing REAL, "
			+ "locationsource TEXT, "
			+ "time INTEGER, "
			+ "todisplay INTEGER)";
	
	private static final String CREATE_TABLE_IMAGES = "CREATE TABLE " + IMAGES_TABLE
			+ "(id INTEGER PRIMARY KEY, "
			+ "latitude_actual REAL, "
			+ "longitude_actual REAL, "
			+ "latitude_display REAL, "
			+ "longitude_display REAL, "
			+ "geohash TEXT, "
			+ "time INTEGER, "
			+ "path TEXT, "
			+ "code INTEGER UNIQUE, "
			+ "state INTEGER)";
	
	private static final String CREATE_TABLE_STATS = "CREATE TABLE " + STATS_TABLE
			+ "(id INTEGER PRIMARY KEY, "
			+ "starttime TEXT, "
			+ "duration TEXT, "
			+ "durationbase REAL, "
			+ "distance TEXT)";
	
	private static class OpenHelper extends SQLiteOpenHelper {
		
		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL(CREATE_TABLE_CURRENT_TRIP);
				db.execSQL(CREATE_TABLE_PENDING_UPLOAD_LOCATION);
				db.execSQL(CREATE_TABLE_IMAGES);
				db.execSQL(CREATE_TABLE_STATS);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			try {
				db.execSQL("DROP TABLE IF EXIST " + DATABASE_NAME + "." + CURRENT_TRIP_TABLE);
				db.execSQL("DROP TABLE IF EXIST " + DATABASE_NAME + "." + PENDING_UPLOAD_LOCATION_TABLE);
				db.execSQL("DROP TABLE IF EXIST " + DATABASE_NAME + "." + IMAGES_TABLE);
				db.execSQL("DROP TABLE IF EXIST " + DATABASE_NAME + "." + STATS_TABLE);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			onCreate(db);
		}
	}
	
	public DBHelper(Context context) {
		OpenHelper openHelper = new OpenHelper(context);
		try {
			this.db = openHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
	}
	
	synchronized public long insertLocation(AntrackLocation alocation){
		return insertLocation(alocation.getLocation(), alocation.getToDisplay());
	}
	
	synchronized private long insertLocation(Location location, int toDisplay) {
		if (db.isOpen()) {
			ContentValues cv = new ContentValues();
			cv.put("latitude", location.getLatitude());
			cv.put("longitude", location.getLongitude());
			cv.put("altitude", location.getAltitude());
			cv.put("accuracy", location.getAccuracy());
			cv.put("speed", location.getSpeed());
			cv.put("bearing", location.getBearing());
			cv.put("locationsource", location.getProvider());
			cv.put("time", location.getTime());
			cv.put("todisplay", toDisplay);
			return db.insert(CURRENT_TRIP_TABLE, null, cv);
		} else {
			return ERROR_DB_IS_CLOSED;
		}
	}
	
	synchronized public List<Location> getAllDisplayableLocations(){
		List<Location> locations = new ArrayList<Location>();
		if(db.isOpen()){
			Cursor cursor = db.query(CURRENT_TRIP_TABLE, null, "todisplay > 0", null, null, null, "time ASC");
			if (cursor.moveToFirst()) {
				do {
					Location locationHolder = new Location("");
					locationHolder.setLatitude(cursor.getDouble(cursor.getColumnIndex("latitude")));
					locationHolder.setLongitude(cursor.getDouble(cursor.getColumnIndex("longitude")));
					locationHolder.setTime(cursor.getLong(cursor.getColumnIndex("time")));
					locations.add(locationHolder);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		return locations;
	}
	
	synchronized public long insertPendingUploadLocation(Location location, boolean toDisplay) {
		List<AntrackLocation> locations = new ArrayList<AntrackLocation>();
		locations.add(new AntrackLocation(location, toDisplay));
		return insertPendingUploadLocations(locations);
	}
	
	synchronized public long insertPendingUploadLocations(List<AntrackLocation> locations) {
		if (db.isOpen()) {
			for(AntrackLocation aLocation : locations){
				ContentValues cv = new ContentValues();
				cv.put("latitude", aLocation.getLocation().getLatitude());
				cv.put("longitude", aLocation.getLocation().getLongitude());
				cv.put("altitude", aLocation.getLocation().getAltitude());
				cv.put("accuracy", aLocation.getLocation().getAccuracy());
				cv.put("speed", aLocation.getLocation().getSpeed());
				cv.put("bearing", aLocation.getLocation().getBearing());
				cv.put("locationsource", aLocation.getLocation().getProvider());
				cv.put("time", aLocation.getLocation().getTime());
				cv.put("todisplay", aLocation.getToDisplay());
				db.insert(PENDING_UPLOAD_LOCATION_TABLE, null, cv);
			}
			return 1;
		} else {
			return ERROR_DB_IS_CLOSED;
		}
	}
	
	synchronized public List<AntrackLocation> getAllPendingUploadLocations(){
		List<AntrackLocation> locations = new ArrayList<AntrackLocation>();
		if(db.isOpen()){
			Cursor cursor = db.query(PENDING_UPLOAD_LOCATION_TABLE, null, null, null, null, null, null);
			if (cursor.moveToFirst()) {
				do {
					Location locationHolder = new Location("");
					locationHolder.setLatitude(cursor.getDouble(cursor.getColumnIndex("latitude")));
					locationHolder.setLongitude(cursor.getDouble(cursor.getColumnIndex("longitude")));
					locationHolder.setAltitude(cursor.getDouble(cursor.getColumnIndex("altitude")));
					locationHolder.setAccuracy(cursor.getFloat(cursor.getColumnIndex("accuracy")));
					locationHolder.setSpeed(cursor.getFloat(cursor.getColumnIndex("speed")));
					locationHolder.setBearing(cursor.getFloat(cursor.getColumnIndex("bearing")));
					locationHolder.setProvider(cursor.getString(cursor.getColumnIndex("locationsource")));
					locationHolder.setTime(cursor.getLong(cursor.getColumnIndex("time")));
					int toDisplay = cursor.getInt(cursor.getColumnIndex("todisplay"));
					locations.add(new AntrackLocation(locationHolder, toDisplay));
				} while (cursor.moveToNext());
				//clear the table to prevent duplicate upload
				db.delete(PENDING_UPLOAD_LOCATION_TABLE, null, null);
			}
			cursor.close();
		}
		return locations;
	}
	
	synchronized public long insertImageMarkerPath(int code, String path){ //XXX
		if (db.isOpen()) {
			ContentValues cv = new ContentValues();
			cv.put("path", path);
			cv.put("code", code);
//			cv.put("geohash", "default"); //give it a default value
			cv.put("state", Constants.IMAGE_MARKER_STATE.INCOMPLETE.ordinal());
			return db.insert(IMAGES_TABLE, null, cv);
		} else {
			return ERROR_DB_IS_CLOSED;
		}
	}
	
	/*
	 * 1. calculate geohash for new location
	 * 2. check if there are already other image markers with the same geohash
	 * 3. if geohash already exist -> find next available geohash, then check again
	 * 4. if geohash ok -> get center lat/lon of the geohash and use it as display lat/lon
	 */
	synchronized public int insertImageMarkerLocation(int code, Location location){ //XXX
		if (db.isOpen()) {
			ContentValues cv = new ContentValues();
			cv.put("latitude_actual", location.getLatitude());
			cv.put("longitude_actual", location.getLongitude());
			
			//get a valid geohash
			GeoHash geohash = GeoHash.withCharacterPrecision(location.getLatitude(), location.getLongitude(), Constants.GEOHASH_CHAR_PRECISION);
			if(isGeoHashOccupied(geohash)){
				//find the next available one
				GeoHash[] geohashes = geohash.getAdjacent();
				for(GeoHash hash : geohashes){
					if(!isGeoHashOccupied(hash)){
						geohash = hash;
						break;
					}
				}
			}
			cv.put("latitude_display", geohash.getBoundingBoxCenterPoint().getLatitude());
			cv.put("longitude_display", geohash.getBoundingBoxCenterPoint().getLongitude());
			cv.put("geohash", geohash.toBase32());
			
			cv.put("time", System.currentTimeMillis()); //XXX
			cv.put("state", Constants.IMAGE_MARKER_STATE.PENDING_UPLOAD.ordinal());
			return db.update(IMAGES_TABLE, cv, "code = " + code, null);
		} else {
			return ERROR_DB_IS_CLOSED;
		}
	}
	
	synchronized private boolean isGeoHashOccupied(GeoHash geohash){
		boolean result = false;
		Cursor cursor = db.query(IMAGES_TABLE, null, "geohash = '" + geohash.toBase32() + "'", null, null, null, null);
		if (cursor.moveToFirst()) {
			result = true;
		}
		cursor.close();
		return result;
	}
	
	synchronized public int setImageMarkerState(int code, Constants.IMAGE_MARKER_STATE state){ //XXX
		if (db.isOpen()) {
			ContentValues cv = new ContentValues();
			cv.put("state", state.ordinal());
			return db.update(IMAGES_TABLE, cv, "code = " + code, null);
		} else {
			return ERROR_DB_IS_CLOSED;
		}
	}
	
	synchronized public ImageMarker getPendingUploadImageMarker(){ //XXX
		if(db.isOpen()){
			ImageMarker imageMarker = new ImageMarker();
			int code = -1;
			Cursor cursor = db.query(IMAGES_TABLE, null, "state = " + Constants.IMAGE_MARKER_STATE.PENDING_UPLOAD.ordinal(), null, null, null, "id ASC");
			if (cursor.moveToFirst()) {
				imageMarker.setLatitude(cursor.getDouble(cursor.getColumnIndex("latitude_actual")));
				imageMarker.setLongitude(cursor.getDouble(cursor.getColumnIndex("longitude_actual")));
				imageMarker.setTime(cursor.getLong(cursor.getColumnIndex("time")));
				imageMarker.setPath(cursor.getString(cursor.getColumnIndex("path")));
				code = cursor.getInt(cursor.getColumnIndex("code"));
				imageMarker.setCode(code);
				//also change the state to upload in progress to avoid duplicate upload
//				setImageMarkerState(code, Constants.IMAGE_MARKER_STATE.UPLOAD_IN_PROGRESS);
				cursor.close();
				return imageMarker;
			} else{
				return null;
			}
		}
		return null;
	}
	
	synchronized public long getNumberOfImageMarkers(){ //XXX
		String sql = "SELECT COUNT(id) FROM " + IMAGES_TABLE;
		if (db.isOpen()) {
			SQLiteStatement statement = db.compileStatement(sql);
			long result = statement.simpleQueryForLong();
			statement.close();
			return result;
		}
		return 0;
	}
	
	synchronized public List<ImageMarker> getImageMarkers(){ //XXX
		List<ImageMarker> imagemarkers = new ArrayList<ImageMarker>();
		if(db.isOpen()){
			Cursor cursor = db.query(IMAGES_TABLE, null, null, null, null, null, null);
			if (cursor.moveToFirst()) {
				do {
					ImageMarker imageMarkerHolder = new ImageMarker();
					imageMarkerHolder.setLatitude(cursor.getDouble(cursor.getColumnIndex("latitude_display")));
					imageMarkerHolder.setLongitude(cursor.getDouble(cursor.getColumnIndex("longitude_display")));
					imageMarkerHolder.setTime(cursor.getLong(cursor.getColumnIndex("time")));
					imageMarkerHolder.setPath(cursor.getString(cursor.getColumnIndex("path")));
					imagemarkers.add(imageMarkerHolder);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		return imagemarkers;
	}
	
	synchronized public Stats getStats(){
		Stats stats = new Stats();
		if(db.isOpen()){
			Cursor cursor = db.query(STATS_TABLE, null, null, null, null, null, null);
			if (cursor.moveToFirst()) {
				stats.setStarttime(cursor.getString(cursor.getColumnIndex("starttime")));
				stats.setDuration(cursor.getString(cursor.getColumnIndex("duration")));
				stats.setDurationbase(cursor.getLong(cursor.getColumnIndex("durationbase")));
				stats.setDistance(cursor.getString(cursor.getColumnIndex("distance")));
			}
			cursor.close();
		}
		return stats;
	}
	
	synchronized public long setStats(Stats stats){
		if(db.isOpen()){
			db.delete(STATS_TABLE, null, null);
			ContentValues cv = new ContentValues();
			cv.put("starttime", stats.getStarttime());
			cv.put("duration", stats.getDuration());
			cv.put("durationbase", stats.getDurationbase());
			cv.put("distance", stats.getDistance());
			return db.insert(STATS_TABLE, null, cv);
		}
		return ERROR_DB_IS_CLOSED;
	}
	
	synchronized public void removeAll(){
		if(db.isOpen()){
			db.delete(CURRENT_TRIP_TABLE, null, null);
			db.delete(IMAGES_TABLE, null, null);
			db.delete(PENDING_UPLOAD_LOCATION_TABLE, null, null);
		}
	}
	
	synchronized public void removeStats(){
		if(db.isOpen()){
			db.delete(STATS_TABLE, null, null);
		}
	}
	
	synchronized public int removeImageMarker(int code){
		if(db.isOpen()){
			return db.delete(IMAGES_TABLE, "code = " + code, null);
		}
		return ERROR_DB_IS_CLOSED;
	}
	
	public boolean isDBOpen() {
		if (db != null) {
			return db.isOpen();
		} else {
			return false;
		}
	}
	
	public void closeDB() {
		if (db != null) { // if null, close will cause null pointer exception
			db.close();
		}
	}
}