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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.KeyFrameKit.AV.Base.KFBufferFrame;
import com.KeyFrameKit.AV.Base.KFFrame;
import com.KeyFrameKit.AV.Capture.KFAudioCapture;
import com.KeyFrameKit.AV.Capture.KFAudioCaptureConfig;
import com.KeyFrameKit.AV.Capture.KFAudioCaptureListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

public class MainActivity extends AppCompatActivity {
    private FileOutputStream mStream = null;
    private KFAudioCapture mAudioCapture = null;
    private KFAudioCaptureConfig mAudioCaptureConfig = null;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ///< 音频录制权限
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
                mStream = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/test.pcm");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    ///< 音频采集回调
    private KFAudioCaptureListener mAudioCaptureListener = new KFAudioCaptureListener() {
        @Override
        public void onError(int error, String errorMsg) {
            Log.e("KFAudioCapture","errorCode" + error + "msg"+errorMsg);
        }

        @Override
        public void onFrameAvailable(KFFrame frame) {
            ///< 获取到音频Buffer数据存储到本地PCM
            try {
                ByteBuffer pcmData = ((KFBufferFrame)frame).buffer;
                byte[] ppsBytes = new byte[pcmData.capacity()];
                pcmData.get(ppsBytes);
                mStream.write(ppsBytes);
            }  catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}