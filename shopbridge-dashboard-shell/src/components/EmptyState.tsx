export function EmptyState({ message }: { message: string }) {
    return (
        <div
            style={{
                color: '#6B7280',
                fontSize: 14,
                padding: '12px 0',
            }}
        >
            {message}
        </div>
    );
}