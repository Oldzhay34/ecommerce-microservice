import { jwtDecode } from 'jwt-decode';
import { useSessionStore, type Role } from '../session/store';

interface JwtPayload {
    sub: string;
    role?: string;
    roles?: string[];
    authorities?: string[] | string;
    exp?: number;
}

export interface LoginResponse {
    accessToken: string;
}

function extractRole(payload: JwtPayload): Role {
    const isStore =
        payload.role === 'STORE' ||
        payload.role === 'ROLE_STORE' ||
        (payload.roles && payload.roles.includes('STORE')) ||
        (Array.isArray(payload.authorities) &&
            payload.authorities.some((a) => a.toUpperCase().includes('STORE')));
    if (isStore) return 'STORE';
    return 'CUSTOMER';
}

export function applyLogin(res: LoginResponse): { redirectTo: string } {
    const payload = jwtDecode<JwtPayload>(res.accessToken);
    const role = extractRole(payload);
    const storeId = payload.sub;

    useSessionStore.getState().setSession({
        authToken: res.accessToken,
        storeId,
        role,
    });

    const redirectTo = role === 'STORE' ? '/storedashboard' : '/dashboard';
    return { redirectTo };
}