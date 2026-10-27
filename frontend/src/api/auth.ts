import api from "./axios";

// Même clé que dans axios.ts et Layout.tsx
const TOKEN_KEY = "token";
const USER_KEY = "user";

export type Role = "STUDENT" | "TEACHER" | "ADMIN";

export interface SessionUser {
    email: string;
    fullName: string;
    role: Role;
}

/** Réponse de POST /api/auth/login (LoginResponse côté backend). */
export interface LoginResponse extends SessionUser {
    token: string;
    tokenType: string;
    expiresIn: number;
}

export async function login(email: string, password: string): Promise<LoginResponse> {
    const { data } = await api.post<LoginResponse>("/auth/login", { email, password });
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify({
        email: data.email,
        fullName: data.fullName,
        role: data.role,
    }));
    return data;
}

export function getUser(): SessionUser | null {
    try {
        const raw = localStorage.getItem(USER_KEY);
        return raw ? (JSON.parse(raw) as SessionUser) : null;
    } catch {
        return null;
    }
}

/** Vrai si un jeton est présent ET pas encore expiré (le JWT expire après 1 h). */
export function isAuthenticated(): boolean {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return false;
    try {
        const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
        return typeof payload.exp === "number" && payload.exp * 1000 > Date.now();
    } catch {
        return false;
    }
}
/** SC-10 : déconnexion sécurisée. Le jeton est révoqué côté serveur, puis effacé du navigateur. */
export async function logout(): Promise<void> {
    try {
        await api.post("/auth/logout");
    } catch {
        // jeton déjà expiré ou serveur injoignable : on déconnecte quand même localement
    } finally {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    }
}