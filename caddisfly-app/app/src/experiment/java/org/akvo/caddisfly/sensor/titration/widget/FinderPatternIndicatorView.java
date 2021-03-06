package org.akvo.caddisfly.sensor.titration.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import org.akvo.caddisfly.R;
import org.akvo.caddisfly.sensor.qrdetector.FinderPattern;
import org.akvo.caddisfly.sensor.qrdetector.PerspectiveTransform;
import org.akvo.caddisfly.sensor.titration.decode.DecodeProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Overlay to indicate QR code corners
 */
public class FinderPatternIndicatorView extends View {
    private static int mGridStepDisplay;
    private static int mGridStepImage;
    private static double mScore = 0.0f;
    private final int GRID_H = 15;
    private final int GRID_V = 15;
    private final Paint patternPaint;
    private final Paint greenPaint;
    private final Paint greenPaintStroke;
    private final Paint yellowPaintStroke;
    private final Paint paint2;
    //    private final Paint paint3;
    private final Bitmap arrowBitmap;
    private final Bitmap closerBitmap;
    ArrayList<Point> points = new ArrayList<>();
    Point point1 = new Point(0, 0);
    Point point2 = new Point(0, 0);
    Point point3 = new Point(0, 0);
    Point point4 = new Point(0, 0);
    RectF bounds;
    float meterLeft = 0;
    float meterBottom = 0;
    private Matrix matrix = new Matrix();
    private List<FinderPattern> patterns;
    private boolean shadowGrid[][];
    private int previewScreenHeight;
    private int previewScreenWidth;
    private int decodeHeight;
    private int decodeWidth;
    private int mFinderPatternViewWidth = 0;
    private int mFinderPatternViewHeight = 0;
    private int tilt = DecodeProcessor.NO_TILT;
    private boolean showDistanceMessage = false;
    private boolean showTiltMessage;

    public FinderPatternIndicatorView(Context context) {
        this(context, null);
    }

    public FinderPatternIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FinderPatternIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        patternPaint = new Paint();
        patternPaint.setStyle(Paint.Style.STROKE);
        patternPaint.setStrokeWidth(5);
        patternPaint.setColor(getResources().getColor(R.color.spring_green));
        patternPaint.setAntiAlias(false);

        greenPaint = new Paint();
        greenPaint.setColor(getResources().getColor(R.color.spring_green));
        greenPaint.setAntiAlias(false);

        greenPaintStroke = new Paint();
        greenPaintStroke.setStyle(Paint.Style.STROKE);
        greenPaintStroke.setStrokeWidth(6);
        greenPaintStroke.setColor(getResources().getColor(R.color.spring_green));
        greenPaintStroke.setAntiAlias(true);

        yellowPaintStroke = new Paint();
        yellowPaintStroke.setStyle(Paint.Style.STROKE);
        yellowPaintStroke.setStrokeWidth(2);
        yellowPaintStroke.setColor(Color.YELLOW);
        yellowPaintStroke.setAntiAlias(false);

        paint2 = new Paint();
        paint2.setColor(Color.BLUE);
        paint2.setAlpha(125);
        paint2.setAntiAlias(false);

        arrowBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.level);
        closerBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.closer);

        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);
    }

    // Sets the various sizes.
    // screenWidth: actual size of the reported preview screen
    // screenHeight: actual size of the reported preview screen
    // previewWidth: size of the image as taken by the camera
    // previewHeight: size of the image as taken by the camera
    // The crop factor (CROP_FINDER_PATTERN_FACTOR) is about 0.75
    public void setMeasure(int screenWidth, int screenHeight, int previewWidth, int previewHeight) {
        this.previewScreenHeight = screenHeight;
        this.previewScreenWidth = screenWidth;
        this.mFinderPatternViewWidth = screenWidth;
        this.decodeWidth = previewWidth;
        this.decodeHeight = decodeHeight;
        this.mFinderPatternViewHeight = screenHeight;
//        this.mFinderPatternViewHeight = (int) Math.round(screenWidth * Constants.CROP_FINDER_PATTERN_FACTOR);

        // we divide the previewHeight into a number of parts
        mGridStepDisplay = Math.round(screenWidth / GRID_H);
        mGridStepImage = Math.round(previewHeight / GRID_H);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mFinderPatternViewWidth || 0 == mFinderPatternViewHeight) {
            setMeasuredDimension(width, height);
        } else {
            setMeasuredDimension(mFinderPatternViewWidth, mFinderPatternViewHeight);
        }
    }

    /*
     * display finder patterns on screen as green dots.
     * height and width are the actual size of the image as it came from the camera
     */
    public void showPatterns(List<FinderPattern> patternList, int tilt, boolean showTiltMessage, boolean showDistanceMessage, int width, int height) {
        this.patterns = patternList;
        this.decodeWidth = width;
        this.decodeHeight = height;
        this.tilt = tilt;
        this.showDistanceMessage = showDistanceMessage;
        this.showTiltMessage = showTiltMessage;
        invalidate();
    }

    public void clearAll() {
        this.patterns = null;
        this.tilt = DecodeProcessor.NO_TILT;
        this.showDistanceMessage = false;
        this.showTiltMessage = false;
        this.shadowGrid = null;
        mScore = (4 * mScore) / 5.0;
        invalidate();
    }

    public void showShadow(List<float[]> shadowPoints, float percentage, PerspectiveTransform cardToImageTransform) {
        shadowGrid = new boolean[GRID_H + 5][GRID_V + 5];
        int xGrid;
        int yGrid;
        if (shadowPoints != null) {
            // the points are in the coordinate system of the card (mm)
            for (float[] point : shadowPoints) {
                float[] points = new float[]{point[0], point[1]};
                // cardToImageTransform transforms from card coordinates (mm) to camera image coordinates
                cardToImageTransform.transformPoints(points);
                xGrid = (int) Math.max(0, Math.floor((this.decodeHeight - points[1]) / mGridStepImage));
                yGrid = (int) Math.floor(points[0] / mGridStepImage);
                shadowGrid[xGrid][yGrid] = true;
            }
        }
        invalidate();
    }

    public void setColor(int color) {
        patternPaint.setColor(color);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {

        if (bounds == null) {
            meterLeft = previewScreenWidth * 0.75f;
            meterBottom = (previewScreenHeight * 0.41f);

            bounds = new RectF(meterLeft - 60,
                    meterBottom - 60,
                    meterLeft + 60,
                    meterBottom + 60);
        }

        canvas.drawArc(bounds, 170, 200, false, greenPaintStroke);
        canvas.drawLine(meterLeft, meterBottom - 70,
                meterLeft, meterBottom - 50, yellowPaintStroke);

        if (patterns != null) {
            // The canvas has a rotation of 90 degrees with respect to the camera preview
            //Camera preview size is in landscape mode, canvas is in portrait mode
            //the width of the canvas corresponds to the height of the decodeSize.
            //float ratio = 1.0f * canvas.getWidth() / decodeHeight;
            float hRatio = 1.0f * previewScreenWidth / decodeHeight;
            float vRatio = 1.0f * previewScreenHeight / decodeWidth;

            for (int i = 0; i < patterns.size(); i++) {
                //The x of the canvas corresponds to the y of the pattern,
                //The y of the canvas corresponds to the x of the pattern.
                float x = previewScreenWidth - patterns.get(i).getY() * hRatio;
                float y = patterns.get(i).getX() * vRatio;

                points.get(i).x = (int) x;
                points.get(i).y = (int) y;

                canvas.drawRect(x - 8, y - 8, x + 8, y + 8, patternPaint);
                canvas.drawRect(x - 18, y - 18, x + 18, y + 18, patternPaint);
            }

            int minDistance = Integer.MAX_VALUE;
            int pointIndex = 0;
            for (int i = 1; i < points.size(); i++) {
                int theDistance = distance(points.get(0), points.get(i));
                if (theDistance < minDistance) {
                    minDistance = theDistance;
                    pointIndex = i;
                }
            }

            canvas.drawLine(points.get(0).x, points.get(0).y, points.get(pointIndex).x, points.get(pointIndex).y, patternPaint);

            minDistance = Integer.MAX_VALUE;
            pointIndex = 0;
            for (int i = 2; i < points.size(); i++) {
                int theDistance = distance(points.get(1), points.get(i));
                if (theDistance < minDistance) {
                    minDistance = theDistance;
                    pointIndex = i;
                }
            }

            canvas.drawLine(points.get(1).x, points.get(1).y, points.get(pointIndex).x, points.get(pointIndex).y, patternPaint);

            int diff = points.get(1).y - points.get(pointIndex).y;
            canvas.drawLine(meterLeft - diff, meterBottom - 60,
                    meterLeft, meterBottom, greenPaintStroke);
        }

        if (showTiltMessage) {
            matrix.reset();
            matrix.postTranslate(-arrowBitmap.getWidth() / 2, -arrowBitmap.getHeight() / 2); // Centers image
            matrix.postRotate(tilt);
            matrix.postTranslate(mFinderPatternViewWidth / 2f, mFinderPatternViewHeight / 2f);
            canvas.drawBitmap(arrowBitmap, matrix, null);
        }

        if (showDistanceMessage) {
            matrix.reset();
            matrix.postTranslate(-closerBitmap.getWidth() / 2, -closerBitmap.getHeight() / 2); // Centers image
            matrix.postTranslate(mFinderPatternViewWidth / 2f, mFinderPatternViewHeight / 2.5f);
            canvas.drawBitmap(closerBitmap, matrix, null);
        }

        if (shadowGrid != null) {
            float hRatio = 1.0f * previewScreenWidth / decodeHeight;
            float vRatio = 1.0f * previewScreenHeight / decodeWidth;
            float ratioRatio = vRatio / hRatio;
            int xTop;
            int yTop;
            for (int i = 0; i < GRID_H; i++) {
                for (int j = 0; j < GRID_V; j++) {
                    if (shadowGrid[i][j]) {
                        xTop = Math.round(i * mGridStepDisplay);
                        yTop = Math.round(j * mGridStepDisplay * ratioRatio);
                        canvas.drawRect(xTop, yTop, xTop + mGridStepDisplay, yTop + mGridStepDisplay, paint2);
                    }
                }
            }
        }
        super.onDraw(canvas);
    }

    int distance(Point point1, Point point2) {
        double deltaX = point1.y - point2.y;
        double deltaY = point1.x - point2.x;
        return (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}
