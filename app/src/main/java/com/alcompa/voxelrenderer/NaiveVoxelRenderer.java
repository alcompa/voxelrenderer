package com.alcompa.voxelrenderer;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.alcompa.voxelrenderer.utils.PlyObject;
import com.alcompa.voxelrenderer.utils.ShaderCompiler;
import com.alcompa.voxelrenderer.utils.VlyObject;

import static android.opengl.GLES10.GL_CCW;
import static android.opengl.GLES20.GL_LINE_LOOP;
import static android.opengl.GLES20.glFrontFace;
import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform3f;
import static android.opengl.GLES20.glUniform3fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

import androidx.core.view.GestureDetectorCompat;

public class NaiveVoxelRenderer extends BasicRenderer {
    private static final String VSHAD_FILENAME = "vertex.glslv";
    private static final String FSHAD_FILENAME = "fragment.glslf";
    private static final String VOXMODEL_FILENAME = "monu2.vly";

    private int[] VAO;
    private int shaderHandle;
    private int MVPloc;
    private int ucolor; // added
    private int uModelM;

    private float[] viewM;
    private float[] modelM;
    private float[] projM;
    private float[] MVP;
    private float[] temp;
    private float[] inverseModel;
    private int uInverseModel;

    private int drawMode;
    private int countFacesToElement;

    private float[] lightPos;
    private int uLightPos;

    private float[] eyePos;
    private int uEyePos;

    private float sideLengthOGL;
    private float[] gridSizeOGL;
    private float maxGridSizeOGL;

    private float angleY;
    private float angleIncrement;

    private float minZoom;
    private float maxZoom;
    private float zoom;
    private float minEyeZ;
    private float maxEyeZ;

    private ScaleGestureDetector scaleDetector;
    private GestureDetectorCompat gestureDetector;
    private boolean gestureDetected;

    private int[] voxelsRaw;
    private float[] paletteRaw;

    public NaiveVoxelRenderer() {
        super(0, 0, 0);

        lightPos = new float[]{-0.25f, 0.25f, 10.0f}; // TODO: check
        eyePos = new float[]{0f,0f,10f}; // TODO: check

        drawMode = GL_TRIANGLES;
        viewM = new float[16];
        modelM = new float[16];
        projM = new float[16];
        MVP = new float[16];
        temp = new float[16];
        inverseModel = new float[16];

        Matrix.setIdentityM(modelM, 0);
        Matrix.setIdentityM(viewM, 0);
        Matrix.setIdentityM(projM, 0);
        Matrix.setIdentityM(MVP, 0);
        Matrix.setIdentityM(inverseModel, 0);

        sideLengthOGL = 1.0f; // TODO: make it final
        angleY = 0.0f;
        angleIncrement = 5.0f;

        minZoom = 1.0f; // TODO: check
        maxZoom = 5.0f; // TODO: check
        zoom = 1.0f;
        minEyeZ = 0.0f; // TODO: cannot be computed now
        maxEyeZ = 10.0f; // TODO: cannot be computed now

        gestureDetected = false;
    }

    @Override
    @SuppressWarnings("ClickableViewAccessibility")
    public void setContextAndSurface(Context context, GLSurfaceView surface) {
        super.setContextAndSurface(context, surface);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override
            public boolean onScale(ScaleGestureDetector detector){
                gestureDetected = true;

                zoom *= detector.getScaleFactor();

                // Don't let the object get too small or too large.
                zoom = Math.max(minZoom, Math.min(zoom, maxZoom));
                Log.v(TAG, "ScaleGesture: zoom set on " + Float.toString(zoom));
                surface.invalidate(); // TODO: check if it calls onSurfaceCreated/Changed
                return true;
            }
        });

        gestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY){
                gestureDetected = true;

                if(distanceX > 0){
                    Log.v(TAG, "Scroll gesture: swipe left");
                    angleY -= angleIncrement;
                } else {
                    Log.v(TAG, "Scroll gesture: swipe right");
                    angleY += angleIncrement;
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent event){
                gestureDetected = true;

                if (drawMode == GL_TRIANGLES)
                    drawMode = GL_LINE_LOOP;
                else drawMode = GL_TRIANGLES;

                Log.v("TAG", "Drawing " + (drawMode == GL_TRIANGLES ? "Triangles" : "Lines"));
            }
        });

        surface.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Let the ScaleGestureDetector and GestureDetector inspect all events.
                scaleDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);

                int action = event.getActionMasked();

                if (action == MotionEvent.ACTION_UP){
                    if(gestureDetected) {
                        gestureDetected = false;
                    } else {
                        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                        Display display = wm.getDefaultDisplay(); // TODO: consider also view.getDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        int screenWidth = size.x;

                        Log.v(TAG, "Display size: " + size.toString());

                        if (event.getX() < screenWidth / 2.0f) {
                            Log.v(TAG, "Touch left");
                            angleY -= angleIncrement;
                        } else {
                            Log.v(TAG, "Touch right");
                            angleY += angleIncrement;
                        }
                    }
                }

                return true;
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        super.onSurfaceChanged(gl10, w, h);
        float aspect = ((float) w) / ((float) (h == 0 ? 1 : h));

        Matrix.perspectiveM(projM, 0, 45f, aspect, 0.1f, 500f);

        Matrix.setLookAtM(viewM, 0, eyePos[0], eyePos[1], eyePos[2],
                0, 0, 0,
                0, 1, 0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);

        /* Compile vertex and fragment shader */
        InputStream isV = null;
        InputStream isF = null;

        try {
            isV = context.getAssets().open(VSHAD_FILENAME);
            isF = context.getAssets().open(FSHAD_FILENAME);
            shaderHandle = ShaderCompiler.createProgram(isV, isF);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        if (shaderHandle == -1) {
            System.exit(-1);
        }

        /* Load .ply model */
        InputStream is;
        float[] vertices = null;
        int[] indices = null;

        try {
            is = context.getAssets().open("pcube.ply");
            PlyObject po = new PlyObject(is);
            po.parse();

            Log.v(getClass().getSimpleName(), po.toString());

            vertices = po.getVertices();
            indices = po.getIndices();
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            Log.e(getClass().getSimpleName(), "Failed to load .ply model");
            System.exit(-1);
        }

        /* Load .vly model */
        int[] gridSizeVLY = null;

        try {
            is = context.getAssets().open(VOXMODEL_FILENAME);
            VlyObject vo = new VlyObject(is);
            vo.parse();

            Log.v(getClass().getSimpleName(), vo.toString());

            voxelsRaw = vo.getVoxelsRaw();
            paletteRaw = vo.getPaletteRaw();
            gridSizeVLY = vo.getGridSize();
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            Log.e(getClass().getSimpleName(), "Failed to load .vly model");
            System.exit(-1);
        }

        /* Precompute things common for all voxels */
        gridSizeOGL = new float[]{gridSizeVLY[0]*sideLengthOGL, gridSizeVLY[2]*sideLengthOGL, gridSizeVLY[1]*sideLengthOGL}; // swap axis -2 with -1
        maxGridSizeOGL = Math.max(Math.max(gridSizeOGL[0], gridSizeOGL[1]), gridSizeOGL[2]);
        minEyeZ = maxGridSizeOGL;
        maxEyeZ = maxGridSizeOGL * 3.0f;
        eyePos = new float[]{0.0f, 0.0f, maxEyeZ};

        /* Resource allocation and initialization */
        FloatBuffer vertexData =
                ByteBuffer.allocateDirect(vertices.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(vertices);
        vertexData.position(0);

        IntBuffer indexData =
                ByteBuffer.allocateDirect(indices.length * Integer.BYTES)
                        .order(ByteOrder.nativeOrder())
                        .asIntBuffer();
        indexData.put(indices);
        indexData.position(0);

        countFacesToElement = indices.length; // TODO: Why faces == indices.length?


        VAO = new int[1]; // one VAO to bind both vpos and normals
        int[] VBO = new int[2]; //0: vpos, 1: indices

        glGenBuffers(2, VBO, 0);

        GLES30.glGenVertexArrays(1, VAO, 0);

        GLES30.glBindVertexArray(VAO[0]);
        glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
        glBufferData(GL_ARRAY_BUFFER, Float.BYTES * vertexData.capacity(),
                vertexData, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 0); // vpos
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES); // normal
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[1]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES * indexData.capacity(), indexData,
                GL_STATIC_DRAW);

        GLES30.glBindVertexArray(0);

        MVPloc = glGetUniformLocation(shaderHandle, "MVP");
        ucolor = glGetUniformLocation(shaderHandle, "color");
        uModelM = glGetUniformLocation(shaderHandle, "modelMatrix");
        uInverseModel = glGetUniformLocation(shaderHandle, "inverseModel");
        uLightPos = glGetUniformLocation(shaderHandle,"lightPos");
        uEyePos = glGetUniformLocation(shaderHandle,"eyePos");

        /* Pre load uniform values */
        glUseProgram(shaderHandle);
        glUniform3fv(uLightPos,1,lightPos,0);
        glUniform3fv(uEyePos,1,eyePos,0);
        glUseProgram(0);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(shaderHandle);
        GLES30.glBindVertexArray(VAO[0]);

        for(int i = 0; i < voxelsRaw.length; i+=VlyObject.VOXEL_DATA_SIZE){
            /* Compute VP part of MVP */
            Matrix.setLookAtM(viewM, 0, 0, 0f, minEyeZ + (maxEyeZ-minEyeZ) * (maxZoom-zoom) / (maxZoom-minZoom),
                    0, 0, 0,
                    0, 1, 0);
            Matrix.multiplyMM(temp, 0, projM, 0, viewM, 0);

            /* Compute model matrix */
            Matrix.setIdentityM(modelM, 0);
            Matrix.rotateM(modelM, 0, angleY, 0.0f, 1.0f, 0.0f);

            // sidelen and voxraw signs are equal, gridsize/2 opposite
            Matrix.translateM(modelM, 0,
                    gridSizeOGL[0]/2.0f - sideLengthOGL/2.0f,
                    -gridSizeOGL[1]/2.0f + sideLengthOGL/2.0f,
                    gridSizeOGL[2]/2.0f - sideLengthOGL/2.0f
            );

            Matrix.translateM(modelM,
                    0,
                    -voxelsRaw[i]*sideLengthOGL,
                    voxelsRaw[i+2]*sideLengthOGL, // since Z is the up axis in .vly
                    -voxelsRaw[i+1]*sideLengthOGL
                    );

            /* Send model matrix */
            glUniformMatrix4fv(uModelM,1,false, modelM,0);

            /* Compute and send MVP */
            Matrix.multiplyMM(MVP, 0, temp, 0, modelM, 0);
            glUniformMatrix4fv(MVPloc, 1, false, MVP, 0);

            /* Compute and send T(M^-1) */
            Matrix.invertM(inverseModel, 0,modelM,0);
            glUniformMatrix4fv(uInverseModel,1,true, inverseModel,0); // transpose while send

            int paletteIdx = voxelsRaw[i + VlyObject.VOXEL_DATA_SIZE - 1];
            glUniform3f(
                    ucolor,
                    paletteRaw[paletteIdx * VlyObject.COLOR_DATA_SIZE],
                    paletteRaw[paletteIdx * VlyObject.COLOR_DATA_SIZE + 1],
                    paletteRaw[paletteIdx * VlyObject.COLOR_DATA_SIZE + 2]
            );

            glDrawElements(drawMode, countFacesToElement, GL_UNSIGNED_INT, 0);
        }

        GLES30.glBindVertexArray(0);
        glUseProgram(0);
    }

}

