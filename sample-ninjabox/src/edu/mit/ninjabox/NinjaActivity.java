package edu.mit.ninjabox;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class NinjaActivity extends Activity {

	private static boolean _initialized;
	private static boolean isNinjaMode;
	private static boolean passwordCorrect = false;
	private static boolean abortPasswordCheck = false;
	private static String password = null;

	private NinjaActivity() {
		// Prevent instantiation
	}

	/*
	 * Override onCreate to check bundle for ninjaBox options
	 * and flip our boolean if necessary. Change window preferences.
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

	}


	/*
	 * TODO: change this to be in onCreate
	 * Forces app to be fullscreen
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
	 * If incorrect, show dialog again with message saying incorrect
	 * If correct, return true and disable sandbox mode
	 */
	public boolean checkPassword() {
		if (isNinjaMode) {
			// launch alert
			while (!passwordCorrect || abortPasswordCheck) {
				continue;
			}
		}
		return false;
	}
	
	/*
	 * sets password to get out of sandbox mode
	 * TODO(adchia):  do salting?
	 */
	public void makePassword(String s) {
		password = s;
	}
	
	/*
	 * Below we override all functions associated with launching activities and
	 * intent handling. We keep track of the intent, and instead launch an alert
	 * dialog prompting for password. If verified, then we proceed with
	 * launching the intent.
	 */
	
	@Override
	public void startActivities(Intent[] intents, Bundle options) {
		// launch dialog for password check and don't start activity until password correct
		// TODO (adchia): only do this if intent is EXTERNAL
		if (checkPassword()) {
			super.startActivities(intents, options);
		}
	}

	@Override
	public void startActivities(Intent[] intents) {

	}

	@Override
	public void startActivity(Intent intent) {

	}

	@Override
	public void startActivity(Intent intent, Bundle options) {

	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {

	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode,
			Bundle options) {

	}

	@Override
	public void startActivityFromChild(Activity child, Intent intent,
			int requestCode, Bundle options) {

	}

	@Override
	public void startActivityFromChild(Activity child, Intent intent,
			int requestCode) {

	}

	@Override
	public void startActivityFromFragment(Fragment fragment, Intent intent,
			int requestCode, Bundle options) {

	}

	@Override
	public void startActivityFromFragment(Fragment fragment, Intent intent,
			int requestCode) {

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
			int flagsMask, int flagsValues, int extraFlags, Bundle options) {

	}

	@Override
	public void startIntentSender(IntentSender intent, Intent fillInIntent,
			int flagsMask, int flagsValues, int extraFlags) {

	}

	@Override
	public void startIntentSenderForResult(IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags, Bundle options) {

	}

	@Override
	public void startIntentSenderForResult(IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags) {

	}

	@Override
	public void startIntentSenderFromChild(Activity child, IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags) {

	}

	@Override
	public void startIntentSenderFromChild(Activity child, IntentSender intent,
			int requestCode, Intent fillInIntent, int flagsMask,
			int flagsValues, int extraFlags, Bundle options) {

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
