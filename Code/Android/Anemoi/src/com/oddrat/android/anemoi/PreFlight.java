package com.oddrat.android.anemoi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
	public final static String MESSAGE_SETTINGS_DATA_PATH = "settings_data_path";
	
	private static final int DIALOG_FLIGHTGEAR_ADDRESS = 0;
	private static final int DIALOG_FLIGHTGEAR_PORT = 1;
	private static final int DIALOG_ANEMOI_PATH = 2;
	
	private static final int ACTIVITY_MISSION_FILE = 0;
	//private static final int ACTIVITY_DATA_PATH = 1;		// replaced with custom dialog
	
	private ArrayAdapter<?> xAdaptor;
	
	private Switch switch_settings_autopilot;
	private Switch switch_settings_copilot;
	
	private Spinner spinner_settings_autopilot_type;
	
	private LinearLayout linearlayout_settings_autopilot;
	private LinearLayout linearlayout_settings_copilot;
	
	private LinearLayout linearlayout_settings_autopilot_flightgear;
	
	private Button button_settings_autopilot_data_path;
	private Button button_settings_autopilot_data_path_check;
	
	private Button button_settings_autopilot_flightgear_address;
	private Button button_settings_autopilot_flightgear_port;
	
	private Button button_settings_autopilot_mission_file;
	
	private Button button_settings_folder_up;
	private Button button_settings_folder_select;
	
	private File root;
	private File curFolder;
	
	private ListView dialog_folder_listview;
	
	private List<String> fileList = new ArrayList<String>();
	
	
	private String data_flightgear_address;
	private String data_flightgear_port;
	private String data_mission_file;
	private String data_data_path;
	
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
		
		button_settings_autopilot_data_path = (Button) findViewById(R.id.button_settings_autopilot_data_path);
		button_settings_autopilot_data_path.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				showDialog(DIALOG_ANEMOI_PATH);
			}
		});
		root = new File("/");
		curFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
		
		button_settings_autopilot_data_path_check = (Button) findViewById(R.id.button_settings_autopilot_data_path_check);
		// redundant by activity_preflight.xml
		//button_settings_autopilot_data_path_check.setOnClickListener(new OnClickListener() {
		//	
		//	public void onClick(View v) {
		//		dataFolderCreate(v);
		//	}
		//});
		
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
        
//        // check if there is mission file in the selected directory path
//        data_data_path = settings.getString("settings_data_path", "");
//        data_mission_file = settings.getString("settings_mission_file", "");
//        String[] separated = data_mission_file.split("/");
//        File missionFile = new File(data_data_path + "/Anemoi/missionFiles/" + separated[separated.length-1]);
//        if (missionFile.exists()) {
//	        button_settings_autopilot_mission_file.setText(getString(R.string.button_settings_autopilot_mission_file) + ": " + separated[separated.length-1]);
//	        //Log.d("anemoi", "yes yes yes");
//        }
//        else {
//        	button_settings_autopilot_mission_file.setText(getString(R.string.button_settings_autopilot_mission_file) + ": ");
//        	Log.d("anemoi", missionFile.getPath().toString());
//        	data_mission_file = "";
//        	//Log.d("anemoi", "no no no");
//        }
//        
//        data_data_path = settings.getString("settings_data_path", "");
//        button_settings_autopilot_data_path.setText(getString(R.string.button_settings_autopilot_data_path) + ": " + data_data_path);
//        
//        // check if the path and data folder structure is there
//        File dataPath = new File(data_data_path + "/Anemoi");
//        if ( dataPath.exists() && dataPath.isDirectory()) {
//        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
//        }
//        else {
//        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_x);
//        }
//        dataPath = new File(data_data_path + "/Anemoi/missionFiles");
//    	if ( dataPath.exists() && dataPath.isDirectory()) {
//    		button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
//    	}
//        else {
//        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_x);
//        }
//    	dataPath = new File(data_data_path + "/Anemoi/missionTelemetry");
//    	if ( dataPath.exists() && dataPath.isDirectory()) {
//    		button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
//    	}
//        else {
//        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_x);
//        }
        // replaces the code above
        data_mission_file = settings.getString("settings_mission_file", "");
        data_data_path = settings.getString("settings_data_path", "");
        button_settings_autopilot_data_path.setText(getString(R.string.button_settings_autopilot_data_path) + ": " + data_data_path);
        dataFolderCheck();
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
        editor.putString("settings_data_path", data_data_path);
        
        editor.commit();
	}

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_preflight, menu);
//        return true;
//    }
	
	 protected void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
	 
		 super.onPrepareDialog(id, dialog, bundle);
	    
		 switch(id) {
		 	case DIALOG_ANEMOI_PATH:
		 		ListDir(curFolder);
		 		break;
		 }
	 }
	
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		
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
		case DIALOG_ANEMOI_PATH:
			dialog = new Dialog(PreFlight.this);
			
			dialog.setContentView(R.layout.dialog_selectfolder);
			dialog.setTitle(R.string.dialog_selectfolder);
			
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(true);
			
			button_settings_folder_up = (Button)dialog.findViewById(R.id.button_settings_folder_up);
			button_settings_folder_up.setOnClickListener(new OnClickListener() {
				
				public void onClick(View v) {
					// TODO Auto-generated method stub
					ListDir(curFolder.getParentFile());
				}
			});
			
			button_settings_folder_select = (Button)dialog.findViewById(R.id.button_settings_folder_select);
			button_settings_folder_select.setOnClickListener(new OnClickListener(){
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
					// here we have the folder
					//Log.d("anemoi", curFolder.toString());
					data_data_path = curFolder.toString();
					button_settings_autopilot_data_path.setText(getString(R.string.button_settings_autopilot_data_path) + ": " + data_data_path);
					
					dataFolderCheck();
					Log.d("anemoi", "got folder !!!!");
					Log.d("anemoi", data_data_path);
					
					dismissDialog(DIALOG_ANEMOI_PATH);
			}});
			
			dialog_folder_listview = (ListView)dialog.findViewById(R.id.listview_folder_list);
			dialog_folder_listview.setOnItemClickListener(new OnItemClickListener(){
			      
			    public void onItemClick(AdapterView parent, View view, int position, long id) {
			    	File selected = new File(fileList.get(position));
			    	if(selected.isDirectory()){
			    		ListDir(selected); 
			    	}
			}});
			
			break;
		}
		
		return dialog;	// null if alert, dialog if dialog
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVITY_MISSION_FILE) {
            if (resultCode == RESULT_OK) {
                data_mission_file = data.getDataString();
                
                String[] separated = data_mission_file.split("/");
                
                button_settings_autopilot_mission_file.setText(getString(R.string.button_settings_autopilot_mission_file) + ": " + separated[separated.length-1]);
            }
        }
		
		// replaced with custom dialog
//		if (requestCode == ACTIVITY_DATA_PATH) {
//            if (resultCode == RESULT_OK) {
//                // !!! data_data_path = data.getDataString();
//                
//                String[] separated = data_data_path.split("/");
//                
//                button_settings_autopilot_data_path.setText(getString(R.string.button_settings_autopilot_data_path) + ": " + separated[separated.length-1]);
//            }
//        }
	}
	
    // list directory
	void ListDir(File f) {

		if (f.equals(root)) {
			button_settings_folder_up.setEnabled(false);
		} else {
			button_settings_folder_up.setEnabled(true);
		}

		curFolder = f;
		button_settings_folder_select.setText("Select Folder " + curFolder);

		File[] files = f.listFiles();
		fileList.clear();
		for (File file : files) {
			if (file.isDirectory()) {
				fileList.add(file.getPath());
			} else {
				Uri selectedUri = Uri.fromFile(file);
				String fileExtension = MimeTypeMap
						.getFileExtensionFromUrl(selectedUri.toString());
				if (fileExtension.equalsIgnoreCase("jpg")) {
					fileList.add(file.getName());
				}
			}
		}
		ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileList);
		dialog_folder_listview.setAdapter(directoryList);
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
    	//intent.putExtra(MESSAGE_SETTINGS_DATA_PATH, data_data_path);	// i changed something and forgot to add this
    	intent.putExtra(MESSAGE_SETTINGS_DATA_PATH, data_data_path + "/Anemoi/");
    	
    	Log.i("anemoi", "Mission: " + data_mission_file.toString());
    	
    	// start next activity
    	startActivity(intent);
    	
    	// finish this one
    	finish();
    }
    
    // check if the Anemoi data structure is present
    public void dataFolderCheck() {        
    	// check if the path and data folder structure is there
        File dataPath = new File(data_data_path + "/Anemoi");
        if ( dataPath.exists() && dataPath.isDirectory()) {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFF00FF00,PorterDuff.Mode.MULTIPLY);
        }
        else {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_x);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFFFF0000,PorterDuff.Mode.MULTIPLY);
        }
        
        dataPath = new File(data_data_path + "/Anemoi/missionFiles");
        if ( dataPath.exists() && dataPath.isDirectory()) {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFF00FF00,PorterDuff.Mode.MULTIPLY);
        }
        else {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_x);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFFFF0000,PorterDuff.Mode.MULTIPLY);
        }
        
        dataPath = new File(data_data_path + "/Anemoi/missionTelemetry");
        if ( dataPath.exists() && dataPath.isDirectory()) {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFF00FF00,PorterDuff.Mode.MULTIPLY);
        }
        else {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_x);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFFFF0000,PorterDuff.Mode.MULTIPLY);
        }
        
     // check if there is mission file in the selected directory path
        String[] separated = data_mission_file.split("/");
        File missionFile = new File(data_data_path + "/Anemoi/missionFiles/" + separated[separated.length-1]);
        if (missionFile.exists()) {
	        button_settings_autopilot_mission_file.setText(getString(R.string.button_settings_autopilot_mission_file) + ": " + separated[separated.length-1]);
	        data_mission_file = data_data_path + "/Anemoi/missionFiles/" + separated[separated.length-1];
	        //Log.d("anemoi", "yes yes yes");
        }
        else {
        	button_settings_autopilot_mission_file.setText(getString(R.string.button_settings_autopilot_mission_file) + ": ");
        	Log.d("anemoi", missionFile.getPath().toString());
        	data_mission_file = "";
        	//Log.d("anemoi", "no no no");
        }
    }
    // create Anemoi data folder structure
    public void dataFolderCreate(View view) {        
    	// check if the path and data folder structure is there
        File dataPath = new File(data_data_path + "/Anemoi");
        if ( dataPath.exists() && dataPath.isDirectory()) {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFF00FF00,PorterDuff.Mode.MULTIPLY);
        }
        else {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_x);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFFFF0000,PorterDuff.Mode.MULTIPLY);
        	
        	new File(data_data_path + "/Anemoi").mkdir();
        	
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFF00FF00,PorterDuff.Mode.MULTIPLY);
        }
        
        dataPath = new File(data_data_path + "/Anemoi/missionFiles");
        if ( dataPath.exists() && dataPath.isDirectory()) {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFF00FF00,PorterDuff.Mode.MULTIPLY);
        }
        else {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_x);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFFFF0000,PorterDuff.Mode.MULTIPLY);
        	
        	new File(data_data_path + "/Anemoi/missionFiles").mkdir();
        	
        	// copy default mission file
            try {    			
    			// copy session manifest file
    			File fileOut = new File(data_data_path + "/Anemoi/missionFiles/dataLogger.myFly");
    			
    			InputStream streamIn = getResources().openRawResource(R.raw.datalogger);
    			OutputStream streamOut = new FileOutputStream(fileOut);
    			
    			byte[] buf = new byte[1024];
    			int len;
    			
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
        	
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFF00FF00,PorterDuff.Mode.MULTIPLY);
        }
        
        dataPath = new File(data_data_path + "/Anemoi/missionTelemetry");
        if ( dataPath.exists() && dataPath.isDirectory()) {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFF00FF00,PorterDuff.Mode.MULTIPLY);
        }
        else {
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_x);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFFFF0000,PorterDuff.Mode.MULTIPLY);
        	
        	new File(data_data_path + "/Anemoi/missionTelemetry").mkdir();
        	
        	button_settings_autopilot_data_path_check.setText(R.string.button_settings_autopilot_data_path_check_o);
        	button_settings_autopilot_data_path_check.getBackground().setColorFilter(0xFF00FF00,PorterDuff.Mode.MULTIPLY);
        }
    }
}


