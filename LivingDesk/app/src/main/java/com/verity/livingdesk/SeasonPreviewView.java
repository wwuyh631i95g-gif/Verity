package com.verity.livingdesk;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

public final class SeasonPreviewView extends View {
    private final SeasonRenderer renderer = new SeasonRenderer();
    private long started = System.currentTimeMillis();

    public SeasonPreviewView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        renderer.setSeason("auto");
    }

    public void setSeason(String season) {
        renderer.setSeason(season);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        renderer.draw(canvas, getWidth(), getHeight(), System.currentTimeMillis() - started);
        postInvalidateDelayed(33L);
    }
}
