package ch.voulgarakis.spring.boot.starter.quickfixj.session.logging;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import quickfix.SessionID;

public class LoggingId {
    private static final String ID = "id";

    public LoggingContext loggingCtx(SessionID sessionId) {
        String id = MDC.get(ID);
        String ctx = StringUtils.isBlank(id) ? sessionId.toString() : id;
        return LoggingUtils.loggingContext(ID, id);
    }
}
