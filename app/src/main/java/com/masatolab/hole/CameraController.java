package com.masatolab.hole;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.Comparator;

public final class CameraController {
    private final Context context;
    private final TextureView textureView;
    private final Runnable onError;

    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private CameraDevice cameraDevice;
    private CameraCaptureSession session;
    private Size previewSize;
    private CameraCharacteristics characteristics;
    private float userScale = 1f;
    private float offsetXFraction = 0f;
    private float offsetYFraction = 0f;

    public CameraController(Context context, TextureView textureView, Runnable onError) {
        this.context = context;
        this.textureView = textureView;
        this.onError = onError;
    }

    public void setCalibration(float scale, float offsetXFraction, float offsetYFraction) {
        this.userScale = scale;
        this.offsetXFraction = offsetXFraction;
        this.offsetYFraction = offsetYFraction;
        if (textureView.isAvailable()) configureTransform(textureView.getWidth(), textureView.getHeight());
    }

    public void start() {
        startThread();
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceListener);
        }
    }

    public void stop() {
        try {
            if (session != null) session.close();
        } catch (Exception ignored) {}
        session = null;
        try {
            if (cameraDevice != null) cameraDevice.close();
        } catch (Exception ignored) {}
        cameraDevice = null;
        if (cameraThread != null) {
            cameraThread.quitSafely();
            try { cameraThread.join(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        cameraThread = null;
        cameraHandler = null;
    }

    private void startThread() {
        if (cameraThread != null) return;
        cameraThread = new HandlerThread("HoleCamera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private final TextureView.SurfaceTextureListener surfaceListener = new TextureView.SurfaceTextureListener() {
        @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }
        @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }
        @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return true; }
        @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    private void openCamera(int viewWidth, int viewHeight) {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            fail();
            return;
        }
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = findBackCamera(manager);
            if (cameraId == null) { fail(); return; }
            CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);
            characteristics = chars;
            if (chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) == null) { fail(); return; }
            Size[] sizes = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(SurfaceTexture.class);
            previewSize = choosePreviewSize(sizes);
            configureTransform(viewWidth, viewHeight);
            manager.openCamera(cameraId, deviceCallback, cameraHandler);
        } catch (Exception e) {
            fail();
        }
    }

    private String findBackCamera(CameraManager manager) throws CameraAccessException {
        for (String id : manager.getCameraIdList()) {
            Integer facing = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) return id;
        }
        return null;
    }

    private Size choosePreviewSize(Size[] sizes) {
        if (sizes == null || sizes.length == 0) return new Size(1280, 720);
        return Arrays.stream(sizes)
                .filter(s -> s.getWidth() <= 1920 && s.getHeight() <= 1080)
                .min(Comparator.comparingLong(s -> Math.abs((long)s.getWidth() * s.getHeight() - 1280L * 720L)))
                .orElse(sizes[0]);
    }

    private final CameraDevice.StateCallback deviceCallback = new CameraDevice.StateCallback() {
        @Override public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createSession();
        }
        @Override public void onDisconnected(CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }
        @Override public void onError(CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            fail();
        }
    };

    private void createSession() {
        if (cameraDevice == null || !textureView.isAvailable() || previewSize == null) return;
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) { fail(); return; }
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);
            final CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession captureSession) {
                    if (cameraDevice == null) return;
                    session = captureSession;
                    try {
                        captureSession.setRepeatingRequest(builder.build(), null, cameraHandler);
                    } catch (CameraAccessException e) {
                        fail();
                    }
                }
                @Override public void onConfigureFailed(CameraCaptureSession captureSession) { fail(); }
            }, cameraHandler);
        } catch (CameraAccessException e) {
            fail();
        }
    }

    public void configureTransform(int viewWidth, int viewHeight) {
        if (previewSize == null || characteristics == null || viewWidth == 0 || viewHeight == 0) return;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int surfaceRotation = wm.getDefaultDisplay().getRotation();
        int surfaceRotationDegrees = surfaceRotation * 90;

        Integer sensorOrientationObj = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
        int sensorOrientation = sensorOrientationObj != null ? sensorOrientationObj : 0;
        int sign = (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) ? 1 : -1;
        int relativeRotation = (sensorOrientation - (surfaceRotationDegrees * sign) + 360) % 360;
        boolean rotationRequired = relativeRotation % 180 != 0;

        float scaleX;
        float scaleY;
        if (sensorOrientation == 0) {
            scaleX = rotationRequired
                    ? (float) viewWidth / previewSize.getWidth()
                    : (float) viewWidth / previewSize.getHeight();
            scaleY = rotationRequired
                    ? (float) viewHeight / previewSize.getHeight()
                    : (float) viewHeight / previewSize.getWidth();
        } else {
            scaleX = rotationRequired
                    ? (float) viewWidth / previewSize.getHeight()
                    : (float) viewWidth / previewSize.getWidth();
            scaleY = rotationRequired
                    ? (float) viewHeight / previewSize.getWidth()
                    : (float) viewHeight / previewSize.getHeight();
        }

        float finalScale = Math.max(scaleX, scaleY);
        float halfWidth = viewWidth / 2f;
        float halfHeight = viewHeight / 2f;
        Matrix matrix = new Matrix();

        if (rotationRequired) {
            matrix.setScale(
                    (1f / scaleX) * finalScale,
                    (1f / scaleY) * finalScale,
                    halfWidth,
                    halfHeight);
        } else {
            matrix.setScale(
                    ((float) viewHeight / viewWidth) / scaleY * finalScale,
                    ((float) viewWidth / viewHeight) / scaleX * finalScale,
                    halfWidth,
                    halfHeight);
        }

        matrix.postRotate(-surfaceRotationDegrees, halfWidth, halfHeight);
        matrix.postScale(userScale, userScale, halfWidth, halfHeight);
        matrix.postTranslate(offsetXFraction * viewWidth, offsetYFraction * viewHeight);
        textureView.setTransform(matrix);
    }

    private void fail() {
        if (onError != null) {
            textureView.post(onError);
        }
    }
}
