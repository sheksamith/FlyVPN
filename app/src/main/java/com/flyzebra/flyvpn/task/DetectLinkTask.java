package com.flyzebra.flyvpn.task;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.text.TextUtils;

import com.flyzebra.flyvpn.data.MpcMessage;
import com.flyzebra.flyvpn.utils.MyTools;

/**
 * ClassName: DetectLinkTask
 * Description:
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 19-12-10 上午11:14
 */
public class DetectLinkTask implements Runnable {
    private Context mContext;
    private RatdSocketTask ratdSocketTask;
    private static final int HEARTBEAT_TIME = 5000;
    private static final HandlerThread mDetectLinkThread = new HandlerThread("DetectLinkTask");

    static {
        mDetectLinkThread.start();
    }

    private static final Handler mDetectLinkHandler = new Handler(mDetectLinkThread.getLooper());

    public DetectLinkTask(Context context, RatdSocketTask ratdSocketTask) {
        this.mContext = context;
        this.ratdSocketTask = ratdSocketTask;
        mDetectLinkHandler.postDelayed(this, SystemClock.uptimeMillis() % 5000);
    }

    public void cancel() {
        mDetectLinkHandler.removeCallbacksAndMessages(null);
        ratdSocketTask = null;
    }

    @Override
    public void run() {
        long curretTime = SystemClock.uptimeMillis() % HEARTBEAT_TIME;
        long delayedTime = curretTime == 0 ? HEARTBEAT_TIME : HEARTBEAT_TIME - curretTime;
        mDetectLinkHandler.postDelayed(this, delayedTime);
        //添加网络
        if (mContext != null) {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Network networks[] = cm.getAllNetworks();
                for (Network network : networks) {
                    LinkProperties linkProperties = cm.getLinkProperties(network);
                    if (linkProperties != null && ratdSocketTask != null) {
                        String iface = linkProperties.getInterfaceName();
                        if (TextUtils.isEmpty(iface)) continue;
                        int netType = iface.startsWith("wlan") ? 4 : iface.startsWith("rmnet_data") ? 2 : iface.startsWith("mcwill") ? 1 : -1;
                        if (netType > 0) {
                            ratdSocketTask.sendMessage(String.format(MpcMessage.testLink, netType, iface, MyTools.createSessionId()));
                        }
                    }
                }
            }
        }
    }
}