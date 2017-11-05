package com.oddrat.android.anemoi;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class autopilotService extends Service{
	/** Used variables */
	
	/** Commands to the service.*/
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_ECHO = 3;
    public static final int MSG_IS_SERVICE_RUNNING = 4;
    
	private NotificationManager mNM;											// For showing and hiding notifications
	
    private static ArrayList<Messenger> mClients = new ArrayList<Messenger>();			// Keeps track of all current registered clients
    
    private static int mValue = 0;														// Holds last value set by a client

    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));	// Target for clients
	
    private static Boolean isAutopilotServiceRunning = false;
    
    static final String ACTION_FOREGROUND = "com.oddrat.android.anemoi.FOREGROUND";
    static final String ACTION_BACKGROUND = "com.oddrat.android.anemo.BACKGROUND";

    private static final Class<?>[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
    private static final Class<?>[] mStopForegroundSignature = new Class[] {boolean.class};
    
    private Method mStartForeground;
    private Method mStopForeground;
    
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];

    
    /**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
    	private final WeakReference<autopilotService> mTarget;
		
		IncomingHandler(autopilotService target) {
			mTarget = new WeakReference<autopilotService>(target);
	    }
		
    	@Override
        public void handleMessage(Message msg) {
    		autopilotService target = mTarget.get(); 
	        
	    	if (target != null){
	            switch (msg.what) {
	                case MSG_REGISTER_CLIENT:
	                    mClients.add(msg.replyTo);
	                    break;
	                case MSG_UNREGISTER_CLIENT:
	                    mClients.remove(msg.replyTo);
	                    break;
	                case MSG_ECHO:
	                    mValue = msg.arg1;
	                    for (int i=mClients.size()-1; i>=0; i--) {
	                        try {
	                            mClients.get(i).send(Message.obtain(null, MSG_ECHO, mValue, 0));
	                        } catch (RemoteException e) {
	                            // The client is dead.  Remove it from the list;
	                            // we are going through the list from back to front
	                            // so this is safe to do inside the loop.
	                            mClients.remove(i);
	                        }
	                    }
	                    break;
	                case MSG_IS_SERVICE_RUNNING:
	                	Bundle bundle = new Bundle();
	                	bundle.putBoolean("isAutopilotServiceRunning", isAutopilotServiceRunning);

                    	Message msgOut =  Message.obtain(null, MSG_IS_SERVICE_RUNNING, 0, 0);
                    	msgOut.setData(bundle);
                    	
	                	for (int i=mClients.size()-1; i>=0; i--) {
	                    	
	                    	
	                    	Log.i("anemoi", "Here 1.1");
	                    	try {
	                    		mClients.get(i).send(msgOut);
	                    	} catch (RemoteException e) {
	                            // The client is dead.  Remove it from the list;
	                            // we are going through the list from back to front
	                            // so this is safe to do inside the loop.
	                            mClients.remove(i);
	                        }
	                    }
	                    removeMessages(autopilotService.MSG_IS_SERVICE_RUNNING);
	                    break;
	                default:
	                    super.handleMessage(msg);
	            }
	    	}
        }
    }
    
    /** Lifecycle methods */
	/** Called when the service is first created. */
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        try {
            mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
        	// Should not happen
            // Running on an older platform
            mStartForeground = mStopForeground = null;
        }
        
        /** Notification stuff */
        CharSequence text = getText(R.string.autopilot_service_started);
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon_autopilot, text, System.currentTimeMillis());
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Flight.class), 0);
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.autopilot_service), text, contentIntent);
        
        mStartForegroundArgs[0] = Integer.valueOf(R.string.autopilot_service);
        mStartForegroundArgs[1] = notification;
        try {
            mStartForeground.invoke(this, mStartForegroundArgs);
        } catch (InvocationTargetException e) {
            // Should not happen.
            //Log.w("ApiDemos", "Unable to invoke startForeground", e);
        } catch (IllegalAccessException e) {
            // Should not happen.
            //Log.w("ApiDemos", "Unable to invoke startForeground", e);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // start stuff here
    	isAutopilotServiceRunning = true;
    	
    	
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    


    @Override
    public void onDestroy() {    	
    	 mStopForegroundArgs[0] = Boolean.TRUE;
         try {
             mStopForeground.invoke(this, mStopForegroundArgs);
         } catch (InvocationTargetException e) {
             // Should not happen.
             //Log.w("ApiDemos", "Unable to invoke stopForeground", e);
         } catch (IllegalAccessException e) {
        	 // Log.w("ApiDemos", "Unable to invoke stopForeground", e);
             // Should not happen.
         }
         isAutopilotServiceRunning = false;
    }
    
    /** The rest of the lot */
    
    /**When binding to the service, we return an interface to our messenger for sending messages to the service. */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}
