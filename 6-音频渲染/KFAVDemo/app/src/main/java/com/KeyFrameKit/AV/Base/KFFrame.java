package com.KeyFrameKit.AV.Base;
//
//  KFFrame
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class KFFrame {
    public enum KFFrameType{
        KFFrameBuffer,
        KFFrameTexture;
    }

    public KFFrameType frameType = KFFrameType.KFFrameBuffer;
    public KFFrame(KFFrameType type){
        frameType = type;
    }
}
