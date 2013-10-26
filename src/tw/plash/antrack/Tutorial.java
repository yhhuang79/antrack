package tw.plash.antrack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

public class Tutorial extends Activity {
	
	private SharedPreferences pref;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		pref = PreferenceManager.getDefaultSharedPreferences(Tutorial.this);
		if (pref.getBoolean("firsttime", true)) {
			setScreenContent(R.layout.tutorialstep1);
		} else {
			finishThisAndGoToMapActivity();
		}
	}
	
	private void setScreenContent(int layoutID) {
		setContentView(layoutID);
		switch (layoutID) {
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
		case R.layout.tutorialstep5:
			setTutorialStep5PageContent();
			break;
		case R.layout.tutorialstep6:
			setTutorialStep6PageContent();
			break;
		case R.layout.setup:
			setSetupPageContent();
			break;
		}
	}
	
	private void setTutorialStep1PageContent() {
		Button previous = (Button) findViewById(R.id.previous);
		previous.setVisibility(View.GONE);
		
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
				setScreenContent(R.layout.tutorialstep5);
			}
		});
	}
	
	private void setTutorialStep5PageContent() {
		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep4);
			}
		});
		
		Button next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep6);
			}
		});
	}
	
	private void setTutorialStep6PageContent() {
		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setScreenContent(R.layout.tutorialstep5);
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
				setScreenContent(R.layout.tutorialstep6);
			}
		});
		
		final EditText et = (EditText) findViewById(R.id.nameinput);
		final CheckBox cb = (CheckBox) findViewById(R.id.legal);
		cb.setText(R.string.legal_stuff);
		cb.setMovementMethod(LinkMovementMethod.getInstance());
		final Button next = (Button) findViewById(R.id.next);
		next.setEnabled(false);
		
		et.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if(s.length() > 0 && cb.isChecked()){
					next.setEnabled(true);
				} else{
					next.setEnabled(false);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		
		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked && (et.getEditableText().length() > 0)){
					next.setEnabled(true);
				} else{
					next.setEnabled(false);
				}
			}
		});
		
		next.setText("Done!");
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = et.getEditableText().toString();
				Toast.makeText(Tutorial.this, "Hi! " + name, Toast.LENGTH_SHORT).show();
				saveNameToSharedPreference(name);
				generateUniqueId();
				finishThisAndGoToMapActivity();
			}
		});
	}
	
	private void saveNameToSharedPreference(String name){
		pref.edit().putBoolean("firsttime", false).putString("name", name).commit();
	}
	
	private void generateUniqueId(){
		String uuid = Installation.id(Tutorial.this);
		pref.edit().putString("uuid", uuid).commit();
	}
	
	private void finishThisAndGoToMapActivity(){
		startActivity(new Intent(Tutorial.this, AntrackMapActivity.class).setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT));
		finish();
	}
}