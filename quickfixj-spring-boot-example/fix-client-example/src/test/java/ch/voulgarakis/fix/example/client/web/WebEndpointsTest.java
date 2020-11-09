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

package ch.voulgarakis.fix.example.client.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = WebEndpointsTestContext.class)
@ActiveProfiles("web-test")
class WebEndpointsTest {

    @LocalServerPort
    private int port;

    @Test
    void testQuoteFromFixServer() {
        Flux<String> flux = WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .get()
                .uri("fix/quote")

                //The exchange
                .exchange()
                //Should return ok
                .expectStatus()
                .isOk()

                //And the body should contain the quote
                .returnResult(String.class)
                .getResponseBody();

        //Receive the quote Id
        StepVerifier
                .create(flux)
                .assertNext(Assertions::assertNotNull)
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void testClientError() {
        WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .put()
                .uri("fix/book")
                .body(Mono.just(""), String.class) //Empty quoteId -> this should return an error
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    void testInvalidEndpoint() {
        WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .get()//Invalid - GET not supported
                .uri("fix/book")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }
}