package ch.voulgarakis.spring.boot.starter.quickfixj.session.utils;

import org.apache.commons.lang3.StringUtils;
import quickfix.Message;
import quickfix.field.MsgSeqNum;
import quickfix.field.RefSeqNum;

import java.util.Optional;
import java.util.function.Predicate;

public class RefIdSelector implements Predicate<Message> {

    private final Message request;

    public RefIdSelector(Message request) {
        this.request = request;
    }

    private static boolean presentAndEquals(Optional<String> s1, Optional<String> s2) {
        return s1.isPresent() && s2.isPresent() && StringUtils.equals(s1.get(), s2.get());
    }

    @Override
    public boolean test(Message message) {
        //The Request Id and message sequence Id
        Optional<String> reqId = FixMessageUtils.safeGetIdForRequest(request);
        Optional<String> msgSeqId = FixMessageUtils.safeGetField(request.getHeader(), new MsgSeqNum()).map(Object::toString);
        //The Response reference Id and the message sequence Reference Id
        Optional<String> refId = FixMessageUtils.safeGetRefIdForResponse(message);
        Optional<String> refSeqNum = FixMessageUtils.safeGetField(message, new RefSeqNum()).map(Object::toString);
        //Compare if reqId of request is same as reference Id from response
        return presentAndEquals(reqId, refId)
                //Or message sequence Id from request is the same as the reference sequence number of the response
                || presentAndEquals(msgSeqId, refSeqNum);
    }

    @Override
    public String toString() {
        return FixMessageUtils.safeGetIdForRequest(request).orElse(super.toString());
    }
}
