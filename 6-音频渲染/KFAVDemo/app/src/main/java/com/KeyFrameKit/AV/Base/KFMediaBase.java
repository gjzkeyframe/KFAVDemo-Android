package com.KeyFrameKit.AV.Base;
//
//  KFMediaBase
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//

public class KFMediaBase {
    public enum KFMediaType{
        KFMediaUnkown(0),
        KFMediaAudio (1 << 0),
        KFMediaVideo  (1 << 1),
        KFMediaAV ((1 << 0) | (1 << 1));
        private int index;
        KFMediaType(int index) {
            this.index = index;
        }

        public int value() {
            return index;
        }
    }
}
