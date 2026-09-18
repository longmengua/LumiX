import { useEffect, useState, type FormEvent } from "react";

import { ConfirmDialog } from "../../../components/base/ConfirmDialog";
import { AdminFormSelect } from "../../components/AdminFormSelect";
import { AdminPageHero } from "../../components/AdminPageHero";
import {
  createAdminAssetAdjustment,
  fetchAdminAirdropAssetOptions,
  fetchAdminAssetAdjustmentReversalSource,
  type AdminAirdropAssetOption,
  type AdminAssetAdjustmentRequest,
  type AdminReversalSource,
  type AssetAdjustmentType,
} from "../../api/adminAssetsApi";
import { AssetAdjustmentHeroIcon } from "./AssetAdjustmentHeroIcon";
import { AssetAdjustmentArtwork } from "./AssetAdjustmentHeroArtwork";

const positive = (value: string) => /^(?=.*[1-9])\d+(?:\.\d+)?$/.test(value);
const initial = (
  adjustmentType: AssetAdjustmentType,
  assetSymbol = "",
): AdminAssetAdjustmentRequest => ({
  adjustmentType,
  userId: "",
  accountType: "SPOT",
  assetSymbol,
  direction: "CREDIT",
  amount: "",
  reason: "",
  sourceBusinessType: "",
  sourceBusinessId: "",
  incidentReference: "",
});

/** 通用調整保留既有 command 與驗證，只重用 Institutional Blue 的 Hero 與表單 surface。 */
export function AssetAdjustmentForm() {
  const [type, setType] = useState<AssetAdjustmentType>("MANUAL_CORRECTION");
  const [assets, setAssets] = useState<AdminAirdropAssetOption[]>([]);
  const [form, setForm] = useState<AdminAssetAdjustmentRequest>(
    initial("MANUAL_CORRECTION"),
  );
  const [sourceId, setSourceId] = useState("");
  const [source, setSource] = useState<AdminReversalSource | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const destructive =
    type === "BUSINESS_REVERSAL" || form.direction === "DEBIT";
  useEffect(() => {
    void fetchAdminAirdropAssetOptions()
      .then((next) => {
        setAssets(next);
        setForm((current) => ({
          ...current,
          assetSymbol: current.assetSymbol || next[0]?.assetSymbol || "",
        }));
      })
      .catch(() => setError("無法載入可調整資產。"));
  }, []);
  const change = <K extends keyof AdminAssetAdjustmentRequest>(
    key: K,
    value: AdminAssetAdjustmentRequest[K],
  ) => setForm((current) => ({ ...current, [key]: value }));
  function choose(next: AssetAdjustmentType) {
    setType(next);
    setSource(null);
    setSourceId("");
    setError(null);
    setResult(null);
    setForm(initial(next, assets[0]?.assetSymbol));
  }
  async function lookup() {
    if (!sourceId.trim()) {
      setError("請輸入來源 Ledger Entry ID。");
      return;
    }
    try {
      const next = await fetchAdminAssetAdjustmentReversalSource(
        sourceId.trim(),
      );
      setSource(next);
      setError(
        next.eligible ? null : (next.ineligibleReason ?? "此來源不可沖銷。"),
      );
    } catch {
      setError("來源查詢失敗。");
    }
  }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!positive(form.amount) || !form.reason.trim()) {
      setError("請輸入正數數量及原因。");
      return;
    }
    if (
      type === "BUSINESS_REVERSAL" &&
      (!source?.eligible ||
        new DecimalComparison(source.remainingReversible ?? "0", form.amount)
          .isLess)
    ) {
      setError("請先取得可用來源，且本次數量不可超過剩餘可沖回額。");
      return;
    }
    if (
      type !== "BUSINESS_REVERSAL" &&
      (!form.userId?.trim() || !form.assetSymbol || !form.direction)
    ) {
      setError("請完整填寫使用者、帳戶、資產與方向。");
      return;
    }
    if (
      Boolean(form.sourceBusinessType?.trim()) !==
      Boolean(form.sourceBusinessId?.trim())
    ) {
      setError("關聯業務必須同時填寫類型與 ID。");
      return;
    }
    setError(null);
    setConfirming(true);
  }
  async function confirm() {
    setConfirming(false);
    setSubmitting(true);
    try {
      const request =
        type === "BUSINESS_REVERSAL"
          ? {
              adjustmentType: type,
              sourceLedgerEntryId: source?.ledgerEntryId,
              amount: form.amount,
              reason: form.reason,
              incidentReference: form.incidentReference || undefined,
              sourceBusinessType: form.sourceBusinessType || undefined,
              sourceBusinessId: form.sourceBusinessId || undefined,
            }
          : {
              ...form,
              adjustmentType: type,
              incidentReference: form.incidentReference || undefined,
              sourceBusinessType: form.sourceBusinessType || undefined,
              sourceBusinessId: form.sourceBusinessId || undefined,
            };
      const response = await createAdminAssetAdjustment(
        request,
        crypto.randomUUID(),
      );
      setResult(
        `調整已完成，Journal #${response.ledgerJournalId}${response.replayed ? "（安全重送）" : ""}`,
      );
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "資產調整未完成。");
    } finally {
      setSubmitting(false);
    }
  }
  return (
    <section className="admin-airdrop-form" aria-label="資產沖銷與調整">
      <AdminPageHero
        icon={<AssetAdjustmentHeroIcon />}
        title="資產調整"
        description="以受控命令處理人工修正、系統補償與業務沖銷，並由後端留下完整帳務與稽核紀錄。"
        chips={[
          { id: "correction", label: "人工修正" },
          { id: "compensation", label: "系統補償" },
          { id: "reversal", label: "業務沖銷" },
        ]}
        illustration={
          <AssetAdjustmentArtwork className="admin-page-hero__artwork-image" />
        }
        slogan="精準・可溯・受控"
        supportingText="提交前請核對來源與金額。"
      />
      <form className="admin-airdrop-form__body" onSubmit={submit} noValidate>
        <section className="admin-airdrop-form__section">
          <label className="field">
            <span className="field__label">調整類型 *</span>
            <AdminFormSelect
              ariaLabel="調整類型"
              value={type}
              onChange={(value) => choose(value as AssetAdjustmentType)}
              options={[
                { value: "MANUAL_CORRECTION", label: "人工修正" },
                { value: "BUSINESS_REVERSAL", label: "業務沖銷" },
                { value: "COMPENSATION", label: "系統補償" },
              ]}
            />
          </label>
        </section>
        {type === "BUSINESS_REVERSAL" ? (
          <section className="admin-airdrop-form__section">
            <label className="field">
              <span className="field__label">原始帳本紀錄 ID *</span>
              <input
                className="input"
                value={sourceId}
                onChange={(event) => setSourceId(event.target.value)}
              />
            </label>
            <button
              className="secondary-button"
              type="button"
              onClick={() => void lookup()}
            >
              解析來源
            </button>
            {source ? <Summary source={source} /> : null}
          </section>
        ) : (
          <section className="admin-airdrop-form__section">
            <div className="admin-airdrop-form__grid">
              <label className="field">
                <span className="field__label">方向 *</span>
                <AdminFormSelect
                  ariaLabel="方向"
                  destructive={form.direction === "DEBIT"}
                  value={form.direction ?? ""}
                  onChange={(value) =>
                    change("direction", value as "CREDIT" | "DEBIT")
                  }
                  options={[
                    { value: "CREDIT", label: "補入" },
                    { value: "DEBIT", label: "扣回" },
                  ]}
                />
              </label>
              <label className="field">
                <span className="field__label">使用者 *</span>
                <input
                  className="input"
                  value={form.userId}
                  onChange={(event) => change("userId", event.target.value)}
                />
              </label>
              <label className="field">
                <span className="field__label">帳戶 *</span>
                <input className="input" value="SPOT" readOnly />
              </label>
              <label className="field">
                <span className="field__label">資產 *</span>
                <AdminFormSelect
                  ariaLabel="資產"
                  value={form.assetSymbol ?? ""}
                  onChange={(value) => change("assetSymbol", value)}
                  options={assets.map((asset) => ({
                    value: asset.assetSymbol,
                    label: asset.assetSymbol,
                  }))}
                />
              </label>
            </div>
          </section>
        )}
        <section className="admin-airdrop-form__section">
          <div className="admin-airdrop-form__grid">
            <label className="field">
              <span className="field__label">數量 *</span>
              <input
                className="input"
                inputMode="decimal"
                value={form.amount}
                onChange={(event) => change("amount", event.target.value)}
              />
            </label>
            <label className="field">
              <span className="field__label">事件／工單編號（選填）</span>
              <input
                className="input"
                value={form.incidentReference ?? ""}
                onChange={(event) =>
                  change("incidentReference", event.target.value)
                }
              />
            </label>
            <label className="field">
              <span className="field__label">來源業務類型（選填）</span>
              <input
                className="input"
                value={form.sourceBusinessType ?? ""}
                onChange={(event) =>
                  change("sourceBusinessType", event.target.value)
                }
              />
            </label>
            <label className="field">
              <span className="field__label">來源業務 ID（選填）</span>
              <input
                className="input"
                value={form.sourceBusinessId ?? ""}
                onChange={(event) =>
                  change("sourceBusinessId", event.target.value)
                }
              />
            </label>
          </div>
          <label className="field">
            <span className="field__label">原因 *</span>
            <textarea
              className="input"
              maxLength={256}
              value={form.reason}
              onChange={(event) => change("reason", event.target.value)}
            />
          </label>
        </section>
        <div className="admin-airdrop-form__footer admin-airdrop-form__footer--actions-only">
          <div className="admin-airdrop-form__actions">
            <button
              className={`primary-button admin-airdrop-form__submit${destructive ? " primary-button--danger" : ""}`}
              disabled={submitting}
              type="submit"
            >
              {submitting
                ? "處理中"
                : type === "BUSINESS_REVERSAL"
                  ? "確認沖銷"
                  : "確認調整"}
            </button>
          </div>
        </div>
        {error ? (
          <p
            className="admin-airdrop-form__feedback admin-airdrop-form__feedback--error"
            role="alert"
          >
            {error}
          </p>
        ) : null}
        {result ? (
          <p
            className="admin-airdrop-form__feedback admin-airdrop-form__feedback--success"
            role="status"
          >
            {result}
          </p>
        ) : null}
      </form>
      <ConfirmDialog
        open={confirming}
        title={type === "BUSINESS_REVERSAL" ? "確認業務沖銷" : "確認資產調整"}
        description="提交後將以 append-only ledger 建立新的 compensating entries。"
        confirmLabel={type === "BUSINESS_REVERSAL" ? "確認沖銷" : "確認調整"}
        cancelLabel="取消"
        confirmTone={destructive ? "danger" : "default"}
        onCancel={() => setConfirming(false)}
        onConfirm={() => void confirm()}
      >
        <dl className="admin-airdrop-form__confirmation">
          <div>
            <dt>類型</dt>
            <dd>{type}</dd>
          </div>
          <div>
            <dt>數量</dt>
            <dd>
              {form.amount}{" "}
              {type === "BUSINESS_REVERSAL"
                ? source?.assetSymbol
                : form.assetSymbol}
            </dd>
          </div>
          <div>
            <dt>原因</dt>
            <dd>{form.reason}</dd>
          </div>
        </dl>
      </ConfirmDialog>
    </section>
  );
}

function Summary({ source }: { source: AdminReversalSource }) {
  return (
    <dl className="admin-airdrop-form__confirmation">
      <div>
        <dt>使用者</dt>
        <dd>{source.userEmail ?? source.userId}</dd>
      </div>
      <div>
        <dt>帳戶／資產</dt>
        <dd>
          {source.accountType}／{source.assetSymbol}
        </dd>
      </div>
      <div>
        <dt>原始影響</dt>
        <dd>
          {source.originalDirection === "CREDIT" ? "補入" : "扣回"}{" "}
          {source.originalAmount}
        </dd>
      </div>
      <div>
        <dt>已沖銷／剩餘可沖銷數量</dt>
        <dd>
          {source.alreadyReversed}／{source.remainingReversible}
        </dd>
      </div>
    </dl>
  );
}

/** 比較 decimal 字串，禁止以 JavaScript binary floating point 判斷可沖銷額。 */
class DecimalComparison {
  readonly isLess: boolean;
  constructor(left: string, right: string) {
    const [li, lf = ""] = left.split(".");
    const [ri, rf = ""] = right.split(".");
    const scale = Math.max(lf.length, rf.length);
    const normalize = (integer: string, fraction: string) =>
      `${integer.replace(/^0+(?=\d)/, "")}${fraction.padEnd(scale, "0")}`;
    const a = normalize(li, lf);
    const b = normalize(ri, rf);
    this.isLess = a.length !== b.length ? a.length < b.length : a < b;
  }
}
