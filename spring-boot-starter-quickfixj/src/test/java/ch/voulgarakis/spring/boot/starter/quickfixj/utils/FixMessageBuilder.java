package ch.voulgarakis.spring.boot.starter.quickfixj.utils;

import quickfix.*;

public class FixMessageBuilder<T extends FieldMap> {

    protected final T message;

    public FixMessageBuilder(T message) {
        this.message = message;
    }

    public FixMessageBuilder<T> add(DoubleField field) {
        message.setField(field);
        return this;
    }

    public FixMessageBuilder<T> add(StringField field) {
        message.setField(field);
        return this;
    }

    public FixMessageBuilder<T> add(IntField field) {
        message.setField(field);
        return this;
    }

    public FixMessageBuilder<T> add(CharField field) {
        message.setField(field);
        return this;
    }

    public FixMessageBuilder<T> add(UtcDateOnlyField field) {
        message.setField(field);
        return this;
    }

    public FixMessageBuilder<T> add(UtcTimeOnlyField field) {
        message.setField(field);
        return this;
    }

    public FixMessageBuilder<T> add(UtcTimeStampField field) {
        message.setField(field);
        return this;
    }

    public FixMessageBuilder<T> addGroup(Group group) {
        message.addGroup(group);
        return this;
    }

    public T build() {
        return message;
    }
}
