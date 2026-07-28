export function Logo({ size = 22 }: { size?: number }) {
    return (
        <span className="font-extrabold tracking-tight" style={{ fontSize: size }}>
            <span className="text-ink">Shop</span>
            <span className="text-brand">Bridge</span>
        </span>
    );
}
