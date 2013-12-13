package edu.mit.ninjabox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;

public class NinjaActivity extends Activity {
	private static final String[] EMPTY_FILE_LIST = {};
	
	private static boolean isNinjaMode;
	private static String correctPassword = null;
	private String attemptedPassword = null;
	private static AlertDialog.Builder passwordPrompt;
	private static AlertDialog.Builder createPasswordPrompt;
	private static EditText passwordInput;
	private int oldFlag;
	private NinjaActivity thisActivity;

	private static String ninjaPkgName;
	private static String ninjaAlias1;
	private static String ninjaAlias2;
	
	private TelephonyManager tm;
	private CallStateListener callStateListener;
	
	private File sandboxFilesDir; 
	private File sandboxCacheDir;
	private File sandboxPreferencesDir;
	private HashMap<String, File> sandboxExternalFilesDirs;
	private File sandboxExternalCacheDir;
	private File sandboxDatabaseDir;
	
	private static HashMap<String, NinjaPreferences> ninjaSharedPrefs;
	
	private final Object mSync = new Object();

	private enum NINJA_EVENT_TYPE {
		SHOW_PASSWORD_INPUT, SHOW_MAKE_PASSWORD_INPUT
	}

	private final Handler messageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == NINJA_EVENT_TYPE.SHOW_PASSWORD_INPUT.ordinal()) {
				if (attemptedPassword != null)
					passwordInput.setText(attemptedPassword);
				passwordPrompt.setView(passwordInput);
				passwordPrompt.show();
				displayingOwnDialog = true;
			} else if (msg.what == NINJA_EVENT_TYPE.SHOW_MAKE_PASSWORD_INPUT
					.ordinal()) {
				createPasswordPrompt.setView(passwordInput);
				createPasswordPrompt.show();
				displayingOwnDialog = true;
			} else {
				Log.e("Handler Error", "Invalid message passed to handler");
			}
		}
	};
	
	private boolean keepClosingRecentApps = false;
	private boolean displayingOwnDialog = false;
	
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
		thisActivity = this;
		
		// initialize password dialogs
		passwordPrompt = new AlertDialog.Builder(this);
		passwordPrompt.setTitle("Input password");

		// set listener for ok when user inputs password
		passwordPrompt.setPositiveButton("Ok",
				new DialogInterface.OnClickListener() {
					@Override
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
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});

		// initialize create password alert dialogs
		createPasswordPrompt = new AlertDialog.Builder(this);
		createPasswordPrompt.setTitle("Make a password");

		// set listener for ok when user inputs password
		createPasswordPrompt.setPositiveButton("Ok",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// save the correct password
						correctPassword = passwordInput.getText().toString()
								.trim();
						Intent i = getIntent();
						i.putExtra("isNinjaMode", true);
						i.putExtra("ninjaPkgName", ninjaPkgName);
						i.putExtra("ninjaAlias1", ninjaAlias1);
						i.putExtra("ninjaAlias2", ninjaAlias2);
						i.putExtra("correctPassword", correctPassword);
						finish();
						startActivity(i);
						dialog.dismiss();
					}
				});

		// set listener for cancel when user inputs password
		createPasswordPrompt.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						stopNinjaMode();
						dialog.cancel();
					}
				});
		
		
		// get ninja mode parameters
		isNinjaMode = getIntent().getBooleanExtra("isNinjaMode", isNinjaMode);
		if (ninjaPkgName == null) {
			ninjaPkgName = getIntent().getStringExtra("ninjaPkgName");
			ninjaAlias1 = getIntent().getStringExtra("ninjaAlias1");
			ninjaAlias2 = getIntent().getStringExtra("ninjaAlias2");
			correctPassword = getIntent().getStringExtra("correctPassword");
		}		
		
		if (isNinjaMode) {
			initializeNinjaMode();
		}
	}

	/*
	 * 
	 */

	public boolean isNinjaMode() {
		return isNinjaMode;
	}

	public void initializeNinjaMode() {
		oldFlag = getWindow().getAttributes().flags;
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		sandboxFilesDir = new File(super.getFilesDir(), "sandbox_files");
		sandboxCacheDir = new File(super.getCacheDir(), "sandbox_cache");
		sandboxExternalFilesDirs = new HashMap<String, File>();
		sandboxExternalCacheDir = new File(super.getExternalCacheDir(), "sandbox_external_cache");
		sandboxDatabaseDir = new File(this.getCacheDir(), "sandbox_data");
		try {
			sandboxFilesDir.createNewFile();
			sandboxCacheDir.createNewFile();
			sandboxExternalCacheDir.createNewFile();
			sandboxDatabaseDir.createNewFile();
		} catch (Exception e) {
		    Log.d("NINJAACTIVITY", "failed to save file - " + e.toString());
		}
		
		callStateListener = new CallStateListener();
		tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}
	
	public void startNinjaMode(String pkgname, String alias1, String alias2) {
		ninjaAlias1 = alias1;
		ninjaAlias2 = alias2;
		ninjaPkgName = pkgname;
		refreshLauncherDefault();
		
		if (ninjaSharedPrefs != null) {
			for (String preferenceName : ninjaSharedPrefs.keySet()) {
				ninjaSharedPrefs.get(preferenceName).deleteFile();
			}
		}
		ninjaSharedPrefs = new HashMap<String, NinjaPreferences>();
		showMakePasswordDialog();
	}

	public void stopNinjaMode() {
		passwordPrompt.setPositiveButton("Ok",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// save the attempted password
						attemptedPassword = passwordInput.getText().toString()
								.trim();
						dialog.dismiss();

						if (checkPassword()) {
							getWindow().setFlags(oldFlag, ~0);
							refreshLauncherDefault();
							tm.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
							
							// delete files
							sandboxFilesDir.delete();
							sandboxCacheDir.delete();
							sandboxExternalCacheDir.delete();
							sandboxDatabaseDir.delete();
							
							for (File file : sandboxExternalFilesDirs.values()) {
								file.delete();
							}
							
							for (String preferenceName : ninjaSharedPrefs.keySet()) {
								ninjaSharedPrefs.get(preferenceName).deleteFile();
							}
							
							ninjaSharedPrefs.clear();

							sandboxExternalFilesDirs.clear();
							
							// keep this at end!
							Intent i = getIntent();
							i.putExtra("isNinjaMode", false);
							finish();
							startActivity(i);
						}
					}
				});
		showCheckPasswordDialog();
	}

	private void refreshLauncherDefault() {

		PackageManager pm = getPackageManager();
		// ComponentName cn1 = new ComponentName("com.example.sample_ninjabox",
		// "com.example.sample_ninjabox.LoginAlias");
		// ComponentName cn2 = new ComponentName("com.example.sample_ninjabox",
		// "com.example.sample_ninjabox.LoginAlias-copy");
		ComponentName cn1 = new ComponentName(ninjaPkgName,
				ninjaAlias1);
		ComponentName cn2 = new ComponentName(ninjaPkgName,
				ninjaAlias2);
		int dis = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

		if (pm.getComponentEnabledSetting(cn1) == dis)
			dis = 3 - dis;
		pm.setComponentEnabledSetting(cn1, dis, PackageManager.DONT_KILL_APP);
		pm.setComponentEnabledSetting(cn2, 3 - dis,
				PackageManager.DONT_KILL_APP);

		pm.clearPackagePreferredActivities(getPackageName());
	}
	

	/*
	 * Shows the dialog for entering a user password to launch external intent
	 * If incorrect, show dialog again with message saying incorrect If correct,
	 * return true and disable sandbox mode
	 */
	public void showCheckPasswordDialog() {
		if (isNinjaMode) {
			// launch alert
			passwordInput = new EditText(this);
			passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
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

		// check for equality
		if (attemptedPassword == null)
			return false;

		if (correctPassword.equals(attemptedPassword))
			return true;

		return false;
	}

	/*
	 * sets password to get out of sandbox mode TODO(adchia): do salting?
	 */
	public void showMakePasswordDialog() {
		passwordInput = new EditText(this);
		passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		messageHandler
				.sendEmptyMessage(NINJA_EVENT_TYPE.SHOW_MAKE_PASSWORD_INPUT
						.ordinal());
	}

	private boolean isExternal(Intent... intents) {
		for (Intent intent : intents) {
			List<ResolveInfo> activities = getPackageManager()
					.queryIntentActivities(intent, 0);
			for (ResolveInfo resolveInfo : activities) {
				ActivityInfo info = resolveInfo.activityInfo;
				try {
					String pkgName = info.name.substring(0,
							info.name.lastIndexOf("."));
					ActivityInfo test = getPackageManager().getActivityInfo(
							new ComponentName(pkgName, info.name), 0);
					Log.d("NINJAACTIVITY", test.name);
				} catch (PackageManager.NameNotFoundException e) {
					Log.d("NINJAACTIVITY", "Not in this package :O");
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * Below we override all functions associated with launching activities and
	 * intent handling. We keep track of the intent, and instead launch an alert
	 * dialog prompting for password. If verified, then we proceed with
	 * launching the intent.
	 */

	@Override
	public void startActivities(final Intent[] intents, final Bundle options) {
		// launch dialog for password check and don't start activity until
		// password correct

		if (isExternal(intents) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;
								// insert whatever was intended.
								((Activity) thisActivity)
										.startActivities(intents);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startActivities(intents, options);
		}
	}

	@Override
	public void startActivities(final Intent[] intents) {
		if (isExternal(intents) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startActivities(intents);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startActivities(intents);
		}
	}

	@Override
	public void startActivity(final Intent intent) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity).startActivity(intent);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startActivity(intent);
		}
	}

	@Override
	public void startActivity(final Intent intent, final Bundle options) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity).startActivity(intent,
										options);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startActivity(intent, options);
		}
	}

	@Override
	public void startActivityForResult(final Intent intent,
			final int requestCode) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startActivityForResult(intent,
												requestCode);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startActivityForResult(intent, requestCode);
		}
	}

	@Override
	public void startActivityForResult(final Intent intent,
			final int requestCode, final Bundle options) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startActivityForResult(intent,
												requestCode, options);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startActivityForResult(intent, requestCode, options);
		}
	}

	@Override
	public void startActivityFromChild(final Activity child,
			final Intent intent, final int requestCode, final Bundle options) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startActivityFromChild(child, intent,
												requestCode, options);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startActivityFromChild(child, intent, requestCode, options);
		}
	}

	@Override
	public void startActivityFromChild(final Activity child,
			final Intent intent, final int requestCode) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startActivityFromChild(child, intent,
												requestCode);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startActivity(intent);
		}
	}

	@Override
	public void startActivityFromFragment(final Fragment fragment,
			final Intent intent, final int requestCode, final Bundle options) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startActivityFromFragment(fragment,
												intent, requestCode, options);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startActivityFromFragment(fragment, intent, requestCode,
					options);
		}
	}

	@Override
	public void startActivityFromFragment(final Fragment fragment,
			final Intent intent, final int requestCode) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startActivityFromFragment(fragment,
												intent, requestCode);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startActivityFromFragment(fragment, intent, requestCode);
		}
	}

	@Override
	public boolean startActivityIfNeeded(final Intent intent,
			final int requestCode, final Bundle options) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startActivityIfNeeded(intent,
												requestCode, options);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
			return false;
		} else {
			return super.startActivityIfNeeded(intent, requestCode, options);
		}
	}

	@Override
	public boolean startActivityIfNeeded(final Intent intent,
			final int requestCode) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startActivityIfNeeded(intent,
												requestCode);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
			return false;
		} else {
			return super.startActivityIfNeeded(intent, requestCode);
		}
	}

	@Override
	public void startIntentSender(final IntentSender intent,
			final Intent fillInIntent, final int flagsMask,
			final int flagsValues, final int extraFlags, final Bundle options)
			throws SendIntentException {
		if (isExternal(fillInIntent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								try {
									((Activity) thisActivity)
											.startIntentSender(intent,
													fillInIntent, flagsMask,
													flagsValues, extraFlags,
													options);
								} catch (SendIntentException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startIntentSender(intent, fillInIntent, flagsMask,
					flagsValues, extraFlags, options);
		}
	}

	@Override
	public void startIntentSender(final IntentSender intent,
			final Intent fillInIntent, final int flagsMask,
			final int flagsValues, final int extraFlags)
			throws SendIntentException {
		if (isExternal(fillInIntent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								try {
									((Activity) thisActivity)
											.startIntentSender(intent,
													fillInIntent, flagsMask,
													flagsValues, extraFlags);
								} catch (SendIntentException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startIntentSender(intent, fillInIntent, flagsMask,
					flagsValues, extraFlags);
		}
	}

	@Override
	public void startIntentSenderForResult(final IntentSender intent,
			final int requestCode, final Intent fillInIntent,
			final int flagsMask, final int flagsValues, final int extraFlags,
			final Bundle options) throws SendIntentException {
		if (isExternal(fillInIntent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								try {
									((Activity) thisActivity)
											.startIntentSenderForResult(intent,
													requestCode, fillInIntent,
													flagsMask, flagsValues,
													extraFlags, options);
								} catch (SendIntentException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startIntentSenderForResult(intent, requestCode, fillInIntent,
					flagsMask, flagsValues, extraFlags, options);
		}
	}

	@Override
	public void startIntentSenderForResult(final IntentSender intent,
			final int requestCode, final Intent fillInIntent,
			final int flagsMask, final int flagsValues, final int extraFlags)
			throws SendIntentException {
		if (isExternal(fillInIntent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								try {
									((Activity) thisActivity)
											.startIntentSenderForResult(intent,
													requestCode, fillInIntent,
													flagsMask, flagsValues,
													extraFlags);
								} catch (SendIntentException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startIntentSenderForResult(intent, requestCode, fillInIntent,
					flagsMask, flagsValues, extraFlags);
		}
	}

	@Override
	public void startIntentSenderFromChild(final Activity child, final IntentSender intent,
			final int requestCode, final Intent fillInIntent, final int flagsMask,
			final int flagsValues, final int extraFlags) throws SendIntentException {
		if (isExternal(fillInIntent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								try {
									((Activity) thisActivity)
											.startIntentSenderFromChild(child, intent,
													requestCode, fillInIntent,
													flagsMask, flagsValues,
													extraFlags);
								} catch (SendIntentException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startIntentSenderFromChild(child, intent, requestCode,
					fillInIntent, flagsMask, flagsValues, extraFlags);
		}
	}

	@Override
	public void startIntentSenderFromChild(final Activity child, final IntentSender intent,
			final int requestCode, final Intent fillInIntent, final int flagsMask,
			final int flagsValues, final int extraFlags, final Bundle options)
			throws SendIntentException {
		if (isExternal(fillInIntent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								try {
									((Activity) thisActivity)
											.startIntentSenderFromChild(child, intent,
													requestCode, fillInIntent,
													flagsMask, flagsValues,
													extraFlags, options);
								} catch (SendIntentException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.startIntentSenderFromChild(child, intent, requestCode,
					fillInIntent, flagsMask, flagsValues, extraFlags, options);
		}
	}

	@Override
	public boolean startNextMatchingActivity(final Intent intent) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startNextMatchingActivity(intent);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
			return false;
		} else {
			return super.startNextMatchingActivity(intent);
		}
	}

	@Override
	public boolean startNextMatchingActivity(final Intent intent, final Bundle options) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.startNextMatchingActivity(intent, options);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
			return false;
		} else {
			return super.startNextMatchingActivity(intent, options);
		}
	}

	@Override
	public void sendBroadcast(final Intent intent) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.sendBroadcast(intent);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.sendBroadcast(intent);
		}
	}

	@Override
	public void sendBroadcast(final Intent intent, final String receiverPermission) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.sendBroadcast(intent, receiverPermission);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.sendBroadcast(intent, receiverPermission);
		}
	}

	@Override
    public void sendOrderedBroadcast(final Intent intent, final String receiverPermission) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.sendOrderedBroadcast(intent, receiverPermission);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.sendOrderedBroadcast(intent, receiverPermission);
		}
	}

	@Override
    public void sendOrderedBroadcast(final Intent intent,
    		final String receiverPermission, final BroadcastReceiver resultReceiver,
    		final Handler scheduler, final int initialCode, final String initialData,
    		final Bundle initialExtras) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}
	}

	@Override
    public void sendStickyBroadcast(final Intent intent) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.sendStickyBroadcast(intent);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.sendStickyBroadcast(intent);
		}
	}

	@Override
    public void sendStickyOrderedBroadcast(final Intent intent,
    		final BroadcastReceiver resultReceiver,
    		final Handler scheduler, final int initialCode, final String initialData,
    		final Bundle initialExtras) {
		if (isExternal(intent) && isNinjaMode) {
			passwordPrompt.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// save the attempted password
							attemptedPassword = passwordInput.getText()
									.toString().trim();
							if (checkPassword()) {
								isNinjaMode = false;
								attemptedPassword = null;

								// insert whatever was intended.
								((Activity) thisActivity)
										.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
							} else {
								Toast toast = Toast.makeText(getApplicationContext(),
										"Incorrect Password", Toast.LENGTH_SHORT);
								toast.show();
							}
							dialog.dismiss();
						}
					});
			showCheckPasswordDialog();
		} else {
			super.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
		}
	}

	/*
	 * Below we override hardware key events. If isNinjaMode, we disable
	 * functionality of the back button
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (isNinjaMode) {
			if (keyCode == KeyEvent.KEYCODE_BACK
					|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
					|| keyCode == KeyEvent.KEYCODE_VOLUME_UP
					|| keyCode == KeyEvent.KEYCODE_SEARCH) {
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (isNinjaMode) {
			if (keyCode == KeyEvent.KEYCODE_BACK
					|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
					|| keyCode == KeyEvent.KEYCODE_VOLUME_UP
					|| keyCode == KeyEvent.KEYCODE_SEARCH) {
				return true;
			}
		}
        return super.onKeyLongPress(keyCode, event);
    }
	
	@Override
	public boolean onSearchRequested() {
		if (isNinjaMode)
			return true;
		return super.onSearchRequested();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		ActivityManager am = (ActivityManager) getApplicationContext()
				.getSystemService(Context.ACTIVITY_SERVICE);
		ComponentName cn = am.getRunningTasks(1).get(0).topActivity;

		if (isNinjaMode && !hasFocus && isExternal(Intent.makeMainActivity(cn))) {
			//Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
			//sendBroadcast(closeDialog);
			keepClosingRecentApps = true;
			windowCloseHandler.postDelayed(windowCloserRunnable, 0);
		}
	}

	private void toggleRecents() {
		Intent closeRecents = new Intent("com.android.systemui.recent.action.TOGGLE_RECENTS");
		closeRecents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
				| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		ComponentName recents = new ComponentName("com.android.systemui",
				"com.android.systemui.recent.RecentsActivity");
		closeRecents.setComponent(recents);
		
		isNinjaMode = false;
		this.startActivity(closeRecents);	
		isNinjaMode = true;
	}
	
	// return to this app.
	// close system dialogs
	private void returnToApp() {
		Intent returnIntent = getIntent();
		Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		
		isNinjaMode = false;
		this.sendBroadcast(closeDialog);
		this.startActivity(returnIntent);	
		isNinjaMode = true;
	}

	private Handler windowCloseHandler = new Handler();
	private Runnable windowCloserRunnable = new Runnable() {

		@Override
		public void run() {
			while (keepClosingRecentApps && !displayingOwnDialog) {
				ActivityManager am = (ActivityManager) getApplicationContext()
						.getSystemService(Context.ACTIVITY_SERVICE);
				ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
	
				if (cn != null
						&& cn.getClassName().equals(
								"com.android.systemui.recent.RecentsActivity")) {
					keepClosingRecentApps = false;
					toggleRecents();
				} else { // for any other app that tries to steal focus, return to app
					returnToApp();
				}
			}
		}
	};

	/**
	* Listener to detect incoming calls. 
	*/
	private class CallStateListener extends PhoneStateListener {
	  @Override
	  public void onCallStateChanged(int state, String incomingNumber) {
	      switch (state) {
	          case TelephonyManager.CALL_STATE_RINGING:
	          // called when someone is ringing to this phone
	        	  try {
	        		  Class c = Class.forName(tm.getClass().getName());
	        		  Method m = c.getDeclaredMethod("getITelephony");
	        		  m.setAccessible(true);
	        		  ITelephony telephonyService = (ITelephony) m.invoke(tm);
	        		  telephonyService.endCall();
	        	  } catch (Exception e) {
	        		  e.printStackTrace();
	        	  } 
	          
	          break;
	      }
	  }
	}
	
	// Preferences

	@Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        SharedPreferences sp;
        if (isNinjaMode) {
        	synchronized (ninjaSharedPrefs) {
	            sp = ninjaSharedPrefs.get(name);
	            if (sp == null) {
	            	Log.d("NINJAACTIVITY", "Creating new NinjaPreferences");
	                File prefsFile = getSharedPrefsFile(name);
	                if (prefsFile.exists()) {
	                	prefsFile.delete();
	                }
	                sp = new NinjaPreferences(prefsFile, mode);
	                ninjaSharedPrefs.put(name, (NinjaPreferences) sp);
	            }
	            return sp;
        	} 
        }
        return super.getSharedPreferences(name, mode);
    }
	
	public File getSharedPrefsFile(String name) {
        return makeFilename(getPreferencesDir(), name + ".xml");
    }
	
	private File getPreferencesDir() {
        synchronized (mSync) {
            if (sandboxPreferencesDir == null) {
            	sandboxPreferencesDir = new File(super.getApplicationInfo().dataDir, "sandbox_shared_prefs");
            }
            return sandboxPreferencesDir;
        }
    }
	
	private File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException(
                "File " + name + " contains a path separator");
    }

	/*
	 * Below we override all functions associated with data storage I/O. If
	 * isNinjaMode, we store all data in a new directory of our choice, which we
	 * delete when the user exits Ninja Mode.
	 */

	@Override
	public FileInputStream openFileInput(String name) throws FileNotFoundException {
		// super.openFileInput() calls getFilesDir(), which has already been overridden
		return super.openFileInput(name);
	}

	@Override
	public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
		// super.openFileOutput() calls getFilesDir(), which has already been overridden
		return super.openFileOutput(name, mode);
	}

	@Override
	public boolean deleteFile(String name) {
		// super.deleteFile() calls getFilesDir(), which has already been overridden
		return super.deleteFile(name);
	}

	@Override
	public File getFilesDir() {
		if (isNinjaMode) {
			return sandboxFilesDir;
		} else {
			return super.getFilesDir();
		}
	}
	
	@Override
	public File getCacheDir() {
		if (isNinjaMode) {
			return sandboxCacheDir;
		} else {
			return super.getCacheDir();
		}
	}

	@Override
	public File getFileStreamPath(String name) {
		// super.getFileStreamPath() calls getFilesDir(), which has already been overridden
		return super.getFileStreamPath(name);
	}

	@Override
	public String[] fileList() {
		// super.fileList() calls getFilesDir(), which has already been overridden
		return super.fileList();
	}

	@Override
	public File getExternalFilesDir(String type) {
		if (isNinjaMode) {
			if (!sandboxExternalFilesDirs.containsKey(type)) {
				File myDir = super.getExternalFilesDir("sandbox_" + type);
				sandboxExternalFilesDirs.put(type, myDir);
			} 
			return sandboxExternalFilesDirs.get(type);
		} else {
			return super.getExternalFilesDir(type);
		}
	}

	@Override
	public File getExternalCacheDir() {
		if (isNinjaMode) {
			return sandboxExternalCacheDir;
		} else {
			return super.getExternalCacheDir();
		}
	}

	@Override
	public String[] databaseList() {
		if (isNinjaMode) {
			final String[] list = sandboxDatabaseDir.list();
			return (list != null) ? list : EMPTY_FILE_LIST;
		} else {
			return super.databaseList();
		}
	}

	@Override
	public boolean deleteDatabase(String name) {
		if (isNinjaMode) {
			return super.deleteDatabase(sandboxDatabaseDir + name);
		} else {
			return super.deleteDatabase(name);
		}
	}

	@Override
	public File getDatabasePath(String name) {
		if (isNinjaMode) {
			return sandboxDatabaseDir;
		} else {
			return super.getDatabasePath(name);
		}
	}

	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode,
			SQLiteDatabase.CursorFactory factory) {
		if (isNinjaMode) {
			return super.openOrCreateDatabase(sandboxDatabaseDir + name, mode, factory);
		} else {
			return super.openOrCreateDatabase(name, mode, factory);
		}
	}

	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode,
			SQLiteDatabase.CursorFactory factory,
			DatabaseErrorHandler errorHandler) {
		if (isNinjaMode) {
			return super.openOrCreateDatabase(sandboxDatabaseDir + name, mode, factory);
		} else {
			return super.openOrCreateDatabase(name, mode, factory, errorHandler);
		}
	}
}
