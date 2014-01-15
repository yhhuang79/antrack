package tw.plash.antrack.location;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import tw.plash.antrack.AntrackService;
import tw.plash.antrack.images.ImageMarker;
import tw.plash.antrack.util.Constants;
import tw.plash.antrack.util.TouchableWrapperCallback;
import tw.plash.antrack.util.Utility;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.AsyncTask;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapController implements TouchableWrapperCallback, Observer{
	
	private GoogleMap gmap;
	private Polyline trajectory;
	private LatLng latestLocation;
	private OnLocationChangedListener onLocationChangedListener;
	private boolean fixToLocation;
	
	private float zoom;
	
	public MapController() {
		this.trajectory = null;
		this.latestLocation = null;
		this.zoom = -1f;
	}
	
	public void setMap(GoogleMap gmap){
		this.gmap = gmap;
		setupGmap();
	}
	
	private void setupGmap(){
		this.gmap.setMyLocationEnabled(true);
		this.gmap.getUiSettings().setMyLocationButtonEnabled(false);
		this.gmap.getUiSettings().setZoomGesturesEnabled(true);
		this.gmap.getUiSettings().setZoomControlsEnabled(false);
		this.gmap.setIndoorEnabled(true);
		this.gmap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
		this.gmap.setOnCameraChangeListener(new OnCameraChangeListener() {
			@Override
			public void onCameraChange(CameraPosition position) {
				zoom = position.zoom;
			}
		});
		this.gmap.setLocationSource(new LocationSource() {
			@Override
			public void activate(OnLocationChangedListener listener) {
				onLocationChangedListener = listener;
			}
			@Override
			public void deactivate() {
				onLocationChangedListener = null;
			}
		});
		this.gmap.setOnMyLocationChangeListener(new OnMyLocationChangeListener() {
			@Override
			public void onMyLocationChange(Location location) {
				if (fixToLocation) {
					moveToLocation(location);
				}
			}
		});
	}
	
	public void clearMap() {
		this.gmap.clear();
		this.trajectory = null;
		this.latestLocation = null;
	}
	
	synchronized public void addLocation(Location location) {
		latestLocation = location2LatLng(location);
		if (trajectory != null) {
			// already have locations
			List<LatLng> points = trajectory.getPoints();
			points.add(location2LatLng(location));
			trajectory.setPoints(points);
		} else {
			// first time adding locations
			ArrayList<Location> list = new ArrayList<Location>();
			list.add(location);
			drawLocations(list);
		}
	}
	
	synchronized public void drawLocations(List<Location> locations) {
		if (locations.isEmpty()) {
			return;
		}
		PolylineOptions pline = new PolylineOptions();
		for (Location location : locations) {
			pline.add(location2LatLng(location));
		}
		pline.color(0xffdd0000);
		trajectory = gmap.addPolyline(pline);
		List<LatLng> points = trajectory.getPoints();
		LatLng firstPoint = points.get(0);
		latestLocation = points.get(points.size() - 1);
		drawMarker(firstPoint, "Start");
		if(zoom > 0){
			gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestLocation, zoom));
		} else{
			gmap.animateCamera(CameraUpdateFactory.newLatLng(latestLocation));
		}
	}
	
	synchronized public void drawEndMarker() {
		//got nothing to draw
		if(trajectory != null){
			List<LatLng> points = trajectory.getPoints();
			LatLng lastPoint = points.get(points.size() - 1);
			drawMarker(lastPoint, "End");
		}
	}
	
	private void drawMarker(LatLng position, String title){
		gmap.addMarker(new MarkerOptions().position(position).title(title));
	}
	
	synchronized public void addImageMarkers(List<ImageMarker> imagemarkers){
		for(ImageMarker im : imagemarkers){
			LatLng latlng = new LatLng(im.getLatitude(), im.getLongitude());
			String path = im.getPath();
			new markerloader(latlng, path).execute();
		}
	}
	
	private class markerloader extends AsyncTask<Void, Void, Bitmap>{
		private LatLng latlng;
		private String path;
		public markerloader(LatLng latlng, String path) {
			this.latlng = latlng;
			this.path = path;
		}
		@Override
		protected Bitmap doInBackground(Void... params) {
			return Utility.getThumbnail(path, 72);
		}
		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if(gmap != null){
				gmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(result)).position(latlng).draggable(true));
			}
		}
	}
	
	private void animateToLocation(Location location){
		if(location != null){
			if(zoom > 0){
				gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(location2LatLng(location), zoom));
			} else{
				gmap.animateCamera(CameraUpdateFactory.newLatLng(location2LatLng(location)));
			}
		}
	}
	
	public void setZoomLevel(float zoom){
		this.zoom = zoom;
	}
	
	public float getZoomLevel(){
		return zoom;
	}
	
	synchronized public void centerAtMyLocation() {
		fixToLocation = true;
		Location location = gmap.getMyLocation();
		if(location != null){
			animateToLocation(location);
		} else{
			throw new NullPointerException();
		}
	}
	
	synchronized public void moveToLocation(Location location){
		animateToLocation(location);
	}
	
	public boolean hasLocation(){
		if(latestLocation != null){
			return true;
		} else{
			return false;
		}
	}
	
	public LatLng getLatestLocation(){
		return latestLocation;
	}
	
	private LatLng location2LatLng(Location location){
		return new LatLng(location.getLatitude(), location.getLongitude());
	}

	@Override
	public void setIsTouched() {
		fixToLocation = false;
	}
	
	public void setFixToLocation(boolean fix){
		this.fixToLocation = fix;
	}
	
	public boolean getFixToLocation(){
		return this.fixToLocation;
	}
	
	private void setNewLocation(Location location){
		onLocationChangedListener.onLocationChanged(location);
	}

	@Override
	public void update(Observable observable, Object data) {
		AntrackLocation antrackLocation = (AntrackLocation) data;
		if(antrackLocation.getToDisplay() == Constants.VALID_LOCATION){
			setNewLocation(antrackLocation.getLocation());
			if(AntrackService.isSharingLocation()){
				addLocation(antrackLocation.getLocation());
			}
		}
	}
}