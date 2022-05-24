package com.KeyFrameKit.AV.Demuxer;
//
//  KFDemuxerConfig
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//

import com.KeyFrameKit.AV.Base.KFMediaBase;

public class KFDemuxerConfig {
    ///< 输入路径
    public String path;
    ///< 音视频解封装类型（仅音频 仅视频 音视频）
    public KFMediaBase.KFMediaType demuxerType = KFMediaBase.KFMediaType.KFMediaAV;
}
