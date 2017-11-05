package com.oddrat.android.anemoi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Flight extends Activity {
	
	private copilotService mBoundCopilotService;
	
	private Messenger mBoundAutopilotService;
	private final Messenger mMessengerServices = new Messenger(new ServicesIncomingHandler(this));
	
	private static Intent intent;
	private static AlertDialog.Builder alert;
	
	private static TextView textview_gps;
	private static TextView textview_ori;
	private static TextView textview_ap;
	private static TextView textview_ftdi;
	
	private static Button button_startstop;
	private static Button button_startstop_autopilot;
	private static Button button_startstop_copilot;
	
	private static RelativeLayout relativelayout_splash;
	
	private static LinearLayout linearlayout_controls_single;
	private static LinearLayout linearlayout_controls_multiple;
	
	private static Boolean isAutopilotServiceRunning_checked = false;
	private static Boolean isCopilotServiceRunning_checked = false;
	
	private static Boolean isAutopilotServiceRunning = false;
	private static Boolean isCopilotServiceRunning = false;
	
	private static String data_session_folder;
	
	public final static String MESSAGE_DATA_SESSION_FOLDER = "settings_session_folder";
	
	/**
	 * Life cycle methods
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setTitle("flight in progress");
        
        setContentView(R.layout.activity_flight);
        
        // assign flags
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        button_startstop = (Button) findViewById(R.id.button_startstop);
        button_startstop.setOnClickListener(mStartStop);
        
        button_startstop_autopilot = (Button) findViewById(R.id.button_startstop_autopilot);
        button_startstop_autopilot.setOnClickListener(mStartStop_autopilot);
        
        button_startstop_copilot = (Button) findViewById(R.id.button_startstop_copilot);
        button_startstop_copilot.setOnClickListener(mStartStop_copilot);
        
        relativelayout_splash = (RelativeLayout) findViewById(R.id.relativelayout_splash);
        linearlayout_controls_single = (LinearLayout) findViewById(R.id.linearlayout_controls_single);
        linearlayout_controls_multiple = (LinearLayout) findViewById(R.id.linearlayout_controls_multiple);
        
        textview_gps = (TextView) findViewById(R.id.textview_gps);
        textview_ori = (TextView) findViewById(R.id.textview_ori);
        textview_ap = (TextView) findViewById(R.id.textview_ap);
        textview_ftdi = (TextView) findViewById(R.id.textview_ftdi);
        
        // Get the message from the intent
        Intent intentx = getIntent();
        String message = intentx.getStringExtra(PreFlight.MESSAGE_ACTIVITY);

        // Create the text view
        TextView textView = (TextView) this.findViewById(R.id.textview_flight);
        textView.setTextSize(40);
        textView.setText(message);
        
        textview_gps.setText("gps");
        textview_gps.setTextSize(15);
        
        textview_ori.setText("ori");
        textview_ori.setTextSize(15);
        
        textview_ap.setText("ap");
        textview_ap.setTextSize(15);

        textview_ftdi.setText("ftdi");
        textview_ftdi.setTextSize(15);
        
        Log.i("anemoi", "onCreate ...");
        
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		// connect to threads
		bindService(new Intent(Flight.this, autopilotService.class), mAutopilotConnection, Context.BIND_AUTO_CREATE);
		bindService(new Intent(Flight.this, copilotService.class), mCopilotConnection, Context.BIND_AUTO_CREATE );		
		
		// assign intent handler
		intent = getIntent();
		// assign alert handler
		alert  = new AlertDialog.Builder(this);
		
		// wait till everything is synced
		new Thread(new Runnable() {
	        public void run() {
	            while (!isAutopilotServiceRunning_checked || !isCopilotServiceRunning_checked) {
	            	try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            }
	            SetupHandler.sendEmptyMessage(0);	// when everything is synced,
	        }
	    }).start();
		
		Log.i("anemoi", "onResume ...");
    }
    
	@Override
	protected void onPause() {
		super.onPause();
		if (!isAutopilotServiceRunning) {
			stopService(new Intent(Flight.this, autopilotService.class));
		}
		if (!isCopilotServiceRunning) {
			stopService(new Intent(Flight.this, copilotService.class));
		}
		unbindService(mAutopilotConnection);
		unbindService(mCopilotConnection);
		
		Log.i("anemoi", "onPause ...");
		finish();	// if we paused it, we lost it, might as well destroy it
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		
		Log.i("anemoi", "onStop ...");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.i("anemoi", "onDestroy ...");
	}
	
	/**
	 * Handlers
	 */
	// handles messages from the services
	static class ServicesIncomingHandler extends Handler {
		private final WeakReference<Flight> mTarget;
		
		ServicesIncomingHandler(Flight target) {
			mTarget = new WeakReference<Flight>(target);
	    } 
		
	    @Override
	    public void handleMessage(Message msg) {
	    	Flight target = mTarget.get(); 
	        
	    	if (target != null){
		    	switch (msg.what) {
		            case autopilotService.MSG_ECHO:
		                //mCallbackText.setText("Received from service: " + msg.arg1);
		                break;
		            case autopilotService.MSG_IS_SERVICE_RUNNING:
		            	Bundle bundle_MSG_IS_SERVICE_RUNNING = msg.getData();
		            	isAutopilotServiceRunning = bundle_MSG_IS_SERVICE_RUNNING.getBoolean("isAutopilotServiceRunning");
		            	if (isAutopilotServiceRunning) {
		    				button_startstop.setText(R.string.button_startstop_off);
		    				button_startstop_autopilot.setText(R.string.button_startstop_autopilot_off);
		    			}
		            	isAutopilotServiceRunning_checked = true;
		            	
		            	Log.i("anemoi", "Here 1");
		        		Log.i("anemoi", isAutopilotServiceRunning.toString());
		        		//removeMessages(autopilotService.MSG_IS_SERVICE_RUNNING);
		        		break;
		            case autopilotService.MSG_GPS:
		            	Bundle bundle_MSG_GPS = msg.getData();
		            	textview_gps.setText(bundle_MSG_GPS.getString("gps"));
		            	//Log.i("anemoi", "Here x");
		            	break;
		            case autopilotService.MSG_ORI:
		            	Bundle bundle_MSG_ORI = msg.getData();
		            	textview_ori.setText(bundle_MSG_ORI.getString("ori"));
		            	//Log.i("anemoi", "Here x");
		            	break;
		            case autopilotService.MSG_AP:
		            	Bundle bundle_MSG_AP = msg.getData();
		            	textview_ap.setText(bundle_MSG_AP.getString("ap"));
		            	break;
		            case autopilotService.MSG_FTDI:
		            	Bundle bundle_MSG_FTDI = msg.getData();
		            	textview_ftdi.setText(bundle_MSG_FTDI.getString("ftdi"));
		            	break;
		            default:
		                super.handleMessage(msg);
		        }
	        }
	    }
	}
	
	// handles the activity setup
	private Handler SetupHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// if activity is started with starting in mind
			if (intent.getStringExtra(PreFlight.MESSAGE_ACTIVITY).equals("start")) {
				// if already running - we can't start
				if (isAutopilotServiceRunning || isCopilotServiceRunning) {
					alert.setTitle(R.string.text_error_user);
					alert.setMessage(R.string.text_error_user_activity);
					alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							//finish();
						}
					});
					alert.show();
				}
				// if starting without anything to do
				if (!intent.getBooleanExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT, false) && !intent.getBooleanExtra(PreFlight.MESSAGE_SETTINGS_COPILOT, false) ) {
					alert.setTitle(R.string.text_error_user);
					alert.setMessage(R.string.text_error_user_no_activity);
					alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							startActivity(new Intent(Flight.this, PreFlight.class));
							//finish();
						}
					});
					alert.show();
				}
				
				// what to show in UI
				if (intent.getBooleanExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT, false) && intent.getBooleanExtra(PreFlight.MESSAGE_SETTINGS_COPILOT, false) ) {
					linearlayout_controls_single.setVisibility(View.VISIBLE);
				}
				else {
					linearlayout_controls_multiple.setVisibility(View.VISIBLE);
					
					if (intent.getBooleanExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT, false)) {
						button_startstop_autopilot.setVisibility(View.VISIBLE);
					}
					if (intent.getBooleanExtra(PreFlight.MESSAGE_SETTINGS_COPILOT, false)) {
						button_startstop_copilot.setVisibility(View.VISIBLE);
					}
				}
			}
			
			// if activity is started with connecting in mind
			if (intent.getStringExtra(PreFlight.MESSAGE_ACTIVITY).equals("connect")) {
				// if connecting without anything running already
				if (!isAutopilotServiceRunning && !isCopilotServiceRunning) {
					alert.setTitle(R.string.text_error_user);
					alert.setMessage(R.string.text_error_user_no_activity);
					alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							startActivity(new Intent(Flight.this, PreFlight.class));
							//finish();
						}
					});
					alert.show();
				}
				
				// what to show in UI
				if (isAutopilotServiceRunning && isCopilotServiceRunning) {
					linearlayout_controls_single.setVisibility(View.VISIBLE);
				}
				else {
					linearlayout_controls_multiple.setVisibility(View.VISIBLE);
					
					if (isAutopilotServiceRunning) {
						button_startstop_autopilot.setVisibility(View.VISIBLE);
					}
					if (isCopilotServiceRunning) {
						button_startstop_copilot.setVisibility(View.VISIBLE);
					}
				}
				
					
			}
			
			// if activity is started from service
			if (intent.getStringExtra(PreFlight.MESSAGE_ACTIVITY).equals("service")) {
				
				// what to show in UI
				if (isAutopilotServiceRunning && isCopilotServiceRunning) {
					linearlayout_controls_single.setVisibility(View.VISIBLE);
				}
				else {
					linearlayout_controls_multiple.setVisibility(View.VISIBLE);
					
					if (isAutopilotServiceRunning) {
						button_startstop_autopilot.setVisibility(View.VISIBLE);
					}
					if (isCopilotServiceRunning) {
						button_startstop_copilot.setVisibility(View.VISIBLE);
					}
				}
			}
			
			// starting services if necessary
			// autopilot
			if ((!isAutopilotServiceRunning) && (intent.getBooleanExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT, false))) {
				Intent intentX = new Intent(Flight.this, autopilotService.class);
				intentX.putExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT_TYPE, intent.getIntExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT_TYPE, 0));
				intentX.putExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_ADDRESS, intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_ADDRESS));
				intentX.putExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_PORT, intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_PORT));
				intentX.putExtra(PreFlight.MESSAGE_SETTINGS_MISSION_FILE, intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_MISSION_FILE));
				
				startService(intentX);
			}
			// autopilot
			if ((!isCopilotServiceRunning) && (intent.getBooleanExtra(PreFlight.MESSAGE_SETTINGS_COPILOT, false))) {
				Intent intentX = new Intent(Flight.this, copilotService.class);
				//intentX.putExtra(MESSAGE_DATA_SESSION_FOLDER, data_sessionFolder);
				
				startService(intentX);
			}
			
			relativelayout_splash.setVisibility(View.GONE);
		}
	};
	
	/**
	 * Service connections
	 */
	private ServiceConnection mAutopilotConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mBoundAutopilotService = new Messenger(service);
	        //mCallbackText.setText("Attached.");

	        // We want to monitor the service for as long as we are
	        // connected to it.
	        try {
	            Message msg = Message.obtain(null, autopilotService.MSG_REGISTER_CLIENT);
	            msg.replyTo = mMessengerServices;
	            mBoundAutopilotService.send(msg);
	            
	            // Give it some value as an example.
	            //msg = Message.obtain(null, autopilotService.MSG_ECHO, this.hashCode(), 0);
	            //mBoundAutopilotService.send(msg);
	            msg = Message.obtain(null, autopilotService.MSG_IS_SERVICE_RUNNING, 0, 0);
	            mBoundAutopilotService.send(msg);
	            Log.i("anemoi", "Here 0");
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }
	        Log.i("anemoi", "Buuu");
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mBoundAutopilotService = null;
	        //mCallbackText.setText("Disconnected.");
	    }
	};
	
	private ServiceConnection mCopilotConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundCopilotService = ((copilotService.LocalBinder)service).getService();
			
			//Log.i("anemoi", mBoundCopilotService.isCopilotServiceRunning().toString());
			
			isCopilotServiceRunning = mBoundCopilotService.isCopilotServiceRunning();
			if (isCopilotServiceRunning) {
				button_startstop.setText(R.string.button_startstop_off);
				button_startstop.setText(R.string.button_startstop_off);
				button_startstop_copilot.setText(R.string.button_startstop_copilot_off);
			}
			isCopilotServiceRunning_checked = true;
			
			Log.i("anemoi", "Here 2");
			Log.i("anemoi", isCopilotServiceRunning.toString());
		}

		public void onServiceDisconnected(ComponentName className) {
			mBoundCopilotService = null;
		}
	};
	
	/** Listeners */
	private OnClickListener mStartStop = new OnClickListener() {
		public void onClick(View v) {
			if (isAutopilotServiceRunning && isCopilotServiceRunning) {
				
				Bundle bundleX = new Bundle();
            	bundleX.putBoolean("setServiceState", false);
            	
				Message msg = Message.obtain(null, autopilotService.MSG_SET_SERVICE_STATE, 0, 0);
				msg.setData(bundleX);
				
				try {
					mBoundAutopilotService.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Bundle bundleY = new Bundle();
            	bundleY.putBoolean("setServiceState", false);
            	
            	mBoundCopilotService.setServiceState(bundleY);
				
				//stopService(new Intent(Flight.this, autopilotService.class));
				//stopService(new Intent(Flight.this, copilotService.class));
				button_startstop.setText(R.string.button_startstop_on);
				
				isAutopilotServiceRunning = false;
				isCopilotServiceRunning = false;
				
				//startActivity(new Intent(Flight.this, PostFlight.class));
				startActivity(new Intent(Flight.this, PreFlight.class));
				//finish();
			}
			else {
				data_session_folder = getSessionDataFolder();
				
				Bundle bundleX = new Bundle();
            	bundleX.putBoolean("setServiceState", true);
            	bundleX.putString(MESSAGE_DATA_SESSION_FOLDER, data_session_folder);
            	
				Message msg = Message.obtain(null, autopilotService.MSG_SET_SERVICE_STATE, 0, 0);
				msg.setData(bundleX);
				
				try {
					mBoundAutopilotService.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Bundle bundleY = new Bundle();
            	bundleY.putBoolean("setServiceState", true);
            	bundleY.putString(MESSAGE_DATA_SESSION_FOLDER, data_session_folder);
				
            	mBoundCopilotService.setServiceState(bundleY);
				
				//startService(new Intent(Flight.this, autopilotService.class));
				//startService(new Intent(Flight.this, copilotService.class));
				button_startstop.setText(R.string.button_startstop_off);
				
				isAutopilotServiceRunning = true;
				isCopilotServiceRunning = true;
			}
		}
	};
	
	private OnClickListener mStartStop_autopilot = new OnClickListener() {
		public void onClick(View v) {
			Log.i("anemoi", "aaaaaaaa-pilot");
			if (isAutopilotServiceRunning) {
				//stopService(new Intent(Flight.this, autopilotService.class));
				Bundle bundleX = new Bundle();
            	bundleX.putBoolean("setServiceState", false);
            	
				Message msg = Message.obtain(null, autopilotService.MSG_SET_SERVICE_STATE, 0, 0);
				msg.setData(bundleX);
				
				try {
					mBoundAutopilotService.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				button_startstop_autopilot.setText(R.string.button_startstop_autopilot_on);
				
				isAutopilotServiceRunning = false;
				
				//startActivity(new Intent(Flight.this, PostFlight.class));
				startActivity(new Intent(Flight.this, PreFlight.class));
				//finish();
			}
			else {
				data_session_folder = getSessionDataFolder();
				
				//Intent intentX = new Intent(Flight.this, autopilotService.class);
				//intentX.putExtra(MESSAGE_DATA_SESSION_FOLDER, data_sessionFolder);
				//intentX.putExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT_TYPE, intent.getIntExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT_TYPE, 0));
				//intentX.putExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_ADDRESS, intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_ADDRESS));
				//intentX.putExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_PORT, intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_FLIGHTGEAR_PORT));
				//
				//startService(intentX);
				Bundle bundleX = new Bundle();
            	bundleX.putBoolean("setServiceState", true);
            	bundleX.putString(MESSAGE_DATA_SESSION_FOLDER, data_session_folder);
            	
				Message msg = Message.obtain(null, autopilotService.MSG_SET_SERVICE_STATE, 0, 0);
				msg.setData(bundleX);
				
				try {
					mBoundAutopilotService.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				button_startstop_autopilot.setText(R.string.button_startstop_autopilot_off);
				
				isAutopilotServiceRunning = true;
			}
		}
	};
	
	private OnClickListener mStartStop_copilot = new OnClickListener() {
		public void onClick(View v) {
			Log.i("anemoi", "cocococo-pilot");
			if (isCopilotServiceRunning) {
				//stopService(new Intent(Flight.this, copilotService.class));
				Bundle bundleX = new Bundle();
            	bundleX.putBoolean("setServiceState", false);
            	
            	mBoundCopilotService.setServiceState(bundleX);
            	
				button_startstop_copilot.setText(R.string.button_startstop_copilot_on);
				
				isCopilotServiceRunning = false;
				
				//startActivity(new Intent(Flight.this, PostFlight.class));
				startActivity(new Intent(Flight.this, PreFlight.class));
				//finish();
			}
			else {
				data_session_folder = getSessionDataFolder();
				
				Bundle bundleX = new Bundle();
            	bundleX.putBoolean("setServiceState", true);
            	bundleX.putString(MESSAGE_DATA_SESSION_FOLDER, data_session_folder);
				
            	mBoundCopilotService.setServiceState(bundleX);
				
				//startService(new Intent(Flight.this, copilotService.class));
				button_startstop_copilot.setText(R.string.button_startstop_copilot_off);
				
				isCopilotServiceRunning = true;
			}
		}
	};
	
	/** misc */
	// generate session data path and create folder
	private String getSessionDataFolder() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
        
        String[] separatedURI = intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_MISSION_FILE).split("///");
        String filePath = "/" + separatedURI[separatedURI.length-1];
        
        String[] separatedPath = intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_MISSION_FILE).split("/");
        String fileName = separatedPath[separatedPath.length-1];
        
        String[] separatedFile = fileName.split("\\.");
        
        String sessionDataFolder = "";
        
        switch (intent.getIntExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT_TYPE, 0)) {
        case 0:	// android
        	sessionDataFolder = intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_DATA_PATH) + "missionTelemetry/" + "android_" + sdf.format(new Date()) + "_mission-" + separatedFile[0];
        	break;
        case 1:	// flightgear
        	sessionDataFolder = intent.getStringExtra(PreFlight.MESSAGE_SETTINGS_DATA_PATH) + "missionTelemetry/" + "flightgear_" + sdf.format(new Date()) + "_mission-" + separatedFile[0];
        	break;
        }
        Log.i("anemoi", sessionDataFolder);
        
        // !!! no safety checks
        // create session directory structure
        new File(sessionDataFolder).mkdir();
        new File(sessionDataFolder + "/mission").mkdir();
        new File(sessionDataFolder + "/logs").mkdir();
        new File(sessionDataFolder + "/media").mkdir();
        
        // copy files
        try {
        	// copy mission file
        	File fileIn = new File(filePath);
            File fileOut = new File(sessionDataFolder + "/mission/mission.myFly");
            
			InputStream streamIn = new FileInputStream(fileIn);
			OutputStream streamOut = new FileOutputStream(fileOut);
			
			byte[] buf = new byte[1024];
			int len;
			
			while ((len = streamIn.read(buf)) > 0){
				streamOut.write(buf, 0, len);
			}
			
			streamIn.close();
			streamOut.close();
			
			// copy session manifest file
			fileOut = new File(sessionDataFolder + "/manifest.txt");
			
			streamIn = getResources().openRawResource(R.raw.manifest);
			streamOut = new FileOutputStream(fileOut);
			
			while ((len = streamIn.read(buf)) > 0){
				streamOut.write(buf, 0, len);
			}
			
			streamIn.close();
			streamOut.close();
        } catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        
		return sessionDataFolder;
	}
}
