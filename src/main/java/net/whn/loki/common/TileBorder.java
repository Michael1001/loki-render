package net.whn.loki.common;

import java.io.Serializable;

public class TileBorder implements Serializable {

    final private float left;
    final private float right;
    final private float bottom;
    final private float top;

    public TileBorder(float l, float r, float b, float t) {
        left = l;
        right = r;
        bottom = b;
        top = t;
        
    }

    public float getBottom() {
        return bottom;
    }

    public float getLeft() {
        return left;
    }

    public float getRight() {
        return right;
    }

    public float getTop() {
        return top;
    }
}
