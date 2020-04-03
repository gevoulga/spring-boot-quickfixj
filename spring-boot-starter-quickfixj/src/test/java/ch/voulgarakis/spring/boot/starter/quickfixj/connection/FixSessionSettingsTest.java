package ch.voulgarakis.spring.boot.starter.quickfixj.connection;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJSettingsNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import quickfix.SessionSettings;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.SYSTEM_VARIABLE_QUICKFIXJ_CONFIG;
import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.loadSettings;
import static org.assertj.core.api.Assertions.assertThat;

public class FixSessionSettingsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldLoadDefaultFromSystemProperty() {
        SessionSettings settings = loadSettings("classpath:quickfixj.cfg", null, null);
        assertThat(settings).isNotNull();
    }

    @Test
    public void shouldThrowSettingsNotFoundExceptionIfNoneFound() {
        thrown.expect(QuickFixJSettingsNotFoundException.class);
        System.setProperty(SYSTEM_VARIABLE_QUICKFIXJ_CONFIG, "crapI.cfg");
        loadSettings(null, null, null);
    }
}