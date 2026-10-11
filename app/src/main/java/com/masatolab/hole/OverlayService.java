package com.masatolab.hole;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

public class OverlayService extends Service {
    public static final String ACTION_START = "com.masatolab.hole.START";
    public static final String ACTION_STOP = "com.masatolab.hole.STOP";
    private static final String CHANNEL_ID = "hole_camera_overlay";
    private static final int NOTIFICATION_ID = 1701;

    private WindowManager windowManager;
    private FrameLayout root;
    private TextureView textureView;
    private CameraController camera;

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START;
        if (ACTION_STOP.equals(action)) {
            stopOverlay();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission is required", Toast.LENGTH_LONG).show();
            Prefs.running(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }

        startAsForeground();
        showOverlay();
        Prefs.running(this, true);
        return START_NOT_STICKY;
    }

    private void startAsForeground() {
        Intent stopIntent = new Intent(this, OverlayService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(this, 2, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int pct = Math.round(Prefs.alpha(this) * 100f);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(com.masatolab.hole.R.drawable.ic_hole)
                .setContentTitle("HOLE · " + pct + "%")
                .setContentText(Prefs.alpha(this) > 0.79f ? "HOLE view mode · underlying touches are blocked" : "Transparent phone is active · touch-through on")
                .setContentIntent(openPending)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "STOP", stopPending).build())
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void showOverlay() {
        stopOverlay();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        root = new FrameLayout(this);
        textureView = new TextureView(this);
        textureView.setOpaque(false);
        textureView.setAlpha(1f);
        root.addView(textureView, new FrameLayout.LayoutParams(-1, -1));

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        // Android 12+ decides whether pass-through touches are safe from the WINDOW alpha,
        // not the alpha of the child TextureView. Keep the real opacity here.
        params.alpha = Prefs.alpha(this);
        windowManager.addView(root, params);

        camera = new CameraController(this, textureView, () -> {
            Toast.makeText(this, "Camera unavailable. HOLE stopped.", Toast.LENGTH_LONG).show();
            stopOverlay();
            stopSelf();
        });
        camera.setCalibration(Prefs.scale(this), Prefs.offsetX(this), Prefs.offsetY(this));
        camera.start();
    }

    private void stopOverlay() {
        if (camera != null) { camera.stop(); camera = null; }
        if (windowManager != null && root != null) {
            try { windowManager.removeView(root); } catch (Exception ignored) {}
        }
        root = null; textureView = null; windowManager = null;
        Prefs.running(this, false);
    }

    @Override public void onDestroy() {
        stopOverlay();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.notification_channel_description));
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);
    }
}
