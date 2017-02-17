package com.focusit.server.specs.integration

import com.focusit.jsflight.server.ServerApplication
import com.focusit.server.specs.BaseSpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.web.client.RestTemplate
/**
 * Created by doki on 11.05.16.
 */

@EnableAutoConfiguration(exclude = [EmbeddedMongoAutoConfiguration.class])
@ContextConfiguration(loader = SpringApplicationContextLoader.class, classes = [ServerApplication.class])
@WebIntegrationTest
@TestPropertySource("classpath:app.integration.test.properties")
class AppContextSpec extends BaseSpec {
    @Value('${local.server.port}')
    int port

    def "startsContext successfully"() {
        when:
        ResponseEntity entity = new RestTemplate().getForEntity("http://localhost:${port}/player/list", String.class);
        then:
        entity.statusCode == HttpStatus.OK
    }

    def "can import record"() {

    }
}
