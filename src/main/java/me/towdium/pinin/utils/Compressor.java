package me.towdium.pinin.utils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Compressor implements Accelerator.Provider {
    StringBuilder chars = new StringBuilder();
    IntList strs = new IntArrayList();

    public IntList offsets() {
        return strs;
    }

    public int put(String s) {
        strs.add(chars.length());
        chars.append(s);
        chars.append('\0');
        return strs.getInt(strs.size() - 1);
    }

    @Override
    public boolean end(int i) {
        return chars.charAt(i) == '\0';
    }

    @Override
    public int get(int i) {
        char first = chars.charAt(i);
        if (Character.isHighSurrogate(first) && i + 1 < chars.length()) {
            char second = chars.charAt(i + 1);
            if (Character.isLowSurrogate(second)) return Character.toCodePoint(first, second);
        }
        return first;
    }
}
