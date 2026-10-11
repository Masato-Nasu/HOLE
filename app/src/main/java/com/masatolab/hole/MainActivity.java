package com.masatolab.hole;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 10;
    private boolean pendingStart = false;
    private TextView cameraState;
    private TextView overlayState;
    private TextView opacityValue;
    private Button startButton;
    private Button stopButton;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    @Override protected void onResume() {
        super.onResume();
        refreshPermissionState();
        if (pendingStart && hasCamera() && Settings.canDrawOverlays(this)) {
            pendingStart = false;
            startHole();
        }
    }

    private View buildUi() {
        int pad = dp(24);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(28), pad, dp(24));
        root.setBackgroundColor(Color.rgb(244, 244, 240));

        TextView title = text("HOLE", 42, Typeface.BOLD, Color.rgb(17,17,17));
        title.setLetterSpacing(0.06f);
        root.addView(title);
        TextView sub = text("TRANSPARENT PHONE", 13, Typeface.NORMAL, Color.DKGRAY);
        sub.setLetterSpacing(0.12f);
        root.addView(sub);

        TextView idea = text("The screen becomes a window.\nCamera image + any app, continuously.", 18, Typeface.NORMAL, Color.rgb(30,30,30));
        LinearLayout.LayoutParams ideaLp = new LinearLayout.LayoutParams(-1, -2);
        ideaLp.topMargin = dp(28);
        root.addView(idea, ideaLp);

        root.addView(divider(dp(24)));

        cameraState = statusRow(root, "CAMERA");
        overlayState = statusRow(root, "DRAW OVER APPS");

        LinearLayout perms = new LinearLayout(this);
        perms.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(46), 1);
        Button cameraButton = button("ALLOW CAMERA", false);
        cameraButton.setOnClickListener(v -> requestCamera());
        perms.addView(cameraButton, half);
        Space gap = new Space(this);
        perms.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
        Button overlayButton = button("ALLOW OVERLAY", false);
        overlayButton.setOnClickListener(v -> requestOverlay());
        perms.addView(overlayButton, half);
        root.addView(perms);

        root.addView(divider(dp(28)));

        TextView oLabel = text("TRANSPARENCY", 12, Typeface.BOLD, Color.rgb(60,60,60));
        oLabel.setLetterSpacing(0.10f);
        root.addView(oLabel);

        LinearLayout opacityHeader = new LinearLayout(this);
        opacityHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView opacityHint = text("Camera layer", 17, Typeface.NORMAL, Color.rgb(20,20,20));
        opacityHeader.addView(opacityHint, new LinearLayout.LayoutParams(0, -2, 1));
        opacityValue = text("50%", 22, Typeface.BOLD, Color.rgb(17,17,17));
        opacityHeader.addView(opacityValue);
        LinearLayout.LayoutParams oh = new LinearLayout.LayoutParams(-1, -2);
        oh.topMargin = dp(8);
        root.addView(opacityHeader, oh);

        SeekBar alpha = new SeekBar(this);
        alpha.setMax(69);
        alpha.setProgress(Math.max(0, Math.min(69, Math.round(Prefs.alpha(this) * 100f) - 10)));
        alpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = (progress + 10) / 100f;
                Prefs.alpha(MainActivity.this, value);
                opacityValue.setText(String.format(Locale.US, "%d%%", Math.round(value * 100)));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(alpha);
        opacityValue.setText(Prefs.alpha(this) > 0.79f
                ? "100% · VIEW ONLY"
                : String.format(Locale.US, "%d%%", Math.round(Prefs.alpha(this) * 100)));

        Button halfBtn = button("50%", false);
        halfBtn.setOnClickListener(v -> { Prefs.alpha(this, 0.50f); alpha.setProgress(40); });
        Button holeBtn = button("HOLE 100% · VIEW", false);
        holeBtn.setOnClickListener(v -> {
            Prefs.alpha(this, 1.00f);
            opacityValue.setText("100% · VIEW ONLY");
        });
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.addView(halfBtn, new LinearLayout.LayoutParams(0, dp(46), 1));
        Space mg = new Space(this); modes.addView(mg, new LinearLayout.LayoutParams(dp(10), 1));
        modes.addView(holeBtn, new LinearLayout.LayoutParams(0, dp(46), 1));
        root.addView(modes);
        TextView touchNote = text("10–79%: touch-through. 100%: optical HOLE view; Android blocks touches to other apps for security.", 11, Typeface.NORMAL, Color.GRAY);
        LinearLayout.LayoutParams tnp = new LinearLayout.LayoutParams(-1, -2); tnp.topMargin = dp(7);
        root.addView(touchNote, tnp);

        root.addView(divider(dp(22)));

        TextView calTitle = text("OPTICAL ALIGNMENT", 12, Typeface.BOLD, Color.rgb(60,60,60));
        calTitle.setLetterSpacing(0.10f);
        root.addView(calTitle);
        TextView calBody = text("Match scale and camera offset so the scene appears to continue through the physical screen edge.", 15, Typeface.NORMAL, Color.rgb(70,70,70));
        LinearLayout.LayoutParams cb = new LinearLayout.LayoutParams(-1, -2); cb.topMargin = dp(7);
        root.addView(calBody, cb);
        Button calibrate = button("CALIBRATE", false);
        calibrate.setOnClickListener(v -> {
            stopHole();
            if (!hasCamera()) { requestCamera(); return; }
            startActivity(new Intent(this, CalibrationActivity.class));
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, dp(50)); clp.topMargin = dp(12);
        root.addView(calibrate, clp);

        Space stretch = new Space(this);
        root.addView(stretch, new LinearLayout.LayoutParams(1, 0, 1));

        startButton = button("START TRANSPARENT PHONE", true);
        startButton.setOnClickListener(v -> beginStartFlow());
        root.addView(startButton, new LinearLayout.LayoutParams(-1, dp(58)));

        stopButton = button("STOP", false);
        stopButton.setOnClickListener(v -> stopHole());
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(-1, dp(48)); stopLp.topMargin = dp(10);
        root.addView(stopButton, stopLp);

        TextView foot = text("P0.1 · manual optical calibration · camera stays on while active", 11, Typeface.NORMAL, Color.GRAY);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(-1, -2); flp.topMargin = dp(14);
        root.addView(foot, flp);
        refreshPermissionState();
        return root;
    }

    private TextView statusRow(LinearLayout root, String name) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text(name, 13, Typeface.BOLD, Color.rgb(50,50,50));
        row.addView(label, new LinearLayout.LayoutParams(0, dp(36), 1));
        TextView status = text("—", 13, Typeface.BOLD, Color.rgb(17,17,17));
        status.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(status, new LinearLayout.LayoutParams(dp(100), dp(36)));
        root.addView(row);
        return status;
    }

    private void refreshPermissionState() {
        if (cameraState == null) return;
        cameraState.setText(hasCamera() ? "READY" : "NEEDED");
        overlayState.setText(Settings.canDrawOverlays(this) ? "READY" : "NEEDED");
        if (startButton != null) startButton.setText(Prefs.running(this) ? "RESTART TRANSPARENT PHONE" : "START TRANSPARENT PHONE");
        if (stopButton != null) stopButton.setEnabled(Prefs.running(this));
    }

    private void beginStartFlow() {
        pendingStart = true;
        if (!hasCamera()) { requestCamera(); return; }
        if (!Settings.canDrawOverlays(this)) { requestOverlay(); return; }
        pendingStart = false;
        startHole();
    }

    private void requestCamera() {
        if (hasCamera()) return;
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
    }

    private void requestOverlay() {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private boolean hasCamera() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startHole() {
        Intent service = new Intent(this, OverlayService.class);
        service.setAction(OverlayService.ACTION_START);
        try {
            startForegroundService(service);
            Prefs.running(this, true);
            Toast.makeText(this, "HOLE is on", Toast.LENGTH_SHORT).show();
            moveTaskToBack(true);
        } catch (Exception e) {
            Prefs.running(this, false);
            Toast.makeText(this, "Could not start camera overlay: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
        refreshPermissionState();
    }

    private void stopHole() {
        Intent service = new Intent(this, OverlayService.class);
        service.setAction(OverlayService.ACTION_STOP);
        startService(service);
        Prefs.running(this, false);
        refreshPermissionState();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshPermissionState();
        if (requestCode == REQ_CAMERA && pendingStart && hasCamera()) {
            if (!Settings.canDrawOverlays(this)) requestOverlay();
            else { pendingStart = false; startHole(); }
        }
    }

    private TextView text(String s, int sp, int style, int color) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setTypeface(Typeface.create("sans", style));
        return v;
    }

    private Button button(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(12); b.setTypeface(Typeface.create("sans", Typeface.BOLD));
        b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : Color.rgb(17,17,17));
        b.setBackgroundColor(primary ? Color.rgb(17,17,17) : Color.rgb(224,224,219));
        return b;
    }

    private View divider(int top) {
        View v = new View(this); v.setBackgroundColor(Color.rgb(210,210,205));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1)); lp.topMargin = top; lp.bottomMargin = dp(14);
        v.setLayoutParams(lp); return v;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
