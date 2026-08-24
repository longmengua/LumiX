package com.lumix.performance;
import java.util.Objects;
/** 可重現且隔離的 workload metadata；不含真實客戶、secret 或可執行 load command。 */
public record WorkloadProfile(String profileId,int maximumConcurrentActors,boolean isolatedEnvironment){public WorkloadProfile{profileId=Objects.requireNonNull(profileId,"profileId").trim();if(profileId.isEmpty()||maximumConcurrentActors<1)throw new IllegalArgumentException("workload identity and positive concurrency required");}}
