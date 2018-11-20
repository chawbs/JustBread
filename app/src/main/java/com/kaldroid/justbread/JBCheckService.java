package com.kaldroid.justbread;

/*
 * @package: com.kaldroid.bbf
 * @activity: BreadCheckService
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: IntentService to fetch RSS data in background - sledge hammer approach
 *               Also handles setting alarm when verse changes etc
 *               Also handles saving to cache
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.TimeZone;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.kaldroid.justbread.R;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

public class JBCheckService extends IntentService {

	private static int NOTIFICATION_ID = 1;
	private static int lastTimestamp = 0;
	private static int lastCheck = 0;
	private static int lastUpdate = 0;
	private RSSFeed feed = null;
	private RSSItem newest = null;
	private String myAddress = null;
	private Intent updateEvent;
	
	private Context context = null;
	
	public JBCheckService() {
		super("JBCheckService");
		updateEvent = new Intent(JBBootReceiver.bcUpdateEvent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		this.context = this.getApplicationContext();
		JBBootReceiver.restoreSettings(context);
		
		if (doesNewBreadExist(context)) {
	    	try{

	    		// fetch bread first so we can see it...
	    		fetchBread();

	    		newest = feed.getItem(0);
    			String bread = newest.getDescription();
    			if (TextUtils.isEmpty(bread))
    				bread = newest.getContent();
    			/* get rid of divs */
    			bread = bread.replaceAll("<[/]*div>", "");
    			bread = bread.replaceAll("<div[ =\"'a-zA-Z0-9]*>", "");
    			bread = bread.replaceAll("<p>", "");
    			bread = bread.replaceAll("</p>", "<br/>");
    			bread = bread.replaceAll("<strong>", "");
    			bread = bread.replaceAll("</strong>", ";");
    			String[] lines = bread.split("<br[ ]*[/]+>", 2);
    			String[] words = lines[0].split(";", 2);
    			if (words.length > 1)
    				myAddress = words[0].trim();
    			else
    				myAddress = lines[0].trim();
    			
	    		// now set up notification (compat new style)
	    		NotificationManager notifications = (NotificationManager)
	    		context.getSystemService(Context.NOTIFICATION_SERVICE);
	    		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
	    		String title = (String)context.getText(R.string.app_name);
	    		String note = (String)context.getText(R.string.new_data);
	    		PendingIntent contentIntent = PendingIntent.getActivity(context, 
	    				NOTIFICATION_ID, 
	    				new Intent(context, JBDevotional.class), 0);
	    		builder.setAutoCancel(true);
	    		builder.setOnlyAlertOnce(true);
	    		builder.setContentTitle(title);
	    		builder.setContentText(note);
	    		builder.setContentInfo(myAddress);
	    		builder.setSmallIcon(R.drawable.ic_launcher);
	    		builder.setSound(JBBootReceiver.notifyUri);
	    		builder.setContentIntent(contentIntent);
	    		builder.setWhen(System.currentTimeMillis());
	    		builder.setDefaults(Notification.DEFAULT_VIBRATE);
	    		builder.setDefaults(Notification.DEFAULT_LIGHTS);

	    		Notification notification = builder.build();

	    		notifications.notify(NOTIFICATION_ID, notification);
	    		
	    		// send message back to UI to refresh
	    		updateEvent.putExtra("update", true);
	    		sendBroadcast(updateEvent);
	    	}
	    	catch(Exception e){}
		}
		// do we close service here to free resources? No, it does it...
	}
	
	//
    // do check for new bread without bringing back whole feed
    //
    private boolean doesNewBreadExist(Context context) {
    	boolean yesItDoes = false;
    	if (JBBootReceiver.debugMe) {
    		yesItDoes = true;
    	}
    	else {
    		String urlt = (String) context.getText(R.string.time_url);
    		long curTime = System.currentTimeMillis();
    		long unixTime = (curTime + TimeZone.getDefault().getOffset(curTime)) / 1000L;
    		urlt = urlt + "&time=" + Long.toString(unixTime);
    		ConnectivityManager conMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);  
    		NetworkInfo info= conMgr.getActiveNetworkInfo();

    		try {
    			if ((info != null) && (info.isConnected())) {
    				// get cached data
    				File sdCard = Environment.getExternalStorageDirectory();
    				File dir = new File (sdCard.getAbsolutePath() + JBBootReceiver.ourFolder);
    				File last = new File(dir, JBBootReceiver.timestampFile);
    				String eol = System.getProperty("line.separator");

    				try {
    					BufferedReader local = new BufferedReader(new InputStreamReader(new FileInputStream(last)));
    					String lasts = local.readLine();
    					lastTimestamp = Integer.parseInt(lasts);
    					lasts = local.readLine();
    					lastCheck = Integer.parseInt(lasts);
    					local.close();
    				}
    				catch (Exception ee) {
    					// set cached data to zero because it does not exist
    					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(last)));
    					writer.write("0" + eol + "0" + eol);
    					writer.close();
    					lastTimestamp = 0;
    					lastCheck = 0;
    				}

    				// get blog data - if it fails, just pretend there is nothing ready
    				try {
    					URL url = new URL(urlt);
    					BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
    					String line = input.readLine();
    					lastUpdate = Integer.parseInt(line);
    					input.close();
    				}
    				catch (IOException ex) {
    					lastUpdate = lastCheck;
    				}

    				if (lastCheck < lastUpdate) {
    					yesItDoes = true;
    					try {
    						lastCheck = lastUpdate;
    						BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(last)));
    						writer.write(Integer.toString(lastTimestamp) + eol + Integer.toString(lastUpdate) + eol);
    						writer.close();
    					}
    					catch (Exception e2) {
    					}
    				}
    			}
    			else {
    				yesItDoes = false;
    			}
    		}
    		catch (Exception ex) {
    			yesItDoes = false;
    		}
    	}
    	return yesItDoes;
    }

    private void fetchBread() {
    	try {
  			String url1 = (String) context.getText(R.string.feed_string) + JBBootReceiver.verseTranslation;
  			long curTime = System.currentTimeMillis();
  			long unixTime = (curTime + TimeZone.getDefault().getOffset(curTime)) / 1000L;
    		url1 = url1 + "&time=" + Long.toString(unixTime);
  			Log.i("BBF","Fetch Bread from: "+url1);
   			feed = getFeed(url1);
   			updateCache(feed.toString());

   			// set cached data
       		File sdCard = Environment.getExternalStorageDirectory();
       		File dir = new File (sdCard.getAbsolutePath() + JBBootReceiver.ourFolder);
       		File last = new File(dir, JBBootReceiver.timestampFile);
       		String eol = System.getProperty("line.separator");
        		
       		lastCheck = lastUpdate;
       		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(last)));
       		writer.write(Integer.toString(lastUpdate) + eol + Integer.toString(lastCheck) + eol);
       		writer.close();
    			
    		lastTimestamp = lastUpdate;
    	}
    	catch (Exception ex) {
    		Log.e("BBF","Exception:"+ex.toString()+ex.getMessage());
    	}
    }
    
    // local function to get and parse the feed - uses external classes to build up items
    private RSSFeed getFeed(String urlToRssFeed)
    {
		getBaseContext();
		ConnectivityManager conMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);  
		NetworkInfo info= conMgr.getActiveNetworkInfo();
		if ((info != null) && (info.isConnected())) {
	    	try
	    	{
	    		// setup the url
	    	   URL url = new URL(urlToRssFeed);

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
	           InputSource is = new InputSource(url.openStream());
	           // perform the synchronous parse           
	           xmlreader.parse(is);
	           // get the results - should be a fully populated RSSFeed instance, or null on error
	           return theRssHandler.getFeed();
	    	}
	    	catch (IOException ioex)
	    	{
	    		Log.e("BBF","IOException:"+ioex.toString()+ioex.getMessage());
	    		return null;
	    	}
	    	catch (Exception e1)
	    	{
	    		Log.e("BBF","Exception:"+e1.toString()+e1.getMessage());
	    		return null;
	    	}
		}
		else {
    		return null;
		}
    }

    private void updateCache(String bread) {
    	try {
    		File bbfc = null;
            try {
            	File sdCard = Environment.getExternalStorageDirectory();
            	File dir = new File (sdCard.getAbsolutePath() + JBBootReceiver.ourFolder);
            	bbfc = new File(dir, JBBootReceiver.cacheFile);
            }
            catch (Exception e1) {
            	bbfc = new File(JBBootReceiver.cacheFile);
            	Log.e("BBF","Exception:"+e1.toString());
            }
            Log.i("BBF","Update Cache in: "+bbfc.getPath());
    		String eol = System.getProperty("line.separator");
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(bbfc)));
			if (feed == null)
				writer.write(bread + eol);
			else
				writer.write(feed.toString() + eol);
			writer.close();
		} catch (Exception e) {
			Log.e("BBF","Exception:"+e.toString()+e.getMessage());
			e.printStackTrace();
		}
    }

}
