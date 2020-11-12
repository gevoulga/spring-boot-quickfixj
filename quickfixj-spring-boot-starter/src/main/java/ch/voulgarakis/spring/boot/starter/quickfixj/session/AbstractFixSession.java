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

import ch.voulgarakis.spring.boot.starter.quickfixj.FixSessionInterface;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionDroppedException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.field.MsgType;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AbstractFixSession implements FixSessionInterface {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFixSession.class);

    private final Set<MessageSink> sinks = ConcurrentHashMap.newKeySet();
    private final AtomicReference<SessionDroppedException> loggedOut = new AtomicReference<>();
    private SessionID sessionId;

    //--------------------------------------------------
    //--------------------CONSTRUCTORS------------------
    //--------------------------------------------------

    /**
     * SessionID resolved by {@link FixSessionManager}.
     */
    public AbstractFixSession() {
    }

    /**
     * @param sessionId Session Id manually assigned.
     */
    public AbstractFixSession(SessionID sessionId) {
        this.sessionId = sessionId;
    }

    //--------------------------------------------------
    //-----------------FIX MESSAGE/ERROR----------------
    //--------------------------------------------------

    /**
     * Notifies that a message has been received.
     *
     * @param message the message that has been received.
     */
    protected void received(Message message) {
        //loggedOut(null);
        notifySubscribers(message, sink -> sink.next(message));
    }

    /**
     * Notifies that a reject message has been received of type:
     * REJECT, BUSINESS_MESSAGE_REJECT, ORDER_CANCEL_REJECT, MARKET_DATA_REQUEST_REJECT, QUOTE_REQUEST_REJECT
     *
     * @param ex the actual reject exception that has been received.
     */
    protected void error(SessionException ex) {
        loggedOut(ex);
        notifySubscribers(ex.getFixMessage(), sink -> sink.error(ex));
    }

    /**
     * Notifies that the session has been logged on.
     */
    protected void loggedOn() {
        loggedOut(null);
    }

    /**
     * Store the logout (if not null) so subsequent subscriptions will be notified that the session has been dropped.
     *
     * @param ex the session exception.
     *           If of type SessionDroppedException, it will be stored.
     *           Otherwise, any existing error will be cleared.
     */
    private void loggedOut(SessionException ex) {
        if (ex instanceof SessionDroppedException) {
            SessionDroppedException droppedException = (SessionDroppedException) ex;
            //Do not override if a SessionDroppedException with a Fix Message already exists!
            if (Objects.nonNull(ex.getFixMessage())) {
                loggedOut.set(droppedException);
            } else {
                loggedOut.compareAndSet(null, droppedException);
            }
        } else {
            loggedOut.set(null);
        }
    }

    /**
     * Notifies that a fix message has been sent
     *
     * @param message the actual message that has been sent.
     */
    //    protected abstract void sent(Message message);

    //--------------------------------------------------
    //-------------------FIX SUBSCRIBE------------------
    //--------------------------------------------------

    /**
     * Notify all the registered sinks that are in scope for the received message by invoking the sinkConsumer.
     *
     * @param message      the fix message that has been received, for which we will find the sinks in scope.
     * @param sinkConsumer what to do for the sinks in scope.
     */
    private synchronized void notifySubscribers(Message message, Consumer<MessageSink> sinkConsumer) {
        //Notify all the sinks in parallel
        int notifiedSinks = sinks.parallelStream()
                .mapToInt(messageSink -> {
                    //Check if we should notify the subscriber (based on the predicate of the sink)
                    boolean notifySubscribers = messageSink.getMessageSelector().test(message);
                    if (notifySubscribers) {
                        //Notify the sink
                        sinkConsumer.accept(messageSink);
                    }
                    //Return the notified sink
                    return notifySubscribers ? 1 : 0;
                })
                .sum();

        //Log
        if (Objects.nonNull(message) && !FixMessageUtils.isMessageOfType(message, MsgType.LOGOUT)) {
            if (notifiedSinks == 0) {
                //Log a warning if nobody was notified
                LOG.warn("Message received could not be associated with any Request. Message: {}", message);
            } else if (notifiedSinks > 1) {
                //Log a warning if more than one sinks were notified
                LOG.warn("Message received was  associated with {} Requests. Suspicious subscriptions. Message: {}",
                        notifiedSinks, message);
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Notified sink for message: {}", message);
            }
        }
    }

    /**
     * Create a message sink and add it in the sink registry.
     * If the session is disconnected/dropped, the error will be immediately propagated.
     *
     * @param messageSelector associates the sink with received fix messages that are in scope.
     * @param onNext          what to do when a new fix message has been received.
     * @param onError         what to do when an error has been received.
     * @return the sink that was created and registered.
     */
    protected MessageSink createSink(Predicate<Message> messageSelector, Consumer<Message> onNext,
            Consumer<Throwable> onError) {
        //Create the sink where we push fix messages
        MessageSink sink = new MessageSink(sinks, messageSelector, onNext, onError);

        //Notify new subscriber if session has been dropped
        SessionDroppedException sessionDroppedException = loggedOut.get();
        if (Objects.nonNull(sessionDroppedException)) {
            sink.error(sessionDroppedException);
        }
        return sink;
    }

    //--------------------------------------------------
    //--------------------SESSION ID--------------------
    //--------------------------------------------------
    final void setSessionId(SessionID sessionID) {
        if (Objects.isNull(this.sessionId)) {
            this.sessionId = sessionID;
        } else if (!Objects.equals(sessionID, this.sessionId)) {
            throw new QuickFixJConfigurationException("Not allowed to set SessionId more than once.");
        }
    }

    @Override
    public SessionID getSessionId() {
        if (Objects.nonNull(sessionId)) {
            return sessionId;
        } else {
            throw new QuickFixJConfigurationException("SessionId is not set.");
        }
    }

    //--------------------------------------------------
    //------------------SESSION STATUS------------------
    //--------------------------------------------------
    @Override
    public boolean isLoggedOn() {
        Session session = Session.lookupSession(getSessionId());
        if (Objects.nonNull(session)) {
            return session.isLoggedOn();
        } else {
            throw new QuickFixJConfigurationException("Session does not exist.");
        }
    }

    protected int sinkSize() {
        return sinks.size();
    }

//    protected boolean isLoggedOut() {
//        return Objects.isNull(loggedOut.get());
//    }
}
