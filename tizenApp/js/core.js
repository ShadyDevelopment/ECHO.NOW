"use strict";
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
const API_ROOT = "https://api.themoviedb.org/3";
const IMAGE_ROOT = "https://image.tmdb.org/t/p";
const appBuildConfig = window.ECHO_BUILD_CONFIG || {};
const appName = appBuildConfig.appName || "ECHO NOW";
const appVersion = appBuildConfig.appVersion || "0.2.0";
const hasConfiguredTmdbCredentials = Boolean(((_a = appBuildConfig.tmdbReadAccessToken) == null ? void 0 : _a.trim()) || ((_b = appBuildConfig.tmdbApiKey) == null ? void 0 : _b.trim()));
const hasConfiguredSubdlKey = Boolean((_c = appBuildConfig.subdlApiKey) == null ? void 0 : _c.trim());
const credentialsLocked = true;
localStorage.removeItem("echo.tmdbCredential");
localStorage.removeItem("echo.subdlApiKey");
const main = document.getElementById("main");
const toast = document.getElementById("toast");
const buildUiColors = appBuildConfig.uiColors || {};
function applyConfiguredUiColors() {
  const root = document.documentElement;
  if (!root) return;
  const colors = window.ECHO_BUILD_CONFIG && window.ECHO_BUILD_CONFIG.uiColors || {};
  if (colors.cardBackground) root.style.setProperty("--card-bg", colors.cardBackground);
  if (colors.surfaceColor) root.style.setProperty("--surface", colors.surfaceColor);
  if (colors.menuBarBackground) root.style.setProperty("--menu-bar-bg", colors.menuBarBackground);
  if (colors.pageBackgroundStart) root.style.setProperty("--page-bg-start", colors.pageBackgroundStart);
  if (colors.pageBackgroundEnd) root.style.setProperty("--page-bg-end", colors.pageBackgroundEnd);
  if (colors.accentPrimary) root.style.setProperty("--accent", colors.accentPrimary);
  if (colors.accentSecondary) root.style.setProperty("--accent-secondary", colors.accentSecondary);
  if (colors.accentTertiary) root.style.setProperty("--accent-tertiary", colors.accentTertiary);
  if (colors.accentFocus) root.style.setProperty("--accent-focus", colors.accentFocus);
  if (colors.textPrimary) root.style.setProperty("--text-color", colors.textPrimary);
  if (colors.textMuted) root.style.setProperty("--muted", colors.textMuted);
  if (colors.dropdownBackground) root.style.setProperty("--dropdown-bg", colors.dropdownBackground);
}
applyConfiguredUiColors();
const preferenceDefaults = {
  theme: "Dark",
  primaryColor: buildUiColors.accentPrimary || "#607d8b",
  secondaryColor: buildUiColors.accentSecondary || "#00838f",
  tertiaryColor: buildUiColors.accentTertiary || "#31a7a0",
  buttonFillColor: "#37474f",
  buttonTextColor: "#ffffff",
  buttonBorderColor: "#607d8b",
  buttonShape: "Rounded",
  interfaceMode: "TV",
  subtitleLanguage: "EN",
  subtitleSize: 22,
  subtitleOffset: 0,
  subtitleVerticalPosition: 0,
  subtitleBackground: "#000000",
  subtitleText: "#ffffff",
  subtitleBoxEnabled: true,
  subtitleBoxTransparency: 20,
  subtitleFont: "Arial",
  autoApplySubtitles: true,
  subtitleCountry: "US",
  showClockOnHome: false,
  showClockOnOtherPages: false,
  timeFormat24Hour: false,
  clockMode: "AUTO",
  manualClockValue: "",
  timeZoneId: "AUTO",
  clockPattern: "EEE, MMM d \xB7 h:mm a",
  clockColor: "#ffffff",
  clockSize: 14,
  languageTag: "SYSTEM",
  showMenuLabels: true,
  navigationBarCollapsed: false,
  deviceName: "ECHO NOW"
};
const readJson = (key, fallback) => {
  var _a2;
  try {
    return (_a2 = JSON.parse(localStorage.getItem(key) || "null")) != null ? _a2 : fallback;
  } catch (e) {
    return fallback;
  }
};
const initialProfileId = localStorage.getItem("echo.activeProfileId") || "";
const storedPreferences = readJson(
  initialProfileId ? "echo.profile.".concat(initialProfileId, ".preferences") : "echo.preferences",
  readJson("echo.preferences", {})
);
const savedPreferences = storedPreferences && typeof storedPreferences === "object" && !Array.isArray(storedPreferences) ? storedPreferences : {};
if (savedPreferences.primaryColor === "#8a5cff") savedPreferences.primaryColor = preferenceDefaults.primaryColor;
if (localStorage.getItem("echo.androidVisualParity") !== "1" || localStorage.getItem("echo.webVisualParity") !== "2") {
  savedPreferences.primaryColor = preferenceDefaults.primaryColor;
  savedPreferences.secondaryColor = preferenceDefaults.secondaryColor;
  savedPreferences.tertiaryColor = preferenceDefaults.tertiaryColor;
  savedPreferences.showMenuLabels = true;
  savedPreferences.navigationBarCollapsed = false;
  localStorage.setItem("echo.preferences", JSON.stringify(savedPreferences));
  localStorage.setItem("echo.androidVisualParity", "1");
  localStorage.setItem("echo.webVisualParity", "2");
}
const state = {
  page: "home",
  previousPage: "home",
  homeData: null,
  homeLoading: false,
  homeError: "",
  mediaMetadataObserver: null,
  catalog: {
    movies: { items: [], page: 0, genre: "", sort: "Recommended", totalPages: 1, loading: false, requestId: 0, genres: null, genresLoading: false, error: null },
    series: { items: [], page: 0, genre: "", sort: "Recommended", totalPages: 1, loading: false, requestId: 0, genres: null, genresLoading: false, error: null }
  },
  currentTitle: null,
  currentDetails: null,
  searchResults: [],
  searchPage: 0,
  searchTotalPages: 0,
  searchLoading: false,
  searchLoadingMore: false,
  searchError: "",
  searchRequestId: 0,
  searchTimer: null,
  favoriteIds: readJson("echo.favoriteIds", []),
  favoriteTitles: readJson("echo.favoriteTitles", {}),
  progress: readJson("echo.watchProgress", {}),
  profiles: readJson("echo.profiles", []),
  activeProfileId: localStorage.getItem("echo.activeProfileId") || "",
  preferences: __spreadValues(__spreadValues({}, preferenceDefaults), savedPreferences),
  selectedSeason: 1,
  currentEpisode: null,
  playerTimer: null,
  playbackRequestId: 0,
  subtitleOptions: [],
  playerSources: [],
  playerSourceIndex: 0,
  failedPlayerSources: [],
  episodeCache: {},
  nextEpisode: null,
  episodeTiming: null,
  autoPlayNextEpisode: true,
  advancingToNextEpisode: false,
  upNextDismissed: false,
  subtitleDialogOpen: false,
  subtitleLoadRequestId: 0,
  subtitleCues: [],
  subtitleCueTrackIndex: -1,
  avplaySubtitlesEnabled: false,
  playerAspectRatio: 0,
  playerFullscreen: false,
  focusedSkipAction: ""
};
let activeHls = null;
let isAvplayActive = false;
function saveState(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
}
function applyDynamicUiScale() {
  const currentW = window.innerWidth || (document.documentElement ? document.documentElement.clientWidth : 1920);
  if (!currentW) return;
  const isTv = /(Tizen|SmartTV|webOS)/i.test(navigator.userAgent || "");
  if (!isTv && currentW !== 1920) {
    const meta = document.querySelector('meta[name="viewport"]');
    if (meta) {
      const scale = (currentW / 1920).toFixed(4);
      meta.setAttribute("content", "width=1920, initial-scale=" + scale + ", user-scalable=no");
    }
  }
}
window.addEventListener("resize", applyDynamicUiScale);
window.addEventListener("DOMContentLoaded", applyDynamicUiScale);
applyDynamicUiScale();
