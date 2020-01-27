package com.liyan.opengdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {

    private int mWidth;
    private int mHeight;
    private SurfaceTexture mSurfaceTexture;
    private float[] mtx = new float[16];
    private int mProgramId;
    private String vertex = "attribute vec4 vPosition;\n" +
            "    attribute vec4 vCoord;\n" +
            "    uniform mat4 vMatrix;\n" +
            "    varying vec2 aCoord;\n" +
            "    void main(){\n" +
            "        gl_Position = vPosition; \n" +
            "        //\n" +
            "        aCoord = (vMatrix * vCoord).xy;\n" +
            "    }";
    private String frag = "#extension GL_OES_EGL_image_external:require\n" +
            "    precision mediump float;\n" +
            "    varying vec2 aCoord;\n" +
            "    uniform samplerExternalOES vTexture;\n" +
            "    void main() {\n" +
            "        gl_FragColor = texture2D(vTexture,aCoord);\n" +
            "    }";
    private int vPosition;
    private int vCoord;
    private int vMatrix;
    private int vTexture;
    private FloatBuffer mGLVertexBuffer;
    private FloatBuffer mGLTextureBuffer;
    private int[] mTexture;
    private CameraHelper mCameraHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final GLSurfaceView glSurfaceview = findViewById(R.id.glsurfaceview);

        mGLVertexBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLVertexBuffer.clear();
        float[] VERTEX = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f,
        };
        mGLVertexBuffer.put(VERTEX);
        mGLTextureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTextureBuffer.clear();
        float[] TEXTURE = {   //屏幕坐标
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                0.0f, 1.0f,
        };
        mGLTextureBuffer.put(TEXTURE);

        glSurfaceview.setEGLContextClientVersion(2);
        glSurfaceview.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        glSurfaceview.setRenderer(new GLSurfaceView.Renderer() {


            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                mTexture = new int[1];
                GLES20.glGenTextures(1, mTexture, 0); // 创建一个纹理id
                mSurfaceTexture = new SurfaceTexture(mTexture[0]);
                mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        glSurfaceview.requestRender();
                    }
                });
                //初始化的操作
                mCameraHelper = new CameraHelper(Camera.CameraInfo.CAMERA_FACING_FRONT);
                mCameraHelper.startPreview(mSurfaceTexture);

                mProgramId = creatProgram(vertex, frag);
                vPosition = GLES20.glGetAttribLocation(mProgramId, "vPosition");
                vCoord = GLES20.glGetAttribLocation(mProgramId, "vCoord");
                vMatrix = GLES20.glGetUniformLocation(mProgramId, "vMatrix");
                vTexture = GLES20.glGetUniformLocation(mProgramId, "vTexture");

            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                mWidth = width;
                mHeight = height;
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                //清理屏幕：可以清理成指定的颜色
                GLES20.glClearColor(0, 0, 0, 0);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                mSurfaceTexture.updateTexImage();//更新纹理成为最新的数据
                mSurfaceTexture.getTransformMatrix(mtx);

                GLES20.glViewport(0, 0, mWidth, mHeight);

                GLES20.glUseProgram(mProgramId);

                mGLVertexBuffer.position(0);
                //  函数的意义是为 Atrtribute 变量制定VBO中的数据.  //屏幕顶点坐标
                GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, mGLVertexBuffer);//设置顶点数据
                GLES20.glEnableVertexAttribArray(vPosition);

                mGLTextureBuffer.position(0);
                GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer); //纹理顶点坐标
                GLES20.glEnableVertexAttribArray(vCoord);

                //变换矩阵
                GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture[0]);
                GLES20.glUniform1i(vTexture, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            }
        });

    }

    private int creatProgram(String vsi, String fsi) {

        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vShader, vsi);
        GLES20.glCompileShader(vShader);

        int[] status = new int[1];
        GLES20.glGetShaderiv(vShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            throw new IllegalStateException("顶点着色器创建失败！");
        }

        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader, fsi);
        GLES20.glCompileShader(fShader);
        GLES20.glGetShaderiv(fShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            throw new IllegalStateException("片元着色器创建失败");
        }

        int mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vShader);
        GLES20.glAttachShader(mProgram, fShader);
        GLES20.glLinkProgram(mProgram);
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            throw new IllegalStateException("link program:" + GLES20.glGetProgramInfoLog(mProgram));
        }

        GLES20.glDeleteShader(vShader);
        GLES20.glDeleteShader(fShader);

        return mProgram;
    }
}
