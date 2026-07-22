export function SectionHeader({ children }: { children: string }) {
    return (
        <h2
            style={{
                textTransform: 'uppercase',
                fontSize: 13,
                letterSpacing: '0.06em',
                fontWeight: 600,
                color: '#6B7280',
                marginBottom: 16,
            }}
        >
            {children}
        </h2>
    );
}