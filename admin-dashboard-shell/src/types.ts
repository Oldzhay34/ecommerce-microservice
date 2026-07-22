export interface AdminSession {
    authToken: string;
    userId: string;
    role: 'ADMIN';
}

export interface JwtPayload {
    sub: string;
    email: string;
    role: string;
    iat: number;
    exp: number;
}