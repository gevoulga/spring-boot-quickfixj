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

package ch.voulgarakis.spring.boot.starter.quickfixj.session.utils;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixConnectionType;
import quickfix.SessionID;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StartupLatch {

    private final FixConnectionType connectionType;
    private final CountDownLatch countDownLatch;
    private final Duration timeout;

    public StartupLatch(int sessions, FixConnectionType connectionType, Duration timeout) {
        this.countDownLatch = new CountDownLatch(sessions);
        this.connectionType = connectionType;
        this.timeout = timeout;
    }

    public void created(SessionID sessionId) {
        if (connectionType.isAcceptor()) {
            countDownLatch.countDown();
        }
    }

    public void loggedOn(SessionID sessionId) {
        if (!connectionType.isAcceptor()) {
            countDownLatch.countDown();
        }
    }

    public void await() {
        if (Objects.nonNull(timeout)) {
            try {
                boolean waited = countDownLatch.await(timeout.getSeconds(), TimeUnit.SECONDS);
                if (!waited) {
                    throw new QuickFixJException("Failed to start FIX session within given timeout: " + timeout);
                }
            } catch (InterruptedException e) {
                throw new QuickFixJException("Interrupted when waiting to start FIX session: ", e);
            }
        }
    }

}
