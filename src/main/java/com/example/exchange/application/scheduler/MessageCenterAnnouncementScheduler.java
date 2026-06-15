/*
 * 檔案用途：訊息中心公告排程發送服務。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.MessageCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageCenterAnnouncementScheduler {

    private final MessageCenterService messageCenterService;

    @Value("${message-center.announcement.scheduler.enabled:false}")
    private boolean enabled;

    @Value("${message-center.announcement.scheduler.batch-size:200}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${message-center.announcement.scheduler.fixed-delay-ms:30000}")
    public void publishDueAnnouncements() {
        if (!enabled) {
            return;
        }

        try {
            int processed = messageCenterService.publishDueAnnouncements(batchSize);
            log.info("message-center announcement publish batch processed={} (enabled={})", processed, enabled);
        } catch (Exception ex) {
            log.error("message-center announcement scheduler failed", ex);
        }
    }
}
