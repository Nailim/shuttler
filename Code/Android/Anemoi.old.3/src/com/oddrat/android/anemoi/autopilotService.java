package com.oddrat.android.anemoi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
    public static final int MSG_SET_SERVICE_STATE = 5;
    public static final int MSG_GPS = 6;
    
	//private NotificationManager mNM	// investigate later													// For showing and hiding notifications
	
    private static ArrayList<Messenger> mClients = new ArrayList<Messenger>();			// Keeps track of all current registered clients

    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));		// Target for clients
	
    private static Boolean isAutopilotServiceRunning = false;
    private static Boolean isAutopilotServicePreparing = false;
    
    static final String ACTION_FOREGROUND = "com.oddrat.android.anemoi.FOREGROUND";
    static final String ACTION_BACKGROUND = "com.oddrat.android.anemo.BACKGROUND";

    private static final Class<?>[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
    private static final Class<?>[] mStopForegroundSignature = new Class[] {boolean.class};
    
    private LocationManager mlocationManager;
    //private LocationListener mLocationListener_preStart;
    
    private Method mStartForeground;
    private Method mStopForeground;
    
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    
    private static String data_session_folder;
    private static int data_flightgear_address;
    private static int data_flightgear_port;
    
    private Writer gpsCSV;
    
    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
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
	                	int mValue = msg.arg1;
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
	                    //removeMessages(autopilotService.MSG_IS_SERVICE_RUNNING);
	                    break;
	                case MSG_SET_SERVICE_STATE:
	                	Bundle bundle_MSG_SET_SERVICE_STATE = msg.getData();
	                	isAutopilotServiceRunning = bundle_MSG_SET_SERVICE_STATE.getBoolean("setServiceState");
	                	
	                	if (isAutopilotServiceRunning) {
	                		data_session_folder = bundle_MSG_SET_SERVICE_STATE.getString(Flight.MESSAGE_DATA_SESSION_FOLDER);
	                		isAutopilotServicePreparing = false;
	                		
	                		/// !!! handle
	                		try {
	                			gpsCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/gps.csv")));
	                		} catch (IOException e) {
	                			// TODO Auto-generated catch block
	                			e.printStackTrace();
	                		}
	                		
		                		mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, mLocationListener_Start);
		                		try {
		                			mlocationManager.removeUpdates(mLocationListener_preStart);
	                	    	} catch (IllegalArgumentException e) {
	                	    		Log.i("anemoi", "IllegalArgumentException");
	                	    	}

	                	}
//	                	else {
//	                		
//	                	}
	                	
	                	break;
	                default:
	                    super.handleMessage(msg);
	            }
	    	}
        }
    }
    
    /** Life cycle methods */
	/** Called when the service is first created. */
    @Override
    public void onCreate() {
        //mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
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
        // Set the icon, scrolling text and time stamp
        Notification notification = new Notification(R.drawable.icon_autopilot, text, System.currentTimeMillis());
        // The PendingIntent to launch our activity if the user selects this notification
        Intent intent = new Intent(this, Flight.class);
        String message = "service";
    	intent.putExtra(PreFlight.MESSAGE_ACTIVITY, message);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.autopilot_service), text, contentIntent);
        
        mStartForegroundArgs[0] = Integer.valueOf(R.string.autopilot_service);
        mStartForegroundArgs[1] = notification;
        try {
            mStartForeground.invoke(this, mStartForegroundArgs);
        } catch (InvocationTargetException e) {
            // Should not happen.
            //Log.w("ApiDemos", "Unable to invoke startForeground", e);
        	Log.i("anemoi", "InvocationTargetException");
        } catch (IllegalAccessException e) {
            // Should not happen.
        	Log.i("anemoi", "IllegalAccessException");
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // start stuff here
    	//isAutopilotServiceRunning = true;
    	isAutopilotServicePreparing = true;
    	
    	if (intent.getIntExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT_TYPE, 0) == 0) {
    		// do android stuff
    		mlocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    		//mLocationListener_preStart = new gpsAndroidListener_preStart();
    		mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, mLocationListener_preStart);
			
    		Bundle bundle = new Bundle();
			Message msgOut =  Message.obtain(null, MSG_GPS, 0, 0);
			
			bundle.putString("gps", "gps: listener ready");
			msgOut.setData(bundle);
			
			for (int i=mClients.size()-1; i>=0; i--) {
				try {
            		mClients.get(i).send(msgOut);
            	} catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mClients.remove(i);
                }
            }
    	}
    	else if (intent.getIntExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT_TYPE, 0) == 1) {
    		// do flightgear stuff
    		data_flightgear_port = Integer.parseInt(intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_PORT));
    		
    		gpsFlightGearListener.start();
    	}
    	
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    


    @Override
    public void onDestroy() {    	
    	super.onDestroy();
    	
    	if (isAutopilotServicePreparing) {
	    	try {
	    	mlocationManager.removeUpdates(mLocationListener_preStart);
	    	mlocationManager.removeUpdates(mLocationListener_Start);
	    	} catch (IllegalArgumentException e) {
	    		Log.i("anemoi", "IllegalArgumentException");
	    	}
	    	
    	}
    	
    	if (isAutopilotServiceRunning) {
	    	try {
				gpsCSV.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	
    	//isAutopilotServiceRunning = false;
    	isAutopilotServicePreparing = false;
    	
    	for (int i=mClients.size()-1; i>=0; i--) {
    		mClients.remove(i);
    	}
        
    	mStopForegroundArgs[0] = Boolean.TRUE;
         try {
             mStopForeground.invoke(this, mStopForegroundArgs);
         } catch (InvocationTargetException e) {
             // Should not happen.
        	 Log.i("anemoi", "InvocationTargetException");
         } catch (IllegalAccessException e) {
        	 // Should not happen.
        	 Log.i("anemoi", "IllegalAccessException");
         }
    }
    
    /** The rest of the lot */
    
    /**When binding to the service, we return an interface to our messenger for sending messages to the service. */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    
    /** threads */
    // flightgear GSP listener
    private Thread gpsFlightGearListener = new Thread() {
    	
		public void run() {
			Log.i("anemoi", Integer.toString(data_flightgear_port));
			
			Bundle bundle = new Bundle();
			bundle.putString("gps", "gps: listener ready");
			
			Message msgOut =  Message.obtain(null, MSG_GPS, 0, 0);
			msgOut.setData(bundle);
			for (int i=mClients.size()-1; i>=0; i--) {
				try {
            		mClients.get(i).send(msgOut);
            	} catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mClients.remove(i);
                }
            }
			
			try {
				ServerSocket gpsReceiverSocket = new ServerSocket(data_flightgear_port+1);
				
				Socket gpsClient = gpsReceiverSocket.accept();
				Log.i("anemoi", "got one");
				BufferedReader gpsNetworkIn = new BufferedReader(new InputStreamReader(gpsClient.getInputStream()));
				
				while (isAutopilotServicePreparing) {
					bundle = new Bundle();
					
					bundle.putString("gps", gpsNetworkIn.readLine() + "\tacc=1.0");
					
					msgOut =  Message.obtain(null, MSG_GPS, 0, 0);
					msgOut.setData(bundle);
					for (int i=mClients.size()-1; i>=0; i--) {
						try {
	                		mClients.get(i).send(msgOut);
	                	} catch (RemoteException e) {
	                        // The client is dead.  Remove it from the list;
	                        // we are going through the list from back to front
	                        // so this is safe to do inside the loop.
	                        mClients.remove(i);
	                    }
	                }
					
				}
				
				bundle = new Bundle();
				bundle.putString("gps", "gps: ok");
				
				msgOut =  Message.obtain(null, MSG_GPS, 0, 0);
				msgOut.setData(bundle);
				for (int i=mClients.size()-1; i>=0; i--) {
					try {
	            		mClients.get(i).send(msgOut);
	            	} catch (RemoteException e) {
	                    // The client is dead.  Remove it from the list;
	                    // we are going through the list from back to front
	                    // so this is safe to do inside the loop.
	                    mClients.remove(i);
	                }
	            }
				
				
				
				long currentTime;
				String gpsString;
				String[] separatedGSPString;
				
				while (isAutopilotServiceRunning) {
					// !!! debug, remove later
					bundle = new Bundle();
					msgOut =  Message.obtain(null, MSG_GPS, 0, 0);
					
					// gps stuff
					gpsString = gpsNetworkIn.readLine();
					currentTime = System.currentTimeMillis();
					
					separatedGSPString = gpsString.split("[=\\t\\n]");	// odd ones have data: lon, lat, alt, spd
					gpsCSV.write(Long.toString(currentTime) + "," + separatedGSPString[1] + "," + separatedGSPString[3] + "," + separatedGSPString[5] + "," + separatedGSPString[7] + "," + separatedGSPString[9] + "\n");
					
					
					
					// !!! debug, remove later
					bundle.putString("gps",gpsString);
					msgOut.setData(bundle);
					for (int i=mClients.size()-1; i>=0; i--) {
						try {
	                		mClients.get(i).send(msgOut);
	                	} catch (RemoteException e) {
	                        // The client is dead.  Remove it from the list;
	                        // we are going through the list from back to front
	                        // so this is safe to do inside the loop.
	                        mClients.remove(i);
	                    }
	                }
					
				}
				
				gpsClient.close();
				gpsReceiverSocket.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
    };
    
    private LocationListener mLocationListener_preStart = new LocationListener() {
    //private class gpsAndroidListener_preStart implements LocationListener {
    	Bundle bundle;
    	Message msgOut;
    	
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			Log.i("anemoi", "preStart new location!");
			bundle = new Bundle();
			msgOut =  Message.obtain(null, MSG_GPS, 0, 0);			
			
			bundle.putString("gps", "lat=" + location.getLatitude() + "\t" + "lon=" + location.getLongitude() + "\t" + "alt=" + location.getAltitude() + "\t" + "spd=" + location.getSpeed() + "\t" + "acc=" + location.getAccuracy() + "\n");
			msgOut.setData(bundle);
			
			for (int i=mClients.size()-1; i>=0; i--) {
				try {
            		mClients.get(i).send(msgOut);
            	} catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mClients.remove(i);
                }
            }
		}

		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
    	
    };
    
    private LocationListener mLocationListener_Start = new LocationListener() {
        	Bundle bundle;
        	Message msgOut;
        	
        	long currentTime;
        	
    		public void onLocationChanged(Location location) {
    			currentTime = System.currentTimeMillis();
    			Log.i("anemoi", "Start new location!");
    			bundle = new Bundle();
    			msgOut =  Message.obtain(null, MSG_GPS, 0, 0);
    			
    			try {
					gpsCSV.write(Long.toString(currentTime) + "," + location.getLatitude() + "," + location.getLongitude() + "," + location.getAltitude() + "," + location.getSpeed() + "," + location.getAccuracy() + "\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			
    			
    			bundle.putString("gps", ":lat=" + location.getLatitude() + "\t" + "lon=" + location.getLongitude() + "\t" + "alt=" + location.getAltitude() + "\t" + "spd=" + location.getSpeed() + "\t" + "acc=" + location.getAccuracy() + "\n");
    			msgOut.setData(bundle);
    			
    			for (int i=mClients.size()-1; i>=0; i--) {
    				try {
                		mClients.get(i).send(msgOut);
                	} catch (RemoteException e) {
                        // The client is dead.  Remove it from the list;
                        // we are going through the list from back to front
                        // so this is safe to do inside the loop.
                        mClients.remove(i);
                    }
                }
    		}

    		public void onProviderDisabled(String provider) {
    			// TODO Auto-generated method stub
    			
    		}

    		public void onProviderEnabled(String provider) {
    			// TODO Auto-generated method stub
    			
    		}

    		public void onStatusChanged(String provider, int status, Bundle extras) {
    			// TODO Auto-generated method stub
    			
    		}
        	
        };
}
