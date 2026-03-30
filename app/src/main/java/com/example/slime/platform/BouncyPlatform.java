package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

/** Extra-bouncy blue platform – launches the slime much higher than normal. */
public class BouncyPlatform extends Platform {

    private final Paint bodyPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BouncyPlatform(float x, float y, float w, float h) {
        super(x, y, w, h);
        bodyPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.parseColor("#66C8FF"), Color.parseColor("#1A72CC")},
                null, Shader.TileMode.CLAMP));
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(70);
        glowPaint.setColor(Color.parseColor("#80DFFF"));
        glowPaint.setAlpha(60);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(3f);
    }

    @Override
    public void draw(Canvas canvas) {
        // Glow outline
        canvas.drawRoundRect(new RectF(x - 2, y - 2, x + w + 2, y + h + 2),
                CORNER + 2, CORNER + 2, glowPaint);
        // Shadow
        canvas.drawRoundRect(new RectF(x + 3, y + 5, x + w + 3, y + h + 5),
                CORNER, CORNER, shadowPaint);
        // Body
        canvas.drawRoundRect(new RectF(x, y, x + w, y + h),
                CORNER, CORNER, bodyPaint);
    }

    @Override
    public float onBounce() {
        return REBOUND_BOUNCY; // Extra-high launch
    }
}
