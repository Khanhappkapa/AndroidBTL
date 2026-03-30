package com.example.slime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.slime.entities.SlimeState;
import com.example.slime.platform.BouncyPlatform;
import com.example.slime.platform.DisappearingPlatform;
import com.example.slime.platform.MovingPlatform;
import com.example.slime.platform.Platform;
import com.example.slime.platform.StandardPlatform;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Main game surface.  Handles the game loop, physics, sensor input,
 * platform generation/recycling, scrolling, score and game-state machine.
 *
 * Logical game units: 400 wide × 700 tall.
 * The canvas is scaled to fill the physical screen every frame.
 */
public class GameView extends SurfaceView
        implements SurfaceHolder.Callback, SensorEventListener {

    // ── Logical game resolution ───────────────────────────────────────────────
    private static final float GW = 400f;   // game width  (units)
    private static final float GH = 700f;   // game height (units)

    // ── Physics ───────────────────────────────────────────────────────────────
    private static final float GRAVITY   = 0.20f;   // units/frame²
    private static final float INITIAL_DY= -10f;    // first jump from start platform

    // ── Platform geometry ─────────────────────────────────────────────────────
    private static final float PW = 68f;   // platform width
    private static final float PH = 14f;   // platform height
    private static final float SPACING = 80f; // vertical gap between platforms
    private static final int   PLAT_COUNT = 10;

    // ── Wrapping ──────────────────────────────────────────────────────────────
    private static final float WRAP_LEFT  = -50f;
    private static final float WRAP_RIGHT = GW + 50f;

    // ── Sensor ────────────────────────────────────────────────────────────────
    private static final float SENSOR_SPEED = 0.8f; // multiplier for tilt → dx

    // ── Game state ────────────────────────────────────────────────────────────
    private enum State { MENU, PLAYING, GAME_OVER }
    private State gameState = State.MENU;

    // ── Core objects ──────────────────────────────────────────────────────────
    private Slime slime;
    private final List<Platform> platforms = new ArrayList<>();
    private final Random rng = new Random();

    // ── Score ─────────────────────────────────────────────────────────────────
    private int score   = 0;
    private int hiScore = 0;
    private static final String PREFS  = "slime_prefs";
    private static final String HI_KEY = "hi_score";

    // ── Sensor ────────────────────────────────────────────────────────────────
    private float sensorX = 0f;   // raw accelerometer x
    private SensorManager sensorManager;

    // ── Scaling ───────────────────────────────────────────────────────────────
    private float scaleX = 1f, scaleY = 1f;

    // ── Resources ─────────────────────────────────────────────────────────────
    private SpriteSheet spriteSheet;

    // ── Paints ────────────────────────────────────────────────────────────────
    private Paint bgPaint;
    private Paint scorePaint;
    private Paint hiScorePaint;
    private Paint titlePaint;
    private Paint promptPaint;
    private Paint overlayPaint;
    private Paint gameOverPaint;
    private Paint starPaint;

    // ── Background stars ─────────────────────────────────────────────────────
    private final float[] starX = new float[60];
    private final float[] starY = new float[60];
    private final float[] starR = new float[60];

    // ── Game thread ───────────────────────────────────────────────────────────
    private GameThread gameThread;

    // ── Menu idle bounce ──────────────────────────────────────────────────────
    private float menuBounceTimer = 0f;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        // Load high score
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        hiScore = prefs.getInt(HI_KEY, 0);

        // Sensor
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        initPaints();
        initStars();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Init helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void initPaints() {
        bgPaint = new Paint();

        scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(26f);
        scorePaint.setTypeface(Typeface.DEFAULT_BOLD);
        scorePaint.setShadowLayer(4f, 2f, 2f, Color.parseColor("#44000000"));

        hiScorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hiScorePaint.setColor(Color.parseColor("#FFD700"));
        hiScorePaint.setTextSize(20f);
        hiScorePaint.setTypeface(Typeface.DEFAULT_BOLD);

        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(54f);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setShadowLayer(8f, 0, 4f, Color.parseColor("#660099EE"));

        promptPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        promptPaint.setColor(Color.parseColor("#DDEEFF"));
        promptPaint.setTextSize(22f);
        promptPaint.setTextAlign(Paint.Align.CENTER);

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#99000022"));

        gameOverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gameOverPaint.setColor(Color.parseColor("#FF4466"));
        gameOverPaint.setTextSize(58f);
        gameOverPaint.setTypeface(Typeface.DEFAULT_BOLD);
        gameOverPaint.setTextAlign(Paint.Align.CENTER);
        gameOverPaint.setShadowLayer(8f, 0f, 4f, Color.BLACK);

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(Color.WHITE);
    }

    private void initStars() {
        for (int i = 0; i < starX.length; i++) {
            starX[i] = rng.nextFloat() * GW;
            starY[i] = rng.nextFloat() * GH;
            starR[i] = 0.5f + rng.nextFloat() * 1.5f;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SurfaceHolder.Callback
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Load sprite sheet
        Bitmap raw = BitmapFactory.decodeResource(getResources(), R.drawable.slimejump);
        spriteSheet = new SpriteSheet(raw);

        setupMenu();

        // Register accelerometer
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        }

        gameThread = new GameThread(holder, this);
        gameThread.setRunning(true);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        scaleX = w / GW;
        scaleY = h / GH;
        // Recreate background gradient with physical dimensions
        bgPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.parseColor("#0A0A2E"), Color.parseColor("#1A1A5E"),
                          Color.parseColor("#2B1B6F")},
                null, Shader.TileMode.CLAMP));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        sensorManager.unregisterListener(this);
        if (gameThread != null) {
            gameThread.setRunning(false);
            try { gameThread.join(1000); } catch (InterruptedException ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Game setup
    // ─────────────────────────────────────────────────────────────────────────

    private void setupMenu() {
        platforms.clear();
        score = 0;

        // Place first platform at center-bottom
        float cx = GW / 2f - PW / 2f;
        float cy = GH - 100f;
        platforms.add(new StandardPlatform(cx, cy, PW, PH));

        // A few more platforms for visual appeal on menu screen
        platforms.add(new StandardPlatform(cx - 90f, cy - 90f,  PW, PH));
        platforms.add(new StandardPlatform(cx + 60f, cy - 170f, PW, PH));
        platforms.add(new StandardPlatform(cx - 50f, cy - 250f, PW, PH));

        // Slime sits on first platform
        slime = new Slime(spriteSheet, GW / 2f, cy);
        slime.dy = 0f;
        slime.setState(SlimeState.IDLE);
    }

    private void startGame() {
        platforms.clear();
        score = 0;
        gameState = State.PLAYING;
        menuBounceTimer = 0f;

        // First platform always in center, slime sits on it
        float cx = GW / 2f - PW / 2f;
        float firstY = GH - 60f;
        platforms.add(new StandardPlatform(cx, firstY, PW, PH));

        // Generate remaining platforms evenly above the starting platform
        float topY = firstY - SPACING;
        for (int i = 1; i < PLAT_COUNT; i++) {
            spawnPlatform(topY);
            topY -= SPACING;
        }

        // Slime starts on first platform, gets initial jump
        slime = new Slime(spriteSheet, GW / 2f, firstY - Slime.SIZE);
        slime.dy = INITIAL_DY;
        slime.setState(SlimeState.LAUNCH);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Game loop
    // ─────────────────────────────────────────────────────────────────────────

    /** Called by GameThread every tick. */
    void update() {
        switch (gameState) {
            case MENU:      updateMenu();    break;
            case PLAYING:   updatePlaying(); break;
            case GAME_OVER:                  break; // wait for tap
        }
    }

    // --- MENU ----------------------------------------------------------------

    private void updateMenu() {
        // Gentle idle bounce on the starting platform
        menuBounceTimer += 0.06f;
        slime.dy = (float) Math.sin(menuBounceTimer) * 0.5f; // just visual
        // Keep slime sitting on platform (don't actually move y)
        slime.setState(SlimeState.IDLE);
        slime.updateAnimation();
    }

    // --- PLAYING -------------------------------------------------------------

    private void updatePlaying() {
        // 1. Horizontal movement from sensor
        slime.dx = -sensorX * SENSOR_SPEED;
        slime.updateFacing();
        slime.x += slime.dx;
        wrapSlime();

        // 2. Apply gravity → new velocity
        slime.dy += GRAVITY;

        // 3. Calculate potential new y (don't commit yet)
        float newY = slime.y + slime.dy;
        float midY = GH / 2f;

        // 4. Scrolling: if slime would go above midpoint
        if (newY < midY) {
            float excess = midY - newY;
            // Slime stays at midpoint
            slime.y = midY;
            // Shift all platforms down
            Platform lowest = null;
            for (Platform p : platforms) {
                p.scrollDown(excess);
                if (lowest == null || p.getY() > lowest.getY()) lowest = p;
            }
            // Score for climbing
            score += (int)(excess / 5f);
        } else {
            slime.y = newY;
        }

        // 5. Collision: only check when FALLING, and only when the slime is in
        //    FALLING state (not LANDING/LAUNCH which are still animating a bounce)
        if (slime.isFalling() && slime.getState() == SlimeState.FALLING) {
            for (Platform p : platforms) {
                if (p.canBounce() && slimeLandsOn(p)) {
                    slime.dy = p.onBounce();  // apply rebound velocity
                    score   += 10;            // +10 pts for each bounce
                    slime.setState(SlimeState.LANDING); // triggers LANDING → LAUNCH → FALLING
                    break;
                }
            }
        }

        // 6. Advance animation – Slime manages LANDING→LAUNCH→FALLING internally
        slime.updateAnimation();

        // 7. Update moving platforms
        for (Platform p : platforms) {
            p.update(GW);
        }

        // 8. Remove off-screen platforms + generate new ones above
        recycleAndGenerate();

        // 9. Game over: slime fell below screen
        if (slime.y > GH) {
            gameOver();
        }
    }

    /** True if the slime's bottom edge passes through the top of platform p. */
    private boolean slimeLandsOn(Platform p) {
        float sl = slime.x + 4f;              // narrow horizontal hit-box
        float sr = slime.x + Slime.SIZE - 4f;
        float sb = slime.y + Slime.SIZE;      // slime bottom
        float pt = p.getY();                  // platform top
        float pl = p.getX();
        float pr = p.getX() + p.getW();

        // The slime bottom is passing through the platform top edge this frame
        return sb >= pt && sb <= pt + p.getH() + Math.abs(slime.dy) + 2f
                && sr > pl && sl < pr;
    }

    private void wrapSlime() {
        if (slime.x + Slime.SIZE < WRAP_LEFT)  slime.x = WRAP_RIGHT - Slime.SIZE;
        else if (slime.x > WRAP_RIGHT)          slime.x = WRAP_LEFT;
    }

    private void recycleAndGenerate() {
        // 1. Find highest (topmost) existing platform y BEFORE any removal
        float highestY = GH;
        for (Platform p : platforms) {
            if (p.getY() < highestY) highestY = p.getY();
        }

        // 2. Collect off-screen platforms
        List<Platform> toRemove = new ArrayList<>();
        for (Platform p : platforms) {
            if (p.getY() > GH + 20f) toRemove.add(p);
        }

        // 3. Remove and replace each with a new one above the current highest
        platforms.removeAll(toRemove);
        for (int i = 0; i < toRemove.size(); i++) {
            highestY -= SPACING;
            spawnPlatform(highestY);
        }
    }

    private void spawnPlatform(float y) {
        float x = rng.nextFloat() * (GW - PW);
        int type = rng.nextInt(10);
        Platform p;
        if (type < 6)      p = new StandardPlatform   (x, y, PW, PH);
        else if (type < 8) p = new DisappearingPlatform(x, y, PW, PH);
        else if (type < 9) p = new BouncyPlatform      (x, y, PW, PH);
        else               p = new MovingPlatform      (x, y, PW, PH);
        platforms.add(p);
    }

    private void gameOver() {
        gameState = State.GAME_OVER;
        if (score > hiScore) {
            hiScore = score;
            getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putInt(HI_KEY, hiScore).apply();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    void render(Canvas canvas) {
        if (canvas == null) return;

        // Draw physical-size background
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), bgPaint);

        // Draw stars (in physical coords so they don't scale weirdly)
        for (int i = 0; i < starX.length; i++) {
            float alpha = 0.4f + 0.6f * ((float) Math.sin(System.currentTimeMillis() * 0.001f + i) * 0.5f + 0.5f);
            starPaint.setAlpha((int)(alpha * 200));
            canvas.drawCircle(starX[i] * scaleX, starY[i] * scaleY, starR[i] * scaleX, starPaint);
        }

        // Scale canvas to logical game units for all game objects
        canvas.save();
        canvas.scale(scaleX, scaleY);

        // Draw platforms
        for (Platform p : platforms) {
            p.draw(canvas);
        }

        // Draw slime
        if (slime != null) slime.draw(canvas);

        canvas.restore(); // back to physical coordinates

        // HUD (drawn in physical coords, not scaled)
        switch (gameState) {
            case MENU:      drawMenu(canvas);    break;
            case PLAYING:   drawHUD(canvas);     break;
            case GAME_OVER: drawGameOver(canvas); break;
        }
    }

    private void drawHUD(Canvas canvas) {
        canvas.drawText("Score: " + score, 20f, 60f, scorePaint);
        hiScorePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Best: " + hiScore, canvas.getWidth() - 20f, 60f, hiScorePaint);
    }

    private void drawMenu(Canvas canvas) {
        float cx = canvas.getWidth() / 2f;
        float cy = canvas.getHeight() / 2f;

        // Semi-transparent overlay at top
        canvas.drawRect(0, 0, canvas.getWidth(), cy - 60f * scaleY, overlayPaint);

        // Title
        canvas.drawText("SlimeJump!", cx, cy - 120f * scaleY, titlePaint);

        // Pulsing prompt
        float alpha = (float)(Math.sin(System.currentTimeMillis() * 0.003f) * 0.5f + 0.5f);
        promptPaint.setAlpha((int)(alpha * 220 + 35));
        canvas.drawText("Tap to Play", cx, cy - 60f * scaleY, promptPaint);

        // Best score
        hiScorePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Best: " + hiScore, cx, cy - 20f * scaleY, hiScorePaint);
    }

    private void drawGameOver(Canvas canvas) {
        float cx = canvas.getWidth() / 2f;
        float cy = canvas.getHeight() / 2f;

        // Dim overlay
        Paint dim = new Paint();
        dim.setColor(Color.parseColor("#CC000015"));
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), dim);

        // Game Over title
        canvas.drawText("Game Over", cx, cy - 120f * scaleY, gameOverPaint);

        // Score box
        Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(Color.parseColor("#4400AABB"));
        canvas.drawRoundRect(new RectF(cx - 160f, cy - 90f * scaleY, cx + 160f, cy + 60f * scaleY),
                24f, 24f, boxPaint);

        Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.WHITE);
        valuePaint.setTextSize(36f);
        valuePaint.setTypeface(Typeface.DEFAULT_BOLD);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setShadowLayer(4f, 0, 2f, Color.BLACK);
        canvas.drawText("Score: " + score, cx, cy - 40f * scaleY, valuePaint);

        if (score >= hiScore && score > 0) {
            Paint newBestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            newBestPaint.setColor(Color.parseColor("#FFD700"));
            newBestPaint.setTextSize(22f);
            newBestPaint.setTypeface(Typeface.DEFAULT_BOLD);
            newBestPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("✨ New High Score! ✨", cx, cy - 5f * scaleY, newBestPaint);
        } else {
            hiScorePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Best: " + hiScore, cx, cy + 20f * scaleY, hiScorePaint);
        }

        // Pulsing continue prompt
        float alpha = (float)(Math.sin(System.currentTimeMillis() * 0.003f) * 0.5f + 0.5f);
        promptPaint.setAlpha((int)(alpha * 220 + 35));
        canvas.drawText("Tap to Continue", cx, cy + 90f * scaleY, promptPaint);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            switch (gameState) {
                case MENU:      startGame(); break;
                case GAME_OVER: setupMenu(); gameState = State.MENU; break;
                case PLAYING:   break; // no tap action in game
            }
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sensor
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorX = event.values[0]; // negative = tilted right on portrait
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    // ─────────────────────────────────────────────────────────────────────────
    // Game Thread
    // ─────────────────────────────────────────────────────────────────────────

    static class GameThread extends Thread {
        private final SurfaceHolder holder;
        private final GameView view;
        private volatile boolean running = false;
        private static final long FRAME_MS = 16L; // ~60 fps

        GameThread(SurfaceHolder holder, GameView view) {
            this.holder = holder;
            this.view   = view;
        }

        void setRunning(boolean r) { running = r; }

        @Override
        public void run() {
            while (running) {
                long start = System.currentTimeMillis();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        synchronized (holder) {
                            view.update();
                            view.render(canvas);
                        }
                    }
                } finally {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas);
                }
                long elapsed = System.currentTimeMillis() - start;
                long sleep   = FRAME_MS - elapsed;
                if (sleep > 0) {
                    try { Thread.sleep(sleep); }
                    catch (InterruptedException ignored) {}
                }
            }
        }
    }
}
