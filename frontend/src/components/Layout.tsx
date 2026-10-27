import { Link, Outlet, useNavigate } from "react-router-dom";
import { logout } from "../api/auth";

export default function Layout() {
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout();                          // SC-10 : jeton révoqué côté serveur
        navigate("/login", { replace: true });
    };

    return (
        <>
            <nav className="navbar">
                <div className="nav-links">
                    <Link to="/">Home</Link>
                </div>
                <button className="logout-btn" onClick={handleLogout}>Logout</button>
            </nav>
            <main className="content">
                <Outlet />
            </main>
        </>
    );
}