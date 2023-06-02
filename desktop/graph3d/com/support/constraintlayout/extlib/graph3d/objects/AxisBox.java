/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.support.constraintlayout.extlib.graph3d.objects;

import com.support.constraintlayout.extlib.graph3d.Object3D;
import com.support.constraintlayout.extlib.graph3d.Scene3D;

/**
 * Draws box along the axis
 */
public class AxisBox extends Object3D {
    int color = 0xFFFF3233;
    public AxisBox() {
   mType = 1;
    }

    public void setRange(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        mMinX = minX;
        mMaxX = maxX;
        mMinY = minY;
        mMaxY = maxY;
        mMinZ = minZ;
        mMaxZ = maxZ;
        buildBox();
    }

    void buildBox() {
        vert = new float[8 * 3]; // cube 8 corners
        tVert = new float[vert.length];
        for (int i = 0; i < 8; i++) {
            vert[i * 3] = ((i & 1) == 0) ? mMinX : mMaxX; // X
            vert[i * 3 + 1] = (((i >> 1) & 1) == 0) ? mMinY : mMaxY; // Y
            vert[i * 3 + 2] = (((i >> 2) & 1) == 0) ? mMinZ : mMaxZ; // Z
        }

        index = new int[6 * 2* 3]; // 6 sides x 2 triangles x 3 points per triangle
        int []sides = {  // pattern of clockwise triangles around cube
                0,2,1, 2,3,1,
                0,1,4, 1,5,4,
                0,4,2, 4,6,2,
                7,5,6, 5,4,6,
                7,6,3, 6,2,3,
                7,3,5, 3,1,5
        };
        index = new int[sides.length];
        for (int i = 0; i < sides.length; i++) {
            index[i] = sides[i]*3;
        }
    }

    public void render(Scene3D s, float[] zbuff, int[] img, int w, int h) {
        for (int i = 0; i < index.length; i += 3) {
            int p1 = index[i];
            int p2 = index[i + 1];
            int p3 = index[i + 2];

            float height = (vert[p1 + 2] + vert[p3 + 2] + vert[p2 + 2]) / 3;
            int val = (int) (255 * Math.abs(height));


            Scene3D.drawline(zbuff, img, color, w, h,
                    tVert[p1], tVert[p1 + 1], tVert[p1 + 2] - 0.01f,
                    tVert[p2], tVert[p2 + 1], tVert[p2 + 2] - 0.01f);
//            Scene3D.drawline(zbuff, img,color, w, h,
//                    tVert[p1], tVert[p1 + 1], tVert[p1 + 2] - 0.01f,
//                    tVert[p3], tVert[p3 + 1], tVert[p3 + 2] - 0.01f);
//            Scene3D.drawline(zbuff, img,color, w, h,
//                    tVert[p2], tVert[p2 + 1], tVert[p2 + 2] - 0.01f,
//                    tVert[p3], tVert[p3 + 1], tVert[p3 + 2] - 0.01f);
        }
    }
}
