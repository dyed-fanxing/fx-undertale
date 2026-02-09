package com.sakpeipei.undertale.common.phys;

import com.mojang.math.Axis;
import org.joml.Quaternionf;

public interface AxisQuaternionf {
    Quaternionf Z_90 = Axis.ZP.rotationDegrees(90f);
    Quaternionf Z_NEG_90 = Axis.ZP.rotationDegrees(-90f);
}
