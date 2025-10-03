package me.towdium.pinin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface DictLoader {
    void load(BiConsumer<Character, String[]> feed);

    default void loadCodePoints(CodePointConsumer feed) {
    }

    @FunctionalInterface
    interface CodePointConsumer {
        void accept(int codePoint, String[] records);
    }

    class Default implements DictLoader {
        @Override
        public void load(BiConsumer<Character, String[]> feed) {
            InputStream is = PinIn.class.getResourceAsStream("data.txt");
            if (is == null) return;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    int separator = line.indexOf(':');
                    if (separator <= 0 || separator + 2 > line.length()) continue;

                    String[] records = line.substring(separator + 2).split(", ");

                    char first = line.charAt(0);
                    feed.accept(first, records);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void loadCodePoints(CodePointConsumer feed) {
            InputStream is = PinIn.class.getResourceAsStream("extra.txt");
            if (is == null) return;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    int separator = line.indexOf(':');
                    if (separator <= 0 || separator + 2 > line.length()) continue;

                    String[] code2records = line.split(": ");
                    int code = code2records[0].codePointAt(0);
                    String[] records = code2records[1].split(", ");
                    feed.accept(code, records);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
}
