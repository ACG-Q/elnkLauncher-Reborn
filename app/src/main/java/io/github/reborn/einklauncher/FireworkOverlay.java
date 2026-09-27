package io.github.reborn.einklauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class FireworkOverlay extends View {

  private static final long FRAME_MS = 100;
  private static final int MAX_PARTICLES = 96;
  private static final int BURST_COUNT = 20;
  private static final int WAVE_COUNT = 3;
  private static final int WAVE_STAGGER = 10;

  private static final int KIND_FREE = 0;
  private static final int KIND_ROCKET = 1;
  private static final int KIND_TRAIL = 2;
  private static final int KIND_BURST = 3;

  private static final float RISE_G = 3.5f;
  private static final float TRAIL_G = 0.6f;
  private static final float TRAIL_DRAG = 0.95f;
  private static final float BURST_G = 0.8f;
  private static final float BURST_DRAG = 0.93f;

  private final float[] posX = new float[MAX_PARTICLES];
  private final float[] posY = new float[MAX_PARTICLES];
  private final float[] velX = new float[MAX_PARTICLES];
  private final float[] velY = new float[MAX_PARTICLES];
  private final float[] size0 = new float[MAX_PARTICLES];
  private final int[] life = new int[MAX_PARTICLES];
  private final int[] maxLife = new int[MAX_PARTICLES];
  private final byte[] kind = new byte[MAX_PARTICLES];

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Random random = new Random();

  private boolean running;
  private int launchedWaves;
  private int waveDelay;
  private float originX;
  private float originY;
  private float density;

  public FireworkOverlay(Context context) {
    super(context);
    density = context.getResources().getDisplayMetrics().density;
  }

  public FireworkOverlay(Context context, AttributeSet attrs) {
    super(context, attrs);
    density = context.getResources().getDisplayMetrics().density;
  }

  public void burst(float x, float y) {
    originX = x;
    originY = y;
    launchedWaves = 0;
    waveDelay = -1;
    for (int i = 0; i < MAX_PARTICLES; i++) {
      kind[i] = KIND_FREE;
    }
    running = true;
    postInvalidateDelayed(FRAME_MS);
  }

  public void stop() {
    for (int i = 0; i < MAX_PARTICLES; i++) {
      kind[i] = KIND_FREE;
    }
    running = false;
    invalidate();
  }

  private int spawn() {
    for (int i = 0; i < MAX_PARTICLES; i++) {
      if (kind[i] == KIND_FREE) {
        return i;
      }
    }
    return -1;
  }

  private void launchRocket(int wave) {
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) {
      return;
    }
    float spread = wave == 0 ? 0f : (wave == 1 ? -0.2f : 0.18f);
    float lift = wave == 0 ? 0f : (wave == 1 ? 0.08f : 0.14f);
    float tx = originX + spread * w + (random.nextFloat() - 0.5f) * 0.06f * w;
    float ty = originY - lift * h;
    tx = Math.max(0.08f * w, Math.min(0.92f * w, tx));
    ty = Math.max(0.06f * h, Math.min(0.6f * h, ty));
    float sx = tx + (random.nextFloat() - 0.5f) * 0.1f * w;
    float sy = h * 0.98f;
    float rise = sy - ty;
    float vy0 = -(float) Math.sqrt(2 * RISE_G * rise);
    float tApex = -vy0 / RISE_G;
    int s = spawn();
    if (s < 0) {
      return;
    }
    kind[s] = KIND_ROCKET;
    posX[s] = sx;
    posY[s] = sy;
    velX[s] = (tx - sx) / tApex;
    velY[s] = vy0;
    life[s] = 400;
    maxLife[s] = 400;
    size0[s] = 8 * density;
  }

  private void explode(float x, float y) {
    for (int k = 0; k < BURST_COUNT; k++) {
      int s = spawn();
      if (s < 0) {
        return;
      }
      double angle = k * 2 * Math.PI / BURST_COUNT + (random.nextFloat() - 0.5f) * 0.35;
      float speed = (9 + random.nextFloat() * 5) * (0.85f + random.nextFloat() * 0.3f);
      kind[s] = KIND_BURST;
      posX[s] = x;
      posY[s] = y;
      velX[s] = (float) (Math.cos(angle) * speed);
      velY[s] = (float) (Math.sin(angle) * speed);
      life[s] = 14 + random.nextInt(5);
      maxLife[s] = life[s];
      size0[s] = 6 * density;
    }
  }

  private void spawnTrail(int rocket) {
    int s = spawn();
    if (s < 0) {
      return;
    }
    kind[s] = KIND_TRAIL;
    posX[s] = posX[rocket] + (random.nextFloat() - 0.5f) * 4;
    posY[s] = posY[rocket];
    velX[s] = (random.nextFloat() - 0.5f) * 1.2f;
    velY[s] = 0.4f + random.nextFloat();
    life[s] = 4 + random.nextInt(3);
    maxLife[s] = life[s];
    size0[s] = 4 * density;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (!running) {
      return;
    }
    if (launchedWaves < WAVE_COUNT && --waveDelay < 0) {
      launchRocket(launchedWaves);
      launchedWaves++;
      waveDelay = WAVE_STAGGER - 1;
    }
    boolean alive = launchedWaves < WAVE_COUNT;
    for (int i = 0; i < MAX_PARTICLES; i++) {
      if (kind[i] == KIND_FREE) {
        continue;
      }
      if (kind[i] == KIND_ROCKET) {
        velY[i] += RISE_G;
      } else if (kind[i] == KIND_TRAIL) {
        velY[i] += TRAIL_G;
        velX[i] *= TRAIL_DRAG;
      } else {
        velX[i] *= BURST_DRAG;
        velY[i] = velY[i] * BURST_DRAG + BURST_G;
      }
      posX[i] += velX[i];
      posY[i] += velY[i];
      life[i]--;
      if (kind[i] == KIND_ROCKET && velY[i] >= 0) {
        explode(posX[i], posY[i]);
        kind[i] = KIND_FREE;
        continue;
      }
      if (life[i] <= 0) {
        kind[i] = KIND_FREE;
        continue;
      }
      alive = true;
      if (kind[i] == KIND_ROCKET) {
        spawnTrail(i);
      }
      float t = life[i] / (float) maxLife[i];
      paint.setColor(t > 0.66f ? 0xFF000000 : t > 0.33f ? 0xFF666666 : 0xFFBBBBBB);
      float size = size0[i] * (0.4f + 0.6f * t);
      canvas.drawRect(posX[i], posY[i], posX[i] + size, posY[i] + size, paint);
    }
    if (!alive) {
      running = false;
    } else {
      postInvalidateDelayed(FRAME_MS);
    }
  }
}
