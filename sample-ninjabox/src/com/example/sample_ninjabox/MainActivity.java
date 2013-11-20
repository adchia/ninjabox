package com.example.sample_ninjabox;

import com.example.sample_ninjabox.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = false;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    
    /**
     * Shared preferences name.
     */
    public static final String POKEMON_PREFS = "POKEMON_PREFS";
    public static final String POKEMON_STARTER = "POKEMON_STARTER";
    private static final String BULBASAUR = "Bulbasaur";
    private static final String CHARMANDER = "Charmander";
    private static final String SQUIRTLE = "Squirtle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider.hide();

        final ImageView bulbasaur = (ImageView) findViewById(R.id.bulbasaur);
        final ImageView charmander = (ImageView) findViewById(R.id.charmander);
        final ImageView squirtle = (ImageView) findViewById(R.id.squirtle);
        final TextView starter = (TextView) findViewById(R.id.starter);
        
        SharedPreferences settings = getSharedPreferences(POKEMON_PREFS, MODE_PRIVATE);
        starter.setText(settings.getString(POKEMON_STARTER, "None!"));
        if (starter.getText().equals(BULBASAUR)) {
        	bulbasaur.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        } else if (starter.getText().equals(SQUIRTLE)) {
        	squirtle.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        } else if (starter.getText().equals(CHARMANDER)) {
        	charmander.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        }
        
        bulbasaur.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SharedPreferences settings = getSharedPreferences(POKEMON_PREFS, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
		        
				editor.putString(POKEMON_STARTER, BULBASAUR);
				starter.setText(BULBASAUR);
				bulbasaur.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY );
				squirtle.clearColorFilter();
				charmander.clearColorFilter();
		        // Commit the edits!
		        editor.commit();
			}
		});
        
        squirtle.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SharedPreferences settings = getSharedPreferences(POKEMON_PREFS, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
		        
				editor.putString(POKEMON_STARTER, SQUIRTLE);
				starter.setText(SQUIRTLE);
				squirtle.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY );
				bulbasaur.clearColorFilter();
				charmander.clearColorFilter();
		        // Commit the edits!
		        editor.commit();
			}
		});

		charmander.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SharedPreferences settings = getSharedPreferences(POKEMON_PREFS, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
		        
				editor.putString(POKEMON_STARTER, CHARMANDER);
				starter.setText(CHARMANDER);
				charmander.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY );
				bulbasaur.clearColorFilter();
				squirtle.clearColorFilter();
		        // Commit the edits!
		        editor.commit();
			}
		});
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
