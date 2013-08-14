package tw.plash.antrack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Tutorial extends Activity {
	
	private SharedPreferences pref;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		pref = PreferenceManager.getDefaultSharedPreferences(Tutorial.this);
		if (pref.getBoolean("firsttime", true)) {
			setScreenContent(R.layout.intro);
		} else {
			finishThisAndGoToMapActivity();
		}
	}
	
	private void setScreenContent(int layoutID) {
		setContentView(layoutID);
		switch (layoutID) {
		case R.layout.intro:
			setIntroPageContent();
			break;
		case R.layout.tutorialstep1:
			setTutorialStep1PageContent();
			break;
		case R.layout.tutorialstep2:
			setTutorialStep2PageContent();
			break;
		case R.layout.tutorialstep3:
			setTutorialStep3PageContent();
			break;
		case R.layout.tutorialstep4:
			setTutorialStep4PageContent();
			break;
		case R.layout.setup:
			setSetupPageContent();
			break;
		}
	}
	
	private void setIntroPageContent() {
		Button previous = (Button) findViewById(R.id.previous);
		previous.setVisibility(View.GONE);
		
		Button next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep1);
			}
		});
		
		final TextView tv = (TextView) findViewById(R.id.yo);
		tv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String text = tv.getText().toString();
				if(text.contains("INTRO")){
					ServerConnectionTest.initialize(Tutorial.this);
					tv.setText("init...");
				} else if(text.contains("init")){
					ServerConnectionTest.upload(Tutorial.this);
					tv.setText("upload...");
				} else if(text.contains("upload...")){
					ServerConnectionTest.stop(Tutorial.this);
					tv.setText("stop...");
				} else if(text.contains("stop...")){
					ServerConnectionTest.upload(Tutorial.this);
					tv.setText("sneaky upload...");
				}
			}
		});
	}
	
	private void setTutorialStep1PageContent() {
		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.intro);
			}
		});
		
		Button next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep2);
			}
		});
	}
	
	private void setTutorialStep2PageContent() {
		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep1);
			}
		});
		
		Button next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep3);
			}
		});
	}
	
	private void setTutorialStep3PageContent() {
		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep2);
			}
		});
		
		Button next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep4);
			}
		});
	}
	
	private void setTutorialStep4PageContent() {
		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep3);
			}
		});
		
		Button next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.setup);
			}
		});
	}
	
	private void setSetupPageContent() {
		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep4);
			}
		});
		
		final EditText et = (EditText) findViewById(R.id.nameinput);
		
		Button next = (Button) findViewById(R.id.next);
		next.setText("Done!");
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = et.getEditableText().toString();
				if (name.isEmpty()) {
					et.setError("please enter a name");
				} else {
					Toast.makeText(Tutorial.this, "Hi! " + name, Toast.LENGTH_SHORT).show();
					saveNameToSharedPreference(name);
					generateUserid();
					finishThisAndGoToMapActivity();
				}
			}
		});
	}
	
	private void saveNameToSharedPreference(String name){
		pref.edit().putBoolean("firsttime", false).putString("name", name).commit();
	}
	
	private void generateUserid(){
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getDeviceId();
		String prehash = imei + String.valueOf(System.currentTimeMillis());
		String userid = Utility.getMD5(prehash);
		pref.edit().putString("userid", userid).commit();
		Toast.makeText(Tutorial.this, "userid=" + userid, Toast.LENGTH_LONG).show();
	}
	
	private void finishThisAndGoToMapActivity(){
		startActivity(new Intent(Tutorial.this, Map.class));
		finish();
	}
}