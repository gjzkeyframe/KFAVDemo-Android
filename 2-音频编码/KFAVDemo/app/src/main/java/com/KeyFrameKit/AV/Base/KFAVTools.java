package com.KeyFrameKit.AV.Base;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.util.Size;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

//
//  KFAVTools
//  KFAVDemo
//  微信搜索『gzjkeyframe』关注公众号『关键帧Keyframe』获得最新音视频技术文章和进群交流。
//  Created by [公众号：关键帧Keyframe] on 2021/12/28.
//
public class KFAVTools {

    // 按音频参数生产 AAC packet 对应的 ADTS 头数据。
    // 当编码器编码的是 AAC 裸流数据时，需要在每个 AAC packet 前添加一个 ADTS 头用于解码器解码音频流。
    // 参考文档：
    // ADTS 格式参考：http://wiki.multimedia.cx/index.php?title=ADTS
    // MPEG-4 Audio 格式参考：http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio#Channel_Configurations
    public static ByteBuffer getADTS(int size, int profile, int sampleRate, int channel) {
        int sampleRateIndex = getSampleRateIndex(sampleRate);// 取得采样率对应的 index。
        int fullSize = 7 + size;
        // ADTS 头固定 7 字节。
        // 填充 ADTS 数据。
        ByteBuffer adtsBuffer = ByteBuffer.allocateDirect(7);
        adtsBuffer.order(ByteOrder.nativeOrder());
        adtsBuffer.put((byte)0xFF);// 11111111     = syncword
        adtsBuffer.put((byte)0xF1);
        adtsBuffer.put((byte)(((profile - 1) << 6) + (sampleRateIndex << 2) + (channel >> 2)));
        adtsBuffer.put((byte)(((channel & 3) << 6) + (fullSize >> 11)));
        adtsBuffer.put((byte)((fullSize & 0x7FF) >> 3));
        adtsBuffer.put((byte)(((fullSize & 7) << 5) + 0x1F));
        adtsBuffer.put((byte)0xFC);
        adtsBuffer.position(0);

        return adtsBuffer;
    }

    private  static int getSampleRateIndex(int sampleRate) {
        int sampleRateIndex = 0;
        switch (sampleRate) {
            case 96000:
                sampleRateIndex = 0;
                break;
            case 88200:
                sampleRateIndex = 1;
                break;
            case 64000:
                sampleRateIndex = 2;
                break;
            case 48000:
                sampleRateIndex = 3;
                break;
            case 44100:
                sampleRateIndex = 4;
                break;
            case 32000:
                sampleRateIndex = 5;
                break;
            case 24000:
                sampleRateIndex = 6;
                break;
            case 22050:
                sampleRateIndex = 7;
                break;
            case 16000:
                sampleRateIndex = 8;
                break;
            case 12000:
                sampleRateIndex = 9;
                break;
            case 11025:
                sampleRateIndex = 10;
                break;
            case 8000:
                sampleRateIndex = 11;
                break;
            case 7350:
                sampleRateIndex = 12;
                break;
            default:
                sampleRateIndex = 15;
        }
        return sampleRateIndex;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static MediaFormat createVideoFormat(boolean isHEVC, Size size,int format,int bitrate,int fps,int gopDuration,int profile,int profileLevel){
        String mimeType = isHEVC ? "video/hevc" : "video/avc";
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, size.getWidth(), size.getHeight());

        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, format);//MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, gopDuration);
        mediaFormat.setInteger(MediaFormat.KEY_PROFILE, profile);
        mediaFormat.setInteger(MediaFormat.KEY_LEVEL, profileLevel);

        return mediaFormat;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static MediaFormat createAudioFormat(int sampleRate, int channel, int bitrate){
        String mimeType = MediaFormat.MIMETYPE_AUDIO_AAC;
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, channel);

        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channel);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        return mediaFormat;
    }
}
