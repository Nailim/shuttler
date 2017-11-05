package com.oddrat.android.anemoi;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;

public class PreFlight extends Activity {
	
	private static final String PREF_NAME = "Anemoi";
	
	public final static String MESSAGE_ACTIVITY = "com.oddrat.android.anemoi.MESSAGE";
	public final static String MESSAGE_SETTINGS_AUTOPILOT = "settings_autopilot";
	public final static String MESSAGE_SETTINGS_COPILOT = "settings_copilot";
	public final static String MESSAGE_SETTINGS_AUTOPILOT_TYPE = "settings_autopilot_type";
	public final static String MESSAGE_SETTINGS_FLIGHTGEAR_ADDRESS = "settings_flightgear_address";
	public final static String MESSAGE_SETTINGS_FLIGHTGEAR_PORT = "settings_flightgear_port";
	public final static String MESSAGE_SETTINGS_MISSION_FILE = "settings_mission_file";
	public final static String MESSAGE_SETTINGS_DATA_FILEPATH = "settings_data_filepath";
	
	private static final int DIALOG_FLIGHTGEAR_ADDRESS = 0;
	private static final int DIALOG_FLIGHTGEAR_PORT = 1;
	
	private static final int ACTIVITY_MISSION_FILE = 0;
	
	private ArrayAdapter<?> xAdaptor;
	
	private Switch switch_settings_autopilot;
	private Switch switch_settings_copilot;
	
	private Spinner spinner_settings_autopilot_type;
	
	private LinearLayout linearlayout_settings_autopilot;
	private LinearLayout linearlayout_settings_copilot;
	
	private LinearLayout linearlayout_settings_autopilot_flightgear;
	
	private Button button_settings_autopilot_flightgear_address;
	private Button button_settings_autopilot_flightgear_port;
	
	private Button button_settings_autopilot_mission_file;
	
	private String data_flightgear_address;
	private String data_flightgear_port;
	private String data_mission_file;
	private String data_data_filepath;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // set screen
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setTitle("pre-flight preparation");
        
        // set view
        setContentView(R.layout.activity_preflight);
        
        // assign handlers
        switch_settings_autopilot = (Switch) findViewById(R.id.switch_settings_autopilot);
        switch_settings_autopilot.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (switch_settings_autopilot.isChecked()) {
		        	linearlayout_settings_autopilot.setVisibility(View.VISIBLE);
		        } else {
		        	linearlayout_settings_autopilot.setVisibility(View.GONE);
		        }
				
			}
		});
        
        switch_settings_copilot = (Switch) findViewById(R.id.switch_settings_copilot);
        switch_settings_copilot.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (switch_settings_copilot.isChecked()) {
		        	linearlayout_settings_copilot.setVisibility(View.VISIBLE);
		        } else {
		        	linearlayout_settings_copilot.setVisibility(View.GONE);
		        }
				
			}
		});
        
        spinner_settings_autopilot_type = (Spinner) findViewById(R.id.spinner_settings_autopilot_type);
        xAdaptor = ArrayAdapter.createFromResource(this, R.array.array_autopilot_type, android.R.layout.simple_spinner_item);
		xAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_settings_autopilot_type.setAdapter(xAdaptor);
		spinner_settings_autopilot_type.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
				if (spinner_settings_autopilot_type.getSelectedItemPosition() == 0) {
					linearlayout_settings_autopilot_flightgear.setVisibility(View.GONE);
		        }
		        else {
		        	linearlayout_settings_autopilot_flightgear.setVisibility(View.VISIBLE);
		        }
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		linearlayout_settings_autopilot = (LinearLayout) findViewById(R.id.linearlayout_settings_autopilot);
		linearlayout_settings_copilot = (LinearLayout) findViewById(R.id.linearlayout_settings_copilot);
		
		linearlayout_settings_autopilot_flightgear = (LinearLayout) findViewById(R.id.linearlayout_settings_autopilot_flightgear);
		
		button_settings_autopilot_flightgear_address = (Button) findViewById(R.id.button_settings_autopilot_flightgear_address);
		button_settings_autopilot_flightgear_address.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showDialog(DIALOG_FLIGHTGEAR_ADDRESS);
			}
		});
		button_settings_autopilot_flightgear_port = (Button) findViewById(R.id.button_settings_autopilot_flightgear_port);
		button_settings_autopilot_flightgear_port.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showDialog(DIALOG_FLIGHTGEAR_PORT);
			}
		});
        
		button_settings_autopilot_mission_file = (Button) findViewById(R.id.button_settings_autopilot_mission_file);
		button_settings_autopilot_mission_file.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//Intent intent = new Intent("com.estrongs.action.PICK_FILE");
				//intent.putExtra("com.estrongs.intent.extra.TITLE", "Open");
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		        intent.setType("file/*");
		        startActivityForResult(intent,ACTIVITY_MISSION_FILE);
			}
		});
		
        // load values
        SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
        
        // set values
        switch_settings_autopilot.setChecked(settings.getBoolean("settings_autopilot", false));
        if (switch_settings_autopilot.isChecked()) {
        	linearlayout_settings_autopilot.setVisibility(View.VISIBLE);
        } else {
        	linearlayout_settings_autopilot.setVisibility(View.GONE);
        }
        switch_settings_copilot.setChecked(settings.getBoolean("settings_copilot", false));
        if (switch_settings_copilot.isChecked()) {
        	linearlayout_settings_copilot.setVisibility(View.VISIBLE);
        } else {
        	linearlayout_settings_copilot.setVisibility(View.GONE);
        }
        spinner_settings_autopilot_type.setSelection(settings.getInt("settings_autopilot_type", 0));
        if (settings.getInt("settings_autopilot_type", 0) == 0) {
        	linearlayout_settings_autopilot_flightgear.setVisibility(View.GONE);
        }
        else {data_flightgear_address = settings.getString("settings_flightgear_address", "");
        button_settings_autopilot_flightgear_address.setText(getString(R.string.button_settings_autopilot_flightgear_address) + ": " + data_flightgear_address);
        
        	linearlayout_settings_autopilot_flightgear.setVisibility(View.VISIBLE);
        }
        
        data_flightgear_address = settings.getString("settings_flightgear_address", "");
        button_settings_autopilot_flightgear_address.setText(getString(R.string.button_settings_autopilot_flightgear_address) + ": " + data_flightgear_address);
        
        data_flightgear_port = settings.getString("settings_flightgear_port", "");
        button_settings_autopilot_flightgear_port.setText(getString(R.string.button_settings_autopilot_flightgear_port) + ": " + data_flightgear_port);
        
        data_mission_file = settings.getString("settings_mission_file", "");
        String[] separated = data_mission_file.split("/");
        button_settings_autopilot_mission_file.setText(getString(R.string.button_settings_autopilot_mission_file) + ": " + separated[separated.length-1]);
        
    	// !!! hardcoded
        if (new File(Environment.getExternalStorageDirectory().toString() + "/external_sd/").exists()) {
        	data_data_filepath = Environment.getExternalStorageDirectory().toString() + "/external_sd/Anemoi/missionTelemetry/";
        }
        else {
        	data_data_filepath = Environment.getExternalStorageDirectory().toString() + "/Anemoi/missionTelemetry/";
        }
    }
    
    @Override
	protected void onResume() {
		super.onResume();
	}
    
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onStop(){
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// save settings
    	SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        
        editor.putBoolean("settings_autopilot", switch_settings_autopilot.isChecked());
        editor.putBoolean("settings_copilot", switch_settings_copilot.isChecked());
        editor.putInt("settings_autopilot_type", spinner_settings_autopilot_type.getSelectedItemPosition());
        
        editor.putString("settings_flightgear_address", data_flightgear_address);
        editor.putString("settings_flightgear_port", data_flightgear_port);
        
        editor.putString("settings_mission_file", data_mission_file);
        //editor.putString("settings_data_filepath", data_data_filepath);	// !!! hardcoded
        
        editor.commit();
	}

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_preflight, menu);
//        return true;
//    }
	
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder alert;
		final EditText input;
		
		switch (id) {
		case DIALOG_FLIGHTGEAR_ADDRESS:
			alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.settings_autopilot_type_simulation);
			alert.setMessage(R.string.button_settings_autopilot_flightgear_address);

			// Set an EditText view to get user input 
			input = new EditText(this);
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			  //String value = input.getText().toString();
			  data_flightgear_address = input.getText().toString();
			  button_settings_autopilot_flightgear_address.setText(getString(R.string.button_settings_autopilot_flightgear_address) + ": " + data_flightgear_address);
			  }
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			    // Canceled.
			  }
			});

			alert.show();
			break;
		case DIALOG_FLIGHTGEAR_PORT:
			alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.settings_autopilot_type_simulation);
			alert.setMessage(R.string.button_settings_autopilot_flightgear_port);

			// Set an EditText view to get user input 
			input = new EditText(this);
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			  //String value = input.getText().toString();
			  data_flightgear_port = input.getText().toString();
			  button_settings_autopilot_flightgear_port.setText(getString(R.string.button_settings_autopilot_flightgear_port) + ": " + data_flightgear_port);
			  }
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			    // Canceled.
			  }
			});

			alert.show();
			break;
		}
		
		return null;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVITY_MISSION_FILE) {
            if (resultCode == RESULT_OK) {
                data_mission_file = data.getDataString();
                
                String[] separated = data_mission_file.split("/");
                
                button_settings_autopilot_mission_file.setText(getString(R.string.button_settings_autopilot_mission_file) + ": " + separated[separated.length-1]);
            }
        }
	}

    // we just want to connect to the flight that might be in progress
    public void connect2flight(View view) {        
    	// create new intent
    	Intent intent = new Intent(this, Flight.class);
    	
    	// populate it with data
    	String message = "connect";
    	intent.putExtra(MESSAGE_ACTIVITY, message);   	
    	
    	// start next activity
    	startActivity(intent);
    	
    	// finish this one
    	finish();
    }
	
    // we are done preparing
    public void preflight2flight(View view) {        
    	// create new intent
    	Intent intent = new Intent(this, Flight.class);
    	
    	// populate it with data
    	String message = "start";
    	intent.putExtra(MESSAGE_ACTIVITY, message);
    	
    	intent.putExtra(MESSAGE_SETTINGS_AUTOPILOT, switch_settings_autopilot.isChecked());
    	intent.putExtra(MESSAGE_SETTINGS_COPILOT, switch_settings_copilot.isChecked());
    	intent.putExtra(MESSAGE_SETTINGS_AUTOPILOT_TYPE, spinner_settings_autopilot_type.getSelectedItemPosition());
    	intent.putExtra(MESSAGE_SETTINGS_FLIGHTGEAR_ADDRESS, data_flightgear_address);
    	intent.putExtra(MESSAGE_SETTINGS_FLIGHTGEAR_PORT, data_flightgear_port);
    	intent.putExtra(MESSAGE_SETTINGS_MISSION_FILE, data_mission_file);
    	intent.putExtra(MESSAGE_SETTINGS_DATA_FILEPATH, data_data_filepath);
    	
    	
    	// start next activity
    	startActivity(intent);
    	
    	// finish this one
    	finish();
    }
}