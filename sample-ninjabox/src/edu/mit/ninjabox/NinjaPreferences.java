package edu.mit.ninjabox;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.SharedPreferences;

public class NinjaPreferences implements SharedPreferences {

	private HashMap<String, ?> tempPreference;

	@Override
	public boolean contains(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Editor edit() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ?> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getBoolean(String arg0, boolean arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public float getFloat(String arg0, float arg1) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInt(String arg0, int arg1) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLong(String arg0, long arg1) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getString(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getStringSet(String arg0, Set<String> arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener arg0) {
		// TODO Auto-generated method stub
		
	}

	public class NinjaEditor implements SharedPreferences.Editor {

		@Override
		public void apply() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Editor clear() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean commit() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Editor putBoolean(String key, boolean value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Editor putFloat(String key, float value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Editor putInt(String key, int value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Editor putLong(String key, long value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Editor putString(String key, String value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Editor putStringSet(String arg0, Set<String> arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Editor remove(String key) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
