/* sidebar.js - small interactions for the Home page */
document.addEventListener("DOMContentLoaded", () => {
  // Load user into sidebar header
  const userRaw = localStorage.getItem("user");
  if (!userRaw) {
    // not logged in
    // redirect to login if on protected page
    if (location.pathname.endsWith('home.html')) window.location.href = 'index.html';
    return;
  }
  const user = JSON.parse(userRaw);
  const headerUser = document.getElementById("headerUser");
  const headerAvatar = document.getElementById("headerAvatar");
  const sidebarAvatar = document.getElementById("sidebarAvatar");
  if (headerUser) headerUser.textContent = user.username || user.name || user.email || 'User';
  if (headerAvatar && (user.profilePic || user.pictureUrl || user.avatar)) {
    headerAvatar.src = user.profilePic || user.pictureUrl || user.avatar;
  }
  if (sidebarAvatar && (user.picture || user.pictureUrl || user.avatar)) {
    sidebarAvatar.src = user.picture || user.pictureUrl || user.avatar;
  }

  // Sidebar toggle
  const sidebar = document.querySelector('.sidebar');
  const menuToggle = document.getElementById("menuToggle");
  if (menuToggle) {
    menuToggle.addEventListener("click", () => {
      sidebar.classList.toggle('collapsed');
    });
  }
});
