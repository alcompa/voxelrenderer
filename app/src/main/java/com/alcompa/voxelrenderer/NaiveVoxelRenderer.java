package com.alcompa.voxelrenderer;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

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

public class NaiveVoxelRenderer extends BasicRenderer {
    private static final String VSHAD_FILENAME = "vertex.glslv";
    private static final String FSHAD_FILENAME = "fragment.glslf";
    private static final String VOXMODEL_FILENAME = "simple.vly";

    private static final float MAX_GRID_SIZE_NDC = 1.0f;

    private int[] VAO;
    private int shaderHandle;
    private int MVPloc;
    private int colorloc; // added

    private float[] viewM;
    private float[] modelM;
    private float[] projM;
    private float[] MVP;
    private float[] temp;

    private float[] SR; // scaling and rotation
    private float scalingFactor;
    private float[] startNDC;

    /* start */
    private int[] voxelsRaw;
    private float[] paletteRaw;
    /* end */

    private int drawMode;
    private int countFacesToElement;

    public NaiveVoxelRenderer() {
        super();
        drawMode = GL_TRIANGLES;
        viewM = new float[16];
        modelM = new float[16];
        projM = new float[16];
        MVP = new float[16];
        temp = new float[16];
        SR = new float[16];

        Matrix.setIdentityM(viewM, 0);
        Matrix.setIdentityM(modelM, 0);
        Matrix.setIdentityM(projM, 0);
        Matrix.setIdentityM(MVP, 0);
        Matrix.setIdentityM(SR, 0);


    }

    @Override
    public void setContextAndSurface(Context context, GLSurfaceView surface) {
        super.setContextAndSurface(context, surface);

        this.surface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawMode == GL_TRIANGLES)
                    drawMode = GL_LINE_LOOP;
                else drawMode = GL_TRIANGLES;

                Log.v("TAG", "Drawing " + (drawMode == GL_TRIANGLES ? "Triangles" : "Lines"));

            }
        });

    }


    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        super.onSurfaceChanged(gl10, w, h);
        float aspect = ((float) w) / ((float) (h == 0 ? 1 : h));

        Matrix.perspectiveM(projM, 0, 45f, aspect, 0.1f, 100f);

        Matrix.setLookAtM(viewM, 0, 0, 0f, 10f,
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
        int[] gridSize = null;

        try {
            is = context.getAssets().open(VOXMODEL_FILENAME);
            VlyObject vo = new VlyObject(is);
            vo.parse();

            Log.v(getClass().getSimpleName(), vo.toString());

            voxelsRaw = vo.getVoxelsRaw();
            paletteRaw = vo.getPaletteRaw();
            gridSize = vo.getGridSize();
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            Log.e(getClass().getSimpleName(), "Failed to load .vly model");
            System.exit(-1);
        }

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

        /* Precompute scaling and rotation, common for all voxels */
        float maxGridSize = Math.max(Math.max(gridSize[0], gridSize[1]), gridSize[2]);
        scalingFactor = MAX_GRID_SIZE_NDC / maxGridSize; // assuming that the sides of the model are 1.0f long

        // Matrix.translateM(modelM, 0, startNDC[0], startNDC[1], startNDC[2]);
        Matrix.rotateM(SR, 0, -90, 1, 0, 0);
        Matrix.scaleM(SR, 0, scalingFactor, scalingFactor, scalingFactor);

        float[] gridSizeNDC = {gridSize[0]/scalingFactor, gridSize[1]/scalingFactor, gridSize[2]/scalingFactor};
        float[] paddingNDC = {(MAX_GRID_SIZE_NDC-gridSizeNDC[0])/2f, (MAX_GRID_SIZE_NDC-gridSizeNDC[1])/2f, (MAX_GRID_SIZE_NDC-gridSizeNDC[2])/2f};
        startNDC = new float[]{-MAX_GRID_SIZE_NDC/2 + paddingNDC[0], -MAX_GRID_SIZE_NDC/2 + paddingNDC[1], -MAX_GRID_SIZE_NDC/2 + paddingNDC[2]};


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
        colorloc = glGetUniformLocation(shaderHandle, "color");

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix.multiplyMM(temp, 0, projM, 0, viewM, 0);

        Matrix.setIdentityM(modelM, 0);

        glUseProgram(shaderHandle);
        GLES30.glBindVertexArray(VAO[0]);

        for(int i = 0; i < voxelsRaw.length; i+=VlyObject.VOXEL_DATA_SIZE){
            Matrix.translateM(modelM,
                    0,
                    startNDC[0] + voxelsRaw[i] / scalingFactor,
                    startNDC[1] + voxelsRaw[i+1] / scalingFactor,
                    startNDC[2] + voxelsRaw[i+2] / scalingFactor
                    );
            Matrix.multiplyMM(modelM, 0, modelM, 0, SR, 0);

            Matrix.multiplyMM(MVP, 0, temp, 0, modelM, 0);

            glUniformMatrix4fv(MVPloc, 1, false, MVP, 0);

            int paletteIdx = voxelsRaw[i + VlyObject.VOXEL_DATA_SIZE - 1];
            glUniform3f(
                    colorloc,
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

