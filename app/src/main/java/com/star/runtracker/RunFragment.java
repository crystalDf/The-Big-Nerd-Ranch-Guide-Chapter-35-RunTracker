package com.star.runtracker;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RunFragment extends Fragment {

    public static final String EXTRA_RUN_ID = "com.star.runtracker.run_id";

    private static final int LOAD_RUN = 1;
    private static final int LOAD_LOCATION = 2;

    private Button mStartButton, mStopButton;
    private TextView mStartedTextView, mLatitudeTextView, mLongitudeTextView,
            mAltitudeTextView, mDurationTextView;

    private RunManager mRunManager;

    private Run mRun;

    private Location mLastLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mRunManager = RunManager.getInstance(getActivity());

        long runId = getActivity().getIntent().getLongExtra(EXTRA_RUN_ID, 0);

        if (runId != 0) {
            LoaderManager loaderManager = getLoaderManager();
            loaderManager.initLoader(LOAD_RUN, null, mRunLoaderCallbacks);
            loaderManager.initLoader(LOAD_LOCATION, null, mLocationLoaderCallbacks);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_run, container, false);

        mStartedTextView = (TextView) v.findViewById(R.id.run_startedTextView);
        mLatitudeTextView = (TextView) v.findViewById(R.id.run_latitudeTextView);
        mLongitudeTextView = (TextView) v.findViewById(R.id.run_longitudeTextView);
        mAltitudeTextView = (TextView) v.findViewById(R.id.run_altitudeTextView);
        mDurationTextView = (TextView) v.findViewById(R.id.run_durationTextView);

        mStartButton = (Button) v.findViewById(R.id.run_StartButton);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRun = mRunManager.startTrackingRun(mRun);

                updateUI();
            }
        });

        mStopButton = (Button) v.findViewById(R.id.run_StopButton);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRunManager.stopTrackingRun();

                updateUI();
            }
        });

        updateUI();

        return v;
    }

    private void updateUI() {
        boolean started = mRunManager.isTrackingRun();
        boolean trackingThisRun = mRunManager.isTrackingRun(mRun);

        mStartButton.setEnabled(!started);
        mStopButton.setEnabled(trackingThisRun);

        if (mRun != null) {
            mStartedTextView.setText(mRun.getFormattedDate());
        }

        int durationSeconds = 0;

        if (mRun != null && mLastLocation != null) {
            durationSeconds = mRun.getDurationSeconds(mLastLocation.getTime());
            mLatitudeTextView.setText(Double.toString(mLastLocation.getLatitude()));
            mLongitudeTextView.setText(Double.toString(mLastLocation.getLongitude()));
            mAltitudeTextView.setText(Double.toString(mLastLocation.getAltitude()));
        }

        mDurationTextView.setText(Run.formatDuration(durationSeconds));
    }

    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {
        @Override
        protected void onLocationReceived(Context context, Location location) {
            // super.onLocationReceived(context, location);

            if (!mRunManager.isTrackingRun(mRun)) {
                return;
            }

            mLastLocation = location;
            if (isVisible()) {
                updateUI();
            }
        }

        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            // super.onProviderEnabledChanged(enabled);

            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mLocationReceiver,
                new IntentFilter(RunManager.ACTION_LOCATION));

        long runId = getActivity().getIntent().getLongExtra(EXTRA_RUN_ID, 0);

        if (runId != 0) {
            mRun = mRunManager.getRun(runId);
            mLastLocation = mRunManager.getLastLocationForRun(mRun.getId());
        }
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mLocationReceiver);
        super.onStop();
    }

    private static class RunLoader extends DataLoader<Run> {

        private long mRunId;

        public RunLoader(Context context, long runId) {
            super(context);
            mRunId = runId;
        }

        @Override
        public Run loadInBackground() {
            return RunManager.getInstance(getContext())
                    .getRun(mRunId);
        }
    }

    private LoaderManager.LoaderCallbacks<Run> mRunLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<Run>() {
                @Override
                public Loader<Run> onCreateLoader(int id, Bundle args) {
                    return new RunLoader(getActivity(),
                            getActivity().getIntent().getLongExtra(EXTRA_RUN_ID, 0));
                }

                @Override
                public void onLoadFinished(Loader<Run> loader, Run data) {
                    mRun = data;
                    updateUI();
                }

                @Override
                public void onLoaderReset(Loader<Run> loader) {

                }
            };

    private static class LastLocationLoader extends DataLoader<Location> {

        private long mRunId;

        public LastLocationLoader(Context context, long runId) {
            super(context);
            mRunId = runId;
        }

        @Override
        public Location loadInBackground() {
            return RunManager.getInstance(getContext())
                    .getLastLocationForRun(mRunId);
        }
    }

    private LoaderManager.LoaderCallbacks<Location> mLocationLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<Location>() {
                @Override
                public Loader<Location> onCreateLoader(int id, Bundle args) {
                    return new LastLocationLoader(getActivity(),
                            getActivity().getIntent().getLongExtra(EXTRA_RUN_ID, 0));
                }

                @Override
                public void onLoadFinished(Loader<Location> loader, Location data) {
                    mLastLocation = data;
                    updateUI();
                }

                @Override
                public void onLoaderReset(Loader<Location> loader) {

                }
            };
}
