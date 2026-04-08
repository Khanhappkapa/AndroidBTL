package com.example.slime;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.slime.entities.SlimeState;

/**
 * The player-controlled Slime character.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  ANIMATION DESIGN                                               │
 * │                                                                 │
 * │  LANDING  → very fast (60 ms/frame) → Row 1 flat frames       │
 * │    └── auto-transitions to LAUNCH when sequence ends           │
 * │                                                                 │
 * │  LAUNCH   → medium  (100 ms/frame) → Row 0 elongated frames   │
 * │    └── plays ONCE, holds last frame until dy > 0 (falling)      │
 * │                                                                 │
 * │  FALLING  → medium  → frames 11, 12, 13                         │
 * │    └── plays ONCE, holds at frame 13 until next collision       │
 * │                                                                 │
 * │  IDLE     → static  → frame 0 of Row 0 (menu screen only)     │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * Sprite sheet layout (5 cols × 2 rows – indices 0-9):
 *  Row 0 │  0:round │  1:elon-A │  2:elon-B │  3:elon-C │  4:elon-D  │  ← "dòng 1"
 *  Row 1 │  5:flat-A │  6:flat-B │  7:flat-C │  8:flat-D │  9:flat-E  │  ← "dòng 2"
 *
 *  LANDING uses Row 1 (5 → 9) – all flat frames in squish order
 *  LAUNCH  uses Row 0 (1 → 4) – elongated, excluding the round frame
 *  FALLING uses frames 11, 12, 13
 */
public class Slime {

    // ── Logical size ─────────────────────────────────────────────────────────
    public static final float SIZE = 43f; // logical game units

    // ─── Frame index arrays ───────────────────────────────────────────────────
    //
    // LANDING: flat frames (Row 1). Start from flattest, spring back to less flat.
    // Adjust order here if the sprite sheet has a different flat→round order.
    private static final int[] F_LANDING = {6, 9};

    // LAUNCH: elongated frames (Row 0, skipping the round frame at index 0).
    // Goes from slightly elongated → most elongated → back to neutral.
    private static final int[] F_LAUNCH  = {1, 2, 3};

    // FALLING: plays once and stops at the final frame
    private static final int[] F_FALLING = {11, 13};

    // IDLE: the single round frame at Row 0 index 0.

    private static final int   F_STATIC  = 0;

    // ─── Timing (game-ticks per animation frame, at ~60 fps) ─────────────────
    // 60 ms ÷ 16.67 ms/tick ≈ 4 ticks  → very fast, strong elasticity feel
    private static final int T_LANDING = 4;
    // 100 ms ÷ 16.67 ms/tick ≈ 6 ticks → medium, one-shot upward burst
    private static final int T_LAUNCH  = 6;
    private static final int T_FALLING = 6;

    // ── Physics ───────────────────────────────────────────────────────────────
    public float x, y;  // top-left position in logical game units
    public float dx;     // horizontal velocity (units/frame)
    public float dy;     // vertical   velocity (units/frame, positive = down)

    // ── Internal animation state ──────────────────────────────────────────────
    private SlimeState state    = SlimeState.IDLE;
    private int        frameIdx = 0;
    private int        animTick = 0;
    private boolean    facingLeft = false;

    private final SpriteSheet sheet;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Slime(SpriteSheet sheet, float startCenterX, float startTopY) {
        this.sheet = sheet;
        this.x = startCenterX - SIZE / 2f;
        this.y = startTopY;
        paint.setFilterBitmap(false); // crisp pixel-art – no bilinear blur
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Switch to a new animation state.
     * Resets frame index so the sequence always starts fresh.
     */
    public void setState(SlimeState next) {
        if (next != state) {
            state    = next;
            frameIdx = 0;
            animTick = 0;
        }
    }

    public SlimeState getState() { return state; }

    /**
     * Advance the animation by one game tick.
     *
     * LANDING and LAUNCH are timed sequences with auto-transitions:
     *   LANDING → (sequence ends) → LAUNCH
     *   LAUNCH  → (sequence ends) → FALLING
     *
     * FALLING and IDLE are static – no tick counting needed.
     */
    public void updateAnimation() {
        switch (state) {

            case LANDING:
                animTick++;
                if (animTick >= T_LANDING) {
                    animTick = 0;
                    frameIdx++;
                    if (frameIdx >= F_LANDING.length) {
                        // LANDING finished → auto-transition to LAUNCH
                        state    = SlimeState.LAUNCH;
                        frameIdx = 0;
                        animTick = 0;
                    }
                }
                break;

            case LAUNCH:
                animTick++;
                if (animTick >= T_LAUNCH) {
                    animTick = 0;
                    if (frameIdx < F_LAUNCH.length - 1) {
                        frameIdx++;
                    }
                }
                // Transition to FALLING only when physics says we are falling
                if (dy > 0f) {
                    state    = SlimeState.FALLING;
                    frameIdx = 0;
                    animTick = 0;
                }
                break;

            case FALLING:
                animTick++;
                if (animTick >= T_FALLING) {
                    animTick = 0;
                    if (frameIdx < F_FALLING.length - 1) {
                        frameIdx++;
                    }
                }
                break;

            case IDLE:
                // Static – nothing to advance
                break;
        }
    }

    /** Update horizontal facing. Call after dx is set each tick. */
    public void updateFacing() {
        if      (dx < -0.2f) facingLeft = true;
        else if (dx >  0.2f) facingLeft = false;
    }

    // ── Convenience helpers ───────────────────────────────────────────────────
    public float bottom()  { return y + SIZE; }
    public float centerX() { return x + SIZE / 2f; }
    public RectF getBounds() { return new RectF(x, y, x + SIZE, y + SIZE); }
    public boolean isRising()  { return dy < 0f; }
    public boolean isFalling() { return dy > 0f; }

    // ── Drawing ───────────────────────────────────────────────────────────────
    public void draw(Canvas canvas) {
        int frameId = currentFrameId();
        Bitmap bmp = sheet.getFrame(frameId);
        if (bmp == null) return;

        int baseLine = getBaseline(frameId);
        float scale = SIZE / 32f;
        float scaledBase = baseLine * scale;
        float offsetY = SIZE - scaledBase;

        RectF dst = new RectF(x, y + offsetY, x + SIZE, y + SIZE + offsetY);
        
        if (facingLeft) {
            canvas.save();
            canvas.scale(-1f, 1f, x + SIZE / 2f, y + SIZE / 2f + offsetY);
            canvas.drawBitmap(bmp, null, dst, paint);
            canvas.restore();
        } else {
            canvas.drawBitmap(bmp, null, dst, paint);
        }
    }

    private int currentFrameId() {
        switch (state) {
            case LANDING: return F_LANDING[Math.min(frameIdx, F_LANDING.length - 1)];
            case LAUNCH:  return F_LAUNCH[Math.min(frameIdx, F_LAUNCH.length - 1)];
            case FALLING: return F_FALLING[Math.min(frameIdx, F_FALLING.length - 1)];
            case IDLE:
            default:      return F_STATIC;
        }
    }

    private int getBaseline(int frameId) {
        switch (frameId) {
            case 6: return 16;
            case 9: return 14;
            case 1: return 29;
            case 2: return 28;
            case 3: return 26;
            case 11: return 19;
            case 13: return 26;
            case 0: default: return 29;
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

}
