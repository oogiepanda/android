package com.bignerdranch.android.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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

    public static boolean isAlarmOn(Context context) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!isNetworkAvailable()) return;
        Log.i(TAG, "Breceived intent: " + intent);
        String query = QueryPreferences.getStoredQuery(this);


        List<GalleryItem> items = (query == null)
                ? new FlickrFetchr().fetchRecentPhotos()
                : new FlickrFetchr().searchPhotos(query);
        if (items.isEmpty()) return;

        String id = items.get(0).getId();
        String lastId = QueryPreferences.getLastResultId(this);
        Log.i(TAG, ((id.equals(lastId)) ? " old Result: " : "new Result: ") + id);

        if(!id.equals(lastId)) {
            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            NotificationManagerCompat.from(this).notify(0, notification);

        }

        QueryPreferences.setLastResultId(this, id);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
