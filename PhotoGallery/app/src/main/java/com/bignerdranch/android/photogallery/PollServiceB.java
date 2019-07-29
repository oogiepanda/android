package com.bignerdranch.android.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollServiceB extends IntentService {
    private static final String TAG = "PollServiceB";

    private static final long POLL_INTERVAL_S = TimeUnit.MINUTES.toMillis(1);

    public PollServiceB() {
        super(TAG);
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, PollServiceB.class);
    }

    public static void setAlarm(Context context, boolean isOn) {
        Intent i = PollServiceB.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isOn) {
            alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_S, pi);
        } else {
            alarm.cancel(pi);
            pi.cancel();
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (!isNetworkAvailable()) return;
        Log.i(TAG, "Breceived intent: " + intent);
        String query = QueryPreferences.getLastResultId(this);


        List<GalleryItem> items = (query == null)
                ? new FlickrFetchr().fetchRecentPhotos()
                : new FlickrFetchr().searchPhotos(query);
        if (items.isEmpty()) return;

        String id = items.get(0).getId();
        String lastId = QueryPreferences.getLastResultId(this);
        Log.i(TAG, (id.equals(lastId)) ? " old Result: " : "new Result: " + id);
        QueryPreferences.setLastResultId(this, id);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
