package com.KeyFrameKit.AV.MediaCodec;
//
//  KFMediaCodecListener
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.media.MediaCodec;

import com.KeyFrameKit.AV.Base.KFBufferFrame;
import com.KeyFrameKit.AV.Base.KFFrame;

import java.nio.ByteBuffer;

public interface KFMediaCodecListener {
    void onError(int error,String errorMsg);
    void dataOnAvailable(KFFrame frame);
}
