package com.example.sample_ninjabox;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.sample_ninjabox.util.SystemUiHider;

import edu.mit.ninjabox.NinjaActivity;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends NinjaActivity {
    
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
        NinjaActivity.initialize(getWindow());
        setContentView(R.layout.activity_main);

        ToggleButton ninjaButton = (ToggleButton) findViewById(R.id.start_ninja_mode);
        ninjaButton.getBackground().setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
        
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

    public void onNinjaModeClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();
        
        if (on) {
            // enable ninja mode
        	startNinjaMode();
        } else {
            // disable ninja mode
        	stopNinjaMode();
        }
        
        PackageManager pm = getPackageManager();
        ComponentName cn1 = new ComponentName("com.example.sample_ninjabox", "com.example.sample_ninjabox.LoginAlias");
        ComponentName cn2 = new ComponentName("com.example.sample_ninjabox", "com.example.sample_ninjabox.LoginAlias-copy");
        int dis = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        
        if(pm.getComponentEnabledSetting(cn1) == dis) dis = 3 - dis;
        pm.setComponentEnabledSetting(cn1, dis, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(cn2, 3 - dis, PackageManager.DONT_KILL_APP);
        
        pm.clearPackagePreferredActivities(getPackageName());
        
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
 
		alertDialogBuilder
			.setTitle("Ninja Mode")
			.setMessage("NinjaMode has been toggled! Please press home button and select appropriate launcher.")
			.setCancelable(false)
			.setPositiveButton("OK",new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,int id) {
					dialog.cancel();
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
    }

}
