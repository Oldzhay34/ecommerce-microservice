import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
    children: ReactNode;
    fallback: ReactNode;
}
interface State {
    hasError: boolean;
}

export class RemoteErrorBoundary extends Component<Props, State> {
    state: State = { hasError: false };

    static getDerivedStateFromError(): State {
        return { hasError: true };
    }

    componentDidCatch(error: Error, info: ErrorInfo) {
        // Remote yükleme/çalışma hatasını logla; sayfa çökmez.
        console.error('[RemoteErrorBoundary]', error, info.componentStack);
    }

    render() {
        if (this.state.hasError) return this.props.fallback;
        return this.props.children;
    }
}