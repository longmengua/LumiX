import { Badge } from '../../../components/base/Badge';
import { EmptyState } from '../../../components/base/State';
import { formatAmount, formatTime } from '../../../utils/format';
import type { DepositRecord, WithdrawRecord } from './mockWalletService';

type DepositTableProps = {
  records: DepositRecord[];
};

type WithdrawTableProps = {
  records: WithdrawRecord[];
};

export function DepositRecordsTable({ records }: DepositTableProps) {
  return records.length > 0 ? (
    <div className="wallet-table">
      <div className="wallet-table__head wallet-table__head--deposit">
        <span>Time</span>
        <span>Asset</span>
        <span>Network</span>
        <span>Amount</span>
        <span>Tx Hash</span>
        <span>Confirmations</span>
        <span>Status</span>
      </div>
      {records.map((record) => (
        <div className="wallet-table__row wallet-table__row--deposit" key={`${record.time}-${record.txHash}`}>
          <span>{formatTime(record.time)}</span>
          <span>{record.asset}</span>
          <span>{record.network}</span>
          <span>{formatAmount(record.amount, getFractionDigits(record.asset))}</span>
          <span>{record.txHash}</span>
          <span>{record.confirmations}</span>
          <Badge tone={getDepositTone(record.status)}>{record.status}</Badge>
        </div>
      ))}
    </div>
  ) : (
    <EmptyState title="No deposit records" description="Deposit records will appear once the wallet adapter returns activity." />
  );
}

export function WithdrawRecordsTable({ records }: WithdrawTableProps) {
  return records.length > 0 ? (
    <div className="wallet-table">
      <div className="wallet-table__head wallet-table__head--withdraw">
        <span>Time</span>
        <span>Asset</span>
        <span>Network</span>
        <span>Amount</span>
        <span>Fee</span>
        <span>Receive Amount</span>
        <span>Address</span>
        <span>Tx Hash</span>
        <span>Status</span>
      </div>
      {records.map((record) => (
        <div className="wallet-table__row wallet-table__row--withdraw" key={`${record.time}-${record.txHash}`}>
          <span>{formatTime(record.time)}</span>
          <span>{record.asset}</span>
          <span>{record.network}</span>
          <span>{formatAmount(record.amount, getFractionDigits(record.asset))}</span>
          <span>{formatAmount(record.fee, 4)}</span>
          <span>{formatAmount(record.receiveAmount, getFractionDigits(record.asset))}</span>
          <span>{record.address}</span>
          <span>{record.txHash}</span>
          <Badge tone={getWithdrawTone(record.status)}>{record.status}</Badge>
        </div>
      ))}
    </div>
  ) : (
    <EmptyState title="No withdraw records" description="Withdraw records will appear once the wallet adapter returns activity." />
  );
}

function getFractionDigits(asset: string) {
  return asset === 'USDT' ? 2 : 4;
}

function getDepositTone(status: DepositRecord['status']) {
  switch (status) {
    case 'Confirmed':
      return 'success';
    case 'Pending':
      return 'warning';
    case 'Rejected':
      return 'danger';
    default:
      return 'neutral';
  }
}

function getWithdrawTone(status: WithdrawRecord['status']) {
  switch (status) {
    case 'Completed':
      return 'success';
    case 'Processing':
      return 'warning';
    case 'Rejected':
      return 'danger';
    default:
      return 'neutral';
  }
}
