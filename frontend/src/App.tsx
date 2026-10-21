import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoutes from "./components/ProtectedRoutes";
import Home from "./pages/Home";
import Login from "./pages/Login";

export default function App() {
    return (
        <Routes>
            {/* Page publique */}
            <Route path="/login" element={<Login />} />

            {/* Pages réservées aux utilisateurs connectés */}
            <Route element={<ProtectedRoutes />}>
                <Route element={<Layout />}>
                    <Route path="/" element={<Home />} />
                </Route>
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
}