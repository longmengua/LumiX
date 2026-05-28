/*
 * 檔案用途：應用服務，管理 reconciliation issue 的 claim/resolve/reopen 工作流。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.ReconciliationIssueWorkflowChanged;
import com.example.exchange.domain.model.entity.ReconciliationReportIssue;
import com.example.exchange.domain.repository.ReconciliationReportStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationIssueWorkflowService {

    public static final String OPEN = "OPEN";
    public static final String CLAIMED = "CLAIMED";
    public static final String RESOLVED = "RESOLVED";

    private final ReconciliationReportStore reportStore;
    private DomainEventPublisher<ReconciliationIssueWorkflowChanged> publisher;

    @Autowired(required = false)
    public void setPublisher(DomainEventPublisher<ReconciliationIssueWorkflowChanged> publisher) {
        this.publisher = publisher;
    }

    @Transactional
    public ReconciliationReportIssue claim(long issueId, String owner) {
        ReconciliationReportIssue issue = requireIssue(issueId);
        if (RESOLVED.equals(issue.getStatus())) {
            throw new IllegalStateException("resolved reconciliation issue cannot be claimed");
        }
        issue.setStatus(CLAIMED);
        issue.setOwner(requireOwner(owner));
        issue.setResolvedAt(null);
        reportStore.saveIssue(issue);
        publish(issue, "CLAIM");
        return issue;
    }

    @Transactional
    public ReconciliationReportIssue resolve(long issueId, String owner) {
        ReconciliationReportIssue issue = requireIssue(issueId);
        if (RESOLVED.equals(issue.getStatus())) {
            return issue;
        }
        issue.setStatus(RESOLVED);
        issue.setOwner(requireOwner(owner));
        issue.setResolvedAt(Instant.now());
        reportStore.saveIssue(issue);
        publish(issue, "RESOLVE");
        return issue;
    }

    @Transactional
    public ReconciliationReportIssue reopen(long issueId, String owner) {
        ReconciliationReportIssue issue = requireIssue(issueId);
        issue.setStatus(OPEN);
        // Keep the latest operator as owner so reopen history remains visible until audit events are added.
        issue.setOwner(requireOwner(owner));
        issue.setResolvedAt(null);
        reportStore.saveIssue(issue);
        publish(issue, "REOPEN");
        return issue;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationReportIssue> openIssues(int limit) {
        return reportStore.findIssuesByStatus(OPEN, limit);
    }

    private ReconciliationReportIssue requireIssue(long issueId) {
        return reportStore.findIssue(issueId)
                .orElseThrow(() -> new IllegalArgumentException("reconciliation issue not found: " + issueId));
    }

    private static String requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("issue owner is required");
        }
        return owner.trim();
    }

    private void publish(ReconciliationReportIssue issue, String action) {
        if (publisher == null) {
            return;
        }
        publisher.publish(new ReconciliationIssueWorkflowChanged(
                issue.getId(),
                issue.getReportId(),
                action,
                issue.getStatus(),
                issue.getOwner(),
                Instant.now()
        ));
    }
}
