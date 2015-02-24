package com.star.runtracker;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import java.util.Date;

public class RunDatabaseHelper extends SQLiteOpenHelper{

    private static final String DB_NAME = "runs.sqlite";
    private static final int VERSION = 1;

    private static final String TABLE_RUN = "run";
    private static final String COLUMN_RUN_ID = "_id";
    private static final String COLUMN_RUN_START_DATE = "start_date";

    private static final String TABLE_LOCATION = "location";
    private static final String COLUMN_LOCATION_ID = "_id";
    private static final String COLUMN_LOCATION_TIMESTAMP = "timestamp";
    private static final String COLUMN_LOCATION_LATITUDE = "latitude";
    private static final String COLUMN_LOCATION_LONGITUDE = "longitude";
    private static final String COLUMN_LOCATION_ALTITUDE = "altitude";
    private static final String COLUMN_LOCATION_PROVIDER = "provider";
    private static final String COLUMN_LOCATION_RUN_ID = "run_id";

    private static final String CREATE_TABLE_RUN =
            "CREATE TABLE " + TABLE_RUN + " (" +
                    COLUMN_RUN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_RUN_START_DATE + " INTEGER" + ")";

    private static final String CREATE_TABLE_LOCATION =
            "CREATE TABLE " + TABLE_LOCATION + " (" +
                    COLUMN_LOCATION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_LOCATION_TIMESTAMP + " INTEGER, " +
                    COLUMN_LOCATION_LATITUDE + " REAL, " +
                    COLUMN_LOCATION_LONGITUDE + " REAL, " +
                    COLUMN_LOCATION_ALTITUDE + " REAL, " +
                    COLUMN_LOCATION_PROVIDER + " VARCHAR(100), " +
                    COLUMN_LOCATION_RUN_ID + " INTEGER REFERENCES run(_id)" + ")";

    public RunDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_RUN);
        db.execSQL(CREATE_TABLE_LOCATION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long insertRun(Run run) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_RUN_START_DATE, run.getStartDate().getTime());

        return getWritableDatabase().insert(TABLE_RUN, null, contentValues);
    }

    public long insertLocation(long runId, Location location) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_LOCATION_TIMESTAMP, location.getTime());
        contentValues.put(COLUMN_LOCATION_LATITUDE, location.getLatitude());
        contentValues.put(COLUMN_LOCATION_LONGITUDE, location.getLongitude());
        contentValues.put(COLUMN_LOCATION_ALTITUDE, location.getAltitude());
        contentValues.put(COLUMN_LOCATION_PROVIDER, location.getProvider());
        contentValues.put(COLUMN_LOCATION_RUN_ID, runId);

        return getWritableDatabase().insert(TABLE_LOCATION, null, contentValues);
    }

    public int deleteRun(long runId) {
        return getWritableDatabase().delete(TABLE_RUN,
                COLUMN_RUN_ID + " = ?", new String[] {runId + ""});
    }

    public int deleteLocation(long runId) {
        return getWritableDatabase().delete(TABLE_LOCATION,
                COLUMN_LOCATION_RUN_ID + " = ?", new String[] {runId + ""});
    }

    public RunCursor queryRuns() {
        Cursor cursor = getReadableDatabase().query(
                TABLE_RUN,
                null,
                null,
                null,
                null,
                null,
                COLUMN_RUN_START_DATE + " ASC");

        return new RunCursor(cursor);
    }

    public RunCursor queryRun(long runId) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_RUN,
                null,
                COLUMN_RUN_ID + " = ?",
                new String[] {runId + ""},
                null,
                null,
                null,
                "1");

        return new RunCursor(cursor);
    }

    public LocationCursor queryLastLocationForRun(long runId) {
        Cursor cursor = getReadableDatabase().query(
                TABLE_LOCATION,
                null,
                COLUMN_LOCATION_RUN_ID + " = ?",
                new String[] {runId + ""},
                null,
                null,
                COLUMN_LOCATION_TIMESTAMP + " DESC",
                "1");

        return new LocationCursor(cursor);
    }

    public static class RunCursor extends CursorWrapper {

        public RunCursor(Cursor cursor) {
            super(cursor);
        }

        public Run getRun() {
            if (isBeforeFirst() || isAfterLast()) {
                return null;
            }

            Run run = new Run();

            long runId = getLong(getColumnIndex(COLUMN_RUN_ID));
            run.setId(runId);

            long startDate = getLong(getColumnIndex(COLUMN_RUN_START_DATE));
            run.setStartDate(new Date(startDate));

            return run;
        }
    }

    public static class LocationCursor extends CursorWrapper {

        public LocationCursor(Cursor cursor) {
            super(cursor);
        }

        public Location getLocation() {
            if (isBeforeFirst() || isAfterLast()) {
                return null;
            }

            String provider = getString(getColumnIndex(COLUMN_LOCATION_PROVIDER));

            Location location = new Location(provider);

            long time = getLong(getColumnIndex(COLUMN_LOCATION_TIMESTAMP));
            location.setTime(time);

            double latitude = getDouble(getColumnIndex(COLUMN_LOCATION_LATITUDE));
            location.setLatitude(latitude);

            double longitude = getDouble(getColumnIndex(COLUMN_LOCATION_LONGITUDE));
            location.setLongitude(longitude);

            double altitude = getDouble(getColumnIndex(COLUMN_LOCATION_ALTITUDE));
            location.setAltitude(altitude);

            return location;
        }
    }
}
