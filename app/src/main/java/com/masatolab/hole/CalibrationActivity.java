package com.masatolab.hole;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.TextureView;
import android.widget.Toast;

import java.util.Locale;

public class CalibrationActivity extends Activity {
    private TextureView preview;
    private CameraController camera;
    private float scale;
    private float offsetX;
    private float offsetY;
    private TextView scaleValue, xValue, yValue;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scale = Prefs.scale(this);
        offsetX = Prefs.offsetX(this);
        offsetY = Prefs.offsetY(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        preview = new TextureView(this);
        preview.setOpaque(true);
        root.addView(preview, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(18));
        panel.setBackgroundColor(0xDD111111);

        TextView title = text("OPTICAL ALIGNMENT", 14, Typeface.BOLD, Color.WHITE);
        title.setLetterSpacing(0.12f);
        panel.addView(title);
        TextView help = text("Hold the phone over a straight edge 30–50 cm away. Adjust until the image inside the screen visually continues the real scene outside the phone.", 13, Typeface.NORMAL, 0xFFD5D5D5);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2); hp.topMargin = dp(7); hp.bottomMargin = dp(10);
        panel.addView(help, hp);

        scaleValue = addSlider(panel, "SCALE", 60, Math.round((scale - 0.80f) * 100), p -> {
            scale = 0.80f + p / 100f; apply();
        });
        xValue = addSlider(panel, "X OFFSET", 40, Math.round((offsetX + 0.20f) * 100), p -> {
            offsetX = p / 100f - 0.20f; apply();
        });
        yValue = addSlider(panel, "Y OFFSET", 40, Math.round((offsetY + 0.20f) * 100), p -> {
            offsetY = p / 100f - 0.20f; apply();
        });

        LinearLayout buttons = new LinearLayout(this);
        Button reset = button("RESET", false);
        reset.setOnClickListener(v -> {
            scale = 1f; offsetX = 0f; offsetY = 0f;
            Prefs.resetCalibration(this);
            recreate();
        });
        Button save = button("SAVE", true);
        save.setOnClickListener(v -> {
            Prefs.scale(this, scale); Prefs.offsetX(this, offsetX); Prefs.offsetY(this, offsetY);
            Toast.makeText(this, "Calibration saved", Toast.LENGTH_SHORT).show();
            finish();
        });
        buttons.addView(reset, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, dp(48), 1); saveLp.leftMargin = dp(10);
        buttons.addView(save, saveLp);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2); bp.topMargin = dp(10);
        panel.addView(buttons, bp);

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        pp.leftMargin = dp(12); pp.rightMargin = dp(12); pp.bottomMargin = dp(12);
        root.addView(panel, pp);
        setContentView(root);

        camera = new CameraController(this, preview, () -> Toast.makeText(this, "Camera preview unavailable", Toast.LENGTH_LONG).show());
        camera.setCalibration(scale, offsetX, offsetY);
        updateLabels();
    }

    @Override protected void onResume() { super.onResume(); camera.start(); }
    @Override protected void onPause() { camera.stop(); super.onPause(); }

    private interface ProgressListener { void onChange(int p); }
    private TextView addSlider(LinearLayout parent, String label, int max, int progress, ProgressListener listener) {
        LinearLayout head = new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        TextView l = text(label, 11, Typeface.BOLD, 0xFFBBBBBB); l.setLetterSpacing(0.08f);
        TextView value = text("", 13, Typeface.BOLD, Color.WHITE); value.setGravity(Gravity.END);
        head.addView(l, new LinearLayout.LayoutParams(0, -2, 1)); head.addView(value, new LinearLayout.LayoutParams(dp(100), -2));
        parent.addView(head);
        SeekBar bar = new SeekBar(this); bar.setMax(max); bar.setProgress(Math.max(0, Math.min(max, progress)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) { listener.onChange(p); updateLabels(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(5);
        parent.addView(bar, lp);
        return value;
    }

    private void apply() { if (camera != null) camera.setCalibration(scale, offsetX, offsetY); }
    private void updateLabels() {
        if (scaleValue != null) scaleValue.setText(String.format(Locale.US, "%.2fx", scale));
        if (xValue != null) xValue.setText(String.format(Locale.US, "%+.0f%%", offsetX * 100));
        if (yValue != null) yValue.setText(String.format(Locale.US, "%+.0f%%", offsetY * 100));
    }

    private TextView text(String s, int sp, int style, int color) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setTypeface(Typeface.create("sans", style)); return v;
    }
    private Button button(String s, boolean primary) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(12); b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(primary ? Color.BLACK : Color.WHITE); b.setBackgroundColor(primary ? Color.WHITE : 0xFF333333); return b;
    }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
