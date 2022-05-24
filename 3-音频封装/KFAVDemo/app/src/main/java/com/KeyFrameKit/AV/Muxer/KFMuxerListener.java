package com.KeyFrameKit.AV.Muxer;
//
//  KFMuxerListener
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
public interface KFMuxerListener {
    ///< 错误回调
    void muxerOnError(int error,String errorMsg);
}
