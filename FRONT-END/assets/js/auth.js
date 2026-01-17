// auth.js - Fully Fixed & Working
import { BACKEND_URL } from './config.js';

// OAuth2 Authorization URLs (Spring Security standard)
const PROVIDERS = {
  google: `${BACKEND_URL}/oauth2/authorization/google`,
  github: `${BACKEND_URL}/oauth2/authorization/github`,
  linkedin: `${BACKEND_URL}/oauth2/authorization/linkedin`
};

// Show message (uses #result or fallback to alert)
function showMsg(message, type = "info") {
  const el = document.getElementById("result");
  if (el) {
    el.style.display = "block";
    el.textContent = message;
    el.className = type === "danger" ? "alert alert-danger" : "alert alert-success";
    setTimeout(() => { el.style.display = "none"; }, 4000);
  } else {
    alert(message);
  }
}

// Open centered popup
function openOAuthPopup(authUrl, providerName) {
  const width = 600;
  const height = 700;
  const left = Math.max(0, (screen.width - width) / 2);
  const top = Math.max(0, (screen.height - height) / 2);
  const features = `width=${width},height=${height},left=${left},top=${top},resizable=yes,scrollbars=yes,status=no,toolbar=no,menubar=no`;

  const popup = window.open(authUrl, `${providerName}-oauth`, features);

  if (!popup || popup.closed || typeof popup.closed === 'undefined') {
    showMsg("Popup blocked! Please allow popups for this site.", "danger");
    return null;
  }
  return popup;
}

// Handle message from OAuth2 redirect page
function handleOAuthMessage(event) {
  // Critical security check
  if (event.origin !== new URL(BACKEND_URL).origin) return;

  const data = event.data;

  if (data.accessToken || data.token) {
    localStorage.setItem("authToken", data.accessToken || data.token);
    localStorage.setItem("user", JSON.stringify(data.user || data));

    showMsg(`Logged in successfully as ${data.user?.name || 'User'}!`, "success");

    window.removeEventListener("message", handleOAuthMessage);
    setTimeout(() => window.location.href = "home.html", 800);
  }

  if (data.error) {
    showMsg("Login failed: " + (data.message || data.error), "danger");
    window.removeEventListener("message", handleOAuthMessage);
  }
}

// Start OAuth2 flow
function loginWithProvider(provider) {
  const url = PROVIDERS[provider];
  if (!url) {
    showMsg("Provider not supported", "danger");
    return;
  }

  const popup = openOAuthPopup(url, provider);
  if (!popup) return;

  // Listen once
  window.addEventListener("message", handleOAuthMessage, { once: true });

  // Detect manual popup close
  const checkClosed = setInterval(() => {
    if (popup.closed) {
      clearInterval(checkClosed);
      if (!localStorage.getItem("authToken")) {
        showMsg("Login cancelled or failed", "danger");
      }
    }
  }, 500);
}

// Global logout
function logout() {
  localStorage.removeItem("authToken");
  localStorage.removeItem("user");
  //showMsg("Logged out successfully", "success");
  setTimeout(() => window.location.href = "login.html", 800);
}

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

// DOM Ready
document.addEventListener("DOMContentLoaded", () => {
  const token = localStorage.getItem("authToken");

  // Check if token is expired
  if (token) {
    const decoded = parseJwt(token);
    if (decoded && decoded.exp) {
      const currentTime = Date.now() / 1000;
      if (decoded.exp < currentTime) {
        // Token expired
        console.warn("Token expired. Logging out...");
        alert("Session expired, please login again");
        logout();
        return; // Stop further execution
      }
    }
  }

  const currentPage = window.location.pathname.split("/").pop() || "index.html";

  const loginPages = ["index.html", "login.html", "register.html", ""]; // "" for root

  // Redirect logged-in users away from login/register pages
  if (token && loginPages.includes(currentPage)) {
    showMsg("Already logged in. Redirecting...", "success");
    setTimeout(() => window.location.replace("home.html"), 800);
    return;
  }

  // Redirect unauthenticated users from protected pages
  if (!token && currentPage === "home.html") {
    showMsg("Please log in to continue", "danger");
    setTimeout(() => window.location.href = "login.html", 1500);
    return;
  }

  const toggleRegister = document.getElementById("toggleRegister");
  const backToLogin = document.getElementById("backToLogin");
  if (toggleRegister) {
    toggleRegister.addEventListener("click", (e) => {
      e.preventDefault();
      const l = document.getElementById("loginForm");
      const r = document.getElementById("registerForm");
      if (l && r) {
        l.style.display = "none";
        r.style.display = "flex";
      }
    });
  }
  if (backToLogin) {
    backToLogin.addEventListener("click", () => {
      const l = document.getElementById("loginForm");
      const r = document.getElementById("registerForm");
      if (l && r) {
        r.style.display = "none";
        l.style.display = "flex";
      }
    });
  }
  // OAuth Buttons
  const googleBtn = document.getElementById("googleBtn");
  const githubBtn = document.getElementById("githubBtn");
  const linkedinBtn = document.getElementById("linkedinBtn");

  if (googleBtn) googleBtn.onclick = () => loginWithProvider("google");
  if (githubBtn) githubBtn.onclick = () => loginWithProvider("github");
  if (linkedinBtn) linkedinBtn.onclick = () => loginWithProvider("linkedin");

  // === Traditional Email/Password Login Form ===
  const loginForm = document.getElementById("loginForm");
  if (loginForm) {
    loginForm.onsubmit = async (e) => {
      e.preventDefault();
      const email = document.getElementById("loginEmail")?.value.trim();
      const password = document.getElementById("loginPassword")?.value.trim();

      if (!email || !password) {
        showMsg("Please fill in all fields", "danger");
        return;
      }

      try {
        const res = await fetch(`${BACKEND_URL}/api/auth/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email, password })
        });

        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          throw new Error(err.message || "Invalid credentials");
        }

        const data = await res.json();
        localStorage.setItem("authToken", data.accessToken || data.token || "");
        localStorage.setItem("user", JSON.stringify(data.user || data));

        showMsg("Logged in successfully!", "success");
        setTimeout(() => window.location.href = "home.html", 700);
      } catch (err) {
        showMsg(err.message || "Login failed", "danger");
      }
    };
  }

  // === Registration Form ===
  const registerForm = document.getElementById("registerForm");
  if (registerForm) {
    registerForm.onsubmit = async (e) => {
      e.preventDefault();
      const name = document.getElementById("regName")?.value.trim();
      const email = document.getElementById("regEmail")?.value.trim();
      const password = document.getElementById("regPassword")?.value.trim();

      if (!name || !email || !password) {
        showMsg("All fields are required", "danger");
        return;
      }

      try {
        const res = await fetch(`${BACKEND_URL}/api/auth/register`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ name, email, password })
        });

        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          throw new Error(err.message || "Registration failed");
        }

        const data = await res.json();
        localStorage.setItem("authToken", data.accessToken || data.token || "");
        localStorage.setItem("user", JSON.stringify(data.user || data));

        showMsg("Account created successfully!", "success");
        setTimeout(() => window.location.href = "home.html", 700);
      } catch (err) {
        showMsg(err.message || "Registration failed", "danger");
      }
    };
  }
});

// Expose for debugging or global use
window.logout = logout;
window.authDebug = { localStorage, BACKEND_URL };