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

package ch.voulgarakis.spring.boot.starter.quickfixj.flux.autoconfigure;

import ch.voulgarakis.spring.boot.starter.quickfixj.flux.ReactiveFixSessions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * The only reason we need this bean is to force the creation of the ReactiveFixSessions bean.
 * This in turn will register dynamically the ReactiveFixSession beans, according to the sessionSettings.
 * <p>
 * See: <a href="https://github.com/gevoulga/spring-boot-quickfixj/issues/6">github issue</a>
 */
public class ForceResolutionOfReactiveFixSessionsFirst implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        //This is enough to force the creation of ReactiveFixSessions bean
        //It will in turn dynamically create the ReactiveFixSession beans, in line with what is defined in the session settings.
        beanFactory.getBean(ReactiveFixSessions.class);
    }
}
