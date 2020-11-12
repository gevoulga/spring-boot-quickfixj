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

import ch.voulgarakis.spring.boot.starter.quickfixj.EmptyContext;
import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.authentication.AuthenticationService;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.RejectException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionDroppedException;
import ch.voulgarakis.spring.boot.starter.quickfixj.fix.session.FixSession;
import ch.voulgarakis.spring.boot.starter.quickfixj.fix.session.FixSessionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.fix43.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EmptyContext.class, FixSessionManagerTest.FixSessionTestContext.class})
@TestPropertySource("classpath:fixSessionTest.properties")
@DirtiesContext //Stop port already bound issues from other tests
public class FixSessionManagerTest {

    @Autowired
    private FixSessionManager sessionManager;
    @Autowired
    private AbstractFixSession fixSession;
    @Autowired
    private AuthenticationService authenticationService;

    @Test
    public void test() throws RejectLogon {
        SessionID sessionId = new SessionID("FIX.4.3", "TEST_CLIENT", "FIX");
        //At context initialization, we should set the sessionId first, and then get it
        verify(fixSession).getSessionId();

        //Logon
        sessionManager.fromAdmin(new Logon(), sessionId);
        verify(authenticationService).authenticate(any(SessionID.class), any(Message.class));
        verify(fixSession).loggedOn();

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

        //Logon (again)
        sessionManager.fromAdmin(new Logon(), sessionId);
        verify(authenticationService, times(2)).authenticate(any(SessionID.class), any(Message.class));
        verify(fixSession, times(2)).loggedOn();

    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EnableQuickFixJ
    static class FixSessionTestContext {
        @Bean
        public FixSession fixSession() {
            return mock(FixSessionImpl.class);
        }

        @Bean
        public AuthenticationService authenticationService() {
            return mock(AuthenticationService.class);
        }
    }

}