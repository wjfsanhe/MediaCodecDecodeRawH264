package com.bunchen.camencode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.bunchen.android.hardcodec.AvcDecoder;
import com.bunchen.android.hardcodec.AvcEncoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.ImageView;

public class TestActivity extends MainActivity {
	
	@Override
	public void onPreviewFrame(final byte[] data, Camera camera) {
		doCodec(data);
	}
	
	private void doCodec(byte[] data){
		int et = mEncoder.encode(data , 0 , data.length, buffer1, 0);
		Log.d("buncodec","encode et="+et);
		
		int dt = mDecoder.decode(buffer1, 0 , et , buffer2 , 0);
		Log.d("buncodec","decode dt="+dt);
		
		
		// 用YuvImage显示 NV21 数据 ，效率不高 ，但只是个demo 。
        YuvImage image = new YuvImage(buffer2,
                getPreviewFormat(), getPreviewWidth(), getPreviewHeight(),
                null);
        setData(image,getPreviewWidth(),getPreviewHeight());
        
        image = null ;
	}
	
	private byte[] buffer1 = new byte[1280*720*3/2];
	private byte[] buffer2 = new byte[1280*720*3/2];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mDataView = (ImageView) this.findViewById(R.id.dataview);
		
		int framerate = 15;
	    int bitrate = 1250000;
		mEncoder = new AvcEncoder(getPreviewWidth(),getPreviewHeight(),framerate,bitrate);
	
		mDecoder = new AvcDecoder(getPreviewWidth(),getPreviewHeight());
	}
	private ImageView mDataView ;
	private void setData(final YuvImage image , final int w , final int h){
		Bitmap bmp = null ;
		if(image!=null){
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			image.compressToJpeg(new Rect(0, 0, w, h), 80, stream);
			bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(bmp!=null){
		final Bitmap bbb = bmp ;
			mDataView.setImageBitmap(bbb);
		}
		bmp = null ;
		
	}
	

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mEncoder.close();
		mDecoder.close();
	}



	private AvcEncoder mEncoder ;
	private AvcDecoder mDecoder ;

}
