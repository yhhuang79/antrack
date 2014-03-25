package tw.plash.antrack;

import java.util.ArrayList;
import java.util.List;

import tw.plash.antrack.util.Installation;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;

public class Tutorial extends Activity {
	
	private SharedPreferences pref;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		pref = PreferenceManager.getDefaultSharedPreferences(Tutorial.this);
		if (pref.getBoolean("firsttime", true)) {
			setScreenContent(R.layout.setup);
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
		previous.setVisibility(View.GONE);
		
		
		final Spinner gmail = (Spinner) findViewById(R.id.nameinput);
		final CheckBox cb = (CheckBox) findViewById(R.id.legal);
		cb.setText(R.string.legal_stuff);
		cb.setMovementMethod(LinkMovementMethod.getInstance());
		final Button next = (Button) findViewById(R.id.next);
		next.setEnabled(false);
		
		List<String> list = new ArrayList<String>();
		Account[] accounts = AccountManager.get(this).getAccounts();
		if (accounts == null) {
			// accounts array shouldn't be null
			Log.i("TAG", "Some error is here! accounts array shouldn't be null");
		} else {
			for (Account account : accounts) {
				Log.i("TAG", "account.name = " + account.name + ", "
						+ "account.type = " + account.type);
				String accountName = account.name;
				String accountType = account.type;

				if (accountType.equals("com.google")
						|| (accountType.equals("com.android.email") && accountName
								.substring(accountName.indexOf("@") + 1)
								.equals("gmail.com"))) {
					//emailString = accountName;
					list.add(accountName);
				}
			}
			if (list.size() > 0) {
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
						android.R.layout.simple_spinner_item, list);
					dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					gmail.setAdapter(dataAdapter);
			}
		}

		
	
		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked && (String.valueOf(gmail.getSelectedItem()).length() > 0)){
					next.setEnabled(true);
				} else{
					next.setEnabled(false);
				}
			}
		});
		
		next.setText(R.string.tutor_done);
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = String.valueOf(gmail.getSelectedItem());
				Toast.makeText(Tutorial.this, "Hi! " + name, Toast.LENGTH_SHORT).show();
				saveNameToSharedPreference(name);
				saveScreenDpiToSharedPreference();
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
	
	private void saveScreenDpiToSharedPreference(){
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int dpi = metrics.densityDpi;
		pref.edit().putInt("dpi", dpi).commit();
	}
	
	private void finishThisAndGoToMapActivity(){
		startActivity(new Intent(Tutorial.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT));
		finish();
	}
}