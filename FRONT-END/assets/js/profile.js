// Update copyright year
document.getElementById('fc-year').textContent = new Date().getFullYear();

document.addEventListener("DOMContentLoaded", () => {
  const userRaw = localStorage.getItem("user");

  // If no user logged in, redirect to login
  if (!userRaw) {
    if (location.pathname.endsWith('profile.html')) {
      window.location.href = 'login.html';
    }
    return;
  }

  let user;
  try {
    user = JSON.parse(userRaw);
  } catch (e) {
    console.error("Invalid user data in localStorage");
    window.location.href = 'login.html';
    return;
  }

  // 1. Update Profile Name
  const nameElement = document.querySelector('.profile-name');
  if (nameElement) {
    const displayName = user.username || user.name || user.displayName || user.email?.split('@')[0] || 'User';
    nameElement.textContent = displayName;
  }

  // 2. Update Profile Image (if available)
  const avatarImg = document.querySelector('.profile-avatar img');
  if (avatarImg && (user.profilePic || user.photoURL || user.avatar || user.image)) {
    avatarImg.src = user.profilePic || user.photoURL || user.avatar || user.image;
    avatarImg.alt = `${nameElement?.textContent || 'User'} Avatar`;
  }

  // 3. Add Email below the name (since your current HTML doesn't have a dedicated email field)
  // We'll insert it dynamically as a new paragraph under profile-info
  const profileInfo = document.querySelector('.profile-info');
  if (profileInfo && user.email) {
    // Avoid adding multiple times if script runs again
    if (!document.getElementById('profile-email')) {
      const emailParagraph = document.createElement('p');
      emailParagraph.id = 'profile-email';
      emailParagraph.className = 'profile-email';
      emailParagraph.textContent = user.email;
      emailParagraph.style.margin = '8px 0';
      emailParagraph.style.color = '#666';
      emailParagraph.style.fontSize = '0.95em';

      // Insert after the name
      profileInfo.insertBefore(emailParagraph, profileInfo.querySelector('.profile-meta'));
    }
  }
});