package com.KeyFrameKit.AV.Render;

import java.nio.ByteBuffer;

public interface KFAudioRenderListener {
    ///< 出错回调
    void onError(int error,String errorMsg);
    ///< 获取PCM数据
    byte[] audioPCMData(int size);
}
