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

package ch.voulgarakis.spring.boot.starter.test;

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
