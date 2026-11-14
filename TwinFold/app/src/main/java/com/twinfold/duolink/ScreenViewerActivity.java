package com.twinfold.duolink;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class ScreenViewerActivity extends Activity {
    public static volatile Bitmap latestFrame;
    private static volatile ScreenViewerActivity active;
    private ImageView image;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(image, new FrameLayout.LayoutParams(-1,-1));
        TextView label = new TextView(this);
        label.setText("TWINFOLD • LIVE SPLIT VIEW   ·   Нажмите, чтобы вернуться");
        label.setTextColor(Color.WHITE);
        label.setTextSize(11);
        label.setGravity(Gravity.CENTER);
        label.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, dp(42), Gravity.TOP);
        root.addView(label, lp);
        View.OnClickListener close = v -> finish();
        root.setOnClickListener(close);
        label.setOnClickListener(close);
        setContentView(root);
        active = this;
        if (latestFrame != null) image.setImageBitmap(latestFrame);
    }

    public static void updateIfVisible(Bitmap bitmap) {
        ScreenViewerActivity viewer = active;
        if (viewer != null && viewer.image != null) {
            viewer.runOnUiThread(() -> {
                if (viewer.image != null) viewer.image.setImageBitmap(bitmap);
            });
        }
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDestroy() {
        if (active == this) active = null;
        super.onDestroy();
    }
}
