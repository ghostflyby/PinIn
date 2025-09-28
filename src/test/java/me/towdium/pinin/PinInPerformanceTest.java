package me.towdium.pinin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.towdium.pinin.searchers.CachedSearcher;
import me.towdium.pinin.searchers.Searcher;
import me.towdium.pinin.searchers.SimpleSearcher;
import me.towdium.pinin.searchers.TreeSearcher;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class PinInPerformanceTest {
    @Test
    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    public void performance() throws IOException {
        List<String> search = new ArrayList<>();
        search.add("boli");
        search.add("yangmao");
        search.add("hongse");
        List<Function<Searcher.Logic, Searcher<Integer>>> funcs = new ArrayList<>();
        funcs.add(l -> new TreeSearcher<>(l, new PinIn()));
        funcs.add(l -> new CachedSearcher<>(l, new PinIn()));
        funcs.add(l -> new SimpleSearcher<>(l, new PinIn()));
        String[] sources = new String[]{"small", "large"};

        for (Searcher.Logic j : Searcher.Logic.values()) {
            for (String k : sources) {
                List<String> data = loadTestData(k);
                boolean reduced = data.size() > 100000;
                System.out.print("Logic: " + j.toString().toLowerCase() + ", source: " + k);

                float contains = time(reduced ? 10 : 100, search, s -> {
                    IntSet result = new IntOpenHashSet();
                    for (int i = 0; i < data.size(); i++) {
                        String in = data.get(i);
                        if (j.raw(in, s)) result.add(i);
                    }
                });
                System.out.print(", contains search: " + String.format("%.2f", contains));

                PinIn p = new PinIn();
                float traverse = time(reduced ? 10 : 100, search, s -> {
                    IntSet result = new IntOpenHashSet();
                    for (int i = 0; i < data.size(); i++) {
                        String in = data.get(i);
                        if (j.test(p, in, s)) result.add(i);
                    }
                });
                System.out.println(", loop search: " + String.format("%.1f", traverse));

                for (Function<Searcher.Logic, Searcher<Integer>> i : funcs) {
                    performance(j, data, search, i);
                }
            }
        }
    }

    private float time(int repeat, Runnable exec) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < repeat; i++) {
            exec.run();
        }
        return (System.currentTimeMillis() - time) / (float) repeat;
    }

    private <T> float time(int repeat, Collection<T> objs, Consumer<T> consumer) {
        long time = System.currentTimeMillis();
        for (int j = 0; j < repeat; j++) {
            for (T i : objs) {
                consumer.accept(i);
            }
        }
        return (System.currentTimeMillis() - time) / (float) repeat / objs.size();
    }

    private List<String> loadTestData(String source) throws IOException {
        String line;
        List<String> data = new ArrayList<>();
        InputStream is = PinInTest.class.getResourceAsStream(source + ".txt");
        assert is != null;
        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        while ((line = br.readLine()) != null) {
            if (line.isEmpty()) continue;
            data.add(line);
        }
        return data;
    }

    private void performance(Searcher.Logic logic, List<String> texts, List<String> tokens,
                             Function<Searcher.Logic, Searcher<Integer>> func) {
        long start = System.currentTimeMillis();
        final Searcher<Integer> searcher = func.apply(logic);
        for (int j = 0; j < texts.size(); j++) searcher.put(texts.get(j), j);
        boolean reduced = texts.size() > 100000;
        System.out.print("  " + searcher.getClass().getSimpleName());

        int loop = reduced ? 10 : 100;
        if (searcher instanceof TreeSearcher) loop /= 5;
        float construct = time(loop, () -> {
            Searcher<Integer> temp = func.apply(logic);
            for (int j = 0; j < texts.size(); j++) temp.put(texts.get(j), j);
        });
        System.out.print(": construction: " + String.format("%.1f", construct));

        if (searcher instanceof CachedSearcher) {
            float warm = time(reduced ? 10 : 100, tokens, s -> {
                List<Integer> is = searcher.search(s);
                ((CachedSearcher<Integer>) searcher).reset();
            });
            System.out.print(", warmup: " + String.format("%.1f", warm));
        }

        loop = reduced ? 1000 : 10000;
        if (searcher instanceof SimpleSearcher) loop /= 100;
        float search = time(loop, tokens, (s) -> {
            List<Integer> is = searcher.search(s);
        });
        System.out.print(", accelerated: " + String.format("%.3f", search));
        System.out.println(", total: " + (System.currentTimeMillis() - start));
    }
}
