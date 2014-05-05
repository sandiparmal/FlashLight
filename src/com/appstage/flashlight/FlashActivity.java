package com.appstage.flashlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;



public class FlashActivity extends Activity {

	
	private Camera camera;
	private boolean isFlashOn;
	private boolean hasFlash;
	Parameters params;
	MediaPlayer mp;
	private static int counter = 0;

	private SeekBar seekbar;
	
	private int brightness;
    //Content resolver used as a handle to the system's settings
    private ContentResolver cResolver;
    //Window object, that will store a reference to the current window
    private Window window;

	private TextView tvPercentage;

	private int isPause = 0;

	private TextView tvOnOff;
	private ImageView btnSwitch;
	private ImageView imvSoundOff;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/** keep screen awake */
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		/**
		 *  Initialize variable 
		 */
		initializeVariable();
		
		//Get the content resolver
        cResolver = getContentResolver();
 
        //Get the current window
        window = getWindow();
 
        //Set the seekbar range between 0 and 255
        seekbar.setMax(255);
        //Set the seek bar progress to 1
        seekbar.setKeyProgressIncrement(1);
        
        try
        {
            //Get the current system brightness
            brightness = android.provider.Settings.System.getInt(cResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS);
        }
        catch (SettingNotFoundException e)
        {
            //Throw an error case it couldn't be retrieved
            Log.e("Error", "Cannot access system brightness");
            e.printStackTrace();
        }
 
        //Set the progress of the seek bar based on the system's brightness
        seekbar.setProgress(brightness);
        float perc = (brightness /(float)255)*100;
        //Set the brightness percentage 
        tvPercentage.setText((int)perc +" %");
 
        imvSoundOff.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View arg0) {
				counter++;
				if(counter%2==0){
					imvSoundOff.setImageResource(R.drawable.sound);
				}else{
					imvSoundOff.setImageResource(R.drawable.sound_off);
				}	
			}
		});
		
		/**
		 *  seek bar change listener
		 */
		seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				android.provider.Settings.System.putInt(cResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness);
                //Get the current window attributes
                LayoutParams layoutpars = window.getAttributes();
                //Set the brightness of this window
                layoutpars.screenBrightness = brightness / (float)255;
                //Apply attribute changes to this window
                window.setAttributes(layoutpars);
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekbar, int progress, boolean arg2) {
				
				if(progress<=20)
                {
                    //Set the brightness to 20
                    brightness=20;
                }
                else //brightness is greater than 20
                {
                    //Set brightness variable based on the progress bar
                    brightness = progress;
                    
                }
				float perc = (brightness /(float)255)*100;
                //Set the brightness percentage 
                tvPercentage.setText((int)perc +" %");
			}
		});

		/*
		 * First check if device is supporting flashlight or not
		 */
		hasFlash = getApplicationContext().getPackageManager()
				.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

		if (!hasFlash) {
			// device doesn't support flash
			// Show alert message and close the application
			AlertDialog alert = new AlertDialog.Builder(FlashActivity.this)
					.create();
			alert.setTitle("Error");
			alert.setMessage("Sorry, your device doesn't support flash light!");
			alert.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					// closing the application
					finish();
				}
			});
			alert.show();
			return;
		}

		// get the camera
		
		
		// displaying button image
		toggleButtonImage();
		
		/*
		 * Switch button click event to toggle flash on/off
		 */
		btnSwitch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				getCamera();
				if (isFlashOn) {
					// turn off flash
					turnOffFlash();
				} else {
					// turn on flash
					turnOnFlash();
				}
			}
		});
	}

	private void initializeVariable() {
		// TODO Auto-generated method stub
		btnSwitch = (ImageView) findViewById(R.id.btnFlash);
		imvSoundOff = (ImageView)findViewById(R.id.imvsoundOnOff);
		seekbar = (SeekBar)findViewById(R.id.seekBar1);
		tvPercentage = (TextView)findViewById(R.id.tvPercentage);
		tvOnOff = (TextView)findViewById(R.id.tvOnOff);
		
	}

	/*
	 * Get the camera
	 */
	private void getCamera() {
		if (camera == null) {
			try {
				camera = Camera.open();
				params = camera.getParameters();
			} catch (RuntimeException e) {
				Log.e("Camera Error. Failed to Open. Error: ", e.getMessage());
			}
		}
	}

	/*
	 * Turning On flash
	 */
	private void turnOnFlash() {
		if (!isFlashOn) {
			if (camera == null || params == null) {
				return;
			}
			// play sound
			playSound();
			
			params = camera.getParameters();
			params.setFlashMode(Parameters.FLASH_MODE_TORCH);
			camera.setParameters(params);
			camera.startPreview();
			isFlashOn = true;
			
			// changing button/switch image
			toggleButtonImage();
		}

	}

	/*
	 * Turning Off flash
	 */
	private void turnOffFlash() {
		if (isFlashOn) {
			if (camera == null || params == null) {
				return;
			}
			// play sound
			playSound();
			
			params = camera.getParameters();
			params.setFlashMode(Parameters.FLASH_MODE_OFF);
			camera.setParameters(params);
			camera.stopPreview();
			isFlashOn = false;
			
			// changing button/switch image
			toggleButtonImage();
		}
	}
	
	/*
	 * Playing sound
	 * will play button toggle sound on flash on / off
	 * */
	private void playSound(){
		if(counter%2==0){
		if(isFlashOn){
			mp = MediaPlayer.create(FlashActivity.this, R.raw.light_switch_off);
		}else{
			mp = MediaPlayer.create(FlashActivity.this, R.raw.light_switch_on);
		}
		mp.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                mp.release();
            }
        }); 
		mp.start();
		}
	}
	
	/*
	 * Toggle switch button images
	 * changing image states to on / off
	 * */
	private void toggleButtonImage(){
		if(isFlashOn){
			btnSwitch.setImageResource(R.drawable.off);
			tvOnOff.setText("Turn OFF");
		}else{
			btnSwitch.setImageResource(R.drawable.on);
			tvOnOff.setText("Turn ON");
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		isPause  = 1;
		turnOffFlash();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(isPause==1){
			turnOnFlash();
			isPause = 0;
		}
		
	}

	@Override
	protected void onStart() {
		super.onStart();
		if(isPause==1){
			turnOnFlash();
			isPause = 0;
		}
		
	}

	@Override
	protected void onStop() {
		super.onStop();
		
		if (camera != null) {
			camera.release();
			camera = null;
		}
	}

}
