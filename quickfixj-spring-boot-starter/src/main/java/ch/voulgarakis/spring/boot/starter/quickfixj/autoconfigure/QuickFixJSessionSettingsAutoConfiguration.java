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
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.settings.SessionSettingsEnhancer;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.settings.SessionSettingsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import quickfix.ConfigError;
import quickfix.Dictionary;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

@Configuration
@AutoConfigurationPackage
@ConditionalOnBean(annotation = EnableQuickFixJ.class)
@EnableConfigurationProperties(QuickFixJBootProperties.class)
//@ConfigurationPropertiesScan
public class QuickFixJSessionSettingsAutoConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(QuickFixJSessionSettingsAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public SessionSettingsEnhancer sessionSettingsEnhancer(Environment environment,
            ResourceLoader resourceLoader) {
        return new SessionSettingsEnhancer(environment, resourceLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionSettings sessionSettings(QuickFixJBootProperties properties,
            List<SessionSettingsSource> sessionSettingsSources,
            SessionSettingsEnhancer sessionSettingsEnhancer) throws ConfigError, IOException {
        Resource quickfixjConfig = FixSessionSettings.findQuickfixjConfig(properties.getConfig());

        //Read the quickfixj file
        try (InputStream stream = quickfixjConfig.getInputStream()) {
            LOG.info("Loading session settings from {}", quickfixjConfig);
            //Create the session settings
            SessionSettings sessionSettings = new SessionSettings(stream);

            //Resolve additional settings specified in data-sources
            for (SessionSettingsSource sessionSettingsSource : sessionSettingsSources) {
                LOG.info("Loading session settings from {}", sessionSettingsSource);
                //For all sessionIDs specified
                for (SessionID sessionID : sessionSettingsSource.findAll()) {
                    //Add them in the session settings
                    Properties props = sessionSettingsSource.findForSessionId(sessionID);
                    LOG.debug("Adding SessionID={}, properties={}", sessionID, properties);

                    //Add them in the session settings
                    sessionSettings.set(sessionID, new Dictionary(null, props));
                }
            }

            //Enhance the session settings by replacing placeholders and file references
            LOG.debug("Resolving placeholders in SessionSettings:\n{}", sessionSettings);
            return sessionSettingsEnhancer.enhanceSettingSettings(sessionSettings);
        }
    }
}
