package com.star.runtracker;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class RunManager {

    private static final String TAG = "RunManager";

    public static final String PREFS_FILE = "runs";
    public static final String PREF_CURRENT_RUN_ID = "RunManager.currentRunId";

    public static final String ACTION_LOCATION =
            "com.star.runtracker.ACTION_LOCATION";

    private static final String TEST_PROVIDER = "TEST_PROVIDER";

    private static RunManager sRunManager;

    private Context mAppContext;

    private LocationManager mLocationManager;

    private RunDatabaseHelper mRunDatabaseHelper;
    private SharedPreferences mSharedPreferences;
    private long mCurrentRunId;

    private RunManager(Context appContext) {
        mAppContext = appContext;
        mLocationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);

        mRunDatabaseHelper = new RunDatabaseHelper(mAppContext);
        mSharedPreferences = mAppContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    public static RunManager getInstance(Context context) {
        if (sRunManager == null) {
            synchronized (RunManager.class) {
                if (sRunManager == null) {
                    sRunManager = new RunManager(context);
                }
            }
        }
        return sRunManager;
    }

    private PendingIntent getLocationPendingIntent(boolean shouldCreate) {
        Intent broadcast = new Intent(ACTION_LOCATION);
        int flags = shouldCreate ? 0 : PendingIntent.FLAG_NO_CREATE;

        return PendingIntent.getBroadcast(mAppContext, 0, broadcast, flags);
    }

    private void startLocationUpdates() {
        String provider = LocationManager.GPS_PROVIDER;

        if (mLocationManager.getProvider(TEST_PROVIDER) != null &&
                mLocationManager.isProviderEnabled(TEST_PROVIDER)) {
            provider = TEST_PROVIDER;
        }

        Log.d(TAG, "Using provider " + provider);

        PendingIntent pi = getLocationPendingIntent(true);

        mLocationManager.requestLocationUpdates(provider, 0, 0, pi);

        Location lastKnown = mLocationManager.getLastKnownLocation(provider);

        if (lastKnown != null) {
            lastKnown.setTime(System.currentTimeMillis());
            broadcastLocation(lastKnown);
        }
    }

    private void stopLocationUpdates() {
        PendingIntent pi = getLocationPendingIntent(false);

        if (pi != null) {
            mLocationManager.removeUpdates(pi);
            pi.cancel();
        }
    }

    public boolean isTrackingRun() {
        return getLocationPendingIntent(false) != null;
    }

    public boolean isTrackingRun(Run run) {
        return run != null && run.getId() == mCurrentRunId;
    }

    private void broadcastLocation(Location location) {
        Intent broadcast = new Intent(ACTION_LOCATION);
        broadcast.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
        mAppContext.sendBroadcast(broadcast);
    }

    public Run startTrackingRun(Run run) {
        if (run == null) {
            run = insertRun();
        }
        mCurrentRunId = run.getId();
        mSharedPreferences.edit().putLong(PREF_CURRENT_RUN_ID, mCurrentRunId).commit();
        startLocationUpdates();

        return run;
    }

    public void stopTrackingRun() {
        stopLocationUpdates();
        mSharedPreferences.edit().remove(PREF_CURRENT_RUN_ID).commit();
        mCurrentRunId = 0;
    }

    public void removeRun(long runId) {
        deleteLocation(runId);
        deleteRun(runId);
    }

    private Run insertRun() {
        Run run = new Run();
        run.setId(mRunDatabaseHelper.insertRun(run));
        return run;
    }

    private int deleteRun(long runId) {
        return mRunDatabaseHelper.deleteRun(runId);
    }

    private int deleteLocation(long runId) {
        return mRunDatabaseHelper.deleteLocation(runId);
    }

    public RunDatabaseHelper.RunCursor queryRuns() {
        return mRunDatabaseHelper.queryRuns();
    }

    public Run getRun(long runId) {
        Run run = null;
        RunDatabaseHelper.RunCursor runCursor = mRunDatabaseHelper.queryRun(runId);

        runCursor.moveToFirst();

        if (!runCursor.isAfterLast()) {
            run = runCursor.getRun();
        }

        runCursor.close();

        return run;
    }

    public void insertLocation(Location location) {
        if (isTrackingRun()) {
            mRunDatabaseHelper.insertLocation(mCurrentRunId, location);
        } else {
            Log.e(TAG, "Location received with no tracking run; ignoring.");
        }
    }

    public Location getLastLocationForRun(long runId) {
        Location location = null;
        RunDatabaseHelper.LocationCursor locationCursor=
                mRunDatabaseHelper.queryLastLocationForRun(runId);

        locationCursor.moveToFirst();

        if (!locationCursor.isAfterLast()) {
            location = locationCursor.getLocation();
        }

        locationCursor.close();

        return location;
    }
}
