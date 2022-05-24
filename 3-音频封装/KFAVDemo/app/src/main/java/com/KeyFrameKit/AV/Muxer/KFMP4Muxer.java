package com.KeyFrameKit.AV.Muxer;
//
//  KFMP4Muxer
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.KeyFrameKit.AV.Base.KFBufferFrame;
import com.KeyFrameKit.AV.Base.KFMediaBase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class KFMP4Muxer {
    public static final int KFMuxerErrorCreate = -2200;
    public static final int KFMuxerErrorAudioAddTrack = -2201;
    public static final int KFMuxerErrorVideoAddTrack = -2202;

    private static final String TAG = "KFMuxer";
    private KFMuxerConfig mConfig = null;///< 封装配置
    private KFMuxerListener mListener = null;///< 回调
    private MediaMuxer mMediaMuxer = null;///< 封装实例
    private int mVideoTrackIndex = -1;///< 视频track轨道下标
    private MediaFormat mVideoFormat = null;///< 视频输入视频格式描述
    private List<KFBufferFrame> mVideoList = new ArrayList<>();///< 视频输入缓存
    private int mAudioTrackIndex = -1;///< 音频track轨道下标
    private MediaFormat mAudioFormat = null;///< 音频输入视频格式描述
    private List<KFBufferFrame> mAudioList = new ArrayList<>();///< 音频输入缓存
    private boolean mIsStart = false;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());///< 主线程

    public KFMP4Muxer(KFMuxerConfig config, KFMuxerListener listener) {
        mConfig = config;
        mListener = listener;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void start(){
        _setupMuxer();
    }

    public void stop(){
        _stop();
    }

    public void setVideoMediaFormat(MediaFormat mediaFormat) {
        mVideoFormat = mediaFormat;
    }

    public void setAudioMediaFormat(MediaFormat mediaFormat) {
        mAudioFormat = mediaFormat;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    ///< 写入音视频数据(编码后数据)
    public void writeSampleData(boolean isVideo, ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo){
        if((bufferInfo.flags & BUFFER_FLAG_CODEC_CONFIG) != 0){
            return;
        }

        if(buffer ==null || bufferInfo == null || mMediaMuxer == null || bufferInfo.size == 0){
            return;
        }

        ///< 校验视频数据是否进入
        if(!_hasAudioTrack() && !isVideo){
            return;
        }

        ///< 校验视频数据是否进入
        if(!_hasVideoTrack() && isVideo){
            return;
        }

        ///< 数据转换结构体KFBufferFrame
        KFBufferFrame packet = new KFBufferFrame();
        ByteBuffer newBuffer = ByteBuffer.allocateDirect(bufferInfo.size);
        newBuffer.put(buffer).position(0);

        MediaCodec.BufferInfo newInfo = new MediaCodec.BufferInfo();
        newInfo.size = bufferInfo.size;
        newInfo.flags = bufferInfo.flags;
        newInfo.presentationTimeUs = bufferInfo.presentationTimeUs;

        packet.buffer = newBuffer;
        packet.bufferInfo = newInfo;
        if(isVideo){
            ///< 初始化视频Track
            if(mVideoFormat != null && mVideoTrackIndex == -1){
                _setupVideoTrack();
            }
            mVideoList.add(packet);
        }else{
            ///< 初始化音频Track
            if(mAudioFormat != null && mAudioTrackIndex == -1){
                _setupAudioTrack();
            }
            mAudioList.add(packet);
        }

        ///< 校验音视频Track是否都初始化好
        if((_hasAudioTrack() && _hasVideoTrack() && mAudioTrackIndex >=0 && mVideoTrackIndex >= 0) ||
                (_hasAudioTrack() && !_hasVideoTrack() && mAudioTrackIndex >= 0) ||
                (!_hasAudioTrack() && _hasVideoTrack() && mVideoTrackIndex >= 0)){
            if(!mIsStart){
                _start();
                mIsStart = true;
            }

            ///< 音视频交错 目的音视频时间戳尽量不跳跃
            if(mIsStart){
                _avInterleavedBuffers();
            }
        }
    }

    public void release() {
        _stop();
    }

    private void _start() {
        ///< 开启封装
        try {
            if(mMediaMuxer != null){
                mMediaMuxer.start();
            }
        }catch (Exception e){
            Log.e(TAG, "start" + e);
        }
    }

    private void _stop() {
        ///< 关闭封装
        try {
            if(mMediaMuxer != null){
                ///< 兜底一路没进来的case 如果外层配置音视频一起封装但最终只进来一路也会处理
                if(!mIsStart && (mVideoTrackIndex != 0 || mAudioTrackIndex != 0) && (mVideoList.size() > 0 || mAudioList.size() > 0)){
                    mMediaMuxer.start();
                    mIsStart = true;
                }

                ///< 将缓冲中数据推入封装器
                if(mIsStart){
                    _appendAudioBuffers();
                    _appendVideoBuffers();
                    mMediaMuxer.stop();
                }

                ///< 释放封装器实例
                mMediaMuxer.release();
                mMediaMuxer = null;
            }
        }catch (Exception e){
            Log.e(TAG, "stop release" + e);
        }
        ///< 清空相关缓存与标记位
        mVideoTrackIndex = -1;
        mAudioTrackIndex = -1;
        mIsStart = false;
        mVideoList.clear();
        mAudioList.clear();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean _hasAudioTrack() {
        return (mConfig.muxerType.value() & KFMediaBase.KFMediaType.KFMediaAudio.value()) != 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean _hasVideoTrack() {
        return (mConfig.muxerType.value() & KFMediaBase.KFMediaType.KFMediaVideo.value()) != 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void _setupMuxer(){
        ///< 初始化封装器
        if(mMediaMuxer == null){
            try {
                mMediaMuxer = new MediaMuxer(mConfig.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException e) {
                Log.e(TAG, "new MediaMuxer" + e);
                _callBackError(KFMuxerErrorCreate,e.getMessage());
                return;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void _setupVideoTrack() {
        ///< 根据外层输入格式描述初始化视频Track
        if(mVideoFormat != null){
            /// add video track
            try {
                mVideoTrackIndex = mMediaMuxer.addTrack(mVideoFormat);
            }catch (Exception e){
                Log.e(TAG, "addTrack" + e);
                _callBackError(KFMuxerErrorVideoAddTrack,e.getMessage());
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void _setupAudioTrack() {
        ///< 根据外层输入格式描述初始化音频Track
        if(mAudioFormat != null){
            /// add audio track
            try {
                mAudioTrackIndex = mMediaMuxer.addTrack(mAudioFormat);
            }catch (Exception e){
                Log.e(TAG, "addTrack" + e);
                _callBackError(KFMuxerErrorAudioAddTrack,e.getMessage());
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void _avInterleavedBuffers() {
        ///< 音视频交错，通过对比时间戳大小交错进入
        if(_hasVideoTrack() && _hasAudioTrack()){
            while (mAudioList.size() > 0 && mVideoList.size() > 0){
                KFBufferFrame audioPacket = mAudioList.get(0);
                KFBufferFrame videoPacket = mVideoList.get(0);

                if(audioPacket.bufferInfo.presentationTimeUs >= videoPacket.bufferInfo.presentationTimeUs){
                    mMediaMuxer.writeSampleData(mVideoTrackIndex,videoPacket.buffer,videoPacket.bufferInfo);
                    mVideoList.remove(0);
                }else{
                    mMediaMuxer.writeSampleData(mAudioTrackIndex,audioPacket.buffer,audioPacket.bufferInfo);
                    mAudioList.remove(0);
                }
            }
        }else if(_hasVideoTrack()){
            _appendVideoBuffers();
        }else if(_hasAudioTrack()){
            _appendAudioBuffers();
        }
    }

    private void _appendAudioBuffers() {
        ///< 音频队列缓冲区推到封装器
        while (mAudioList.size() > 0){
            KFBufferFrame packet = mAudioList.get(0);
            mMediaMuxer.writeSampleData(mAudioTrackIndex,packet.buffer,packet.bufferInfo);
            mAudioList.remove(0);
        }
    }

    private void _appendVideoBuffers() {
        ///< 视频队列缓冲区推到封装器
        while (mVideoList.size() > 0){
            KFBufferFrame packet = mVideoList.get(0);
            mMediaMuxer.writeSampleData(mVideoTrackIndex,packet.buffer,packet.bufferInfo);
            mVideoList.remove(0);
        }
    }

    private void _callBackError(int error, String errorMsg){
        ///< 错误回调
        if(mListener != null){
            mMainHandler.post(()->{
                mListener.muxerOnError(error,TAG + errorMsg);
            });
        }
    }
}
