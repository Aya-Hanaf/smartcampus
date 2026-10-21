import axios from "axios";

const api = axios.create({ baseURL: "/api" });

api.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
                // Une erreur 401 sur la connexion elle-même = mauvais identifiants : on reste sur la page pour afficher le message
        if (error.response?.status === 401 && !error.config?.url?.includes("/auth/login")) {
            localStorage.removeItem("token");
            window.location.href = "/login";
        }
        return Promise.reject(error);
    }
);

export default api;