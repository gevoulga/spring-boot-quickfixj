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

package ch.voulgarakis.fix.example.server;

import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@EnableQuickFixJ
public class FixServerContext {
    private static final Logger LOG = LoggerFactory.getLogger(FixServerContext.class);

    public static void main(String[] args) {
        try {
            LOG.info("FIX server initiated.");
            new SpringApplicationBuilder(FixServerContext.class)
                    .bannerMode(Banner.Mode.OFF)
                    .run(args);
            LOG.info("Context created. FIX server started.");
        } catch (Exception e) {
            LOG.error("Failed to start FIX server.", e);
            System.exit(1);
        }
    }
}
