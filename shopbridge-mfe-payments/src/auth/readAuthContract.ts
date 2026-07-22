const TOKEN_KEY = 'shopbridge_access_token';

export function getAccessToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function getUserIdFromToken(): string | null {
    const token = getAccessToken();
    if (!token) return null;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.sub ?? null;
    } catch {
        return null;
    }
}