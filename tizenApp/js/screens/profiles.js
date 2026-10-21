var __defProp = Object.defineProperty;
var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __spreadValues = (a, b) => {
  for (var prop in b || (b = {}))
    if (__hasOwnProp.call(b, prop))
      __defNormalProp(a, prop, b[prop]);
  if (__getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(b)) {
      if (__propIsEnum.call(b, prop))
        __defNormalProp(a, prop, b[prop]);
    }
  return a;
};
const profileAvatarCategories = ["anime", "girls", "boys", "couples", "server-icons"];
const legacyProfileAvatars = /* @__PURE__ */ new Set(["film", "rocket", "star", "panda", "fox", "frog", "music", "ball"]);
function profileAvatarValue(profile) {
  return (profile == null ? void 0 : profile.avatar) || (profile == null ? void 0 : profile.avatarId) || "piccdn:anime:0";
}
function renderProfiles() {
  var _a;
  if (state.profiles.length) {
    showProfileSwitcher();
    return;
  }
  state.page = "profiles";
  navState("");
  const avatar = "piccdn:anime:0";
  main.innerHTML = '<section class="profile-setup-stage"><form class="profile-editor-dialog profile-setup-card" id="profile-form">\n        <h1>Who\'s watching?</h1><p class="muted">Create a profile to keep favorites and viewing progress separate.</p>\n        <input type="hidden" name="avatar" value="'.concat(avatar, '">\n        <button class="profile-avatar-picker-button focusable" type="button" data-action="choose-profile-avatar" data-form-id="profile-form" aria-label="Choose a profile picture">\n            ').concat(profileAvatarMarkup(avatar, "profile-avatar-preview"), '<span>Change picture</span>\n        </button>\n        <label class="setting-control">Profile name<input class="text-input focusable" name="nickname" maxlength="24" autocomplete="off" required></label>\n        <p class="profile-editor-hint">Tap the profile picture to choose another avatar.</p>\n        <p class="profile-editor-error" data-profile-error role="alert" hidden></p>\n        <div class="profile-editor-actions"><button class="action-button primary focusable" type="submit">OK</button></div>\n    </form></section>');
  (_a = main.querySelector('[name="nickname"]')) == null ? void 0 : _a.focus();
}
function showProfileSwitcher() {
  if (!state.profiles.length) {
    renderProfiles();
    return;
  }
  let overlay = document.querySelector(".profile-switcher-overlay");
  if (!overlay) {
    overlay = document.createElement("div");
    overlay.className = "profile-switcher-overlay";
    overlay.setAttribute("role", "presentation");
    overlay.returnFocus = document.activeElement;
    overlay.addEventListener("click", (event) => {
      var _a;
      if (event.target === overlay) {
        overlay.remove();
        (_a = overlay.returnFocus) == null ? void 0 : _a.focus();
      }
    });
    document.body.append(overlay);
  }
  renderProfileSwitcher(overlay);
}
function renderProfileSwitcher(overlay = document.querySelector(".profile-switcher-overlay")) {
  var _a;
  if (!overlay) return;
  const active = activeProfile();
  const cards = state.profiles.map((profile) => {
    const selected = profile.id === state.activeProfileId;
    return '<button class="profile-switcher-choice focusable'.concat(selected ? " selected" : "", '" data-action="select-profile" data-id="').concat(escapeHtml(profile.id), '" aria-pressed="').concat(selected, '">\n            <span class="profile-switcher-avatar-wrap">').concat(profileAvatarMarkup(profileAvatarValue(profile), "profile-switcher-avatar")).concat(selected ? '<span class="profile-switcher-check" aria-label="Active profile">\u2713</span>' : "", '</span>\n            <span class="profile-switcher-name">').concat(escapeHtml(profile.nickname), "</span>\n        </button>");
  }).join("");
  overlay.innerHTML = '<section class="profile-switcher-dialog" role="dialog" aria-modal="true" aria-labelledby="profile-switcher-title">\n        <h2 id="profile-switcher-title">Who\'s watching?</h2>\n        <div class="profile-switcher-list">'.concat(cards).concat(state.profiles.length < 4 ? '<button class="profile-switcher-choice profile-switcher-add focusable" data-action="new-profile"><span class="profile-switcher-avatar-wrap"><span class="profile-switcher-add-icon">\uFF0B</span></span><span class="profile-switcher-name">Add</span></button>' : "", "</div>\n        ").concat(active ? '<div class="profile-switcher-actions">\n            <button class="action-button outlined focusable" data-action="edit-profile" data-id="'.concat(escapeHtml(active.id), '">\u270E Edit</button>\n            <button class="action-button focusable" data-action="remove-profile" data-id="').concat(escapeHtml(active.id), '">Remove</button>\n        </div>') : "", '\n        <p class="profile-editor-error" data-profile-error role="alert" hidden></p>\n        <div class="profile-switcher-footer"><button class="action-button primary focusable" data-action="close-profile-switcher">Done</button></div>\n    </section>');
  (_a = overlay.querySelector('[aria-pressed="true"]')) == null ? void 0 : _a.focus();
}
function showProfileEditor(profile = null) {
  var _a;
  if (!profile && state.profiles.length >= 4) {
    toastMessage("You can create up to 4 profiles.");
    return;
  }
  const avatar = profileAvatarValue(profile);
  const overlay = document.createElement("div");
  overlay.className = "profile-editor-overlay";
  overlay.setAttribute("role", "presentation");
  overlay.returnFocus = document.activeElement;
  overlay.innerHTML = '<form class="profile-editor-dialog" id="profile-editor-form" data-profile-id="'.concat(escapeHtml((profile == null ? void 0 : profile.id) || ""), '" aria-labelledby="profile-editor-title">\n        <h2 id="profile-editor-title">').concat(profile ? "Edit profile" : "Create profile", '</h2>\n        <p class="muted">').concat(profile ? "Update this profile's name or picture." : "Give this profile a name and picture.", '</p>\n        <input type="hidden" name="avatar" value="').concat(escapeHtml(avatar), '">\n        <button class="profile-avatar-picker-button focusable" type="button" data-action="choose-profile-avatar" data-form-id="profile-editor-form" aria-label="Choose a profile picture">\n            ').concat(profileAvatarMarkup(avatar, "profile-avatar-preview"), '<span>Change picture</span>\n        </button>\n        <label class="setting-control">Profile name<input class="text-input focusable" name="nickname" maxlength="24" autocomplete="off" required value="').concat(escapeHtml((profile == null ? void 0 : profile.nickname) || ""), '"></label>\n        <p class="profile-editor-hint">Tap the profile picture to choose another avatar.</p>\n        <p class="profile-editor-error" data-profile-error role="alert" hidden></p>\n        <div class="profile-editor-actions"><button class="action-button primary focusable" type="submit">OK</button><button class="action-button focusable" type="button" data-action="close-profile-editor">Cancel</button></div>\n    </form>');
  overlay.addEventListener("click", (event) => {
    var _a2;
    if (event.target === overlay) {
      overlay.remove();
      (_a2 = overlay.returnFocus) == null ? void 0 : _a2.focus();
    }
  });
  document.body.append(overlay);
  (_a = overlay.querySelector('[name="nickname"]')) == null ? void 0 : _a.focus();
}
function showProfileAvatarPicker(formId, selectedAvatar) {
  const current = /^piccdn:(anime|girls|boys|couples|server-icons):(\d{1,3})$/.exec(selectedAvatar);
  let category = (current == null ? void 0 : current[1]) || profileAvatarCategories[0];
  let selectedIndex = current ? Number(current[2]) : 0;
  let visibleCount = Math.min(1e3, Math.max(48, selectedIndex + 1));
  const overlay = document.createElement("div");
  overlay.className = "profile-editor-overlay profile-avatar-picker-overlay";
  overlay.setAttribute("role", "presentation");
  overlay.returnFocus = document.activeElement;
  overlay.innerHTML = '<section class="profile-avatar-picker-dialog" role="dialog" aria-modal="true" aria-labelledby="avatar-picker-title">\n        <h2 id="avatar-picker-title">Choose a profile picture</h2>\n        <p class="muted">Pick an image for your profile.</p>\n        <div class="profile-avatar-categories" role="group" aria-label="Avatar category">\n            '.concat(profileAvatarCategories.map((name) => '<button class="action-button focusable'.concat(name === category ? " selected" : "", '" type="button" data-category="').concat(name, '" aria-pressed="').concat(name === category, '">').concat(name.replace("-", " ").replace(/\b\w/g, (letter) => letter.toUpperCase()), "</button>")).join(""), '\n        </div>\n        <div class="profile-avatar-grid" role="listbox" aria-label="Profile pictures"></div>\n        <div class="profile-avatar-picker-footer"><span class="muted" data-avatar-count></span><div><button class="action-button focusable" type="button" data-action="cancel-avatar-picker">Cancel</button><button class="action-button primary focusable" type="button" data-action="cancel-avatar-picker">Done</button></div></div>\n    </section>');
  document.body.append(overlay);
  const grid = overlay.querySelector(".profile-avatar-grid");
  const countLabel = overlay.querySelector("[data-avatar-count]");
  const renderOptions = (append = false) => {
    var _a;
    const start = append ? grid.childElementCount : 0;
    if (!append) grid.innerHTML = "";
    for (let index = start; index < visibleCount; index += 1) {
      const value = "piccdn:".concat(category, ":").concat(index);
      const option = document.createElement("button");
      option.type = "button";
      option.className = "profile-avatar-option focusable".concat(value === selectedAvatar ? " selected" : "");
      option.setAttribute("role", "option");
      option.setAttribute("aria-label", "".concat(category.replace("-", " "), " avatar ").concat(index + 1));
      option.setAttribute("aria-selected", String(value === selectedAvatar));
      option.id = "profile-avatar-option-".concat(index);
      option.innerHTML = '<img src="https://raw.githubusercontent.com/itsfizys/piccdn/main/'.concat(category, "/").concat(index, '.jpg" alt="" loading="lazy">');
      option.addEventListener("click", () => {
        const form = document.getElementById(formId);
        if (!form) {
          overlay.remove();
          return;
        }
        const avatarInput = form.elements.namedItem("avatar");
        if (avatarInput) avatarInput.value = value;
        const preview = form.querySelector(".profile-avatar-picker-button");
        if (preview) preview.innerHTML = "".concat(profileAvatarMarkup(value, "profile-avatar-preview"), "<span>Change picture</span>");
        overlay.remove();
        preview == null ? void 0 : preview.focus();
      });
      grid.append(option);
    }
    countLabel.textContent = "Images from piccdn \xB7 ".concat(Math.min(visibleCount, 1e3), " of 1,000");
    if (!append) {
      const focusIndex = (current == null ? void 0 : current[1]) === category ? selectedIndex : 0;
      (_a = overlay.querySelector("#profile-avatar-option-".concat(focusIndex))) == null ? void 0 : _a.focus();
    }
  };
  const renderCategory = (nextCategory) => {
    category = nextCategory;
    selectedIndex = (current == null ? void 0 : current[1]) === category ? Number(current[2]) : 0;
    visibleCount = Math.min(1e3, Math.max(48, selectedIndex + 1));
    overlay.querySelectorAll("[data-category]").forEach((button) => {
      const selected = button.dataset.category === category;
      button.classList.toggle("selected", selected);
      button.setAttribute("aria-pressed", String(selected));
    });
    renderOptions();
  };
  overlay.querySelectorAll("[data-category]").forEach((button) => {
    button.addEventListener("click", () => renderCategory(button.dataset.category));
  });
  grid.addEventListener("scroll", () => {
    if (visibleCount < 1e3 && grid.scrollTop + grid.clientHeight >= grid.scrollHeight - 100) {
      visibleCount = Math.min(1e3, visibleCount + 48);
      renderOptions(true);
    }
  });
  overlay.querySelectorAll('[data-action="cancel-avatar-picker"]').forEach((button) => {
    button.addEventListener("click", () => {
      var _a;
      overlay.remove();
      (_a = overlay.returnFocus) == null ? void 0 : _a.focus();
    });
  });
  overlay.addEventListener("click", (event) => {
    var _a;
    if (event.target === overlay) {
      overlay.remove();
      (_a = overlay.returnFocus) == null ? void 0 : _a.focus();
    }
  });
  renderOptions();
}
function saveProfile(nicknameValue, avatarValue, profileId = "") {
  const nickname = String(nicknameValue || "").trim();
  const avatar = String(avatarValue || "");
  if (!nickname) throw new Error("Enter a profile name.");
  if (nickname.length > 24) throw new Error("Profile names can be up to 24 characters.");
  if (!isValidProfileAvatar(avatar)) throw new Error("Choose a valid profile picture.");
  if (profileId) {
    const profile = state.profiles.find((item) => item.id === profileId);
    if (!profile) throw new Error("This profile no longer exists.");
    profile.nickname = nickname;
    profile.avatar = avatar;
  } else {
    if (state.profiles.length >= 4) throw new Error("You can create up to 4 profiles.");
    const firstProfile = state.profiles.length === 0;
    const profile = {
      id: "profile-".concat(Date.now(), "-").concat(Math.random().toString(36).slice(2, 7)),
      nickname,
      avatar
    };
    state.profiles.push(profile);
    state.activeProfileId = profile.id;
    localStorage.setItem("echo.activeProfileId", profile.id);
    if (firstProfile) {
      state.favoriteIds = readJson("echo.favoriteIds", state.favoriteIds);
      state.favoriteTitles = readJson("echo.favoriteTitles", state.favoriteTitles);
      state.progress[profileKey("watchProgress")] = readJson(
        profileKey("watchProgress"),
        state.progress["echo.watchProgress"] || {}
      );
      state.preferences = __spreadValues(__spreadValues({}, preferenceDefaults), readJson("echo.preferences", state.preferences));
      saveState(profileKey("favoriteIds"), state.favoriteIds);
      saveState(profileKey("favoriteTitles"), state.favoriteTitles);
      saveState(profileKey("watchProgress"), state.progress[profileKey("watchProgress")]);
    } else {
      state.favoriteIds = [];
      state.favoriteTitles = {};
      state.progress[profileKey("watchProgress")] = {};
      state.preferences = __spreadValues({}, preferenceDefaults);
    }
    saveProfilePreferences();
  }
  saveState("echo.profiles", state.profiles);
  applyPreferences();
  return true;
}
function activateProfile(profileId) {
  if (!state.profiles.some((profile) => profile.id === profileId)) {
    throw new Error("This profile no longer exists.");
  }
  state.activeProfileId = profileId;
  localStorage.setItem("echo.activeProfileId", profileId);
  state.favoriteIds = readJson(profileKey("favoriteIds"), []);
  state.favoriteTitles = readJson(profileKey("favoriteTitles"), {});
  state.progress[profileKey("watchProgress")] = readJson(profileKey("watchProgress"), {});
  state.preferences = __spreadValues(__spreadValues({}, preferenceDefaults), readJson(profileKey("preferences"), {}));
  applyPreferences();
}
function isValidProfileAvatar(value) {
  if (legacyProfileAvatars.has(value)) return true;
  const match = /^piccdn:(anime|girls|boys|couples|server-icons):(\d{1,3})$/.exec(value);
  return Boolean(match && Number(match[2]) <= 999);
}
function showRemoveProfileDialog(profileId) {
  var _a;
  const profile = state.profiles.find((item) => item.id === profileId);
  if (!profile) return;
  const overlay = document.createElement("div");
  overlay.className = "profile-editor-overlay profile-remove-overlay";
  overlay.setAttribute("role", "presentation");
  overlay.returnFocus = document.activeElement;
  overlay.innerHTML = '<section class="profile-editor-dialog" role="alertdialog" aria-modal="true" aria-labelledby="remove-profile-title">\n        <h2 id="remove-profile-title">Remove '.concat(escapeHtml(profile.nickname), '?</h2>\n        <p class="muted">This removes this profile and its saved settings, favorites, and watch progress from this device.</p>\n        <p class="profile-editor-error" data-profile-error role="alert" hidden></p>\n        <div class="profile-editor-actions"><button class="action-button primary focusable" data-action="confirm-remove-profile" data-id="').concat(escapeHtml(profileId), '">Remove</button>\n        <button class="action-button focusable" data-action="close-profile-editor">Cancel</button></div></section>');
  overlay.addEventListener("click", (event) => {
    var _a2;
    if (event.target === overlay) {
      overlay.remove();
      (_a2 = overlay.returnFocus) == null ? void 0 : _a2.focus();
    }
  });
  document.body.append(overlay);
  (_a = overlay.querySelector('[data-action="confirm-remove-profile"]')) == null ? void 0 : _a.focus();
}
function removeProfile(profileId) {
  var _a;
  if (!state.profiles.some((profile) => profile.id === profileId)) {
    throw new Error("This profile no longer exists.");
  }
  state.profiles = state.profiles.filter((profile) => profile.id !== profileId);
  const profileStoragePrefix = "echo.profile.".concat(profileId, ".");
  for (let index = localStorage.length - 1; index >= 0; index -= 1) {
    const key = localStorage.key(index);
    if (key == null ? void 0 : key.startsWith(profileStoragePrefix)) localStorage.removeItem(key);
  }
  delete state.progress["".concat(profileStoragePrefix, "watchProgress")];
  saveState("echo.profiles", state.profiles);
  if (state.activeProfileId === profileId) {
    if (state.profiles.length) {
      activateProfile(state.profiles[0].id);
    } else {
      state.activeProfileId = "";
      localStorage.removeItem("echo.activeProfileId");
      state.favoriteIds = [];
      state.favoriteTitles = {};
      state.preferences = __spreadValues({}, preferenceDefaults);
      applyPreferences();
    }
  }
  if (!state.profiles.length) {
    (_a = document.querySelector(".profile-switcher-overlay")) == null ? void 0 : _a.remove();
    renderProfiles();
  } else {
    renderProfileSwitcher();
    if (state.page === "home") renderHome();
  }
}
