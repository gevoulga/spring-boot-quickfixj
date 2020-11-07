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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.SessionID;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StartupLatch {
    private static final Logger LOG = LoggerFactory.getLogger(StartupLatch.class);

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
                logMessage();
                boolean waited = countDownLatch.await(timeout.getSeconds(), TimeUnit.SECONDS);
                if (!waited) {
                    error();
                }
            } catch (InterruptedException e) {
                throw new QuickFixJException("Interrupted when waiting to start FIX session: ", e);
            }
        }
    }

    private void logMessage() {
        if (connectionType.isAcceptor()) {
            LOG.info("Waiting for {} FIX sessions to be created. Timeout={}.",
                    countDownLatch.getCount(), timeout);
        } else {
            LOG.info("Waiting for {} FIX sessions to be connected/logged-on. Timeout={}.",
                    countDownLatch.getCount(), timeout);
        }
    }

    private void error() {
        if (connectionType.isAcceptor()) {
            throw new QuickFixJException("Failed to create FIX sessions within given timeout: " + timeout);
        } else {
            throw new QuickFixJException(
                    "Failed to connected/logged-on to FIX sessions within given timeout: " + timeout);
        }
    }

}
