package com.bunchen.android.hardcodec;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

@SuppressLint("NewApi")
public class AvcEncoder {

	public AvcEncoder(int width, int height, int framerate, int bitrate) {
		mWidth = width;
		mHeight = height;
		mFramerate = framerate;
		mBitrate = bitrate;
		init();
	}
	
	private MediaCodec mMediaCodec;
	private int mWidth ;
	private int mHeight ;
	private int mFramerate;
	private int mBitrate;
	private void init()
	{
		mMediaCodec = MediaCodec.createEncoderByType("video/avc");
	    MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
	    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
	    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate);
	    mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
	    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
	    mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
	    mMediaCodec.start();
	    
	}
	
	public void close(){
		try {
			mMediaCodec.stop();
			mMediaCodec.release();
			mMediaCodec=null;
	    } catch (Exception e){ 
	        e.printStackTrace();
	    }
	}
	
	public int encode(byte[] input , int offset , int count , byte[] output , int out_offset) 
	{	
		int len = 0;
		byte[] yuv420  = input ;
		ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
	    ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
	    int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
	    if (inputBufferIndex >= 0) {
	        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	        inputBuffer.clear();
	        inputBuffer.put(yuv420, offset, count);
	        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, count, 0, 0);
	    }
	    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
	    int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,1000000l);  // 1000000 us timeout , one second
	    if (outputBufferIndex >= 0) {
	        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
	        outputBuffer.get(output, out_offset, bufferInfo.size);
	        len = bufferInfo.size ;
	        mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
	    } 
	    return len;
	}

}
