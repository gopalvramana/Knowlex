package com.symphony.docweave.util;

import java.util.ArrayList;
import java.util.List;

public final class EmbeddingUtils {

    private EmbeddingUtils() {}

    public static float[] toFloatArray(List<Double> vector) {
        float[] arr = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            arr[i] = vector.get(i).floatValue();
        }
        return arr;
    }

    public static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }
}
