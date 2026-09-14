import { Component, type ErrorInfo, type ReactNode } from 'react';

type Props = { children: ReactNode; fallback: ReactNode; resetKey: string };
type State = { failed: boolean };

/** 防止單一 React runtime exception 拆掉 Admin chrome；詳細例外僅留在 development console。 */
export class AdminErrorBoundary extends Component<Props, State> {
  state: State = { failed: false };

  static getDerivedStateFromError() { return { failed: true }; }

  componentDidCatch(error: Error, info: ErrorInfo) {
    if (import.meta.env.DEV) console.error('Admin route runtime error', error, info);
  }

  componentDidUpdate(previous: Props) {
    if (this.state.failed && previous.resetKey !== this.props.resetKey) this.setState({ failed: false });
  }

  render() { return this.state.failed ? this.props.fallback : this.props.children; }
}
