// api.js
import { BACKEND_URL } from "./config.js";

// Helper to parse JWT
function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function (c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

export async function apiFetch(url, options = {}) {
  const token = localStorage.getItem("authToken");

  // Pre-check: Client-side expiration check
  if (token) {
    const decoded = parseJwt(token);
    if (decoded && decoded.exp) {
      if (decoded.exp < Date.now() / 1000) {
        console.warn("Token expired (pre-check). Logging out...");
        forceLogout();
        throw new Error("Session expired");
      }
    }
  }

  const headers = {
    ...(options.headers || {}),
    Authorization: token ? `Bearer ${token}` : undefined,
  };

  try {
    const res = await fetch(url.startsWith("http") ? url : `${BACKEND_URL}${url}`, {
      ...options,
      headers,
    });

    // 🔥 TOKEN EXPIRED / INVALID
    if (res.status === 401 || res.status === 403) {
      forceLogout();
      throw new Error("Session expired");
    }

    return res;
  } catch (err) {
    throw err;
  }
}

function forceLogout() {
  alert("Session expired, please login again");
  localStorage.removeItem("authToken");
  localStorage.removeItem("user");

  // prevent infinite redirect loop
  if (!location.pathname.endsWith("login.html")) {
    location.replace("login.html");
  }
}
