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

import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.AbstractFixSession;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.Message;

@Configuration
@EnableAutoConfiguration
@EnableQuickFixJ
public class QuickFixJAutoConfigurationTestConfig {

    @Bean
    public AbstractFixSession fixSession() {
        return new AbstractFixSession() {
            @Override
            protected void received(Message message) {
            }

            @Override
            protected void error(SessionException message) {
            }

            @Override
            protected void authenticate(Message message) {
            }
        };
    }
}
