* 该项目为**Android端播放器demo**示例代码。该demo集成了播放器的所有功能。
* [播放sdk文档地址](http://doc.yfcloud.com/index.php?s=/5&page_id=83)
* [下载安装推流demo](http://www.yfcloud.com/yunfansdkdownload/YFPlayerSDK_Android_demo.apk)

![播放器demo](https://i.imgur.com/FNGsd3V.png)

--- 


# 云帆Android播放器SDK 

### 简介

---

YfPlayerKit是云帆加速推出的Android平台用于播放视频的软件开发工具包（SDK)，为您提供简单、便捷的开发接口，助您在基于 Android 4.1 及以上版本的移动设备上实现视频播放功能。

- [YfPlayerKit SDK下载地址](https://github.com/yfcloudStreamEngine/YfPlayerKit_Android_DEMO)

### 功能特点
---
- 支持主流文件格式和协议，包括
flv, mp4, ts, rtmp, http, hls等协议的直播和点播
- 支持超低延时播放，配合云帆直播服务，控制播放延迟2秒内
- 支持软硬解切换
- 支持画面旋转
- 支持H265解码
- 支持OpenGL渲染
- 支持无缝衔接播放，软解下无缝渲染
- 支持秒开视频，打开速度仅需300毫秒
- 提供类VideoView及类Mediaplayer两种接口的调用，接入简单快捷


### 运行环境
---

功能 | Android版本| 备注
---|---|---
 软解 | Android 2.3及以上版本| 支持无缝渲染
硬解 | Android 4.1及以上版本| 效率更高

### 下载并使用SDK
---
### 1. 从github下载工程

    工程内容说明(完整下载包包含SDK、DOC、Demo三部分）：
    - SDK目录：YfPlayerKit.jar、libffmpeg.so、libyfplayer.so、libyfsdl.so
    - DOC目录：YFEncoderKit帮助文档.pdf、README.MD
    - Demo:集成了推流sdk的所有功能示范

### 2、鉴权
获取SDK使用许可的Token,在app启动的时候调用全局静态鉴权方法。

```
private YfAuthentication.AuthCallBack authCallBack = new YfAuthentication.AuthCallBack() {
        @Override
        public void onAuthenticateSuccess() {
            Log.d(TAG, "鉴权成功~！");
        }

        @Override
        public void onAuthenticateError(int errorCode) {
            Log.d(TAG, "鉴权失败啦：" + errorCode);
        }
    };
YfAuthentication.getInstance().authenticate(ACCESS_KEY,TOKEN, authCallBack);
```
为了防止在鉴权的时候因为网络异常而鉴权失败，可以在恢复网络的时候确认一下是否鉴权成功，如果没有则再次发起鉴权。
    
```
if(!Authentication.isAuthenticateSucceed()){
            //retry
        }
```
#### 注意事项
github或官网上下载的sdk仅支持使用测试域名进行播放，正式使用请与我们联系获取正式token并设置播放域名。

### 3、在APP上集成SDK
#### YfPlayerKit(类似Android原生的VideoView，封装了Surface）

1、在layout使用YfPlayerKit
    
```
    <com.yunfan.player.widget.YfPlayerKit
            android:id="@+id/surface_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_centerInParent="true" />
```
3、实现回调接口并设置回调

```
//根据需要实现YfCloudPlayer.OnPreparedListener等回调接口
mVideoView.setOnPreparedListener(this);
mVideoView.setOnErrorListener(this);
mVideoView.setOnBufferingUpdateListener(this);
mVideoView.setOnInfoListener(this);
```

3、播放视频

```
YfPlayerKit mVideoView = (YfPlayerKit) findViewById(R.id.surface_view);
mVideoView.setVideoPath(path);
mVideoView.start();
```



4、暂停/停止播放
    
```
mVideoView.pause();//暂停
mVideoView.stopPlayback();//停止
mVideoView.release(true);//释放播放器

```

#### YfCloudPlayer（类似于原生MediaPlayer，不带Surface）
1、配置播放器

```
YfCloudPlayer yfMediaPlayer = YfCloudPlayer.Factory.createPlayer(this, mDecodeMode);//以某种解码方式初始化播放器
yfMediaPlayer.setDisplay(holder);//设置渲染Surface
yfMediaPlayer.prepareAsync();//准备播放

```

2、设置YfCloudPlayer.OnPreparedListener（必需）及其他回调

```
yfMediaPlayer.setOnCompletionListener(mCompletionListener);
yfMediaPlayer.setOnPreparedListener(mPreparedListener);
yfMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
yfMediaPlayer.setOnErrorListener(mErrorListener);
```

3、在OnPrepared的回调中播放视频

```
yfMediaPlayer.start();
```

4、停止播放

```
 yfMediaPlayer.stop();//停止
 
 yfMediaPlayer.release();//释放播放器
 yfMediaPlayer = null;//释放播放器后为保证状态一致建议将其置空
```

   更多功能的使用请参考Demo。
   


### 接口说明
---
[Android端播放器API文档](http://doc.yfcloud.com/index.php?s=/5&page_id=82 "播放器API文档")

### 反馈和建议
---
主页：云帆加速





 