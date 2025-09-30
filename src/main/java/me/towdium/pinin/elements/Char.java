package me.towdium.pinin.elements;

import me.towdium.pinin.utils.IndexSet;

public class Char implements Element {
    public static final Pinyin[] NONE = new Pinyin[0];

    Pinyin[] pinyin;
    protected int ch;

    public Char(char ch, Pinyin[] pinyin) {
        this.ch = ch;
        this.pinyin = pinyin;
    }

    public Char(int ch, Pinyin[] pinyin) {
        this.ch = ch;
        this.pinyin = pinyin;
    }

    public Char(String s, Pinyin[] pinyin) {
        this.ch = s.codePointAt(0);
        this.pinyin = pinyin;
    }

    @Override
    public IndexSet match(String str, int start, boolean partial) {
        IndexSet ret = new IndexSet();
        if (start < str.length()) {
            char first = str.charAt(start);
            int codePoint = first;
            int consumed = 1;
            if (Character.isHighSurrogate(first) && start + 1 < str.length()) {
                char second = str.charAt(start + 1);
                if (Character.isLowSurrogate(second)) {
                    codePoint = Character.toCodePoint(first, second);
                    consumed = 2;
                }
            }
            if (codePoint == ch) ret.set(consumed);
        }
        for (Element p : pinyin) ret.merge(p.match(str, start, partial));
        return ret;
    }

    public char get() {
        return (char) ch;
    }

    public int getCodePoint() {
        return ch;
    }

    public Pinyin[] pinyins() {
        return pinyin;
    }

    public static class Dummy extends Char {
        public Dummy() {
            super('\0', NONE);
        }

        public void set(char ch) {
            this.ch = ch;
        }

        public void set(int ch) {
            this.ch = ch;
        }
    }
}
