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

import com.KeyFrameKit.AV.Base.KFAVTools;
import com.KeyFrameKit.AV.Base.KFMediaBase;
import com.KeyFrameKit.AV.Demuxer.KFMP4Demuxer;
import com.KeyFrameKit.AV.Demuxer.KFDemuxerConfig;
import com.KeyFrameKit.AV.Demuxer.KFDemuxerListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private KFMP4Demuxer mDemuxer;///< 解封装实例
    private KFDemuxerConfig mDemuxerConfig;///< 解封装配置
    private FileOutputStream mStream = null;

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

        mDemuxerConfig = new KFDemuxerConfig();
        mDemuxerConfig.path = Environment.getExternalStorageDirectory().getPath() + "/2.mp4";
        mDemuxerConfig.demuxerType = KFMediaBase.KFMediaType.KFMediaAudio;
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
                ///< 创建解封装实例
                if(mDemuxer == null){
                    mDemuxer = new KFMP4Demuxer(mDemuxerConfig,mDemuxerListener);

                    ///< 读取音频数据
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    ByteBuffer nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
                    while (nextBuffer != null){
                        try {
                            ///< 添加 ADTS
                            ByteBuffer adtsBuffer = KFAVTools.getADTS(bufferInfo.size,mDemuxer.audioProfile(),mDemuxer.samplerate(),mDemuxer.channel());
                            byte[] adtsBytes = new byte[adtsBuffer.capacity()];
                            adtsBuffer.get(adtsBytes);
                            mStream.write(adtsBytes);

                            byte[] dst = new byte[bufferInfo.size];
                            nextBuffer.get(dst);
                            mStream.write(dst);
                        }  catch (IOException e) {
                            e.printStackTrace();
                        }
                        nextBuffer = mDemuxer.readAudioSampleData(bufferInfo);
                    }
                    Log.i("KFDemuxer","complete");
                }
            }
        });
        addContentView(startButton, startParams);
    }

    private KFDemuxerListener mDemuxerListener = new KFDemuxerListener() {
        ///< 解封装错误回调
        @Override
        public void demuxerOnError(int error, String errorMsg) {
            Log.i("KFDemuxer","error" + error + "msg" + errorMsg);
        }
    };
}