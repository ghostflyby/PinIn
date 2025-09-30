package me.towdium.pinin;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import me.towdium.pinin.elements.Char;
import me.towdium.pinin.elements.Phoneme;
import me.towdium.pinin.elements.Pinyin;
import me.towdium.pinin.utils.Accelerator;
import me.towdium.pinin.utils.Cache;
import me.towdium.pinin.utils.IndexSet;
import me.towdium.pinin.utils.PinyinFormat;

@SuppressWarnings("unused")
public class PinIn {
    public final Cache<String, Phoneme> phonemes = new Cache<>(s -> new Phoneme(s, this));
    private final Char[] chars = new Char[Character.MAX_VALUE];
    private final Int2ObjectArrayMap<Char> codePoints = new Int2ObjectArrayMap<>();
    private final Char.Dummy temp = new Char.Dummy();
    private final ThreadLocal<Accelerator> acc;
    private int total = 0;
    public final Cache<String, Pinyin> pinyins = new Cache<>(s -> new Pinyin(s, this, total++));
    private Keyboard keyboard = Keyboard.QUANPIN;
    private int modification = 0;
    private boolean fZh2Z = false;
    private boolean fSh2S = false;
    private boolean fCh2C = false;
    private boolean fAng2An = false;
    private boolean fIng2In = false;
    private boolean fEng2En = false;
    private boolean fU2V = false;
    private boolean accelerate = false;
    private PinyinFormat format = PinyinFormat.NUMBER;

    /**
     * Use PinIn object to manage the context
     * To configure it, use {@link #config()}
     */
    public PinIn() {
        this(new DictLoader.Default());
    }

    public PinIn(DictLoader loader) {
        acc = ThreadLocal.withInitial(() -> new Accelerator(this));
        loader.load((c, ss) -> {
            if (ss == null) {
                chars[c] = null;
            } else {
                Pinyin[] pinyins = new Pinyin[ss.length];
                for (int i = 0; i < ss.length; i++) {
                    pinyins[i] = getPinyin(ss[i]);
                }
                chars[c] = new Char(c, pinyins);
            }
        });
        loader.loadCodePoints((cp, ss) -> {
            if (ss == null) {
                if (Character.isBmpCodePoint(cp)) {
                    chars[cp] = null;
                } else {
                    codePoints.remove(cp);
                }
                return;
            }

            Pinyin[] pinyins = new Pinyin[ss.length];
            for (int i = 0; i < ss.length; i++) {
                pinyins[i] = getPinyin(ss[i]);
            }

            Char value = new Char(cp, pinyins);
            if (Character.isBmpCodePoint(cp)) {
                chars[cp] = value;
                codePoints.remove(cp);
            } else {
                codePoints.put(cp, value);
            }
        });
    }

    public boolean contains(String s1, String s2) {
        if (accelerate) {
            Accelerator a = acc.get();
            a.setProvider(s1);
            a.search(s2);
            return a.contains(0, 0);
        } else return Matcher.contains(s1, s2, this);
    }

    public boolean begins(String s1, String s2) {
        if (accelerate) {
            Accelerator a = acc.get();
            a.setProvider(s1);
            a.search(s2);
            return a.begins(0, 0);
        } else return Matcher.begins(s1, s2, this);
    }

    public boolean matches(String s1, String s2) {
        if (accelerate) {
            Accelerator a = acc.get();
            a.setProvider(s1);
            a.search(s2);
            return a.matches(0, 0);
        } else return Matcher.matches(s1, s2, this);
    }

    public Phoneme getPhoneme(String s) {
        return phonemes.get(s);
    }

    public Pinyin getPinyin(String s) {
        return pinyins.get(s);
    }

    public Char getChar(char c) {
        Char ret = chars[c];
        if (ret != null) {
            return ret;
        } else {
            temp.set(c);
            return temp;
        }
    }

    public Char getChar(int codePoint) {
        if (Character.isBmpCodePoint(codePoint)) {
            return getChar((char) codePoint);
        }
        Char ret = codePoints.get(codePoint);
        if (ret != null) {
            return ret;
        } else {
            temp.set(codePoint);
            return temp;
        }
    }

    public Keyboard keyboard() {
        return keyboard;
    }

    public boolean fZh2Z() {
        return fZh2Z;
    }

    public boolean fSh2S() {
        return fSh2S;
    }

    public boolean fCh2C() {
        return fCh2C;
    }

    public boolean fAng2An() {
        return fAng2An;
    }

    public boolean fIng2In() {
        return fIng2In;
    }

    public boolean fEng2En() {
        return fEng2En;
    }

    public boolean fU2V() {
        return fU2V;
    }

    public PinyinFormat format() {
        return format;
    }

    public String format(Pinyin p) {
        return format.format(p);
    }

    /**
     * Set values in returned {@link Config} object,
     * then use {@link Config#commit()} to apply
     */
    public Config config() {
        return new Config();
    }

    public Ticket ticket(Runnable r) {
        return new Ticket(r);
    }

    private void config(Config c) {
        format = c.format;

        if (fAng2An == c.fAng2An && fEng2En == c.fEng2En && fIng2In == c.fIng2In
                && fZh2Z == c.fZh2Z && fSh2S == c.fSh2S && fCh2C == c.fCh2C
                && keyboard == c.keyboard && fU2V == c.fU2V && accelerate == c.accelerate) return;

        keyboard = c.keyboard;
        fZh2Z = c.fZh2Z;
        fSh2S = c.fSh2S;
        fCh2C = c.fCh2C;
        fAng2An = c.fAng2An;
        fIng2In = c.fIng2In;
        fEng2En = c.fEng2En;
        fU2V = c.fU2V;
        accelerate = c.accelerate;
        phonemes.foreach((s, p) -> p.reload(s, this));
        pinyins.foreach((s, p) -> p.reload(s, this));
        modification++;
    }

    public static class Matcher {
        public static boolean begins(String s1, String s2, PinIn p) {
            if (s1.isEmpty()) return s1.startsWith(s2);
            else return check(s1, 0, s2, 0, p, true);
        }

        public static boolean contains(String s1, String s2, PinIn p) {
            if (s1.isEmpty()) return s1.contains(s2);
            else {
                for (int i = 0; i < s1.length(); ) {
                    if (check(s1, i, s2, 0, p, true)) return true;
                    char ch = s1.charAt(i);
                    if (Character.isHighSurrogate(ch) && i + 1 < s1.length()
                            && Character.isLowSurrogate(s1.charAt(i + 1))) {
                        i += 2;
                    } else i++;
                }
                return false;
            }
        }

        public static boolean matches(String s1, String s2, PinIn p) {
            if (s1.isEmpty()) return s1.equals(s2);
            else return check(s1, 0, s2, 0, p, false);
        }

        private static boolean check(String s1, int start1, String s2, int start2, PinIn p, boolean partial) {
            if (start2 == s2.length()) return partial || start1 == s1.length();
            if (start1 >= s1.length()) return false;

            char first = s1.charAt(start1);
            int consumed = 1;
            int codePoint = first;
            if (Character.isHighSurrogate(first) && start1 + 1 < s1.length()) {
                char second = s1.charAt(start1 + 1);
                if (Character.isLowSurrogate(second)) {
                    codePoint = Character.toCodePoint(first, second);
                    consumed = 2;
                }
            }
            Char r = p.getChar(codePoint);
            IndexSet s = r.match(s2, start2, partial);

            int next = start1 + consumed;
            if (next >= s1.length()) {
                int i = s2.length() - start2;
                return s.get(i);
            } else return s.traverse(i -> check(s1, next, s2, start2 + i, p, partial));
        }
    }

    public class Ticket {
        int modification;
        Runnable runnable;

        private Ticket(Runnable r) {
            runnable = r;
            modification = PinIn.this.modification;
        }

        public void renew() {
            int i = PinIn.this.modification;
            if (modification != i) {
                modification = i;
                runnable.run();
            }
        }
    }

    public class Config {
        public Keyboard keyboard;
        public boolean fZh2Z;
        public boolean fSh2S;
        public boolean fCh2C;
        public boolean fAng2An;
        public boolean fIng2In;
        public boolean fEng2En;
        public boolean fU2V;
        public boolean accelerate;
        public PinyinFormat format;

        private Config() {
            keyboard = PinIn.this.keyboard;
            fZh2Z = PinIn.this.fZh2Z;
            fSh2S = PinIn.this.fSh2S;
            fCh2C = PinIn.this.fCh2C;
            fAng2An = PinIn.this.fAng2An;
            fIng2In = PinIn.this.fIng2In;
            fEng2En = PinIn.this.fEng2En;
            fU2V = PinIn.this.fU2V;
            accelerate = PinIn.this.accelerate;
            format = PinyinFormat.NUMBER;
        }

        public Config keyboard(Keyboard keyboard) {
            this.keyboard = keyboard;
            return this;
        }

        public Config fZh2Z(boolean fZh2Z) {
            this.fZh2Z = fZh2Z;
            return this;
        }

        public Config fSh2S(boolean fSh2S) {
            this.fSh2S = fSh2S;
            return this;
        }

        public Config fCh2C(boolean fCh2C) {
            this.fCh2C = fCh2C;
            return this;
        }

        public Config fAng2An(boolean fAng2An) {
            this.fAng2An = fAng2An;
            return this;
        }

        public Config fIng2In(boolean fIng2In) {
            this.fIng2In = fIng2In;
            return this;
        }

        public Config fEng2En(boolean fEng2En) {
            this.fEng2En = fEng2En;
            return this;
        }

        public Config fU2V(boolean fU2V) {
            this.fU2V = fU2V;
            return this;
        }

        public Config format(PinyinFormat format) {
            this.format = format;
            return this;
        }

        /**
         * Set accelerate mode of immediate matching.
         * When working in accelerate mode, accelerator will be used.
         * <p>
         * When calling immediate matching functions continuously with
         * different {@code s1} but same {@code s2}, for, say, 100 times,
         * they are considered stable calls.
         * If the scenario uses mainly stable calls and most of {@code s1}
         * contains Chinese characters, using accelerate mode provides
         * significant speed up. Otherwise, overhead of cache management
         * in accelerator will slow down the matching process.
         * Accelerate mode is disabled by default for consistency in
         * different scenarios.
         */
        public Config accelerate(boolean accelerate) {
            this.accelerate = accelerate;
            return this;
        }

        public PinIn commit() {
            PinIn.this.config(this);
            return PinIn.this;
        }
    }
}
