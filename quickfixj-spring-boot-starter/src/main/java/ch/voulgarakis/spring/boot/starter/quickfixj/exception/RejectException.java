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

import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils;
import quickfix.Message;
import quickfix.field.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.safeGetField;
import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StaticExtractor.toText;
import static java.lang.String.format;

public class RejectException extends SessionException {

    private static final long serialVersionUID = -4858291950466675312L;

    public RejectException(Message fixMessage) {
        this(fixMessage, extractText(fixMessage));
    }

    public RejectException(Message fixMessage, String errorMessage) {
        super(fixMessage, errorMessage);
    }

    private static String extractText(Message message) {
        return Stream.of(
                //MessageType
                safeGetField(message.getHeader(), new MsgType())
                        .map(s -> toText(new MsgType(), s))
                        .map(s -> format("RejectType: %s", s)),

                //Text
                safeGetField(message, new Text())
                        .map(s -> format("Text: %s", s)),
                //RefId
                safeGetField(message, new RefTagID())
                        .map(s -> format("RefTagID: %s", s)),
                safeGetField(message, new QuoteReqID())
                        .map(s -> format("QuoteReqID: %s", s)),
                safeGetField(message, new BusinessRejectRefID())
                        .map(s -> format("BusinessRejectRefID: %s", s)),

                //Symbol
                safeGetField(message, new Symbol())
                        .map(s -> format("Symbol: %s", s)),
                //QuoteType
                safeGetField(message, new QuoteType())
                        .map(s -> toText(new QuoteType(), s))
                        .map(s -> format("QuoteType: %s", s)),
                //RefMsgType
                safeGetField(message, new RefMsgType())
                        .map(s -> toText(new MsgType(), s))
                        .map(s -> format("RefMsgType: %s", s)),

                //RejectReason
                safeGetField(message, new SessionRejectReason())
                        .map(s -> toText(new SessionRejectReason(), s))
                        .map(s -> format("SessionRejectReason: %s", s)),
                safeGetField(message, new BusinessRejectReason())
                        .map(s -> toText(new BusinessRejectReason(), s))
                        .map(s -> format("BusinessRejectReason: %s", s)),
                safeGetField(message, new QuoteRequestRejectReason())
                        .map(s -> toText(new QuoteRequestRejectReason(), s))
                        .map(s -> format("QuoteRequestRejectReason: %s", s))
        )
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.joining(", "));
    }

    public static boolean isReject(Message message) {
        return FixMessageUtils.isMessageOfType(message,
                MsgType.REJECT,
                MsgType.BUSINESS_MESSAGE_REJECT,
                MsgType.ORDER_CANCEL_REJECT,
                MsgType.MARKET_DATA_REQUEST_REJECT,
                MsgType.QUOTE_REQUEST_REJECT);
    }
}
