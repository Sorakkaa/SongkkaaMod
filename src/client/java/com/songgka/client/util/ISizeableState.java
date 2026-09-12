package com.songgka.client.util;

public interface ISizeableState {
    float songgka$getScaleX();
    float songgka$getScaleY();
    float songgka$getScaleZ();
    boolean songgka$hasCustomScale();

    void songgka$setScale(float x, float y, float z);
}
