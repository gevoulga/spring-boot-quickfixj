package ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = QuickFixJBootProperties.PROPERTY_PREFIX)
public class QuickFixJBootProperties {

    static final String PROPERTY_PREFIX = "quickfixj";

    private boolean jmxEnabled = false;
    private String config;

    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    public void setJmxEnabled(boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
    }

    String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
