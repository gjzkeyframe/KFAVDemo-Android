package com.KeyFrameKit.AV.MediaCodec;
//
//  KFMediaCodecInterface
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.media.MediaFormat;
import android.opengl.EGLContext;

import com.KeyFrameKit.AV.Base.KFFrame;


public interface KFMediaCodecInterface {
    public static final int KFMediaCodecInterfaceErrorCreate = -2000;
    public static final int KFMediaCodecInterfaceErrorConfigure = -2001;
    public static final int KFMediaCodecInterfaceErrorStart = -2002;
    public static final int KFMediaCodecInterfaceErrorDequeueOutputBuffer = -2003;
    public static final int KFMediaCodecInterfaceErrorParams = -2004;

    public static int KFMediaCodeProcessParams = -1;
    public static int KFMediaCodeProcessAgainLater = -2;
    public static int KFMediaCodeProcessSuccess = 0;

    ///< 初始化Codec,第一个参数需告知使用编码还是解码
    public void setup(boolean isEncoder,MediaFormat mediaFormat, KFMediaCodecListener listener, EGLContext eglShareContext);
    ///< 释放Codec
    public void release();

    ///< 获取输出格式描述
    public MediaFormat getOutputMediaFormat();
    ///< 获取输入格式描述
    public MediaFormat getInputMediaFormat();
    ///< 处理每一帧数据，编码前与编码后都可以，支持编解码2种模式
    public int processFrame(KFFrame frame);
    ///< 清空 Codec 缓冲区
    public void flush();
}
