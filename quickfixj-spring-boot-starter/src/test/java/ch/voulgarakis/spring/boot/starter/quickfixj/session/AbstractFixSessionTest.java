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

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionDroppedException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import org.junit.jupiter.api.Test;
import quickfix.Message;
import quickfix.SessionID;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AbstractFixSessionTest {

    @Test
    void testLogonLogout() {
        AbstractFixSession session = new AbstractFixSession();

        /////////////////
        // Add the first sink, should be OK
        ////////////////
        Consumer<Message> onMessage1 = mock(Consumer.class);
        Consumer<Throwable> onError1 = mock(Consumer.class);
        session.createSink(message -> true, onMessage1, onError1);
        assertEquals(1,session.sinkSize());
        //Callbacks should not be invoked
        verifyNoInteractions(onMessage1, onError1);

        /////////////////
        // Push a mock fix message
        ////////////////
        session.received(null);
        //OnMessage should be invoked
        verify(onMessage1).accept(null);
        verifyNoMoreInteractions(onMessage1, onError1);

        /////////////////
        // Set the session as logged out
        ////////////////
        SessionDroppedException dropped = new SessionDroppedException();
        session.error(dropped);
        //session 1 should have been kicked off
        assertEquals(0,session.sinkSize());
        //OnError should be invoked
        verify(onError1).accept(dropped);
        verifyNoMoreInteractions(onMessage1, onError1);

        /////////////////
        // Trying to register another sink now should invoke onError
        /////////////////
        Consumer<Message> onMessage2 = mock(Consumer.class);
        Consumer<Throwable> onError2 = mock(Consumer.class);
        session.createSink(message -> true, onMessage2, onError2);
        assertEquals(0,session.sinkSize());
        verify(onError2).accept(dropped);
        verifyNoMoreInteractions(onMessage2, onError2);


        /////////////////
        //Set the session back as logged on
        /////////////////
        session.loggedOn();
        assertEquals(0,session.sinkSize());

        /////////////////
        //Registering another sink now should be OK
        /////////////////
        Consumer<Message> onMessage3 = mock(Consumer.class);
        Consumer<Throwable> onError3 = mock(Consumer.class);
        session.createSink(message -> true, onMessage3, onError3);
        assertEquals(1,session.sinkSize());

        /////////////////
        // Push another mock fix message
        ////////////////
        session.received(null);
        //OnMessage should be invoked
        verify(onMessage3).accept(null);
        verifyNoMoreInteractions(onMessage3, onError3);

    }

    @Test
    void received() {
        AbstractFixSession session = new AbstractFixSession();
        Consumer<Message> onMessage = mock(Consumer.class);
        Consumer<Throwable> onError = mock(Consumer.class);

        //Add a sink
        session.createSink(message -> true, onMessage, onError);
        assertEquals(1,session.sinkSize());

        //Push a mock fix message
        session.received(null);

        assertEquals(1,session.sinkSize());
        //OnMessage should be invoked
        verify(onMessage).accept(null);
        verifyNoMoreInteractions(onMessage, onError);
    }

    @Test
    void error() {
        AbstractFixSession session = new AbstractFixSession();
        Consumer<Message> onMessage = mock(Consumer.class);
        Consumer<Throwable> onError = mock(Consumer.class);

        //Add a sink
        session.createSink(message -> true, onMessage, onError);

        //Push a mock error
        SessionException ex = mock(SessionException.class);
        session.error(ex);

        //Sink should be disposed (by on error)
        assertEquals(0,session.sinkSize());
        //OnMessage should be invoked
        verify(onError).accept(ex);
        verifyNoMoreInteractions(onMessage, onError);
    }

    @Test
    void dispose() {
        AbstractFixSession session = new AbstractFixSession();
        Consumer<Message> onMessage = mock(Consumer.class);
        Consumer<Throwable> onError = mock(Consumer.class);

        //Add a sink
        MessageSink sink = session.createSink(message -> true, onMessage, onError);
        assertEquals(1,session.sinkSize());

        //Dispose the sink
        sink.dispose();

        //Sink should be disposed
        assertEquals(0,session.sinkSize());
        //No callbacks should have happened
        verifyNoMoreInteractions(onMessage, onError);
    }

    @Test
    void getSessionId() {
        AbstractFixSession session = new AbstractFixSession();

        //SessionId not already set, should throw exception
        assertThrows(QuickFixJConfigurationException.class, session::getSessionId);

        SessionID sessionID = new SessionID("FIX.4.3", "TEST_CLIENT", "FIX");
        session.setSessionId(sessionID);

        //Now that the sessionID is set, getSessionId should be OK
        assertEquals(sessionID, session.getSessionId());
    }
}