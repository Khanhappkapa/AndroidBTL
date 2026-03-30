package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Disappearing platform – red until bounced, turns yellow, then unusable.
 */
public class DisappearingPlatform extends Platform {

    private boolean bounced = false;

    private final Paint redPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yellowPaint= new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint= new Paint(Paint.ANTI_ALIAS_FLAG);

    public DisappearingPlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
        redPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.parseColor("#FF6B6B"), Color.parseColor("#CC2222")},
                null, Shader.TileMode.CLAMP));
        yellowPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.parseColor("#FFE566"), Color.parseColor("#E0A000")},
                null, Shader.TileMode.CLAMP));
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(70);
    }

    @Override
    public void draw(Canvas canvas) {
        Paint body = bounced ? yellowPaint : redPaint;
        canvas.drawRoundRect(new RectF(x + 3, y + 5, x + w + 3, y + h + 5),
                CORNER, CORNER, shadowPaint);
        canvas.drawRoundRect(new RectF(x, y, x + w, y + h),
                CORNER, CORNER, body);
    }

    @Override
    public float onBounce() {
        if (!bounced) {
            bounced = true; // Turn yellow – can no longer be bounced
        }
        return REBOUND_STANDARD;
    }

    @Override
    public boolean canBounce() {
        return !bounced; // Only usable before first bounce
    }
}
