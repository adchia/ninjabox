package edu.mit.ninjabox;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.xmlpull.v1.XmlPullParserException;

import com.android.internal.util.XmlUtils;

import android.content.SharedPreferences;
import android.util.Log;

import libcore.io.ErrnoException;
import libcore.io.IoUtils;
import libcore.io.Libcore;
import libcore.io.StructStat;

public class NinjaPreferences implements SharedPreferences {
	private static final String TAG = "NinjaPreferences";
	
	private HashMap<String, ?> tempPreference;
	private final File mFile;
	private final File mBackupFile;
	private final int mMode;

	private Map<String, Object> mMap;     // guarded by 'this'
	private int mDiskWritesInFlight = 0;  // guarded by 'this'
	private boolean mLoaded = false;      // guarded by 'this'
	private long mStatTimestamp;          // guarded by 'this'
	private long mStatSize;               // guarded by 'this'

	private final Object mWritingToDiskLock = new Object();
	private static final Object mContent = new Object();
	private final WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners =
			new WeakHashMap<OnSharedPreferenceChangeListener, Object>();;

	public NinjaPreferences(File file, int mode) {
        mFile = file;
        mBackupFile = makeBackupFile(file);
        mMode = mode;
        mLoaded = false;
        mMap = null;
        startLoadFromDisk();
    }
	
	private void startLoadFromDisk() {
        synchronized (this) {
            mLoaded = false;
        }
        new Thread("NinjaPreferences-load") {
            public void run() {
                synchronized (NinjaPreferences.this) {
                    loadFromDiskLocked();
                }
            }
        }.start();
    }

    private void loadFromDiskLocked() {
        if (mLoaded) {
            return;
        }
        if (mBackupFile.exists()) {
            mFile.delete();
            mBackupFile.renameTo(mFile);
        }

        // Debugging
        if (mFile.exists() && !mFile.canRead()) {
            Log.w(TAG, "Attempt to read preferences file " + mFile + " without permission");
        }

        Map map = null;
        StructStat stat = null;
        try {
            stat = Libcore.os.stat(mFile.getPath());
            if (mFile.canRead()) {
                BufferedInputStream str = null;
                try {
                    str = new BufferedInputStream(
                            new FileInputStream(mFile), 16*1024);
                    map = XmlUtils.readMapXml(str);
                } catch (XmlPullParserException e) {
                    Log.w(TAG, "getSharedPreferences", e);
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "getSharedPreferences", e);
                } catch (IOException e) {
                    Log.w(TAG, "getSharedPreferences", e);
                } finally {
                    IoUtils.closeQuietly(str);
                }
            }
        } catch (ErrnoException e) {
        }
        mLoaded = true;
        if (map != null) {
            mMap = map;
            mStatTimestamp = stat.st_mtime;
            mStatSize = stat.st_size;
        } else {
            mMap = new HashMap<String, Object>();
        }
        notifyAll();
    }

	private static File makeBackupFile(File prefsFile) {
		return new File(prefsFile.getPath() + ".bak");
	}

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
