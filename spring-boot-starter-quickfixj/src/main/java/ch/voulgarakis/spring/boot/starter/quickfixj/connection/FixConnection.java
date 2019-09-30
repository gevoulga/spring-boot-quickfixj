package ch.voulgarakis.spring.boot.starter.quickfixj.connection;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import quickfix.ConfigError;
import quickfix.Connector;
import quickfix.RuntimeError;

public class FixConnection implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(FixConnection.class);

    private final Connector connector;
    private boolean running = false;

    public FixConnection(Connector connector) {
        this.connector = connector;
    }

    @Override
    public synchronized void start() {
        if (!isRunning()) {
            LOG.info("Starting FixConnection");
            try {
                connector.start();
            } catch (ConfigError | RuntimeError ex) {
                throw new QuickFixJConfigurationException(ex.getMessage(), ex);
            } catch (Throwable ex) {
                throw new IllegalStateException("Could not start the connector", ex);
            }
            running = true;
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