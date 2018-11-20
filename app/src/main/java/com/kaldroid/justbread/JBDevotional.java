package com.kaldroid.justbread;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.kaldroid.justbread.R;

import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.widget.Toast;

public class JBDevotional extends Activity {
	
	private static PowerManager pm;
	private WebView wv = null;
	private RSSFeed feed = null;
	private RSSItem newest = null;
	private Menu myMenu;
	private BroadcastReceiver bcUpdateEvent = null;
	private BroadcastReceiver bcSpeechDone = null;
	private WakeLock mWakeLock;
	private int scale = 0;
	private String myPlainBread;
	private String emptyBread = "<rss version=\"0.91\">\n" +
			"<channel>\n" +
			"<title>FBC XT Blog (cache)</title><link>http://justbread.org/devotionals/</link>\n" +
			"<description>Just Bread Devotional</description>\n<language>en</language>\n" +
			"<item>\n" +
			"<title>Today</title>\n" +
			"<description>" +
			"&lt;strong&gt;Today&lt;/strong&gt;: John 3:16&lt;br /&gt; &lt;br /&gt;&lt;br /&gt; &lt;strong&gt;Key Verse&lt;/strong&gt;: John 3:16&lt;br /&gt; 16  For God so loved the world that He gave His only Son so that whosoever believes in Him should have eternal life.&lt;br /&gt; &lt;br /&gt;&lt;br /&gt; &lt;strong&gt;Devotion&lt;/strong&gt;:&lt;br /&gt; This is a place-holder until the real data is fetched" +
			"</description>\n" +
			"</item>\n" +
			"</channel>\n" +
			"</rss>";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		JBBootReceiver.restoreSettings(this);
    	pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	if (JBBootReceiver.sdkVersion < 15) {
			setTheme(JBBootReceiver.ourTheme);
		} else {
			if (JBBootReceiver.ourTheme == android.R.style.Theme_Black)
				setTheme(0x01030128);
			else
				setTheme(0x0103012b);
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_devotional);
		wv = (WebView) findViewById(R.id.webView1);
        wv.setWebViewClient(new JBScaledWebViewClient());
        wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setBuiltInZoomControls(true);
		wv.getSettings().setSupportZoom(true);
		wv.getSettings().setDefaultZoom(ZoomDensity.FAR);
        scale = (int)JBBootReceiver.scale;
        wv.setInitialScale(scale);
        wv.setBackgroundColor(0);
        
        if (JBBootReceiver.stayAwake) {
        	getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        }
        
        bcUpdateEvent = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// we have received something
				String action = intent.getAction();
		        if (action.equals(JBBootReceiver.bcUpdateEvent)) {
		            // do a local screen update
	        		Bundle b = intent.getExtras();
	        		Boolean doit = b.getBoolean("update");
	        		if (doit)
	        			refreshBread();
		        }
			}
        };
        bcSpeechDone = new BroadcastReceiver() {
        	@Override
        	public void onReceive(Context context, Intent intent) {
        		String action = intent.getAction();
        		if(action.equals(JBBootReceiver.bcSpeechDone)) {
        			updateIcon(false);
        		}
        	}
        };
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_devotional, menu);
		return true;
	}

	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// goPrevious, goVerse, goNext, goShareText, goShareLink, goRefresh, goSettings, goAbout, goHelp
    	myMenu = menu;
    	MenuItem mi = (MenuItem) menu.findItem(R.id.goShareText);
//    	if (JBBootReceiver.sdkVersion < 15) {
//    		mi.setIcon(R.drawable.sharetxt);
//    	}
//    	else {
    		if (JBBootReceiver.ourTheme == android.R.style.Theme_Black) {
    			mi.setIcon(R.drawable.ics_share_txt_dark);
    		} else {
    			mi.setIcon(R.drawable.ics_share_txt);
    		}
//    	}
    	mi = (MenuItem) menu.findItem(R.id.goShareLink);
//    	if (JBBootReceiver.sdkVersion < 15) {
//    		mi.setIcon(R.drawable.sharelnk);
//    	}
//    	else {
    		if (JBBootReceiver.ourTheme == android.R.style.Theme_Black) {
    			mi.setIcon(R.drawable.ics_share_lnk_dark);
    		} else {
    			mi.setIcon(R.drawable.ics_share_lnk);
    		}
//    	}
    	mi = (MenuItem) menu.findItem(R.id.goRefresh);
//    	if (JBBootReceiver.sdkVersion < 15) {
//    		mi.setIcon(R.drawable.refresh);
//    	}
//    	else {
    		if (JBBootReceiver.ourTheme == android.R.style.Theme_Black) {
    			mi.setIcon(R.drawable.ics_refresh_dark);
    		} else {
    			mi.setIcon(R.drawable.ics_refresh);
    		}
//    	}
    	mi = (MenuItem) menu.findItem(R.id.goSettings);
//    	if (JBBootReceiver.sdkVersion < 15) {
//    		mi.setIcon(R.drawable.settings);
//    	}
//    	else {
    		if (JBBootReceiver.ourTheme == android.R.style.Theme_Black) {
    			mi.setIcon(R.drawable.ics_settings_dark);
    		} else {
    			mi.setIcon(R.drawable.ics_settings);
    		}
//    	}
    	mi = (MenuItem) menu.findItem(R.id.goSpeakDevotion);
//    	if (JBBootReceiver.sdkVersion < 15) {
//    		mi.setIcon(R.drawable.play);
//    	}
//    	else {
    		if (JBBootReceiver.ourTheme == android.R.style.Theme_Black) {
    			mi.setIcon(R.drawable.ics_play_dark);
    		} else {
    			mi.setIcon(R.drawable.ics_play);
    		}
//    	}
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onPause() {
       	Intent speaker = new Intent(getBaseContext(), JBSpeakService.class);
       	getBaseContext().stopService(speaker);
    	if(bcUpdateEvent != null) unregisterReceiver(bcUpdateEvent);
        if(bcSpeechDone != null) unregisterReceiver(bcSpeechDone);
    	if(pm != null) {
    		if(mWakeLock != null) {
    			mWakeLock.release();
    		}
    	}
    	// save settings - just in case user changed font size
    	JBBootReceiver.saveSettings();
    	// now pause
    	super.onPause();
    }
    
    @Override
    public void onResume() {
        if(bcUpdateEvent != null) registerReceiver(bcUpdateEvent, new IntentFilter(JBBootReceiver.bcUpdateEvent));
        if(bcSpeechDone != null) registerReceiver(bcSpeechDone, new IntentFilter(JBBootReceiver.bcSpeechDone));
    	if(pm != null) {
    		if(mWakeLock != null) {
    			mWakeLock.acquire();
    		}
    	}
    	super.onResume();
    }
    
    public void updateIcon(Boolean stop) {
    	if (myMenu != null) {
    		MenuItem mi = (MenuItem) myMenu.findItem(R.id.goSpeakDevotion);
    		if (stop) {
//    			if (JBBootReceiver.sdkVersion < 15) {
//    				mi.setIcon(R.drawable.stop);
//    			}
//    			else {
    				if (JBBootReceiver.ourTheme == android.R.style.Theme_Black) {
    					mi.setIcon(R.drawable.ics_stop_dark);
    				} else {
    					mi.setIcon(R.drawable.ics_stop);
    				}
//    			}

    		}
    		else {
//    			if (JBBootReceiver.sdkVersion < 15) {
//    				mi.setIcon(R.drawable.play);
//    			}
//    			else {
    				if (JBBootReceiver.ourTheme == android.R.style.Theme_Black) {
    					mi.setIcon(R.drawable.ics_play_dark);
    				} else {
    					mi.setIcon(R.drawable.ics_play);
    				}
//    			}

    		}
    	}
    }
    
    // basic menu handling
    @Override
	public boolean onOptionsItemSelected(MenuItem itm) {
    	switch(itm.getItemId()) {
    	case R.id.goRefresh:
    		Toast.makeText(this, (String)getBaseContext().getText(R.string.reget), Toast.LENGTH_SHORT).show();

    		// zap the time and schedule a re-get
	        try {
	        	File sdCard = Environment.getExternalStorageDirectory();
	        	File dir = new File (sdCard.getAbsolutePath() + JBBootReceiver.ourFolder);
	        	File last = new File(dir, JBBootReceiver.timestampFile);
	        	String eol = System.getProperty("line.separator");

	        	// zap last fetch time to force get from internet
	        	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(last)));
	        	writer.write("0" + eol + "0" + eol);
	        	writer.close();
	        }
	        catch (Exception e1) {
	        	Log.e("JB","Exception:"+e1.toString());
	        }
	        // do the update in the background else ICS complains
	        Intent service = new Intent(getBaseContext(), JBCheckService.class);
			getBaseContext().startService(service);
			
			// go home because the refresh will notify us
			finish();
			
    		break;
    	
    	case R.id.goSettings:
    		Intent mySettings = new Intent(this, JBSettings.class);
    		this.startActivityForResult(mySettings, 0);
    		break;
    		
    	case R.id.goShareText:
    		if(feed!=null) {
    			RSSItem sharethis = feed.getItem(0);
    			String bread = sharethis.getDescription();
    			if (TextUtils.isEmpty(bread))
    				bread = sharethis.getContent();
    			bread = bread.replaceAll("<[/]*div>", "");
    			bread = Html.fromHtml(bread).toString();
    			Intent sharingIntent = new Intent(Intent.ACTION_SEND);
    			sharingIntent.setType("text/plain");
    			sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, bread);
    			sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Just Bread");
    			startActivity(Intent.createChooser(sharingIntent, "Share using..."));
    		}
    		break;

    	case R.id.goShareLink:
    		if(feed!=null) {
    			RSSItem sharelink = feed.getItem(0);
    			String breadlink = "I wanted to share this with you: " + sharelink.getLink();
    			try {
    				// we just do this to validate the link is a valid url
    				URL url = new URL(sharelink.getLink());
    				Log.i("JB","Share link: "+url.toString());
    			} catch (MalformedURLException e) {
    				breadlink = "Sorry - No Links exist yet, please refresh the devotions...";
    				Log.e("JB","Exception:"+e.toString()+">"+sharelink.getLink());
    			}
    			Intent sharingLinkIntent = new Intent(Intent.ACTION_SEND);
    			sharingLinkIntent.setType("text/plain");
    			sharingLinkIntent.putExtra(android.content.Intent.EXTRA_TEXT, breadlink);
    			sharingLinkIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Just Bread");
    			startActivity(Intent.createChooser(sharingLinkIntent, "Share using..."));
    		}
    		break;

    	case R.id.goSpeakDevotion:
    		String words = stripHtml(myPlainBread);
    		speakWords(words);
            break;
    	}
		return true;
	}

    private String stripHtml(String speech) {
    	return Html.fromHtml(speech.replaceAll("s/<(.*?, replacement)>//g","")).toString();
    }
    
    private void speakWords(String speech) {
        Intent speaker = new Intent(getBaseContext(), JBSpeakService.class);
		speaker.putExtra("SPEAKTHIS", speech);
		if (getBaseContext().stopService(speaker) == false) {
			getBaseContext().startService(speaker);
			updateIcon(true);
		}
		else {
			updateIcon(false);
		}
	}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == RESULT_OK) {
    		// we need to restart but only when settings comes back - just in case theme changed
    		//getBaseContext().stopService(new Intent(this, BreadBootReceiver.class));
    		Activity aclone;
			try {
				aclone = (Activity) this.clone();
	    		this.finish();
	    		startActivity(new Intent(aclone, JBDevotional.class));
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // do the update in the background or ICS complains
		Intent service = new Intent(getBaseContext(), JBCheckService.class);
		getBaseContext().startService(service);
        
		// in the mean time, get and display the cached data
        File jbc = null;
        try {
        	File sdCard = Environment.getExternalStorageDirectory();
        	File dir = new File (sdCard.getAbsolutePath() + JBBootReceiver.ourFolder);
        	if (!dir.exists()) {
        		if (!dir.mkdir()) {
        			// cannot make directory???
        			Log.e("JB","Cannot make folder:"+dir.getAbsolutePath());
        		}
        	}
        	jbc = new File(dir, JBBootReceiver.cacheFile);
        }
        catch (Exception e1) {
        	jbc = new File(JBBootReceiver.cacheFile);
        	Log.e("JB","Exception:"+e1.toString());
        }
        // save temporary bbf - this is a new cache
        if (!checkCacheExists(jbc))
        	updateCache(jbc, emptyBread);
        refreshBread();
        
		// now set the timer for the next wake-up and check...
        // we do this 1 minute after initial load to make sure things settle down before we go for longer triggers
		Intent intent = new Intent(getBaseContext(), JBAlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 001000, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if(JBBootReceiver.refreshInt > 0)
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1 * (60 * 1000), JBBootReceiver.refreshInt * 60 * 1000, pendingIntent);
    }
    
    private boolean checkCacheExists(File jbc) {
    	// check if cache file exists
		try {
			String eol = System.getProperty("line.separator");
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(jbc)));
			String line;
			StringBuffer buffer = new StringBuffer();
			while ((line = input.readLine()) != null) {
				buffer.append(line + eol);
			}
			input.close();
			return true;

		} catch (Exception e) {
			Log.e("JB","Exception:"+e.toString());
			return false;
		}
    }
    
    private void updateCache(File jbc, String bread) {
    	try {
    		String eol = System.getProperty("line.separator");
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(jbc)));
			if (feed == null)
				writer.write(bread + eol);
			else
				writer.write(feed.toString() + eol);
			writer.close();
		} catch (Exception e) {
			Log.e("JB","Exception:"+e.toString());
			e.printStackTrace();
		}
    }
    
    // Just get data from cached file and display it - much faster
    // Let someone else update the file :-)
    public void refreshBread() {
    	// refresh the data from the cache file
    	Log.i("JB","Refresh Bread called");
        File jbc = null;
        try {
        	File sdCard = Environment.getExternalStorageDirectory();
        	File dir = new File (sdCard.getAbsolutePath() + JBBootReceiver.ourFolder);
        	jbc = new File(dir, JBBootReceiver.cacheFile);
        }
        catch (Exception e1) {
        	jbc = new File(JBBootReceiver.cacheFile);
        	Log.e("JB","Exception:"+e1.toString());
        }
        feed = getFeedFromFile(jbc);
        String bread = null;
        String htmlbegin = "<div style=\"color:#";
        if (JBBootReceiver.ourTheme == android.R.style.Theme_Black)
        	htmlbegin += "FFFFFF ;  \">";
        else
        	htmlbegin += "000000 ;  \">";
        String htmlend = " </div>";
        // get the latest bread by faith
        if ((feed == null) || (feed.equals(null))) {
        	bread = htmlbegin + "<H2>" + (String)getBaseContext().getText(R.string.cannot_fetch) + "</H2>" + htmlend;
        	wv.loadDataWithBaseURL("", bread, "text/html", "UTF-8", "");
        }
        else {
        	try {
        		newest = feed.getItem(0);
        		bread = newest.getDescription();
        		if (TextUtils.isEmpty(bread))
        			bread = newest.getContent();
        		bread = bread.replaceAll("<[/]*div>", "");
        		bread = htmlbegin + bread + htmlend;
        		myPlainBread = stripHtml(bread);
        	}
        	catch (Exception ex) {
        		Log.e("JB","Exception:"+ex.toString());
        		bread = "<H2>"+(String)getBaseContext().getText(R.string.cannot_fetch)+"</H2>";
        	}
        	try {
        		wv.loadDataWithBaseURL("", bread, "text/html", "UTF-8", "");
        	}
        	catch (Exception ee) {
        		Log.e("JB","Exception:"+ee.toString());
        	}
        }
    }
    

    // RSS stuff
    private RSSFeed getFeedFromFile(File bbfc)
    {
       	try
       	{
       		// create the factory
       		SAXParserFactory factory = SAXParserFactory.newInstance();
       		// create a parser
       		SAXParser parser = factory.newSAXParser();

       		// create the reader (scanner)
       		XMLReader xmlreader = parser.getXMLReader();
       		// instantiate our handler
       		RSSHandler theRssHandler = new RSSHandler();
       		// assign our handler
       		xmlreader.setContentHandler(theRssHandler);
       		// get our data via the url class
       		InputSource is = new InputSource(new FileInputStream(bbfc));
       		// perform the synchronous parse           
       		xmlreader.parse(is);
       		// get the results - should be a fully populated RSSFeed instance, or null on error
       		return theRssHandler.getFeed();
       	}
       	catch (Exception e2)
       	{
       		Log.e("JB","Exception:"+e2.toString());
       		// if we have a problem, simply return null
       		return null;
       	}
    }


}
