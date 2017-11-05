package com.oddrat.android.anemoi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
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
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

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
    public static final int MSG_AP = 0;
    public static final int MSG_FTDI = 8;
    
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
    
    private static Boolean isActive_mEventListenerOriantation = false;	// old orientation is new rotation
    //private static Boolean isActive_mEventListenerOriantation_new_01 = false;
    
    private static Boolean isActive_mEventListenerAccelerometer = false;
    private static Boolean isActive_mEventListenerMagnetic = false;
    private static Boolean isActive_mEventListenerGyroscope = false;
    
    private static Boolean isActive_mEventListenerAcceleration = false;
    private static Boolean isActive_mEventListenerGravity = false;
    private static Boolean isActive_mEventListenerRotation = false;
    
    private Method mStartForeground;
    private Method mStopForeground;
    
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    
    private static String data_session_folder;
    private static String data_flightgear_address;
    private static int data_flightgear_port;
    
    private static int settings_autopilot_type = -1;
    
    private static Writer gpsCSV;
    
    private static Writer oriCSV;	// old orientation is new rotation
    //private static Writer oriCSV_new_01;
    
    private static Writer accCSV;
    private static Writer magCSV;
    private static Writer gyrCSV;
    
    private static Writer aclCSV;
    private static Writer graCSV;
    private static Writer rotCSV;
    
    private static Writer infoTXT;
    private static Writer rollPID;
    private static Writer pitchPID;
    private static Writer yawPID;
    
    private static Boolean isOpen_file_gpsCSV = false;

    private static Boolean isOpen_file_oriCSV = false;	// old orientation is new rotation
    //private static Boolean isOpen_file_oriCSV_new_01 = false;
    
    private static Boolean isOpen_file_accCSV = false;
    private static Boolean isOpen_file_magCSV = false;
    private static Boolean isOpen_file_gyrCSV = false;
    
    private static Boolean isOpen_file_aclCSV = false;
    private static Boolean isOpen_file_graCSV = false;
    private static Boolean isOpen_file_rotCSV = false;
    
    private static Boolean isOpen_file_infoTXT = false;
    private static Boolean isOpen_file_rollPID = false;
    private static Boolean isOpen_file_pitchPID = false;
    private static Boolean isOpen_file_yawPID = false;
    
    //private static double testOut = 1.0;
    
    private static PrintWriter networkOut;		/// !!! handle - close
    private static Socket sendSocket = null;	/// !!! handle - close
    
    /** autopilot control input / output values */
    private static double outElevator = 0.0;
    private static double outAileron = 0.0;
    private static double outRudder = 0.0;
    
    private static double outThrottle = 0.0;
    
    private KalmanSimple filterElevater = null;//new KalmanSimple(0.1, 31, 1);
    private KalmanSimple filterAileron = null;//new KalmanSimple(1, 0.1, 1);
    private KalmanSimple filterRudder = null;//new KalmanSimple(0.1, 65, 1);
    
    private KalmanSimple filterThrottle = null;//new KalmanSimple(0.01, 35, 1);
    
    /** keeping statistics */
    private static double minSpeed = 0.0;
    private static double maxSpeed = 0.0;
    private static double currentSpeed = 0.0;
    
    private static double minAltitude = 0.0;
    private static double maxAltitude = 0.0;
    private static double currentAltitude = 0.0;
    
    private static long time_start = 0;
    private static long time_stop = 0;
    
    /** tracking variables*/
    private ServerSocket serverSocket = null;
    //private Thread serverThread = null;
    private Handler trackingHandler;
    
    /** navigation controller variables*/
    double home_lat = 0.0;
    double home_lon = 0.0;
    double home_alt = 0.0;
    
    int fpCurrent = -1;						// current waypoint
    
    int fpDirection = 0;					// direction to turn towards current waypoint
    
    double minDistToTarget = 0.0;			// distance to current waypoint
    
    double targetAltitude = 0.0;			// altitude of current waypoint
    double targetAltitudeDifference = 0.0;	// difference in altitude of current waypoint
    
    /** PID controller variables */
    double e_n_ail = 0;
	double y_n_ail = 0;
	double r_n_ail = 0;
	
	double e_n_elv = 0;
	double y_n_elv = 0;
	double r_n_elv = 0;
	
	double e_n_rud = 0;
	double y_n_rud = 0;
	double r_n_rud = 0;
	
	double Kp_ail = 0;//0.1111;
	double Kp_elv = 0;//-0.019;
	double Kp_rud = 0;//0.1;
	
	double Ki_ail = 0;//0.0;
	double Ki_elv = 0;//-0.025;
	double Ki_rud = 0;//0.0;
	
	double Kd_ail = 0;//0.5;
	double Kd_elv = 0;//0.75;
	double Kd_rud = 0;//0.5;
	
	double errSUM_ail = 0.0;
	double errSUM_elv = 0.0;
	double errSUM_rud = 0.0;
	
	double lastERR_ail = 0.0;
	double lastERR_elv = 0.0;
	double lastERR_rud = 0.0;
	
	double dERR_ail = 0.0;
	double dERR_elv = 0.0;
	double dERR_rud = 0.0;
	
	long timeBEFORE = 0;
	long timeCHANGE = 0;
	
	/** custom structures*/
//	class wayPoint {
//		public double lat;
//		public double lon;
//		public double alt;
//		
//		public int direction;	// -1 = left | 0 = closest | 1 = right
//		
//		public int next;
//	}
//	
	//wayPoint[] flightPlan_old = new wayPoint[3];
	
	//WayPoint[] flightPlan = null;
	ArrayList<com.oddrat.android.anemoi.myFly.WayPoint> flightPlan = null;
    com.oddrat.android.anemoi.myFly.Envelope flightEnvelope = null;
    com.oddrat.android.anemoi.myFly.Settings flightSettings = null;
    
    com.oddrat.android.anemoi.myFly.WayPoint wpStop = null;
    com.oddrat.android.anemoi.myFly.WayPoint wpStart = null;
    com.oddrat.android.anemoi.myFly.WayPoint wpCurrent = null;
    
    // FTDI STUFF
    
    private D2xxManager ftD2xx = null;
	private FT_Device ftDev = null;
    
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
	                    	
	                    	
	                    	//Log.i("anemoi", "Here 1.1");
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
	                		
	                		Log.i("anemoi", data_session_folder.toString());
	                		
	                		time_start = System.currentTimeMillis();
	                		
	                		isAutopilotServicePreparing = false;
	                		
	                		// general
	                		try {
	                			gpsCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/gps.csv")));
	                			isOpen_file_gpsCSV = true;	// semaphore
	                			
	                			// old orientation is new rotation
	                			oriCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/ori.csv")));
	                			isOpen_file_oriCSV = true;	// semaphore
	                			//oriCSV_new_01 = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/ori_new_01.csv")));
	                			//isOpen_file_oriCSV_new_01 = true;	// semaphore
	                			
	                			rollPID = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/pidROLL.csv")));
	                			isOpen_file_rollPID = true;	// semaphore
	                			pitchPID = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/pidPITCH.csv")));
	                			isOpen_file_pitchPID = true;	// semaphore
	                			yawPID = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/pidYAW.csv")));
	                			isOpen_file_yawPID = true;	// semaphore
	                		} catch (IOException e) {
	                			// TODO Auto-generated catch block
	                			e.printStackTrace();
	                		}
	                		
	                		// type specific
	                		if (settings_autopilot_type == 0) {
	                    		// do android stuff
	                			try {
		                			accCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/rawACC.csv")));
		                			isOpen_file_accCSV = true;	// semaphore
		                			magCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/rawMAG.csv")));
		                			isOpen_file_magCSV = true;	// semaphore
		                			gyrCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/rawGYR.csv")));
		                			isOpen_file_gyrCSV = true;	// semaphore
		                			
		                			aclCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/comACL.csv")));
		                			isOpen_file_aclCSV = true;	// semaphore
		                			graCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/comGRA.csv")));
		                			isOpen_file_graCSV = true;	// semaphore
		                			rotCSV = new BufferedWriter(new FileWriter(new File(data_session_folder + "/logs/comROT.csv")));
		                			isOpen_file_rotCSV = true;	// semaphore
	                			} catch (IOException e) {
		                			// TODO Auto-generated catch block
		                			e.printStackTrace();
		                		}
	                			
		                		mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, mLocationListener_Start);
		                		isActive_mLocationListener_Start = true;
		                		try {
		                			mlocationManager.removeUpdates(mLocationListener_preStart);
		                			isActive_mLocationListener_preStart = false;
	                	    	} catch (IllegalArgumentException e) {
	                	    		Log.i("anemoi", "IllegalArgumentException");
	                	    	}
		                		
		                		// old orientation is new rotation
		                		//mSensorManager.registerListener(mSensorEventListenerOrientation, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
		                		//isActive_mEventListenerOriantation = true;
		                		mSensorManager.registerListener(mSensorEventListenerOrientation, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME);
		                		isActive_mEventListenerOriantation = true;
		                		
		                		mSensorManager.registerListener(mSensorEventListenerAccelerometer, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
		                		isActive_mEventListenerAccelerometer = true;
		                		mSensorManager.registerListener(mSensorEventListenerMagnetic, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
		                		isActive_mEventListenerMagnetic = true;
		                		mSensorManager.registerListener(mSensorEventListenerGyroscope, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
		                		isActive_mEventListenerGyroscope = true;
		                		
		                		mSensorManager.registerListener(mSensorEventListenerAcceleration, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
		                		isActive_mEventListenerAcceleration = true;
		                		mSensorManager.registerListener(mSensorEventListenerGravity, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_GAME);
		                		isActive_mEventListenerGravity = true;
		                		
		                		AndroidSender.start();
	                		}
	                		
	                		/// !!!
	                		if (settings_autopilot_type == 1) {
	                			FlightGearSender.start();
	                			AndroidSender.start();
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
	                    	// old orientation is new rotation
	                    	if (isActive_mEventListenerOriantation) {
	                	    	mSensorManager.unregisterListener(mSensorEventListenerOrientation);
	                    		isActive_mEventListenerOriantation = false;
	                    	}
	                    	
	                    	//if (isActive_mEventListenerOriantation_new_01) {
	                	    //	mSensorManager.unregisterListener(mSensorEventListenerOrientation_new_01);
	                    	//	isActive_mEventListenerOriantation_new_01 = false;
	                    	//}
	                    	
	                    	if (isActive_mEventListenerAccelerometer) {
	                	    	mSensorManager.unregisterListener(mSensorEventListenerAccelerometer);
	                    		isActive_mEventListenerAccelerometer = false;
	                    	}
	                    	if (isActive_mEventListenerMagnetic) {
	                	    	mSensorManager.unregisterListener(mSensorEventListenerMagnetic);
	                    		isActive_mEventListenerMagnetic = false;
	                    	}
	                    	if (isActive_mEventListenerGyroscope) {
	                	    	mSensorManager.unregisterListener(mSensorEventListenerGyroscope);
	                    		isActive_mEventListenerGyroscope = false;
	                    	}
	                    	
	                    	if (isActive_mEventListenerAcceleration) {
	                	    	mSensorManager.unregisterListener(mSensorEventListenerAcceleration);
	                    		isActive_mEventListenerAcceleration = false;
	                    	}
	                    	if (isActive_mEventListenerGravity) {
	                	    	mSensorManager.unregisterListener(mSensorEventListenerGravity);
	                    		isActive_mEventListenerGravity = false;
	                    	}
	                    	
	                    	
	                    	fgfs.cancel();	/// !!!
	                    	
	                    	anemoi2fly.cancel(); /// !!!
	                    	if(ftDev != null) {
	                            ftDev.close();
	                        }
	                    	ftDev = null;
	                    	
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
    	
    	final UncaughtExceptionHandler subclass = Thread.currentThread().getUncaughtExceptionHandler();
    	Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
    	    
    	    public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
    	    Log.w("anemoi", Log.getStackTraceString(paramThrowable));

    	    subclass.uncaughtException(paramThread, paramThrowable);
    	    }
    	});
        
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
    	
    	myFly myFlyHelper = new myFly();
    	
    	flightPlan = myFlyHelper.returnFlightPlan(intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_MISSION_FILE));
    	flightEnvelope = myFlyHelper.returnEnvelope(intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_MISSION_FILE));
    	
    	flightSettings = myFlyHelper.returnSettings(intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_MISSION_FILE));
    	
		wpStart = myFlyHelper.getStartWayPoint(intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_MISSION_FILE));
		wpStop = myFlyHelper.getStopWayPoint(intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_MISSION_FILE));
    	
		
    	filterAileron = new KalmanSimple(flightSettings.kal_q_ail, flightSettings.kal_r_ail, flightSettings.kal_p_ail);
    	filterElevater = new KalmanSimple(flightSettings.kal_q_elv, flightSettings.kal_r_elv, flightSettings.kal_p_elv);
    	filterRudder = new KalmanSimple(flightSettings.kal_q_rud, flightSettings.kal_r_thr, flightSettings.kal_p_rud);
    	filterThrottle = new KalmanSimple(flightSettings.kal_q_thr, flightSettings.kal_r_thr, flightSettings.kal_p_thr);
    	
    	Kp_ail = flightSettings.pid_p_ail;
    	Kp_elv = flightSettings.pid_p_elv;
    	Kp_rud = flightSettings.pid_p_rud;
    	
    	Ki_ail = flightSettings.pid_i_ail;
    	Ki_elv = flightSettings.pid_i_elv;
    	Ki_rud = flightSettings.pid_i_rud;
    	
    	Kd_ail = flightSettings.pid_d_ail;
    	Kd_elv = flightSettings.pid_d_elv;
    	Kd_rud = flightSettings.pid_d_rud;
    	
    	Log.i("anemoi", "myfly node count !!!!: " + flightPlan.size());
    	
    	if (flightPlan == null) {
    		Bundle bundle = new Bundle();
			bundle.putString("ap", "autopilot: error setting up");
			
			Message msgOut =  Message.obtain(null, MSG_AP, 0, 0);
			msgOut.setData(bundle);
			for (int i=mClients.size()-1; i>=0; i--) {
				try {
            		mClients.get(i).send(msgOut);
            	} catch (RemoteException e) {
                    mClients.remove(i);
                }
            }
    	} else {
    		Bundle bundle = new Bundle();
			bundle.putString("ap", "autopilot: flight plan ready");
			
			Message msgOut =  Message.obtain(null, MSG_AP, 0, 0);
			msgOut.setData(bundle);
			for (int i=mClients.size()-1; i>=0; i--) {
				try {
            		mClients.get(i).send(msgOut);
            	} catch (RemoteException e) {
                    mClients.remove(i);
                }
            }
    	}
    	
    	if (settings_autopilot_type == 0) {
    		// do android stuff
    		mlocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    		
    		mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener_preStart);
    		isActive_mLocationListener_preStart = true;
    		
    		Bundle bundle = new Bundle();
			Message msgOutGPS =  Message.obtain(null, MSG_GPS, 0, 0);
			Message msgOutORI =  Message.obtain(null, MSG_ORI, 0, 0);
			
			bundle.putString("gps", "gps: waiting for lock");
			msgOutGPS.setData(bundle);
			bundle.putString("ori", "sensors: waiting for autopilot start");	// ori tag is legacy, change !!!
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
    	
    	// hard coded default values, should be safe
		maxSpeed = 0.0;			
		minSpeed = 0.0;
		
		maxAltitude = 0.0;		// ground and up
		minAltitude = 100000.0;	// space and up	
    	
		// !!! tracking
		trackingServerThread.start();
		
		try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            Log.e("anemoi", ex.toString());
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
    	
    	time_stop = System.currentTimeMillis();
    	
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
    	// old orientation is new rotation
    	if (isActive_mEventListenerOriantation) {
	    	mSensorManager.unregisterListener(mSensorEventListenerOrientation);
    		isActive_mEventListenerOriantation = false;
    	}
    	
    	//if (isActive_mEventListenerOriantation_new_01) {
	    //	mSensorManager.unregisterListener(mSensorEventListenerOrientation_new_01);
    	//	isActive_mEventListenerOriantation_new_01 = false;
    	//}
    	
    	if (isActive_mEventListenerAccelerometer) {
	    	mSensorManager.unregisterListener(mSensorEventListenerAccelerometer);
    		isActive_mEventListenerAccelerometer = false;
    	}
    	if (isActive_mEventListenerMagnetic) {
	    	mSensorManager.unregisterListener(mSensorEventListenerMagnetic);
    		isActive_mEventListenerMagnetic = false;
    	}
    	if (isActive_mEventListenerGyroscope) {
	    	mSensorManager.unregisterListener(mSensorEventListenerGyroscope);
    		isActive_mEventListenerGyroscope = false;
    	}
    	
    	if (isActive_mEventListenerAcceleration) {
	    	mSensorManager.unregisterListener(mSensorEventListenerAcceleration);
    		isActive_mEventListenerAcceleration = false;
    	}
    	if (isActive_mEventListenerGravity) {
	    	mSensorManager.unregisterListener(mSensorEventListenerGravity);
    		isActive_mEventListenerGravity = false;
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
    	// old orientation is new rotation
    	//if (isOpen_file_oriCSV_new_01) {
	    //	try {
		//		oriCSV_new_01.close();
		//		isOpen_file_oriCSV_new_01 = false;
		//	} catch (IOException e) {
		//		// TODO Auto-generated catch block
		//		//e.printStackTrace();
		//		Log.i("anemoi", "IOException");
		//	}
    	//}
    	
    	if (isOpen_file_accCSV) {
	    	try {
				accCSV.close();
				isOpen_file_accCSV = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	if (isOpen_file_magCSV) {
	    	try {
				magCSV.close();
				isOpen_file_magCSV = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	if (isOpen_file_gyrCSV) {
	    	try {
				gyrCSV.close();
				isOpen_file_gyrCSV = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	
    	if (isOpen_file_aclCSV) {
	    	try {
				aclCSV.close();
				isOpen_file_aclCSV = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	if (isOpen_file_graCSV) {
	    	try {
				graCSV.close();
				isOpen_file_graCSV = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	if (isOpen_file_rotCSV) {
	    	try {
				rotCSV.close();
				isOpen_file_rotCSV = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	
    	if (isOpen_file_rollPID) {
	    	try {
				rollPID.close();
				isOpen_file_rollPID = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	
    	if (isOpen_file_pitchPID) {
	    	try {
				pitchPID.close();
				isOpen_file_pitchPID = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	
    	if (isOpen_file_yawPID) {
	    	try {
				yawPID.close();
				isOpen_file_yawPID = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				Log.i("anemoi", "IOException");
			}
    	}
    	
    	if (data_session_folder != null) {
	    	try {
				infoTXT = new BufferedWriter(new FileWriter(new File(data_session_folder + "/mission/info.txt")));
				isOpen_file_infoTXT = true;	// semaphore
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	if (isOpen_file_infoTXT) {
	    	try {
	    		infoTXT.write("autopilot_type: " + settings_autopilot_type + "\n");
	    		infoTXT.write("\n");
	    		infoTXT.write("time_start: " + time_start + "\n");
	    		infoTXT.write("time_stop: " + time_stop + "\n");
	    		infoTXT.write("\n");
	    		infoTXT.write("speed_min: " + ((minSpeed * 3600) / 1000) + "\n");	// convert m/s to km/s
				infoTXT.write("speed_max: " + ((maxSpeed * 3600) / 1000) + "\n");	// convert m/s to km/s
				infoTXT.write("\n");
				infoTXT.write("altitude_min: " + minAltitude + "\n");
				infoTXT.write("altitude_max: " + maxAltitude + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.i("anemoi", "IOException");
			}
	    }
    	if (isOpen_file_infoTXT) {
	    	try {
				infoTXT.close();
				isOpen_file_infoTXT = false;
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
         
        // !!! tracking
        trackingServerThread.interrupt();
        try {
        	if (serverSocket != null) {
        		serverSocket.close();
        	}
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }
    
    /** The rest of the lot */
    
    /**When binding to the service, we return an interface to our messenger for sending messages to the service. */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    
    /** threads */
    
    /** Flightgear */
    
    // Flightgear GSP listener
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
					currentTime = System.currentTimeMillis();
					
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
					
					gpsString = null;
					separatedGSPString = null;
					
					// gps stuff
					
					currentTime = System.currentTimeMillis();
					
					
					try {
						gpsString = gpsNetworkIn.readLine();
						separatedGSPString = gpsString.split("[=\\t\\n]");	// odd ones have data: lat, lon alt, hdg, spd, acc
					} catch (Exception e) {
						//Log.w("anemoi", "gpsString exception for some reason");
						//continue;
					}
					
		if ((gpsString != null) && (separatedGSPString != null)) {
					
					try {
						currentSpeed = Double.parseDouble(separatedGSPString[9]);
						currentAltitude = Double.parseDouble(separatedGSPString[5]);
						
						
						if (Double.compare(currentSpeed, maxSpeed) > 0 ) {
							maxSpeed = currentSpeed;
						}
						
						if (Double.compare(currentAltitude, maxAltitude) > 0 ) {
							maxAltitude = currentAltitude;
						}
						if (Double.compare(currentAltitude, minAltitude) < 0 ) {
							minAltitude = currentAltitude;
						} 
					} catch (Exception e) {
						// ignore nan from flightgear
						Log.w("anemoi", "ignore nan from flightgear gps");
					}
					
					// !!! fake flight plan
					//flightPlan_old[0] = new wayPoint();
					//flightPlan_old[0].lat = 45.932075;
					//flightPlan_old[0].lon = 15.488658333333333;
					//flightPlan_old[0].alt = 200.0;
					//flightPlan_old[0].direction = -1;
					//flightPlan_old[0].next = 1;
					//
					//flightPlan_old[1] = new wayPoint();
					//flightPlan_old[1].lat = 45.930055555555555;
					//flightPlan_old[1].lon = 15.494630555555556;
					//flightPlan_old[1].alt = 300.0;
					//flightPlan_old[1].direction = 0;
					//flightPlan_old[1].next = 2;
					//
					//flightPlan_old[2] = new wayPoint();
					//flightPlan_old[2].lat = 45.92979444444445;
					//flightPlan_old[2].lon = 15.490736111111111;
					//flightPlan_old[2].alt = 300.0;
					//flightPlan_old[2].direction = 0;
					//flightPlan_old[2].next = 0;
					
					try {
						reactGPS(Double.parseDouble(separatedGSPString[1]), Double.parseDouble(separatedGSPString[3]), Double.parseDouble(separatedGSPString[5]), Double.parseDouble(separatedGSPString[7]), Double.parseDouble(separatedGSPString[9]), Double.parseDouble(separatedGSPString[11]));
					} catch (Exception e) {
						// ignore nan from flightgear
						Log.w("anemoi", "ignore nan from flightgear gpsreact");
					}
					
					try {
						gpsCSV.write(Long.toString(currentTime) + "," + separatedGSPString[1] + "," + separatedGSPString[3] + "," + separatedGSPString[5] + "," + "0.0," + separatedGSPString[7] + "," + separatedGSPString[9] + "," + separatedGSPString[11] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.w("anemoi", "ignore nan from flightgear log write");
					}
					
					try {
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
					} catch (Exception e) {
						Log.w("anemoi", "ignore nan from flightgear bundle sms");
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
    
 // Flightgear ORI listener
    private Thread oriFlightGearListener = new Thread() {
    	
		public void run() {
			Log.i("anemoi", Integer.toString(data_flightgear_port));
			
			Bundle bundle = new Bundle();
			bundle.putString("ori", "sensors: listener ready");
			
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
				ServerSocket oriReceiverSocket = new ServerSocket(data_flightgear_port+2);
				
				Socket oriClient = oriReceiverSocket.accept();
				Log.i("anemoi", "got one");
				BufferedReader oriNetworkIn = new BufferedReader(new InputStreamReader(oriClient.getInputStream()));
				
				long currentTime;
				String oriString;
				String[] separatedORIString;
				
				while (isAutopilotServicePreparing) {
					oriString = oriNetworkIn.readLine();
					
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
				
				
				
				while (isAutopilotServiceRunning) {
					// !!! debug, remove later
					bundle = new Bundle();
					msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
					
					// gps stuff
					currentTime = System.currentTimeMillis();
					//timeCHANGE = currentTime - timeBEFORE;
					
					oriString = null;
					separatedORIString = null;
					
					try {
						oriString = oriNetworkIn.readLine();
						/// !! handle if null or something
						separatedORIString = oriString.split("[=\\t\\n]");	// odd ones have data: yaw pitch roll
					} catch (Exception e) {
						//Log.w("anemoi", "oriString exception for some reason");
						//continue;
					}
		
		if ((oriString != null) && (separatedORIString != null)) {			
					
					// PID controller
					try {
						reactSensors(Double.parseDouble(separatedORIString[1]), Double.parseDouble(separatedORIString[3]), Double.parseDouble(separatedORIString[5]), currentTime);
					} catch (Exception e) {
						Log.i("anemoi", "PID exception (probably nan, ignore)");
					}
					//Log.i("anemoi", "PID-ail: " + Double.parseDouble(separatedORIString[5]) + " " + outAileron + " ::: " + errSUM_ail + " " + dERR_ail);
					//Log.i("anemoi", "PID-rud: " + Double.parseDouble(separatedORIString[3]) + " " + outElevator + " ::: " + errSUM_elv + " " + dERR_elv);
					
					try {
						oriCSV.write(Long.toString(currentTime) + "," + separatedORIString[1] + "," + separatedORIString[3] + "," + separatedORIString[5] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					try {
						rollPID.write(Long.toString(currentTime) + "," + separatedORIString[5] + "," + r_n_ail + "," + outAileron + "," + e_n_ail + "," + errSUM_ail + "," + dERR_ail + "," + Kp_ail + "," + Ki_ail + "," + Kd_ail + "\n");
						pitchPID.write(Long.toString(currentTime) + "," + separatedORIString[3] + "," + r_n_elv + "," + outElevator + "," + e_n_elv + "," + errSUM_elv + "," + dERR_elv + "," + Kp_elv + "," + Ki_elv + "," + Kd_elv + "\n");
						yawPID.write(Long.toString(currentTime) + "," + separatedORIString[1] + "," + r_n_rud + "," + outRudder + "," + e_n_rud + "," + errSUM_rud + "," + dERR_rud + "," + Kp_rud + "," + Ki_rud + "," + Kd_rud + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
//					try {
//						// !!! debug, remove later
//						bundle.putString("ori","ori: " + oriString);
//						msgOut.setData(bundle);
//						for (int i=mClients.size()-1; i>=0; i--) {
//							try {
//		                		mClients.get(i).send(msgOut);
//		                	} catch (RemoteException e) {
//		                        // The client is dead.  Remove it from the list;
//		                        // we are going through the list from back to front
//		                        // so this is safe to do inside the loop.
//		                        mClients.remove(i);
//		                    }
//		                }
//					} catch (Exception e) {
//						Log.w("anemoi", "ignore nan from flightgear bundle");
//					}
		}			
				}
				
				oriClient.close();
				oriReceiverSocket.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
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
			
			myTimer.scheduleAtFixedRate(fgfs, 0, 40);
			
			//while (isAutopilotServiceRunning) {
				//
			//}
			
		}
	};
	
private Thread AndroidSender = new Thread() {
		
		public void run() {
			
			Log.w("anemoi", "AN SENDER");
			
			int tryCount = 0;
			
			int devCount = 0;
			
			while (tryCount < 3) {
		        devCount = ftD2xx.createDeviceInfoList(autopilotService.this);
	
		        Log.w("anemoi", "Device number : "+ Integer.toString(devCount));
	
		        D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
		        ftD2xx.getDeviceInfoList(devCount, deviceList);
		        Log.w("anemoi", "AN 1");
		        
		        if(devCount > 0) {
		            break;
		        }
		        
		        tryCount++;
		        
		        try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
			
			if(devCount <= 0) {
	            return;
	        }
	        
	        Log.w("anemoi", "AN 1.0");
	        if(ftDev == null) {
	        	Log.w("anemoi", "AN 1.1");
	        	ftDev = ftD2xx.openByIndex(autopilotService.this, 0);
	            
	        } else {
	        	Log.w("anemoi", "AN 1.2");
	            synchronized (ftDev) {
	                ftDev = ftD2xx.openByIndex(autopilotService.this, 0);
	            }
	        }
	        Log.w("anemoi", "AN 2");
	        if(ftDev.isOpen()) {
	                SetConfig(115200, (byte)8, (byte)1, (byte)0, (byte)0);
	                ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
	                ftDev.restartInTask();
	                Log.w("anemoi", "AN SENDER pre timer");
	                Timer myTimer = new Timer();
	    			myTimer.scheduleAtFixedRate(anemoi2fly, 0, 40);
	    			Log.w("anemoi", "AN SENDER post timer");
	    			
	    			Bundle bundle = new Bundle();
	    			bundle.putString("ftdi", "FTDI: timer up");
	    			
	    			Message msgOut =  Message.obtain(null, MSG_FTDI, 0, 0);
	    			msgOut.setData(bundle);
	    			for (int i=mClients.size()-1; i>=0; i--) {
	    				try {
	                		mClients.get(i).send(msgOut);
	                	} catch (RemoteException e) {
	                        mClients.remove(i);
	                    }
	                }
	        }
		}
	};
	
	private TimerTask fgfs = new TimerTask() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			//Log.i("anemoi", "SEND: " + String.format("%1.3f\t%1.3f\t%1.3f\t%1.3f\t", outThrottle, outElevator, outRudder, outAileron));
			//networkOut.println(String.format("%1.3f\t", testOut));
			//networkOut.println(String.format("%1.3f\t%1.3f\t%1.3f\t%1.3f\t", outThrottle, outElevator, outRudder, outAileron));
			networkOut.println(String.format("%1.3f\t%1.3f\t%1.3f\t%1.3f\t", outThrottle, outElevator, outRudder, outAileron));
			
			//testOut += 0.025;
			//
			//if (testOut > 1.0) {
			//	testOut = -1.0;
			//}
		}
	};
    
	private TimerTask anemoi2fly = new TimerTask() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			//Log.w("anemoi", "ftdi write");
			
			if (ftDev != null) {
				if(ftDev.isOpen()) {
					
					ftDev.setLatencyTimer((byte)16);
					//Log.w("anemoi", "---");
					//Log.w("anemoi", outRudder + " | " + outElevator + " | " +
					//		outThrottle + " | " + outAileron);
					//Log.w("anemoi", ((short) (3000.0+(1.0+outRudder)*1500.0)) + " | " + ((short) (3000.0+(1.0+outElevator)*1500.0)) + " | " +
					//		((short) (3000.0+outThrottle*3000.0)) + " | " + ((short) (3000.0+(1.0+outAileron)*1500.0)));
					byte[] toSend = ByteBuffer.allocate(12).putInt(0xFFAA55FF).putShort((short) (3000.0+(1.0+outRudder)*1450.0))
							.putShort((short) (3000.0+(1.0+outElevator)*1450.0)).putShort((short) (3000.0+outThrottle*3000.0))
							.putShort((short) (3000.0+(1.0+outAileron)*1450.0)).array();
					
		            ftDev.write(toSend, 12);
	            }
			}
		}
	};
	
    
    /** Android */
    
    private LocationListener mLocationListener_preStart = new LocationListener() {
    //private class gpsAndroidListener_preStart implements LocationListener {
    	Bundle bundle;
    	Message msgOut;
    	
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			//Log.i("anemoi", "preStart new location!");
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
    			
    			currentSpeed = location.getSpeed();
				currentAltitude = location.getAltitude();
				
				
				if (Double.compare(currentSpeed, maxSpeed) > 0 ) {
					maxSpeed = currentSpeed;
				}
				
				if (Double.compare(currentAltitude, maxAltitude) > 0 ) {
					maxAltitude = currentAltitude;
				}
				if (Double.compare(currentAltitude, minAltitude) < 0 ) {
					minAltitude = currentAltitude;
				} 
    			
				//Log.i("anemoi", "altitude " + currentAltitude);
				//Log.i("anemoi", "minAltitude " + minAltitude);
				//Log.i("anemoi", "maxAltitude " + maxAltitude);
				//Log.i("anemoi", "----");
				
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
    			
    			// !!! reactGPSTracking(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getBearing(), location.getSpeed(), location.getAccuracy());
    			try {
    			reactGPS(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getBearing(), location.getSpeed(), location.getAccuracy());
    			} catch (Exception e) {
					// ignore nan from flightgear
					Log.w("anemoi", "ignore nan from android gpsreact");
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
        
        //private SensorEventListener mSensorEventListenerOrientation = new SensorEventListener() {
        //	Bundle bundle;
        //	Message msgOut;
        //	
        //	long currentTime;
		//	
		//	public void onSensorChanged(SensorEvent event) {
		//		synchronized (this) {
		//		
		//			currentTime = System.currentTimeMillis(); //event.timestamp;
	    //			// 0 - yaw, 1 - pitch, 2 - roll
	    //			try {
		//				oriCSV.write(Long.toString(currentTime) + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
		//			} catch (IOException e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}
    	//		
		//		}
    	//		
    	//		bundle = new Bundle();
    	//		msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
    	//		
    	//		bundle.putString("ori", "ori:" + "\n" + "yaw=" + event.values[0] + "\n" + "pth=" + event.values[1] + "\n" + "rol=" + event.values[2] + "\n");
    	//		msgOut.setData(bundle);
    	//		
    	//		for (int i=mClients.size()-1; i>=0; i--) {
    	//			try {
        //        		mClients.get(i).send(msgOut);
        //        	} catch (RemoteException e) {
        //                // The client is dead.  Remove it from the list;
        //                // we are going through the list from back to front
        //                // so this is safe to do inside the loop.
        //                mClients.remove(i);
        //            }
        //        }
		//		
		//	}
		//	
		//	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//		// TODO Auto-generated method stub
		//		
		//	}
		//};
		
		private SensorEventListener mSensorEventListenerOrientation = new SensorEventListener() {        	
			Bundle bundle;
	        Message msgOut;
			
			long currentTime;
        	float[] mRotationMatrix = new float[16];
        	float[] mRotationMatrix_rotated = new float[16];
        	float[] mOrientation = new float[3];
        	
			public void onSensorChanged(SensorEvent event) {
				synchronized (this) {
				
					currentTime = System.currentTimeMillis(); //event.timestamp;
					
					SensorManager.getRotationMatrixFromVector(mRotationMatrix , event.values);
					SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Y, mRotationMatrix_rotated);
					SensorManager.getOrientation(mRotationMatrix_rotated, mOrientation);
					
					try {
						reactSensors(((float)Math.toDegrees(mOrientation[0]) + 180.0), (float)Math.toDegrees(mOrientation[1]), (float)Math.toDegrees(mOrientation[2]), currentTime);
					} catch (Exception e) {
						Log.i("anemoi", "PID exception android (probably nan, ignore)");
					}
					
//	    			// 0 - yaw, 1 - pitch, 2 - roll
	    			try {
						oriCSV.write(Long.toString(currentTime) + "," + ((float)Math.toDegrees(mOrientation[0]) + 180.0) + "," + (float)Math.toDegrees(mOrientation[1]) + "," + (float)Math.toDegrees(mOrientation[2]) + "\n");
						rotCSV.write(Long.toString(currentTime) + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
	    			} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    			
	    			try {
						rollPID.write(Long.toString(currentTime) + "," + (float)Math.toDegrees(mOrientation[2]) + "," + r_n_ail + "," + outAileron + "," + e_n_ail + "," + errSUM_ail + "," + dERR_ail + "," + Kp_ail + "," + Ki_ail + "," + Kd_ail + "\n");
						pitchPID.write(Long.toString(currentTime) + "," + (float)Math.toDegrees(mOrientation[1]) + "," + r_n_elv + "," + outElevator + "," + e_n_elv + "," + errSUM_elv + "," + dERR_elv + "," + Kp_elv + "," + Ki_elv + "," + Kd_elv + "\n");
						yawPID.write(Long.toString(currentTime) + "," + ((float)Math.toDegrees(mOrientation[0]) + 180.0) + "," + r_n_rud + "," + outRudder + "," + e_n_rud + "," + errSUM_rud + "," + dERR_rud + "," + Kp_rud + "," + Ki_rud + "," + Kd_rud + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
//				bundle = new Bundle();
//    			msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
//    			
//    			bundle.putString("ori", "ori:" + "\n" + "yaw=" + ((float)Math.toDegrees(mOrientation[0]) + 180.0) + "\n" + "pth=" + (float)Math.toDegrees(mOrientation[1]) + "\n" + "rol=" + (float)Math.toDegrees(mOrientation[2]) + "\n");
//    			msgOut.setData(bundle);
//    			
//    			for (int i=mClients.size()-1; i>=0; i--) {
//    				try {
//                		mClients.get(i).send(msgOut);
//                	} catch (RemoteException e) {
//                        // The client is dead.  Remove it from the list;
//                        // we are going through the list from back to front
//                        // so this is safe to do inside the loop.
//                        mClients.remove(i);
//                    }
//                }
			}
			
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
		
		private SensorEventListener mSensorEventListenerAccelerometer = new SensorEventListener() {
        	//Bundle bundle;
        	//Message msgOut;
        	
        	long currentTime;
			
			public void onSensorChanged(SensorEvent event) {
				synchronized (this) {
				
					currentTime = System.currentTimeMillis(); // event.timestamp;
	    			// 0 - x, 1 - y, 2 - z
	    			try {
						accCSV.write(Long.toString(currentTime) + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			
				}
    			
    			//bundle = new Bundle();
    			//msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
    			//
    			//bundle.putString("ori", "ori:" + "\n" + "yaw=" + event.values[0] + "\n" + "pth=" + event.values[1] + "\n" + "rol=" + event.values[2] + "\n");
    			//msgOut.setData(bundle);
    			//
    			//for (int i=mClients.size()-1; i>=0; i--) {
    			//	try {
                //		mClients.get(i).send(msgOut);
                //	} catch (RemoteException e) {
                //        // The client is dead.  Remove it from the list;
                //        // we are going through the list from back to front
                //        // so this is safe to do inside the loop.
                //        mClients.remove(i);
                //    }
                //}
				
			}
			
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
		private SensorEventListener mSensorEventListenerMagnetic = new SensorEventListener() {
        	//Bundle bundle;
        	//Message msgOut;
        	
        	long currentTime;
			
			public void onSensorChanged(SensorEvent event) {
				synchronized (this) {
				
					currentTime = System.currentTimeMillis(); //event.timestamp;
	    			// 0 - x, 1 - y, 2 - z
	    			try {
						magCSV.write(Long.toString(currentTime) + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    			try {
	    				if (serverSocket != null) {
	    	        		serverSocket.close();
	    	        	}
	    	         } catch (IOException e) {
	    	        	 e.printStackTrace();
	    	         }
				}
    			
    			//bundle = new Bundle();
    			//msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
    			//
    			//bundle.putString("ori", "ori:" + "\n" + "yaw=" + event.values[0] + "\n" + "pth=" + event.values[1] + "\n" + "rol=" + event.values[2] + "\n");
    			//msgOut.setData(bundle);
    			//
    			//for (int i=mClients.size()-1; i>=0; i--) {
				//	try {
				//		mClients.get(i).send(msgOut);
				//	} catch (RemoteException e) {
				//        // The client is dead.  Remove it from the list;
				//        // we are going through the list from back to front
				//        // so this is safe to do inside the loop.
				//        mClients.remove(i);
				//    }
				//}
				
			}
			
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
		private SensorEventListener mSensorEventListenerGyroscope = new SensorEventListener() {
        	//Bundle bundle;
			//Message msgOut;
        	
        	long currentTime;
			
			public void onSensorChanged(SensorEvent event) {
				synchronized (this) {
				
					currentTime = System.currentTimeMillis(); //event.timestamp;
	    			// 0 - x, 1 - y, 2 - z
	    			try {
						gyrCSV.write(Long.toString(currentTime) + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			
				}
    			
				//bundle = new Bundle();
				//msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
				//
				//bundle.putString("ori", "ori:" + "\n" + "yaw=" + event.values[0] + "\n" + "pth=" + event.values[1] + "\n" + "rol=" + event.values[2] + "\n");
				//msgOut.setData(bundle);
				//
				//for (int i=mClients.size()-1; i>=0; i--) {
				//	try {
				//		mClients.get(i).send(msgOut);
				//	} catch (RemoteException e) {
				//       // The client is dead.  Remove it from the list;
				//       // we are going through the list from back to front
				//       // so this is safe to do inside the loop.
				//       mClients.remove(i);
				//   }
				//}
				
			}
			
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
		
		private SensorEventListener mSensorEventListenerGravity = new SensorEventListener() {
        	//Bundle bundle;
			//Message msgOut;
        	
        	long currentTime;
			
			public void onSensorChanged(SensorEvent event) {
				synchronized (this) {
				
					currentTime = System.currentTimeMillis(); //event.timestamp;
	    			// 0 - x, 1 - y, 2 - z
	    			try {
						graCSV.write(Long.toString(currentTime) + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			
				}try {
					if (serverSocket != null) {
		        		serverSocket.close();
		        	}
		         } catch (IOException e) {
		        	 e.printStackTrace();
		         }
    			
				//bundle = new Bundle();
				//msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
				//
				//bundle.putString("ori", "ori:" + "\n" + "yaw=" + event.values[0] + "\n" + "pth=" + event.values[1] + "\n" + "rol=" + event.values[2] + "\n");
				//msgOut.setData(bundle);
				//
				//for (int i=mClients.size()-1; i>=0; i--) {
				//	try {
				//		mClients.get(i).send(msgOut);
				//	} catch (RemoteException e) {
				//       // The client is dead.  Remove it from the list;
				//       // we are going through the list from back to front
				//       // so this is safe to do inside the loop.
				//       mClients.remove(i);
				//   }
				//}
				
			}
			
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
		private SensorEventListener mSensorEventListenerAcceleration = new SensorEventListener() {
        	//Bundle bundle;
			//Message msgOut;
        	
        	long currentTime;
			
			public void onSensorChanged(SensorEvent event) {
				synchronized (this) {
				
					currentTime = System.currentTimeMillis(); //event.timestamp;
	    			// 0 - x, 1 - y, 2 - z
	    			try {
						aclCSV.write(Long.toString(currentTime) + "," + event.values[0] + "," + event.values[1] + "," + event.values[2] + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			
				}
    			
				//bundle = new Bundle();
				//msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
				//
				//bundle.putString("ori", "ori:" + "\n" + "yaw=" + event.values[0] + "\n" + "pth=" + event.values[1] + "\n" + "rol=" + event.values[2] + "\n");
				//msgOut.setData(bundle);
				//
				//for (int i=mClients.size()-1; i>=0; i--) {
				//	try {
				//		mClients.get(i).send(msgOut);
				//	} catch (RemoteException e) {
				//       // The client is dead.  Remove it from the list;
				//       // we are going through the list from back to front
				//       // so this is safe to do inside the loop.
				//       mClients.remove(i);
				//   }
				//}
				
			}
			
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
		
		/** tracking */
		private Thread trackingServerThread = new Thread() {

			public void run() {
				Log.i("anemoi", "tracking up and waiting");
				Socket socket = null;
				try {
					serverSocket = new ServerSocket(4646);
				} catch (IOException e) {
					e.printStackTrace();
				}
				while (!Thread.currentThread().isInterrupted()) {
				//while (isAutopilotServiceRunning) {
				//while ((isAutopilotServiceRunning) || (!Thread.currentThread().isInterrupted())) {
					try {
						if (!serverSocket.isClosed()) {
							socket = serverSocket.accept();
							Log.i("anemoi", "tracking got another one");
							//CommunicationThread commThread = new CommunicationThread(socket);
							//new Thread(commThread).start();
							
							//trackingClientThread.start();
							trackingClientThread commThread = new trackingClientThread(socket);
							new Thread(commThread).start();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				//try {
				//	socket.close();
				//	//serverSocket.close();
		        //} catch (IOException e) {
		        //	 e.printStackTrace();
		        //}
			}
		};
		
		//Thread trackingClientThread = new Thread() {
		class trackingClientThread implements Runnable {
			private Socket clientSocket;
			//private BufferedReader input;
			private PrintWriter trackingNetworkOut;
			
			public trackingClientThread(Socket clientSocket) {

				this.clientSocket = clientSocket;

				try {
					//this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
					this.trackingNetworkOut = new PrintWriter(this.clientSocket.getOutputStream(), true);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		    public void run(){
		           Looper.prepare();

		           trackingHandler = new Handler() {
		                   public void handleMessage(Message msg) {
		                       //Log.i("anemoi", "sent tracking data");
		                       //Log.i("anemoi", msg.obj.toString());
		                       trackingNetworkOut.write(msg.obj.toString()); trackingNetworkOut.flush();
		                   }
		           };
		           Looper.loop();
		    }
		};
		
		// tracking
		private void reactGPSTracking(double lat, double lon, double alt, double hdg, double spd, double acc, String wp, double latTarget, double lonTarget) {
			// !! this might be better somehow not like this
			if (trackingHandler != null) {
				//Log.i("anemoi", "GPS tracking");
				
				Message msg = Message.obtain();
				msg.obj = Double.toString(lon) + ";" + Double.toString(lat) + ";" + Double.toString(alt) + ";" + Double.toString(spd) + ";" + Double.toString(acc) + ";" + Double.toString(hdg) + ";" + wp.trim() + ";" + Double.toString(lonTarget) + ";" + Double.toString(latTarget) + ";" + "\n";
				trackingHandler.sendMessage(msg);				
			}
			
			
		};
		
		
		/** autopilot logic */
		// navigation
		private void reactGPS(double lat, double lon, double alt, double hdg, double spd, double acc) {
			//Log.i("anemoi", "reactGPS: " + fpCurrent);
			
			Location locNow;
			Location locWaypoint;
			
			double distToTarget;
			
			locNow = new Location("");
			locWaypoint = new Location("");
			
			Bundle bundle = new Bundle();
			
			// 1. step - check if current if special	// !!! this is to be redone proper
			switch (fpCurrent) {
				// start case
				case -1:
					Log.w("anemoi", "case -1");
					home_lat = lat;
					home_lon = lon;
					home_alt = alt;
					
					if (wpStart == null) {
						
						myFly myFlyHelper = new myFly();
						wpStart = myFlyHelper.returnNewWayPoint();
						
						wpStart.name = "HOME";
						wpStart.lat = lat;
						wpStart.lon = lon;
						wpStart.alt = alt;
					}
					
					if (wpStop == null) {
						wpStop = wpStart;
					}
					
					if (flightPlan.size() > 1){
						//Log.w("anemoi", "else 1");
						
						fpCurrent = 0;
						wpCurrent = flightPlan.get(fpCurrent);
						//bundle.putString("ap", flightPlan.get(fpCurrent).name);
						bundle.putString("ap", wpCurrent.name);
					} else {
						//Log.w("anemoi", "else 2");
						fpCurrent = -2;
						wpCurrent = wpStop;
						
						//bundle.putString("ap", "STOP Condition");
						bundle.putString("ap", wpCurrent.name);
					}
					//fpCurrent = 0;
					//wpCurrent = flightPlan.get(fpCurrent);
										
					locNow.setLatitude(lat);
					locNow.setLongitude(lon);
					
					//locWaypoint.setLatitude(flightPlan.get(fpCurrent).lat);
					//locWaypoint.setLongitude(flightPlan.get(fpCurrent).lon);
					locWaypoint.setLatitude(wpCurrent.lat);
					locWaypoint.setLongitude(wpCurrent.lon);
					
					
					minDistToTarget = locNow.distanceTo(locWaypoint);
					
					// for the P.I.D.
					//targetAltitude = flightPlan.get(fpCurrent).alt;		// how high
					targetAltitude = wpCurrent.alt;		// how high
					targetAltitudeDifference = 0.0;					    // just fly straight
					
					//r_n_rud = locNow.bearingTo(locWaypoint);		// where to
					//if (r_n_rud < 0.0) {
					//	r_n_rud = r_n_rud + 360.0; // bearing (-1807+180 to compass heading 0-360)
					//}
					
					//bundle.putString("ap", "START condition");
					bundle.putString("ap", wpCurrent.name);
					break;
				// stop case
				case -2:
					Log.w("anemoi", "case -2");
					locWaypoint.setLatitude(wpCurrent.lat);
					locWaypoint.setLongitude(wpCurrent.lon);
					
					locNow.setLatitude(lat);
					locNow.setLongitude(lon);
					
					distToTarget = locNow.distanceTo(locWaypoint);
					
					double distToNewTarget = ((wpCurrent.distAcq / (distToTarget / wpCurrent.distAcq)) / 1000) / 6372.795477598;
					//double distToNewTarget = ((wpCurrent.distAcq) / 1000) / 6372.795477598;
					
					double bearingToNewTarget = locWaypoint.bearingTo(locNow) * -1.0;

					
					if (wpCurrent.dir > 0) {
						bearingToNewTarget = bearingToNewTarget - 45.0;
					} else {
						bearingToNewTarget = bearingToNewTarget + 45.0;
					}					
					
					if (bearingToNewTarget < 0.0) {
						bearingToNewTarget = bearingToNewTarget + 360.0; // bearing (-180/+180 to compass heading 0-360)
					}
					bearingToNewTarget = bearingToNewTarget / 180.0 * Math.PI;
					
					
					if (bearingToNewTarget < 0.0) {
						bearingToNewTarget = bearingToNewTarget + (Math.PI * 2);
					}
					
					if (bearingToNewTarget > (Math.PI * 2)) {
						bearingToNewTarget = bearingToNewTarget - (Math.PI * 2);
					}
					
					double curLatInRad = wpCurrent.lat / 180.0 * Math.PI;
					double curLonInRad = wpCurrent.lon / 180.0 * Math.PI;
					
					double newLatInRad = Math.asin(Math.sin(curLatInRad) * Math.cos(distToNewTarget) + Math.cos(curLatInRad) * Math.sin(distToNewTarget) * Math.cos(bearingToNewTarget));
					double dlon = Math.atan2(Math.sin(bearingToNewTarget) * Math.sin(distToNewTarget) * Math.cos(curLatInRad), Math.cos(distToNewTarget) - Math.sin(curLatInRad) * Math.sin(newLatInRad));
					double newLonInRad = ((curLonInRad - dlon + Math.PI) % (2*Math.PI)) - Math.PI;
					
					
					locWaypoint.setLatitude(newLatInRad * 180.0 / Math.PI);
					locWaypoint.setLongitude(newLonInRad * 180.0 / Math.PI);
					
					targetAltitude = wpCurrent.alt;		// how high
					
					Log.w("anemoi", (wpCurrent.lat) + " - " + (wpCurrent.lon) + " # " + (newLatInRad * 180.0 / Math.PI) + " - " + (newLonInRad * 180.0 / Math.PI) + " | " + bearingToNewTarget);
					
					//bundle.putString("ap", "STOP condition");
					bundle.putString("ap", wpCurrent.name);
					break;
				
				default:
					//Log.w("anemoi", "case default");
					// 2. step - check if we got the checkpoint and need to set new one
					
					//locWaypoint.setLatitude(flightPlan.get(fpCurrent).lat);
					//locWaypoint.setLongitude(flightPlan.get(fpCurrent).lon);
					locWaypoint.setLatitude(wpCurrent.lat);
					locWaypoint.setLongitude(wpCurrent.lon);
					
					locNow.setLatitude(lat);
					locNow.setLongitude(lon);
					
					distToTarget = locNow.distanceTo(locWaypoint);
					
					// for the P.I.D.
					//targetAltitude = flightPlan.get(fpCurrent).alt;		// how high
					targetAltitude = wpCurrent.alt;		// how high
					//targetAltitudeDifference = targetAltitude - alt;
					
					//r_n_rud = locNow.bearingTo(locWaypoint);		// where to
					
					if (distToTarget < minDistToTarget) {
						minDistToTarget = distToTarget;
						//Log.i("anemoi", Double.toString(minDistToTarget));
					}
					else {
						//if (distToTarget <= flightPlan.get(fpCurrent).distAcq) {
						if (distToTarget <= wpCurrent.distAcq) {
							if (distToTarget > minDistToTarget) {
								
								//fpDirection = flightPlan.get(fpCurrent).dir;
								fpDirection = wpCurrent.dir;
								//Log.w("anemoi", Integer.toString(flightPlan.get(fpCurrent).next));
								
								//if (flightPlan.get(fpCurrent).next >= 0) {
								if (wpCurrent.next >= 0) {
									//Log.w("anemoi", "if");
									//fpCurrent = flightPlan.get(fpCurrent).next;
									fpCurrent = wpCurrent.next;
									wpCurrent = flightPlan.get(fpCurrent);
								}
								else {
									//Log.w("anemoi", "else: " + Integer.toString(flightPlan.size()) + " " + Integer.toString((fpCurrent + 1)));
									if (flightPlan.size() > (fpCurrent + 1) ){
										//Log.w("anemoi", "else 1");
										
										fpCurrent++;
										wpCurrent = flightPlan.get(fpCurrent);
										//bundle.putString("ap", flightPlan.get(fpCurrent).name);
										bundle.putString("ap", wpCurrent.name);
									} else {
										//Log.w("anemoi", "else 2");
										fpCurrent = -2;
										wpCurrent = wpStop;
										
										//bundle.putString("ap", "STOP Condition");
										bundle.putString("ap", wpCurrent.name);
									}
								}
							}
						}
					}
					
					//bundle.putString("ap", flightPlan.get(fpCurrent).name);
					bundle.putString("ap", wpCurrent.name);
					break;
			}
			
			//Log.w("anemoi", "end of case");
			
			//distToTarget = locNow.distanceTo(locWaypoint);
			
			
			
			// 3. step - recalculate bearing from location to waypoint
			
			r_n_rud = locNow.bearingTo(locWaypoint);		// where to
			if (r_n_rud < 0.0) {
				r_n_rud = r_n_rud + 360.0; // bearing (-180/+180 to compass heading 0-360)
			}
			
			//Log.i("anemoi", Float.toString(locNow.distanceTo(locWaypoint)));
			//Log.i("anemoi", Float.toString(locNow.bearingTo(locWaypoint)));
			//Log.i("anemoi", Double.toString(r_n_rud));
			
			
			//targetAltitudeDifference = targetAltitude - alt + flightPlan.get(fpCurrent).altDiff;
			targetAltitudeDifference = targetAltitude - alt + wpCurrent.altDiff;
			
			// height difference (constraint from 1-100)
			if (Math.abs(targetAltitudeDifference) > 100.0) {
				r_n_elv = Math.sqrt(Math.abs(100)/(Math.log(6-Math.log(Math.abs(100)))));
			} else if (Math.abs(targetAltitudeDifference) > 1.0) {
				r_n_elv = Math.sqrt(Math.abs(targetAltitudeDifference)/(Math.log(6-Math.log(Math.abs(targetAltitudeDifference)))));
			} else {
				// if we're off by one meter call it close enough (otherwise log function can give inf)
				r_n_elv = 0;
			}
			
			//Log.w("anemoi", "alt dif");
			
			//Log.w("anemoi", "ALTITUDE DIFF: " + targetAltitudeDifference);
			
			/*
			// old
			r_n_elv = Math.sqrt(Math.abs(targetAltitudeDifference)) / Math.log(2);
			//r_n_elv = Math.log(Math.abs(targetAltitudeDifference)) / Math.log(1.5);
			
			//if (Math.abs(targetAltitudeDifference) >= 100) {
			//	r_n_elv = Math.log(Math.abs(targetAltitudeDifference)) / Math.log(1.5);
			//}
			//else if (Math.abs(targetAltitudeDifference) >= 10) {
			//	r_n_elv = Math.log(Math.abs(targetAltitudeDifference)) / Math.log(3.5);
			//}
			//else {
			//	r_n_elv = Math.log(Math.abs(targetAltitudeDifference)) / Math.log(5.5);
			//}
			*/
			
			
			
			if (targetAltitudeDifference < 0.0) {
				r_n_elv = r_n_elv * -1;
			}
			
			//Log.w("anemoi", "call gps tracking");
			reactGPSTracking(lat, lon, alt, hdg, spd, acc, bundle.getString("ap"), locWaypoint.getLatitude(), locWaypoint.getLongitude());
//			// !! this might be better somehow not like this
//			if (trackingHandler != null) {
//				Log.i("anemoi", "GPS transmitt");
//				
//				Message msg = Message.obtain();
//				msg.obj = Double.toString(lon) + ";" + Double.toString(lat) + ";" + Double.toString(alt) + ";" + Double.toString(spd) + ";" + Double.toString(acc) + ";" + bundle.getString("ap") + "\n";
//				trackingHandler.sendMessage(msg);				
//			}
			
			
			Message msgOut =  Message.obtain(null, MSG_AP, 0, 0);
			msgOut.setData(bundle);
			for (int i=mClients.size()-1; i>=0; i--) {
				try {
            		mClients.get(i).send(msgOut);
            	} catch (RemoteException e) {
                    mClients.remove(i);
                }
            }
		};
		
		// PID controller
		private void reactSensors(double in_yaw, double in_pitch, double in_roll, long in_time) {
			//Log.i("anemoi", "Sensors");
			
			/// PID-Controller
			timeCHANGE = in_time - timeBEFORE;
			
			// yaw
			
			y_n_rud = in_yaw;
			
			Double rudErrRight = (360.0 - y_n_rud) + r_n_rud;
			Double rudErrLeft = (-1.0 * y_n_rud) + r_n_rud;
			
			if (Math.abs(rudErrRight) < Math.abs(rudErrLeft)) {
				e_n_rud = rudErrRight;
			} else {
				e_n_rud = rudErrLeft;
			}
			
			//if (y_n_rud > 180) {
			//	//y_n_rud = in_yaw - 360.0;
			//	e_n_rud = r_n_rud - (y_n_rud - 360.0);
			//}
			//else {
			//	//y_n_rud = in_yaw;
			//	e_n_rud = r_n_rud - y_n_rud;
			//}
			////e_n_rud = r_n_rud - y_n_rud;
			
			if (e_n_rud < -180.0) {
				e_n_rud = e_n_rud + 360.0;
			}
			
			if (e_n_rud > 180.0) {
				e_n_rud = e_n_rud - 360.0;
			}
			
			// which way to turn
			
			//Log.w("anemoi", "TURN: " + Integer.toString(fpDirection) + " " +  Double.toString(e_n_rud));
			if (fpDirection != 0) {					// do we care which way we turn
				//Log.i("anemoi", "TURN1: " + Integer.toString(fpDirection) + " " +  Double.toString(e_n_rud));
				if (Math.abs(e_n_rud) > 45.0) {		// if we are not "looking" at the point, check which way to turn
					
					if (fpDirection > 0) {			// we want a right turn
//						if (e_n_rud < 0.0) {		// check if we need to turn right
							e_n_rud = 170.0;		// if so, force it right until we are "looking" at the point
//						}
					}
					
					if (fpDirection < 0) {			// we want a right left
//						if (e_n_rud > 0.0) {		// check if we need to turn left
							e_n_rud = -170.0;		// if so, force it left until we are "looking" at the point
//						}
					}
					
				}
//				else {
//					fpDirection = 0;
//				}
				//Log.w("anemoi", "TURN2: " + Integer.toString(fpDirection) + " " +  Double.toString(e_n_rud));
			}
			
			//Log.i("anemoi", "LOG: " + Double.toString(in_yaw) + " " + Double.toString(r_n_rud) + " " +  Double.toString(e_n_rud));
			
			// roll
			
			//Log.w("anemoi", "TURN3: " +  Double.toString(e_n_rud));
			// we can roll to get to desired yaw faster
			if (Math.abs(e_n_rud) > 15.0) {
				
				if (e_n_rud < 0.0) {
					r_n_ail = ((e_n_rud - 15.0) / 180.0);
				} else {
					r_n_ail = ((e_n_rud + 15.0) / 180.0);
				}
				
				r_n_ail = r_n_ail * 45.0;
				
				if (r_n_ail < -30.0) {
					r_n_ail = -30.0;
				}
				
				if (r_n_ail > 30.0) {
					r_n_ail = 30.0;
				}
				//Log.w("anemoi", "TURN4: " +  Double.toString(r_n_ail));
			}
			else {
				r_n_ail = 0;	// fly with no roll
			}
			
			y_n_ail = in_roll;
			e_n_ail = r_n_ail - y_n_ail;
			
			// pitch
			y_n_elv = in_pitch;
			e_n_elv = r_n_elv - y_n_elv;
			
			// value check
			if (Double.isNaN(e_n_ail)) {
				e_n_ail = 0.0;
				Log.w("anemoi", "!!!!!!!!!!!!! AIL NAN");
			}
			if (Double.isNaN(r_n_ail)) {
				r_n_ail = 0.0;
				Log.w("anemoi", "!!!!!!!!!!!!! AIL R NAN");
			}
			
			if (Double.isNaN(e_n_rud)) {
				e_n_rud = 0.0;
				Log.w("anemoi", "!!!!!!!!!!!!! RUD NAN");
			}
			if (Double.isNaN(r_n_rud)) {
				r_n_rud = 0.0;
				Log.w("anemoi", "!!!!!!!!!!!!! RUD R NAN");
			}
			
			if (Double.isNaN(e_n_elv)) {
				e_n_elv = 0.0;
				Log.w("anemoi", "!!!!!!!!!!!!! ELE NAN");
			}
			if (Double.isNaN(r_n_elv)) {
				r_n_elv = 0.0;
				Log.w("anemoi", "!!!!!!!!!!!!! ELE R NAN");
			}
			
			
						
			// roll
			errSUM_ail += (double)(e_n_ail * timeCHANGE);
			if (errSUM_ail > 1) {
				errSUM_ail = 1;
			}
			if (errSUM_ail < -1) {
				errSUM_ail = -1;
			}
			
			// pitch
			errSUM_elv += (double)(e_n_elv * timeCHANGE);
			if (errSUM_elv > 1) {
				errSUM_elv = 1;
			}
			if (errSUM_elv < -1) {
				errSUM_elv = -1;
			}
			
			// yaw
			errSUM_rud += (double)(e_n_rud * timeCHANGE);
			if (errSUM_rud > 1) {
				errSUM_rud = 1;
			}
			if (errSUM_rud < -1) {
				errSUM_rud = -1;
			}
			
			dERR_ail = (e_n_ail - lastERR_ail) / (double)timeCHANGE;
			dERR_elv = (e_n_elv - lastERR_elv) / (double)timeCHANGE;
			dERR_rud = (e_n_rud - lastERR_rud) / (double)timeCHANGE;
			
			// !!! helping exceptin for yaw
			if (Math.abs(e_n_rud) < 0.25) {
				dERR_rud = 0.0;
			}
			
			outAileron = filterAileron.update(Kp_ail * e_n_ail + Ki_ail * errSUM_ail + Kd_ail * dERR_ail);
			//outAileron = (Kp_ail * e_n_ail + Ki_ail * errSUM_ail + Kd_ail * dERR_ail);
			if (Double.isNaN(outAileron)) {
				outAileron = 0.0;
				filterAileron = new KalmanSimple(flightSettings.kal_q_ail, flightSettings.kal_r_ail, flightSettings.kal_p_ail);
				//Log.w("anemoi", "!!!!!!!!!!!!! reset AIL");
			}
			
			if (outAileron > 1.0) {
				outAileron = 1.0;
			} else if (outAileron < -1.0) {
				outAileron = -1.0;
			}
			
			outElevator = filterElevater.update(Kp_elv * e_n_elv + Ki_elv * errSUM_elv + Kd_elv * dERR_elv);
			//outElevator = (Kp_elv * e_n_elv + Ki_elv * errSUM_elv + Kd_elv * dERR_elv);
			if (Double.isNaN(outElevator)) {
				outElevator = 0.0;
		    	filterElevater = new KalmanSimple(flightSettings.kal_q_elv, flightSettings.kal_r_elv, flightSettings.kal_p_elv);
		    	//Log.w("anemoi", "!!!!!!!!!!!!! reset ELE");
			}
			
			if (outElevator > 1.0) {
				outElevator = 1.0;
			} else if (outElevator < -1.0) {
				outElevator = -1.0;
			}
			
			outRudder = filterRudder.update(Kp_rud * e_n_rud + Ki_rud * errSUM_rud + Kd_rud * dERR_rud);
			//outRudder = (Kp_rud * e_n_rud + Ki_rud * errSUM_rud + Kd_rud * dERR_rud);
			if (Double.isNaN(outRudder)) {
				outRudder = 0.0;
		    	filterRudder = new KalmanSimple(flightSettings.kal_q_rud, flightSettings.kal_r_thr, flightSettings.kal_p_rud);
		    	//Log.w("anemoi", "!!!!!!!!!!!!! reset RUD");
			}
			//Log.w("anemoi", "RUD: " + String.format("%.3f ",outRudder) + " | " + String.format("%.3f ",e_n_rud) + " | " + String.format("%.3f ",r_n_rud) + " | " + String.format("%.3f ",y_n_rud)
			//		+ " | " + String.format("%.3f ",rudErrLeft) + " | " + String.format("%.3f ",rudErrRight));
			if (outRudder > 1.0) {
				outRudder = 1.0;
			} else if (outRudder < -1.0) {
				outRudder = -1.0;
			}
			
			// auto throttle	
			if (r_n_elv >= 0.0) {
				outThrottle = filterThrottle.update(flightEnvelope.cruiseThrottle + ((1-flightEnvelope.cruiseThrottle) / Math.sqrt(Math.abs(100)/(Math.log(6-Math.log(Math.abs(100)))))) *  Math.abs(r_n_elv));
			}
			else {
				outThrottle = filterThrottle.update(flightEnvelope.cruiseThrottle - (flightEnvelope.cruiseThrottle / Math.sqrt(Math.abs(100)/(Math.log(6-Math.log(Math.abs(100)))))) *  Math.abs(r_n_elv));
			}
			
			//Log.w("anemoi", "THRX: " + Double.toString(outThrottle) + " | " + Double.toString(r_n_elv)
			//		+ " | " + Double.toString((1-flightEnvelope.cruiseThrottle)) + " | " + Double.toString( Math.sqrt(Math.abs(100)/(Math.log(6-Math.log(Math.abs(100)))))));
			
			if (Double.isNaN(outThrottle)) {
				outThrottle = flightEnvelope.cruiseThrottle;
		    	filterThrottle = new KalmanSimple(flightSettings.kal_q_thr, flightSettings.kal_r_thr, flightSettings.kal_p_thr);
		    	//Log.w("anemoi", "!!!!!!!!!!!!! reset THR");
			}
			
			if (outThrottle > 1.0) {
				outThrottle = 1.0;
			} else if (outThrottle < 0.0) {
				outThrottle = 0.0;
			}
			
			//outThrottle = 0.5;
			//Log.w("anemoi", "THR: " + Double.toString(outThrottle) + " | " + Double.toString(e_n_elv) + " | " + Double.toString(r_n_elv));
			//Log.w("anemoi", "ELE: " + Double.toString(outElevator) + " | " + Double.toString(e_n_elv) + " | " + Double.toString(r_n_elv));
			//Log.w("anemoi", "RUD: " + Double.toString(outRudder) + " | " + Double.toString(e_n_rud) + " | " + Double.toString(r_n_rud));
			
			//Log.w("anemoi", "THR: " + String.format("%.3f ",outThrottle) + "ELE: " + String.format("%.3f ",outElevator) + "RUD: " + String.format("%.3f",outRudder));
			
			//Log.w("anemoi", "RUD: " + String.format("%.3f ",outRudder) + " | " + String.format("%.3f ",e_n_rud) + " | " + String.format("%.3f ",r_n_rud));
			
			lastERR_ail = e_n_ail;
			lastERR_elv = e_n_elv;
			lastERR_rud = e_n_rud;
			
			timeBEFORE = in_time;//currentTime;
			
//			Bundle bundle = new Bundle();
//			Message msgOut =  Message.obtain(null, MSG_ORI, 0, 0);
//			
//			bundle.putString("ori", "orireact:" + "\n" + "yaw=" + in_yaw + "\n" + "pth=" + in_pitch + "\n" + "rol=" + in_roll + "\n");
//			msgOut.setData(bundle);
//			
//			for (int i=mClients.size()-1; i>=0; i--) {
//				try {
//            		mClients.get(i).send(msgOut);
//            	} catch (RemoteException e) {
//                    // The client is dead.  Remove it from the list;
//                    // we are going through the list from back to front
//                    // so this is safe to do inside the loop.
//                    mClients.remove(i);
//                }
//            }
			
			//Log.i("anemoi", Double.toString(in_yaw) + " ! " + Double.toString(in_pitch) + " ! " + Double.toString(in_time)+ " ! " + Double.toString(in_roll));
			//Log.i("anemoi", Double.toString(e_n_rud) + " | " + Double.toString(e_n_elv) + " | " + Double.toString(in_time)+ " | " + Double.toString(e_n_ail));
			//Log.i("anemoi", Double.toString(outRudder) + " | " + Double.toString(outElevator) + " | " + Double.toString(outThrottle)+ " | " + Double.toString(outAileron));
			//
			//Log.i("anemoi" , Kp_ail + " # " + e_n_ail + " # " + Ki_ail + " # " + errSUM_ail + " # " + Kd_ail + " # " + dERR_ail);
		};
		
		// FTDI STUFF
		
		public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
			Log.w("anemoi", "SetConfig: ...");
			if (ftDev.isOpen() == false) {
	            Log.e("anemoi", "SetConfig: device not open");
	            return;
	        }

	        // configure our port
	        // reset to UART mode for 232 devices
	        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

	        ftDev.setBaudRate(baud);

	        switch (dataBits) {
	        case 7:
	            dataBits = D2xxManager.FT_DATA_BITS_7;
	            break;
	        case 8:
	            dataBits = D2xxManager.FT_DATA_BITS_8;
	            break;
	        default:
	            dataBits = D2xxManager.FT_DATA_BITS_8;
	            break;
	        }

	        switch (stopBits) {
	        case 1:
	            stopBits = D2xxManager.FT_STOP_BITS_1;
	            break;
	        case 2:
	            stopBits = D2xxManager.FT_STOP_BITS_2;
	            break;
	        default:
	            stopBits = D2xxManager.FT_STOP_BITS_1;
	            break;
	        }

	        switch (parity) {
	        case 0:
	            parity = D2xxManager.FT_PARITY_NONE;
	            break;
	        case 1:
	            parity = D2xxManager.FT_PARITY_ODD;
	            break;
	        case 2:
	            parity = D2xxManager.FT_PARITY_EVEN;
	            break;
	        case 3:
	            parity = D2xxManager.FT_PARITY_MARK;
	            break;
	        case 4:
	            parity = D2xxManager.FT_PARITY_SPACE;
	            break;
	        default:
	            parity = D2xxManager.FT_PARITY_NONE;
	            break;
	        }

	        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

	        short flowCtrlSetting;
	        switch (flowControl) {
	        case 0:
	            flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
	            break;
	        case 1:
	            flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
	            break;
	        case 2:
	            flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
	            break;
	        case 3:
	            flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
	            break;
	        default:
	            flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
	            break;
	        }

	        // TODO : flow ctrl: XOFF/XOM
	        // TODO : flow ctrl: XOFF/XOM
	        ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);
	    }
}
