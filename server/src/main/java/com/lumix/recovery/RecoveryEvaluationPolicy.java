package com.lumix.recovery;
import java.util.Objects;
/** P32 recovery gate，要求 artifact digest、deterministic replay digest 與 human approval 同時成立；不執行 restore/resume。 */
public final class RecoveryEvaluationPolicy { public RecoveryReadiness evaluate(RecoveryArtifactManifest manifest,String replayDigest,boolean humanApproved){manifest=Objects.requireNonNull(manifest,"manifest");replayDigest=Objects.requireNonNull(replayDigest,"replayDigest");if(!manifest.integrityDigest().equals(replayDigest))return RecoveryReadiness.REPLAY_MISMATCH;if(!humanApproved)return RecoveryReadiness.HUMAN_APPROVAL_MISSING;return RecoveryReadiness.READY;} }
