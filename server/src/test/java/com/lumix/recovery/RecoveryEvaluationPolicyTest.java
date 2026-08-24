package com.lumix.recovery;
import static org.junit.jupiter.api.Assertions.assertEquals; import java.time.Instant; import org.junit.jupiter.api.Test;
class RecoveryEvaluationPolicyTest { @Test void mismatchOrMissingHumanApprovalCannotBecomeReady(){
 // restore readiness 不能只靠存在 backup；必須可重放驗證且保留人工 resume gate。
 RecoveryEvaluationPolicy policy=new RecoveryEvaluationPolicy(); RecoveryArtifactManifest manifest=new RecoveryArtifactManifest("backup-1","0".repeat(64),Instant.EPOCH); assertEquals(RecoveryReadiness.REPLAY_MISMATCH,policy.evaluate(manifest,"1".repeat(64),true)); assertEquals(RecoveryReadiness.HUMAN_APPROVAL_MISSING,policy.evaluate(manifest,"0".repeat(64),false)); assertEquals(RecoveryReadiness.READY,policy.evaluate(manifest,"0".repeat(64),true)); }
}
