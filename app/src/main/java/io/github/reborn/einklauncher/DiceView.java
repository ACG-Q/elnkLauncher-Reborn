package io.github.reborn.einklauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class DiceView extends View {

  private static final int FRAMES = 6;
  private static final long FRAME_MS = 150;
  private static final float STEP_DEG = 60f;
  private static final float CAM_DIST = 4.5f;
  private static final float PIP_SPACING = 0.5f;
  private static final float PIP_RADIUS = 0.16f;
  private static final int[][] PIPS = {
      {}, {4}, {0, 8}, {0, 4, 8}, {0, 2, 6, 8}, {0, 2, 4, 6, 8}, {0, 2, 3, 5, 6, 8}
  };

  private static final class Face {
    final int[] corners;
    final float[] normal;
    final float[] center;
    final float[] u;
    final float[] v;
    final int number;
    final int color;

    Face(int[] corners, float[] normal, float[] center, float[] u, float[] v,
        int number, int color) {
      this.corners = corners;
      this.normal = normal;
      this.center = center;
      this.u = u;
      this.v = v;
      this.number = number;
      this.color = color;
    }
  }

  private static final int WHITE = 0xFFFFFFFF;
  private static final int LIGHT_GRAY = 0xFFD9D9D9;
  private static final int DARK_GRAY = 0xFFB3B3B3;

  private static final Face[] FACES = {
      new Face(new int[]{4, 5, 6, 7}, new float[]{0, 0, 1},
          new float[]{0, 0, 1}, new float[]{1, 0, 0}, new float[]{0, 1, 0}, 1, WHITE),
      new Face(new int[]{0, 1, 2, 3}, new float[]{0, 0, -1},
          new float[]{0, 0, -1}, new float[]{-1, 0, 0}, new float[]{0, 1, 0}, 6, WHITE),
      new Face(new int[]{0, 1, 4, 5}, new float[]{0, -1, 0},
          new float[]{0, -1, 0}, new float[]{1, 0, 0}, new float[]{0, 0, 1}, 2, WHITE),
      new Face(new int[]{3, 2, 6, 7}, new float[]{0, 1, 0},
          new float[]{0, 1, 0}, new float[]{1, 0, 0}, new float[]{0, 0, -1}, 5, DARK_GRAY),
      new Face(new int[]{1, 2, 6, 5}, new float[]{1, 0, 0},
          new float[]{1, 0, 0}, new float[]{0, 0, -1}, new float[]{0, 1, 0}, 3, LIGHT_GRAY),
      new Face(new int[]{0, 3, 7, 4}, new float[]{-1, 0, 0},
          new float[]{-1, 0, 0}, new float[]{0, 0, 1}, new float[]{0, 1, 0}, 4, LIGHT_GRAY),
  };

  private static final float[][] VERTICES = {
      {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
      {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1},
  };

  private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint pipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path path = new Path();

  private float rotX = -28f;
  private float rotY = 38f;
  private boolean spinning;
  private int frame;

  public DiceView(Context context) {
    super(context);
    init();
  }

  public DiceView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    fillPaint.setStyle(Paint.Style.FILL);
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
    strokePaint.setStrokeJoin(Paint.Join.ROUND);
    strokePaint.setStrokeCap(Paint.Cap.ROUND);
    strokePaint.setColor(0xFF000000);
    pipPaint.setStyle(Paint.Style.FILL);
    pipPaint.setColor(0xFF000000);
  }

  public boolean isSpinning() {
    return spinning;
  }

  public void tumble() {
    if (spinning) {
      return;
    }
    spinning = true;
    frame = 0;
    postInvalidateDelayed(FRAME_MS);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (spinning) {
      frame++;
      rotX += STEP_DEG;
      rotY += STEP_DEG;
      if (frame >= FRAMES) {
        spinning = false;
      } else {
        postInvalidateDelayed(FRAME_MS);
      }
    }

    float cx = getWidth() / 2f;
    float cy = getHeight() / 2f;
    float half = Math.min(getWidth(), getHeight()) * 0.345f;

    float[][] rotVerts = new float[VERTICES.length][3];
    for (int i = 0; i < VERTICES.length; i++) {
      rotVerts[i] = rotate(VERTICES[i][0], VERTICES[i][1], VERTICES[i][2]);
    }

    Face[] sorted = FACES.clone();
    final float[] centersZ = new float[sorted.length];
    for (int i = 0; i < sorted.length; i++) {
      float[] c = rotate(sorted[i].center[0], sorted[i].center[1], sorted[i].center[2]);
      centersZ[i] = c[2];
    }
    for (int i = 1; i < sorted.length; i++) {
      Face keyFace = sorted[i];
      float keyZ = centersZ[i];
      int j = i - 1;
      while (j >= 0 && centersZ[j] > keyZ) {
        sorted[j + 1] = sorted[j];
        centersZ[j + 1] = centersZ[j];
        j--;
      }
      sorted[j + 1] = keyFace;
      centersZ[j + 1] = keyZ;
    }

    float density = getResources().getDisplayMetrics().density;
    for (Face face : sorted) {
      float[] n = rotate(face.normal[0], face.normal[1], face.normal[2]);
      if (n[2] <= 0) {
        continue;
      }
      float[] pts = new float[8];
      for (int i = 0; i < 4; i++) {
        float[] p = project(rotVerts[face.corners[i]], cx, cy, half);
        pts[i * 2] = p[0];
        pts[i * 2 + 1] = p[1];
      }
      path.reset();
      addRoundedQuad(path, pts, 9 * density * 0.6f);
      fillPaint.setColor(face.color);
      canvas.drawPath(path, fillPaint);
      canvas.drawPath(path, strokePaint);

      for (int idx : PIPS[face.number]) {
        int gi = idx % 3;
        int gj = idx / 3;
        float lx = face.center[0] + (gi - 1) * PIP_SPACING * face.u[0]
            + (gj - 1) * PIP_SPACING * face.v[0];
        float ly = face.center[1] + (gi - 1) * PIP_SPACING * face.u[1]
            + (gj - 1) * PIP_SPACING * face.v[1];
        float lz = face.center[2] + (gi - 1) * PIP_SPACING * face.u[2]
            + (gj - 1) * PIP_SPACING * face.v[2];
        float[] r = rotate(lx, ly, lz);
        float[] sp = project(r, cx, cy, half);
        canvas.drawCircle(sp[0], sp[1], PIP_RADIUS * half * 0.6f, pipPaint);
      }
    }
  }

  private float[] rotate(float x, float y, float z) {
    float ry = (float) Math.toRadians(rotY);
    float rx = (float) Math.toRadians(rotX);
    float x1 = x * (float) Math.cos(ry) + z * (float) Math.sin(ry);
    float z1 = -x * (float) Math.sin(ry) + z * (float) Math.cos(ry);
    float y2 = y * (float) Math.cos(rx) - z1 * (float) Math.sin(rx);
    float z2 = y * (float) Math.sin(rx) + z1 * (float) Math.cos(rx);
    return new float[]{x1, y2, z2};
  }

  private float[] project(float[] p, float cx, float cy, float half) {
    float scale = CAM_DIST / (CAM_DIST - p[2]);
    return new float[]{cx + p[0] * scale * half, cy + p[1] * scale * half};
  }

  private void addRoundedQuad(Path path, float[] pts, float radius) {
    for (int i = 0; i < 4; i++) {
      float px = pts[i * 2];
      float py = pts[i * 2 + 1];
      int pi = (i + 3) % 4;
      int ni = (i + 1) % 4;
      float[] enter = trim(px, py, pts[pi * 2], pts[pi * 2 + 1], radius);
      float[] exit = trim(px, py, pts[ni * 2], pts[ni * 2 + 1], radius);
      if (i == 0) {
        path.moveTo(enter[0], enter[1]);
      } else {
        path.lineTo(enter[0], enter[1]);
      }
      path.quadTo(px, py, exit[0], exit[1]);
    }
    path.close();
  }

  private float[] trim(float px, float py, float ox, float oy, float radius) {
    float dx = ox - px;
    float dy = oy - py;
    float len = (float) Math.sqrt(dx * dx + dy * dy);
    if (len < 0.001f) {
      return new float[]{px, py};
    }
    float r = Math.min(radius, len * 0.5f);
    return new float[]{px + dx / len * r, py + dy / len * r};
  }
}
