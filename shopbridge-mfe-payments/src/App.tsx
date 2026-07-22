import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PaymentsWidget from './components/Widget';

const queryClient = new QueryClient();

export default function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <div className="max-w-md mx-auto mt-10 p-4 border-2 border-dashed border-gray-300 rounded-lg">
                <h1 className="text-xl font-bold mb-4 text-gray-800 text-center">İzole Çalışma Modu (Payments)</h1>
                <PaymentsWidget />
            </div>
        </QueryClientProvider>
    );
}