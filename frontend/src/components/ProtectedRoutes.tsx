import { Link, Outlet, useNavigate } from "react-router-dom";

export default function Layout() {
    const navigate = useNavigate();

    const logout = () => {
        localStorage.removeItem("token");
        navigate("/login");
    };

    return (
        <>
            <nav className="navbar">
                <div className="nav-links">
                    <Link to="/">Home</Link>
                </div>
                <button className="logout-btn" onClick={logout}>Logout</button>
            </nav>
            <main className="content">
                <Outlet />
            </main>
        </>
    );
}