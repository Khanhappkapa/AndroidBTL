package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Moving brown platform – slides left and right, reverses at screen edges.
 * Behaves like a standard platform when bounced.
 */
public class MovingPlatform extends Platform {

    private static final float SPEED = 1.5f; // logical units per frame
    private float direction = 1f;            // +1 = right, -1 = left

    private final Paint bodyPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public MovingPlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
        bodyPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.parseColor("#C8946A"), Color.parseColor("#8B5E3C")},
                null, Shader.TileMode.CLAMP));
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(70);
    }

    @Override
    public void update(float screenW) {
        x += SPEED * direction;
        // Reverse at edges (never leave the screen)
        if (x < 0) {
            x = 0;
            direction = 1f;
        } else if (x + w > screenW) {
            x = screenW - w;
            direction = -1f;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(new RectF(x + 3, y + 5, x + w + 3, y + h + 5),
                CORNER, CORNER, shadowPaint);
        canvas.drawRoundRect(new RectF(x, y, x + w, y + h),
                CORNER, CORNER, bodyPaint);
    }

    @Override
    public float onBounce() {
        return REBOUND_STANDARD;
    }
}
