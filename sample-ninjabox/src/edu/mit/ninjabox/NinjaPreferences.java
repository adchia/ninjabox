package edu.mit.ninjabox;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;

import org.xmlpull.v1.XmlPullParserException;

import com.android.internal.util.XmlUtils;
import com.android.internal.app.QueuedWork;
import com.android.internal.os.FileUtils;

import android.content.SharedPreferences;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;

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
	
	public void deleteFile() {
		mFile.delete();
	}

	private void startLoadFromDisk() {
        synchronized (this) {
            mLoaded = false;
        }
        synchronized (NinjaPreferences.this) {
            loadFromDiskLocked();
        }
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
                    try {
						str.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
        Log.d("NINJAPREFERENCES", mMap.toString());
        notifyAll();
    }

	private static File makeBackupFile(File prefsFile) {
		return new File(prefsFile.getPath() + ".bak");
	}

	@Override
	public boolean contains(String key) {
		synchronized (this) {
            return mMap.containsKey(key);
        }
	}

	@Override
	public Editor edit() {
		return new NinjaEditor();
	}

	@Override
	public Map<String, ?> getAll() {
		synchronized(this) {
            //noinspection unchecked
            return new HashMap<String, Object>(mMap);
        }
	}

	@Override
	public boolean getBoolean(String key, boolean defValue) {
		synchronized (this) {
            Boolean v = (Boolean)mMap.get(key);
            return v != null ? v : defValue;
        }
	}

	@Override
	public float getFloat(String key, float defValue) {
		synchronized (this) {
            Float v = (Float)mMap.get(key);
            return v != null ? v : defValue;
        }
	}

	@Override
	public int getInt(String key, int defValue) {
		synchronized (this) {
            Integer v = (Integer)mMap.get(key);
            return v != null ? v : defValue;
        }
	}

	@Override
	public long getLong(String key, long defValue) {
		synchronized (this) {
            Long v = (Long)mMap.get(key);
            return v != null ? v : defValue;
        }
	}

	@Override
	public String getString(String key, String defValue) {
		synchronized (this) {
			if (mMap == null) {
				Log.d("NINJAPREFERENCES", ":(");
			}
            String v = (String)mMap.get(key);
            return v != null ? v : defValue;
        }
	}

	@Override
	public Set<String> getStringSet(String key, Set<String> defValue) {
		synchronized (this) {
			Set<String> v = (Set<String>)mMap.get(key);
            return v != null ? v : defValue;
        }
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener listener) {
		synchronized(this) {
            mListeners.put(listener, mContent);
        }
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(
			OnSharedPreferenceChangeListener listener) {
		synchronized(this) {
            mListeners.remove(listener);
        }
	}

	/**
     * Enqueue an already-committed-to-memory result to be written
     * to disk.
     *
     * They will be written to disk one-at-a-time in the order
     * that they're enqueued.
     *
     * @param postWriteRunnable if non-null, we're being called
     *   from apply() and this is the runnable to run after
     *   the write proceeds.  if null (from a regular commit()),
     *   then we're allowed to do this disk write on the main
     *   thread (which in addition to reducing allocations and
     *   creating a background thread, this has the advantage that
     *   we catch them in userdebug StrictMode reports to convert
     *   them where possible to apply() ...)
     */
    private void enqueueDiskWrite(final MemoryCommitResult mcr,
                                  final Runnable postWriteRunnable) {
        final Runnable writeToDiskRunnable = new Runnable() {
                public void run() {
                    synchronized (mWritingToDiskLock) {
                        writeToFile(mcr);
                    }
                    synchronized (NinjaPreferences.this) {
                        mDiskWritesInFlight--;
                    }
                    if (postWriteRunnable != null) {
                        postWriteRunnable.run();
                    }
                }
            };

        final boolean isFromSyncCommit = (postWriteRunnable == null);

        // Typical #commit() path with fewer allocations, doing a write on
        // the current thread.
        if (isFromSyncCommit) {
            boolean wasEmpty = false;
            synchronized (NinjaPreferences.this) {
                wasEmpty = mDiskWritesInFlight == 1;
            }
            if (wasEmpty) {
                writeToDiskRunnable.run();
                return;
            }
        }

        QueuedWork.singleThreadExecutor().execute(writeToDiskRunnable);
    }

    private static FileOutputStream createFileOutputStream(File file) {
        FileOutputStream str = null;
        try {
            str = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            File parent = file.getParentFile();
            if (!parent.mkdir()) {
                Log.e(TAG, "Couldn't create directory for SharedPreferences file " + file);
                return null;
            }
            FileUtils.setPermissions(
                parent.getPath(),
                FileUtils.S_IRWXU|FileUtils.S_IRWXG|FileUtils.S_IXOTH,
                -1, -1);
            try {
                str = new FileOutputStream(file);
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "Couldn't create SharedPreferences file " + file, e2);
            }
        }
        return str;
    }

    // Note: must hold mWritingToDiskLock
    private void writeToFile(MemoryCommitResult mcr) {
        // Rename the current file so it may be used as a backup during the next read
        if (mFile.exists()) {
            if (!mcr.changesMade) {
                // If the file already exists, but no changes were
                // made to the underlying map, it's wasteful to
                // re-write the file.  Return as if we wrote it
                // out.
                mcr.setDiskWriteResult(true);
                return;
            }
            if (!mBackupFile.exists()) {
                if (!mFile.renameTo(mBackupFile)) {
                    Log.e(TAG, "Couldn't rename file " + mFile
                            + " to backup file " + mBackupFile);
                    mcr.setDiskWriteResult(false);
                    return;
                }
            } else {
                mFile.delete();
            }
        }

        // Attempt to write the file, delete the backup and return true as atomically as
        // possible.  If any exception occurs, delete the new file; next time we will restore
        // from the backup.
        try {
            FileOutputStream str = createFileOutputStream(mFile);
            if (str == null) {
                mcr.setDiskWriteResult(false);
                return;
            }
            XmlUtils.writeMapXml(mcr.mapToWriteToDisk, str);
            FileUtils.sync(str);
            str.close();
            setFilePermissionsFromMode(mFile.getPath(), mMode, 0);
            try {
            	final StructStat stat = Libcore.os.stat(mFile.getPath());
                synchronized (this) {
                	mStatTimestamp = stat.st_mtime;
                	mStatSize = stat.st_size;
                }
            } catch (ErrnoException e) {
            	// Do nothing
            }
            // Writing was successful, delete the backup file if there is one.
            mBackupFile.delete();
            mcr.setDiskWriteResult(true);
            return;
        } catch (XmlPullParserException e) {
            Log.w(TAG, "writeToFile: Got exception:", e);
        } catch (IOException e) {
            Log.w(TAG, "writeToFile: Got exception:", e);
        }
        // Clean up an unsuccessfully written file
        if (mFile.exists()) {
            if (!mFile.delete()) {
                Log.e(TAG, "Couldn't clean up partially-written file " + mFile);
            }
        }
        mcr.setDiskWriteResult(false);
    }
    
    private static void setFilePermissionsFromMode(String name, int mode,
            int extraPermissions) {
        int perms = FileUtils.S_IRUSR|FileUtils.S_IWUSR
            |FileUtils.S_IRGRP|FileUtils.S_IWGRP
            |extraPermissions;
        FileUtils.setPermissions(name, perms, -1, -1);
    }

	private static class MemoryCommitResult {
        public boolean changesMade;  // any keys different?
        public List<String> keysModified;  // may be null
        public Set<OnSharedPreferenceChangeListener> listeners;  // may be null
        public Map<?, ?> mapToWriteToDisk;
        public final CountDownLatch writtenToDiskLatch = new CountDownLatch(1);
        public volatile boolean writeToDiskResult = false;

        public void setDiskWriteResult(boolean result) {
            writeToDiskResult = result;
            writtenToDiskLatch.countDown();
        }
    }

	public class NinjaEditor implements SharedPreferences.Editor {
		private final Map<String, Object> mModified = new HashMap<String, Object>();
		private boolean mClear = false;

		@Override
		public void apply() {
			final MemoryCommitResult mcr = commitToMemory();
			final Runnable awaitCommit = new Runnable() {
				public void run() {
					try {
						mcr.writtenToDiskLatch.await();
					} catch (InterruptedException ignored) {
					}
				}
			};
			QueuedWork.add(awaitCommit);
			Runnable postWriteRunnable = new Runnable() {
				public void run() {
					awaitCommit.run();
					QueuedWork.remove(awaitCommit);
				}
			};
			NinjaPreferences.this.enqueueDiskWrite(mcr, postWriteRunnable);

			// Okay to notify the listeners before it's hit disk
			// because the listeners should always get the same
			// SharedPreferences instance back, which has the
			// changes reflected in memory.
			notifyListeners(mcr);
		}

		// Returns true if any changes were made
        private MemoryCommitResult commitToMemory() {
        	MemoryCommitResult mcr = new MemoryCommitResult();
            synchronized (NinjaPreferences.this) {
                // We optimistically don't make a deep copy until
                // a memory commit comes in when we're already
                // writing to disk.
                if (mDiskWritesInFlight > 0) {
                    // We can't modify our mMap as a currently
                    // in-flight write owns it.  Clone it before
                    // modifying it.
                    // noinspection unchecked
                    mMap = new HashMap<String, Object>(mMap);
                }
                mcr.mapToWriteToDisk = mMap;
                mDiskWritesInFlight++;

                boolean hasListeners = mListeners.size() > 0;
                if (hasListeners) {
                    mcr.keysModified = new ArrayList<String>();
                    mcr.listeners =
                            new HashSet<OnSharedPreferenceChangeListener>(mListeners.keySet());
                }

                synchronized (this) {
                    if (mClear) {
                        if (!mMap.isEmpty()) {
                            mcr.changesMade = true;
                            mMap.clear();
                        }
                        mClear = false;
                    }

                    for (Map.Entry<String, Object> e : mModified.entrySet()) {
                        String k = e.getKey();
                        Object v = e.getValue();
                        if (v == this) {  // magic value for a removal mutation
                            if (!mMap.containsKey(k)) {
                                continue;
                            }
                            mMap.remove(k);
                        } else {
                            boolean isSame = false;
                            if (mMap.containsKey(k)) {
                                Object existingValue = mMap.get(k);
                                if (existingValue != null && existingValue.equals(v)) {
                                    continue;
                                }
                            }
                            mMap.put(k, v);
                        }

                        mcr.changesMade = true;
                        if (hasListeners) {
                            mcr.keysModified.add(k);
                        }
                    }

                    mModified.clear();
                }
            }
            return mcr;
        }
        
        private void notifyListeners(final MemoryCommitResult mcr) {
            if (mcr.listeners == null || mcr.keysModified == null ||
                mcr.keysModified.size() == 0) {
                return;
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                for (int i = mcr.keysModified.size() - 1; i >= 0; i--) {
                    final String key = mcr.keysModified.get(i);
                    for (OnSharedPreferenceChangeListener listener : mcr.listeners) {
                        if (listener != null) {
                            listener.onSharedPreferenceChanged(NinjaPreferences.this, key);
                        }
                    }
                }
            } else {
                // Run this function on the main thread.
            	Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(new Runnable() {
                    public void run() {
                        notifyListeners(mcr);
                    }
                });
            }
        }

		@Override
		public Editor clear() {
			synchronized (this) {
                mClear = true;
                return this;
            }
		}

		@Override
		public boolean commit() {
			MemoryCommitResult mcr = commitToMemory();
            NinjaPreferences.this.enqueueDiskWrite(
                mcr, null /* sync write on this thread okay */);
            try {
                mcr.writtenToDiskLatch.await();
            } catch (InterruptedException e) {
                return false;
            }
            notifyListeners(mcr);
            return mcr.writeToDiskResult;
		}

		@Override
		public Editor putBoolean(String key, boolean value) {
			synchronized (this) {
                mModified.put(key, value);
                return this;
            }
		}

		@Override
		public Editor putFloat(String key, float value) {
			synchronized (this) {
                mModified.put(key, value);
                return this;
            }
		}

		@Override
		public Editor putInt(String key, int value) {
			synchronized (this) {
                mModified.put(key, value);
                return this;
            }
		}

		@Override
		public Editor putLong(String key, long value) {
			synchronized (this) {
                mModified.put(key, value);
                return this;
            }
		}

		@Override
		public Editor putString(String key, String value) {
			synchronized (this) {
                mModified.put(key, value);
                return this;
            }
		}

		@Override
		public Editor putStringSet(String key, Set<String> value) {
			synchronized (this) {
                mModified.put(key, value);
                return this;
            }
		}

		@Override
		public Editor remove(String key) {
			synchronized (this) {
                mModified.put(key, this);
                return this;
            }
		}
		
	}
}
