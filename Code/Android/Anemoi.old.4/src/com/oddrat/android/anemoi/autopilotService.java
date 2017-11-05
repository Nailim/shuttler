package com.oddrat.android.anemoi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
    public static final int MSG_ORI = 7;
    
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
 	private SensorManager mSensorManager;
    
    private static Boolean isActive_mLocationListener_preStart = false;
    private static Boolean isActive_mLocationListener_Start = false;
    private static Boolean isActive_mEventListenerOriantation = false;
    
    private Method mStartForeground;
    private Method mStopForeground;
    
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    
    private static String data_session_folder;
    private static String data_flightgear_address;
    private static int data_flightgear_port;
    
    private static int settings_autopilot_type = -1;
    
    private static Writer gpsCSV;
    private static Writer oriCSV;
    
    private static Boolean isOpen_file_gpsCSV = false;
    private static Boolean isOpen_file_oriCSV = false;
    
    private static double testOut = 1.0;
    
    private static PrintWriter networkOut;		/// !!! handle - close
    private static Socket sendSocket = null;	/// !!! handle - close
    
    private static double outRudder = 0.0;
    private static double outAileron = 0.0;
    
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
	                	
	                	if (isAutopilotServiceRunning) {	// turn on auto pilot
	                		Log.i("anemoi", "autopilot on");
	                		data_session_folder = bundle_MSG_SET_SERVICE_STATE.getString(Flight.MESSAGE_DATA_SESSION_FOLDER);
	                		
	                		isAutopilotServicePreparing = false;
	                		
	                		// general
	                		try {
	                			gpsCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/gps.csv")));
	                			isOpen_file_gpsCSV = true;	// semaphore
	                			oriCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/ori.csv")));
	                			isOpen_file_oriCSV = true;	// semaphore
	                		} catch (IOException e) {
	                			// TODO Auto-generated catch block
	                			e.printStackTrace();
	                		}
	                		
	                		// type specific
	                		if (settings_autopilot_type == 0) {
	                    		// do android stuff
		                		mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, mLocationListener_Start);
		                		isActive_mLocationListener_Start = true;
		                		try {
		                			mlocationManager.removeUpdates(mLocationListener_preStart);
		                			isActive_mLocationListener_preStart = false;
	                	    	} catch (IllegalArgumentException e) {
	                	    		Log.i("anemoi", "IllegalArgumentException");
	                	    	}
		                		
		                		mSensorManager.registerListener(mSensorEventListenerOrientation, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
		                		isActive_mEventListenerOriantation = true;
	                		}
	                		
	                		/// !!!
	                		if (settings_autopilot_type == 1) {
	                			FlightGearSender.start();
	                		}

	                	}
	                	else {	// turn off autopilot and prepare to finish
	                		/// !!! handle shutting off more gently (like unregistering listeners and such)
	                		// GPS no more
	                		if (isActive_mLocationListener_preStart) {
	                	    	try {
	                	    		mlocationManager.removeUpdates(mLocationListener_preStart);
	                	    		isActive_mLocationListener_preStart = false;
	                	    	} catch (IllegalArgumentException e) {
	                	    		Log.i("anemoi", "IllegalArgumentException");
	                	    	}
	                    	}
	                    	
	                    	if (isActive_mLocationListener_Start) {
	                	    	try {
	                	    		mlocationManager.removeUpdates(mLocationListener_Start);
	                	    		isActive_mLocationListener_Start = false;
	                	    	} catch (IllegalArgumentException e) {
	                	    		Log.i("anemoi", "IllegalArgumentException");
	                	    	}
	                    	}
	                    	
	                    	if (isActive_mEventListenerOriantation) {
	                	    	mSensorManager.unregisterListener(mSensorEventListenerOrientation);
	                    		isActive_mEventListenerOriantation = false;
	                    	}
	                    	
	                    	fg.cancel();	/// !!!
	                    	
	                    	Log.i("anemoi", "autopilot off");
	                    	// sleep till everything finishes
	                    	try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
							}
	                    	Log.i("anemoi", "autopilot off and done waiting");
	                	}
	                	
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
        Notification notification = new Notification(R.drawable.icon_autopilot, null, System.currentTimeMillis());
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
    	
    	settings_autopilot_type = intent.getIntExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT_TYPE, 0);
    	
    	if (settings_autopilot_type == 0) {
    		// do android stuff
    		mlocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    		
    		mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, mLocationListener_preStart);
    		isActive_mLocationListener_preStart = true;
    		
    		Bundle bundle = new Bundle();
			Message msgOutGPS =  Message.obtain(null, MSG_GPS, 0, 0);
			Message msgOutORI =  Message.obtain(null, MSG_ORI, 0, 0);
			
			bundle.putString("gps", "gps: waiting for lock");
			msgOutGPS.setData(bundle);
			bundle.putString("ori", "ori: waiting for autopilot start");
			msgOutORI.setData(bundle);
			
			for (int i=mClients.size()-1; i>=0; i--) {
				try {
            		mClients.get(i).send(msgOutGPS);
            		mClients.get(i).send(msgOutORI);
            	} catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mClients.remove(i);
                }
            }
    	}
    	else if (settings_autopilot_type == 1) {
    		// do flightgear stuff
    		data_flightgear_port = Integer.parseInt(intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_PORT));
    		data_flightgear_address = intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_ADDRESS);
    		
    		gpsFlightGearListener.start();
    		oriFlightGearListener.start();
    		
    		//FlightGearSender.start(); /// !!!
    	}
    	
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    


    @Override
    public void onDestroy() {    	
    	super.onDestroy();
    	Log.i("anemoi", "autopilot on destroy");
    	isAutopilotServicePreparing = false;
    	isAutopilotServiceRunning = false;
    	
    	if (isActive_mLocationListener_preStart) {
	    	try {
	    		mlocationManager.removeUpdates(mLocationListener_preStart);
	    		isActive_mLocationListener_preStart = false;
	    	} catch (IllegalArgumentException e) {
	    		Log.i("anemoi", "IllegalArgumentException");
	    	}
    	}
    	
    	if (isActive_mLocationListener_Start) {
	    	try {
	    		mlocationManager.removeUpdates(mLocationListener_Start);
	    		isActive_mLocationListener_Start = false;
	    	} catch (IllegalArgumentException e) {
	    		Log.i("anemoi", "IllegalArgumentException");
	    	}
    	}
    	
    	if (isActive_mEventListenerOriantation) {
	    	mSensorManager.unregisterListener(mSensorEventListenerOrientation);
    		isActive_mEventListenerOriantation = false;
    	}
    	
    	if (isOpen_file_gpsCSV) {
	    	try {
				gpsCSV.close();
				isOpen_file_gpsCSV = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	
    	if (isOpen_file_oriCSV) {
	    	try {
				oriCSV.close();
				isOpen_file_oriCSV = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	
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
				
				long currentTime;
				String gpsString;
				String[] separatedGSPString;
				
				while (isAutopilotServicePreparing) {
					gpsString = gpsNetworkIn.readLine();
					
					Log.i("anemoi", gpsString);
					
					bundle = new Bundle();
					
					bundle.putString("gps", "gps: " + gpsString + "\tacc=1.0");
					
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
				
				while (isAutopilotServiceRunning) {
					// !!! debug, remove later
					bundle = new Bundle();
					msgOut =  Message.obtain(null, MSG_GPS, 0, 0);
					
					// gps stuff
					gpsString = gpsNetworkIn.readLine();
					currentTime = System.currentTimeMillis();
					
					separatedGSPString = gpsString.split("[=\\t\\n]");	// odd ones have data: lan, lot, alt, hdg, spd, acc
					
					try {
						gpsCSV.write(Long.toString(currentTime) + "," + separatedGSPString[1] + "," + separatedGSPString[3] + "," + separatedGSPString[5] + "," + "0.0," + separatedGSPString[7] + "," + separatedGSPString[9] + "," + separatedGSPString[11] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					// !!! debug, remove later
					bundle.putString("gps","gps: " + gpsString);
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
    
 // flightgear GSP listener
    private Thread oriFlightGearListener = new Thread() {
    	
		public void run() {
			Log.i("anemoi", Integer.toString(data_flightgear_port));
			
			Bundle bundle = new Bundle();
			bundle.putString("ori", "ori: listener ready");
			
			Message msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
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
				ServerSocket gpsReceiverSocket = new ServerSocket(data_flightgear_port+2);
				
				Socket gpsClient = gpsReceiverSocket.accept();
				Log.i("anemoi", "got one");
				BufferedReader gpsNetworkIn = new BufferedReader(new InputStreamReader(gpsClient.getInputStream()));
				
				long currentTime;
				String oriString;
				String[] separatedORIString;
				
				while (isAutopilotServicePreparing) {
					oriString = gpsNetworkIn.readLine();
					
					bundle = new Bundle();
					
					bundle.putString("ori", "ori: " + oriString);
					
					msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
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
				bundle.putString("ori", "ori: ok");
				
				msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
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
				
				double e_n = 0;
				double y_n = 0;
				double r_n = 0;
				double Kp = 0.1111;
				
				while (isAutopilotServiceRunning) {
					// !!! debug, remove later
					bundle = new Bundle();
					msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
					
					// gps stuff
					oriString = gpsNetworkIn.readLine();
					currentTime = System.currentTimeMillis();
					/// !! handly if null or something
					separatedORIString = oriString.split("[=\\t\\n]");	// odd ones have data: yaw pitch roll
					
					try {
						oriCSV.write(Long.toString(currentTime) + "," + separatedORIString[1] + "," + separatedORIString[3] + "," + separatedORIString[5] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					/// P-Controller
					y_n = Double.parseDouble(separatedORIString[5]);
					e_n = r_n - y_n;
					outAileron = e_n * Kp;
					outRudder = (r_n - Double.parseDouble(separatedORIString[3])) * -0.019;
					//Log.i("anemoi", "ori: " + Double.parseDouble(separatedORIString[3]) + " " + outRudder);
					
					
					// !!! debug, remove later
					bundle.putString("ori","ori: " + oriString);
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
			
			bundle.putString("gps", "gps:" + "\n" + "lat=" + location.getLatitude() + "\n" + "lon=" + location.getLongitude() + "\n" + "alt=" + location.getAltitude() + "\n" + "spd=" + location.getSpeed() + "\n" + "acc=" + location.getAccuracy() + "\n");
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
    			
    			try {
					gpsCSV.write(Long.toString(currentTime) + "," + location.getLatitude() + "," + location.getLongitude() + "," + location.getAltitude() + "," + "0.0," + location.getBearing() + "," + location.getSpeed() + "," + location.getAccuracy() + "\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			
    			Log.i("anemoi", "Start new location!");
    			
    			bundle = new Bundle();
    			msgOut =  Message.obtain(null, MSG_GPS, 0, 0);
    			
    			bundle.putString("gps", "gps:" + "\n" + "lat=" + location.getLatitude() + "\n" + "lon=" + location.getLongitude() + "\n" + "alt=" + location.getAltitude() + "\n" + "spd=" + location.getSpeed() + "\n" + "acc=" + location.getAccuracy() + "\n");
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
        
        private SensorEventListener mSensorEventListenerOrientation = new SensorEventListener() {
        	Bundle bundle;
        	Message msgOut;
        	
        	long currentTime;
			
			public void onSensorChanged(SensorEvent event) {
				synchronized (this) {
				
					currentTime = System.currentTimeMillis();
	    			// 0 - yaw, 1 - pitch, 2 - roll
	    			try {
						oriCSV.write(Long.toString(currentTime) + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			
				}
    			
    			bundle = new Bundle();
    			msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
    			
    			bundle.putString("ori", "ori:" + "\n" + "yaw=" + event.values[0] + "\n" + "pth=" + event.values[1] + "\n" + "rol=" + event.values[2] + "\n");
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
			
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
		
		private Thread FlightGearSender = new Thread() {
			
			public void run() {
				
				Log.i("anemoi", "FG SENDER");
				try {
					sendSocket = new Socket(data_flightgear_address, data_flightgear_port);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Log.i("anemoi", "Conected to FG!!!");
				
				try {
					networkOut = new PrintWriter(sendSocket.getOutputStream(), true);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Log.i("anemoi", "Output Socket !!!");
				
				Timer myTimer = new Timer();
				
				myTimer.scheduleAtFixedRate(fg, 0, 40);
				
				//while (isAutopilotServiceRunning) {
					//
				//}
				
			}
		};
		
		private TimerTask fg = new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				//Log.i("anemoi", "SEND SEND SEND");
				//networkOut.println(String.format("%1.3f\t", testOut));
				networkOut.println(String.format("%1.3f\t%1.3f\t", outRudder, outAileron));
				
				testOut += 0.025;
				
				if (testOut > 1.0) {
					testOut = -1.0;
				}
			}
		};
}
