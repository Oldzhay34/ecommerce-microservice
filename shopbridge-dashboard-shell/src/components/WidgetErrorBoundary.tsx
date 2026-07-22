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
                <div className="p-6 h-48 border border-gray-200 rounded-lg bg-gray-50 flex flex-col items-center justify-center text-center">
                    <p className="text-gray-600 font-semibold mb-1">{this.props.widgetName}</p>
                    <p className="text-sm text-gray-400">Bu bölüm şu anda yüklenemiyor veya erişilemiyor.</p>
                </div>
            );
        }
        return this.props.children;
    }
}