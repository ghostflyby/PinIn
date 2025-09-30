package me.towdium.pinin.searchers;

import it.unimi.dsi.fastutil.chars.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.towdium.pinin.PinIn;
import me.towdium.pinin.elements.Char;
import me.towdium.pinin.elements.Phoneme;
import me.towdium.pinin.elements.Pinyin;
import me.towdium.pinin.utils.Accelerator;
import me.towdium.pinin.utils.Compressor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import static me.towdium.pinin.searchers.Searcher.Logic.EQUAL;

/**
 * Author: Towdium
 * Date: 21/04/19
 */
public class TreeSearcher<T> implements Searcher<T> {
    Node<T> root = new NDense<>();

    List<T> objects = new ObjectArrayList<>();
    List<NAcc<T>> naccs = new ArrayList<>();
    final Accelerator acc;
    final Compressor strs = new Compressor();
    final PinIn context;
    final Logic logic;
    final PinIn.Ticket ticket;
    static final int THRESHOLD = 128;

    public TreeSearcher(Logic logic, PinIn context) {
        this.logic = logic;
        this.context = context;
        acc = new Accelerator(context);
        acc.setProvider(strs);
        ticket = context.ticket(() -> {
            naccs.forEach(i -> i.reload(this));
            acc.reset();
        });
    }

    public void put(String name, T identifier) {
        ticket.renew();
        int pos = strs.put(name);
        if (logic == Logic.CONTAIN) {
            for (int i = 0; i < name.length(); ) {
                root = root.put(this, pos + i, objects.size());
                int consumed = Character.charCount(name.codePointAt(i));
                i += consumed;
            }
        } else {
            root = root.put(this, pos, objects.size());
        }
        objects.add(identifier);
    }

    public List<T> search(String s) {
        ticket.renew();
        acc.search(s);
        IntSet ret = new IntRBTreeSet();
        root.get(this, ret, 0);
        return ret.stream().map(i -> objects.get(i))
                .collect(Collectors.toList());
    }

    public PinIn context() {
        return context;
    }

    public void refresh() {
        ticket.renew();
    }

    interface Node<T> {
        void get(TreeSearcher<T> p, IntSet ret, int offset);

        void get(TreeSearcher<T> p, IntSet ret);

        Node<T> put(TreeSearcher<T> p, int name, int identifier);
    }

    public static class NSlice<T> implements Node<T> {
        Node<T> exit = new NMap<>();
        int start, end;

        public NSlice(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public void get(TreeSearcher<T> p, IntSet ret, int offset) {
            get(p, ret, offset, 0);
        }

        @Override
        public void get(TreeSearcher<T> p, IntSet ret) {
            exit.get(p, ret);
        }

        @Override
        public Node<T> put(TreeSearcher<T> p, int name, int identifier) {
            int length = end - start;
            int match = p.acc.common(start, name, length);
            if (match >= length) exit = exit.put(p, name + length, identifier);
            else {
                cut(p, start + match);
                exit = exit.put(p, name + match, identifier);
            }
            return start == end ? exit : this;
        }

        private void cut(TreeSearcher<T> p, int offset) {
            NMap<T> insert = new NMap<>();
            int codePoint = p.strs.get(offset);
            int consumed = Character.charCount(codePoint);
            if (offset + consumed == end) insert.putChild(codePoint, exit);
            else {
                NSlice<T> half = new NSlice<>(offset + consumed, end);
                half.exit = exit;
                insert.putChild(codePoint, half);
            }
            exit = insert;
            end = offset;
        }

        private void get(TreeSearcher<T> p, IntSet ret, int offset, int start) {
            if (this.start + start == end)
                exit.get(p, ret, offset);
            else if (offset == p.acc.search().length()) {
                if (p.logic != EQUAL) exit.get(p, ret);
            } else {
                int codePoint = p.strs.get(this.start + start);
                p.acc.get(codePoint, offset).foreach(i ->
                        get(p, ret, offset + i, start + Character.charCount(codePoint)));
            }
        }
    }

    public static class NDense<T> implements Node<T> {
        // offset, object, offset, object
        IntList data = new IntArrayList();

        @Override
        public void get(TreeSearcher<T> p, IntSet ret, int offset) {
            boolean full = p.logic == EQUAL;
            if (!full && p.acc.search().length() == offset) get(p, ret);
            else {
                for (int i = 0; i < data.size() / 2; i++) {
                    int ch = data.getInt(i * 2);
                    if (full ? p.acc.matches(offset, ch) : p.acc.begins(offset, ch))
                        ret.add(data.getInt(i * 2 + 1));
                }
            }
        }

        @Override
        public void get(TreeSearcher<T> p, IntSet ret) {
            for (int i = 0; i < data.size() / 2; i++)
                ret.add(data.getInt(i * 2 + 1));
        }

        @Override
        public Node<T> put(TreeSearcher<T> p, int name, int identifier) {
            if (data.size() >= THRESHOLD) {
                int pattern = data.getInt(0);
                Node<T> ret = new NSlice<>(pattern, pattern + match(p));
                for (int j = 0; j < data.size() / 2; j++)
                    ret.put(p, data.getInt(j * 2), data.getInt(j * 2 + 1));
                ret.put(p, name, identifier);
                return ret;
            } else {
                data.add(name);
                data.add(identifier);
                return this;
            }
        }

        private int match(TreeSearcher<T> p) {
            int offset = 0;
            for (; ; ) {
                int base = data.getInt(0) + offset;
                if (p.strs.end(base)) return offset;
                int a = p.strs.get(base);
                int consumed = Character.charCount(a);
                for (int j = 1; j < data.size() / 2; j++) {
                    int other = data.getInt(j * 2) + offset;
                    if (p.strs.end(other) || p.strs.get(other) != a) return offset;
                }
                offset += consumed;
            }
        }
    }

    public static class NMap<T> implements Node<T> {
        Char2ObjectMap<Node<T>> charChildren;
        Int2ObjectMap<Node<T>> intChildren;
        IntSet leaves = new IntArraySet(1);

        @Override
        public void get(TreeSearcher<T> p, IntSet ret, int offset) {
            if (p.acc.search().length() == offset) {
                if (p.logic == EQUAL) ret.addAll(leaves);
                else get(p, ret);
            } else {
                if (charChildren != null) {
                    charChildren.char2ObjectEntrySet().forEach(entry ->
                            p.acc.get(entry.getCharKey(), offset)
                                    .foreach(i -> entry.getValue().get(p, ret, offset + i)));
                }
                if (intChildren != null) {
                    intChildren.int2ObjectEntrySet().forEach(entry ->
                            p.acc.get(entry.getIntKey(), offset)
                                    .foreach(i -> entry.getValue().get(p, ret, offset + i)));
                }
            }
        }

        @Override
        public void get(TreeSearcher<T> p, IntSet ret) {
            ret.addAll(leaves);
            if (charChildren != null) charChildren.values().forEach(n -> n.get(p, ret));
            if (intChildren != null) intChildren.values().forEach(n -> n.get(p, ret));
        }

        @Override
        public NMap<T> put(TreeSearcher<T> p, int name, int identifier) {
            if (p.strs.end(name)) {
                if (leaves.size() >= THRESHOLD && leaves instanceof IntArraySet)
                    leaves = new IntOpenHashSet(leaves);
                leaves.add(identifier);
            } else {
                int codePoint = p.strs.get(name);
                Node<T> sub = getChild(codePoint);
                if (sub == null) {
                    sub = new NDense<>();
                    putChild(codePoint, sub);
                }
                int consumed = Character.charCount(codePoint);
                sub = sub.put(p, name + consumed, identifier);
                putChild(codePoint, sub);
            }
            return !(this instanceof NAcc) && childCount() > 32 ?
                    new NAcc<>(p, this) : this;
        }

        private void putChild(int codePoint, Node<T> node) {
            if (Character.isBmpCodePoint(codePoint)) {
                if (charChildren == null) charChildren = new Char2ObjectArrayMap<>();
                else if (charChildren.size() >= THRESHOLD && charChildren instanceof Char2ObjectArrayMap)
                    charChildren = new Char2ObjectOpenHashMap<>(charChildren);
                charChildren.put((char) codePoint, node);
            } else {
                if (intChildren == null) intChildren = new Int2ObjectArrayMap<>();
                else if (intChildren.size() >= THRESHOLD && intChildren instanceof Int2ObjectArrayMap)
                    intChildren = new Int2ObjectOpenHashMap<>(intChildren);
                intChildren.put(codePoint, node);
            }
        }

        Node<T> getChild(int codePoint) {
            if (Character.isBmpCodePoint(codePoint))
                return charChildren == null ? null : charChildren.get((char) codePoint);
            else return intChildren == null ? null : intChildren.get(codePoint);
        }

        void forEachChildCodePoint(IntConsumer consumer) {
            if (charChildren != null)
                charChildren.keySet().forEach(consumer);
            if (intChildren != null)
                intChildren.keySet().forEach(consumer);
        }

        private int childCount() {
            int count = 0;
            if (charChildren != null) count += charChildren.size();
            if (intChildren != null) count += intChildren.size();
            return count;
        }
    }

    public static class NAcc<T> extends NMap<T> {
        Map<Phoneme, CodePointIndex> index = new Object2ObjectArrayMap<>();

        private NAcc(TreeSearcher<T> p, NMap<T> n) {
            charChildren = n.charChildren;
            intChildren = n.intChildren;
            leaves = n.leaves;
            reload(p);
            p.naccs.add(this);
        }

        @Override
        public void get(TreeSearcher<T> p, IntSet ret, int offset) {
            if (p.acc.search().length() == offset) {
                if (p.logic == EQUAL) ret.addAll(leaves);
                else get(p, ret);
            } else {
                int searchCodePoint = p.acc.searchCodePoint(offset);
                int consumed = Character.charCount(searchCodePoint);
                Node<T> n = getChild(searchCodePoint);
                if (n != null) n.get(p, ret, offset + consumed);
                index.forEach((k, v) -> {
                    if (!k.match(p.acc.search(), offset, true).isEmpty()) {
                        v.forEach(i -> {
                            Node<T> child = getChild(i);
                            if (child != null) {
                                p.acc.get(i, offset)
                                        .foreach(j -> child.get(p, ret, offset + j));
                            }
                        });
                    }
                });
            }
        }

        @Override
        public NAcc<T> put(TreeSearcher<T> p, int name, int identifier) {
            super.put(p, name, identifier);
            if (!p.strs.end(name)) index(p, p.strs.get(name));
            return this;
        }

        public void reload(TreeSearcher<T> p) {
            index.clear();
            forEachChildCodePoint(i -> index(p, i));
        }

        private void index(TreeSearcher<T> p, int codePoint) {
            Char ch = p.context.getChar(codePoint);
            for (Pinyin py : ch.pinyins()) {
                CodePointIndex bucket = index.computeIfAbsent(py.phonemes()[0], k -> new CodePointIndex());
                bucket.add(codePoint);
            }
        }

        private static class CodePointIndex {
            CharSet chars;
            IntSet ints;

            void add(int codePoint) {
                if (Character.isBmpCodePoint(codePoint)) {
                    if (chars == null) chars = new CharArraySet();
                    else if (chars instanceof CharArraySet && chars.size() >= THRESHOLD && !chars.contains((char) codePoint))
                        chars = new CharOpenHashSet((CharCollection) chars);
                    chars.add((char) codePoint);
                } else {
                    if (ints == null) ints = new IntArraySet();
                    else if (ints instanceof IntArraySet && ints.size() >= THRESHOLD && !ints.contains(codePoint))
                        ints = new IntOpenHashSet((IntCollection) ints);
                    ints.add(codePoint);
                }
            }

            void forEach(IntConsumer consumer) {
                if (chars != null) chars.forEach(consumer);
                if (ints != null) ints.forEach(consumer);
            }
        }
    }
}
