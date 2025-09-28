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
            if (feed == null) return;
            loadInternal(feed, null);
        }

        @Override
        public void loadCodePoints(CodePointConsumer feed) {
            if (feed == null) return;
            loadInternal(null, feed);
        }

        private void loadInternal(BiConsumer<Character, String[]> charFeed, CodePointConsumer codePointFeed) {
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
                    if (Character.isHighSurrogate(first) && line.length() > 1) {
                        char second = line.charAt(1);
                        if (Character.isLowSurrogate(second)) {
                            if (codePointFeed != null) {
                                int codePoint = Character.toCodePoint(first, second);
                                codePointFeed.accept(codePoint, records);
                            }
                            continue;
                        }
                    }

                    if (charFeed != null) charFeed.accept(first, records);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
