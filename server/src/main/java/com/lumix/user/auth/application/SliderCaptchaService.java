package com.lumix.user.auth.application;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.user.auth.domain.LoginRequestMetadata;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 圖形滑動驗證的 server-side 安全邊界。
 *
 * <p>挑戰答案與成功通行 token 都只存 Redis 並以原子 get-and-delete 消耗。這讓單點與 cluster topology
 * 共用同一段程式，且無論 challenge 或 token 被重放，都不可能獲得第二次成功結果。</p>
 */
@Service
@Profile("infrastructure")
public class SliderCaptchaService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHALLENGE_KEY_PREFIX = "lumix:auth:slider-captcha:challenge:";
    private static final String PASS_KEY_PREFIX = "lumix:auth:slider-captcha:pass:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(2);
    private static final Duration PASS_TTL = Duration.ofMinutes(5);
    private static final int IMAGE_WIDTH = 280;
    private static final int IMAGE_HEIGHT = 144;
    private static final int PIECE_WIDTH = 44;
    private static final int PIECE_HEIGHT = 44;
    private static final int DECOY_GAP_COUNT = 2;
    private static final int ACCEPTED_OFFSET_DEVIATION = 6;

    private final RedisTemplate<String, String> redisTemplate;

    public SliderCaptchaService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 建立一個僅能由原 browser fingerprint 解答的短時效滑動題。 */
    public SliderChallengeResponse create(LoginRequestMetadata metadata) {
        String captchaId = UUID.randomUUID().toString();
        int targetX = randomInt(PIECE_WIDTH + 12, IMAGE_WIDTH - PIECE_WIDTH - 12);
        int targetY = randomInt(10, IMAGE_HEIGHT - PIECE_HEIGHT - 10);
        SliderImages images = renderImages(targetX, targetY);

        put(CHALLENGE_KEY_PREFIX + captchaId, targetX + "|" + metadata.userAgentDigest(), CHALLENGE_TTL);
        return new SliderChallengeResponse(
            captchaId, images.background(), images.piece(), targetY, IMAGE_WIDTH, IMAGE_HEIGHT, PIECE_WIDTH, PIECE_HEIGHT
        );
    }

    /**
     * 驗證滑動結果並核發用途限定通行 token。
     *
     * <p>先消耗 challenge 才比對答案；即使答錯也不能重複嘗試同一張圖。成功 token 同樣只可消耗一次，
     * 也會綁定當前瀏覽器摘要，避免它被其他 browser 拿去提交帳號表單。</p>
     */
    public CaptchaVerificationResponse verify(String captchaId, int offsetX, CaptchaPurpose purpose, LoginRequestMetadata metadata) {
        if (!isBounded(captchaId, 64) || purpose == null || offsetX < 0 || offsetX > IMAGE_WIDTH - PIECE_WIDTH) {
            throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
        }

        String challenge = getAndDelete(CHALLENGE_KEY_PREFIX + captchaId);
        if (challenge == null) {
            throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
        }
        String[] values = challenge.split("\\|", -1);
        if (values.length != 2 || !metadata.userAgentDigest().equals(values[1])) {
            throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
        }

        int targetX;
        try {
            targetX = Integer.parseInt(values[0]);
        } catch (NumberFormatException exception) {
            throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
        }
        if (Math.abs(targetX - offsetX) > ACCEPTED_OFFSET_DEVIATION) {
            throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
        }

        return issuePassToken(purpose, metadata);
    }

    /** 其他 server-side 題型驗證答案後，只能經由同一條用途限定 token 邊界核發通行證。 */
    public CaptchaVerificationResponse issuePassToken(CaptchaPurpose purpose, LoginRequestMetadata metadata) {
        String captchaToken = randomOpaqueSecret();
        put(PASS_KEY_PREFIX + captchaToken, purpose.name() + "|" + metadata.userAgentDigest(), PASS_TTL);
        return new CaptchaVerificationResponse(captchaToken);
    }

    /** 在真正帳號動作前一次性消耗 captcha token；Redis 故障採 fail-closed，不能靜默跳過防護。 */
    public void consume(String captchaToken, CaptchaPurpose purpose, LoginRequestMetadata metadata) {
        if (!isBounded(captchaToken, 128)) {
            throw new ApiException(ApiErrorCode.CAPTCHA_REQUIRED);
        }
        String pass = getAndDelete(PASS_KEY_PREFIX + captchaToken);
        if (pass == null) {
            throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
        }
        String expected = purpose.name() + "|" + metadata.userAgentDigest();
        if (!expected.equals(pass)) {
            throw new ApiException(ApiErrorCode.CAPTCHA_INVALID);
        }
    }

    private void put(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (RedisConnectionFailureException exception) {
            // 驗證碼是防濫用控制；Redis 不可用時不可退化成未驗證也可登入或寄信。
            throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private String getAndDelete(String key) {
        try {
            return redisTemplate.opsForValue().getAndDelete(key);
        } catch (RedisConnectionFailureException exception) {
            throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private SliderImages renderImages(int targetX, int targetY) {
        BufferedImage background = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D backgroundGraphics = background.createGraphics();
        configureGraphics(backgroundGraphics);
        drawBackground(backgroundGraphics);

        Path2D pieceShape = puzzlePieceShape(0, 0);
        BufferedImage piece = new BufferedImage(PIECE_WIDTH, PIECE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pieceGraphics = piece.createGraphics();
        configureGraphics(pieceGraphics);
        pieceGraphics.setClip(pieceShape);
        pieceGraphics.drawImage(background, -targetX, -targetY, null);
        pieceGraphics.dispose();

        // 白色描邊會直接暴露矩形邊界；改以貼齊圖塊的不規則輪廓，並加入同形狀干擾缺口。
        drawMaskedGap(backgroundGraphics, puzzlePieceShape(targetX, targetY));
        for (int index = 0; index < DECOY_GAP_COUNT; index++) {
            int[] decoyPosition = nextDecoyPosition(targetX, targetY);
            drawMaskedGap(backgroundGraphics, puzzlePieceShape(decoyPosition[0], decoyPosition[1]));
        }
        backgroundGraphics.dispose();

        return new SliderImages(toDataUrl(background), toDataUrl(piece));
    }

    private static void configureGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static void drawBackground(Graphics2D graphics) {
        Color first = new Color(randomChannel(20, 70), randomChannel(70, 130), randomChannel(145, 220));
        Color second = new Color(randomChannel(70, 145), randomChannel(45, 95), randomChannel(130, 210));
        graphics.setPaint(new GradientPaint(0, 0, first, IMAGE_WIDTH, IMAGE_HEIGHT, second));
        graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        graphics.setComposite(AlphaComposite.SrcOver.derive(0.24f));
        for (int index = 0; index < 18; index++) {
            graphics.setColor(new Color(255, 255, 255));
            int size = randomInt(8, 44);
            graphics.fillOval(randomInt(-10, IMAGE_WIDTH - 5), randomInt(-10, IMAGE_HEIGHT - 5), size, size);
        }
        graphics.setComposite(AlphaComposite.SrcOver);
    }

    /**
     * 建立會向內凹折的拼圖輪廓，讓可移動圖塊沒有可被矩形白框標示的大片透明邊界。
     *
     * <p>輪廓的控制點固定在回傳的 piece 尺寸內；因此前端仍能以同一個 X offset 對齊，無須把答案或額外座標交給 browser。</p>
     */
    private static Path2D puzzlePieceShape(int x, int y) {
        double left = x + 1;
        double top = y + 1;
        double right = x + PIECE_WIDTH - 1;
        double bottom = y + PIECE_HEIGHT - 1;
        Path2D.Double shape = new Path2D.Double();
        shape.moveTo(left, top);
        shape.lineTo(x + 14, top);
        shape.curveTo(x + 14, y + 6, x + 16, y + 9, x + 20, y + 9);
        shape.curveTo(x + 24, y + 9, x + 26, y + 6, x + 26, top);
        shape.lineTo(right, top);
        shape.lineTo(right, y + 14);
        shape.curveTo(x + 38, y + 14, x + 35, y + 16, x + 35, y + 20);
        shape.curveTo(x + 35, y + 24, x + 38, y + 26, right, y + 26);
        shape.lineTo(right, bottom);
        shape.lineTo(x + 28, bottom);
        shape.curveTo(x + 28, y + 38, x + 26, y + 35, x + 22, y + 35);
        shape.curveTo(x + 18, y + 35, x + 16, y + 38, x + 16, bottom);
        shape.lineTo(left, bottom);
        shape.lineTo(left, y + 28);
        shape.curveTo(x + 7, y + 28, x + 10, y + 26, x + 10, y + 22);
        shape.curveTo(x + 10, y + 18, x + 7, y + 16, left, y + 16);
        shape.closePath();
        return shape;
    }

    private static void drawMaskedGap(Graphics2D graphics, Path2D shape) {
        graphics.setColor(new Color(2, 6, 23, 150));
        graphics.fill(shape);
    }

    private static int[] nextDecoyPosition(int targetX, int targetY) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int candidateX = randomInt(PIECE_WIDTH + 12, IMAGE_WIDTH - PIECE_WIDTH - 12);
            int candidateY = randomInt(10, IMAGE_HEIGHT - PIECE_HEIGHT - 10);
            if (Math.abs(candidateX - targetX) >= PIECE_WIDTH || Math.abs(candidateY - targetY) >= PIECE_HEIGHT) {
                return new int[] { candidateX, candidateY };
            }
        }
        // 極少數連續碰撞時仍回傳有效座標；這只影響干擾效果，絕不改變 Redis 保存的正確答案。
        return new int[] { PIECE_WIDTH + 12, 10 };
    }

    private static String toDataUrl(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IllegalStateException("PNG encoder is unavailable");
            }
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render slider captcha", exception);
        }
    }

    private static String randomOpaqueSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean isBounded(String value, int maximumLength) {
        return value != null && !value.isBlank() && value.length() <= maximumLength;
    }

    private static int randomInt(int minimum, int maximum) {
        return minimum + SECURE_RANDOM.nextInt(maximum - minimum + 1);
    }

    private static int randomChannel(int minimum, int maximum) {
        return randomInt(minimum, maximum);
    }

    /** 通行 token 的用途不可共用，避免註冊驗證被挪用到登入或忘記密碼。 */
    public enum CaptchaPurpose {
        LOGIN,
        REGISTRATION,
        PASSWORD_RESET
    }

    /** 瀏覽器只會取得拼圖資料與 id；不會取得 target X。 */
    public record SliderChallengeResponse(
        String captchaId,
        String backgroundImage,
        String pieceImage,
        int pieceY,
        int width,
        int height,
        int pieceWidth,
        int pieceHeight
    ) { }

    /** 成功後 token 僅短暫存在於 React state，送出表單後立即由 server 消耗。 */
    public record CaptchaVerificationResponse(String captchaToken) { }

    private record SliderImages(String background, String piece) { }
}
