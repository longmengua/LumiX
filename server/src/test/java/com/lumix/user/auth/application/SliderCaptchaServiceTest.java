package com.lumix.user.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lumix.user.auth.domain.LoginRequestMetadata;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SliderCaptchaServiceTest {

    @Test
    @SuppressWarnings("unchecked") // Mockito 無法在 type erasure 後保留 RedisTemplate 的泛型資訊。
    void challengeUsesTightIrregularPieceWithoutExposingAnswer() throws Exception {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        SliderCaptchaService service = new SliderCaptchaService(redisTemplate);

        SliderCaptchaService.SliderChallengeResponse response = service.create(
            new LoginRequestMetadata("203.0.113.12", "Test browser", "browser-digest")
        );

        BufferedImage piece = decodePng(response.pieceImage());
        // 拼圖角落維持透明且中央保有影像內容，避免退回帶大片白框的矩形切片。
        assertEquals(0, piece.getRGB(0, 0) >>> 24);
        assertTrue((piece.getRGB(22, 22) >>> 24) > 0);
        assertEquals(response.pieceWidth(), piece.getWidth());
        assertEquals(response.pieceHeight(), piece.getHeight());
        ArgumentCaptor<String> storedChallenge = ArgumentCaptor.forClass(String.class);
        verify(values).set(
            startsWith("lumix:auth:slider-captcha:challenge:"), storedChallenge.capture(), any(Duration.class)
        );
        // target X 僅能在 Redis 的 server-side challenge 內出現，回傳契約沒有答案欄位。
        assertTrue(storedChallenge.getValue().endsWith("|browser-digest"));
    }

    private static BufferedImage decodePng(String dataUrl) throws Exception {
        String encodedImage = dataUrl.substring("data:image/png;base64,".length());
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedImage)));
    }
}
