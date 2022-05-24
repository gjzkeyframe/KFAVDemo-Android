package com.KeyFrameKit.AV.Render;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecListener;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static android.media.AudioTrack.STATE_INITIALIZED;

//
//  KFAudioRender
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
public class KFAudioRender {
    private static final String TAG = "KFAudioRender";
    public static final int KFAudioRenderErrorCreate = -2700;
    public static final int KFAudioRenderErrorPlay = -2701;
    public static final int KFAudioRenderErrorStop = -2702;
    public static final int KFAudioRenderErrorPause = -2703;

    private static final int KFAudioRenderMaxCacheSize = 500*1024;///< 音频PCM缓存最大值
    private KFAudioRenderListener mListener = null;///< 回调
    private Handler mMainHandler = new Handler(Looper.getMainLooper()); ///< 主线程
    private HandlerThread mThread = null;///< 音频管控线程
    private Handler mHandler = null;
    private HandlerThread mRenderThread = null;///< 音频渲染线程
    private Handler mRenderHandler = null;
    private AudioTrack mAudioTrack = null;///< 音频播放实例
    private int mMinBufferSize = 0;
    private byte mCache[] = new byte[KFAudioRenderMaxCacheSize];///< 音频PCM缓存
    private int mCacheSize = 0;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public KFAudioRender(KFAudioRenderListener listener, int sampleRate, int channel){
        mListener = listener;
        ///< 创建音频管控线程
        mThread = new HandlerThread("KFAudioRenderThread");
        mThread.start();
        mHandler = new Handler((mThread.getLooper()));
        ///< 创建音频渲染线程
        mRenderThread = new HandlerThread("KFAudioGetDataThread");
        mRenderThread.start();
        mRenderHandler = new Handler((mRenderThread.getLooper()));

        mHandler.post(()->{
            ///< 初始化音频播放实例
           _setupAudioTrack(sampleRate,channel);
        });
    }

    public void release() {
        mHandler.post(()-> {
            ///< 停止与释放音频播放实例
            if(mAudioTrack != null){
                try {
                    mAudioTrack.stop();
                    mAudioTrack.release();
                } catch (Exception e) {
                    Log.e(TAG, "release: " + e.toString());
                }
                mAudioTrack = null;
            }

            mThread.quit();
            mRenderThread.quit();
        });
    }

    public void play() {
        mHandler.post(()-> {
            ///< 音频实例播放
            try {
                mAudioTrack.play();
            }catch (Exception e){
                _callBackError(KFAudioRenderErrorPlay,e.getMessage());
                return;
            }

            mRenderHandler.post(()->{
                ///< 循环写入PCM数据，写入系统缓冲区，当读取到最大值或者状态机不等于STATE_INITIALIZED 则退出循环
                while (mAudioTrack.getState() == STATE_INITIALIZED){
                    if(mListener != null && mCacheSize < KFAudioRenderMaxCacheSize){
                        byte[] bytes = mListener.audioPCMData(mMinBufferSize);
                        if(bytes != null && bytes.length > 0){
                            System.arraycopy(bytes,0,mCache,mCacheSize,bytes.length);
                            mCacheSize += bytes.length;
                            if(mCacheSize >= mMinBufferSize){
                                int writeSize = mAudioTrack.write(mCache,0,mMinBufferSize);
                                if(writeSize > 0){
                                    mCacheSize -= writeSize;
                                    System.arraycopy(mCache,writeSize,mCache,0,mCacheSize);
                                }
                            }
                        }else{
                            break;
                        }
                    }
                }
            });
        });
    }

    public void stop() {
        ///< 停止音频播放
        mHandler.post(()-> {
            try {
                mAudioTrack.stop();
            }catch (Exception e){
                _callBackError(KFAudioRenderErrorStop,e.getMessage());
            }
            mCacheSize = 0;
        });
    }

    public void pause() {
        ///< 暂停音频播放
        mHandler.post(()-> {
            try {
                mAudioTrack.pause();
            }catch (Exception e){
                _callBackError(KFAudioRenderErrorPause,e.getMessage());
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void  _setupAudioTrack(int sampleRate, int channel){
        ///< 根据采样率、声道获取每次音频播放塞入数据大小，根据采样率、声道、数据大小创建音频播放实例
        if(mAudioTrack == null){
            try {
                mMinBufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, AudioFormat.ENCODING_PCM_16BIT);
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,channel == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,mMinBufferSize,AudioTrack.MODE_STREAM);
            }catch (Exception e){
                _callBackError(KFAudioRenderErrorCreate,e.getMessage());
            }
        }
    }

    private void _callBackError(int error, String errorMsg){
        if(mListener != null){
            mMainHandler.post(()->{
                mListener.onError(error,TAG + errorMsg);
            });
        }
    }
}
