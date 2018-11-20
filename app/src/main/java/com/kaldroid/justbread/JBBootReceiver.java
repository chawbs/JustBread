package com.kaldroid.justbread;

/*
 * @package: com.kaldroid.justbread
 * @activity: BreadBootReceiver
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: Broadcast receiver to get wake-up when device first boots
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.kaldroid.justbread.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class JBBootReceiver extends BroadcastReceiver {

	public static final String ourFolder = "/justbread";
	public static final String settingsFile = "settings.txt";
	public static final String cacheFile = "jbcache.xml";
	public static final String timestampFile = "lastTimestamp.txt";
	
	public static int ourTheme = android.R.style.Theme_Black_NoTitleBar;
	public static int refreshInt = 60;
	public static Uri notifyUri = null;
	public static boolean debugMe = false;
	public static int sdkVersion = android.os.Build.VERSION.SDK_INT;
	public static String sdkRelease = android.os.Build.VERSION.RELEASE;
	public static String verseTranslation = "KJV";
	public static String[] bibleTranslations = null;
	public static String[] bibleLookups = null;
	public static boolean stayAwake = true;
	public static String bcUpdateEvent = "com.kaldroid.justbread.event.UPDATE";
	public static String bcSpeechDone = "com.kaldroid.justbread.event.SPEECHDONE";
	public static float scale = 100;
	private static final String TAG = "BCReceiver";

	//private static String bibleStack = "http://bbf.fbcxt.org/translations.php";

	public static void saveSettings() {
		try {
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + ourFolder);
			File sett = new File(dir, settingsFile);
			String eol = System.getProperty("line.separator");

			// save our settings to file
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sett)));
			writer.write(Integer.toString(refreshInt) + eol);
			writer.write(Integer.toString(ourTheme) + eol);
			writer.write(notifyUri.toString() + eol);
			writer.write(verseTranslation + eol);
			writer.write(Boolean.toString(stayAwake) + eol);
			writer.write(Float.toString(scale) + eol);
			writer.close();
		}
		catch (Exception ex) {
			Log.e(TAG, "Exception Saving:" + ex.getMessage());
		}
	}
	
	public static void restoreSettings(Context context) {
		// first try get bible translations available
		getBibleTranslations(context);
		try {
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + ourFolder);
			File sett = new File(dir, settingsFile);

			// zap last fetch time to force get from internet
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(sett)));
			String line = input.readLine();
			refreshInt = Integer.parseInt(line);
			line = input.readLine();
			ourTheme = Integer.parseInt(line);
			line = input.readLine();
			notifyUri = Uri.parse(line);
			line = input.readLine();
			verseTranslation = line;
			line = input.readLine();
			stayAwake = Boolean.parseBoolean(line);
			line = input.readLine();
			scale = Float.parseFloat(line);
			input.close();
		}
		catch (Exception ex) {
			Log.e(TAG, "Exception Restoring:" + ex.getMessage());
			refreshInt = 60;
			ourTheme = android.R.style.Theme_Light_NoTitleBar;
			notifyUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			verseTranslation = "KJV";
			stayAwake = true;
			scale = (float) 100.0;
			// now save it to write a good file
			saveSettings();
		}
		
	}
	
	public static void getBibleTranslations(Context context) {
		Resources res = context.getResources();
		bibleTranslations = res.getStringArray(R.array.bibleTranslations);
		bibleLookups = res.getStringArray(R.array.bibleLookups);
	}
	
	@Override
	public void onReceive(Context context, Intent intentIn) {
		// now set the timer for the next wake-up and check...
		restoreSettings(context);
		
		// schedule first lookup after 1 minute for quicker wakeup
		Intent intent = new Intent(context, JBAlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 001000, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (refreshInt > 0)
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1 * (60 * 1000), refreshInt * 60 * 1000, pendingIntent);
	
		
	}

}
