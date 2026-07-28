import { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
    children: ReactNode;
    widgetName: string;
}

interface State {
    hasError: boolean;
}

export class WidgetErrorBoundary extends Component<Props, State> {
    public state: State = { hasError: false };

    public static getDerivedStateFromError(_: Error): State {
        return { hasError: true };
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        console.error(`[MFE Yükleme Hatası] ${this.props.widgetName}:`, error, errorInfo);
    }

    public render() {
        if (this.state.hasError) {
            return (
                <div className="p-6 h-48 rounded-sb-lg bg-surface shadow-sb flex flex-col items-center justify-center text-center">
                    <p className="text-ink font-semibold mb-1">{this.props.widgetName}</p>
                    <p className="text-sm text-ink-faint">Bu bölüm şu anda yüklenemiyor veya erişilemiyor.</p>
                </div>
            );
        }
        return this.props.children;
    }
}