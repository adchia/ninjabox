package edu.mit.ninjabox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

public class NinjaActivity extends Activity {

	private static boolean _initialized;
	private static boolean isNinjaMode;
	private static boolean abortPasswordCheck = false;
	private static String correctPassword = null;
	private static String attemptedPassword = null;
	private static AlertDialog.Builder passwordPrompt;
	private static AlertDialog.Builder createPasswordPrompt;
	private static EditText passwordInput;
	private int oldFlag;
	
	private String ninjaPkgName;
	private String ninjaAlias1;
	private String ninjaAlias2;

	private enum NINJA_EVENT_TYPE {
		SHOW_PASSWORD_INPUT, SHOW_MAKE_PASSWORD_INPUT
	}

	private static final Handler messageHandler = new Handler() {

		public void handleMessage(Message msg) {
			if (msg.what == NINJA_EVENT_TYPE.SHOW_PASSWORD_INPUT.ordinal()) {
				if (attemptedPassword != null)
					passwordInput.setText(attemptedPassword);
				passwordPrompt.setView(passwordInput);
				passwordPrompt.show();
			} else if (msg.what == NINJA_EVENT_TYPE.SHOW_MAKE_PASSWORD_INPUT
					.ordinal()) {
				createPasswordPrompt.setView(passwordInput);
				createPasswordPrompt.show();
			} else {
				Log.e("Handler Error", "Invalid message passed to handler");
			}
		}
	};

	/*
	 * Override onCreate to check bundle for ninjaBox options and flip our
	 * boolean if necessary. Change window preferences.
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// savedInstanceState.get("ninjaModeOn");

		// initialize password dialogs
		passwordPrompt = new AlertDialog.Builder(this);
		passwordPrompt.setTitle("Input password");

		// set listener for ok when user inputs password
		passwordPrompt.setPositiveButton("Ok",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// save the attempted password
						attemptedPassword = passwordInput.getText().toString()
								.trim();
						dialog.dismiss();
					}
				});

		// set listener for cancel when user inputs password
		passwordPrompt.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						abortPasswordCheck = true;
						dialog.cancel();
					}
				});

		// initialize create password alert dialogs
		createPasswordPrompt = new AlertDialog.Builder(this);
		createPasswordPrompt.setTitle("Make a password");

		// set listener for ok when user inputs password
		createPasswordPrompt.setPositiveButton("Ok",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// save the correct password
						correctPassword = passwordInput.getText().toString()
								.trim();
						dialog.dismiss();
					}
				});

		// set listener for cancel when user inputs password
		createPasswordPrompt.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						stopNinjaMode();
						dialog.cancel();
					}
				});
	}

	/*
	 * 
	 */
	public void startNinjaMode(String pkgname, String alias1, String alias2) {
		oldFlag = getWindow().getAttributes().flags;
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		isNinjaMode = true;
		this.ninjaPkgName = pkgname;
		this.ninjaAlias1 = alias1;
		this.ninjaAlias2 = alias2;
		refreshLauncherDefault();
		showMakePasswordDialog();
	}

	public void stopNinjaMode() {
//		showCheckPasswordDialog();
//		if (checkPassword()) {
//			getWindow().setFlags(oldFlag, ~0);
//			isNinjaMode = false;
			refreshLauncherDefault();
//		}
	}

	private void refreshLauncherDefault() {
        
        PackageManager pm = getPackageManager();
        //ComponentName cn1 = new ComponentName("com.example.sample_ninjabox", "com.example.sample_ninjabox.LoginAlias");
        //ComponentName cn2 = new ComponentName("com.example.sample_ninjabox", "com.example.sample_ninjabox.LoginAlias-copy");
        ComponentName cn1 = new ComponentName(this.ninjaPkgName, this.ninjaAlias1);
        ComponentName cn2 = new ComponentName(this.ninjaPkgName, this.ninjaAlias2);
        int dis = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        
        if(pm.getComponentEnabledSetting(cn1) == dis) dis = 3 - dis;
        pm.setComponentEnabledSetting(cn1, dis, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(cn2, 3 - dis, PackageManager.DONT_KILL_APP);
        
        pm.clearPackagePreferredActivities(getPackageName());
	}
	
	/*
	 * TODO: change this to be in onCreate Forces app to be fullscreen
	 */
	@Deprecated
	public static void initialize(Window window) {
		if (!_initialized) {
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

//	public NinjaPreferences getSharedPreferences(String preferenceName, int mode) {
//		return null;
//	}

	/*
	 * Shows the dialog for entering a user password to launch external intent
	 * If incorrect, show dialog again with message saying incorrect If correct,
	 * return true and disable sandbox mode
	 */
	public void showCheckPasswordDialog() {
		if (isNinjaMode) {
			// launch alert
			passwordInput = new EditText(this);
			messageHandler
					.sendEmptyMessage(NINJA_EVENT_TYPE.SHOW_PASSWORD_INPUT
							.ordinal());
		}
	}

	/*
	 * compares password to input password
	 */
	private boolean checkPassword() {
		// allow if we don't have a password
		if (correctPassword == null)
			return true;
		
		while (!abortPasswordCheck) {
			// check for equality
			if (attemptedPassword == null)
				continue;
			
			abortPasswordCheck = true;
			if (correctPassword.equals(attemptedPassword))
				return true;
		}
		
		return false;
	}

	/*
	 * sets password to get out of sandbox mode TODO(adchia): do salting?
	 */
	public void showMakePasswordDialog() {
		passwordInput = new EditText(this);
		messageHandler
				.sendEmptyMessage(NINJA_EVENT_TYPE.SHOW_MAKE_PASSWORD_INPUT
						.ordinal());
	}

	/*
	 * Below we override all functions associated with launching activities and
	 * intent handling. We keep track of the intent, and instead launch an alert
	 * dialog prompting for password. If verified, then we proceed with
	 * launching the intent.
	 */

	@Override
	public void startActivities(Intent[] intents, Bundle options) {
		// launch dialog for password check and don't start activity until
		// password correct
		// TODO (adchia): only do this if intent is EXTERNAL
		showCheckPasswordDialog();
		if (checkPassword()) {
			super.startActivities(intents, options);
		}
	}

	@Override
	public void startActivities(Intent[] intents) {
		super.startActivities(intents);
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
	}

	@Override
	public void startActivity(Intent intent, Bundle options) {
		super.startActivity(intent, options);
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode,
			Bundle options) {
		super.startActivityForResult(intent, requestCode, options);
	}

	@Override
	public void startActivityFromChild(Activity child, Intent intent,
			int requestCode, Bundle options) {
		super.startActivityFromChild(child, intent, requestCode, options);
	}

	@Override
	public void startActivityFromChild(Activity child, Intent intent,
			int requestCode) {
		super.startActivityFromChild(child, intent, requestCode);
	}

	@Override
	public void startActivityFromFragment(Fragment fragment, Intent intent,
			int requestCode, Bundle options) {
		super.startActivityFromFragment(fragment, intent, requestCode, options);
	}

	@Override
	public void startActivityFromFragment(Fragment fragment, Intent intent,
			int requestCode) {
		super.startActivityFromFragment(fragment, intent, requestCode);
	}

	@Override
	public boolean startActivityIfNeeded(Intent intent, int requestCode,
			Bundle options) {
		return false;
	}

	@Override
	public boolean startActivityIfNeeded(Intent intent, int requestCode) {
		return false;
	}

	@Override
	public void startIntentSender(IntentSender intent, Intent fillInIntent,
			int flagsMask, int flagsValues, int extraFlags, Bundle options)
			throws SendIntentException {
		super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues,
				extraFlags, options);
	}

	@Override
	public void startIntentSender(IntentSender intent, Intent fillInIntent,
			int flagsMask, int flagsValues, int extraFlags)
			throws SendIntentException {
		super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues,
				extraFlags);
	}

	@Override
	public void startIntentSenderForResult(IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags, Bundle options)
			throws SendIntentException {
		super.startIntentSenderForResult(intent, requestCode, fillInIntent,
				flagsMask, flagsValues, extraFlags, options);
	}

	@Override
	public void startIntentSenderForResult(IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags) throws SendIntentException {
		super.startIntentSenderForResult(intent, requestCode, fillInIntent,
				flagsMask, flagsValues, extraFlags);
	}

	@Override
	public void startIntentSenderFromChild(Activity child, IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags) throws SendIntentException {
		super.startIntentSenderFromChild(child, intent, requestCode,
				fillInIntent, flagsMask, flagsValues, extraFlags);
	}

	@Override
	public void startIntentSenderFromChild(Activity child, IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags, Bundle options)
			throws SendIntentException {
		super.startIntentSenderFromChild(child, intent, requestCode,
				fillInIntent, flagsMask, flagsValues, extraFlags, options);
	}

	@Override
	public boolean startNextMatchingActivity(Intent intent) {
		return false;
	}

	@Override
	public boolean startNextMatchingActivity(Intent intent, Bundle options) {
		return false;
	}
	
	/*
	 * Below we override hardware key events.
	 * If isNinjaMode, we disable functionality of the back button
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (isNinjaMode) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				return true;
			}
		}
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (isNinjaMode && !hasFocus) {
			//Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            //sendBroadcast(closeDialog);
			windowCloseHandler.postDelayed(windowCloserRunnable, 50);
		}
	}

	private void toggleRecents() {
		Intent closeRecents = new Intent("com.android.systemui.recent.action.TOGGLE_RECENTS");
		closeRecents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		ComponentName recents = new ComponentName("com.android.systemui", "com.android.systemui.recent.RecentsActivity");
		closeRecents.setComponent(recents);
		this.startActivity(closeRecents);
	}

	private Handler windowCloseHandler = new Handler();
	private Runnable windowCloserRunnable = new Runnable() {
	       
		@Override
		public void run() {
			ActivityManager am = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
			ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
			
			if (cn != null && cn.getClassName().equals("com.android.systemui.recent.RecentsActivity")) {
				toggleRecents();
			}
		}
	};
	
	/*
	 * Below we override all functions associated with data storage I/O. 
	 * If isNinjaMode, we store all data in a new directory of our choice, 
	 * which we delete when the user exits Ninja Mode.
	 */
	
	@Override
	public FileInputStream openFileInput(String name) throws FileNotFoundException {
		String myName = name;
		if (isNinjaMode) {
			// THIS IS WRONG
			myName += "_sandbox";
		} 
			
		return super.openFileInput(name);
	}
	
	@Override
	public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
		String myName = name;
		if (isNinjaMode) {
			// THIS IS WRONG
			myName += "_sandbox";
		} 
			
		return super.openFileOutput(name, mode);
	}
	
	@Override
	public boolean deleteFile(String name) {
		return super.deleteFile(name);
	}
	
	@Override
	public File getDir(String name, int mode) {
		return super.getDir(name, mode);
	}
	
	@Override
	public File getFilesDir() {
		return super.getFilesDir();
	}
	
	@Override
	public File getFileStreamPath(String name) {
		return super.getFileStreamPath(name);
	}
	
	@Override
	public String[] fileList() {
		return super.fileList();
	}
	
	@Override
	public File getExternalFilesDir(String type) {
		return super.getExternalFilesDir(type);
	}
	
	public static File getExternalStoragePublicDirectory(String type) {
		return Environment.getExternalStoragePublicDirectory(type);
	}
	
	@Override
	public File getCacheDir() {
		File myFile = super.getCacheDir();
		if (isNinjaMode) {
			// 1) check if ninja mode cache folder exists - if not, create
			// 2) change myFile to ninja mode cache folder
		} 
			
		return myFile;
	}
	
	@Override
	public File getExternalCacheDir() {
		return super.getExternalCacheDir();
	}
	
	@Override
	public String[] databaseList() {
		return super.databaseList();
	}
	
	@Override
	public boolean deleteDatabase(String name) {
		return super.deleteDatabase(name);
	}

	@Override
	public File getDatabasePath(String name) {
		return super.getDatabasePath(name);
	}
	
	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
		return super.openOrCreateDatabase(name, mode, factory);
	}
	
	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
		return super.openOrCreateDatabase(name, mode, factory, errorHandler);
	}
	
}
