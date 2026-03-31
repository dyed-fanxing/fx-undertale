package com.fanxing.fx_undertale.entity.capability;

public interface Rollable {
    float getRoll();
    default float getRollO(){
        return 0;
    }
}
