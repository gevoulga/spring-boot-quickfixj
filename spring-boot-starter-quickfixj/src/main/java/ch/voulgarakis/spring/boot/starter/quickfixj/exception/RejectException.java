package ch.voulgarakis.spring.boot.starter.quickfixj.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.MsgType;
import quickfix.field.Text;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionManager.isMessageOfType;

public class RejectException extends Exception {

    private final Message fixMessage;

    public RejectException(Message fixMessage) {
        super(extractText(fixMessage));
        this.fixMessage = fixMessage;
    }

    public Message getFixMessage() {
        return fixMessage;
    }

    private static String extractText(Message message) {
        try {
            return message.getField(new Text()).getValue();
        } catch (FieldNotFound fieldNotFound) {
            return message.toString() + System.lineSeparator() + ExceptionUtils.getStackTrace(fieldNotFound);
        }
    }

    public static boolean isReject(Message message) throws FieldNotFound {
        return isMessageOfType(message, MsgType.REJECT, MsgType.BUSINESS_MESSAGE_REJECT, MsgType.ORDER_CANCEL_REJECT,
            MsgType.MARKET_DATA_REQUEST_REJECT, MsgType.QUOTE_REQUEST_REJECT);
    }
}
