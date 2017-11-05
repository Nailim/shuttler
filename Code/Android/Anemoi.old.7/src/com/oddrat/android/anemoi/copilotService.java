package com.oddrat.android.anemoi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public class copilotService extends Service{
	
	/** Used variables */
	
	private NotificationManager mNM;										// For showing and hiding notifications
	
	private final IBinder mBinder = new LocalBinder();						// Service binding object
	
	private Boolean isCopilotServiceRunning = false;
	private Boolean isCopilotServicePreparing = false;
	
	private Boolean isCopilotActive = false;
	private Boolean isCopilotRunning = false;
	
	private String data_session_folder;
	
	/** Called when the service is first created. */
	@Override
	 public void onCreate() {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		showNotification();
	}
	
	/** Called when the service is started. */
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		//isCopilotServiceRunning = true;
		isCopilotServicePreparing = true;
		
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

	/** Called when the service is destroyed. */
	@Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.copilot_service);
        
        //isCopilotServiceRunning = false;
        isCopilotServiceRunning = false;
    }
	
	/** The rest of the lot */
	public Boolean isCopilotServiceRunning() {
		return isCopilotServiceRunning;
	}
	
	public void setServiceState(Bundle bundle) {
		isCopilotServiceRunning = bundle.getBoolean("setServiceState");
		
		if (isCopilotServiceRunning) {
    		data_session_folder = bundle.getString(Flight.MESSAGE_DATA_SESSION_FOLDER);
    		isCopilotServicePreparing = false;
    	}
	}
	
	public Boolean isCopilotActive() {
		return isCopilotActive;
	}
	
	public Boolean isCopilotRunning() {
		return isCopilotRunning;
	}
	
    /** When binding to the service, we return an interface to our messenger for sending messages to the service. */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	/** Class for clients to access. */
	public class LocalBinder extends Binder {
		 copilotService getService() {
			 return copilotService.this;
		 }
	}
	
	/**
     * Show a notification while this service is running.
     */
	private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.copilot_service_started);

        // Set the icon, scrolling text and time-stamp
        Notification notification = new Notification(R.drawable.icon_copilot, null, System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        Intent intent = new Intent(this, Flight.class);
        String message = "service";
    	intent.putExtra(PreFlight.MESSAGE_ACTIVITY, message);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.copilot_service), text, contentIntent);
        
        // Set the notification to ongoing event
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        
        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.copilot_service, notification);
    }
}