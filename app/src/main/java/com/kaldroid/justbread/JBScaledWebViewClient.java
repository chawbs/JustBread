package com.kaldroid.justbread;

import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class JBScaledWebViewClient extends WebViewClient {

	@Override
	public void onScaleChanged(WebView view, float oldScale, float newScale) {
		super.onScaleChanged(view, oldScale, newScale);
		JBBootReceiver.scale = (float) (100.0 * newScale);
		Log.v("JB","Scaled from "+Float.toString(oldScale)+" to "+Float.toString(newScale));
	}
}
