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

package ch.voulgarakis.spring.boot.starter.quickfixj.connection;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixConnectionType;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StartupLatch;
import org.junit.jupiter.api.Test;
import quickfix.ConfigError;
import quickfix.Connector;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FixConnectionTest {

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    @Test
    public void startAndStop() throws Exception {
        Connector connector = mock(Connector.class);
        StartupLatch startupLatch = new StartupLatch(1, FixConnectionType.INITIATOR, Duration.ofSeconds(1));
        FixConnection connectorManager = new FixConnection(connector, startupLatch);

        //Not yet running
        assertFalse(connectorManager.isRunning());

        //Notify startup latch about fix sessio logged on
        scheduledExecutorService.schedule(() -> startupLatch.loggedOn(null), 1, TimeUnit.SECONDS);

        // Wait until started
        connectorManager.start();

        //Check running
        assertTrue(connectorManager.isRunning());

        //Stop
        connectorManager.stop();
        assertFalse(connectorManager.isRunning());

        // Then
        verify(connector).start();
        verify(connector).stop(eq(true));
    }

    @Test
    public void configError() throws Exception {

        Connector connector = mock(Connector.class);
        StartupLatch startupLatch = mock(StartupLatch.class);
        willThrow(ConfigError.class).given(connector).start();
        FixConnection connectorManager = new FixConnection(connector, startupLatch);

        assertThrows(QuickFixJConfigurationException.class, connectorManager::start);
        assertFalse(connectorManager.isRunning());

        verify(connector).start();
    }
}