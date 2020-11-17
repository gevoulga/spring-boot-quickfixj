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

package ch.voulgarakis.spring.boot.starter.quickfixj.exception;

import quickfix.Message;
import quickfix.field.Text;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.safeGetField;
import static java.lang.String.format;

public class SessionDroppedException extends SessionException {

    private static final long serialVersionUID = -6938947691835025139L;

    public SessionDroppedException(Message fixMessage) {
        super(fixMessage, extractText(fixMessage));
    }

    public SessionDroppedException() {
        this(null);
    }

    private static String extractText(Message message) {
        StringBuilder response = new StringBuilder("Logged Out");
        safeGetField(message, new Text()).map(s -> format(": Text: %s", s))
                .ifPresent(response::append);
        return response.toString();
    }
}
