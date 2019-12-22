package ch.voulgarakis.spring.boot.starter.quickfixj.utils;

import org.assertj.core.util.Lists;
import quickfix.FieldMap;
import quickfix.Group;
import quickfix.field.BodyLength;
import quickfix.field.CheckSum;
import quickfix.field.TransactTime;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Comparator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;

public class FixMessageTestUtils {

    private static final List<Integer> IGNORED_FIELDS = Lists.list(
            CheckSum.FIELD,
            TransactTime.FIELD,
            BodyLength.FIELD
    );

    public static void assertFixMessagesEquals(FieldMap expectedFix, FieldMap actualFix) {

        List<? extends Tuple2<Integer, ?>> expectedFixFields = extractFixFields(expectedFix);
        List<? extends Tuple2<Integer, ?>> actualFixFields = extractFixFields(actualFix);
        assertEquals("Fields are not equal.", expectedFixFields, actualFixFields);

        List<Group> expectedGroups = extractGroups(expectedFix);
        List<Group> actualGroups = extractGroups(actualFix);

        List<Integer> expectedGroupTags = expectedGroups.stream().map(Group::getFieldTag).collect(Collectors.toList());
        List<Integer> actualGroupTags = actualGroups.stream().map(Group::getFieldTag).collect(Collectors.toList());
        assertEquals("Group tags are not equal.", expectedGroupTags, actualGroupTags);


        for (int i = 0; i < expectedGroups.size(); i++) {
            assertFixMessagesEquals(expectedGroups.get(i), actualGroups.get(i));
        }
    }

    private static List<? extends Tuple2<Integer, ?>> extractFixFields(FieldMap fix) {
        return StreamSupport.stream(Spliterators
                .spliteratorUnknownSize(fix.iterator(), Spliterator.ORDERED), false)
                .filter(field -> !IGNORED_FIELDS.contains(field.getTag()))
                .map(field -> Tuples.of(field.getTag(), field.getObject()))
                .sorted(Comparator.comparingInt(Tuple2::getT1))
                .collect(Collectors.toList());
    }

    private static List<Group> extractGroups(FieldMap fix) {
        return StreamSupport.stream(Spliterators
                .spliteratorUnknownSize(fix.groupKeyIterator(), Spliterator.ORDERED), false)
                .filter(groupTag -> !IGNORED_FIELDS.contains(groupTag))
                .sorted(Integer::compare)
                .flatMap(groupTag -> fix.getGroups(groupTag).stream())
                .collect(Collectors.toList());
    }
}
