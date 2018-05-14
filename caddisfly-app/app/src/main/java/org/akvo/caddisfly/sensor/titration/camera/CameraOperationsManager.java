/*
 * Copyright (C) Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Caddisfly.
 *
 * Akvo Caddisfly is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Caddisfly is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Caddisfly. If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.caddisfly.sensor.titration.camera;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.akvo.caddisfly.helper.FileHelper;
import org.akvo.caddisfly.preference.AppPreferences;
import org.akvo.caddisfly.sensor.striptest.utils.MessageUtils;
import org.akvo.caddisfly.sensor.titration.ui.TitrationMeasureActivity;
import org.akvo.caddisfly.sensor.titration.ui.TitrationTestHandler;
import org.akvo.caddisfly.util.CameraPreview;
import org.akvo.caddisfly.util.ImageUtil;

public class CameraOperationsManager {
    private static final long AUTO_FOCUS_DELAY = 5000L;

    // A Handler for running camera tasks in the background.
    private Handler mCameraHandler;

    private Camera mCamera;

    private boolean changingExposure = false;
    private TitrationTestHandler titrationTestHandler;

    //debug code
    private byte[] bytes;

    private Context context;

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] imageData, Camera camera) {

            if (bytes != null && bytes.length > 0 && AppPreferences.isTestMode()) {
                // Use test image if we are in test mode
                TitrationTestHandler.mDecodeData.setDecodeImageByteArray(bytes);

            } else {
//                ImageUtil.saveImageBytes(context, camera, imageData, FileHelper.FileType.TEST_IMAGE,
//                        String.valueOf(Calendar.getInstance().getTimeInMillis()));

                // store image for later use
                TitrationTestHandler.mDecodeData.setDecodeImageByteArray(imageData);
            }
            MessageUtils.sendMessage(titrationTestHandler, TitrationTestHandler.DECODE_IMAGE_CAPTURED_MESSAGE, 0);
        }
    };

    private Runnable runAutoFocus = new Runnable() {
        public void run() {
            if (mCamera != null) {
                if (!changingExposure) {
                    // Check the focus. This is mainly needed in order to restart focus that doesn't run anymore,
                    // which sometimes happens on samsung devices.
                    mCamera.autoFocus((success, camera) -> {
                        // if we are in one of the other modes, we need to run 'cancelAutofocus' in order
                        // to start the continuous focus again. *sigh*
                        if (!camera.getParameters().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                            mCamera.cancelAutoFocus();
                        }
                    });
                }
                mCameraHandler.postDelayed(runAutoFocus, AUTO_FOCUS_DELAY);
            }
        }
    };

    public CameraOperationsManager(Context context, String name) {
        this.context = context;
        if (AppPreferences.isTestMode()) {
            bytes = ImageUtil.loadImageBytes(name, FileHelper.FileType.TEST_IMAGE);
        }
    }

    public CameraPreview initCamera(Context context) {
        startCameraThread();

        // open the camera and create a preview surface for it
        CameraPreview cameraPreview = new CameraPreview(context);
        mCamera = cameraPreview.getCamera();
        return cameraPreview;
    }

    public void setTitrationTestHandler(TitrationTestHandler titrationTestHandler) {
        this.titrationTestHandler = titrationTestHandler;
    }

    // TODO add cancel request
    public void setDecodeImageCaptureRequest() {
        if (mCameraHandler != null && mCamera != null) {
            try {
                mCameraHandler.post(() -> {
                    if (mCamera != null) {
                        mCamera.setOneShotPreviewCallback(previewCallback);
                    }
                });
            } catch (Exception ignored) {
                // do nothing
            }
        }
    }

    ///////////////////////////// autofocus handling ////////////////////////////////////////

    // TODO check if these boundaries are ok
    public void changeExposure(int exposureChange) {
        int expComp = mCamera.getParameters().getExposureCompensation();
        int newSetting = expComp + exposureChange;

        // if we are within bounds, change the capture request
        if (newSetting != expComp &&
                newSetting <= mCamera.getParameters().getMaxExposureCompensation() &&
                newSetting >= mCamera.getParameters().getMinExposureCompensation()) {
            changingExposure = true;
            mCamera.stopPreview();
            mCamera.cancelAutoFocus();
            stopAutofocus();
            Camera.Parameters cameraParam = mCamera.getParameters();
            cameraParam.setExposureCompensation(newSetting);
            mCamera.setParameters(cameraParam);
            mCamera.startPreview();
            changingExposure = false;
            startAutofocus();
        }
    }

    public void startAutofocus() {
        if (mCameraHandler != null) {
            mCameraHandler.removeCallbacks(runAutoFocus);
            mCameraHandler.postDelayed(runAutoFocus, AUTO_FOCUS_DELAY);
        } else {
            throw new UnsupportedOperationException("can't start autofocus");
        }
    }

    public void stopAutofocus() {
        if (mCameraHandler != null) {
            mCameraHandler.removeCallbacks(runAutoFocus);
        }
    }

    public void stopCamera() {
        mCamera = null;
        mCameraHandler = null;
    }
    ////////////////////////////////////////// background thread ///////////////////////////////////

    /**
     * Starts a background thread and its Handler.
     */
    private void startCameraThread() {
        if (TitrationMeasureActivity.DEBUG) {
            Log.d("Caddisfly", "Starting camera background thread");
        }
        HandlerThread mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }
}