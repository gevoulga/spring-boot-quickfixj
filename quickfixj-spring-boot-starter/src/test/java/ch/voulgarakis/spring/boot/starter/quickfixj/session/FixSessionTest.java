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

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.RejectException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionDroppedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import quickfix.Message;
import quickfix.*;
import quickfix.fix43.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FixSessionTestContext.class)
@TestPropertySource("classpath:fixSessionTest.properties")
@DirtiesContext //Stop port already bound issues from other tests
public class FixSessionTest {

    @Autowired
    private FixSessionManager sessionManager;
    @Autowired
    private AbstractFixSession fixSession;

    @Test
    public void test() throws FieldNotFound, IncorrectTagValue, IncorrectDataFormat, UnsupportedMessageType, RejectLogon {
        SessionID sessionId = new SessionID("FIX.4.3", "TEST_CLIENT", "FIX");

        //Logon
        sessionManager.fromAdmin(new Logon(), sessionId);
        verify(fixSession).authenticate(any(Message.class));

        //Heartbeats
        sessionManager.fromAdmin(new Heartbeat(), sessionId);
        sessionManager.fromAdmin(new Heartbeat(), sessionId);
        verifyNoMoreInteractions(fixSession);

        //Quote (message received)
        sessionManager.fromApp(new Quote(), sessionId);
        sessionManager.fromApp(new Quote(), sessionId);
        verify(fixSession, times(2)).received(any(Message.class));

        //Rejections
        sessionManager.fromApp(new BusinessMessageReject(), sessionId);
        sessionManager.fromAdmin(new Reject(), sessionId);
        verify(fixSession, times(2)).error(any(RejectException.class));

        //Logout
        sessionManager.fromAdmin(new Logout(), sessionId);
        sessionManager.onLogout(sessionId);
        verify(fixSession, times(2)).error(any(SessionDroppedException.class));

    }

}