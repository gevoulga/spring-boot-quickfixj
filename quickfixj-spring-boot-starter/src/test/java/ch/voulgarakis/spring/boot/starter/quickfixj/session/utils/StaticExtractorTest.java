/*
 * Copyright (c) 2020 Georgios Voulgarakis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.voulgarakis.spring.boot.starter.quickfixj.session.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import quickfix.field.QuoteType;

import java.util.ArrayList;
import java.util.List;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StaticExtractor.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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