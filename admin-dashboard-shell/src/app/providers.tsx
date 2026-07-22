import { useState, type ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

interface AppProvidersProps {
    children: ReactNode;
}

export function AppProviders({ children }: AppProvidersProps) {
    const [queryClient] = useState(
        () =>
            new QueryClient({
                defaultOptions: {
                    queries: {
                        retry: 1,
                        refetchOnWindowFocus: false,
                        staleTime: 30_000,
                    },
                    mutations: {
                        retry: 0,
                    },
                },
            }),
    );

    return (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
}