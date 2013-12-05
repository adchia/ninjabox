package edu.mit.ninjabox;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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
			} else if (msg.what == NINJA_EVENT_TYPE.SHOW_MAKE_PASSWORD_INPUT.ordinal()){
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
		
//		savedInstanceState.get("ninjaModeOn");
		
		// initialize password dialogs
		passwordPrompt = new AlertDialog.Builder(this);
		passwordPrompt.setTitle("Input password");

		// set listener for ok when user inputs password
		passwordPrompt.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// save the attempted password
				attemptedPassword = passwordInput.getText().toString().trim();
				dialog.dismiss();
			}
		});

		// set listener for cancel when user inputs password
		passwordPrompt.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});
		
		// initialize create password alert dialogs
		createPasswordPrompt = new AlertDialog.Builder(this);
		createPasswordPrompt.setTitle("Make a password");

		// set listener for ok when user inputs password
		createPasswordPrompt.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// save the correct password
				correctPassword = passwordInput.getText().toString().trim();
				dialog.dismiss();
			}
		});

		// set listener for cancel when user inputs password
		createPasswordPrompt.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				isNinjaMode = false;
				dialog.cancel();
			}
		});
	}

	
	/*
	 * 
	 */
	public void startNinjaMode() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		isNinjaMode = true;
		showMakePasswordDialog();
	}
	
	public void stopNinjaMode() {
		// TODO(adchia): change window layout back?
		isNinjaMode = false;
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

	public NinjaPreferences getSharedPreferences(String preferenceName, int mode) {
		return null;
	}

	/*
	 * Shows the dialog for entering a user password to launch external intent
	 * If incorrect, show dialog again with message saying incorrect If correct,
	 * return true and disable sandbox mode
	 */
	public boolean showCheckPasswordDialog() {
		if (isNinjaMode) {
			// launch alert
			passwordInput = new EditText(this);
			messageHandler.sendEmptyMessage(NINJA_EVENT_TYPE.SHOW_PASSWORD_INPUT.ordinal());

			while (!checkPassword() || abortPasswordCheck) {
				continue;
			}
		}
		return checkPassword();
	}
	
	/*
	 * compares password to input password
	 */
	private boolean checkPassword() {
		// allow if we don't have a password
		if (correctPassword == null)
			return true;
		
		// check for equality
		// TODO(adchia): salting and hashing?
		if (attemptedPassword == null)
			return false;
		return correctPassword.equals(attemptedPassword);
	}
	
	/*
	 * sets password to get out of sandbox mode TODO(adchia): do salting?
	 */
	public void showMakePasswordDialog() {
		passwordInput = new EditText(this);
		messageHandler.sendEmptyMessage(NINJA_EVENT_TYPE.SHOW_MAKE_PASSWORD_INPUT.ordinal());
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
		if (showCheckPasswordDialog()) {
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
			int flagsMask, int flagsValues, int extraFlags, Bundle options) throws SendIntentException {
		super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options);
	}

	@Override
	public void startIntentSender(IntentSender intent, Intent fillInIntent,
			int flagsMask, int flagsValues, int extraFlags) throws SendIntentException {
		super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags);
	}

	@Override
	public void startIntentSenderForResult(IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags, Bundle options) throws SendIntentException {
		super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options);
	}

	@Override
	public void startIntentSenderForResult(IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags) throws SendIntentException {
		super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags);
	}

	@Override
	public void startIntentSenderFromChild(Activity child, IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags) throws SendIntentException {
		super.startIntentSenderFromChild(child, intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags);
	}

	@Override
	public void startIntentSenderFromChild(Activity child, IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags, Bundle options) throws SendIntentException {
		super.startIntentSenderFromChild(child, intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options);
	}

	@Override
	public boolean startNextMatchingActivity(Intent intent) {
		return false;
	}

	@Override
	public boolean startNextMatchingActivity(Intent intent, Bundle options) {
		return false;
	}
}
