package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

public class DBHelper {
	
	private static final String DATABASE_NAME = "antrack";
	private static final int DATABASE_VERSION = 2;
	private static final String CURRENT_TRIP_TABLE = "currenttriptable";
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
	
	private static class OpenHelper extends SQLiteOpenHelper {
		
		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL(CREATE_TABLE_CURRENT_TRIP);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			try {
				db.execSQL("DROP TABLE IF EXIST " + DATABASE_NAME + "." + CURRENT_TRIP_TABLE);
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
	
	synchronized public long insert(Location location, int toDisplay) {
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
		ArrayList<Location> locations = new ArrayList<Location>();
		if(db.isOpen()){
			Cursor cursor = db.query(CURRENT_TRIP_TABLE, null, "todisplay > 0", null, null, null, null);
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
	
	synchronized public void removeAllLocations(){
		if(db.isOpen()){
			db.delete(CURRENT_TRIP_TABLE, null, null);
		}
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