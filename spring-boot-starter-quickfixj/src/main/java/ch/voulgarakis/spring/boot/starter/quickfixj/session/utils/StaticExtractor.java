package ch.voulgarakis.spring.boot.starter.quickfixj.session.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class StaticExtractor {

    public static final String SERIAL_UUID_VERSION = "serialVersionUID";
    public static final String FIELD = "FIELD";
    private static final Logger LOG = LoggerFactory.getLogger(StaticExtractor.class);
    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP =
            new HashMap<Class<?>, Class<?>>() {
                private static final long serialVersionUID = -9154379364464149840L;

                {
                    put(boolean.class, Boolean.class);
                    put(byte.class, Byte.class);
                    put(char.class, Character.class);
                    put(double.class, Double.class);
                    put(float.class, Float.class);
                    put(int.class, Integer.class);
                    put(long.class, Long.class);
                    put(short.class, Short.class);
                }
            };

    public static List<Pair<String, Object>> extract(Object obj, Class<?> fieldClass) {
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        List<Pair<String, Object>> staticRegistry = new ArrayList<>();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    && isAssignableTo(field.getType(), fieldClass)) {
                String name = field.getName();
                if (!StringUtils.equalsIgnoreCase(FIELD, name)
                        && !StringUtils.equalsIgnoreCase(SERIAL_UUID_VERSION, name)) {
                    try {
                        Object value = field.get(obj);
                        staticRegistry.add(Pair.of(name, value));
                    } catch (IllegalAccessException e) {
                        LOG.error("Failed to extract static value from field {}", field, e);
                    }
                }
            }
        }
        return staticRegistry;
    }

    private static boolean isAssignableTo(Class<?> from, Class<?> to) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (from.isPrimitive()) {
            return isPrimitiveWrapperOf(to, from);
        }
        if (to.isPrimitive()) {
            return isPrimitiveWrapperOf(from, to);
        }
        return false;
    }

    private static boolean isPrimitiveWrapperOf(Class<?> targetClass, Class<?> primitive) {
        if (!primitive.isPrimitive()) {
            throw new IllegalArgumentException("First argument has to be primitive type");
        }
        return PRIMITIVE_WRAPPER_MAP.get(primitive) == targetClass;
    }

    private static <T> String toText(quickfix.Field<T> field, T value, Class<?> type) {
        List<Pair<String, Object>> extract = extract(field, type);
        List<Pair<String, Object>> collect = extract.stream()
                .filter(tuple -> Objects.equals(value, tuple.getRight()))
                .collect(Collectors.toList());
        if (collect.size() == 1) {
            return collect.get(0).getLeft();
        } else if (collect.isEmpty()) {
            throw new IllegalStateException("No values found");
        } else {
            throw new IllegalStateException("Found multiple values: " + collect);
        }
    }

    private static <T> T toValue(quickfix.Field field, String name, Class<?> type) {
        List<Pair<String, Object>> extract = extract(field, type);
        List<Pair<String, Object>> collect = extract.stream()
                .filter(tuple -> Objects.equals(name, tuple.getLeft()))
                .collect(Collectors.toList());
        if (collect.size() == 1) {
            return (T) collect.get(0).getRight();
        } else {
            throw new IllegalStateException("Found multiple values: " + collect);
        }
    }

    public static String toText(StringField field) {
        return toText(field, field.getValue(), String.class);
    }

    public static String toText(StringField field, String value) {
        return toText(field, value, String.class);
    }

    public static String toValue(StringField field, String name) {
        return toValue(field, name, String.class);
    }

    public static String toText(CharField field) {
        return toText(field, field.getValue(), Character.class);
    }

    public static String toText(CharField field, Character value) {
        return toText(field, value, Character.class);
    }

    public static Character toValue(CharField field, String name) {
        return toValue(field, name, Character.class);
    }

    public static String toText(IntField field) {
        return toText(field, field.getValue(), Integer.class);
    }

    public static String toText(IntField field, Integer value) {
        return toText(field, value, Integer.class);
    }

    public static Integer toValue(IntField field, String name) {
        return toValue(field, name, Integer.class);
    }

    public static String toText(DoubleField field) {
        return toText(field, field.getValue(), Double.class);
    }

    public static String toText(DoubleField field, Double value) {
        return toText(field, value, Double.class);
    }

    public static Double toValue(DoubleField field, String name) {
        return toValue(field, name, Double.class);
    }

    public static String toText(UtcTimeStampField field) {
        return toText(field, field.getValue(), LocalDateTime.class);
    }

    public static String toText(UtcTimeStampField field, LocalDateTime value) {
        return toText(field, value, LocalDateTime.class);
    }

    public static LocalDateTime toValue(UtcTimeStampField field, String name) {
        return toValue(field, name, LocalDateTime.class);
    }

    public static String toText(UtcDateOnlyField field) {
        return toText(field, field.getValue(), LocalDate.class);
    }

    public static String toText(UtcDateOnlyField field, LocalDate value) {
        return toText(field, value, LocalDate.class);
    }

    public static LocalDate toValue(UtcDateOnlyField field, String name) {
        return toValue(field, name, LocalDate.class);
    }

    public static String toText(UtcTimeOnlyField field) {
        return toText(field, field.getValue(), LocalTime.class);
    }

    public static String toText(UtcTimeOnlyField field, LocalTime value) {
        return toText(field, value, LocalTime.class);
    }

    public static LocalTime toValue(UtcTimeOnlyField field, String name) {
        return toValue(field, name, LocalTime.class);
    }
}
