package com.kaldroid.justbread;

/*
 * @package: com.kaldroid.justbread
 * @activity: AlarmReceiver
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: Broadcast receiver to get alarm wake-ups
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Messenger;

///
/// The receiver to do an auto-update check
///
public class JBAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service = new Intent(context, JBCheckService.class);
		Messenger messenger = null;
		service.putExtra("MESSENGER", messenger);
		context.startService(service);
	}
	
}
