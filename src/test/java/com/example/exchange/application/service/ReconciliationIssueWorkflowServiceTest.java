/*
 * 檔案用途：測試 reconciliation issue claim/resolve/reopen 工作流。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.ReconciliationIssueWorkflowChanged;
import com.example.exchange.domain.model.entity.ReconciliationReport;
import com.example.exchange.domain.model.entity.ReconciliationReportIssue;
import com.example.exchange.domain.repository.ReconciliationReportStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconciliationIssueWorkflowServiceTest {

    @Test
    @DisplayName("claim/resolve/reopen 會更新 issue 狀態、owner 與 resolvedAt")
    void claimResolveAndReopenIssue() {
        MemReconciliationReportStore store = new MemReconciliationReportStore();
        ReconciliationReportIssue issue = issue(1L, "OPEN");
        store.saveIssue(issue);
        ReconciliationIssueWorkflowService service = new ReconciliationIssueWorkflowService(store);
        List<ReconciliationIssueWorkflowChanged> published = new ArrayList<>();
        service.setPublisher(published::add);

        // 流程：營運人員 claim -> resolve -> reopen，確認 workflow 欄位每一步都可查。
        ReconciliationReportIssue claimed = service.claim(1L, "ops-a");
        ReconciliationReportIssue resolved = service.resolve(1L, "ops-b");
        ReconciliationReportIssue reopened = service.reopen(1L, "ops-c");

        assertThat(claimed.getStatus()).isEqualTo(ReconciliationIssueWorkflowService.CLAIMED);
        assertThat(claimed.getOwner()).isEqualTo("ops-a");
        assertThat(resolved.getStatus()).isEqualTo(ReconciliationIssueWorkflowService.RESOLVED);
        assertThat(resolved.getOwner()).isEqualTo("ops-b");
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(reopened.getStatus()).isEqualTo(ReconciliationIssueWorkflowService.OPEN);
        assertThat(reopened.getOwner()).isEqualTo("ops-c");
        assertThat(reopened.getResolvedAt()).isNull();
        assertThat(published).extracting(ReconciliationIssueWorkflowChanged::action)
                .containsExactly("CLAIM", "RESOLVE", "REOPEN");
        assertThat(published.getLast().owner()).isEqualTo("ops-c");
    }

    @Test
    @DisplayName("resolved issue 不能重新 claim，openIssues 只列 OPEN")
    void resolvedIssueCannotBeClaimedAndOpenIssuesFiltersStatus() {
        MemReconciliationReportStore store = new MemReconciliationReportStore();
        ReconciliationReportIssue open = issue(2L, "OPEN");
        ReconciliationReportIssue resolved = issue(3L, "RESOLVED");
        resolved.setResolvedAt(Instant.parse("2026-05-28T00:00:00Z"));
        store.saveIssue(open);
        store.saveIssue(resolved);
        ReconciliationIssueWorkflowService service = new ReconciliationIssueWorkflowService(store);

        // 流程：resolved case 不可 claim；open queue 不應混入已結案 issue。
        assertThatThrownBy(() -> service.claim(3L, "ops-a"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be claimed");
        assertThat(service.openIssues(10)).extracting(ReconciliationReportIssue::getId)
                .containsExactly(2L);
    }

    private static ReconciliationReportIssue issue(long id, String status) {
        ReconciliationReportIssue issue = new ReconciliationReportIssue();
        setId(issue, id);
        issue.setReportId("report-1");
        issue.setLineNo((int) id);
        issue.setSeverity("ERROR");
        issue.setCode("POSITION_MARGIN_MISMATCH");
        issue.setMessage("test issue");
        issue.setStatus(status);
        issue.setCreatedAt(Instant.parse("2026-05-28T00:00:00Z").plusSeconds(id));
        return issue;
    }

    private static void setId(ReconciliationReportIssue issue, long id) {
        try {
            Field field = ReconciliationReportIssue.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(issue, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class MemReconciliationReportStore implements ReconciliationReportStore {
        private final Map<Long, ReconciliationReportIssue> issues = new LinkedHashMap<>();

        @Override
        public void save(ReconciliationReport report, List<ReconciliationReportIssue> issues) {
            issues.forEach(this::saveIssue);
        }

        @Override
        public Optional<ReconciliationReport> findById(String reportId) {
            return Optional.empty();
        }

        @Override
        public List<ReconciliationReportIssue> findIssues(String reportId) {
            return issues.values().stream()
                    .filter(issue -> reportId.equals(issue.getReportId()))
                    .sorted(Comparator.comparing(ReconciliationReportIssue::getLineNo))
                    .toList();
        }

        @Override
        public Optional<ReconciliationReportIssue> findIssue(long issueId) {
            return Optional.ofNullable(issues.get(issueId));
        }

        @Override
        public void saveIssue(ReconciliationReportIssue issue) {
            issues.put(issue.getId(), copy(issue));
        }

        @Override
        public List<ReconciliationReportIssue> findIssuesByStatus(String status, int limit) {
            return issues.values().stream()
                    .filter(issue -> status.equals(issue.getStatus()))
                    .sorted(Comparator.comparing(ReconciliationReportIssue::getCreatedAt))
                    .limit(limit)
                    .map(ReconciliationIssueWorkflowServiceTest::copy)
                    .toList();
        }

        @Override
        public List<ReconciliationReport> latest(int limit) {
            return new ArrayList<>();
        }
    }

    private static ReconciliationReportIssue copy(ReconciliationReportIssue source) {
        ReconciliationReportIssue copy = new ReconciliationReportIssue();
        setId(copy, source.getId());
        copy.setReportId(source.getReportId());
        copy.setLineNo(source.getLineNo());
        copy.setSeverity(source.getSeverity());
        copy.setCode(source.getCode());
        copy.setMessage(source.getMessage());
        copy.setStatus(source.getStatus());
        copy.setOwner(source.getOwner());
        copy.setResolvedAt(source.getResolvedAt());
        copy.setCreatedAt(source.getCreatedAt());
        return copy;
    }
}
