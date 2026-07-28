import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import OrdersWidget from './components/Widget';

const queryClient = new QueryClient();

export default function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <div className="max-w-md mx-auto mt-10 p-4 border-2 border-dashed border-border rounded-sb-lg">
                <h1 className="text-xl font-bold mb-4 text-ink text-center">İzole Çalışma Modu (Orders)</h1>
                <OrdersWidget />
            </div>
        </QueryClientProvider>
    );
}