package me.towdium.pinin;

import me.towdium.pinin.elements.Element;
import me.towdium.pinin.utils.IndexSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;

public class CharLayoutTest {


    @Test
    public void layout() {
        var intBased = ClassLayout.parseClass(CharInt.class);
        System.out.println(intBased.toPrintable());

        var charBased = ClassLayout.parseClass(CharChar.class);
        System.out.println(charBased.toPrintable());

        Assertions.assertEquals(intBased.instanceSize(), charBased.instanceSize());
    }

    @SuppressWarnings("unused")
    static class CharInt implements Element {
        Object obj = null;
        int ch;

        @Override
        public IndexSet match(String str, int start, boolean partial) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    static class CharChar implements Element {
        Object obj = null;
        char ch;

        @Override
        public IndexSet match(String str, int start, boolean partial) {
            return null;
        }
    }
}
