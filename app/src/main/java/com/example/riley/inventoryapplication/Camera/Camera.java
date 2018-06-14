package com.example.riley.inventoryapplication.Camera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.example.riley.inventoryapplication.Model.ProductProfile;
import com.example.riley.inventoryapplication.Model.SQLiteHelper;
import com.example.riley.inventoryapplication.R;
import com.example.riley.inventoryapplication.View.AddEntry;
import com.example.riley.inventoryapplication.View.ProductEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class sets up the camera for the barcode scanner to use
 */
public class Camera extends AppCompatActivity {
    private SQLiteHelper sqLiteHelper;
    private TextureView textureView;
    private CameraDevice camera;
    private CameraCaptureSession cameraSession;
    private String cameraId;
    private Size previewSize;
    private CaptureRequest.Builder captureRequestBuilder;
    private BarcodeReader barcodeReader;
    private ImageReader imgReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);
        textureView = findViewById(R.id.textureView);
        sqLiteHelper = new SQLiteHelper(getApplicationContext());
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setupCamera(width, height);
            setupImageReader();
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            closeCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private CameraDevice.StateCallback currCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            camera = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            camera = cameraDevice;
            closeCamera();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            camera = cameraDevice;
            closeCamera();
        }
    };

    /**
     * Sets up a camera preview for the given width and height
     * @param width Width of the preview
     * @param height Height of the preview
     */
    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String currId : cameraManager.getCameraIdList()) {
                CameraCharacteristics camCharacteristics = cameraManager.getCameraCharacteristics(currId);
                if (camCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = currId;
                }
                StreamConfigurationMap map = camCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                try {
                    previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the largest size of a screen that is possible given the width and height paramaters of the screen
     * @param sizes All possible sizes of the camera view
     * @param width Width of the screen
     * @param height Height of the screen
     * @return The largest size that can be fit in the screen
     */
    private Size chooseOptimalSize(Size[] sizes, int width, int height) {
        List<Size> bigEnough = new ArrayList<>();
        for (Size currSize : sizes) {
            if (currSize.getHeight() == currSize.getWidth() * height / width &&
                    currSize.getWidth() >= width && currSize.getHeight() >= height) {
                bigEnough.add(currSize);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        }
        return sizes[0];
    }

    /**
     * Comparator utility class to get the largest camera width and height for
     * the camera preview
     */
    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() / ((long) rhs.getWidth() * rhs.getHeight()));
        }
    }

    /**
     * Starts the preview for the designated camera. Projects the preview onto the texture view. Sends the images ////////////////////////////////////////
     */
    private void startPreview() {
        SurfaceTexture currSurfaceTexture = textureView.getSurfaceTexture();
        currSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface currSurface = new Surface(currSurfaceTexture);
        try {
            captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(currSurface);
            captureRequestBuilder.addTarget(imgReader.getSurface());
            camera.createCaptureSession(Arrays.asList(currSurface, imgReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                cameraSession = session;
                                session.setRepeatingRequest(captureRequestBuilder.build(),
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {

                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request permission to use the screen if not already granted. Opens up the camera after
     * permission has been given
     */
    private void connectCamera() {
        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                camManager.openCamera(cameraId, currCameraStateCallback, backgroundHandler);
            } else {
                requestPermissions(new String[] {android.Manifest.permission.CAMERA}, getResources().getInteger(R.integer.camera_request_code));
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close the camera
     */
    private void closeCamera() {
        if (camera != null) {
            camera.close();
            camera = null;
        }
    }

    /**
     * Sets up the image reader for the camera
     */
    private void setupImageReader() {
        imgReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 3);
        barcodeReader = new BarcodeReader(this, imgReader, backgroundHandler);
    }

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;

    private void startBackgroundThread() {
        backgroundHandlerThread = new HandlerThread("CameraVideoImage");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
            backgroundHandlerThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        newActivity = false;
        if (textureView.isAvailable()) {
            setupCamera(textureView.getWidth(), textureView.getHeight());
            setupImageReader();
            connectCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        try {
            cameraSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        stopBackgroundThread();
        closeCamera();
        imgReader.close();
        super.onPause();
    }

    private boolean newActivity = false; // Avoid opening up multiple instances

    /**
     * Determines what to do after scanning the given barcode
     * @param barcode The barcode whose entry will be added or updated
     */
    public void onDetect(String barcode) {
        ProductProfile prevEntry = sqLiteHelper.findByBarcode(barcode);
        if (!newActivity) {
            if (prevEntry != null) {
                updateActivity(barcode, prevEntry);
            } else {
                addActivity(barcode);
            }
            newActivity = true;
        }
    }

    /**
     * Takes the user to the update entry page
     * @param barcode The barcode of the product already in the database
     * @param prevEntry The entry already contained in the database with
     *                  the same barcode
     */
    private void updateActivity(String barcode, ProductProfile prevEntry) {
        Intent intent = new Intent(getApplicationContext(), ProductEntry.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("barcode", barcode);
        intent.putExtra("brand", prevEntry.getBrandName());
        intent.putExtra("product", prevEntry.getProductName());
        intent.putExtra("update", true);
        startActivity(intent);
    }

    /**
     * Takes the user to the add entry page
     * @param barcode The barcode of the product to be added
     */
    private void addActivity(String barcode) {
        Intent intent = new Intent(getApplicationContext(), AddEntry.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("barcode", barcode);
        startActivity(intent);
    }

}
