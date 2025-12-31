/* profile.js - loads profile.html content */
document.addEventListener("DOMContentLoaded", () => {
  try {
    const raw = localStorage.getItem("user");
    if (!raw) {
      window.location.href = "index.html";
      return;
    }
    const user = JSON.parse(raw);
    document.getElementById("profileName").textContent = user.username || user.name || user.email.split('@')[0];
    document.getElementById("profileEmail").textContent = user.email || "—";
    document.getElementById("detailProvider").textContent = user.provider || user.authProvider || "Local";
    document.getElementById("detailId").textContent = user.id || user.userId || "N/A";
    if (user.picture || user.pictureUrl || user.avatar) {
      const img = document.getElementById("profileAvatar");
      img.src = user.picture || user.pictureUrl || user.avatar;
    }
  } catch (err) {
    console.error(err);
    localStorage.removeItem("user");
    window.location.href = "index.html";
  }
});
