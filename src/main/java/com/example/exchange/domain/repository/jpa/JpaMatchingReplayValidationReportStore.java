/*
 * 檔案用途：JPA adapter，實作 durable matching replay validation report store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.MatchingReplayValidationReport;
import com.example.exchange.domain.model.entity.MatchingReplayValidationReportRecord;
import com.example.exchange.domain.repository.MatchingReplayValidationReportStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaMatchingReplayValidationReportStore implements MatchingReplayValidationReportStore {

    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<>() {
            };

    private final MatchingReplayValidationReportRecordJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void save(MatchingReplayValidationReport report) {
        MatchingReplayValidationReportRecord record = new MatchingReplayValidationReportRecord();
        record.setSymbolCode(normalize(report.symbolCode()));
        record.setValid(report.valid());
        record.setStartCommandOffset(report.startCommandOffset());
        record.setExpectedCommandOffset(report.expectedCommandOffset());
        record.setActualCommandOffset(report.actualCommandOffset());
        record.setExpectedEventOffset(report.expectedEventOffset());
        record.setActualEventOffset(report.actualEventOffset());
        record.setExpectedMatchSequence(report.expectedMatchSequence());
        record.setActualMatchSequence(report.actualMatchSequence());
        record.setIssuesPayload(writeIssues(report.issues()));
        record.setValidatedAt(report.validatedAt());
        repository.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchingReplayValidationReport> findBySymbol(String symbolCode, int limit) {
        return repository.findBySymbolCodeOrderByValidatedAtDesc(
                        normalize(symbolCode),
                        PageRequest.of(0, Math.max(1, limit))
                )
                .stream()
                .map(this::toReport)
                .toList();
    }

    private MatchingReplayValidationReport toReport(MatchingReplayValidationReportRecord record) {
        return new MatchingReplayValidationReport(
                record.getSymbolCode(),
                record.getValid(),
                record.getStartCommandOffset(),
                record.getExpectedCommandOffset(),
                record.getActualCommandOffset(),
                record.getExpectedEventOffset(),
                record.getActualEventOffset(),
                record.getExpectedMatchSequence(),
                record.getActualMatchSequence(),
                readIssues(record.getIssuesPayload()),
                record.getValidatedAt()
        );
    }

    private String writeIssues(List<String> issues) {
        try {
            return objectMapper.writeValueAsString(issues == null ? List.of() : issues);
        } catch (Exception e) {
            throw new IllegalStateException("serialize matching replay issues failed", e);
        }
    }

    private List<String> readIssues(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            throw new IllegalStateException("deserialize matching replay issues failed", e);
        }
    }

    private static String normalize(String symbolCode) {
        return symbolCode == null ? "" : symbolCode.trim().toUpperCase();
    }
}
