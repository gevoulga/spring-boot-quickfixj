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

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJSettingsNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.core.io.Resource;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.SYSTEM_VARIABLE_QUICKFIXJ_CONFIG;
import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.findQuickfixjConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
public class FixSessionSettingsTest {

    @Test
    public void shouldLoadDefaultFromSystemProperty() {
        Resource resource = findQuickfixjConfig("classpath:quickfixj.cfg");
        assertThat(resource).isNotNull();
    }

    @Test
    public void shouldThrowSettingsNotFoundExceptionIfNoneFound() {
        System.setProperty(SYSTEM_VARIABLE_QUICKFIXJ_CONFIG, "crapI.cfg");
        assertThrows(QuickFixJSettingsNotFoundException.class, () -> {
            findQuickfixjConfig(null);
        });
        System.clearProperty(SYSTEM_VARIABLE_QUICKFIXJ_CONFIG);
    }
}