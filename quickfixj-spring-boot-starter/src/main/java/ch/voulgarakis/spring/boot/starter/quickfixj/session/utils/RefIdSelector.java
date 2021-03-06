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

import org.apache.commons.lang3.StringUtils;
import quickfix.Message;
import quickfix.field.MsgSeqNum;
import quickfix.field.RefSeqNum;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.*;

public class RefIdSelector implements Predicate<Message> {

    private final Message request;

    public RefIdSelector(Message request) {
        this.request = request;
    }

    @Override
    public boolean test(Message message) {
        //The Request Id and message sequence Id
        Optional<String> reqId = safeGetIdForRequest(request);
        Optional<String> msgSeqId = safeGetField(request.getHeader(), new MsgSeqNum()).map(Object::toString);
        //The Response reference Id and the message sequence Reference Id
        List<String> refId = safeGetRefIdForResponse(message);
        Optional<String> refSeqNum = safeGetField(message, new RefSeqNum()).map(Object::toString);
        //Compare if reqId of request is same as reference Id from response
        return presentAndEquals(reqId, refId)
                //Or message sequence Id from request is the same as the reference sequence number of the response
                || presentAndEquals(msgSeqId, refSeqNum)
                //Or use a custom defined comparison between request-response
                || orElse(request, message);
    }

    private boolean presentAndEquals(Optional<String> s1, List<String> s2) {
        return s1.isPresent() && s2.stream()
                .map(Optional::ofNullable)
                .anyMatch(s -> presentAndEquals(s1, s));
    }

    private boolean presentAndEquals(Optional<String> s1, Optional<String> s2) {
        return s1.isPresent() && s2.isPresent() && isEqual(s1.get(), s2.get());
    }

    protected boolean isEqual(String reqId, String refId) {
        return StringUtils.equals(reqId, refId);
    }

    protected boolean orElse(Message request, Message message) {
        return false;
    }

    @Override
    public String toString() {
        return safeGetIdForRequest(request).orElse(super.toString());
    }
}
