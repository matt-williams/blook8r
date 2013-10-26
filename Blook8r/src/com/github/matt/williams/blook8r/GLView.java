package com.github.matt.williams.blook8r;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.util.AttributeSet;

import com.github.matt.williams.android.gl.FragmentShader;
import com.github.matt.williams.android.gl.Program;
import com.github.matt.williams.android.gl.Projection;
import com.github.matt.williams.android.gl.VertexShader;

public class GLView extends GLSurfaceView implements Renderer {

    private Program mLocationProgram;
    private float mLocationY;
    private float mLocationX;
    private Projection mProjection;

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
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mLocationProgram = new Program(new VertexShader(getResources().getString(R.string.locationVertexShader)),
                                       new FragmentShader(getResources().getString(R.string.locationFragmentShader)));
        mProjection = new Projection(45.0f, 45.0f, 0.0f);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClearDepthf(1.0f);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST); // TODO: Enable again
        GLES20.glDepthMask(true);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mLocationProgram.use();
//        mLocationProgram.setUniform("matrix", mProjection.getViewMatrix());
        float[] vertices = new float[] {-0.5f, -0.5f, 0.0f,
                                        -0.5f, 0.5f, 0.0f,
                                        0.5f, -0.5f, 0.0f};
        mLocationProgram.setVertexAttrib("xyz", vertices, 3);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
    }

    public void setLocation(float x, float y) {
        mLocationX = x;
        mLocationY = y;
    }
}
