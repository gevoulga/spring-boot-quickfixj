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

import ch.voulgarakis.spring.boot.starter.quickfixj.connection.FixConnection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import quickfix.*;

import javax.management.ObjectName;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = QuickFixJAutoConfigurationTestConfig.class,
        properties = {
                "quickfixj.config=classpath:quickfixj.cfg",
                "quickfixj.jmx-enabled=true",
                "quickfixj.startup.timeout="
        })
@DirtiesContext //Stop port already bound issues from other tests
public class QuickFixJAutoConfigurationTest {

    @Autowired
    private SessionSettings sessionSettings;
    @Autowired
    private MessageStoreFactory messageStoreFactory;
    @Autowired(required = false)
    private LogFactory logFactory;
    @Autowired
    private MessageFactory messageFactory;
    @Autowired
    private Connector connector;
    @Autowired
    private FixConnection fixConnection;
    @Autowired
    private ObjectName connectorMBean;

    @Test
    public void testAutoConfiguredBeans() {
        assertThat(sessionSettings).isNotNull();
        assertThat(messageStoreFactory).isNotNull();
        assertThat(logFactory).isNull();
        assertThat(messageFactory).isNotNull();
        assertThat(connector).isNotNull();
        assertThat(fixConnection).isNotNull();
        assertThat(connectorMBean).isNotNull();
    }
}