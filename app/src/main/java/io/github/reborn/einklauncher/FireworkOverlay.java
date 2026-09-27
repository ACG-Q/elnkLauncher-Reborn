package io.github.reborn.einklauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class FireworkOverlay extends View {

  private static final long FRAME_MS = 100;
  private static final int PARTICLE_COUNT = 18;
  private static final int GRAVITY = 28;
  private static final float UNIT = 0.1f;

  private static final class Particle {
    float x;
    float y;
    float vx;
    float vy;
    int life;
    int maxLife;

    Particle(float x, float y, float vx, float vy, int life) {
      this.x = x;
      this.y = y;
      this.vx = vx;
      this.vy = vy;
      this.life = life;
      this.maxLife = life;
    }
  }

  private final List<Particle> particles = new ArrayList<>();
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Random random = new Random();
  private boolean running;

  public FireworkOverlay(Context context) {
    super(context);
  }

  public FireworkOverlay(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void burst(float x, float y) {
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      double angle = Math.toRadians(-90 + (random.nextDouble() * 140 - 70));
      float speed = 4 + random.nextFloat() * 4;
      int life = 10 + random.nextInt(5);
      particles.add(new Particle(
          x, y,
          (float) (Math.cos(angle) * speed),
          (float) (Math.sin(angle) * speed),
          life));
    }
    if (!running) {
      running = true;
      postInvalidateDelayed(FRAME_MS);
    }
  }

  public void stop() {
    particles.clear();
    running = false;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (!running) {
      return;
    }
    Iterator<Particle> it = particles.iterator();
    while (it.hasNext()) {
      Particle p = it.next();
      p.x += p.vx;
      p.y += p.vy;
      p.vy += GRAVITY * UNIT;
      p.life--;
      if (p.life <= 0) {
        it.remove();
        continue;
      }
      float t = p.life / (float) p.maxLife;
      paint.setColor(t > 0.66f ? 0xFF000000 : t > 0.33f ? 0xFF666666 : 0xFFBBBBBB);
      float size = 6 * getResources().getDisplayMetrics().density;
      canvas.drawRect(p.x, p.y, p.x + size, p.y + size, paint);
    }
    if (particles.isEmpty()) {
      running = false;
    } else {
      postInvalidateDelayed(FRAME_MS);
    }
  }
}
