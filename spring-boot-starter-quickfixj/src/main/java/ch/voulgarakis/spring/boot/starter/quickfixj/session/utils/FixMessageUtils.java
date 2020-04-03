package ch.voulgarakis.spring.boot.starter.quickfixj.session.utils;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import org.springframework.lang.Nullable;
import quickfix.*;
import quickfix.field.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.time.format.DateTimeFormatter.ofPattern;

public class FixMessageUtils {

    private static final DateTimeFormatter FIX_TIME_FORMAT = ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FIX_DATE_FORMAT = ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FIX_DATE_N_TIME_FORMAT = ofPattern("yyyyMMdd-HH:mm:ss.SSS");

    public static boolean isMessageOfType(Message message, String... types) {
        String messageType = safeGetField(message.getHeader(), new MsgType())
                .orElseThrow(() -> new QuickFixJException("messageType not found"));
        return Arrays.asList(types).contains(messageType);
    }

    public static Optional<String> safeGetIdForRequest(Message message) {
        if (Objects.isNull(message)) {
            return Optional.empty();
        }

        String messageType = safeGetField(message.getHeader(), new MsgType())
                .orElseThrow(() -> new QuickFixJException("messageType not found"));

        switch (messageType) {
            case MsgType.QUOTE_REQUEST:
            case MsgType.QUOTE_CANCEL:
                return safeGetField(message, new QuoteReqID());
            case MsgType.MARKET_DATA_REQUEST:
                return safeGetField(message, new MDReqID());
            case MsgType.NEW_ORDER_MULTILEG:
            case MsgType.NEW_ORDER_CROSS:
            case MsgType.ORDER_SINGLE:
                return safeGetField(message, new ClOrdID());
            case MsgType.TRADE_CAPTURE_REPORT:
            case MsgType.TRADE_CAPTURE_REPORT_REQUEST:
                return safeGetField(message, new TradeReportID());
            default:
                return Optional.empty();
            //                return safeGetField(message.getHeader(), new MsgSeqNum()).map(Object::toString)
            //                    .orElseThrow(() -> new QuickFixJException("RefSeqNum not found"));
        }
    }

    public static List<String> safeGetRefIdForResponse(Message message) {
        if (Objects.isNull(message)) {
            return of();
        }

        String messageType = safeGetField(message.getHeader(), new MsgType())
                .orElseThrow(() -> new QuickFixJException("messageType not found"));

        switch (messageType) {
            case MsgType.QUOTE:
            case MsgType.QUOTE_REQUEST_REJECT:
                return of(safeGetField(message, new QuoteReqID()));
            case MsgType.QUOTE_RESPONSE:
                return of(safeGetField(message, new QuoteRespID()));
            case MsgType.MARKET_DATA_INCREMENTAL_REFRESH:
            case MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH:
            case MsgType.MARKET_DATA_REQUEST:
            case MsgType.MARKET_DATA_REQUEST_REJECT:
                return of(safeGetField(message, new MDReqID()));
            case MsgType.EXECUTION_ACKNOWLEDGEMENT:
            case MsgType.EXECUTION_REPORT:
                return of(
                        safeGetField(message, new ExecRefID()),
                        safeGetField(message, new ClOrdID())
                );
            case MsgType.TRADE_CAPTURE_REPORT_ACK:
            case MsgType.TRADE_CAPTURE_REPORT_REQUEST_ACK:
                return of(safeGetField(message, new ClOrdID()));
            case MsgType.TRADE_CAPTURE_REPORT:
                return of(
                        safeGetField(message, new TradeReportID()),
                        safeGetField(message, new ClOrdID())
                );
            default:
                return of();
            //                return safeGetField(message, new RefSeqNum()).map(Object::toString)
            //                    .orElseThrow(() -> new QuickFixJException("RefSeqNum not found"));

            //                Optional<Integer> msgSeqNum = safeGetField(message.getHeader(), new MsgSeqNum());
            //                Optional<Integer> refSeqNum = safeGetField(message, new RefSeqNum());
            //                String id = of(msgSeqNum, refSeqNum).map(Object::toString).collect(Collectors.joining("-"));
            //                if (StringUtils.isNotBlank(id)) {
            //                    return id;
            //                } else {
            //                    throw new QuickFixJException("ID not found");
            //                }
        }
    }

    @SafeVarargs
    public static <T> List<T> of(Optional<T>... optionals) {
        return Stream.of(optionals)
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList());
    }

    public static <T> Stream<T> stream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    public static List<List<Group>> findAnyGroups(Group group, Integer... tags) {
        Map<Integer, List<Group>> groupMap = stream(group.groupKeyIterator()).collect(
                Collectors.toMap(tag -> tag, group::getGroups));
        return Stream.of(tags)
                .map(tag -> Optional.ofNullable(groupMap.get(tag)))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList());
    }

    public static List<List<Group>> findAllGroups(Group group, Integer... tags) {
        List<List<Group>> anyGroups = findAnyGroups(group, tags);
        return anyGroups.size() == tags.length ? anyGroups : new ArrayList<>();
    }

    public static List<? extends Field<?>> findAnyFields(Group group, Integer... tags) {
        List<Integer> tagsList = Arrays.asList(tags);
        Map<Integer, ? extends Field<?>> fieldMap = stream(group.iterator())
                .filter(field -> tagsList.contains(field.getTag()))
                .collect(Collectors.toMap(Field::getTag, field -> field));
        return Stream.of(tags)
                .map(tag -> Optional.ofNullable(fieldMap.get(tag)))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList());
    }

    public static List<? extends Field<?>> findAllFields(Group group, Integer... tags) {
        List<? extends Field<?>> anyFields = findAnyFields(group, tags);
        return anyFields.size() == tags.length ? anyFields : new ArrayList<>();
    }

    public static boolean allFieldsExist(Group group, Integer... tags) {
        return Stream.of(tags).noneMatch(group::isSetField);
    }

    public static boolean anyFieldsExist(Group group, Integer... tags) {
        return Stream.of(tags).anyMatch(group::isSetField);
    }

    public static boolean noFieldsExist(Group group, Integer... tags) {
        return Stream.of(tags).noneMatch(group::isSetField);
    }

    public static List<Integer> safeGetFieldFromGroup(FieldMap group, int field, IntField intField) {
        return group.getGroups(field).stream().map(gr -> safeGetField(gr, intField)).flatMap(
                o -> o.map(Stream::of).orElseGet(Stream::empty)).collect(Collectors.toList());
    }

    public static List<String> safeGetFieldFromGroup(FieldMap group, int field, StringField stringField) {
        return group.getGroups(field).stream().map(gr -> safeGetField(gr, stringField)).flatMap(
                o -> o.map(Stream::of).orElseGet(Stream::empty)).collect(Collectors.toList());
    }

    public static List<Double> safeGetFieldFromGroup(FieldMap group, int field, DoubleField doubleField) {
        return group.getGroups(field).stream().map(gr -> safeGetField(gr, doubleField)).flatMap(
                o -> o.map(Stream::of).orElseGet(Stream::empty)).collect(Collectors.toList());
    }

    public static List<Character> safeGetFieldFromGroup(FieldMap group, int field, CharField charField) {
        return group.getGroups(field).stream().map(gr -> safeGetField(gr, charField)).flatMap(
                o -> o.map(Stream::of).orElseGet(Stream::empty)).collect(Collectors.toList());
    }

    public static List<Boolean> safeGetFieldFromGroup(FieldMap group, int field, BooleanField booleanField) {
        return group.getGroups(field).stream().map(gr -> safeGetField(gr, booleanField)).flatMap(
                o -> o.map(Stream::of).orElseGet(Stream::empty)).collect(Collectors.toList());
    }

    public static List<LocalTime> safeGetFieldFromGroup(FieldMap group, int field,
                                                        UtcTimeOnlyField utcTimeOnlyField) {
        return group.getGroups(field).stream().map(gr -> safeGetField(gr, utcTimeOnlyField)).flatMap(
                o -> o.map(Stream::of).orElseGet(Stream::empty)).collect(Collectors.toList());
    }

    public static List<LocalDate> safeGetFieldFromGroup(FieldMap group, int field,
                                                        UtcDateOnlyField utcDateOnlyField) {
        return group.getGroups(field).stream().map(gr -> safeGetField(gr, utcDateOnlyField)).flatMap(
                o -> o.map(Stream::of).orElseGet(Stream::empty)).collect(Collectors.toList());
    }

    public static List<Instant> safeGetFieldFromGroup(FieldMap group, int field,
                                                      UtcTimeStampField utcTimeStampField) {
        return group.getGroups(field).stream().map(gr -> safeGetField(gr, utcTimeStampField)).flatMap(
                o -> o.map(Stream::of).orElseGet(Stream::empty)).collect(Collectors.toList());
    }

    public static Optional<String> safeGetField(FieldMap group, StringField field) {
        if (Objects.nonNull(group) && group.isSetField(field.getField())) {
            try {
                group.getField(field);
                return Optional.of(field.getValue());
            } catch (FieldNotFound e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<Character> safeGetField(FieldMap group, CharField field) {
        if (Objects.nonNull(group) && group.isSetField(field.getField())) {
            try {
                group.getField(field);
                return Optional.of(field.getValue());
            } catch (FieldNotFound e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<Boolean> safeGetField(FieldMap group, BooleanField field) {
        if (Objects.nonNull(group) && group.isSetField(field.getField())) {
            try {
                group.getField(field);
                return Optional.of(field.getValue());
            } catch (FieldNotFound e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<Instant> safeGetField(FieldMap group, UtcTimeStampField field) {
        if (Objects.nonNull(group) && group.isSetField(field.getField())) {
            try {
                group.getField(field);
                return Optional.of(field.getValue())
                        .map(localDateTime -> localDateTime.toInstant(ZoneOffset.UTC));
            } catch (FieldNotFound e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<LocalDate> safeGetField(FieldMap group, UtcDateOnlyField field) {
        if (Objects.nonNull(group) && group.isSetField(field.getField())) {
            try {
                group.getField(field);
                return Optional.of(field.getValue());
                //                    .map(localDate -> localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
            } catch (FieldNotFound e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<LocalTime> safeGetField(FieldMap group, UtcTimeOnlyField field) {
        if (Objects.nonNull(group) && group.isSetField(field.getField())) {
            try {
                group.getField(field);
                return Optional.of(field.getValue());
                //                    .map(localTime -> localTime.atDate(LocalDate.now()).toInstant(ZoneOffset.UTC));
            } catch (FieldNotFound e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<Integer> safeGetField(FieldMap group, IntField field) {
        if (Objects.nonNull(group) && group.isSetField(field.getField())) {
            try {
                group.getField(field);
                return Optional.of(field.getValue());
            } catch (FieldNotFound e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<Double> safeGetField(FieldMap group, DoubleField field) {
        if (Objects.nonNull(group) && group.isSetField(field.getField())) {
            try {
                group.getField(field);
                return Optional.of(field.getValue());
            } catch (FieldNotFound e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<Object> safeGetField(FieldMap group, FieldType fieldType, int tag) {
        Class<?> javaType = fieldType.getJavaType();

        if (javaType.equals(Double.class)) {
            return safeGetField(group, new DoubleField(tag)).map(d -> d);
        } else if (javaType.equals(Integer.class)) {
            return safeGetField(group, new IntField(tag)).map(d -> d);
        } else if (javaType.equals(Date.class)) {
            switch (fieldType) {
                case UTCTIMESTAMP:
                    return safeGetField(group, new UtcTimeStampField(tag)).map(d -> d);
                case UTCDATEONLY:
                    return safeGetField(group, new UtcDateOnlyField(tag)).map(d -> d);
                case UTCTIMEONLY:
                    return safeGetField(group, new UtcTimeOnlyField(tag)).map(d -> d);
                default:
                    throw new IllegalArgumentException("Cannot of field from type: " + fieldType);
            }
        } else {
            return safeGetField(group, new StringField(tag)).map(d -> d);
        }
    }

    public static void setFieldInGroup(Group group, FieldType fieldType, int tag, @Nullable Object value) {
        Class<?> javaType = fieldType.getJavaType();

        if (javaType.equals(Double.class)) {
            if (value instanceof Number) {
                group.setField(new DoubleField(tag, ((Number) value).doubleValue()));
            } else {
                String v = Objects.nonNull(value) ? value.toString() : "";
                group.setField(new DoubleField(tag, Double.parseDouble(v)));
            }
        } else if (javaType.equals(Integer.class)) {
            if (value instanceof Number) {
                group.setField(new IntField(tag, ((Number) value).intValue()));
            } else {
                String v = Objects.nonNull(value) ? value.toString() : "";
                group.setField(new DoubleField(tag, Integer.parseInt(v)));
            }
        } else if (javaType.equals(Date.class)) {
            switch (fieldType) {
                case UTCTIMESTAMP:
                    group.setField(utcTimeStampField(tag, value));
                case UTCDATEONLY:
                    group.setField(utcDateOnlyField(tag, value));
                case UTCTIMEONLY:
                    group.setField(utcTimeOnlyField(tag, value));
                default:
                    throw new IllegalArgumentException("Cannot of field from type: " + fieldType);
            }
        } else {
            String v = Objects.nonNull(value) ? value.toString() : "";
            group.setField(new StringField(tag, v));
        }
    }

    public static UtcTimeStampField utcTimeStampField(int tag, Object value) {
        UtcTimeStampField utcDateOnlyField = new UtcTimeStampField(tag);
        if (value instanceof LocalDateTime) {
            utcDateOnlyField.setValue((LocalDateTime) value);
        } else if (value instanceof Instant) {
            utcDateOnlyField.setValue(LocalDateTime.ofInstant((Instant) value, ZoneOffset.UTC));
        } else {
            String v = Objects.nonNull(value) ? value.toString() : "";
            Instant dateTime = Instant.parse(v);
            utcDateOnlyField.setValue(LocalDateTime.ofInstant(dateTime, ZoneOffset.UTC));
        }
        return utcDateOnlyField;
    }

    public static UtcDateOnlyField utcDateOnlyField(int tag, Object value) {
        UtcDateOnlyField utcDateOnlyField = new UtcDateOnlyField(tag);
        if (value instanceof LocalDate) {
            utcDateOnlyField.setValue((LocalDate) value);
        } else if (value instanceof LocalDateTime) {
            utcDateOnlyField.setValue(((LocalDateTime) value).toLocalDate());
        } else if (value instanceof Instant) {
            utcDateOnlyField.setValue(LocalDateTime.ofInstant((Instant) value, ZoneOffset.UTC).toLocalDate());
        } else {
            String v = Objects.nonNull(value) ? value.toString() : "";
            LocalDate date = LocalDate.parse(v);
            utcDateOnlyField.setValue(date);
        }
        return utcDateOnlyField;
    }

    public static UtcTimeOnlyField utcTimeOnlyField(int tag, Object value) {
        UtcTimeOnlyField utcDateOnlyField = new UtcTimeOnlyField(tag);
        if (value instanceof LocalTime) {
            utcDateOnlyField.setValue((LocalTime) value);
        } else if (value instanceof LocalDateTime) {
            utcDateOnlyField.setValue(((LocalDateTime) value).toLocalTime());
        } else if (value instanceof Instant) {
            utcDateOnlyField.setValue(LocalDateTime.ofInstant((Instant) value, ZoneOffset.UTC).toLocalTime());
        } else {
            String v = Objects.nonNull(value) ? value.toString() : "";
            LocalTime time = LocalTime.parse(v);
            utcDateOnlyField.setValue(time);
        }
        return utcDateOnlyField;
    }

    public static Group createGroup(int groupTag, Field<?>... fields) {
        int fieldTag = Arrays.stream(fields).mapToInt(Field::getTag).findFirst()
                .orElseThrow(() -> new IllegalStateException("Field without any tag"));
        boolean allMatch = Arrays.stream(fields).mapToInt(Field::getTag).allMatch(tag -> tag == fieldTag);
        if (!allMatch) {
            throw new IllegalStateException("Trying to of a group from fields with different tags: " +
                    Arrays.toString(fields));
        }
        Group retGroup = new Group(groupTag, fieldTag);
        Arrays.stream(fields)
                .map(field -> {
                    Group group = new Group(groupTag, fieldTag);
                    setField(group, field);
                    return group;
                })
                .forEach(retGroup::addGroup);
        return retGroup;
    }

    public static void copy(FieldMap to, FieldMap from) {
        stream(from.iterator())
                .forEach(field -> setField(to, field));
        stream(from.groupKeyIterator())
                .flatMap(i -> from.getGroups(i).stream())
                .forEach(to::addGroup);
    }

    public static void setField(FieldMap to, Field<?> field) {
        if (field instanceof DoubleField) {
            to.setField((DoubleField) field);
        } else if (field instanceof IntField) {
            to.setField((IntField) field);
        } else if (field instanceof CharField) {
            to.setField((CharField) field);
        } else if (field instanceof BooleanField) {
            to.setField((BooleanField) field);
        } else if (field instanceof UtcTimeOnlyField) {
            to.setField((UtcTimeOnlyField) field);
        } else if (field instanceof UtcTimeStampField) {
            to.setField((UtcTimeStampField) field);
        } else if (field instanceof UtcDateOnlyField) {
            to.setField((UtcDateOnlyField) field);
        } else if (field instanceof StringField) {
            to.setField((StringField) field);
        } else {
            throw new IllegalStateException("Field type not recognised");
        }
    }

    public static LocalDate parseFixDate(String dateString) {
        return LocalDate.parse(dateString, FIX_DATE_FORMAT);
    }

    public static String toFix(LocalDate date) {
        return date.format(FIX_DATE_FORMAT);
    }

    public static String toFix(LocalTime time) {
        return time.format(FIX_TIME_FORMAT);
    }

    public static String toFix(LocalDateTime dateTime) {
        return dateTime.format(FIX_DATE_N_TIME_FORMAT);
    }
}
