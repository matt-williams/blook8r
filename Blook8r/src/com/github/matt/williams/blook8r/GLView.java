package com.github.matt.williams.blook8r;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.github.matt.williams.android.gl.BitmapTexture;
import com.github.matt.williams.android.gl.FragmentShader;
import com.github.matt.williams.android.gl.Program;
import com.github.matt.williams.android.gl.Projection;
import com.github.matt.williams.android.gl.Utils;
import com.github.matt.williams.android.gl.VertexShader;

public class GLView extends GLSurfaceView implements Renderer {

    private static final float MIN_ALTITUDE = -12.0f;
    private static final float MAX_ALTITUDE = 12.0f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        android.util.Log.e(TAG, "Got TouchEvent " + event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mTouched = true;
        } else if ((event.getActionMasked() == MotionEvent.ACTION_MOVE) &&
                   (event.getHistorySize() >= 1)) {
            mBearing += (event.getX(0) - event.getHistoricalX(0, 0)) / 6.0;
            mBearing = normalizeBearing(mBearing);
            mAltitude -= (event.getY(0) - event.getHistoricalY(0, 0)) / 50.0;
            if (mAltitude < MIN_ALTITUDE) {
                mAltitude = MIN_ALTITUDE;
            } else if (mAltitude > MAX_ALTITUDE) {
                mAltitude = MAX_ALTITUDE;
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            mTouched = false;
        }
        super.onTouchEvent(event);
        return true;
    }

    private static final String TAG = "GLView";
    private Program mMapProgram;
    private Program mLocationProgram;
    private float mLocationY;
    private float mLocationX;
    private Projection mProjection;
    private float[] mVertices;
    private float[] mOtherVertices;
    private float mDesiredBearing;
    private float mBearing;
    private float mAltitude;
    private BitmapTexture mLocationTexture;
    private BitmapTexture mJewleryTexture;
    private float mTargetLocationX = 1.5f;
    private float mTargetLocationY = 3.5f;
    private boolean mTouched;
    private BitmapTexture mJewleryTextureLow;
    private BitmapTexture mLocationTextureLow;

    private static final double CENTRE_X;
    private static final double CENTRE_Y;
    private static final double RADIUS;
    static {
        CENTRE_X = (-0.01975621696528207 + -0.01902470873661133 + -0.0191509753757757 + -0.01987457772519519) / 4;
        CENTRE_Y = (51.50525850486366 + 51.5051846939183 + 51.50471551467772 + 51.50479160522183) / 4;
        RADIUS = (Math.pow(Math.pow(-0.01975621696528207 - CENTRE_X, 2) + Math.pow(51.50525850486366 - CENTRE_Y, 2), 0.5) +
                  Math.pow(Math.pow(-0.01902470873661133 - CENTRE_X, 2) + Math.pow(51.5051846939183 - CENTRE_Y, 2), 0.5) +
                  Math.pow(Math.pow(-0.0191509753757757 - CENTRE_X, 2) + Math.pow(51.50471551467772 - CENTRE_Y, 2), 0.5) +
                  Math.pow(Math.pow(-0.01987457772519519 - CENTRE_X, 2) + Math.pow(51.50479160522183 - CENTRE_Y, 2), 0.5)) / 4;
    }

    public GLView(Context context) {
        super(context);
        initialize();
    }

    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        setRenderer(this);
        try {
            JSONArray jsonArray = (JSONArray)new JSONTokener(JSONUtils.readStream(getContext().getAssets().open("map.json"))).nextValue();
            float[] vertices = new float[jsonArray.length()];
            for (int ii = 0; ii < vertices.length; ii++) {
                vertices[ii] = (float)jsonArray.getDouble(ii);
            }
            mVertices = vertices;

            jsonArray = (JSONArray)new JSONTokener(JSONUtils.readStream(getContext().getAssets().open("other_map.json"))).nextValue();
            vertices = new float[jsonArray.length()];
            for (int ii = 0; ii < vertices.length; ii++) {
                vertices[ii] = (float)jsonArray.getDouble(ii);
            }
            mOtherVertices = vertices;
        } catch (JSONException e) {
            android.util.Log.e(TAG, "Failed to parse map.json", e);
        } catch (IOException e) {
            android.util.Log.e(TAG, "Failed to read map.json", e);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mMapProgram = new Program(new VertexShader(getResources().getString(R.string.mapVertexShader)),
                                       new FragmentShader(getResources().getString(R.string.mapFragmentShader)));
        mLocationProgram = new Program(new VertexShader(getResources().getString(R.string.locationVertexShader)),
                                       new FragmentShader(getResources().getString(R.string.locationFragmentShader)));

        mLocationTexture = new BitmapTexture(BitmapFactory.decodeResource(getResources(), R.raw.male, Utils.BITMAP_OPTIONS));
        mLocationTextureLow = new BitmapTexture(BitmapFactory.decodeResource(getResources(), R.raw.male_low, Utils.BITMAP_OPTIONS));
        mJewleryTexture = new BitmapTexture(BitmapFactory.decodeResource(getResources(), R.raw.jewelry, Utils.BITMAP_OPTIONS));
        mJewleryTextureLow = new BitmapTexture(BitmapFactory.decodeResource(getResources(), R.raw.jewelry_low, Utils.BITMAP_OPTIONS));

        mProjection = new Projection(40.0f, 60.0f, 0.0f);
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.rotateM(matrix, 0, 90.0f, -1.0f, 0.0f, 0.0f);
        mProjection.setRotationMatrix(matrix);

        GLES20.glEnable(GLES20.GL_BLEND);
        Utils.checkErrors("glEnable(GLES20.GL_BLEND)");
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        Utils.checkErrors("glBlendFunc");

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        Utils.checkErrors("glEnable(GLES20.GL_DEPTH_TEST)");
        GLES20.glDepthMask(true);
        Utils.checkErrors("glDepthMask");

        GLES20.glClearColor(0, 0, 0.25f, 0);
        Utils.checkErrors("glClearColor");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public void setHighLowColors(Program program, float altitude) {
        if (altitude < -6.0f) {
            program.setUniform("lowColor", 0, 0, 0, 1);
            program.setUniform("highColor", 0.333f, 0.333f, 0.333f, 1);
        } else if (altitude < -2.0f) {
            program.setUniform("lowColor", 0, 0, 0, 1);
            program.setUniform("highColor", (altitude + 8) / 6, (altitude + 8) / 6, (altitude + 8) / 6, 1);
        } else if (altitude < 2.0f) {
            program.setUniform("lowColor", 0, 0, 0, 1);
            program.setUniform("highColor", 1, 1, 1, 1);
        } else if (altitude < 6.0f) {
            program.setUniform("lowColor", (altitude - 2) / 8, (altitude - 2) / 8, (altitude - 2) / 8, 1);
            program.setUniform("highColor", (6 - altitude) / 4, (6 - altitude) / 4, (6 - altitude) / 4, 1);
        } else {
            program.setUniform("lowColor", 0.5f, 0.5f, 0.5f, 1);
            program.setUniform("highColor", 0, 0, 0, 1);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (!mTouched) {
            mBearing = slideToBearing(mBearing, mDesiredBearing, 0.05f);
            float desiredAltitude = Math.round(mAltitude / 6.0f) * 6.0f;
            mAltitude = mAltitude * 0.95f + desiredAltitude * 0.05f;
        }

        float[] matrix = new float[16];
        System.arraycopy(mProjection.getViewMatrix(), 0, matrix, 0, 16);
        Matrix.rotateM(matrix, 0, -15.0f, -1.0f, 0.0f, 0.0f);
        Matrix.translateM(matrix, 0, 0, 15.0f, -4.0f);
        Matrix.rotateM(matrix, 0, mBearing + 65, 0.0f, 0.0f, 1.0f); // = 65 accounts for difference between actual compass directions and model
        Matrix.translateM(matrix, 0, -mLocationX, -mLocationY, 0);

        Matrix.translateM(matrix, 0, 0, 0, mAltitude - 12.0f);
        mMapProgram.use();
        mMapProgram.setUniform("matrix", matrix);
        setHighLowColors(mMapProgram, mAltitude - 12.0f);
        mMapProgram.setVertexAttrib("xyz", mOtherVertices, 3);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mOtherVertices.length / 3);
        Matrix.translateM(matrix, 0, 0, 0, 6.0f);

        mMapProgram.use();
        mMapProgram.setUniform("matrix", matrix);
        setHighLowColors(mMapProgram, mAltitude - 6.0f);
        mMapProgram.setVertexAttrib("xyz", mOtherVertices, 3);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mOtherVertices.length / 3);
        Matrix.translateM(matrix, 0, 0, 0, 6.0f);

        mMapProgram.use();
        mMapProgram.setUniform("matrix", matrix);
        setHighLowColors(mMapProgram, mAltitude);
        mMapProgram.setVertexAttrib("xyz", mVertices, 3);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mVertices.length / 3);

        mLocationProgram.use();
        mLocationProgram.setUniform("matrix", matrix);
        float[] vertices;
        if (mAltitude < 4.0f) {
            mLocationTexture.use(GLES20.GL_TEXTURE0);
            vertices = new float[] {mLocationX, mLocationY, 0.5f};
        } else {
            mLocationTextureLow.use(GLES20.GL_TEXTURE0);
            vertices = new float[] {mLocationX, mLocationY, -1.0f};
        }
        mLocationProgram.setUniform("texture", 0);
        mLocationProgram.setVertexAttrib("xyz", vertices, 3);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

        Matrix.translateM(matrix, 0, 0, 0, 6.0f);
        mMapProgram.use();
        mMapProgram.setUniform("matrix", matrix);
        setHighLowColors(mMapProgram, mAltitude + 6.0f);
        mMapProgram.setVertexAttrib("xyz", mOtherVertices, 3);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mOtherVertices.length / 3);

        mLocationProgram.use();
        mLocationProgram.setUniform("matrix", matrix);
        if (mAltitude < -2.0f) {
            mJewleryTexture.use(GLES20.GL_TEXTURE0);
            vertices = new float[] {mTargetLocationX, mTargetLocationY, 0.5f};
        } else {
            mJewleryTextureLow.use(GLES20.GL_TEXTURE0);
            vertices = new float[] {mTargetLocationX, mTargetLocationY, -1.0f};
        }
        mLocationProgram.setUniform("texture", 0);
        mLocationProgram.setVertexAttrib("xyz", vertices, 3);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

        Matrix.translateM(matrix, 0, 0, 0, 6.0f);
        mMapProgram.use();
        mMapProgram.setUniform("matrix", matrix);
        setHighLowColors(mMapProgram, mAltitude + 12.0f);
        mMapProgram.setVertexAttrib("xyz", mOtherVertices, 3);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mOtherVertices.length / 3);
    }

    public void setLocation(double x, double y) {
        x = (x - CENTRE_X) / RADIUS * 11;
        y = (y - CENTRE_Y) / RADIUS * 11;
        mLocationX = (float)(x * Math.cos(10 / 180 * Math.PI) + y * Math.sin(10 / 180 * Math.PI));
        mLocationY = (float)(x * -Math.sin(10 / 180 * Math.PI) + y * Math.cos(10 / 180 * Math.PI));
        android.util.Log.e(TAG, "Got OpenGL location " + mLocationX + ", " + mLocationY);
    }

    public void setTargetLocation(double x, double y) {
        x = (x - CENTRE_X) / RADIUS * 11;
        y = (y - CENTRE_Y) / RADIUS * 11;
        mTargetLocationX = (float)(x * Math.cos(10 / 180 * Math.PI) + y * Math.sin(10 / 180 * Math.PI));
        mTargetLocationY = (float)(x * -Math.sin(10 / 180 * Math.PI) + y * Math.cos(10 / 180 * Math.PI));
        android.util.Log.e(TAG, "Got OpenGL target location " + mLocationX + ", " + mLocationY);
    }

    public float slideToBearing(float current, float desired, float alpha) {
        if ((current < -90) &&
            (desired > 90)) {
            desired -= 360;
        }
        if ((current > 90) &&
            (desired < -90)) {
            desired += 360;
        }
        current = alpha * desired + (1 - alpha) * current;
        return normalizeBearing(current);
    }

    private float normalizeBearing(float current) {
        if (current < -180) {
            current += 360;
        }
        if (current > 180) {
            current -= 360;
        }
        return current;
    }

    public void setBearing(float bearing) {
        mDesiredBearing = bearing;
    }
}
