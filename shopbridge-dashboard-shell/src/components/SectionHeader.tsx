export function SectionHeader({ children }: { children: string }) {
    return (
        <h2 className="text-2xl font-semibold tracking-tight text-ink mb-6">
            {children}
        </h2>
    );
}
