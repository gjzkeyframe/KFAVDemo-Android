package com.KeyFrameKit.AV.MediaCodec;
//
//  KFByteBufferCodec
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.KeyFrameKit.AV.Base.KFBufferFrame;
import com.KeyFrameKit.AV.Base.KFFrame;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecInterface;
import com.KeyFrameKit.AV.MediaCodec.KFMediaCodecListener;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class KFByteBufferCodec implements KFMediaCodecInterface {
    public static final int KFByteBufferCodecErrorParams = -2500;
    public static final int KFByteBufferCodecErrorCreate = -2501;
    public static final int KFByteBufferCodecErrorConfigure = -2502;
    public static final int KFByteBufferCodecErrorStart = -2503;

    private static final int KFByteBufferCodecInputBufferMaxCache = 20 * 1024 * 1024;
    private static final String TAG = "KFByteBufferCodec";
    private KFMediaCodecListener mListener = null;///< 回调
    private MediaCodec mMediaCodec = null;///< Codec实例
    private ByteBuffer[] mInputBuffers;///<  Codec输入缓冲区
    private MediaFormat mInputMediaFormat = null;///< 输入数据格式描述
    private MediaFormat mOutMediaFormat = null;///< 输出数据格式描述

    private long mLastInputPts = 0;///< 上一帧时间戳
    private List<KFBufferFrame> mList = new ArrayList<>();///< 输入数据缓存
    private int mListCacheSize = 0;///< 输入数据缓存数量
    private ReentrantLock mListLock = new ReentrantLock(true);///< 数据缓存锁
    private boolean mIsEncoder = true;

    private HandlerThread mCodecThread = null;///<  Codec线程
    private Handler mCodecHandler = null;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());///< 主线程

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void setup(boolean isEncoder,MediaFormat mediaFormat, KFMediaCodecListener listener, EGLContext eglShareContext) {
        mListener = listener;
        mInputMediaFormat = mediaFormat;
        mIsEncoder = isEncoder;

        mCodecThread = new HandlerThread("KFByteBufferCodecThread");
        mCodecThread.start();
        mCodecHandler = new Handler((mCodecThread.getLooper()));

        mCodecHandler.post(()->{
            if(mInputMediaFormat == null){
                _callBackError(KFByteBufferCodecErrorParams,"mInputMediaFormat null");
                return;
            }
            ///< 初始化 Codec 实例
            _setupCodec();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void  release() {
        ///< 释放 Codec 实例、输入缓存
        mCodecHandler.post(()-> {
            if(mMediaCodec != null){
                try {
                    mMediaCodec.stop();
                    mMediaCodec.release();
                } catch (Exception e) {
                    Log.e(TAG, "release: " + e.toString());
                }
                mMediaCodec = null;
            }

            mListLock.lock();
            mList.clear();
            mListCacheSize = 0;
            mListLock.unlock();

            mCodecThread.quit();
        });
    }

    @Override
    public MediaFormat getOutputMediaFormat() {
        return mOutMediaFormat;
    }

    @Override
    public MediaFormat getInputMediaFormat() {
        return mInputMediaFormat;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int processFrame(KFFrame inputFrame) {
        ///< 处理输入帧数据
        if(inputFrame == null){
            return KFMediaCodeProcessParams;
        }

        KFBufferFrame frame = (KFBufferFrame)inputFrame;
        if(frame.buffer ==null || frame.bufferInfo == null || frame.bufferInfo.size == 0){
            return KFMediaCodeProcessParams;
        }

        ///< 先添加到缓冲区，一旦缓冲区满则返回 KFMediaCodeProcessAgainLater
        boolean appendSuccess = _appendFrame(frame);
        if(!appendSuccess){
            return KFMediaCodeProcessAgainLater;
        }

        mCodecHandler.post(()-> {
            if(mMediaCodec == null){
                return;
            }

            ///< 子线程处理编解码，从队列取出一组数据，能塞多少塞多少数据
            mListLock.lock();
            int mListSize = mList.size();
            mListLock.unlock();
            while (mListSize > 0){
                mListLock.lock();
                KFBufferFrame packet = mList.get(0);
                mListLock.unlock();

                int bufferIndex;
                try {
                    bufferIndex = mMediaCodec.dequeueInputBuffer(10 * 1000);
                } catch (Exception e) {
                    Log.e(TAG, "dequeueInputBuffer" + e);
                    return;
                }

                if(bufferIndex >= 0){
                    mInputBuffers[bufferIndex].clear();
                    mInputBuffers[bufferIndex].put(packet.buffer);
                    mInputBuffers[bufferIndex].flip();
                    try {
                        mMediaCodec.queueInputBuffer(bufferIndex, 0, packet.bufferInfo.size, packet.bufferInfo.presentationTimeUs, packet.bufferInfo.flags);
                    } catch (Exception e) {
                        Log.e(TAG, "queueInputBuffer" + e);
                        return;
                    }

                    mLastInputPts = packet.bufferInfo.presentationTimeUs;
                    mListLock.lock();
                    mList.remove(0);
                    mListSize = mList.size();
                    mListCacheSize -= packet.bufferInfo.size;
                    mListLock.unlock();
                }else{
                    break;
                }
            }

            ///< 获取Codec后的数据，一样策略，尽量拿出最多的数据出来，回调给外层
            long outputDts = -1;
            MediaCodec.BufferInfo outputBufferInfo = new MediaCodec.BufferInfo();
            while (outputDts < mLastInputPts) {
                int bufferIndex;
                try {
                    bufferIndex = mMediaCodec.dequeueOutputBuffer(outputBufferInfo, 10 * 1000);
                } catch (Exception e) {
                    Log.e(TAG, "dequeueOutputBuffer" + e);
                    return;
                }

                if(bufferIndex >= 0){
                    ByteBuffer decodeBuffer = mMediaCodec.getOutputBuffer(bufferIndex);
                    if(mListener != null){
                        KFBufferFrame bufferFrame = new KFBufferFrame(decodeBuffer,outputBufferInfo);
                        mListener.dataOnAvailable(bufferFrame);
                    }
                    mMediaCodec.releaseOutputBuffer(bufferIndex,true);
                }else{
                    if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mOutMediaFormat = mMediaCodec.getOutputFormat();
                    }
                    break;
                }
            }
        });

        return KFMediaCodeProcessSuccess;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void flush() {
        ///< Codec 清空缓冲区，一般用于Seek、结束时时使用
        mCodecHandler.post(()-> {
            if(mMediaCodec == null){
                return;
            }

            try {
                mMediaCodec.flush();
            } catch (Exception e) {
                Log.e(TAG, "flush" + e);
            }

            mListLock.lock();
            mList.clear();
            mListCacheSize = 0;
            mListLock.unlock();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean _appendFrame(KFBufferFrame frame) {
        ///< 将输入数据添加至缓冲区
        mListLock.lock();
        int cacheSize = mListCacheSize;
        mListLock.unlock();
        if(cacheSize >= KFByteBufferCodecInputBufferMaxCache){
            return false;
        }

        KFBufferFrame packet = new KFBufferFrame();

        ByteBuffer newBuffer = ByteBuffer.allocateDirect(frame.bufferInfo.size);
        newBuffer.put(frame.buffer).position(0);
        MediaCodec.BufferInfo newInfo = new MediaCodec.BufferInfo();
        newInfo.size = frame.bufferInfo.size;
        newInfo.flags = frame.bufferInfo.flags;
        newInfo.presentationTimeUs = frame.bufferInfo.presentationTimeUs;
        packet.buffer = newBuffer;
        packet.bufferInfo = newInfo;

        mListLock.lock();
        mList.add(packet);
        mListCacheSize += packet.bufferInfo.size;
        mListLock.unlock();

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean _setupCodec() {
        ///< 初始化Codec 模块，支持编码、解码，根据不同MediaFormat 创建不同Codec
        try {
            String mimetype = mInputMediaFormat.getString(MediaFormat.KEY_MIME);
            if(mIsEncoder){
                mMediaCodec = MediaCodec.createEncoderByType(mimetype);
            }else{
                mMediaCodec = MediaCodec.createDecoderByType(mimetype);
            }

        }catch (Exception e) {
            Log.e(TAG, "createCodecByType" + e + mIsEncoder);
            _callBackError(KFByteBufferCodecErrorCreate,e.getMessage());
            return false;
        }

        try {
            mMediaCodec.configure(mInputMediaFormat, null, null, mIsEncoder ? MediaCodec.CONFIGURE_FLAG_ENCODE : 0);
        }catch (Exception e) {
            Log.e(TAG, "configure" + e);
            _callBackError(KFByteBufferCodecErrorConfigure,e.getMessage());
            return false;
        }

        try {
            mMediaCodec.start();
            mInputBuffers = mMediaCodec.getInputBuffers();
        }catch (Exception e) {
            Log.e(TAG, "start" +  e );
            _callBackError(KFByteBufferCodecErrorStart,e.getMessage());
            return false;
        }

        return true;
    }

    private void _callBackError(int error, String errorMsg){
        if(mListener != null){
            mMainHandler.post(()->{
                mListener.onError(error,TAG + errorMsg);
            });
        }
    }
}
