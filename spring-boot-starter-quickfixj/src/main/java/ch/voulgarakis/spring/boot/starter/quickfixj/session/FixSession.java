package ch.voulgarakis.spring.boot.starter.quickfixj.session;

import quickfix.Message;

public interface FixSession {

    Message send(Message message);
}
