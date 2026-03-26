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

    // ========== 星形曲线 ==========
    STAR {
        @Override
        public Function<Float, Vec3> create(float... params) {
            int points = params.length > 0 ? (int) params[0] : 5;
            float depth = params.length > 1 ? params[1] : 0.5f;
            return ParametricCurveUtils.star(points, depth);
        }
    },
    STAR_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(STAR.create(params));
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

    // ========== 正弦波曲线 ==========
    SINE_WAVE {
        @Override
        public Function<Float, Vec3> create(float... params) {
            float waves = params.length > 0 ? params[0] : 2f;
            return ParametricCurveUtils.sineWave(waves);
        }
    },
    SINE_WAVE_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(SINE_WAVE.create(params));
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

    // ========== 布朗漂移曲线 ==========
    BROWNIAN {
        @Override
        public Function<Float, Vec3> create(float... params) {
            float intensity = params.length > 0 ? params[0] : 0.3f;
            return ParametricCurveUtils.brownian(intensity);
        }
    },
    BROWNIAN_REVERSED {
        @Override
        public Function<Float, Vec3> create(float... params) {
            return ParametricCurveUtils.reverse(BROWNIAN.create(params));
        }
    };

    public abstract Function<Float, Vec3> create(float... params);

    public static ParametricCurveType fromId(int id) {
        return values()[id];
    }
}