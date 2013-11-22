package edu.mit.ninjabox;

import android.view.Window;
import android.view.WindowManager;

public class NinjaBox {

	private static boolean _initialized;
	private static boolean isNinjaMode;

	private NinjaBox() {
		// Prevent instantiation
	}
	
	public static void initialize(Window window) {
		if (!_initialized) {
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}
	
	public static NinjaPreferences getSharedPreferences(String preferenceName, int mode) {
		return null;
	}
}
