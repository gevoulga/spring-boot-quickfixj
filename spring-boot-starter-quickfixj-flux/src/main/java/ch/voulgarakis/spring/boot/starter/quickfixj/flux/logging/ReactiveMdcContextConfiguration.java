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

package ch.voulgarakis.spring.boot.starter.quickfixj.flux.logging;

import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class ReactiveMdcContextConfiguration {

    public static final String LOGGING_CONTEXT = "LoggingContext";

    @PostConstruct
    public void contextOperatorHook() {
        Hooks.onEachOperator(LOGGING_CONTEXT,
                Operators.lift((scannable, coreSubscriber) -> new ReactiveMdcContext<>(coreSubscriber)));
    }

    @PreDestroy
    public void cleanupHook() {
        Hooks.resetOnEachOperator(LOGGING_CONTEXT);
    }

}
