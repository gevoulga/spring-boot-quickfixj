package ch.voulgarakis.spring.boot.starter.quickfixj.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.FixSessionInterface;
import quickfix.Message;

public interface FixSession extends FixSessionInterface {

    Message send(Message message);
}
