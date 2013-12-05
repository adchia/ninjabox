package com.example.sample_ninjabox;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
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
public class MainActivity extends Activity {
    
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
        } else {
            // disable ninja mode
        }
    }

}
