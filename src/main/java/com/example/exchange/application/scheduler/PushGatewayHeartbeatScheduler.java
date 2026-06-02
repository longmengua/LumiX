/*
 * 檔案用途：應用層排程任務，定期向 SSE/WebSocket gateway clients 發送 heartbeat。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.PushGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PushGatewayHeartbeatScheduler {

    private final PushGatewayService pushGatewayService;

    @Value("${push-gateway.heartbeat.enabled:false}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${push-gateway.heartbeat.fixed-delay-ms:30000}")
    public void publishHeartbeat() {
        if (!enabled) return;
        pushGatewayService.publishHeartbeat(Instant.now());
    }
}
