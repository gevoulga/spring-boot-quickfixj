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

package ch.voulgarakis.spring.boot.starter.quickfixj.flux;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ReactiveFixSessionsTestContext.class)
//@DirtiesContext
class ReactiveFixSessionsTest {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    @Qualifier("TEST2")
    private ReactiveFixSession fixSession;

    @Test
    void testRuntimeBeanCreation() {
        //First session defined
        ReactiveFixSession testClient =
                applicationContext.getBean("FIX.4.3:TEST_CLIENT->FIX", ReactiveFixSession.class);
        assertNotNull(testClient);

        //Second session defined
        ReactiveFixSession testClient2 =
                applicationContext.getBean("TEST2", ReactiveFixSession.class);
        assertNotNull(testClient2);
        assertEquals(fixSession, testClient2);

        String[] beanNames = applicationContext.getBeanNamesForType(ReactiveFixSession.class);
        assertThat(beanNames)
                .containsExactlyInAnyOrder("FIX.4.3:TEST_CLIENT->FIX", "TEST2", "TEST3");
    }
}