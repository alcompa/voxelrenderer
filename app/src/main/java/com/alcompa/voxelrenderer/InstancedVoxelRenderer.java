package com.alcompa.voxelrenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
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

import static android.opengl.GLES20.GL_CCW;
import static android.opengl.GLES20.GL_INT;
import static android.opengl.GLES20.GL_LINE_LOOP;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_NO_ERROR;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_VERSION;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
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
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetString;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform3fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

import androidx.annotation.ColorInt;
import androidx.core.view.GestureDetectorCompat;

public class InstancedVoxelRenderer extends BasicRenderer {
    private static final String VSHAD_FILENAME = "lightinstancedvertex.glslv";
    private static final String FSHAD_FILENAME = "lightinstancedfragment.glslf";

    private String modelFilename;

    private int shaderHandle;
    private int[] VAO;
    private int[] texObjId;
    private int uTexUnit;

    private float[] viewM;
    private float[] projM;

    private float[] VP;
    private float[] axesM; // to convert vly axes to opengl axes
    private int uVP;
    private int uAxesM;

    private int countFacesToElement;

    private int numVoxels;

    private float[] eyePos;
    private int uEyePos;
    private float minEyeDistance; // min eye distance from the origin
    private float maxEyeDistance;

    private float[] lightPos;
    private int uLightPos;

    private float sideLengthOGL;
    private float[] gridSizeOGL;
    private float maxGridSize;

    private float angleY; // angle around y, positive from z to x
    private float slowAngleIncrement;
    private float fastAngleIncrement;

    private float zoom;
    private float minZoom;
    private float maxZoom;

    private int drawMode;

    private ScaleGestureDetector scaleDetector;
    private GestureDetectorCompat gestureDetector;
    private boolean gestureDetected;

    public InstancedVoxelRenderer(String modelFilename) {
        super(0, 0, 0);

        this.modelFilename = modelFilename;

        drawMode = GL_TRIANGLES;

        viewM = new float[16];
        projM = new float[16];
        VP = new float[16];
        axesM = new float[16];

        Matrix.setIdentityM(viewM, 0);
        Matrix.setIdentityM(projM, 0);
        Matrix.setIdentityM(VP, 0);
        Matrix.setIdentityM(axesM, 0);

        sideLengthOGL = 1.0f; // TODO: make it final

        angleY = 0.0f;
        slowAngleIncrement = 0.5f; // TODO: tune
        fastAngleIncrement = 5.0f; // TODO: tune

        zoom = 1.0f;
        minZoom = 1.0f; // TODO: tune
        maxZoom = 10.0f; // TODO: tune

        gestureDetected = false;
    }

    @Override
    @SuppressWarnings("ClickableViewAccessibility")
    public void setContextAndSurface(Context context, GLSurfaceView surface) {
        super.setContextAndSurface(context, surface);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                gestureDetected = true;

                zoom *= detector.getScaleFactor();
                // Don't let the object get too small or too large.
                zoom = Math.max(minZoom, Math.min(zoom, maxZoom));
                Log.v(TAG, "[onScale]: zoom set to " + Float.toString(zoom));
                surface.invalidate(); // forces update

                return true;
            }
        });

        gestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
                gestureDetected = true;

                if (distanceX > 0) {
                    Log.v(TAG, "[onScroll]: swipe left");
                    angleY -= slowAngleIncrement;
                } else {
                    Log.v(TAG, "[onScroll]: swipe right");
                    angleY += slowAngleIncrement;
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent event) {
                gestureDetected = true;

                if (drawMode == GL_TRIANGLES)
                    drawMode = GL_LINE_LOOP;
                else drawMode = GL_TRIANGLES;

                Log.v(TAG, "[onLongPress]: Drawing " + (drawMode == GL_TRIANGLES ? "Triangles" : "Lines"));
            }
        });

        surface.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Let the ScaleGestureDetector and GestureDetector inspect all events.
                scaleDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);

                int action = event.getActionMasked();

                if (action == MotionEvent.ACTION_UP) {
                    if (gestureDetected) {
                        gestureDetected = false;
                    } else {
                        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                        Display display = wm.getDefaultDisplay(); // TODO: consider also view.getDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        int screenWidth = size.x;

                        Log.v(TAG, "[onTouch]: Display size: " + size.toString());

                        if (event.getX() < screenWidth / 2.0f) {
                            Log.v(TAG, "[onTouch]: Touch left");
                            angleY -= fastAngleIncrement;
                        } else {
                            Log.v(TAG, "[onTouch]: Touch right");
                            angleY += fastAngleIncrement;
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

        Log.v(TAG, "[onSurfaceChanged]: Aspect ratio: " + Float.toString(aspect));

        Matrix.perspectiveM(projM, 0, 45f, aspect, 0.1f, 1000f);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);

        Log.v(TAG,"[onSurfaceCreated]: OPENGL VERSION " + glGetString(GL_VERSION));

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

            Log.v(TAG, po.toString());

            vertices = po.getVertices();
            indices = po.getIndices();
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load .ply model");
            System.exit(-1);
        }

        /* Load .vly model */
        int[] gridSizeVLY = null;
        int[] voxelsRaw = null;
        int[] paletteRaw = null;

        try {
            is = context.getAssets().open(modelFilename);
            VlyObject vo = new VlyObject(is);
            vo.parse();

            Log.v(TAG, vo.toString());

            voxelsRaw = vo.getVoxelsRaw();
            numVoxels = voxelsRaw.length / VlyObject.VOXEL_DATA_SIZE;
            paletteRaw = vo.getPaletteRaw();
            gridSizeVLY = vo.getGridSize();

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load .vly model");
            System.exit(-1);
        }

        /* Resource allocation and initialization */

        // VERTEX LEVEL DATA
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

        countFacesToElement = indices.length;

        // INSTANCE LEVEL DATA
        IntBuffer voxelsData =
                ByteBuffer.allocateDirect(voxelsRaw.length * Integer.BYTES)
                        .order(ByteOrder.nativeOrder())
                        .asIntBuffer();
        voxelsData.put(voxelsRaw);
        voxelsData.position(0);

        // GLOBAL LEVEL DATA

        // grid size (with OpenGL axes order)
        gridSizeOGL = new float[]{
                gridSizeVLY[0],
                gridSizeVLY[2], // swap axis -2 with -1
                gridSizeVLY[1]
        };

        maxGridSize = Math.max(Math.max(gridSizeOGL[0], gridSizeOGL[1]), gridSizeOGL[2]); // TODO: make it a local variable

        // diameter of the cylinder built by the object while rotating
        float objectDiameter = (float) Math.sqrt(gridSizeOGL[0]*gridSizeOGL[0] + gridSizeOGL[2]*gridSizeOGL[2]);

        // TODO: tune
        minEyeDistance = objectDiameter / 2.0f + 1.0f; // add 1.0f to avoid touching the object
        maxEyeDistance = maxGridSize * 4.0f; // keeps in account also object height

        eyePos = new float[]{0.0f, 0.0f, 0.0f}; // TODO: these values are ignored, they are computed again using zoom
        zoom = (maxZoom - minZoom) / 2.0f;
        lightPos = new float[]{0.0f, gridSizeOGL[1] * 2.0f, maxGridSize * 2.0f}; // TODO: tune lightPos[1]

        // Axes transformation: R @ T @ ... @ vertex
        Matrix.rotateM(axesM, 0, 90, 1, 0, 0);

        Matrix.translateM(axesM, 0,
                +(gridSizeVLY[0] / 2.0f - sideLengthOGL / 2.0f),
                +(gridSizeVLY[1] / 2.0f - sideLengthOGL / 2.0f),
                +(gridSizeVLY[2] / 2.0f - sideLengthOGL / 2.0f)
        );

        // Load palette
        Bitmap paletteBitmap = createPaletteBitmap(paletteRaw);

        /* VAO and VBOs */
        VAO = new int[1]; // one mesh <--> one VAO to bind vpos, normals, translations, paletteindexes
        int[] VBO = new int[3]; // 0: vpos|normals, 1: indices, 2:translations|paletteindexes

        glGenBuffers(3, VBO, 0);
        GLES30.glGenVertexArrays(1, VAO, 0);

        GLES30.glBindVertexArray(VAO[0]);
            // VERTEX LEVEL DATA
            glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
                glBufferData(GL_ARRAY_BUFFER, Float.BYTES * vertexData.capacity(), vertexData, GL_STATIC_DRAW);
                glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 0 * Float.BYTES); // vpos
                glVertexAttribPointer(2, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES); // normal
                glEnableVertexAttribArray(1);
                glEnableVertexAttribArray(2);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[1]);
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES * indexData.capacity(), indexData, GL_STATIC_DRAW);

            // INSTANCE LEVEL DATA
            glBindBuffer(GL_ARRAY_BUFFER, VBO[2]);
                glBufferData(GL_ARRAY_BUFFER, Integer.BYTES * voxelsData.capacity(), voxelsData, GL_STATIC_DRAW);
                // Note "AttribIPointer"!!!
                GLES30.glVertexAttribIPointer(3, 3, GL_INT, 4 * Integer.BYTES, 0 * Integer.BYTES); // translation
                GLES30.glVertexAttribIPointer(4, 1, GL_INT, 4 * Integer.BYTES, 3 * Integer.BYTES); // palette index
                glEnableVertexAttribArray(3);
                glEnableVertexAttribArray(4);
                GLES30.glVertexAttribDivisor(3, 1); // update the variable at location 3 (translation) each 1 istance
                GLES30.glVertexAttribDivisor(4, 1);
        GLES30.glBindVertexArray(0);

        // GLOBAL LEVEL DATA
        uVP = glGetUniformLocation(shaderHandle, "VP");
        uEyePos = glGetUniformLocation(shaderHandle, "eyePos");
        uLightPos = glGetUniformLocation(shaderHandle, "lightPos");
        uAxesM = glGetUniformLocation(shaderHandle, "axesM");

        /* Pre load uniform values */
        glUseProgram(shaderHandle);
            glUniform3fv(uLightPos, 1, lightPos, 0);
            glUniform3fv(uEyePos, 1, eyePos, 0); // TODO: useless if you update in onDraw
            glUniformMatrix4fv(uAxesM, 1, false, axesM, 0);
        glUseProgram(0);

        texObjId = new int[1];
        glGenTextures(1, texObjId, 0); // texture object creaction
        glBindTexture(GL_TEXTURE_2D, texObjId[0]); // binding
            // these two are mandatory also with texelFetch !!!
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            // related to “texObjId[0]”, what happens if we go over the S and T dimension?
            // glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            // glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, paletteBitmap, 0); // transfer host data to device memory
        glBindTexture(GL_TEXTURE_2D, 0); // unbinding

        uTexUnit = glGetUniformLocation(shaderHandle, "tex");

        /* Pre load uniforms values */
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texObjId[0]);
            glUseProgram(shaderHandle);
                // bind image pointed by texObjId[0] to the sampler object (that has location uTexUnit)
                glUniform1i(uTexUnit, 0); // 0 because active texture is GL_TEXTURE0
            glUseProgram(0);
        glBindTexture(GL_TEXTURE_2D, 0);

        paletteBitmap.recycle();

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // TODO: move it in callback, so that it is computed only when zoom occurs
        // compute magnitude (distance from origin) based on zoom
        float magnitude = minEyeDistance + (maxZoom-zoom)/(maxZoom-minZoom) * (maxEyeDistance-minEyeDistance);
        // magnitude = Math.max(minEyeDistance, Math.min(magnitude, maxEyeDistance)); // TODO: replace with an assert

        // Note the +90 shift is needed to start at (0, 0, magnitude)
        // adj = cos(angle) * hyp
        eyePos[0] = (float) Math.cos(Math.toRadians(angleY+90.0f)) * magnitude;
        // opp = sin(angle) * hyp
        eyePos[2] = (float) Math.sin(Math.toRadians(angleY+90.0f)) * magnitude;

        // comment these 2 lines if you want to keep lightPos fixed
        lightPos[0] = eyePos[0];
        lightPos[2] = eyePos[2];

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texObjId[0]);

        glUseProgram(shaderHandle);
        GLES30.glBindVertexArray(VAO[0]);

            glUniform3fv(uEyePos, 1, eyePos, 0);
            glUniform3fv(uLightPos, 1, lightPos, 0); // comment if you want to keep lightPos fixed

            Matrix.setIdentityM(VP, 0);
            /* Compute VP part of MVP */
            Matrix.setLookAtM(viewM, 0, eyePos[0], eyePos[1], eyePos[2],
                    0, 0, 0,
                    0, 1, 0);
            Matrix.multiplyMM(VP, 0, projM, 0, viewM, 0);

            glUniformMatrix4fv(uVP, 1, false, VP, 0);

            GLES30.glDrawElementsInstanced(drawMode, countFacesToElement, GL_UNSIGNED_INT, 0, numVoxels);

        GLES30.glBindVertexArray(0);
        glUseProgram(0);

    }


    private static Bitmap createPaletteBitmap(int[] paletteRaw){
        /* Create a bitmap from palette data */

        int numColors = paletteRaw.length / 3;
        int y = (int) Math.ceil(Math.log(numColors) / Math.log(2) / 2.0);
        // int paletteBitmapSide = (int) Math.pow(2, y);
        int paletteBitmapSide = 1 << y;

        @ColorInt int[] encodedColors = new int[paletteBitmapSide * paletteBitmapSide];
        for (int i = 0; i < numColors; i++) {
            encodedColors[i] = Color.rgb(
                    paletteRaw[i * VlyObject.COLOR_DATA_SIZE],
                    paletteRaw[i * VlyObject.COLOR_DATA_SIZE + 1],
                    paletteRaw[i * VlyObject.COLOR_DATA_SIZE + 2]
            );
        }

        Bitmap paletteBitmap = Bitmap.createBitmap(encodedColors, paletteBitmapSide, paletteBitmapSide, Bitmap.Config.ARGB_8888); // TODO: try also RGB565

        Log.v(TAG, "[createPaletteBitmap]: bitmap of size " + paletteBitmap.getWidth() + "x" + paletteBitmap.getHeight() + " loaded " +
                "with format " + paletteBitmap.getConfig().name());

        return paletteBitmap;
    }

}

