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

package ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;


@ConfigurationProperties("quickfixj")
@ConstructorBinding //Makes our properties Immutable
public class QuickFixJBootProperties {

    /**
     * Whether to register the Jmx MBeans.
     */
    private final boolean jmxEnabled;
    /**
     * The location of the configuration file to use to initialize QuickFixJ.
     */
    private final String config;

    /**
     * The maximum time to wait (spring-context startup) until the connection is established successfully.
     * <p>
     * In case of initiator: max time to wait until connected & loggedOn to FIX session(s).
     * In case of acceptor: max time to wait until FIX session(s) is/are created.
     * <p>
     * If session has not been established, spring startup fails.
     * If null(not defined), no startup timeout is applied.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private final Duration startupTimeout;

    /**
     * Monitor the quickfixj configuration file, and pick-up/apply changes on the fly.
     */
    private final boolean configLive;

    public QuickFixJBootProperties(boolean jmxEnabled, String config, Duration startupTimeout, boolean configLive) {
        this.jmxEnabled = jmxEnabled;
        this.config = config;
        this.startupTimeout = startupTimeout;
        this.configLive = configLive;
    }

    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    public String getConfig() {
        return config;
    }

    public Duration getStartupTimeout() {
        return startupTimeout;
    }
}
