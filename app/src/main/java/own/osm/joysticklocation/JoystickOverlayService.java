package own.osm.joysticklocation;



/*

Open source!
JoyStick Location Spoofer

FUCK YOU Google Maps. you are fucking greedy!!!!

ELY M.


*/



import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;


//TODO FUCK GoogleMaps!!! FUCK their Greediness//
/*
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.GroundOverlay;
*/


import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.GroundOverlay2;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


import java.util.Random;

/**


 <item name="walking">0.000015</item>
 <item name="running">0.000030</item>
 <item name="driving">0.000060</item>
 <item name="fast driving">0.000100</item>
 <item name="flying">0.000500</item>
 <item name="warp speed!!!">0.130100</item>
 <item name="SUPER FAST!!!">0.939100</item>


 */
public class JoystickOverlayService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks {

    private String TAG = "joystickoverlay";
    private String KEY_SPEED = "last_known_speed";
    private String KEY_MYSPEED = "last_known_myspeed";
    private String KEY_LAST_LATITUDE = "last_known_latitude";
    private String KEY_LAST_LONGITUDE = "last_known_longitude";

    float walking = (float)(2 / 2.237);
    float running = (float)(8 / 2.237);
    float driving = (float)(60 / 2.237);
    float fastdriving = (float)(100 / 2.237);
    float flying = (float)(500 / 2.237);
    float wrap = (float)(1000 / 2.237);
    float superfast = (float)(9300 / 2.237);

    private double WALKINGSPEED = 0.000015;
    private double RUNNINGSPEED = 0.000030;
    private double DRIVINGSPEED = 0.000060;
    private double FASTDRIVINGSPEED = 0.000100;
    private double FLYINGSPEED = 0.000500;
    private double WRAPSPEED = 0.130100;
    private double SUPERFASTSPEED = 0.939100;
    private double setspeed = WALKINGSPEED;
    private float setmyspeed = 0;
    private long UPDATE_DURATION = 250L;
    private long UPDATE_DURATION_STATIONARY = 1000L;
    private double MAX_SPEED_FACTOR = setspeed;

    //double MperSec = progress * 1000.0 / 3600.0;

    //2.237

    //walking speed about 1.4 meters per second (m/s), or about 3.1 miles per hour
    /*
    * Now, all that said, here is a general guideline on treadmill speeds:
    * for most people 2 to 4 mph will be a walking speed; 4 to 5 mph will be a very fast walk or jog;
    * and anything over 5 mph will be jogging or running.
    * */


    private View mOverlay;
    Marker myLocation;
    boolean added = false;

    private CheckBox mSnapBack;
    private WindowManager.LayoutParams mOverlayParams;
    private WindowManager mWindowManager;
    private MapView mapView;
    private GoogleApiClient mGoogleApiClient;

    private float mStartTouchPointX;
    private float mStartTouchPointY;

    private Location mCurrentLocation;

    private MockLocationProvider mMockLocationProvider;
    private Handler mHandler;
    private double mLongitudeSpeed;
    private double mLatitudeSpeed;

    private Random mRandom;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void setupMap() {

        Log.i(TAG, "setupMap() ran");
        mapView = mOverlay.findViewById(R.id.map_view);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(false);
        mapView.setMultiTouchControls(true);
        //setContentView(mapView); //displaying the MapView

        mapView.getController().setZoom(18); //set initial zoom-level, depends on your need

        //mapView.getController().setCenter(new GeoPoint(40.003, -82.428));
        //mapView.getController().setCenter(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
        //mapView.setUseDataConnection(false); //keeps the mapView from loading online tiles using network connection.
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);


        myLocation = new Marker(mapView);
        myLocation.setIcon(getResources().getDrawable(org.osmdroid.library.R.drawable.person));
        myLocation.setImage(getResources().getDrawable(org.osmdroid.library.R.drawable.person));


        //CompassOverlay compassOverlay = new CompassOverlay(this, mapView);
        //compassOverlay.enableCompass();
        //mapView.getOverlays().add(compassOverlay);


        mapView.setMapListener(new DelayedMapListener(new MapListener() {
            public boolean onZoom(final ZoomEvent e) {
                mapView = mOverlay.findViewById(R.id.map_view);
                Log.i(TAG, "onZoom: " + mapView.getMapCenter().getLatitude() + ", " + mapView.getMapCenter().getLongitude());
                return true;
            }

            public boolean onScroll(final ScrollEvent e) {
                mapView = mOverlay.findViewById(R.id.map_view);
                Log.i(TAG, "onScroll: " + mapView.getMapCenter().getLatitude() + ", " + mapView.getMapCenter().getLongitude());
                return true;
            }
        }, 1000));

    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mGoogleApiClient.connect();
        mHandler = new Handler();

        LayoutInflater inflater = getSystemService(LayoutInflater.class);
        mOverlay = inflater.inflate(R.layout.joystick_mapview_overlay, null, false);

        mapView = mOverlay.findViewById(R.id.map_view);
        //mapView.onCreate(null);
        setupMap();
        /*
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(false);
        mapView.setMultiTouchControls(true);
        //setContentView(mapView); //displaying the MapView

        mapView.getController().setZoom(50); //set initial zoom-level, depends on your need
        //mapView.getController().setCenter(new GeoPoint(lat, lon));
        //mapView.getController().setCenter(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
        mapView.setUseDataConnection(false); //keeps the mapView from loading online tiles using network connection.
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        */

        mapView.onResume();
        //mapView.getMapAsync(this);

        final JoystickView jv = mOverlay.findViewById(R.id.joystick);
        jv.setOnJoystickPositionChangedListener(mListener);

        mSnapBack = mOverlay.findViewById(R.id.snap_back);
        mSnapBack.setChecked(jv.getSnapBackToCenter());
        mSnapBack.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                jv.setSnapBackToCenter(isChecked);
            }
        });

        setspeed = getspeed();
        setmyspeed = getmyspeed();
        View SettingsButton = mOverlay.findViewById(R.id.settings);
        SettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsdialog();

            }
        });


        int overlayWidth = getResources().getDimensionPixelSize(R.dimen.overlay_width);
        int overlayHeight = getResources().getDimensionPixelSize(R.dimen.overlay_height);
        mOverlayParams =  new WindowManager.LayoutParams(overlayWidth, overlayHeight,
                WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
        mOverlayParams.gravity = Gravity.TOP | Gravity.LEFT;

        mWindowManager = getSystemService(WindowManager.class);
        mWindowManager.addView(mOverlay, mOverlayParams);

        View windowMover = mOverlay.findViewById(R.id.window_mover);
        windowMover.setOnTouchListener(mMoveWindowTouchListener);

        View cancelButton = mOverlay.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });
        mRandom = new Random();

        mMockLocationProvider = new MockLocationProvider(LocationManager.GPS_PROVIDER, this);


        if (mCurrentLocation != null) {
            Log.i(TAG, "onCreate() lat: "+ mCurrentLocation.getLatitude()+" lon: "+mCurrentLocation.getLongitude());
            onLocationChanged(mCurrentLocation);
            mHandler.post(mUpdateLocationRunnable);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //MapView.onDestroy();
        if (mOverlay.isAttachedToWindow()) {
            mWindowManager.removeView(mOverlay);
        }
        mGoogleApiClient.disconnect();
        mMockLocationProvider.shutdown();
        mHandler.removeCallbacks(mUpdateLocationRunnable);

        if (mCurrentLocation != null) {
            SharedPreferences.Editor edit =
                    PreferenceManager.getDefaultSharedPreferences(this).edit();
            edit.putString(KEY_LAST_LATITUDE, "" + mCurrentLocation.getLatitude());
            edit.putString(KEY_LAST_LONGITUDE, "" + mCurrentLocation.getLongitude());
            edit.commit();
            edit.apply();
        }
    }

    /*
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);
        mGoogleApiClient.connect();
    }
    */

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "onLocationChanged loaded");
        if (location != null) {
            Log.i(TAG, "onLocationChanged lat: "+location.getLatitude()+ " lon: "+location.getLongitude());
            mapView.getController().setCenter(new GeoPoint(location.getLatitude(), location.getLongitude()));
            myLocation.setPosition(new GeoPoint(location.getLatitude(), location.getLongitude()));
            if (!added) {
                mapView.getOverlayManager().add(myLocation);
                added = true;
            }
            mapView.getController().animateTo(myLocation.getPosition());

        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private View.OnTouchListener mMoveWindowTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.i(TAG, "onTouch...");
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mStartTouchPointX = event.getRawX() - mOverlayParams.x;
                    mStartTouchPointY = event.getRawY() - mOverlayParams.y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    mOverlayParams.x = (int)(event.getRawX() - mStartTouchPointX);
                    mOverlayParams.y = (int)(event.getRawY() - mStartTouchPointY);
                    if (mOverlayParams.x < 0) mOverlayParams.x = 0;
                    if (mOverlayParams.y < 0) mOverlayParams.y = 0;
                    mWindowManager.updateViewLayout(mOverlay, mOverlayParams);
                    break;
            }
            return true;
        }
    };

    private JoystickView.OnJoystickPositionChangedListener mListener =
            new JoystickView.OnJoystickPositionChangedListener() {
        @Override
        public void onJoystickPositionChanged(float x, float y) {
            MAX_SPEED_FACTOR = setspeed;
            if (mCurrentLocation != null) {
                if (x != 0.0 || y != 0.0) {
                    double theta = Math.atan2(-y, x);
                    double magnitude = Math.sqrt(x*x + y*y) * MAX_SPEED_FACTOR;
                    mLatitudeSpeed = magnitude * Math.sin(theta);
                    mLongitudeSpeed = magnitude * Math.cos(theta);
                } else {
                    mLatitudeSpeed = mLongitudeSpeed = 0;
                }
                Log.i(TAG, "OnJoystickPositionChangedListener lat: "+mCurrentLocation.getLatitude()+ " lon: "+mCurrentLocation.getLongitude());
            }
        }
    };

    private Runnable mUpdateLocationRunnable = new Runnable() {
        @Override
        public void run() {
            final Location lastLocation = mCurrentLocation != null
                    ? new Location(mCurrentLocation)
                    : null;
            double latitude = mCurrentLocation.getLatitude();
            double longitude = mCurrentLocation.getLongitude();
            //float speed = mCurrentLocation.getSpeed();
            //Log.i(TAG, "mCurrentLocation.getSpeed(): "+ speed);
            Log.i(TAG, "Runnable lat: "+latitude+ " lon: "+longitude);
            Log.i(TAG, "Runnable setmyspeed: "+ setmyspeed);
            float finalmyspeed = (float)(setmyspeed / 2.237);
            Log.i(TAG, "Runnable finalmyspeed: "+ finalmyspeed);
            long updateDuration = UPDATE_DURATION_STATIONARY;
            if (mLatitudeSpeed != 0.0 || mLongitudeSpeed != 0.0) {
                latitude += mLatitudeSpeed;
                longitude += mLongitudeSpeed;
                mCurrentLocation.setLatitude(latitude);
                mCurrentLocation.setLongitude(longitude);
                onLocationChanged(mCurrentLocation);
                updateDuration = UPDATE_DURATION;
            }
            MAX_SPEED_FACTOR = setspeed;
            latitude += mRandom.nextDouble() * MAX_SPEED_FACTOR - MAX_SPEED_FACTOR / 2;
            longitude += mRandom.nextDouble() * MAX_SPEED_FACTOR - MAX_SPEED_FACTOR / 2;
            mMockLocationProvider.pushLocation(latitude, longitude,
                    lastLocation != null ? lastLocation.bearingTo(mCurrentLocation) : 0f, finalmyspeed);
            mHandler.postDelayed(this, updateDuration);
        }
    };



    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected started");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            Log.i(TAG, "onConnected location is null - getting from prefs");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String latStr = prefs.getString(KEY_LAST_LATITUDE, null);
            String lngStr = prefs.getString(KEY_LAST_LONGITUDE, null);
            if (latStr != null && lngStr != null) {
                location = new Location(LocationManager.GPS_PROVIDER);
                location.setLatitude(Double.valueOf(latStr));
                location.setLongitude(Double.valueOf(lngStr));
                location.setTime(System.currentTimeMillis());
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

            }
        }
        if (location != null) {
            Log.i(TAG, "onConnected lat: "+ location.getLatitude()+" lon: "+location.getLongitude());
            mCurrentLocation = location;
            onLocationChanged(location);
            mHandler.post(mUpdateLocationRunnable);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended");
    }

    double getspeed() {
        double getspeed;
        getspeed = Double.parseDouble(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(KEY_SPEED, String.valueOf(FASTDRIVINGSPEED)));
        return getspeed;
    }

    float getmyspeed() {
        float getmyspeed;
        getmyspeed = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(KEY_MYSPEED, String.valueOf(0)));
        return getmyspeed;
    }

    void settingsdialog() {
        Resources res = getResources();
        final String[] speed = res.getStringArray(R.array.speed);
        final String[] myspeed = res.getStringArray(R.array.setspeed);
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        alt_bld.setTitle("Select a speed for mock gps");
        alt_bld.setSingleChoiceItems(speed, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Toast.makeText(getApplicationContext(), "Set Speed to " + speed[item], Toast.LENGTH_SHORT).show();
                setspeed = Double.parseDouble(speed[item]);
                setmyspeed = Float.parseFloat(myspeed[item]);
                //save to our prefs
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                edit.putString(KEY_SPEED, String.valueOf(setspeed));
                edit.putString(KEY_MYSPEED, String.valueOf(setmyspeed));
                edit.commit();
                edit.apply();
                Log.i(TAG, "saved speed: "+setspeed);
                Log.i(TAG, "saved myspeed: "+setmyspeed);
                dialog.dismiss();


            }
        });

        AlertDialog alert = alt_bld.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
    }



}
