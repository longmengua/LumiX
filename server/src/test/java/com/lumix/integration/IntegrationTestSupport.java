package com.lumix.integration;

import com.lumix.LumiXServerApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * integration test 的共用基底。
 *
 * 這個基底只負責載入 Spring context 與套用 test profile，不承擔任何業務行為。
 */
@SpringBootTest(classes = LumiXServerApplication.class)
@ActiveProfiles("integration-test")
public abstract class IntegrationTestSupport {
}
