package com.example.slime;

import android.graphics.Bitmap;
import android.graphics.RectF;

/**
 * Extracts individual animation frames from a sprite sheet image.
 *
 * Expected sprite sheet layout  (5 columns × 2 rows = 10 frames):
 *
 *  Row 0 │ 0:idle-A  │ 1:idle-B  │ 2:squish-L │ 3:squish-M │ 4:airborne │
 *  Row 1 │ 5:squish-H│ 6:normal-A│ 7:normal-B │ 8:slight-sq│ 9:fall     │
 *
 * Animation mappings:
 *  IDLE    → [6, 7]         gentle bob
 *  LANDING → [5, 8, 6]      heavy → slight → normal squish
 *  LAUNCH  → [3, 2, 0]      squish releasing → round
 *  RISING  → [0, 1]         round forms going up
 *  FALLING → [4, 9]         airborne → round falling
 */
public class SpriteSheet {

    public static final int COLS = 8;
    public static final int ROWS = 3;

    private final Bitmap[] frames;
    private final int frameW;
    private final int frameH;

    public SpriteSheet(Bitmap sheet) {
        frameW = sheet.getWidth()  / COLS;
        frameH = sheet.getHeight() / ROWS;
        frames = new Bitmap[ROWS * COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                frames[r * COLS + c] = Bitmap.createBitmap(sheet,
                        c * frameW, r * frameH, frameW, frameH);
            }
        }
    }

    public Bitmap getFrame(int index) {
        if (index < 0 || index >= frames.length) return frames[0];
        return frames[index];
    }

    public int getFrameW() { return frameW; }
    public int getFrameH() { return frameH; }
}
