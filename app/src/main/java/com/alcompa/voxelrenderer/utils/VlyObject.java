package com.alcompa.voxelrenderer.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class VlyObject {
    private static final String TAG = "VLY_PARSER";

    private int[] gridSize;
    private int voxelNum;
    private int[] voxelsRaw;
    private int colorsNum;
    private int[] paletteRaw;
    private InputStream is;

    public static final int VOXEL_DATA_SIZE = 4;
    public static final int COLOR_DATA_SIZE = 3;

    public VlyObject(InputStream is) {
        this.is = is;
        this.gridSize = new int[3];
    }

    public int[] getGridSize() {
        return gridSize;
    }

    public int[] getVoxelsRaw() {
        return voxelsRaw;
    }

    public int[] getPaletteRaw() {
        return paletteRaw;
    }

    @Override
    public String toString() {
        return "VlyObject{" +
                "gridSize=" + Arrays.toString(gridSize) +
                ", voxelNum=" + voxelNum +
                ", voxelsRaw=" + Arrays.toString(voxelsRaw).substring(0, 30) +
                ", colorsNum=" + colorsNum +
                ", paletteRaw=" + Arrays.toString(paletteRaw).substring(0, 30)  +
                '}';
    }

    public void parse() throws IOException, NumberFormatException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String s;
        String[] tokens;
        Pattern pattern;
        Matcher matcher;

        s = reader.readLine();
        pattern = Pattern.compile("^grid_size: [0-9]+ [0-9]+ [0-9]+");
        matcher = pattern.matcher(s);

        if (!matcher.find()) {
            Log.e(TAG, "Wrong input format (grid size)");
            throw new IOException("Wrong input format (grid size)");
        }

        tokens = s.split(": ")[1].split(" ");
        gridSize[0] = Integer.parseInt(tokens[0].trim());
        gridSize[1] = Integer.parseInt(tokens[1].trim());
        gridSize[2] = Integer.parseInt(tokens[2].trim());

        s = reader.readLine();
        pattern = Pattern.compile("^voxel_num: [0-9]+");
        matcher = pattern.matcher(s);

        if (!matcher.find()) {
            Log.e(TAG, "Wrong input format (voxel num)");
            throw new IOException("Wrong input format (voxel num)");
        }

        voxelNum = Integer.parseInt(s.split(": ")[1].trim());
        voxelsRaw = new int[voxelNum * VOXEL_DATA_SIZE];

        colorsNum = 0;

        for (int i = 0; i < voxelNum; i++){
            s = reader.readLine();
            tokens = s.split(" ");
            for (int j = 0; j < VOXEL_DATA_SIZE; j++){
                voxelsRaw[i*VOXEL_DATA_SIZE+j] = Integer.parseInt(tokens[j].trim());
            }

            if (voxelsRaw[i*VOXEL_DATA_SIZE + VOXEL_DATA_SIZE-1] > colorsNum){
               colorsNum = voxelsRaw[i*VOXEL_DATA_SIZE + VOXEL_DATA_SIZE-1];
            }
        }

        colorsNum++; // since Ci starts with 0

        paletteRaw = new int[colorsNum * COLOR_DATA_SIZE];

        for (int i = 0; i < colorsNum; i++){
            s = reader.readLine();
            tokens = s.split(" ");
            for (int j = 0; j < COLOR_DATA_SIZE; j++){
                paletteRaw[i*COLOR_DATA_SIZE+j] = Integer.parseInt(tokens[j+1].trim()); // skip token 0 since Ci is a sequential number
            }
        }

        reader.close();
        is.close();
    }
}
