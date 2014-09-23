/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.projecttango.areadescriptionnative;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class AreaDescriptionActivity extends Activity implements
		View.OnClickListener {
	GLSurfaceView glView;
	RelativeLayout layout;

	TextView eventString;
	TextView versionString;
	TextView device2StartText;
	TextView device2ADFText;
	TextView start2ADFText;

	TextView learningModeText;
	TextView uuidText;

	Button saveADFButton;
	Button startButton;
	Button firstPersonCamButton;
	Button thirdPersonCamButton;
	Button topDownCamButton;

	ToggleButton isUsingADFToggleButton;
	ToggleButton isLearningToggleButton;

	boolean isUsingADF = false;
	boolean isLearning = false;

	private float[] touchStartPos = new float[2];
	private float[] touchCurPos = new float[2];
	private float touchStartDist = 0.0f;
	private float touchCurDist = 0.0f;
	private Point screenSize = new Point();
	private float screenDiagnal = 0.0f;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TangoJNINative.Initialize();

		Display display = getWindowManager().getDefaultDisplay();
		display.getSize(screenSize);
		screenDiagnal = (float) Math.sqrt(screenSize.x * screenSize.x
				+ screenSize.y * screenSize.y);
		
		setContentView(R.layout.activity_area_description);
		glView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
		glView.setRenderer(new Renderer());

		eventString = (TextView) findViewById(R.id.tangoevent);
		versionString = (TextView) findViewById(R.id.version);
		device2StartText = (TextView) findViewById(R.id.device_start);
		device2ADFText = (TextView) findViewById(R.id.adf_device);
		start2ADFText = (TextView) findViewById(R.id.adf_start);
		learningModeText = (TextView) findViewById(R.id.learningmode);
		uuidText = (TextView) findViewById(R.id.uuid);

		Intent intent = getIntent();
		isLearning = intent.getBooleanExtra(StartActivity.USE_AREA_LEARNING,
				false);
		isUsingADF = intent.getBooleanExtra(StartActivity.LOAD_ADF, false);

		saveADFButton = (Button) findViewById(R.id.saveAdf);
		saveADFButton.setOnClickListener(this);
		firstPersonCamButton = (Button) findViewById(R.id.first_person_button);
		firstPersonCamButton.setOnClickListener(this);
		thirdPersonCamButton = (Button) findViewById(R.id.third_person_button);
		thirdPersonCamButton.setOnClickListener(this);
		topDownCamButton = (Button) findViewById(R.id.top_down_button);
		topDownCamButton.setOnClickListener(this);

		if (!isLearning) {
			saveADFButton.setVisibility(View.GONE);
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(100);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								try {
									updateUIs();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		TangoJNINative.SetupConfig(isLearning, isUsingADF);
		TangoJNINative.Connect();
	}

	@Override
	protected void onPause() {
		super.onPause();
		TangoJNINative.Disconnect();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		TangoJNINative.OnDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.first_person_button:
			TangoJNINative.SetCamera(0);
			break;
		case R.id.top_down_button:
			TangoJNINative.SetCamera(2);
			break;
		case R.id.third_person_button:
			TangoJNINative.SetCamera(1);
			break;
		case R.id.saveAdf:
			String uuid = TangoJNINative.SaveADF();
			CharSequence text = "Saved Map: " + uuid;
			Toast toast = Toast.makeText(getApplicationContext(), text,
					Toast.LENGTH_SHORT);
			toast.show();
			break;
		default:
			return;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int pointCount = event.getPointerCount();
		if (pointCount == 1) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN: {
				TangoJNINative.StartSetCameraOffset();
				touchCurDist = 0.0f;
				touchStartPos[0] = event.getX(0);
				touchStartPos[1] = event.getY(0);
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				touchCurPos[0] = event.getX(0);
				touchCurPos[1] = event.getY(0);

				// Normalize to screen width.
				float normalizedRotX = (touchCurPos[0] - touchStartPos[0])
						/ screenSize.x;
				float normalizedRotY = (touchCurPos[1] - touchStartPos[1])
						/ screenSize.y;

				TangoJNINative.SetCameraOffset(normalizedRotX, normalizedRotY,
						touchCurDist / screenDiagnal);
				Log.i("tango_jni", String.valueOf(touchCurDist / screenDiagnal));
				break;
			}
			}
		}
		if (pointCount == 2) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_POINTER_DOWN: {
				TangoJNINative.StartSetCameraOffset();
				float absX = event.getX(0) - event.getX(1);
				float absY = event.getY(0) - event.getY(1);
				touchStartDist = (float) Math.sqrt(absX * absX + absY * absY);
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				float absX = event.getX(0) - event.getX(1);
				float absY = event.getY(0) - event.getY(1);

				touchCurDist = touchStartDist
						- (float) Math.sqrt(absX * absX + absY * absY);

				TangoJNINative.SetCameraOffset(0.0f, 0.0f, touchCurDist
						/ screenDiagnal);
				break;
			}
			case MotionEvent.ACTION_POINTER_UP: {
				int index = event.getActionIndex() == 0 ? 1 : 0;
				touchStartPos[0] = event.getX(index);
				touchStartPos[1] = event.getY(index);
				break;
			}
			}
		}
		return true;
	}
	
	private void updateUIs() {
		eventString.setText(TangoJNINative.GetEventString());
		versionString.setText(TangoJNINative.GetVersionString());

		device2StartText.setText(TangoJNINative.GetPoseString(0));
		device2ADFText.setText(TangoJNINative.GetPoseString(1));
		start2ADFText.setText(TangoJNINative.GetPoseString(2));

		uuidText.setText(TangoJNINative.GetUUID());
		learningModeText.setText(isLearning ? "Enabled" : "Disabled");
	}
}
