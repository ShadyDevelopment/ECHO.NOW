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
var _a, _b, _c;
function colorWithAlpha(color, alpha) {
  const match = /^#([0-9a-f]{6})$/i.exec(color);
  if (!match) return "rgba(0,0,0,".concat(alpha, ")");
  const value = Number.parseInt(match[1], 16);
  return "rgba(".concat(value >> 16, ",").concat(value >> 8 & 255, ",").concat(value & 255, ",").concat(alpha, ")");
}
function applyPreferences() {
  var _a2, _b2, _c2, _d, _e, _f;
  if (typeof applyConfiguredUiColors === "function") applyConfiguredUiColors();
  const preferences = state.preferences;
  document.documentElement.style.setProperty("--accent", preferences.primaryColor);
  document.documentElement.style.setProperty("--accent-soft", colorWithAlpha(preferences.primaryColor, 0.28));
  document.documentElement.style.setProperty("--accent-glow", colorWithAlpha(preferences.primaryColor, 0.38));
  document.documentElement.style.setProperty("--accent-secondary", preferences.secondaryColor || preferenceDefaults.secondaryColor);
  document.documentElement.style.setProperty("--accent-tertiary", preferences.tertiaryColor || preferenceDefaults.tertiaryColor);
  document.documentElement.style.setProperty("--button-fill", preferences.buttonFillColor || preferences.primaryColor);
  document.documentElement.style.setProperty("--button-text", preferences.buttonTextColor || "#ffffff");
  document.documentElement.style.setProperty("--button-border", preferences.buttonBorderColor || preferences.primaryColor);
  const resolvedTheme = preferences.theme.toLowerCase() === "system" ? ((_a2 = window.matchMedia) == null ? void 0 : _a2.call(window, "(prefers-color-scheme: light)").matches) ? "light" : "dark" : preferences.theme.toLowerCase();
  document.documentElement.dataset.theme = resolvedTheme;
  document.documentElement.style.setProperty("--button-radius", preferences.buttonShape === "Pill" ? "999px" : preferences.buttonShape === "Square" ? "2px" : "10px");
  document.title = appName;
  const brandName = document.querySelector(".brand-name");
  const brandWordmark = document.querySelector(".brand-wordmark");
  if (brandName) {
    brandName.textContent = appName;
    brandName.hidden = appName.toUpperCase() === "ECHO NOW";
  }
  if (brandWordmark) brandWordmark.hidden = appName.toUpperCase() !== "ECHO NOW";
  document.documentElement.style.setProperty("--subtitle-size", "".concat(Math.min(36, Math.max(14, Number(preferences.subtitleSize) || 22)), "px"));
  (_b2 = document.getElementById("subtitle-style")) == null ? void 0 : _b2.remove();
  const subtitleStyle = document.createElement("style");
  subtitleStyle.id = "subtitle-style";
  const transparency = Math.max(0, Math.min(100, Number(preferences.subtitleBoxTransparency) || 0));
  const subtitleBackground = preferences.subtitleBoxEnabled ? colorWithAlpha(preferences.subtitleBackground, (100 - transparency) / 100) : "transparent";
  document.documentElement.style.setProperty("--subtitle-text", preferences.subtitleText || "#fff");
  document.documentElement.style.setProperty("--subtitle-background", subtitleBackground);
  document.documentElement.style.setProperty("--subtitle-font", preferences.subtitleFont || "Arial");
  document.documentElement.style.setProperty(
    "--subtitle-position",
    "".concat(Math.max(0, Math.min(25, Number(preferences.subtitleVerticalPosition) || 0)), "%")
  );
  subtitleStyle.textContent = "video::cue { font-size: var(--subtitle-size); font-family: ".concat(preferences.subtitleFont || "Arial", ", sans-serif; background-color: ").concat(subtitleBackground, "; color: ").concat(preferences.subtitleText || "#fff", "; }");
  document.head.appendChild(subtitleStyle);
  const profileButton = document.querySelector(".profile-button");
  (_c2 = document.querySelector(".topbar")) == null ? void 0 : _c2.classList.toggle("compact", preferences.navigationBarCollapsed !== false);
  (_d = document.querySelector(".topbar")) == null ? void 0 : _d.classList.toggle("show-labels", preferences.showMenuLabels === true);
  if (profileButton) {
    const profileIcon = profileButton.querySelector(".profile-nav-icon");
    const profileName = profileButton.querySelector(".profile-name");
    if (profileIcon) profileIcon.innerHTML = profileAvatarMarkup(((_e = activeProfile()) == null ? void 0 : _e.avatar) || "\u{1F3AC}", "profile-avatar-nav");
    if (profileName) profileName.textContent = ((_f = activeProfile()) == null ? void 0 : _f.nickname) || "Profile";
  }
  renderClock();
  applyPlayerSubtitleStyle();
}
(_c = (_a = window.matchMedia) == null ? void 0 : (_b = _a.call(window, "(prefers-color-scheme: light)")).addEventListener) == null ? void 0 : _c.call(_b, "change", () => {
  if (state.preferences.theme.toLowerCase() === "system") applyPreferences();
});
function renderClock() {
  var _a2;
  const clock = document.getElementById("connection-status");
  if (!clock) return;
  const visible = state.page === "home" ? state.preferences.showClockOnHome : state.preferences.showClockOnOtherPages;
  if (!visible) {
    clock.textContent = "";
    return;
  }
  const prefs = state.preferences;
  if (prefs.clockMode === "MANUAL" && prefs.manualClockValue) {
    clock.textContent = prefs.manualClockValue;
  } else {
    const options = {
      weekday: "short",
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
      hour12: !prefs.timeFormat24Hour
    };
    if (prefs.timeZoneId && prefs.timeZoneId !== "AUTO") options.timeZone = prefs.timeZoneId;
    try {
      const formatter = new Intl.DateTimeFormat(prefs.languageTag && prefs.languageTag !== "SYSTEM" ? prefs.languageTag : void 0, options);
      const parts = Object.fromEntries(formatter.formatToParts(/* @__PURE__ */ new Date()).map((part) => [part.type, part.value]));
      const values = {
        EEEE: parts.weekday,
        EEE: parts.weekday,
        MMMM: parts.month,
        MMM: parts.month,
        MM: parts.month,
        yyyy: parts.year,
        yy: (_a2 = parts.year) == null ? void 0 : _a2.slice(-2),
        dd: parts.day,
        d: String(Number(parts.day)),
        HH: parts.hour,
        H: String(Number(parts.hour)),
        hh: parts.hour,
        h: String(Number(parts.hour)),
        mm: parts.minute,
        a: parts.dayPeriod || ""
      };
      const pattern = prefs.clockPattern || preferenceDefaults.clockPattern;
      clock.textContent = pattern.replace(/EEEE|EEE|MMMM|MMM|yyyy|yy|MM|dd|HH|hh|H|h|mm|d|a/g, (token) => {
        var _a3;
        return (_a3 = values[token]) != null ? _a3 : token;
      });
    } catch (error) {
      console.warn("Configured clock locale or time zone is invalid:", error.message);
      const fallbackOptions = __spreadValues({}, options);
      delete fallbackOptions.timeZone;
      clock.textContent = new Intl.DateTimeFormat(void 0, fallbackOptions).format(/* @__PURE__ */ new Date());
    }
  }
  clock.style.color = prefs.clockColor || "#ffffff";
  clock.style.fontSize = "".concat(Math.max(10, Math.min(28, Number(prefs.clockSize) || 14)), "px");
}
function activeProfile() {
  return state.profiles.find((profile) => profile.id === state.activeProfileId) || state.profiles[0];
}
function saveProfilePreferences() {
  saveState(state.activeProfileId ? profileKey("preferences") : "echo.preferences", state.preferences);
}
function profileAvatarMarkup(avatar, className = "profile-avatar") {
  const value = String(avatar || "\u{1F3AC}");
  const match = /^piccdn:(anime|girls|boys|couples|server-icons):(\d{1,3})$/.exec(value);
  if (!match) return '<span class="'.concat(className, '">').concat(escapeHtml(value), "</span>");
  const url = "https://raw.githubusercontent.com/itsfizys/piccdn/main/".concat(match[1], "/").concat(match[2], ".jpg");
  return '<span class="'.concat(className, '"><img src="').concat(url, '" alt="Profile picture" loading="lazy"></span>');
}
function profileKey(key) {
  return "echo.profile.".concat(state.activeProfileId, ".").concat(key);
}
function progressFor(media, episode) {
  if (!media) return null;
  const mediaType = media.media_type || media.type || (media.name ? "tv" : "movie");
  const allProgress = state.progress[profileKey("watchProgress")] || {};
  if (episode) {
    const entryId = "".concat(mediaType, ":").concat(media.id, ":").concat(episode.season, ":").concat(episode.episode);
    return allProgress[entryId] || null;
  }
  const directId = "".concat(mediaType, ":").concat(media.id);
  if (allProgress[directId]) return allProgress[directId];
  const prefix = "".concat(mediaType, ":").concat(media.id, ":");
  let latest = null;
  for (const [key, entry] of Object.entries(allProgress)) {
    if (key.startsWith(prefix)) {
      if (!latest || (entry.updatedAt || 0) > (latest.updatedAt || 0)) {
        latest = entry;
      }
    }
  }
  return latest;
}
function escapeHtml(value) {
  return String(value != null ? value : "").replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[character]);
}
function storedCredential() {
  var _a2, _b2;
  return ((_a2 = appBuildConfig.tmdbReadAccessToken) == null ? void 0 : _a2.trim()) || ((_b2 = appBuildConfig.tmdbApiKey) == null ? void 0 : _b2.trim()) || "";
}
function configuredSubdlApiKey() {
  var _a2;
  return ((_a2 = appBuildConfig.subdlApiKey) == null ? void 0 : _a2.trim()) || "";
}
function configuredWebsite() {
  try {
    const url = new URL(appBuildConfig.appWeb || "");
    return url.protocol === "https:" || url.protocol === "http:" ? url.href : "";
  } catch (e) {
    return "";
  }
}
async function tmdb(path, parameters = {}) {
  const credential = storedCredential();
  if (!credential) throw new Error("TMDB credentials are missing from the project config.cfg.");
  const url = new URL("".concat(API_ROOT).concat(path));
  const preferredLanguage = state.preferences.languageTag === "SYSTEM" ? navigator.language || "en-US" : state.preferences.languageTag;
  url.searchParams.set("language", preferredLanguage || "en-US");
  for (const [key, value] of Object.entries(parameters)) url.searchParams.set(key, value);
  const headers = {};
  if (credential.startsWith("eyJ")) headers.Authorization = "Bearer ".concat(credential);
  else url.searchParams.set("api_key", credential);
  const response = await fetch(url.toString(), { headers });
  if (!response.ok) {
    const detail = await response.json().catch(() => ({}));
    throw new Error(detail.status_message || "TMDB request failed (".concat(response.status, ")."));
  }
  return response.json();
}
window.tmdb = tmdb;
function toastMessage(message) {
  toast.textContent = message;
  toast.classList.add("visible");
  clearTimeout(toastMessage.timer);
  toastMessage.timer = setTimeout(() => toast.classList.remove("visible"), 3500);
}
function imageUrl(path, size) {
  if (!path) return "";
  const resolvedSize = size || (window.TIZEN_DEVICE && typeof window.TIZEN_DEVICE.getOptimizedPosterSize === "function" ? window.TIZEN_DEVICE.getOptimizedPosterSize() : "w500");
  return "".concat(IMAGE_ROOT, "/").concat(resolvedSize).concat(path);
}
function backdropUrl(path, size) {
  if (!path) return "";
  const resolvedSize = size || (window.TIZEN_DEVICE && typeof window.TIZEN_DEVICE.getOptimizedBackdropSize === "function" ? window.TIZEN_DEVICE.getOptimizedBackdropSize() : "w1280");
  return "".concat(IMAGE_ROOT, "/").concat(resolvedSize).concat(path);
}
