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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.KeyFrameKit.AV.Base.KFAVTools;
import com.KeyFrameKit.AV.Base.KFBufferFrame;
import com.KeyFrameKit.AV.Base.KFFrame;
import com.KeyFrameKit.AV.Base.KFMediaBase;
import com.KeyFrameKit.AV.Capture.KFAudioCapture;
import com.KeyFrameKit.AV.Capture.KFAudioCaptureConfig;
import com.KeyFrameKit.AV.Capture.KFAudioCaptureListener;
import com.KeyFrameKit.AV.MediaCodec.KFAudioByteBufferEncoder;
import com.KeyFrameKit.AV.MediaCodec.KFByteBufferCodec;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecInterface;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecListener;
import com.KeyFrameKit.AV.Muxer.KFMP4Muxer;
import com.KeyFrameKit.AV.Muxer.KFMuxerConfig;
import com.KeyFrameKit.AV.Muxer.KFMuxerListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

public class MainActivity extends AppCompatActivity {
    private KFAudioCapture mAudioCapture = null;///< 音频采集
    private KFAudioCaptureConfig mAudioCaptureConfig = null;///< 音频采集配置
    private KFMediaCodecInterface mEncoder = null;///< 音频编码
    private MediaFormat mAudioEncoderFormat = null;///< 音频编码格式描述
    private KFMP4Muxer mMuxer;///< 封装起器
    private KFMuxerConfig mMuxerConfig; ///< 封装器配置
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ///< 申请存储、音频采集权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this,
                    new String[] {Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }

        ///< 创建采集实例
        mAudioCaptureConfig = new KFAudioCaptureConfig();
        mAudioCapture = new KFAudioCapture(mAudioCaptureConfig,mAudioCaptureListener);
        mAudioCapture.startRunning();

        mMuxerConfig = new KFMuxerConfig(Environment.getExternalStorageDirectory().getPath() + "/test.m4a");
        mMuxerConfig.muxerType = KFMediaBase.KFMediaType.KFMediaAudio;

        FrameLayout.LayoutParams startParams = new FrameLayout.LayoutParams(200, 120);
        startParams.gravity = Gravity.CENTER_HORIZONTAL;
        Button startButton = new Button(this);
        startButton.setTextColor(Color.BLUE);
        startButton.setText("开始");
        startButton.setVisibility(View.VISIBLE);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ///< 创建音频编码实例
                if(mEncoder == null){
                    mEncoder = new KFAudioByteBufferEncoder();
                    MediaFormat mediaFormat = KFAVTools.createAudioFormat(mAudioCaptureConfig.sampleRate,mAudioCaptureConfig.channel,96*1000);
                    mEncoder.setup(true,mediaFormat,mAudioEncoderListener,null);
                    ((Button)view).setText("停止");
                    mMuxer = new KFMP4Muxer(mMuxerConfig,mMuxerListener);
                }else{
                    mEncoder.release();
                    mEncoder = null;
                    mMuxer.stop();
                    mMuxer.release();
                    mMuxer = null;
                    ((Button)view).setText("开始");
                }
            }
        });
        addContentView(startButton, startParams);
    }

    private KFAudioCaptureListener mAudioCaptureListener = new KFAudioCaptureListener() {
        @Override
        public void onError(int error, String errorMsg) {
            Log.e("KFAudioCapture","errorCode" + error + "msg"+errorMsg);
        }

        @Override
        public void onFrameAvailable(KFFrame frame) {
            ///< 采集回调输入编码
            if(mEncoder != null){
                mEncoder.processFrame(frame);
            }
        }
    };

    private KFMediaCodecListener mAudioEncoderListener = new KFMediaCodecListener() {
        @Override
        public void onError(int error, String errorMsg) {
            Log.i("KFMediaCodecListener","error" + error + "msg" + errorMsg);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void dataOnAvailable(KFFrame frame) {
            ///< 编码回调写入封装器
            if(mAudioEncoderFormat == null && mEncoder != null){
                mAudioEncoderFormat = mEncoder.getOutputMediaFormat();
                mMuxer.setAudioMediaFormat(mEncoder.getOutputMediaFormat());
                mMuxer.start();
            }

            if(mMuxer != null){
                mMuxer.writeSampleData(false,((KFBufferFrame)frame).buffer,((KFBufferFrame)frame).bufferInfo);
            }
        }
    };

    private KFMuxerListener mMuxerListener = new KFMuxerListener() {
        @Override
        public void muxerOnError(int error, String errorMsg) {
            ///< 音频封装错误回调
            Log.i("KFMuxerListener","error" + error + "msg" + errorMsg);
        }
    };
}