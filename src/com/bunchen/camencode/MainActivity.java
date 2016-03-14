package com.bunchen.camencode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.bunchen.android.hardcodec.AvcDecoder;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.SurfaceHolder.Callback;
import android.os.Build;

public class MainActivity extends Activity implements SurfaceHolder.Callback,
		PreviewCallback, Runnable {
	private SurfaceView mPreview;
	private int preview_width;
	private int preview_height;
	private int preview_format;
	private SurfaceHolder mHolder;
	private Rect rect = null;
	private String TAG = MainActivity.class.getSimpleName();
	private Bitmap bmp;
	private Thread mainLoop;
	private AvcDecoder mDecoder;
	private byte[] buffer = new byte[1280 * 720 * 3 / 2];
	private String H264FILE = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		preview_width = 1280;
		preview_height = 720;
		preview_format = ImageFormat.NV21;
		mPreview = (SurfaceView) this.findViewById(R.id.preview);
		mHolder = mPreview.getHolder();

		mHolder.setFixedSize(preview_width, preview_height); // 预览大小設置
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHolder.addCallback(this);
	}

	@Override
	public void run() {

		while (true) {
			if (rect == null) {
				rect = new Rect(0, 0, this.preview_width, this.preview_height);
			}

			get_bitmap();
			Canvas canvas = mHolder.lockCanvas();
			if (canvas != null) {
				canvas.drawBitmap(bmp, null, rect, null);
				mHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	private void readH264FromFile() {

		File file = new File(H264FILE);
		if (!file.exists() || !file.canRead()) {
			Log.e(TAG, "failed to open h264 file.");
			return;
		}

		try {
			int len = 0;
			FileInputStream fis = new FileInputStream(file);
			byte[] buf = new byte[1024];
			while ((len = fis.read(buf)) > 0) {
				// offerDecoder(buf, len);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return;
	}

	/**
	 * 
	 */
	private void get_bitmap() {
		mDecoder.decode(read_frame(), 0, 0, buffer, 0);
	}

	/**
	 * 
	 */
	private byte[] read_frame() {

		return null;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (bmp == null) {
			bmp = Bitmap.createBitmap(this.preview_width, this.preview_width,
					Bitmap.Config.ARGB_8888);
		}
		mainLoop = new Thread(this);
		mainLoop.start();

		mDecoder = new AvcDecoder(getPreviewWidth(), getPreviewHeight());

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		Log.i(TAG, "format=" + format + ",,width=" + width + ",,height="
				+ height);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "surfaceDestroyedsurfaceDestroyed");
		mDecoder.close();
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onPreviewFrameonPreviewFrame");
	}

	public int getPreviewWidth() {
		return this.preview_width;
	}

	public int getPreviewHeight() {
		return this.preview_height;
	}

	public int getPreviewFormat() {
		return preview_format;
	}

}
