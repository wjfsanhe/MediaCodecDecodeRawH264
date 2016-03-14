package com.bunchen.android.hardcodec;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;

@SuppressLint("NewApi")
public class AvcDecoder {

	public AvcDecoder(int width, int height) {
		mWidth = width;
		mHeight = height;
		init();
	}
	
	private MediaCodec mMediaCodec;
	private int mWidth ;
	private int mHeight ;
	private void init()
	{
		mMediaCodec = MediaCodec.createDecoderByType("video/avc");
	    MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
	    mMediaCodec.configure(mediaFormat, null, null, 0);
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
	
	public int decode(byte[] input , int offset , int count , byte[] output , int out_offset) 
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
