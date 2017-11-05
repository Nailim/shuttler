package com.oddrat.android.anemoi;

import java.lang.ref.WeakReference;

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
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class Flight extends Activity {
	
	private copilotService mBoundCopilotService;
	
	private Messenger mBoundAutopilotService;
	private final Messenger mMessenger = new Messenger(new IncomingHandler(this));
	
	private static Button button_startstop;
	private static Button button_startstop_autopilot;
	private static Button button_startstop_copilot;
	
	private static Boolean isAutopilotServiceRunning_checked = false;
	private static Boolean isCopilotServiceRunning_checked = false;
	
	private static Boolean isAutopilotServiceRunning = false;
	private static Boolean isCopilotServiceRunning = false;
	
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
        
        button_startstop = (Button) findViewById(R.id.button_startstop);
        button_startstop.setOnClickListener(mStartStop);
        
        button_startstop_autopilot = (Button) findViewById(R.id.button_startstop_autopilot);
        button_startstop_autopilot.setOnClickListener(mStartStop_autopilot);
        
        button_startstop_copilot = (Button) findViewById(R.id.button_startstop_copilot);
        button_startstop_copilot.setOnClickListener(mStartStop_copilot);
        
        

        // Get the message from the intent
        Intent intent = getIntent();
        String message = intent.getStringExtra(PreFlight.MESSAGE_ACTIVITY);

        // Create the text view
        TextView textView = (TextView) this.findViewById(R.id.tv_flight);
        textView.setTextSize(40);
        textView.setText(message);
        
        Log.i("anemoi", "onCreate ...");
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		bindService(new Intent(Flight.this, autopilotService.class), mAutopilotConnection, Context.BIND_AUTO_CREATE);
		bindService(new Intent(Flight.this, copilotService.class), mCopilotConnection, Context.BIND_AUTO_CREATE );		
		
		// Get the message from the intent
        Intent intent = getIntent();
        
        Boolean autopilot = intent.getBooleanExtra(PreFlight.MESSAGE_SETTINGS_AUTOPILOT, false);
        Boolean copilot = intent.getBooleanExtra(PreFlight.MESSAGE_SETTINGS_COPILOT, false);
        
        if ((autopilot == false) && (copilot == false)) {
        	AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.text_error_user);
			alert.setMessage(R.string.text_error_user_no_selection);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
				}
			});
			alert.show();
        }
		
		Log.i("anemoi", "onResume ...");
    }
    
	@Override
	protected void onPause() {
		super.onPause();
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
	 * Handler of incoming messages from services
	 */
	static class IncomingHandler extends Handler {
		private final WeakReference<Flight> mTarget;
		
		IncomingHandler(Flight target) {
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
		            	Bundle bundle = msg.getData();
		            	isAutopilotServiceRunning = bundle.getBoolean("isAutopilotServiceRunning");
		            	if (isAutopilotServiceRunning) {
		    				button_startstop.setText(R.string.button_startstop_off);
		    			}
		            	isAutopilotServiceRunning_checked = true;
		            	
		            	Log.i("anemoi", "Here 1");
		        		Log.i("anemoi", isAutopilotServiceRunning.toString());
		        		removeMessages(autopilotService.MSG_IS_SERVICE_RUNNING);
		        		break;
		            default:
		                super.handleMessage(msg);
		        }
	        }
	    }
	}
	
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
	            msg.replyTo = mMessenger;
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
			if (isAutopilotServiceRunning || isCopilotServiceRunning) {
				stopService(new Intent(Flight.this, autopilotService.class));
				stopService(new Intent(Flight.this, copilotService.class));
				button_startstop.setText(R.string.button_startstop_on);
			}
			else {
				startService(new Intent(Flight.this, autopilotService.class));
				startService(new Intent(Flight.this, copilotService.class));
				button_startstop.setText(R.string.button_startstop_off);
			}
		}
	};
	
	private OnClickListener mStartStop_autopilot = new OnClickListener() {
		public void onClick(View v) {
			Log.i("anemoi", "Button");
		}
	};
	
	private OnClickListener mStartStop_copilot = new OnClickListener() {
		public void onClick(View v) {
			Log.i("anemoi", "Button");
		}
	};
}
