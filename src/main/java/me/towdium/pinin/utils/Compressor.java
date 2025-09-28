package me.towdium.pinin.utils;

import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Compressor implements Accelerator.Provider {
    CharList chars = new CharArrayList();
    IntList strs = new IntArrayList();

    public IntList offsets() {
        return strs;
    }

    public int put(String s) {
        strs.add(chars.size());
        for (int i = 0; i < s.length(); i++)
            chars.add(s.charAt(i));
        chars.add('\0');
        return strs.getInt(strs.size() - 1);
    }

    @Override
    public boolean end(int i) {
        return chars.getChar(i) == '\0';
    }

    @Override
    public int get(int i) {
        char first = chars.getChar(i);
        if (Character.isHighSurrogate(first) && i + 1 < chars.size()) {
            char second = chars.getChar(i + 1);
            if (Character.isLowSurrogate(second)) return Character.toCodePoint(first, second);
        }
        return first;
    }
}
