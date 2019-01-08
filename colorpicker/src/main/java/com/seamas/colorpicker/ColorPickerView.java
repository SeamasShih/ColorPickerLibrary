package com.seamas.colorpicker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {
    private Paint paintCircle;
    private Paint paintTriangle;
    private Paint paintPreview;
    private Path circle;
    private Path triangle;
    private Path preview;

    private float[] color = {0, 1, 1}; //red
    private PointF[] pointFS = new PointF[3];
    private PointF pickerCursor = new PointF();

    private Resources resources = this.getResources();
    private DisplayMetrics dm = resources.getDisplayMetrics();
    private int screenWidth = dm.widthPixels;
    private int screenHeight = dm.heightPixels;

    private float circleOuterR = screenWidth / 2 - 50;
    private float circleInnerR = screenWidth / 2 - 100;
    private float triangleLength = screenWidth * 2 / 3;

    private Region regionCircle = new Region();
    private Region regionTriangle = new Region();

    private OnColorChoosingListener onColorChoosingListener;
    private OnColorChoseListener onColorChoseListener;

    private enum REGION {
        TRIANGLE,
        CIRCLE,
        CANCEL
    }

    private REGION mode = REGION.CANCEL;

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPaintCircle();
        setPaintTriangle();
        setPaintPreview();
        setCircle();
        setTriangle();
        setPreview();
    }

    private void setPreview() {
        preview = new Path();
        preview.addRect(-100, 250, 100, 350, Path.Direction.CCW);
    }

    private void setTriangle() {
        triangle = new Path();
        for (int i = 0; i < pointFS.length; i++) {
            pointFS[i] = new PointF();
        }
        pointFS[0].set(0, -triangleLength * ((float) (Math.sqrt(3))) / 3f);
        pointFS[1].set(-triangleLength / 2, triangleLength * ((float) (Math.sqrt(3))) / 6f);
        pointFS[2].set(triangleLength / 2, triangleLength * ((float) (Math.sqrt(3))) / 6f);
        triangle.moveTo(pointFS[0].x, pointFS[0].y);
        triangle.lineTo(pointFS[1].x, pointFS[1].y);
        triangle.lineTo(pointFS[2].x, pointFS[2].y);
        triangle.close();
    }

    private void setRegionTriangle() {
        regionTriangle.set(-screenWidth, -screenHeight, screenWidth, screenHeight);
        regionTriangle.setPath(triangle, regionTriangle);
    }

    private void setCircle() {
        circle = new Path();
        circle.addCircle(0, 0, circleOuterR, Path.Direction.CCW);
        circle.addCircle(0, 0, circleInnerR, Path.Direction.CW);
    }

    private void setRegionCircle() {
        regionCircle.set(-screenWidth, -screenHeight, screenWidth, screenHeight);
        regionCircle.setPath(circle, regionCircle);
    }

    private void setPaintPreview() {
        paintPreview = new Paint();
        refreshPaintPreview();
    }

    private void refreshPaintPreview() {
        paintPreview.setColor(Color.HSVToColor(color));
    }

    private void setPaintTriangle() {
        paintTriangle = new Paint();
    }

    private void setPaintCircle() {
        paintCircle = new Paint();
        int[] colors = new int[]{
                Color.RED,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.MAGENTA,
                Color.RED
        };
        SweepGradient sweepGradient = new SweepGradient(0, 0, colors, null);
        paintCircle.setShader(sweepGradient);
        paintCircle.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        screenWidth = getMeasuredWidth();
        screenHeight = getMeasuredHeight();
        circleOuterR = screenWidth / 2f - 50;
        circleInnerR = screenWidth / 2f - 100;
        triangleLength = screenWidth * 2f / 3;
        setCircle();
        setTriangle();
        setColor(Color.HSVToColor(color));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX() - getWidth() / 2;
        int y = (int) event.getY() - getHeight() / 2;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (regionCircle.contains(x, y)) {
                    mode = REGION.CIRCLE;
                    setCircleCursor((MyMath.getTheta(x, y) + 90) % 360);
                } else if (regionTriangle.contains(x, y)) {
                    mode = REGION.TRIANGLE;
                    setPickerCursor(x, y);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                switch (mode) {
                    case CIRCLE:
                        setCircleCursor((MyMath.getTheta(x, y) + 90) % 360);
                        break;
                    case TRIANGLE:
                        if (regionTriangle.contains(x, y))
                            setPickerCursor(x, y);
                        else {
                            PointF p = calculatePointByStyle(x, y, 1);
                            setPickerCursor(p.x, p.y);
                        }
                        break;
                }
                if (onColorChoosingListener != null)
                    onColorChoosingListener.OnColorChoosing(Color.HSVToColor(color));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mode = REGION.CANCEL;
                if (onColorChoseListener != null)
                    onColorChoseListener.OnColorChose(Color.HSVToColor(color));
                break;
        }
        performClick();
        return true;
    }

    private PointF calculatePointByStyle(float x, float y, int style) {
        PointF p = new PointF(x, y);
        float theta;
        float r;
        switch (style) {
            case 0:
                theta = MyMath.getTheta(x, y);
                if (theta >= 30 && theta <= 150) {
                    r = (float) Math.tan(Math.toRadians(theta - 90)) * (float) Math.sqrt(3) / 6 + .5f;
                    p = MyMath.getPoint(pointFS[2], pointFS[1], r);
                } else if (theta > 150 && theta <= 270) {
                    r = (float) Math.tan(Math.toRadians(theta - 210)) * (float) Math.sqrt(3) / 6 + .5f;
                    p = MyMath.getPoint(pointFS[1], pointFS[0], r);
                } else {
                    r = (float) Math.tan(Math.toRadians(theta - 330)) * (float) Math.sqrt(3) / 6 + .5f;
                    p = MyMath.getPoint(pointFS[0], pointFS[2], r);
                }
                break;
            case 1:
                theta = MyMath.getTheta(x, y, pointFS[0]);
                if (theta >= 240 && theta < 300) {
                    p.set(pointFS[0]);
                } else if (theta >= 60 && theta < 120) {
                    r = (float) Math.tan(Math.toRadians(theta - 90)) * (float) Math.sqrt(3) / 2 + .5f;
                    p = MyMath.getPoint(pointFS[2], pointFS[1], r);
                }
                theta = MyMath.getTheta(x, y, pointFS[1]);
                if (theta >= 120 && theta < 180) {
                    p.set(pointFS[1]);
                } else if (theta >= 300 && theta < 360) {
                    r = (float) Math.tan(Math.toRadians(theta - 330)) * (float) Math.sqrt(3) / 2 + .5f;
                    p = MyMath.getPoint(pointFS[0], pointFS[2], r);
                }
                theta = MyMath.getTheta(x, y, pointFS[2]);
                if (theta >= 0 && theta < 60) {
                    p.set(pointFS[2]);
                } else if (theta >= 180 && theta < 240) {
                    r = (float) Math.tan(Math.toRadians(theta - 210)) * (float) Math.sqrt(3) / 2 + .5f;
                    p = MyMath.getPoint(pointFS[1], pointFS[0], r);
                }
                break;
        }
        return p;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.HSVToColor(color));
        setRegionTriangle();
        setRegionCircle();
        canvas.save();
        canvas.translate(getWidth() / 2, getHeight() / 2);
        canvas.save();
        canvas.rotate(-90);
        canvas.drawPath(circle, paintCircle);
        drawCircleCursor(canvas);
        canvas.restore();
        drawTriangle(canvas);
        drawPickerCursor(canvas);
        refreshPaintPreview();
        canvas.drawPath(preview, paintPreview);
        canvas.restore();
    }

    private void drawPickerCursor(Canvas canvas) {
        Paint outer = new Paint();
        outer.setColor(Color.GRAY);
        outer.setStrokeWidth(6);
        outer.setStyle(Paint.Style.STROKE);
        outer.setAntiAlias(true);
        Paint inner = new Paint();
        inner.setColor(Color.BLACK);
        inner.setStrokeWidth(6);
        outer.setAntiAlias(true);
        inner.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(pickerCursor.x, pickerCursor.y, 15, outer);
        canvas.drawCircle(pickerCursor.x, pickerCursor.y, 10, inner);
    }

    private void drawCircleCursor(Canvas canvas) {
        Paint line = new Paint();
        line.setStrokeWidth(9);
        line.setColor(Color.BLACK);
        line.setAntiAlias(true);
        canvas.save();
        canvas.rotate(color[0]);
        canvas.drawLine(circleInnerR - 10, 0, circleOuterR + 10, 0, line);
        canvas.restore();
    }

    private void setCircleCursor(float hue) {
        if (hue >= 0f && hue <= 360f) {
            color[0] = hue;
            invalidate();
        }
    }

    public int getColor() {
        return Color.HSVToColor(color);
    }

    public void setColor(int color) {
        float[] mColor = new float[3];
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), mColor);
        this.color = mColor;
        paintPreview.setColor(color);
        setCircleCursor(mColor[0]);
        setPickerCursor(mColor);
    }

    private void setPickerCursor(float[] hsv) {
        float s = hsv[1];
        float v = hsv[2];
        PointF p = MyMath.getPoint(pointFS[2], pointFS[0], s);
        p = MyMath.getPoint(pointFS[1], p, v);
        pickerCursor.set(p.x, p.y);
        invalidate();
    }

    private void setPickerCursor(float x, float y) {
        float v = MyMath.getDistanceFromPointToLine(new PointF(x, y), pointFS[0], pointFS[2]);
        float s = (float) Math.sqrt(3) * triangleLength / 2 - v;
        s = MyMath.getDistanceFromPointToLine(new PointF(x, y), pointFS[1], pointFS[2]) / s;
        v = 1 - v * 2 / triangleLength / (float) Math.sqrt(3);
        color[1] = s;
        color[2] = v;
        paintPreview.setColor(Color.HSVToColor(color));
        pickerCursor.set(x, y);
        invalidate();
    }

    private void drawTriangle(Canvas canvas) {
        LinearGradient shader1 = new LinearGradient(
                pointFS[0].x, pointFS[0].y, (pointFS[1].x + pointFS[2].x) / 2, (pointFS[1].y + pointFS[2].y) / 2,
                Color.HSVToColor(new float[]{color[0], 1, 1}), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        );
        SweepGradient shader2 = new SweepGradient(
                pointFS[0].x, pointFS[0].y,
                new int[]{Color.WHITE, Color.BLACK}, new float[]{1f / 6f, 2f / 6f}
        );
        paintTriangle.setShader(shader2);
        canvas.drawPath(triangle, paintTriangle);
        paintTriangle.setShader(shader1);
        canvas.drawPath(triangle, paintTriangle);
    }

    public void setOnColorChoosingListener(OnColorChoosingListener onColorChoosingListener) {
        this.onColorChoosingListener = onColorChoosingListener;
    }

    public void setOnColorChoseListener(OnColorChoseListener onColorChoseListener) {
        this.onColorChoseListener = onColorChoseListener;
    }

    public interface OnColorChoosingListener{
        void OnColorChoosing(int color);
    }

    public interface OnColorChoseListener{
        void OnColorChose(int color);
    }

    private static class MyMath {
        private static float getPoint(float a, float b, float rAB) {
            return a * (1 - rAB) + b * rAB;
        }

        private static PointF getPoint(PointF a, PointF b, float rAB) {
            return new PointF(getPoint(a.x, b.x, rAB), getPoint(a.y, b.y, rAB));
        }

        private static float getDistanceFromPointToLine(PointF p, PointF lineA, PointF lineB) {
            float a = getDistanceFromPointToPoint(p, lineA);
            float b = getDistanceFromPointToPoint(p, lineB);
            float c = getDistanceFromPointToPoint(lineA, lineB);
            float s = (a + b + c) / 2;
            return (float) Math.sqrt(Math.abs(s * (s - a) * (s - b) * (s - c))) * 2 / c;
        }

        private static float getDistanceFromPointToPoint(PointF a, PointF b) {
            return (float) Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
        }

        private static float getTheta(float x, float y) {
            return ((float) (Math.atan2(y, x) * 180 / Math.PI) + 360) % 360;
        }

        private static float getTheta(float x, float y, PointF c) {
            return getTheta(x - c.x, y - c.y);
        }
    }
}
