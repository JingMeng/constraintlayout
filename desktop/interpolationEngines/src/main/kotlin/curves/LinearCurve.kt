/*
 * Copyright (C) 2020 The Android Open Source Project
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
package curves

import kotlin.math.hypot

/**
 * This performs a simple linear interpolation in multiple dimensions
 *
 *
 */
class LinearCurve(time: FloatArray, y: List<FloatArray>) {
    private val timePoints: FloatArray
    var mY: ArrayList<FloatArray>
    private var mTotalLength = Float.NaN
    private val mExtrapolate = true
    private var mSlopeTemp: FloatArray

    init {
        val dim = y[0].size
        mSlopeTemp = FloatArray(dim)
        timePoints = time
        mY = copyData(y)
        if (dim > 2) {
            var sum = 0f
            var lastx = 0f
            var lasty = 0f
            for (i in time.indices) {
                val px = y[i][0]
                val py = y[i][1]
                if (i > 0) {
                    sum +=  hypot((px - lastx), (py - lasty))
                }
                lastx = px
                lasty = py
            }
            mTotalLength = 0f
        }
    }

    private fun copyData(y: List<FloatArray>): ArrayList<FloatArray> {
        val ret = ArrayList<FloatArray>()
        for (array in y) {
            ret.add(array)
        }
        return ret
    }

    /**
     * Calculate the length traveled by the first two parameters assuming they are x and y.
     * (Added for future work)
     *
     * @param t the point to calculate the length to
     */
    private fun getLength2D(t: Float): Float {
        if ( mTotalLength.isNaN()) {
            return 0f
        }
        val n = timePoints.size
        if (t <= timePoints[0]) {
            return 0f
        }
        if (t >= timePoints[n - 1]) {
            return mTotalLength
        }
        var sum = 0f
        var last_x = 0f
        var last_y = 0f
        for (i in 0 until n - 1) {
            var px = mY[i][0]
            var py = mY[i][1]
            if (i > 0) {
                sum += hypot((px - last_x), (py - last_y))
            }
            last_x = px
            last_y = py
            if (t == timePoints[i]) {
                return sum
            }
            if (t < timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                val x1 = mY[i][0]
                val x2 = mY[i + 1][0]
                val y1 = mY[i][1]
                val y2 = mY[i + 1][1]
                py -= y1 * (1 - x) + y2 * x
                px -= x1 * (1 - x) + x2 * x
                sum += hypot(py, px)
                return sum
            }
        }
        return 0f
    }

    // @TODO: add description
    fun getPos(t: Float, v: FloatArray) {
        val n = timePoints.size
        val dim = mY[0].size
        if (mExtrapolate) {
            if (t <= timePoints[0]) {
                getSlope(timePoints[0], mSlopeTemp)
                for (j in 0 until dim) {
                    v[j] = mY[0][j] + (t - timePoints[0]) * mSlopeTemp[j]
                }
                return
            }
            if (t >= timePoints[n - 1]) {
                getSlope(timePoints[n - 1], mSlopeTemp)
                for (j in 0 until dim) {
                    v[j] = mY[n - 1][j] + (t - timePoints[n - 1]) * mSlopeTemp[j]
                }
                return
            }
        } else {
            if (t <= timePoints[0]) {
                for (j in 0 until dim) {
                    v[j] = mY[0][j]
                }
                return
            }
            if (t >= timePoints[n - 1]) {
                for (j in 0 until dim) {
                    v[j] = mY[n - 1][j]
                }
                return
            }
        }
        for (i in 0 until n - 1) {
            if (t == timePoints[i]) {
                for (j in 0 until dim) {
                    v[j] = mY[i][j]
                }
            }
            if (t < timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                for (j in 0 until dim) {
                    val y1 = mY[i][j]
                    val y2 = mY[i + 1][j]
                    v[j] = y1 * (1 - x) + y2 * x
                }
                return
            }
        }
    }

    // @TODO: add description
    fun getPos(t: Float, j: Int): Float {
        val n = timePoints.size
        if (mExtrapolate) {
            if (t <= timePoints[0]) {
                return mY[0][j] + (t - timePoints[0]) * getSlope(timePoints[0], j)
            }
            if (t >= timePoints[n - 1]) {
                return mY[n - 1][j] + (t - timePoints[n - 1]) * getSlope(timePoints[n - 1], j)
            }
        } else {
            if (t <= timePoints[0]) {
                return mY[0][j]
            }
            if (t >= timePoints[n - 1]) {
                return mY[n - 1][j]
            }
        }
        for (i in 0 until n - 1) {
            if (t == timePoints[i]) {
                return mY[i][j]
            }
            if (t < timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                val y1 = mY[i][j]
                val y2 = mY[i + 1][j]
                return y1 * (1 - x) + y2 * x
            }
        }
        return 0f // should never reach here
    }

    // @TODO: add description
    fun getSlope(time: Float, v: FloatArray) {
        var t = time
        val n = timePoints.size
        val dim = mY[0].size
        if (t <= timePoints[0]) {
            t = timePoints[0]
        } else if (t >= timePoints[n - 1]) {
            t = timePoints[n - 1]
        }
        for (i in 0 until n - 1) {
            if (t <= timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                for (j in 0 until dim) {
                    val y1 = mY[i][j]
                    val y2 = mY[i + 1][j]
                    v[j] = (y2 - y1) / h
                }
                break
            }
        }
        return
    }

    // @TODO: add description
    fun getSlope(time: Float, j: Int): Float {
        var t = time
        val n = timePoints.size
        if (t < timePoints[0]) {
            t = timePoints[0]
        } else if (t >= timePoints[n - 1]) {
            t = timePoints[n - 1]
        }
        for (i in 0 until n - 1) {
            if (t <= timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val y1 = mY[i][j]
                val y2 = mY[i + 1][j]
                return (y2 - y1) / h
            }
        }
        return 0f // should never reach here
    }

}