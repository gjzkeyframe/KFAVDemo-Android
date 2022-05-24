package com.KeyFrameKit.AV.Demuxer;
//
//  KFDemuxer
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.KeyFrameKit.AV.Base.KFMediaBase;

import java.nio.ByteBuffer;

public class KFMP4Demuxer {
    public static final int KFDemuxerErrorAudioSetDataSource = -2300;
    public static final int KFDemuxerErrorVideoSetDataSource = -2301;
    public static final int KFDemuxerErrorAudioReadData = -2302;
    public static final int KFDemuxerErrorVideoReadData = -2303;

    private static final String TAG = "KFDemuxer";
    private KFDemuxerConfig mConfig = null;///< 解封装配置
    private KFDemuxerListener mListener = null;///< 回调
    private MediaExtractor mAudioMediaExtractor = null;///< 音频解封装器
    private MediaFormat mAudioMediaFormat = null;///< 音频格式描述
    private MediaExtractor mVideoMediaExtractor = null;///< 视频解封装器
    private MediaFormat mVideoMediaFormat = null;///< 视频格式描述
    private MediaMetadataRetriever mRetriever = null;///< 视频信息获取实例
    private Handler mMainHandler = new Handler(Looper.getMainLooper());///< 主线程

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public KFMP4Demuxer(KFDemuxerConfig config, KFDemuxerListener listener) {
        mConfig = config;
        mListener = listener;
        if(mRetriever == null){
            mRetriever = new MediaMetadataRetriever();
            mRetriever.setDataSource(mConfig.path);
        }

        ///< 初始化音频解封装器
        if(hasAudio() && (config.demuxerType.value() & KFMediaBase.KFMediaType.KFMediaAudio.value()) != 0){
            _setupAudioMediaExtractor();
        }

        ///< 初始化视频解封装器
        if(hasVideo() && (config.demuxerType.value() & KFMediaBase.KFMediaType.KFMediaVideo.value()) != 0){
            _setupVideoMediaExtractor();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void release() {
        ///< 释放音视频解封装器、视频信息获取实例
        if(mAudioMediaExtractor != null){
            mAudioMediaExtractor.release();
            mAudioMediaExtractor = null;
        }

        if(mVideoMediaExtractor != null){
            mVideoMediaExtractor.release();
            mVideoMediaExtractor = null;
        }

        if(mRetriever != null){
            mRetriever.release();
            mRetriever = null;
        }
    }

    public boolean hasVideo() {
        ///< 是否包含视频
        if(mRetriever == null){
            return false;
        }
        String value = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
        return value != null && value.equals("yes");
    }

    public boolean hasAudio() {
        ///< 是否包含音频
        if(mRetriever == null){
            return false;
        }
        String value = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
        return value != null && value.equals("yes");
    }

    public int duration() {
        ///< 文件时长
        if(mRetriever == null){
            return 0;
        }
        return Integer.parseInt(mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public int rotation() {
        ///< 视频旋转
        if(mVideoMediaFormat == null){
            return 0;
        }
        return mVideoMediaFormat.getInteger(MediaFormat.KEY_ROTATION);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public boolean isHEVC() {
        ///< 是否为H265
        if(mVideoMediaFormat == null){
            return false;
        }
        String mime = mVideoMediaFormat.getString(MediaFormat.KEY_MIME);
        return mime.contains("hevc") || mime.contains("dolby-vision");
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public int width() {
        ///< 视频宽度
        if(mVideoMediaFormat == null){
            return 0;
        }
        return mVideoMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public int height() {
        ///< 视频高度
        if(mVideoMediaFormat == null){
            return 0;
        }
        return mVideoMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public int samplerate() {
        ///< 音频采样率
        if(mAudioMediaFormat == null){
            return 0;
        }
        return mAudioMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public int channel() {
        ///< 音频声道数
        if(mAudioMediaFormat == null){
            return 0;
        }
        return mAudioMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public int audioProfile() {
        ///<AAC HEAAC等
        if(mAudioMediaFormat == null){
            return 0;
        }
        return mAudioMediaFormat.getInteger(MediaFormat.KEY_PROFILE);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public int videoProfile() {
        ///< 视频画质级别 BaseLine Main High 等
        if(mVideoMediaFormat == null){
            return 0;
        }
        return mVideoMediaFormat.getInteger(MediaFormat.KEY_PROFILE);
    }

    public MediaFormat audioMediaFormat() {
        return mAudioMediaFormat;
    }

    public MediaFormat videoMediaFormat() {
        return mVideoMediaFormat;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public ByteBuffer readAudioSampleData(MediaCodec.BufferInfo bufferInfo){
        ///< 音频数据读取
        if (mAudioMediaExtractor == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(500 * 1024);
        try {
            bufferInfo.size = mAudioMediaExtractor.readSampleData(buffer, 0);
        } catch (Exception e) {
            Log.e(TAG, "readSampleData" + e);
            return null;
        }

        if (bufferInfo.size > 0) {
            bufferInfo.flags = mAudioMediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
            bufferInfo.presentationTimeUs = mAudioMediaExtractor.getSampleTime();
            mAudioMediaExtractor.advance();
            return buffer;
        } else {
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public ByteBuffer readVideoSampleData(MediaCodec.BufferInfo bufferInfo){
        ///< 视频数据读取
        if (mVideoMediaExtractor == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(1000 * 1024);
        try {
            bufferInfo.size = mVideoMediaExtractor.readSampleData(buffer, 0);
        } catch (Exception e) {
            Log.e(TAG, "readVideoData" + e);
            return null;
        }

        if (bufferInfo.size > 0) {
            bufferInfo.flags = mVideoMediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
            bufferInfo.presentationTimeUs = mVideoMediaExtractor.getSampleTime();
            mVideoMediaExtractor.advance();
            return buffer;
        } else {
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void _setupAudioMediaExtractor() {
        ///< 初始化音频解封装器
        if(mAudioMediaExtractor == null){
            mAudioMediaExtractor = new MediaExtractor();
            try {
                mAudioMediaExtractor.setDataSource(mConfig.path);
            }catch (Exception e){
                Log.e(TAG, "setDataSource" + e);
                _callBackError(KFDemuxerErrorAudioSetDataSource,e.getMessage());
                return;
            }

            ///< 查找音频轨道与格式描述
            int numberTracks = mAudioMediaExtractor.getTrackCount();
            for(int index = 0;index < numberTracks;index ++){
                MediaFormat format = mAudioMediaExtractor.getTrackFormat(index);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    mAudioMediaFormat = format;
                    mAudioMediaExtractor.selectTrack(index);
                    mAudioMediaExtractor.seekTo(0,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void _setupVideoMediaExtractor() {
        ///< 初始化视频解封装器
        if(mVideoMediaExtractor == null){
            mVideoMediaExtractor = new MediaExtractor();
            try {
                mVideoMediaExtractor.setDataSource(mConfig.path);
            }catch (Exception e){
                Log.e(TAG, "setDataSource" + e);
                _callBackError(KFDemuxerErrorVideoSetDataSource,e.getMessage());
                return;
            }

            ///< 查找视频轨道与格式描述
            int numberTracks = mVideoMediaExtractor.getTrackCount();
            for(int index = 0;index < numberTracks;index ++){
                MediaFormat format = mVideoMediaExtractor.getTrackFormat(index);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    mVideoMediaFormat = format;
                    mVideoMediaExtractor.selectTrack(index);
                    mVideoMediaExtractor.seekTo(0,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                }
            }
        }
    }

    private void _callBackError(int error, String errorMsg){
        if(mListener != null){
            mMainHandler.post(()->{
                mListener.demuxerOnError(error,TAG + errorMsg);
            });
        }
    }
}
