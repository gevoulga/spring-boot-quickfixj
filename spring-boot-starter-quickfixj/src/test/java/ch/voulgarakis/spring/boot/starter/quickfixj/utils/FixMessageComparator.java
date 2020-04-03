package ch.voulgarakis.spring.boot.starter.quickfixj.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import quickfix.FieldMap;
import quickfix.Group;
import quickfix.field.BodyLength;
import quickfix.field.CheckSum;
import quickfix.field.TransactTime;

import java.util.Comparator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;

public class FixMessageComparator {
    public static final FixMessageComparator FIX_MESSAGE_COMPARATOR = new FixMessageComparator();

    private static final int[] IGNORED_FIELDS = new int[]{
            CheckSum.FIELD,
            TransactTime.FIELD,
            BodyLength.FIELD
    };
    private static final String FIX_DELIMITER = "\u0001";
    private static final String TAG_VALUE_SEPARATOR = "=";

    private final List<Integer> ignoredFields;

    public FixMessageComparator(int... ignoredFields) {
        int[] fields = ArrayUtils.addAll(IGNORED_FIELDS, ignoredFields);
        this.ignoredFields = IntStream.of(fields).boxed().collect(Collectors.toList());
    }

    public void assertFixMessagesEquals(String expectedFix, String actualFix) {
        List<Pair<Integer, String>> expected = parse(expectedFix);
        List<Pair<Integer, String>> actual = parse(actualFix);

        List<Integer> expectedTags = expected.stream().map(Pair::getLeft).collect(Collectors.toList());
        List<Integer> actualTags = actual.stream().map(Pair::getLeft).collect(Collectors.toList());
        assertEquals("Tags are not equal.", expectedTags, actualTags);

        List<String> expectedValues = expected.stream().map(Pair::getRight).collect(Collectors.toList());
        List<String> actualValues = actual.stream().map(Pair::getRight).collect(Collectors.toList());
        assertEquals("Values are not equal.", expectedValues, actualValues);
    }

    public List<Pair<Integer, String>> parse(String fix) {
        return Stream.of(fix.split(FIX_DELIMITER))
                .map(s -> {
                    String[] split = s.split(TAG_VALUE_SEPARATOR, 2);
                    return Pair.of(Integer.valueOf(split[0]), split[1]);
                })
                .filter(t -> !ignoredFields.contains(t.getLeft()))
                .sorted(Comparator.comparingInt(Pair::getLeft))
                .collect(Collectors.toList());
    }

    public void assertFixMessagesEquals(FieldMap expectedFix, FieldMap actualFix) {
        List<? extends Pair<Integer, ?>> expectedFixFields = extractFixFields(expectedFix);
        List<? extends Pair<Integer, ?>> actualFixFields = extractFixFields(actualFix);
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

    private List<? extends Pair<Integer, ?>> extractFixFields(FieldMap fix) {
        return StreamSupport.stream(Spliterators
                .spliteratorUnknownSize(fix.iterator(), Spliterator.ORDERED), false)
                .filter(field -> !ignoredFields.contains(field.getTag()))
                .map(field -> Pair.of(field.getTag(), field.getObject()))
                .sorted(Comparator.comparingInt(Pair::getLeft))
                .collect(Collectors.toList());
    }

    private List<Group> extractGroups(FieldMap fix) {
        return StreamSupport.stream(Spliterators
                .spliteratorUnknownSize(fix.groupKeyIterator(), Spliterator.ORDERED), false)
                .filter(groupTag -> !ignoredFields.contains(groupTag))
                .sorted(Integer::compare)
                .flatMap(groupTag -> fix.getGroups(groupTag).stream())
                .collect(Collectors.toList());
    }
}
