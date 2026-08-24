package com.lumix.api.gateway;
/** API admission outcome；ALLOWED 不會觸發 endpoint、command 或任何 domain mutation。 */
public enum ApiAdmissionDecision { ALLOWED, HEALTH_NOT_TRUSTED_REJECTED, RATE_LIMIT_REJECTED, IDEMPOTENCY_CONFLICT_REJECTED }
