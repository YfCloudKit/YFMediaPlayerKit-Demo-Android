package com.yunfan.player.extra;

import android.util.Log;

import com.yunfan.net.IYfCallBack;
import com.yunfan.net.Yfnet;


/**
 * Created by xjx-pc on 2016/1/29 0029.
 */
public class YfP2PHelper {
    private static String TAG = "Yf_P2P_Helper";
    private static Yfnet mYfp2p = new Yfnet();
    private static String handleUrl = "";

    /**
     * 只需要初始化一次即可
     *
     * @return
     */
    public static int initNetLib() {
//        System.loadLibrary("yfnet_xinlan");
//        String strConfigPath = Environment.getExternalStorageDirectory().toString() + "/yfnet/config/";
//        String strCachePath = Environment.getExternalStorageDirectory().toString() + "/yfnet/cache/";
//        int ret = mYfp2p.JInit(strConfigPath, strCachePath, callBack);
//        Log.i(TAG, "Init Yfnet ret = " + ret);
//        configP2P();//也可以不配置
        //直播部分
        mYfp2p = new Yfnet();
        int ret = mYfp2p.JInit(callBack);
        Log.i(TAG, "Init Yfnet ret = " + ret);
        return ret;
    }

    /**
     * 配置P2P点播相关内容，默认允许上传
     */
    private static void configP2P() {
        //禁止p2p上传
//        mYfp2p.JEnableUpload(false);
    }

    /**
     * 生成p2p点播任务
     *
     * @param strOrgUrl 在线视频的链接,可直接用来http下载文件的url //详情参考文档
     * @param strKeyUrl 标识视频唯一性的字符串 //详情参考文档
     * @return
     */
    public static String createDemandNetTask(String strOrgUrl, String strKeyUrl) {
        String[] mStrHash = new String[1];
        String[] mStrHttpProxyUrl = new String[1];
//        int ret = mYfp2p.JCreateTask(strOrgUrl, strKeyUrl, mStrHash, mStrHttpProxyUrl);
//        if (Yfnet.E_NET_OK == ret || Yfnet.E_NET_TASK_FINISH == ret) {
//            handleUrl = mStrHash[0];
//            Log.i(TAG, "HttpProxyUrl = " + mStrHttpProxyUrl[0]);
//            //任务创建成功或者任务已经缓存完
//            //设置任务为播放状态
//            mYfp2p.JSetPlayingStatus(handleUrl, true);
//            //设置任务为运行状态
//            mYfp2p.JRunTask(handleUrl);
//
//        }
        return mStrHttpProxyUrl[0];
    }

    /**
     * 生成P2P直播任务地址
     *
     * @param url 视频播放地址
     * @return 放入播放器的地址
     */
    public static String createDirectNetTast(String url) {
        if (!(url.startsWith("rtmp:") || url.startsWith("http:"))) {
            return url;
        }
        String p2pUrl = mYfp2p.JCreateChannel(url);
        Log.i(TAG, "CreateChannel ret = " + p2pUrl);
        return p2pUrl;
    }

    /**
     * P2P直播暂停播放时调用
     */
    public static void pauseDirectNetBuffer() {
        mYfp2p.JPauseChannel();
    }

    /**
     * P2P直播恢复播放时调用
     */
    public static void resumeDirectNetBuffer() {
        mYfp2p.JResumeChannel();
    }

    /**
     * P2P点播播放完、退出播放，需要暂停缓存任务并设置为非播放状态
     * 根据app情景使用
     */
    public static void stopDemandNetBuffer() {
//        //设置任务为暂停缓存状态
//        mYfp2p.JPauseTask(handleUrl);
//        //设置任务为非播放状态
//        mYfp2p.JSetPlayingStatus(handleUrl, false);
    }

    /**
     * P2P直播播放完、退出播放，需要暂停缓存任务并设置为非播放状态
     * 根据app情景使用
     */
    public static void stopDirectNetBuffer() {
        mYfp2p.JCloseChannel();
    }

    /**
     * 退出程序时注销sdk
     */
    public static void clearNetSdk() {
        mYfp2p.JClear();
    }

    private static IYfCallBack callBack = new IYfCallBack() {
        @Override
        public void CallBack(int i, String s) {

        }
    };
}
