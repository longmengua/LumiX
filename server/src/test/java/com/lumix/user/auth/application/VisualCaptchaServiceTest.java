package com.lumix.user.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class VisualCaptchaServiceTest {

    @Test
    @SuppressWarnings("unchecked") // Mockito 無法在 type erasure 後保留 RedisTemplate 的泛型資訊。
    void iconMatchIsSelectedByServerConfigurationAndStoresOnlyTheIndexAnswer() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        UserAuthenticationProperties properties = propertiesFor("ICON_MATCH");
        VisualCaptchaService service = new VisualCaptchaService(redisTemplate, mock(SliderCaptchaService.class), properties);

        VisualCaptchaService.CaptchaChallengeResponse response = service.create(metadata());

        // 題面需要六個候選圖，但 response 不可帶出哪一個才是 server 保存的答案。
        assertEquals(VisualCaptchaService.CaptchaType.ICON_MATCH, response.type());
        assertNotNull(response.referenceImage());
        assertEquals(6, response.candidateImages().size());
        verify(values).set(startsWith("lumix:auth:visual-captcha:challenge:"), startsWith("ICON_MATCH|"), any(Duration.class));
    }

    @Test
    @SuppressWarnings("unchecked") // Mockito 無法在 type erasure 後保留 RedisTemplate 的泛型資訊。
    void imageGridCreatesNineTilesAndRequiresServerSideSelectionSet() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        VisualCaptchaService service = new VisualCaptchaService(redisTemplate, mock(SliderCaptchaService.class), propertiesFor("IMAGE_GRID"));

        VisualCaptchaService.CaptchaChallengeResponse response = service.create(metadata());

        // 九宮格只回傳圖片與提示；正確 index 集合留在 Redis，不能由 DOM 或 API body 讀取。
        assertEquals(VisualCaptchaService.CaptchaType.IMAGE_GRID, response.type());
        assertEquals(9, response.gridImages().size());
        assertFalse(response.gridImages().stream().anyMatch(String::isBlank));
        verify(values).set(startsWith("lumix:auth:visual-captcha:challenge:"), startsWith("IMAGE_GRID|"), any(Duration.class));
    }

    private static UserAuthenticationProperties propertiesFor(String type) {
        UserAuthenticationProperties properties = new UserAuthenticationProperties();
        properties.getCaptcha().setEnabledTypes(List.of(type));
        properties.getCaptcha().setSelectionMode("RANDOM");
        return properties;
    }

    private static LoginRequestMetadata metadata() {
        return new LoginRequestMetadata("203.0.113.27", "test browser", "browser-digest");
    }
}
