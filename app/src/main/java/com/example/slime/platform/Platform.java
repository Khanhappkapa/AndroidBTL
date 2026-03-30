package com.example.slime.platform;

import android.graphics.Canvas;
import android.graphics.RectF;

/**
 * Abstract base class for all platform types.
 */
public abstract class Platform {

    protected float x, y;
    protected final float w, h;
    protected boolean active = true;

    protected static final float CORNER = 10f;

    // Standard rebound velocity (logical game units/frame)
    public static final float REBOUND_STANDARD = -10f;
    public static final float REBOUND_BOUNCY    = -16f;

    public Platform(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /** Called each game tick. Override for moving platforms. */
    public void update(float screenW) { }

    /** Draw this platform on canvas (in logical game coordinates). */
    public abstract void draw(Canvas canvas);

    /**
     * Called when the slime bounces on this platform.
     * @return vertical rebound velocity to apply to the slime.
     */
    public abstract float onBounce();

    /** Whether the slime can currently collide with this platform. */
    public boolean canBounce() { return active; }

    /** Scroll platform downward by delta logical units. */
    public void scrollDown(float delta) { y += delta; }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getW() { return w; }
    public float getH() { return h; }
    public boolean isActive() { return active; }

    /** Collision rectangle for this platform. */
    public RectF getBounds() {
        return new RectF(x, y, x + w, y + h);
    }
}
