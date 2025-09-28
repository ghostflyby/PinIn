package me.towdium.pinin.utils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.towdium.pinin.PinIn;
import me.towdium.pinin.elements.Char;
import me.towdium.pinin.elements.Pinyin;

import java.util.List;

public class Accelerator {
    final PinIn context;
    List<IndexSet.Storage> cache;
    char[] searchChars = new char[0];
    String searchStr;
    Provider provider;
    Str str = new Str();
    boolean partial;

    public Accelerator(PinIn context) {
        this.context = context;
    }

    public void search(String s) {
        if (!s.equals(searchStr)) {
            // cache the last search token to reuse phoneme matches across calls
            searchStr = s;
            searchChars = s.toCharArray();
            reset();
        }
    }

    public IndexSet get(int codePoint, int offset) {
        Char c = context.getChar(codePoint);
        IndexSet ret = new IndexSet();
        if (offset < searchChars.length) {
            int searchCode = searchCodePoint(offset);
            if (searchCode == c.getCodePoint()) {
                ret.set(Character.charCount(searchCode));
            }
        }
        for (Pinyin p : c.pinyins()) ret.merge(get(p, offset));
        return ret;
    }

    public IndexSet get(Pinyin p, int offset) {
        for (int i = cache.size(); i <= offset; i++)
            cache.add(new IndexSet.Storage());
        IndexSet.Storage data = cache.get(offset);
        IndexSet ret = data.get(p.id);
        if (ret == null) {
            ret = p.match(searchStr, offset, partial);
            data.set(ret, p.id);
        }
        return ret;
    }

    public void setProvider(Provider p) {
        provider = p;
    }

    public void setProvider(String s) {
        str.s = s;
        provider = str;
    }

    public void reset() {
        cache = new ObjectArrayList<>();
    }

    // offset - offset in search string
    // start - start point in raw text
    public boolean check(int offset, int start) {
        if (offset == searchChars.length) return partial || provider.end(start);
        if (provider.end(start)) return false;

        int codePoint = provider.get(start);
        IndexSet s = get(codePoint, offset);
        int consumed = Character.charCount(codePoint);

        if (provider.end(start + consumed)) {
            int remaining = searchChars.length - offset;
            return s.get(remaining);
        } else return s.traverse(i -> check(offset + i, start + consumed));
    }

    public boolean matches(int offset, int start) {
        if (partial) {
            partial = false;
            reset();
        }
        return check(offset, start);
    }

    public boolean begins(int offset, int start) {
        if (!partial) {
            partial = true;
            reset();
        }
        return check(offset, start);
    }

    public boolean contains(int offset, int start) {
        if (!partial) {
            partial = true;
            reset();
        }
        for (int i = start; !provider.end(i); ) {
            if (check(offset, i)) return true;
            int consumed = Character.charCount(provider.get(i));
            i += consumed;
        }
        return false;
    }

    public String search() {
        return searchStr;
    }

    public int searchCodePoint(int offset) {
        return searchCodePoint(offset, searchChars);
    }

    public int common(int s1, int s2, int max) {
        int o1 = s1;
        int o2 = s2;
        int matched = 0;
        while (matched < max) {
            if (provider.end(o1) || provider.end(o2)) return matched;
            int a = provider.get(o1);
            int b = provider.get(o2);
            if (a != b) return matched;
            int consumed = Character.charCount(a);
            o1 += consumed;
            o2 += consumed;
            matched += consumed;
        }
        return max;
    }

    public interface Provider {
        boolean end(int i);

        int get(int i);
    }

    static class Str implements Provider {
        String s;

        @Override
        public boolean end(int i) {
            return i >= s.length();
        }

        @Override
        public int get(int i) {
            char first = s.charAt(i);
            if (Character.isHighSurrogate(first) && i + 1 < s.length()) {
                char second = s.charAt(i + 1);
                if (Character.isLowSurrogate(second)) return Character.toCodePoint(first, second);
            }
            return first;
        }
    }

    private static int searchCodePoint(int offset, char[] chars) {
        char first = chars[offset];
        if (Character.isHighSurrogate(first) && offset + 1 < chars.length) {
            char second = chars[offset + 1];
            if (Character.isLowSurrogate(second)) return Character.toCodePoint(first, second);
        }
        return first;
    }
}
