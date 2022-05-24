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
import com.KeyFrameKit.AV.Capture.KFAudioCapture;
import com.KeyFrameKit.AV.Capture.KFAudioCaptureConfig;
import com.KeyFrameKit.AV.Capture.KFAudioCaptureListener;
import com.KeyFrameKit.AV.MediaCodec.KFAudioByteBufferEncoder;
import com.KeyFrameKit.AV.MediaCodec.KFByteBufferCodec;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecInterface;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

public class MainActivity extends AppCompatActivity {
    private FileOutputStream mStream = null;
    private KFAudioCapture mAudioCapture = null;///< 音频采集模块
    private KFAudioCaptureConfig mAudioCaptureConfig = null;///< 音频采集配置
    private KFMediaCodecInterface mEncoder = null;///< 音频编码
    private MediaFormat mAudioEncoderFormat = null;///< 音频编码格式描述
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this,
                    new String[] {Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }

        mAudioCaptureConfig = new KFAudioCaptureConfig();
        mAudioCapture = new KFAudioCapture(mAudioCaptureConfig,mAudioCaptureListener);
        mAudioCapture.startRunning();

        if(mStream == null){
            try {
                mStream = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/test.aac");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        FrameLayout.LayoutParams startParams = new FrameLayout.LayoutParams(200, 120);
        startParams.gravity = Gravity.CENTER_HORIZONTAL;
        Button startButton = new Button(this);
        startButton.setTextColor(Color.BLUE);
        startButton.setText("开始");
        startButton.setVisibility(View.VISIBLE);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mEncoder == null){
                    mEncoder = new KFAudioByteBufferEncoder();
                    MediaFormat mediaFormat = KFAVTools.createAudioFormat(mAudioCaptureConfig.sampleRate,mAudioCaptureConfig.channel,96*1000);
                    mEncoder.setup(true,mediaFormat,mAudioEncoderListener,null);
                    ((Button)view).setText("停止");
                }else{
                    mEncoder.release();
                    mEncoder = null;
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

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void dataOnAvailable(KFFrame frame) {
            ///< 音频回调数据
            if(mAudioEncoderFormat == null && mEncoder != null){
                mAudioEncoderFormat = mEncoder.getOutputMediaFormat();
            }
            KFBufferFrame bufferFrame = (KFBufferFrame)frame;
            try {
                ///< 添加ADTS数据
                ByteBuffer adtsBuffer = KFAVTools.getADTS(bufferFrame.bufferInfo.size,mAudioEncoderFormat.getInteger(MediaFormat.KEY_PROFILE),mAudioEncoderFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),mAudioEncoderFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                byte[] adtsBytes = new byte[adtsBuffer.capacity()];
                adtsBuffer.get(adtsBytes);
                mStream.write(adtsBytes);

                byte[] dst = new byte[bufferFrame.bufferInfo.size];
                bufferFrame.buffer.get(dst);
                mStream.write(dst);
            }  catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}