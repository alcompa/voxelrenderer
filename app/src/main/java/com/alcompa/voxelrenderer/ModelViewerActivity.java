package com.alcompa.voxelrenderer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/* Adapted from mobile programming course samples (https://git.hipert.unimore.it/ncapodieci/mobileprogramming/) */
public class ModelViewerActivity extends AppCompatActivity {
    public static final int DESIRED_DEPTH_SIZE = 24;

    private GLSurfaceView surface;
    private boolean isSurfaceCreated;
    private String modelFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState==null && getIntent().getExtras()==null) {
            modelFilename = "chrk.vly";
        }else if (savedInstanceState!=null)
            modelFilename = savedInstanceState.getString("modelFilename");
        else
            modelFilename = getIntent().getExtras().getString("modelFilename");

        //Optional for full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags
                (WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //get a reference to the Activity Manager (AM)
        final ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        //from the AM we get an object with our mobile device info
        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();

        int supported = 1;

        if(configurationInfo.reqGlEsVersion>=0x30000)
            supported = 3;
        else if(configurationInfo.reqGlEsVersion>=0x20000)
            supported = 2;

        Log.v("TAG","Opengl ES supported >= " +
                supported + " (" + Integer.toHexString(configurationInfo.reqGlEsVersion) + " " +
                configurationInfo.getGlEsVersion() + ")");

        surface = new GLSurfaceView(this);
        surface.setEGLContextClientVersion(supported);
        surface.setPreserveEGLContextOnPause(true);
        surface.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
            @Override
            public EGLConfig chooseConfig(EGL10 egl10, EGLDisplay eglDisplay) {
                return getConfig(DESIRED_DEPTH_SIZE);
            }
        });

        // GLSurfaceView.Renderer renderer = new BasicRenderer(0.45f,0.32f,0.13f);
        // GLSurfaceView.Renderer renderer = new NaiveVoxelRenderer();
        GLSurfaceView.Renderer renderer = new InstancedVoxelRenderer(modelFilename);

        setContentView(surface);
        ((BasicRenderer) renderer).setContextAndSurface(this,surface);
        surface.setRenderer(renderer);
        isSurfaceCreated = true;

        //Log.v("TAG",getWindow().getDecorView().findViewById(android.R.id.content).toString());

    }

    @Override
    public void onResume(){
        super.onResume();
        if(isSurfaceCreated)
            surface.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(isSurfaceCreated)
            surface.onPause();
    }

    private EGLConfig getConfig(int desiredDepthSize) {
        //Riferimento al contesto EGL
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        //ottenimento di numConfigs[0] configurazioni supportate.
        int[] numConfigs = new int[1];
        egl.eglChooseConfig(display, null, null, 0, numConfigs);
        EGLConfig[] configs = new EGLConfig[numConfigs[0]];
        egl.eglChooseConfig(display, null, configs, numConfigs[0], numConfigs);

        Log.v("EGLCONFIG", "configs " + numConfigs[0]);

        //per ogni configurazione supportata
        for (EGLConfig cfg : configs) {
            int[] depthv = new int[1];
            //estraiamo il numero di bit usati per rappresentare la profondita
            egl.eglGetConfigAttrib(display, cfg, EGL10.EGL_DEPTH_SIZE, depthv);
            Log.v("EGLCONFIG", "EGL_DEPTH_SIZE: " + depthv[0]);

            /*se depthv[0] contiene un valore in bit maggiore o uguale di quello desiderato
            possiamo restituire cfg ed impostare quella configurazione nella GLSurfaceView */
            if (depthv[0] >= desiredDepthSize) {
                return cfg;
            }
        }

        // Nessuna configurazione soddisfa i requisiti
        Log.w("EGLCONFIG", "No config satisfies DESIRED_DEPTH_SIZE");
        return configs[0];
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString("modelFilename", modelFilename);
    }
}