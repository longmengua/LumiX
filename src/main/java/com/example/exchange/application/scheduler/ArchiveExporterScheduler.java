/*
 * 檔案用途：預設關閉的 archive exporter 排程入口。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.ArchiveExporterService;
import com.example.exchange.infra.config.ArchiveExporterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArchiveExporterScheduler {

    private final ArchiveExporterService archiveExporterService;
    private final ArchiveExporterProperties properties;

    @Scheduled(fixedDelayString = "${archive.exporter.fixed-delay-ms:86400000}")
    public void exportPreviousWindow() {
        if (!properties.isEnabled()) {
            return;
        }
        archiveExporterService.run(null);
    }
}
