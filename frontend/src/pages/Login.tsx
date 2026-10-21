import { useState, type FormEvent } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import axios from "axios";
import { isAuthenticated, login } from "../api/auth";
import "./Login.css";

/** Transforme l'erreur de l'API en message compréhensible (critère 2 de SC-9). */
function errorMessage(err: unknown): string {
    if (axios.isAxiosError(err)) {
        if (!err.response) {
            return "Impossible de joindre le serveur. Réessayez dans un instant.";
        }
        const apiMessage = (err.response.data as { message?: string } | undefined)?.message;
        if (apiMessage) return apiMessage;

        switch (err.response.status) {
            case 400: return "Veuillez saisir un e-mail valide et un mot de passe.";
            case 401: return "E-mail ou mot de passe incorrect.";
            case 403: return "Ce compte est désactivé. Contactez l’administration.";
            case 423: return "Compte temporairement verrouillé. Réessayez dans quelques minutes.";
        }
    }
    return "Une erreur inattendue est survenue. Réessayez.";
}

export default function Login() {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    // Déjà connecté : pas besoin de revoir la page de connexion
    if (isAuthenticated()) {
        return <Navigate to="/" replace />;
    }

    async function handleSubmit(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setError(null);

        if (!email.trim() || !password) {
            setError("Veuillez saisir votre e-mail et votre mot de passe.");
            return;
        }

        setLoading(true);
        try {
            await login(email.trim(), password);
            navigate("/", { replace: true });   // critère 1 : accès à l'application
        } catch (err) {
            setError(errorMessage(err));
            setPassword("");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="login-page">
            <form className="login-card" onSubmit={handleSubmit} noValidate>
                <h1>SmartCampus</h1>
                <p className="login-subtitle">Connectez-vous avec votre e-mail de l’école</p>

                {error && <div className="login-error" role="alert">{error}</div>}

                <label htmlFor="email" className="sr-only">E-mail</label>
                <div className="input-group">
                    <input
                        id="email"
                        type="email"
                        placeholder="E-mail"
                        autoComplete="username"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        disabled={loading}
                    />
                    <svg className="input-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <rect x="3" y="5" width="18" height="14" rx="2" />
                        <path d="m3 7 9 6 9-6" />
                    </svg>
                </div>

                <label htmlFor="password" className="sr-only">Mot de passe</label>
                <div className="input-group">
                    <input
                        id="password"
                        type={showPassword ? "text" : "password"}
                        placeholder="Mot de passe"
                        autoComplete="current-password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        disabled={loading}
                    />
                    <button
                        type="button"
                        className="input-icon-button"
                        onClick={() => setShowPassword((v) => !v)}
                        aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                    >
                        <svg className="input-icon" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
                            <circle cx="12" cy="12" r="3" />
                            {showPassword && <path d="M3 3l18 18" />}
                        </svg>
                    </button>
                </div>

                <div className="login-options">
                    {/* Mot de passe oublié : SC-31 (sprint 4) */}
                    <span className="login-link is-soon" title="Bientôt disponible">Mot de passe oublié ?</span>
                </div>

                <button type="submit" className="login-button" disabled={loading}>
                    {loading ? "Connexion…" : "Se connecter"}
                </button>

                <p className="login-footer">
                    {/* Inscription : SC-30 (sprint 4) */}
                    Pas encore de compte ?{" "}
                    <span className="login-link is-soon" title="Bientôt disponible">Créer un compte</span>
                </p>
            </form>
        </div>
    );
}