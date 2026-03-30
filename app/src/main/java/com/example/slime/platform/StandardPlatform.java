package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/** Standard green platform – normal bounce every time. */
public class StandardPlatform extends Platform {

    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StandardPlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
        bodyPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.parseColor("#74E474"), Color.parseColor("#28A828")},
                null, Shader.TileMode.CLAMP));
        shadowPaint.setColor(Color.parseColor("#1A6B1A"));
        shadowPaint.setAlpha(90);
    }

    @Override
    public void draw(Canvas canvas) {
        // Drop shadow
        canvas.drawRoundRect(new RectF(x + 3, y + 5, x + w + 3, y + h + 5),
                CORNER, CORNER, shadowPaint);
        // Body
        canvas.drawRoundRect(new RectF(x, y, x + w, y + h),
                CORNER, CORNER, bodyPaint);
    }

    @Override
    public float onBounce() {
        return REBOUND_STANDARD;
    }
}
