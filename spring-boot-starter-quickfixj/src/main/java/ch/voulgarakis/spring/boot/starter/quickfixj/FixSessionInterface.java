package ch.voulgarakis.spring.boot.starter.quickfixj;

import quickfix.Session;
import quickfix.SessionID;

public interface FixSessionInterface {
    SessionID getSessionId();

    Session getSession();
}
