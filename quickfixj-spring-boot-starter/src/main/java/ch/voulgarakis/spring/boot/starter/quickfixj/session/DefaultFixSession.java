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

package ch.voulgarakis.spring.boot.starter.quickfixj.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionNotFound;

public abstract class DefaultFixSession extends AbstractFixSession implements FixSession {

    @Override
    public Message send(Message message) {
        try {
            Session.sendToTarget(message, getSessionId());
            return message;
        } catch (SessionNotFound sessionNotFound) {
            throw new QuickFixJException(sessionNotFound);
        }
    }
}
