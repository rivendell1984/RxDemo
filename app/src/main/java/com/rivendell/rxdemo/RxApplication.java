package com.rivendell.rxdemo;

import android.app.Application;

import com.rivendell.rxdemo.utils.ThreadAwareDebugTree;

import timber.log.Timber;

/**
 * Created by Rivendell on 2017/8/18.
 */

public class RxApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new ThreadAwareDebugTree());
        }
    }
}
