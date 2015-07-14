package com.joostvdoorn.glutenvrij;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by joostvandoorn on 14/07/15.
 */
public class TrackedActivity extends Activity {

    private Tracker mTracker;

    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState, String name) {
        super.onCreate(savedInstanceState);
        // Obtain the shared Tracker instance.
        GlutenvrijApplication application = (GlutenvrijApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName(name);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void trackEvent(String category, String action, String label, long value) {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());

    }
}
