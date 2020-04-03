package ch.voulgarakis.spring.boot.starter.quickfixj.session.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import quickfix.field.QuoteType;

import java.util.ArrayList;
import java.util.List;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StaticExtractor.*;
import static org.junit.Assert.assertEquals;

public class StaticExtractorTest {

    @Test
    public void testExtractor() {
        List<Pair<String, Object>> expected = new ArrayList<>();
        expected.add(Pair.of("INDICATIVE", 0));
        expected.add(Pair.of("TRADEABLE", 1));
        expected.add(Pair.of("RESTRICTED_TRADEABLE", 2));
        expected.add(Pair.of("COUNTER", 3));

        List<Pair<String, Object>> actual = extract(new QuoteType(), Integer.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetName() {
        QuoteType quoteType = new QuoteType(QuoteType.TRADEABLE);
        String name = toText(quoteType);

        assertEquals("TRADEABLE", name);
    }

    @Test
    public void testGetValue() {
        int value = toValue(new QuoteType(QuoteType.TRADEABLE), "TRADEABLE");

        assertEquals(QuoteType.TRADEABLE, value);
    }
}