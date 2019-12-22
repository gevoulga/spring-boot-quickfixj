package ch.voulgarakis.spring.boot.starter.quickfixj.connection;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixConnectionType;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StartupLatch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import quickfix.ConfigError;
import quickfix.Connector;

import java.time.Duration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FixConnectionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void startAndStop() throws Exception {
        Connector connector = mock(Connector.class);
        StartupLatch startupLatch = new StartupLatch(1, FixConnectionType.ACCEPTOR, Duration.ofSeconds(1));
        FixConnection connectorManager = new FixConnection(connector, startupLatch);

        // When
        connectorManager.start();
        startupLatch.created(null);
        assertTrue(connectorManager.isRunning());

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

        thrown.expect(QuickFixJConfigurationException.class);
        connectorManager.start();
        assertFalse(connectorManager.isRunning());

        verify(connector).start();
    }
}