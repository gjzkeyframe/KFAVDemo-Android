package com.KeyFrameKit.AV;
//
//  MainActivity
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.KeyFrameKit.AV.Base.KFBufferFrame;
import com.KeyFrameKit.AV.Base.KFFrame;
import com.KeyFrameKit.AV.Base.KFMediaBase;
import com.KeyFrameKit.AV.MediaCodec.KFByteBufferCodec;
import com.KeyFrameKit.AV.Demuxer.KFMP4Demuxer;
import com.KeyFrameKit.AV.Demuxer.KFDemuxerConfig;
import com.KeyFrameKit.AV.Demuxer.KFDemuxerListener;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecInterface;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecListener;
import com.KeyFrameKit.AV.Render.KFAudioRender;
import com.KeyFrameKit.AV.Render.KFAudioRenderListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {
    private KFMP4Demuxer mDemuxer;///< 音频解封装实例
    private KFDemuxerConfig mDemuxerConfig;///< 音频解决封装配置
    private KFMediaCodecInterface mDecoder;///< 音频解码实例
    private KFAudioRender mRender;///< 音频渲染实例
    private byte[] mPCMCache = new byte[10*1024*1024];///< PCM数据缓存
    private int mPCMCacheSize = 0;
    private ReentrantLock mLock = new ReentrantLock(true);

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ///< 获取音频采集、本地存储权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this,
                    new String[] {Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }

        ///< 创建音频解封装配置
        mDemuxerConfig = new KFDemuxerConfig();
        mDemuxerConfig.path = Environment.getExternalStorageDirectory().getPath() + "/test.aac";
        mDemuxerConfig.demuxerType = KFMediaBase.KFMediaType.KFMediaAudio;

        ///< 创建音频解封装实例
        mDemuxer = new KFMP4Demuxer(mDemuxerConfig,mDemuxerListener);
        mDecoder = new KFByteBufferCodec();
        mDecoder.setup(false,mDemuxer.audioMediaFormat(),mDecoderListener,null);

        ///< 循环获取解封装数据塞入解码器
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
        while (nextBuffer != null){
            mDecoder.processFrame(new KFBufferFrame(nextBuffer,bufferInfo));
            nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
        }

        ///< 创建音频渲染实例
        mRender = new KFAudioRender(mRenderListener,mDemuxer.samplerate(),mDemuxer.channel());
        mRender.play();
    }

    private KFDemuxerListener mDemuxerListener = new KFDemuxerListener() {
        @Override
        ///< 解封装出错
        public void demuxerOnError(int error, String errorMsg) {
            Log.i("KFDemuxer","error" + error + "msg" + errorMsg);
        }
    };

    private KFMediaCodecListener mDecoderListener = new KFMediaCodecListener() {
        @Override
        ///< 解码出错
        public void onError(int error, String errorMsg) {

        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        ///< 解码数据回调存储到本地 PCM 缓存，Demo 处理比较简单，没有考虑到渲染暂停解码不暂停等case，可能存在缓冲区溢出
        public void dataOnAvailable(KFFrame frame) {
            KFBufferFrame bufferFrame = (KFBufferFrame)frame;
            if(bufferFrame.buffer != null && bufferFrame.bufferInfo.size > 0){
                byte[] bytes = new byte[bufferFrame.bufferInfo.size];
                bufferFrame.buffer.get(bytes);
                mLock.lock();
                System.arraycopy(bytes,0,mPCMCache,mPCMCacheSize,bytes.length);
                mPCMCacheSize += bytes.length;
                mLock.unlock();
            }
        }
    };

    private KFAudioRenderListener mRenderListener = new KFAudioRenderListener() {
        @Override
        ///< 音频渲染出错
        public void onError(int error, String errorMsg) {

        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        ///< 音频播放模块获取音频 PCM 数据
        public byte[] audioPCMData(int size) {
            if(mPCMCacheSize >= size){
                byte[] dst = new byte[size];
                mLock.lock();
                System.arraycopy(mPCMCache,0,dst,0,size);
                mPCMCacheSize -= size;
                System.arraycopy(mPCMCache,size,mPCMCache,0,mPCMCacheSize);
                mLock.unlock();
                return dst;
            }
            return null;
        }
    };
}