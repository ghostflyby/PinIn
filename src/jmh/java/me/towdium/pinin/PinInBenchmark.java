package me.towdium.pinin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.towdium.pinin.searchers.CachedSearcher;
import me.towdium.pinin.searchers.Searcher;
import me.towdium.pinin.searchers.SimpleSearcher;
import me.towdium.pinin.searchers.TreeSearcher;
import org.openjdk.jmh.annotations.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1)
@Fork(1)
public class PinInBenchmark {

    static Map<String, List<String>> textsMap = new HashMap<>();
    static List<String> search = Arrays.asList("boli", "yangmao", "hongse");

    static {
        textsMap.put("small", loadTestData("small"));
        textsMap.put("large", loadTestData("large"));
    }

    @Param({"small", "large"})
    public String dataset;
    @Param({"BEGIN", "CONTAIN", "EQUAL"})
    public Searcher.Logic logic;
    List<String> data;
    Searcher<Integer> tree;
    Searcher<Integer> cached;
    Searcher<Integer> simple;

    PinIn pinIn;

    private static List<String> loadTestData(String source) {
        String line;
        List<String> data = new ArrayList<>();
        InputStream is = PinInBenchmark.class.getResourceAsStream(source + ".txt");
        if (is == null) throw new IllegalStateException("Missing dataset: " + source);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) data.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    @Setup(Level.Trial)
    public void setup() {
        data = textsMap.get(dataset);
        tree = createSearcher(l -> new TreeSearcher<>(l, new PinIn()), logic);
        cached = createSearcher(l -> new CachedSearcher<>(l, new PinIn()), logic);
        simple = createSearcher(l -> new SimpleSearcher<>(l, new PinIn()), logic);
        pinIn = new PinIn();
    }

    @Benchmark
    public void containsSearch() {
        for (var s : search) {
            @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
            IntSet result = new IntOpenHashSet();
            for (int i = 0; i < data.size(); i++) {
                String in = data.get(i);
                if (logic.raw(in, s)) result.add(i);
            }
        }
    }

    @Benchmark
    public void loopSearch() {
        for (var s : search) {
            @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
            IntSet result = new IntOpenHashSet();
            for (int i = 0; i < data.size(); i++) {
                String in = data.get(i);
                if (logic.test(pinIn, in, s)) result.add(i);
            }
        }
    }

    @Benchmark
    public int treeSearcher() {
        return benchmarkSearcher(tree);
    }

    @Benchmark
    public int cachedSearcher() {
        return benchmarkSearcher(cached);
    }

    @Benchmark
    public int simpleSearcher() {
        return benchmarkSearcher(simple);
    }

    private int benchmarkSearcher(Searcher<Integer> searcher) {
        IntSet result = new IntOpenHashSet();
        for (String token : search) {
            List<Integer> indices = searcher.search(token);
            result.addAll(indices);
        }
        return result.size();
    }

    private Searcher<Integer> createSearcher(Function<Searcher.Logic, Searcher<Integer>> factory,
                                             Searcher.Logic logic) {
        Searcher<Integer> searcher = factory.apply(logic);
        for (int i = 0; i < data.size(); i++) searcher.put(data.get(i), i);
        return searcher;
    }

}
