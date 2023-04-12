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
package android.support.drag2d.lib;

public class MaterialVelocity {
    float mStartPos, mStartVel;
    float mEndPos;
    float mDuration;
    Stage[] mStage = new Stage[]{new Stage(1), new Stage(2), new Stage(3)};
    int mNumberOfStages;
    Easing mEasing;
    double mEasingAdapterDistance;
    double mEasingAdapterA, mEasingAdapterB;
    protected boolean oneDimension = true;
    private float mTotalEasingDuration;

    public float getDuration() {
        if (mEasing != null) {
            return mTotalEasingDuration;
        }
        return mDuration;
    }

    public float getEndPos() {
        return mEndPos;
    }

    public float getStartPos() {
        return mStartPos;
    }

    protected static class Stage {
        float mStartV;
        float mStartPos;
        float mStartTime;
        float mEndV;
        float mEndPos;
        float mEndTime;
        float mDeltaV;
        float mDeltaT;
        int n;

        public float getEndTime() {
            return mEndTime;
        }

        public Stage(int n) {
            this.n = n;
        }

        public void setUp(
                float startV,
                float startPos,
                float startTime,
                float endV,
                float endPos,
                float endTime) {
            mStartV = startV;
            mStartPos = startPos;
            mStartTime = startTime;
            mEndV = endV;
            mEndTime = endTime;
            mEndPos = endPos;
            mDeltaV = mEndV - mStartV;
            mDeltaT = mEndTime - mStartTime;
        }

        public float getPos(float t) {
            float dt = t - mStartTime;
            float pt = dt / (mDeltaT);
            float v = mStartV + (mDeltaV) * pt;
            return dt * (mStartV + v) / 2 + mStartPos;
        }

        public float getVel(float t) {
            float dt = t - mStartTime;
            float pt = dt / (mEndTime - mStartTime);
            return mStartV + (mDeltaV) * pt;
        }
    }

    public float getV(float t) {
        if (mEasing == null) {
            for (int i = 0; i < mNumberOfStages; i++) {
                if (mStage[i].mEndTime > t) {
                    return mStage[i].getVel(t);
                }
            }
            return 0;
        }
        int lastStages = mNumberOfStages;
        for (int i = 0; i < lastStages; i++) {
            if (mStage[i].mEndTime > t) {
                return mStage[i].getVel(t);
            }
        }
        return (float) getEasingDiff((t - mStage[lastStages].mStartTime));
    }

    public float getPos(float t) {
        if (mEasing == null) {
            for (int i = 0; i < mNumberOfStages; i++) {
                if (mStage[i].mEndTime > t) {
                    return mStage[i].getPos(t);
                }
            }
            return mEndPos;
        }


        int lastStages = mNumberOfStages - 1;

        for (int i = 0; i < lastStages; i++) {
            if (mStage[i].mEndTime > t) {
                return mStage[i].getPos(t);
            }
        }

        float ret = (float) getEasing(t - mStage[lastStages].mStartTime);
        System.out.println(">>>>>>>>>>>>>>> easing= " + ret);

        ret += mStage[lastStages].mStartPos;
        System.out.println(">>>>>>>>>>>>>>> offset " + ret);
        return ret;

    }

    @Override
    public String toString() {
        String s = " ";
        for (int i = 0; i < mNumberOfStages; i++) {
            Stage stage = mStage[i];
            s += " " + i + " " + stage.toString();
        }
        return s;
    }

    public interface Easing {
        double get(double t);

        double getDiff(double t);

        Easing clone();

    }

    public void config(float currentPos, float destination, float currentVelocity,
                       float maxTime, float maxAcceleration, float maxVelocity, Easing easing) {
        mStartPos = currentPos;
        mEndPos = destination;

        if (easing != null) {
            mEasing = easing.clone();
        } else {
            mEasing = null;
        }


        float dir = Math.signum(destination - currentPos);
        float maxV = maxVelocity * dir;
        float maxA = maxAcceleration * dir;

        if (currentVelocity == 0.0) {
            currentVelocity = 0.0001f * dir;
        }

        mStartVel = currentVelocity;

        if (!rampDown(currentPos, destination, currentVelocity, maxTime)) {
            if (!(oneDimension && cruseThenRampDown(currentPos, destination, currentVelocity, maxTime, maxA, maxV))) {
                if (!rampUpRampDown(currentPos, destination, currentVelocity, maxA, maxV)) {
                    rampUpCruseRampDown(currentPos, destination, currentVelocity, maxA, maxV);
                }
            }
        }
        if (oneDimension) {

            configureEasingAdapter();

        }
    }

    private boolean rampDown(float currentPos, float destination, float currentVelocity,
                             float maxTime) {
        float timeToDestination = 2 * ((destination - currentPos) / currentVelocity);
        if (timeToDestination > 0 && timeToDestination <= maxTime) { // hit the brakes
            mNumberOfStages = 1;
            mStage[0].setUp(currentVelocity, currentPos, 0, 0, destination, timeToDestination);
            mDuration = timeToDestination;
            return true;
        }
        return false;
    }

    private boolean cruseThenRampDown(float currentPos, float destination, float currentVelocity,
                                      float maxTime, float maxA, float maxV) {

        float timeToBreak = currentVelocity / maxA;
        float brakeDist = currentVelocity * timeToBreak / 2;
        float cruseDist = (destination - currentPos) - brakeDist;
        float cruseTime = cruseDist / currentVelocity;
        float totalTime = cruseTime + timeToBreak;


        if (totalTime > 0 && totalTime < maxTime) {
            mNumberOfStages = 2;
            mStage[0].setUp(currentVelocity, currentPos, 0, currentVelocity, cruseDist, cruseTime);
            mStage[1].setUp(currentVelocity, currentPos + cruseDist, cruseTime, 0, destination, cruseTime + timeToBreak);
            mDuration = cruseTime + timeToBreak;
            return true;
        }
        return false;
    }

    private boolean rampUpRampDown(float currentPos, float destination, float currentVelocity,
                                   float maxA, float maxVelocity) {
        float peak_v = Math.signum(maxA) * (float) Math.sqrt(maxA * (destination - currentPos) + currentVelocity * currentVelocity / 2);

        if (maxVelocity / peak_v > 1) {
            float t1 = (peak_v - currentVelocity) / maxA;
            float d1 = (peak_v + currentVelocity) * t1 / 2 + currentPos;
            float t2 = peak_v / maxA;
            mNumberOfStages = 2;
            mStage[0].setUp(currentVelocity, currentPos, 0, peak_v, d1, t1);
            mStage[1].setUp(peak_v, d1, t1, 0, destination, t2 + t1);
            mDuration = t2 + t1;
            return true;
        }
        return false;
    }

    private void rampUpCruseRampDown(float currentPos, float destination, float currentVelocity,
                                     float maxA, float maxV) {
        float t1 = (maxV - currentVelocity) / maxA;
        float d1 = (maxV + currentVelocity) * t1 / 2 + currentPos;
        float t3 = maxV / maxA;
        float d3 = (maxV) * t3 / 2;
        float d2 = destination - d1 - d3;
        float t2 = d2 / maxV;

        mNumberOfStages = 3;
        mStage[0].setUp(currentVelocity, currentPos, 0, maxV, d1, t1);
        mStage[1].setUp(maxV, d1, t1, maxV, d2 + d1, t2 + t1);
        mStage[2].setUp(maxV, d1 + d2, t1 + t2, 0, destination, t2 + t1 + t3);
        mDuration = t3 + t2 + t1;
    }


    public double getEasing(double t) {
        double gx = t * t * mEasingAdapterA + t * mEasingAdapterB;
        if (gx > 1) {
            return mEasingAdapterDistance;
        }
        return mEasing.get(gx) * mEasingAdapterDistance;
    }

    private double getEasingDiff(double t) {
        double gx = t * t * mEasingAdapterA + t * mEasingAdapterB;
        if (gx > 1) {
            return 0;
        }
        return mEasing.getDiff(gx) * mEasingAdapterDistance;
    }


    protected void configureEasingAdapter() {
        if (mEasing == null) {
            return;
        }
        int last = mNumberOfStages - 1;
        float initialVelocity = mStage[last].mStartV;
        float distance = mStage[last].mEndPos - mStage[last].mStartPos;
        float duration = mStage[last].mEndTime - mStage[last].mStartTime;
        double baseVel = mEasing.getDiff(0);

        mEasingAdapterB = initialVelocity / (baseVel * distance);
        mEasingAdapterA = 1 - mEasingAdapterB;
        mEasingAdapterDistance = distance;
        double easingDuration = (Math.sqrt(4 * mEasingAdapterA + mEasingAdapterB * mEasingAdapterB) - mEasingAdapterB) / (2 * mEasingAdapterA);
        mTotalEasingDuration = (float) (easingDuration + mStage[last].mStartTime);

    }


}
