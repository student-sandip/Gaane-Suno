package com.example.gaanesuno;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

//public class NotificationActionReceiver extends BroadcastReceiver {
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        String action = intent.getAction();
//
//        Intent serviceIntent = new Intent(context, MusicService.class);
//        serviceIntent.setAction(action);
//
//        // Start the MusicService with the action (defined in MusicService)
//        context.startService(serviceIntent);
//    }
//}


public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Intent serviceIntent = new Intent(context, MusicService.class);

        if ("com.example.gaanesuno.ACTION_PREV".equals(action)) {
            serviceIntent.setAction("ACTION_PREV");
        } else if ("com.example.gaanesuno.ACTION_PLAY_PAUSE".equals(action)) {
            serviceIntent.setAction("ACTION_PLAY_PAUSE");
        } else if ("com.example.gaanesuno.ACTION_NEXT".equals(action)) {
            serviceIntent.setAction("ACTION_NEXT");
        } else if ("com.example.gaanesuno.ACTION_CLOSE".equals(action)) {
            serviceIntent.setAction("ACTION_CLOSE");
        } else {
            return; // Unrecognized action
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}

