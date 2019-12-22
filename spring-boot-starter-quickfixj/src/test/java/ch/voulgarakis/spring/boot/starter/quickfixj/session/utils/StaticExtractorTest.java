package ch.voulgarakis.spring.boot.starter.quickfixj.session.utils;

import org.junit.Test;
import quickfix.field.QuoteType;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.List;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StaticExtractor.*;
import static org.junit.Assert.assertEquals;

public class StaticExtractorTest {

    @Test
    public void testExtractor() {
        List<Tuple2<String, Object>> expected = new ArrayList<>();
        expected.add(Tuples.of("INDICATIVE", 0));
        expected.add(Tuples.of("TRADEABLE", 1));
        expected.add(Tuples.of("RESTRICTED_TRADEABLE", 2));
        expected.add(Tuples.of("COUNTER", 3));

        List<Tuple2<String, Object>> actual = extract(new QuoteType(), Integer.class);
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