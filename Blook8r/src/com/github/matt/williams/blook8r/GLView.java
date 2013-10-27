package com.github.matt.williams.blook8r;


import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.AttributeSet;

import com.github.matt.williams.android.gl.FragmentShader;
import com.github.matt.williams.android.gl.Program;
import com.github.matt.williams.android.gl.Projection;
import com.github.matt.williams.android.gl.Utils;
import com.github.matt.williams.android.gl.VertexShader;

public class GLView extends GLSurfaceView implements Renderer {

    private static final String TAG = "GLView";
    private Program mMapProgram;
    private Program mLocationProgram;
    private float mLocationY;
    private float mLocationX;
    private Projection mProjection;
    private float[] mVertices;
    private float mBearing;

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

        GLES20.glClearColor(0, 0, 0, 0);
        Utils.checkErrors("glClearColor");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float[] matrix = new float[16];
        System.arraycopy(mProjection.getViewMatrix(), 0, matrix, 0, 16);
        Matrix.rotateM(matrix, 0, -30.0f, -1.0f, 0.0f, 0.0f);
        Matrix.translateM(matrix, 0, 0, 10.0f, -10.0f);
        Matrix.rotateM(matrix, 0, mBearing, 0.0f, 0.0f, 1.0f);
        Matrix.translateM(matrix, 0, -mLocationX, -mLocationY, 0);

        mMapProgram.use();
        mMapProgram.setUniform("matrix", matrix);
        mMapProgram.setVertexAttrib("xyz", mVertices, 3);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mVertices.length / 3);

        mLocationProgram.use();
        mLocationProgram.setUniform("matrix", matrix);
        float[] vertices = new float[] {mLocationX, mLocationY, 1.2f};
        mLocationProgram.setVertexAttrib("xyz", vertices, 3);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

    }

    public void setLocation(double x, double y) {
        mLocationX = (float)((x + 0.01945718950902564f) / (-0.01945659122807086 - -0.01945718950902564) * 14 - 7);
        mLocationY = (float)((y - 51.5049040161022f) / (51.50490606936728 - 51.5049040161022) * 14 - 7);
        android.util.Log.e(TAG, "Got OpenGL location " + mLocationX + ", " + mLocationY);
    }

    public void setBearing(float bearing) {
        mBearing = 0.1f * bearing + 0.9f * mBearing;
    }
}
