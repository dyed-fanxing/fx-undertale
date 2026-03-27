package com.fanxing.fx_undertale.utils;

import net.minecraft.world.phys.Vec3;
import java.util.function.Function;

public enum ParametricCurveType {
    // ========== 径向曲线 ==========
    RADIAL {
        @Override
        public Function<Float, Vec3> create(float... params) {
            float wobble = params.length > 0 ? params[0] : 0f;
            return ParametricCurveUtils.radial(wobble);
        }
    },
    RADIAL_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(RADIAL.create(params));
        }
    },

    // ========== 螺旋曲线 ==========
    SPIRAL {
        @Override
        public Function<Float, Vec3> create(float... params) {
            float turns = params.length > 0 ? params[0] : 3f;
            return ParametricCurveUtils.spiral(turns);
        }
    },
    SPIRAL_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(SPIRAL.create(params));
        }
    },

    // ========== 花瓣曲线 ==========
    FLOWER {
        @Override
        public Function<Float, Vec3> create(float... params) {
            int petals = params.length > 0 ? (int) params[0] : 5;
            return ParametricCurveUtils.flower(petals);
        }
    },
    FLOWER_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(FLOWER.create(params));
        }
    },

    // ========== 爱心曲线 ==========
    HEART {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.heart();
        }
    },
    HEART_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(HEART.create(params));
        }
    },


    // ========== 分形折叠曲线 ==========
    FRACTAL {
        @Override
        public Function<Float, Vec3> create(float... params) {
            int segments = params.length > 0 ? (int) params[0] : 5;
            float sharpness = params.length > 1 ? params[1] : 0.5f;
            return ParametricCurveUtils.fractal(segments, sharpness);
        }
    },
    FRACTAL_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(FRACTAL.create(params));
        }
    },
    // ========== 锯齿尖刺 (Sawtooth Radial) ==========
    /**
     * 锯齿尖刺径向曲线
     * 参数: [waves, amplitude]
     *   waves    - 锯齿波数量，建议 3.0 ~ 8.0
     *   amplitude- 摆动幅度，建议 0.2 ~ 0.6
     */
    SAWTOOTH_RADIAL {
        @Override
        public Function<Float, Vec3> create(float... params) {
            float waves = params.length > 0 ? params[0] : 4.2f;
            float amplitude = params.length > 1 ? params[1] : 0.45f;
            return ParametricCurveUtils.sawtoothRadial(waves, amplitude);
        }
    },
    SAWTOOTH_RADIAL_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(SAWTOOTH_RADIAL.create(params));
        }
    },

    // ========== 星爆脉冲 (StarBurst) ==========
    /**
     * 星爆脉冲曲线
     * 参数: [spikes, sharpness]
     *   spikes   - 星芒数量，建议 5 ~ 12
     *   sharpness- 锐度系数 (0~1)，建议 0.4 ~ 0.9
     */
    STARBURST {
        @Override
        public Function<Float, Vec3> create(float... params) {
            float spikes = params.length > 0 ? params[0] : 6.0f;
            float sharpness = params.length > 1 ? params[1] : 0.6f;
            return ParametricCurveUtils.starburst(spikes, sharpness);
        }
    },
    STARBURST_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(STARBURST.create(params));
        }
    },

    // ========== 折叠波刃 (WaveFold) ==========
    /**
     * 折叠波刃曲线
     * 参数: [freq1, freq2, amplitude]
     *   freq1    - 主波频率，建议 4 ~ 8
     *   freq2    - 次波频率，建议 6 ~ 12
     *   amplitude- 波动幅度，建议 0.2 ~ 0.6
     */
    WAVEFOLD {
        @Override
        public Function<Float, Vec3> create(float... params) {
            float freq1 = params.length > 0 ? params[0] : 5.0f;
            float freq2 = params.length > 1 ? params[1] : 11.5f;
            float amplitude = params.length > 2 ? params[2] : 0.42f;
            return ParametricCurveUtils.wavefold(freq1, freq2, amplitude);
        }
    },
    WAVEFOLD_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(WAVEFOLD.create(params));
        }
    }
    ;

    public abstract Function<Float, Vec3> create(float... params);

    public static ParametricCurveType fromId(int id) {
        return values()[id];
    }
}