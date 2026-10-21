import { Navigate, Outlet } from "react-router-dom";
import { isAuthenticated } from "../api/auth";

/** Laisse passer vers les pages protégées seulement si l'utilisateur est connecté. */
export default function ProtectedRoutes() {
    return isAuthenticated() ? <Outlet /> : <Navigate to="/login" replace />;
}