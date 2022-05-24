package com.KeyFrameKit.AV.Muxer;
//
//  KFMuxerConfig
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.KeyFrameKit.AV.Base.KFMediaBase;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class KFMuxerConfig {
    ///< 输出路径
    public String outputPath = null;
    ///< 封装仅音频、仅视频、音视频
    public KFMediaBase.KFMediaType muxerType = KFMediaBase.KFMediaType.KFMediaAV;

    public KFMuxerConfig(String path){
        outputPath = path;
    }
}
