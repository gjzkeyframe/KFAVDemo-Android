package com.KeyFrameKit.AV.Base;

import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;

import static com.KeyFrameKit.AV.Base.KFFrame.KFFrameType.KFFrameBuffer;

//
//  KFBufferFrame
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
public class KFBufferFrame extends KFFrame {
    public ByteBuffer buffer;
    public MediaCodec.BufferInfo bufferInfo;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public KFBufferFrame() {
        super(KFFrameBuffer);
    }

    public KFFrameType frameType() {
        return KFFrameBuffer;
    }
}
