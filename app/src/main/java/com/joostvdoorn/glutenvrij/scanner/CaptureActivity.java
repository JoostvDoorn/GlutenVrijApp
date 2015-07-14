/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.joostvdoorn.glutenvrij.scanner;

import com.joostvdoorn.glutenvrij.TrackedActivity;
import com.joostvdoorn.glutenvrij.scanner.PreferencesActivity;
import com.joostvdoorn.glutenvrij.GlutenvrijActivity;
import com.joostvdoorn.glutenvrij.R;
import com.joostvdoorn.glutenvrij.Search;
import com.joostvdoorn.glutenvrij.SearchActivity;
import com.joostvdoorn.glutenvrij.SearchObserver;
import com.joostvdoorn.glutenvrij.SearchResult;
import com.joostvdoorn.glutenvrij.scanner.camera.CameraManager;
import com.joostvdoorn.glutenvrij.scanner.core.BarcodeFormat;
import com.joostvdoorn.glutenvrij.scanner.core.Result;
import com.joostvdoorn.glutenvrij.scanner.core.ResultMetadataType;
import com.joostvdoorn.glutenvrij.scanner.core.ResultPoint;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * The barcode reader activity itself. This is loosely based on the CameraPreview
 * example included in the Android SDK.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends TrackedActivity implements SurfaceHolder.Callback, SearchObserver {

  protected static final String NAME = "Barcode scanner";
  private static final String TAG = CaptureActivity.class.getSimpleName();

  private static final int SETTINGS_ID = Menu.FIRST;
  private static final int ABOUT_ID = Menu.FIRST + 1;

  private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;


  private static final Set<ResultMetadataType> DISPLAYABLE_METADATA_TYPES;
  static {
    DISPLAYABLE_METADATA_TYPES = new HashSet<ResultMetadataType>(5);
    DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.ISSUE_NUMBER);
    DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.SUGGESTED_PRICE);
    DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.ERROR_CORRECTION_LEVEL);
    DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.POSSIBLE_COUNTRY);
  }

  private enum Source {
    NATIVE_APP_INTENT,
    PRODUCT_SEARCH_LINK,
    ZXING_LINK,
    NONE
  }

  private CaptureActivityHandler handler;
  private ViewfinderView viewfinderView;
  private TextView statusView;
  private Result lastResult;
  private boolean hasSurface;
  private Source source;
  private Vector<BarcodeFormat> decodeFormats;
  private String characterSet;
  private InactivityTimer inactivityTimer;
  private BeepManager beepManager;

  ViewfinderView getViewfinderView() {
    return viewfinderView;
  }

  public Handler getHandler() {
    return handler;
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle, this.NAME);

    Window window = getWindow();
    window.setFormat(PixelFormat.RGBA_8888);
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.capture);

    CameraManager.init(getApplication());
    viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    statusView = (TextView) findViewById(R.id.status_view);
    handler = null;
    lastResult = null;
    hasSurface = false;
    inactivityTimer = new InactivityTimer(this);
    beepManager = new BeepManager(this);

  }

  @Override
  protected void onResume() {
    super.onResume();
    resetStatusView();

    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

	  source = Source.NONE;
	  decodeFormats = null;
	  characterSet = null;



    beepManager.updatePrefs();

    inactivityTimer.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (handler != null) {
      handler.quitSynchronously();
      handler = null;
    }
    inactivityTimer.onPause();
    CameraManager.get().closeDriver();
  }

  @Override
  protected void onDestroy() {
    inactivityTimer.shutdown();
    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (source == Source.NATIVE_APP_INTENT) {
        setResult(RESULT_CANCELED);
        finish();
        return true;
      } else if ((source == Source.NONE || source == Source.ZXING_LINK) && lastResult != null) {
        resetStatusView();
        if (handler != null) {
          handler.sendEmptyMessage(R.id.restart_preview);
        }
        return true;
      }
    } else if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
      // Handle these events so they don't launch the Camera app
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, SETTINGS_ID, 0, R.string.menu_settings)
    .setIcon(android.R.drawable.ic_menu_preferences);
    menu.add(0, ABOUT_ID, 0, R.string.menu_about)
    .setIcon(android.R.drawable.ic_menu_info_details);
    return true;
  }



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	  switch (item.getItemId()) {
	    case SETTINGS_ID: {
	      Intent intent = new Intent(Intent.ACTION_VIEW);
	      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	      intent.setClassName(this, PreferencesActivity.class.getName());
	      startActivity(intent);
	      break;
	    }
	    case ABOUT_ID:
	      AlertDialog.Builder builder = new AlertDialog.Builder(this);
	      builder.setTitle(getString(R.string.title_about) + GlutenvrijActivity.VERSION_NAME);
	      builder.setMessage(getString(R.string.msg_about) + "\n" + getString(R.string.zxing_url));
	      builder.show();
	      break;
	  }
	  return super.onOptionsItemSelected(item);
	}

  public void surfaceCreated(SurfaceHolder holder) {
    if (!hasSurface) {
      hasSurface = true;
      initCamera(holder);
    }
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

  /**
   * A valid barcode has been found, so give an indication of success and show the results.
   *
   * @param rawResult The contents of the barcode.
   * @param barcode   A greyscale bitmap of the camera data which was decoded.
   */
  public void handleDecode(Result rawResult, Bitmap barcode) {
    inactivityTimer.onActivity();
    lastResult = rawResult;
    beepManager.playBeepSoundAndVibrate();
    this.trackEvent("scanner","search",""+rawResult.getText(), 0);
    Toast.makeText(this, ""+rawResult.getText(), Toast.LENGTH_SHORT).show();

    new Search().execute(1,rawResult.getText(),this);
    // Wait a moment or else it will scan the same barcode continuously about 3 times
    if (handler != null) {
      handler.sendEmptyMessageDelayed(R.id.restart_preview, BULK_MODE_SCAN_DELAY_MS);
    }
    drawResultPoints(barcode, rawResult);
    resetStatusView();

    }

  /**
   * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
   *
   * @param barcode   A bitmap of the captured image.
   * @param rawResult The decoded results which contains the points to draw.
   */
  private void drawResultPoints(Bitmap barcode, Result rawResult) {
    ResultPoint[] points = rawResult.getResultPoints();
    if (points != null && points.length > 0) {
      Canvas canvas = new Canvas(barcode);
      Paint paint = new Paint();
      paint.setColor(getResources().getColor(R.color.result_image_border));
      paint.setStrokeWidth(3.0f);
      paint.setStyle(Paint.Style.STROKE);
      Rect border = new Rect(2, 2, barcode.getWidth() - 2, barcode.getHeight() - 2);
      canvas.drawRect(border, paint);

      paint.setColor(getResources().getColor(R.color.result_points));
      if (points.length == 2) {
        paint.setStrokeWidth(4.0f);
        drawLine(canvas, paint, points[0], points[1]);
      } else if (points.length == 4 &&
                 (rawResult.getBarcodeFormat().equals(BarcodeFormat.UPC_A) ||
                  rawResult.getBarcodeFormat().equals(BarcodeFormat.EAN_13))) {
        // Hacky special case -- draw two lines, for the barcode and metadata
        drawLine(canvas, paint, points[0], points[1]);
        drawLine(canvas, paint, points[2], points[3]);
      } else {
        paint.setStrokeWidth(10.0f);
        for (ResultPoint point : points) {
          canvas.drawPoint(point.getX(), point.getY(), paint);
        }
      }
    }
  }

  private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b) {
    canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
  }

  
  private void initCamera(SurfaceHolder surfaceHolder) {
    try {
      CameraManager.get().openDriver(surfaceHolder);
      // Creating the handler starts the preview, which can also throw a RuntimeException.
      if (handler == null) {
        handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
      }
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      displayFrameworkBugMessageAndExit();
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      Log.w(TAG, "Unexpected error initializating camera", e);
      displayFrameworkBugMessageAndExit();
    }
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.app_name));
    builder.setMessage(getString(R.string.msg_camera_framework_bug));
    builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
    builder.setOnCancelListener(new FinishListener(this));
    builder.show();
  }

  private void resetStatusView() {
    statusView.setText(R.string.msg_default_status);
    statusView.setVisibility(View.VISIBLE);
    viewfinderView.setVisibility(View.VISIBLE);
    lastResult = null;
  }

  public void drawViewfinder() {
    viewfinderView.drawViewfinder();
  }


@Override
public void notify(ArrayList<SearchResult> result) {
	if(result.size()>1) {
	    this.trackEvent("scanner","result-multi",result.get(0).getEan(), result.size());
		//Show an more advance interface for multiple results
    	Intent myIntent = new Intent(getBaseContext(), SearchActivity.class);
    	myIntent.putExtra("com.joostvdoorn.glutenvrij.SearchValue", result.get(0).getEan());
    	myIntent.putExtra("com.joostvdoorn.glutenvrij.SearchCategory", Integer.toString(1));
    	startActivity(myIntent);
	}
	else if(result.size()==1) {
		findViewById(R.id.scannerResultContainer).setVisibility(View.VISIBLE);
		if(result.get(0).allowed(PreferenceManager.getDefaultSharedPreferences(this)) == SearchResult.FREE_FROM) {
			findViewById(R.id.scannerResultContainer).setBackgroundResource(R.drawable.green_gradient);
		}
		else if(result.get(0).allowed(PreferenceManager.getDefaultSharedPreferences(this)) == SearchResult.CONTAINS) {
			findViewById(R.id.scannerResultContainer).setBackgroundResource(R.drawable.red_gradient);
		}
		else {
			findViewById(R.id.scannerResultContainer).setBackgroundResource(R.drawable.grey_gradient);
		}
	    this.trackEvent("scanner","result",""+result.get(0).getName(), 1);
		((TextView) findViewById(R.id.scannerResult)).setText(result.get(0).getName());
		String line = "";
		line += result.get(0).getStore() + " - ";
		line += result.get(0).getEan() + " - ";
		Resources res = getResources();
		switch(result.get(0).getLactoseInfo()) {
			case SearchResult.FREE_FROM:
				line += res.getString(R.string.lactosefree) + " - ";
				break;
			case SearchResult.CONTAINS:
				line += res.getString(R.string.lactose_contains) + " - ";
				break;
			case SearchResult.NO_DATA:
				line += res.getString(R.string.lactose_no_data) + " - ";
				break;
		}
		switch(result.get(0).getStarchInfo()) {
			case SearchResult.FREE_FROM:
				line += res.getString(R.string.starchfree);
				break;
			case SearchResult.CONTAINS:
				line += res.getString(R.string.starch_contains);
				break;
			case SearchResult.NO_DATA:
				line += res.getString(R.string.starch_no_data);
				break;
		}
		((TextView) findViewById(R.id.resultInfoLine)).setText(line);
	}
	else {
	    this.trackEvent("scanner","result-none","", 0);
		findViewById(R.id.scannerResultContainer).setVisibility(View.VISIBLE);
		findViewById(R.id.scannerResultContainer).setBackgroundResource(R.drawable.yellow_gradient);
		((TextView) findViewById(R.id.scannerResult)).setText(R.string.barcode_not_found);
		((TextView) findViewById(R.id.resultInfoLine)).setText("");
	}
}
}
