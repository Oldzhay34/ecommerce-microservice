import { type PropsWithChildren, useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

/**
 * Tekil (singleton) QueryClient tüm route ve remote'ları sarar.
 * Remote'lar KENDİ QueryClientProvider'ını kurmaz; bu cache paylaşılır.
 */
export function AppProviders({ children }: PropsWithChildren) {
    const [queryClient] = useState(
        () =>
            new QueryClient({
                defaultOptions: {
                    queries: {
                        retry: 1,
                        refetchOnWindowFocus: false,
                        staleTime: 30_000,
                    },
                },
            }),
    );

    return (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
}