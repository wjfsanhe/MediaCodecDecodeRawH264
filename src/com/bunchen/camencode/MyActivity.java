package com.bunchen.camencode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;


/**   
*    
* 项目名称：Camcodec   
* 类名称：MyActivity   
* 类描述：   http://stackoverflow.com/questions/20760763/mediacodec-decoder-always-times-out-while-decoding-h264-file
* 创建人：刘新朋  
* 创建时间：2016-3-7 下午3:23:32   
* 修改人：mac   
* 修改时间：2016-3-7 下午3:23:32   
* 修改备注：   添加注释快捷键shift+alt+j  
* @version    
*    
*/
@SuppressLint("NewApi")
public class MyActivity extends Activity implements SurfaceHolder.Callback {

	private static final String filePath = Environment
			.getExternalStorageDirectory() + "/video1.h264"; // +
																// "/video_encoded.263";//"/video_encoded.264";
	private PlayerThread mPlayer = null;
	Handler handler = null;

	public static ArrayList<Frame> frames = null;
	public static int frameID = 0;
	public static boolean incompleteLastFrame = false;
	File encodedFile = new File(filePath);
	InputStream is;
	//这里sps和pps
	private byte[] header_sps;
	private byte[] header_pps;
	byte[] data ;
	
	private static class Frame {
		public int id;
		public byte[] frameData;

		public Frame(int id) {
			this.id = id;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			is = new FileInputStream(encodedFile);
			data = new byte[(int) encodedFile.length()];

			System.out.println("Total file size : " + encodedFile.length());
			frameID = 0;
			frames = new ArrayList<Frame>();

			try {
				if ((is.read(data, 0, (int) encodedFile.length())) != -1) {

					SurfaceView sv = new SurfaceView(this);
					handler = new Handler();
					sv.getHolder().addCallback(this);
					setContentView(sv);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	public static void getFramesFromData(byte[] data) {
		int dataLength = data.length;
		int frameLength = 0;
		frameID = 0;

		if (data.length <= 0)
			return;

		// each iteration in this loop indicates generation of a new frame
		for (int i = 0;;) {
			if (i + 3 >= dataLength)
				return;

			frameLength = ((data[i] & 0xff) << 24)
					+ ((data[i + 1] & 0xff) << 16)
					+ ((data[i + 2] & 0xff) << 8) + (data[i + 3] & 0xff);

			i += 4;

			if (frameLength > 0) {
				if (i + frameLength - 1 >= dataLength)
					return;
				Frame frame = new Frame(frameID);
				frame.frameData = new byte[frameLength];
				System.arraycopy(data, i, frame.frameData, 0, frameLength);
				frames.add(frame);
				frameID++;
				i += frameLength;
			}

		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("DecodeActivity", "in surfaceCreated");
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d("DecodeActivity", "in surfaceChanged");
		if (mPlayer == null) {
			Toast.makeText(getApplicationContext(),
					"in surfaceChanged. creating playerthread",
					Toast.LENGTH_SHORT).show();
			mPlayer = new PlayerThread(holder.getSurface());
			mPlayer.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mPlayer != null) {
			mPlayer.interrupt();
		}
	}

	private class PlayerThread extends Thread {
		// private MediaExtractor extractor;
		private MediaCodec decoder;
		private Surface surface;

		public PlayerThread(Surface surface) {
			this.surface = surface;
		}

		@Override
		public void run() {
			handler.post(new Runnable() {

				@Override
				public void run() {
					get_sps_pps();
					decoder = MediaCodec.createDecoderByType("video/avc");
					MediaFormat mediaFormat = MediaFormat.createVideoFormat(
							"video/avc", 1280, 720);
					mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1280*720);
					
					mediaFormat.setByteBuffer("csd-0",ByteBuffer.wrap(header_sps));
					mediaFormat.setByteBuffer("csd-1",ByteBuffer.wrap(header_pps));

					decoder.configure(mediaFormat, surface /* surface */,
							null /* crypto */, 0 /* flags */);

					if (decoder == null) {
						Log.e("DecodeActivity", "Can't find video info!");
						return;
					}

					decoder.start();
					Log.d("DecodeActivity", "decoder.start() called");

					ByteBuffer[] inputBuffers = decoder.getInputBuffers();
					ByteBuffer[] outputBuffers = decoder.getOutputBuffers();

					long startMs = System.currentTimeMillis();
					int i = 0;
					while (!Thread.interrupted()) {
						if (i >= frames.size())
							break;
						byte[] data = new byte[frames.get(i).frameData.length];
						System.arraycopy(frames.get(i).frameData, 0, data, 0,
								frames.get(i).frameData.length);
						Log.d("DecodeActivity", "i = " + i + " dataLength = "
								+ frames.get(i).frameData.length);

						int inIndex = 0;
						while ((inIndex = decoder.dequeueInputBuffer(1)) < 0);//判断解码器输入队列缓冲区有多少个buffer==，inIndex。
						
						if (inIndex >= 0) {
							ByteBuffer buffer = inputBuffers[inIndex];//取出解码器输入队列缓冲区最后一个buffer
							buffer.clear();
							int sampleSize = data.length;
							if (sampleSize < 0) {
								Log.d("DecodeActivity",
										"InputBuffer BUFFER_FLAG_END_OF_STREAM");
								decoder.queueInputBuffer(inIndex, 0, 0, 0,
										MediaCodec.BUFFER_FLAG_END_OF_STREAM);
								break;
							} else {
								Log.d("DecodeActivity", "sample size: "
										+ sampleSize);

//								buffer = ByteBuffer.allocate(data.length);
								buffer.clear();
								buffer.put(data);//向最后一个缓冲区inIndex中放入一帧数据。
								decoder.queueInputBuffer(inIndex, 0,
										sampleSize, 0, 0);//
							}

							BufferInfo info = new BufferInfo();
							int outIndex = decoder.dequeueOutputBuffer(info,
									100000);//出队列缓冲区的信息

							switch (outIndex) {
							case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
								Log.d("DecodeActivity",
										"INFO_OUTPUT_BUFFERS_CHANGED");
								outputBuffers = decoder.getOutputBuffers();
								break;
							case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
								Log.d("DecodeActivity",
										"New format "
												+ decoder.getOutputFormat());

								break;
							case MediaCodec.INFO_TRY_AGAIN_LATER:
								Log.d("DecodeActivity",
										"dequeueOutputBuffer timed out!");
								try {
									sleep(100);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								break;
							default:
								ByteBuffer outbuffer = outputBuffers[outIndex];

								Log.d("DecodeActivity",
										"We can't use this buffer but render it due to the API limit, "
												+ outbuffer);

								/*
								 * while (info.presentationTimeUs / 1000 >
								 * System.currentTimeMillis() - startMs) { try {
								 * sleep(10); } catch (InterruptedException e) {
								 * e.printStackTrace(); break; } }
								 */

								decoder.releaseOutputBuffer(outIndex, true);//冲缓冲区的出队列，然后解码显示到surfaceview上。
								break;
							}
							i++;
							// All decoded frames have been rendered, we can
							// stop playing now
							/*
							 * if ((info.flags &
							 * MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
							 * Log.d("DecodeActivity",
							 * "OutputBuffer BUFFER_FLAG_END_OF_STREAM"); break;
							 * }
							 */

						}
					}

					decoder.stop();
					decoder.release();
				}
			});
		}
	}
	
	/**
	 * 
	 */
	protected void get_sps_pps() {
		int dataLength = data.length;
		int i=0;
		
		while (i + 3 <= dataLength) {
			if(data[i]==0&&data[i+1]==0&&data[i+2]==0&&data[i+3]==1&&data[i+4]==0x67)
			{
				int j=i+4;
				while(j<dataLength-1&&!(data[j]==0&&data[j+1]==0&&data[j+2]==0&&data[j+3]==1))j++;
				header_sps = new byte[j-i];
				Arrays.fill(header_sps, (byte) 0);
				 System.arraycopy(data, i, header_sps, 0, j-i);
				 i = j;
				 continue;
			}
			
			if(data[i]==0&&data[i+1]==0&&data[i+2]==0&&data[i+3]==1&&data[i+4]==0x68)
			{
				int j=i+4;
				while(j<dataLength-1&&!(data[j]==0&&data[j+1]==0&&data[j+2]==0&&data[j+3]==1))j++;
				header_pps = new byte[j-i];
				Arrays.fill(header_pps, (byte) 0);
				 System.arraycopy(data, i, header_pps, 0, j-i);
				 i = j;
				 continue;
			}
			
			if(data[i]==0&&data[i+1]==0&&data[i+2]==0&&data[i+3]==1&&(data[i+4]==0x65||data[i+4]==0x41))
			{
				int j=i+4;
				while(j<dataLength-1&&!(data[j]==0&&data[j+1]==0&&data[j+2]==0&&data[j+3]==1))j++;
				int frameLength = j-i-4;
				Frame frame = new Frame(frameID);
				frame.frameData = new byte[frameLength];
				Arrays.fill(frame.frameData, (byte) 0);
				System.arraycopy(data, i, frame.frameData, 0, frameLength);
				frames.add(frame);
				frameID++;
				i = j;
				if(j==dataLength-1){
					Log.e("000", "99999999");
					break;
				}
				continue;
			}
			
			i++;
		}
	}
	
}