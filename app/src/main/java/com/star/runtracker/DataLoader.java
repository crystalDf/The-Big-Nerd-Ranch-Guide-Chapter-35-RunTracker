package com.star.runtracker;


import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class DataLoader<D> extends AsyncTaskLoader<D> {

    private D mD;

    public DataLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        if (mD != null) {
            deliverResult(mD);
        } else {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(D data) {
        mD = data;

        if (isStarted()) {
            super.deliverResult(data);
        }
    }
}
