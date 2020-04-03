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
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StartupLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import quickfix.ConfigError;
import quickfix.Connector;
import quickfix.RuntimeError;

public class FixConnection implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(FixConnection.class);

    private final Connector connector;
    private final StartupLatch startupLatch;
    private boolean running = false;

    public FixConnection(Connector connector, StartupLatch startupLatch) {
        this.connector = connector;
        this.startupLatch = startupLatch;
    }

    @Override
    public synchronized void start() {
        if (!isRunning()) {
            LOG.info("Starting FixConnection");
            try {
                connector.start();
                startupLatch.await();
                LOG.info("FixConnection started");
                running = true;
            } catch (ConfigError | RuntimeError ex) {
                throw new QuickFixJConfigurationException(ex.getMessage(), ex);
            }
        }
    }

    @Override
    public synchronized void stop() {
        if (isRunning()) {
            LOG.info("Stopping FixConnection");
            try {
                connector.stop(true);
            } finally {
                running = false;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}