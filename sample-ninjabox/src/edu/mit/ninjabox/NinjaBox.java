package edu.mit.ninjabox;

import android.view.Window;
import android.view.WindowManager;

public class NinjaBox {

	private static boolean _initialized;

	private NinjaBox() {
		// Prevent instantiation
	}
	
	public static void initialize(Window window) {
		if (!_initialized) {
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}
}
