import { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
}

export class RemoteErrorBoundary extends Component<Props, State> {
    public state: State = {
        hasError: false
    };

    public static getDerivedStateFromError(): State {
        return { hasError: true };
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        console.error('Remote Module Error caught:', error, errorInfo);
    }

    public render() {
        if (this.state.hasError) {
            return (
                <div className="p-6 border border-red-200 bg-[#FEF2F2] rounded-xl text-[#B91C1C]">
                    <h4 className="font-semibold text-base mb-1">Modül Yükleme Hatası</h4>
                    <p className="text-sm">Bu bölüm şu anda yüklenemedi. Sayfayı yenileyin.</p>
                </div>
            );
        }

        return this.props.children;
    }
}