var __defProp = Object.defineProperty;
var __defProps = Object.defineProperties;
var __getOwnPropDescs = Object.getOwnPropertyDescriptors;
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
var __spreadProps = (a, b) => __defProps(a, __getOwnPropDescs(b));
var _a;
function renderPage(page) {
  if (page === "home") renderHome();
  else if (page === "movies" || page === "series") renderCatalog(page);
  else if (page === "search") renderSearch();
  else if (page === "settings") renderSettings();
  else if (page === "profiles") renderProfiles();
  renderClock();
}
let activeDpadSelectMenu = null;
let activeEditControl = null;
let editControlInitialValue = "";
let lastFocusedControl = null;
let pendingFocusRestore = null;
let dpadSelectMenuSequence = 0;
function closeDpadSelectMenu(restoreFocus = true) {
  const active = activeDpadSelectMenu;
  if (!active) return;
  activeDpadSelectMenu = null;
  active.menu.remove();
  active.select.setAttribute("aria-expanded", "false");
  active.select.removeAttribute("aria-controls");
  if (restoreFocus && active.select.isConnected) {
    active.select.focus();
    scrollElementIntoViewIfNeeded(active.select);
  }
}
function focusDpadSelectOption(index) {
  const active = activeDpadSelectMenu;
  if (!active) return;
  const options = Array.from(active.select.options);
  let nextIndex = index;
  const direction = index >= active.index ? 1 : -1;
  while (nextIndex >= 0 && nextIndex < options.length && (options[nextIndex].disabled || options[nextIndex].parentElement.disabled || options[nextIndex].hidden)) {
    nextIndex += direction;
  }
  if (nextIndex < 0 || nextIndex >= options.length) return false;
  const option = active.menu.querySelector('[data-dpad-option-index="'.concat(nextIndex, '"]'));
  if (!option) return false;
  active.index = nextIndex;
  active.menu.querySelectorAll(".dpad-select-option").forEach((item) => {
    const selected = Number(item.dataset.dpadOptionIndex) === nextIndex;
    item.classList.toggle("selected", selected);
    item.setAttribute("aria-selected", String(selected));
  });
  option.focus();
  const optTop = option.offsetTop;
  const optBottom = optTop + option.offsetHeight;
  if (optTop < active.menu.scrollTop) {
    active.menu.scrollTop = optTop;
  } else if (optBottom > active.menu.scrollTop + active.menu.clientHeight) {
    active.menu.scrollTop = optBottom - active.menu.clientHeight;
  }
  return true;
}
function openDpadSelectMenu(select) {
  var _a2, _b, _c;
  if (activeDpadSelectMenu || !(select == null ? void 0 : select.isConnected)) return;
  const bounds = select.getBoundingClientRect();
  const menu = document.createElement("div");
  const id = "dpad-select-menu-".concat(select.id || ++dpadSelectMenuSequence);
  const maxHeight = Math.min(420, window.innerHeight * 0.58);
  const spaceBelow = Math.max(0, window.innerHeight - bounds.bottom - 12);
  const spaceAbove = Math.max(0, bounds.top - 12);
  const openAbove = spaceAbove > spaceBelow;
  const availableHeight = Math.max(120, Math.min(maxHeight, openAbove ? spaceAbove : spaceBelow));
  const menuWidth = Math.max(Math.min(bounds.width, window.innerWidth - 24), Math.min(240, window.innerWidth - 24));
  menu.id = id;
  menu.className = "dpad-select-menu";
  menu.setAttribute("role", "listbox");
  menu.setAttribute("aria-label", select.getAttribute("aria-label") || ((_c = (_b = (_a2 = select.labels) == null ? void 0 : _a2[0]) == null ? void 0 : _b.textContent) == null ? void 0 : _c.trim()) || "Options");
  menu.style.left = "".concat(Math.max(12, Math.min(bounds.left, window.innerWidth - menuWidth - 12)), "px");
  menu.style.width = "".concat(menuWidth, "px");
  menu.style.maxHeight = "".concat(availableHeight, "px");
  menu.style.top = openAbove ? "".concat(Math.max(12, bounds.top - availableHeight - 8), "px") : "".concat(Math.min(bounds.bottom + 8, window.innerHeight - availableHeight - 12), "px");
  Array.from(select.options).forEach((option, index) => {
    if (option.hidden) return;
    const item = document.createElement("button");
    item.type = "button";
    item.className = "dpad-select-option focusable";
    item.setAttribute("role", "option");
    item.dataset.dpadOptionIndex = String(index);
    item.setAttribute("aria-selected", String(index === select.selectedIndex));
    item.textContent = option.textContent;
    item.disabled = option.disabled || option.parentElement.disabled;
    if (index === select.selectedIndex) {
      item.classList.add("selected");
      item.classList.add("current-value");
    }
    menu.appendChild(item);
  });
  document.body.appendChild(menu);
  const selectedIndex = select.selectedIndex >= 0 ? select.selectedIndex : 0;
  activeDpadSelectMenu = { select, menu, index: selectedIndex };
  select.setAttribute("aria-expanded", "true");
  select.setAttribute("aria-controls", id);
  focusDpadSelectOption(selectedIndex);
}
function chooseDpadSelectOption(index) {
  const active = activeDpadSelectMenu;
  const option = active == null ? void 0 : active.select.options[index];
  if (!active || !option || option.disabled || option.parentElement.disabled || option.hidden) return;
  const changed = active.select.selectedIndex !== index;
  active.select.selectedIndex = index;
  closeDpadSelectMenu();
  if (changed) {
    active.select.dispatchEvent(new Event("input", { bubbles: true }));
    active.select.dispatchEvent(new Event("change", { bubbles: true }));
  }
}
function finishControlEditing(commit = true) {
  const control = activeEditControl;
  if (!control) return;
  activeEditControl = null;
  control.classList.remove("editing");
  const value = control.isContentEditable ? control.textContent : control.value;
  if (commit && control.isConnected && value !== editControlInitialValue) {
    control.dispatchEvent(new Event(control.isContentEditable ? "input" : "change", { bubbles: true }));
  }
  editControlInitialValue = "";
}
function startControlEditing(control) {
  if (!control || control.disabled || control.readOnly) return;
  if (activeEditControl !== control) finishControlEditing();
  activeEditControl = control;
  editControlInitialValue = control.isContentEditable ? control.textContent : control.value;
  control.classList.add("editing");
}
function scrollElementIntoViewIfNeeded(element) {
  if (!element || typeof element.getBoundingClientRect !== "function") return;
  const hRow = element.closest(".card-row, .cast-row, .profile-switcher-list, .profile-avatar-categories, .subtitle-search-results");
  if (hRow) {
    const elRect = element.getBoundingClientRect();
    const rowRect = hRow.getBoundingClientRect();
    const margin = 80;
    if (elRect.left < rowRect.left + margin) {
      hRow.scrollLeft -= Math.round(rowRect.left + margin - elRect.left);
    } else if (elRect.right > rowRect.right - margin) {
      hRow.scrollLeft += Math.round(elRect.right - rowRect.right + margin);
    }
  }
  const mainEl = document.getElementById("main");
  if (mainEl && mainEl.contains(element)) {
    const elRect = element.getBoundingClientRect();
    const mainRect = mainEl.getBoundingClientRect();
    const vMargin = 90;
    if (elRect.top < mainRect.top + vMargin) {
      mainEl.scrollTop -= Math.round(mainRect.top + vMargin - elRect.top);
    } else if (elRect.bottom > mainRect.bottom - vMargin) {
      mainEl.scrollTop += Math.round(elRect.bottom - mainRect.bottom + vMargin);
    }
  }
}
function moveFocus(direction) {
  finishControlEditing();
  const target = nearestFocusable(direction);
  if (!target) return false;
  target.focus();
  scrollElementIntoViewIfNeeded(target);
  return true;
}
function isFocusableVisible(item) {
  if (!item.getClientRects().length || item.disabled || item.getAttribute("aria-disabled") === "true" || item.closest('[hidden], [aria-hidden="true"], [inert]') || getComputedStyle(item).visibility === "hidden") return false;
  const closedDetails = item.closest("details:not([open])");
  return !closedDetails || item === closedDetails.querySelector("summary");
}
function firstFocusable(container) {
  return Array.from((container == null ? void 0 : container.querySelectorAll(".focusable")) || []).find(isFocusableVisible) || null;
}
function isConfirmKey(keyOrEvent) {
  if (!keyOrEvent) return false;
  if (typeof keyOrEvent === "object") {
    const code = keyOrEvent.keyCode || keyOrEvent.which;
    if (code === 13) return true;
    const k = keyOrEvent.key;
    return ["Enter", " ", "Spacebar", "Accept", "Select", "OK"].includes(k);
  }
  return ["Enter", " ", "Spacebar", "Accept", "Select", "OK"].includes(keyOrEvent);
}
function isBackNavigationKey(event) {
  if (!event) return false;
  const code = event.keyCode || event.which;
  if (code === 10009 || event.key === "XF86Back") return true;
  if (["Escape", "Back", "BrowserBack", "GoBack"].includes(event.key)) return true;
  var _a2, _b;
  return (event.key === "Backspace" || code === 8) && !((_b = (_a2 = event.target).closest) == null ? void 0 : _b.call(_a2, "input, textarea, select, [contenteditable=true]"));
}
function setSettingsCategoryOpen(details, open) {
  var _a2;
  if (open) {
    main.querySelectorAll("details.settings-card[open]").forEach((other) => {
      var _a3;
      if (other === details) return;
      other.open = false;
      (_a3 = other.querySelector("summary")) == null ? void 0 : _a3.setAttribute("aria-expanded", "false");
    });
  }
  details.open = open;
  (_a2 = details.querySelector("summary")) == null ? void 0 : _a2.setAttribute("aria-expanded", String(open));
}
function activateSettingsCategory(summary, focusContent = false) {
  const details = summary.closest("details");
  if (!details) return;
  setSettingsCategoryOpen(details, true);
  if (focusContent) {
    const target = Array.from(details.querySelectorAll(".focusable")).find((item) => item !== summary && isFocusableVisible(item));
    if (target) {
      target.focus();
      scrollElementIntoViewIfNeeded(target);
    }
  }
}
function rememberFocusedControl(element) {
  var _a2;
  if (lastFocusedControl && !lastFocusedControl.element.isConnected && lastFocusedControl.page === state.page) {
    pendingFocusRestore = lastFocusedControl;
  }
  const focusable = (_a2 = element.closest) == null ? void 0 : _a2.call(element, ".focusable");
  if (!focusable || !main.contains(focusable)) return;
  const attributes = ["data-action", "data-id", "data-media-id", "data-index", "data-kind"].map((name) => [name, focusable.getAttribute(name)]).filter(([, value]) => value !== null);
  lastFocusedControl = {
    page: state.page,
    element: focusable,
    id: focusable.id,
    attributes,
    index: Array.from(main.querySelectorAll(".focusable")).indexOf(focusable)
  };
}
document.addEventListener("focusin", (event) => {
  if (activeEditControl && event.target !== activeEditControl) finishControlEditing();
  if (activeDpadSelectMenu && event.target !== activeDpadSelectMenu.select && !activeDpadSelectMenu.menu.contains(event.target)) closeDpadSelectMenu(false);
  rememberFocusedControl(event.target);
});
document.addEventListener("pointerdown", (event) => {
  var _a2, _b;
  if (activeDpadSelectMenu && event.target !== activeDpadSelectMenu.select && !activeDpadSelectMenu.menu.contains(event.target)) closeDpadSelectMenu(false);
  const control = (_b = (_a2 = event.target).closest) == null ? void 0 : _b.call(_a2, "input, textarea, [contenteditable=true]");
  if (control && !(control instanceof HTMLInputElement && ["button", "submit", "reset", "checkbox", "radio", "file", "color", "hidden", "image"].includes(control.type))) {
    startControlEditing(control);
  }
});
new MutationObserver(() => {
  if (activeDpadSelectMenu && !activeDpadSelectMenu.select.isConnected) closeDpadSelectMenu(false);
  const remembered = pendingFocusRestore || (lastFocusedControl && !lastFocusedControl.element.isConnected ? lastFocusedControl : null);
  if (!remembered || remembered.page !== state.page) return;
  const focusedAtMutation = document.activeElement;
  requestAnimationFrame(() => {
    if (remembered.page !== state.page || document.activeElement !== focusedAtMutation) return;
    const candidates = Array.from(main.querySelectorAll(".focusable")).filter(isFocusableVisible);
    let target = remembered.id ? document.getElementById(remembered.id) : null;
    if (!target && remembered.attributes.length) {
      target = candidates.find((item) => remembered.attributes.every(([name, value]) => item.getAttribute(name) === value));
    }
    target || (target = candidates[remembered.index] || null);
    if (target) {
      target.focus();
      scrollElementIntoViewIfNeeded(target);
    }
    pendingFocusRestore = null;
  });
}).observe(main, { childList: true, subtree: true });
document.addEventListener("submit", (event) => {
  var _a2;
  event.preventDefault();
  if (event.target.id === "search-form") {
    state.searchQuery = document.getElementById("search-query").value;
    state.searchFocusFirstAfterSubmit = true;
    clearTimeout(state.searchTimer);
    searchTitles(state.searchQuery);
  }
  if (event.target.id === "profile-form" || event.target.id === "profile-editor-form") {
    const data = new FormData(event.target);
    try {
      saveProfile(data.get("nickname"), String(data.get("avatar") || ""), event.target.dataset.profileId || "");
      if (event.target.id === "profile-form") {
        renderHome();
      } else {
        (_a2 = event.target.closest(".profile-editor-overlay")) == null ? void 0 : _a2.remove();
        if (state.page === "home") renderHome();
        else if (state.page === "settings") renderSettings();
        renderProfileSwitcher();
      }
    } catch (error) {
      const output = event.target.querySelector("[data-profile-error]");
      if (output) {
        output.textContent = error.message;
        output.hidden = false;
      } else {
        toastMessage(error.message);
      }
    }
  }
});
document.addEventListener("input", (event) => {
  if (event.target.id !== "pref-subtitle-position" && event.target.id !== "player-subtitle-position") return;
  state.preferences.subtitleVerticalPosition = Math.max(0, Math.min(25, Number(event.target.value) || 0));
  saveProfilePreferences();
  applyPreferences();
  const label = document.getElementById("player-subtitle-position-label");
  if (label) label.textContent = "".concat(state.preferences.subtitleVerticalPosition, "%");
});
document.addEventListener("input", (event) => {
  if (event.target.id !== "search-query") return;
  state.searchQuery = event.target.value;
  clearTimeout(state.searchTimer);
  state.searchFocusFirstAfterSubmit = false;
  const count = document.getElementById("search-result-count");
  if (count && state.searchQuery.trim()) count.textContent = "Searching…";
  state.searchTimer = setTimeout(() => searchTitles(state.searchQuery), 500);
});
document.addEventListener("input", (event) => {
  if (event.target.id !== "player-progress") return;
  const dur = playerDuration();
  if (dur > 0) playerSeekTo(Number(event.target.value) / 1e3 * dur);
});
document.addEventListener("change", (event) => {
  var _a2, _b;
  if (event.target.id === "season-select" && state.currentTitle) loadEpisodes(state.currentTitle.id, Number(event.target.value));
  if (event.target.id === "season-select") state.selectedSeason = Number(event.target.value);
  if (event.target.id === "catalog-genre") {
    const kind = state.page;
    state.catalog[kind].genre = event.target.value;
    loadCatalog(kind, true);
  }
  if (event.target.id === "catalog-sort") {
    const kind = state.page;
    state.catalog[kind].sort = event.target.value;
    loadCatalog(kind, true);
  }
  if (event.target.id === "pref-subtitle-country") {
    state.preferences.subtitleCountry = event.target.value;
    state.preferences.subtitleLanguage = ((_a2 = event.target.selectedOptions[0]) == null ? void 0 : _a2.dataset.language) || "EN";
    saveProfilePreferences();
    applyPreferences();
  }
  const preferenceControls = {
    "pref-theme": ["theme", "value"],
    "pref-color": ["primaryColor", "value"],
    "pref-secondary-color": ["secondaryColor", "value"],
    "pref-tertiary-color": ["tertiaryColor", "value"],
    "pref-button-fill": ["buttonFillColor", "value"],
    "pref-button-text": ["buttonTextColor", "value"],
    "pref-button-border": ["buttonBorderColor", "value"],
    "pref-shape": ["buttonShape", "value"],
    "pref-interface": ["interfaceMode", "value"],
    "pref-nav-collapse": ["navigationBarCollapsed", "boolean"],
    "pref-menu-labels": ["showMenuLabels", "boolean"],
    "pref-subtitle-size": ["subtitleSize", "number"],
    "pref-subtitle-offset": ["subtitleOffset", "number"],
    "pref-subtitle-text": ["subtitleText", "value"],
    "pref-subtitle-background": ["subtitleBackground", "value"],
    "pref-subtitle-font": ["subtitleFont", "value"],
    "pref-subtitle-transparency": ["subtitleBoxTransparency", "number"],
    "pref-subtitle-box": ["subtitleBoxEnabled", "boolean"],
    "pref-auto-subtitles": ["autoApplySubtitles", "boolean"],
    "pref-clock": ["showClockOnHome", "boolean"],
    "pref-clock-other": ["showClockOnOtherPages", "boolean"],
    "pref-clock-format": ["timeFormat24Hour", "boolean"],
    "pref-clock-mode": ["clockMode", "value"],
    "pref-clock-manual": ["manualClockValue", "value"],
    "pref-timezone": ["timeZoneId", "value"],
    "pref-clock-pattern": ["clockPattern", "value"],
    "pref-clock-color": ["clockColor", "value"],
    "pref-clock-size": ["clockSize", "number"],
    "pref-language": ["languageTag", "value"],
    "pref-device-name": ["deviceName", "value"]
  };
  const setting = preferenceControls[event.target.id];
  if (setting) {
    const [key, type] = setting;
    const value = type === "number" ? Number(event.target.value) : type === "boolean" ? event.target.value === "true" : type === "uppercase" ? event.target.value.trim().slice(0, 2).toUpperCase() : event.target.value.trim();
    state.preferences[key] = value;
    saveProfilePreferences();
    applyPreferences();
  }
  if (event.target.id === "player-progress") {
    const dur = playerDuration();
    if (dur > 0) playerSeekTo(Number(event.target.value) / 1e3 * dur);
  }
  if (event.target.id === "player-speed") {
    const video = document.getElementById("video-player");
    if (video) video.playbackRate = Number(event.target.value);
  }
  if (event.target.id === "player-track") {
    setSubtitleTrack(Number(event.target.value), Number(event.target.value) >= 0);
  }
  if (event.target.id === "player-quality") {
    setPlayerSource(Number(event.target.value), playerCurrentTime());
  }
  if (event.target.id === "subtitle-file") {
    loadLocalSubtitle((_b = event.target.files) == null ? void 0 : _b[0]).catch((error) => toastMessage("Subtitle load failed: ".concat(error.message)));
  }
});
document.addEventListener("click", (event) => {
  var _a2, _b, _c, _d, _e, _f, _g, _h, _i, _j, _k, _l;
  const dpadOption = (_b = (_a2 = event.target).closest) == null ? void 0 : _b.call(_a2, "[data-dpad-option-index]");
  if (dpadOption && (activeDpadSelectMenu == null ? void 0 : activeDpadSelectMenu.menu.contains(dpadOption))) {
    chooseDpadSelectOption(Number(dpadOption.dataset.dpadOptionIndex));
    return;
  }
  const button = event.target.closest("[data-action]");
  if (!button) return;
  const action = button.dataset.action;
  if (document.querySelector(".player-shell") && action !== "stop") setPlayerControlsVisible(true, true);
  if (["home", "movies", "series", "search", "settings"].includes(action)) renderPage(action);
  else if (action === "profiles") showProfileSwitcher();
  else if (action === "details") openDetails(button.dataset.id, button.dataset.type);
  else if (action === "hero-play") {
    const media = (_c = state.homeData) == null ? void 0 : _c.movies.find((item) => String(item.id) === button.dataset.id);
    if (media) startPlayback(__spreadProps(__spreadValues({}, media), { media_type: "movie" }), null, Number(button.dataset.position || 0));
  } else if (action === "hero-favorite") {
    const media = (_c = state.homeData) == null ? void 0 : _c.movies.find((item) => String(item.id) === button.dataset.id.split(":").pop());
    if (media) {
      toggleFavorite(button.dataset.id, __spreadProps(__spreadValues({}, media), { media_type: "movie" }));
      const isFavorite = state.favoriteIds.includes(button.dataset.id);
      button.textContent = isFavorite ? "Remove from My list" : "Add to My list";
      button.setAttribute("aria-pressed", String(isFavorite));
    }
  } else if (action === "back") renderPage(state.previousPage || "home");
  else if (action === "favorite") toggleFavorite(button.dataset.id);
  else if (action === "details-trailer") {
    if (state.currentTrailerUrl) window.open(state.currentTrailerUrl, "_blank", "noopener");
  } else if (action === "search-favorite") {
    const media = state.searchResults.find((item) => "".concat(item.media_type, ":").concat(item.id) === button.dataset.id);
    if (media) toggleFavorite(button.dataset.id, media);
  } else if (action === "test-subdl") {
    toastMessage("Testing SubDL connection\u2026");
    testSubdlConnection().then(
      () => toastMessage("Connected to SubDL."),
      (error) => toastMessage("SubDL connection failed: ".concat(error.message))
    );
  } else if (action === "reset-settings") {
    state.preferences = __spreadValues({}, preferenceDefaults);
    saveProfilePreferences();
    applyPreferences();
    renderSettings();
    toastMessage("Settings reset.");
  } else if (action === "retry-home") {
    state.homeError = "";
    loadHome();
  } else if (action === "clear-search") {
    clearTimeout(state.searchTimer);
    state.searchQuery = "";
    const input = document.getElementById("search-query");
    if (input) {
      input.value = "";
      input.focus();
    }
    searchTitles("");
  } else if (action === "load-search-more") {
    searchTitles(state.searchQuery, state.searchPage + 1);
  } else if (action === "retry-search-more") {
    searchTitles(state.searchQuery, state.searchPage + 1);
  } else if (action === "retry-search") {
    searchTitles(state.searchQuery);
  } else if (action === "check-updates") {
    toastMessage("No release feed is configured for this build.");
  } else if (action === "select-profile") {
    try {
      activateProfile(button.dataset.id);
      if (state.page === "home") renderHome();
      else if (state.page === "settings") renderSettings();
      renderProfileSwitcher();
    } catch (error) {
      const output = document.querySelector(".profile-switcher-overlay [data-profile-error]");
      if (output) {
        output.textContent = error.message;
        output.hidden = false;
      } else {
        toastMessage(error.message);
      }
    }
  } else if (action === "remove-profile") {
    showRemoveProfileDialog(button.dataset.id || state.activeProfileId);
  } else if (action === "resume") {
    state.resumePosition = Number(button.dataset.position || 0);
    state.resumeEpisode = button.dataset.season ? { season: Number(button.dataset.season), episode: Number(button.dataset.episode) } : null;
    openDetails(button.dataset.id, button.dataset.type).then(() => {
      var _a3;
      if (String((_a3 = state.currentTitle) == null ? void 0 : _a3.id) === button.dataset.id) {
        const isSeries = button.dataset.type === "tv" || button.dataset.type === "series" || Boolean(state.resumeEpisode);
        const episode = isSeries ? state.resumeEpisode ? __spreadProps(__spreadValues({}, state.resumeEpisode), { position: state.resumePosition }) : { season: 1, episode: 1, position: state.resumePosition } : null;
        startPlayback(state.currentTitle, episode, state.resumePosition);
      }
    });
  } else if (action === "edit-profile") {
    const profile = state.profiles.find((item) => item.id === button.dataset.id);
    if (profile) showProfileEditor(profile);
  } else if (action === "new-profile") {
    if (state.profiles.length < 4) showProfileEditor();
  } else if (action === "choose-profile-avatar") {
    const form = document.getElementById(button.dataset.formId);
    const avatar = ((_d = form == null ? void 0 : form.elements.namedItem("avatar")) == null ? void 0 : _d.value) || "piccdn:anime:0";
    if (form) showProfileAvatarPicker(form.id, avatar);
  } else if (action === "close-profile-switcher") {
    const overlay = document.querySelector(".profile-switcher-overlay");
    const returnFocus = overlay == null ? void 0 : overlay.returnFocus;
    overlay == null ? void 0 : overlay.remove();
    returnFocus == null ? void 0 : returnFocus.focus();
  } else if (action === "close-profile-editor") {
    dismissProfileOverlay();
  } else if (action === "confirm-remove-profile") {
    const overlay = button.closest(".profile-editor-overlay");
    try {
      removeProfile(button.dataset.id);
      overlay == null ? void 0 : overlay.remove();
    } catch (error) {
      const output = overlay == null ? void 0 : overlay.querySelector("[data-profile-error]");
      if (output) {
        output.textContent = error.message;
        output.hidden = false;
      } else {
        toastMessage(error.message);
      }
    }
  } else if (action === "play" || action === "play-series" || action === "play-episode") {
    const isSeries = (((_e = state.currentTitle) == null ? void 0 : _e.media_type) || button.dataset.type) === "tv" || action === "play-series" || action === "play-episode";
    let episode = null;
    if (isSeries) {
      const season = Number(button.dataset.season || state.selectedSeason || 1);
      const epNum = Number(button.dataset.episode || 1);
      const pos = Number(button.dataset.position || 0);
      episode = { season, episode: epNum, position: pos };
    }
    startPlayback(state.currentTitle, episode, Number(button.dataset.position || 0));
  } else if (action === "load-more") loadCatalog(button.dataset.kind);
  else if (action === "catalog-retry") {
    loadCatalog(button.dataset.kind, button.dataset.reset === "true");
  } else if (action === "search-subtitles") searchSubdlSubtitles();
  else if (action === "download-subtitle") {
    downloadSubdlSubtitle(Number(button.dataset.index)).catch((error) => toastMessage("Subtitle download failed: ".concat(error.message)));
  } else if (action === "player-subtitles") {
    state.subtitleDialogOpen = true;
    document.getElementById("player-subtitle-dialog").hidden = false;
    (_f = document.querySelector(".player-shell")) == null ? void 0 : _f.classList.add("subtitle-dialog-open");
    (_g = document.querySelector('[data-action="close-subtitles"]')) == null ? void 0 : _g.focus();
  } else if (action === "close-subtitles") {
    state.subtitleDialogOpen = false;
    document.getElementById("player-subtitle-dialog").hidden = true;
    (_h = document.querySelector(".player-shell")) == null ? void 0 : _h.classList.remove("subtitle-dialog-open");
    (_i = document.querySelector('[data-action="player-subtitles"]')) == null ? void 0 : _i.focus();
  } else if (action === "toggle-subtitles") {
    const video = document.getElementById("video-player");
    const tracks = Array.from((video == null ? void 0 : video.textTracks) || []);
    const enabled = isAvplayActive ? state.avplaySubtitlesEnabled : tracks.some((track) => track.mode === "showing");
    const selector = document.getElementById("player-track");
    const selectedIndex = selector ? Number(selector.value) : -1;
    const firstTrackIndex = tracks.findIndex((track) => track.kind === "subtitles" || track.kind === "captions");
    const targetIndex = enabled ? -1 : selectedIndex >= 0 ? selectedIndex : firstTrackIndex;
    if (targetIndex < 0 && !enabled) {
      const status = document.getElementById("subtitle-status");
      if (status) status.textContent = "No subtitle track is loaded. Search SubDL or load an SRT/VTT file.";
      return;
    }
    setSubtitleTrack(targetIndex, targetIndex >= 0);
    state.subtitleDialogOpen = true;
  } else if (action === "subtitle-offset") {
    state.preferences.subtitleOffset = Math.max(-1e4, Math.min(1e4, Number(state.preferences.subtitleOffset || 0) + Number(button.dataset.offset)));
    saveProfilePreferences();
    applyPlayerSubtitleStyle();
    document.getElementById("subtitle-offset-label").textContent = "Offset: ".concat(state.preferences.subtitleOffset, " ms");
  } else if (action === "subtitle-offset-reset") {
    state.preferences.subtitleOffset = 0;
    saveProfilePreferences();
    applyPlayerSubtitleStyle();
    document.getElementById("subtitle-offset-label").textContent = "Offset: 0 ms";
  } else if (action === "subtitle-size") {
    state.preferences.subtitleSize = Math.max(14, Math.min(36, Number(state.preferences.subtitleSize || 22) + Number(button.dataset.change)));
    saveProfilePreferences();
    applyPreferences();
    document.getElementById("subtitle-size-label").textContent = "".concat(state.preferences.subtitleSize, "px");
  } else if (action === "subtitle-background") {
    state.preferences.subtitleBoxEnabled = !state.preferences.subtitleBoxEnabled;
    saveProfilePreferences();
    applyPreferences();
    button.setAttribute("aria-pressed", String(state.preferences.subtitleBoxEnabled));
    button.textContent = "Subtitle background: ".concat(state.preferences.subtitleBoxEnabled ? "On" : "Off");
  } else if (action === "subtitle-preset") {
    const preset = button.dataset.preset;
    if (preset === "dark") {
      state.preferences.subtitleBackground = "#000000";
      state.preferences.subtitleText = "#ffffff";
      state.preferences.subtitleBoxEnabled = true;
      state.preferences.subtitleBoxTransparency = 20;
    } else if (preset === "light") {
      state.preferences.subtitleBackground = "#ffffff";
      state.preferences.subtitleText = "#000000";
      state.preferences.subtitleBoxEnabled = true;
      state.preferences.subtitleBoxTransparency = 15;
    } else {
      state.preferences.subtitleText = "#ffffff";
      state.preferences.subtitleBoxEnabled = true;
      state.preferences.subtitleBoxTransparency = 0;
    }
    saveProfilePreferences();
    applyPreferences();
  } else if (action === "toggle-quality-menu") {
    const menu = document.getElementById("player-quality-menu");
    if (menu) {
      menu.hidden = !menu.hidden;
      if (!menu.hidden) {
        const activeItem = menu.querySelector(".quality-menu-item.active") || menu.querySelector(".quality-menu-item");
        activeItem == null ? void 0 : activeItem.focus();
      }
    }
  } else if (action === "select-quality") {
    const index = Number(button.dataset.index);
    const menu = document.getElementById("player-quality-menu");
    if (menu) menu.hidden = true;
    setPlayerSource(index, playerCurrentTime());
    (_j = document.querySelector('[data-action="toggle-quality-menu"]')) == null ? void 0 : _j.focus();
  } else if (action === "aspect-ratio") {
    state.playerAspectRatio = (state.playerAspectRatio + 1) % 3;
    updatePlayerAspectRatio();
    const labels = ["Fit (Aspect)", "Crop (Zoom)", "Stretch (Fill)"];
    toastMessage("Screen ratio: ".concat(labels[state.playerAspectRatio % 3]));
  } else if (action === "toggle-fullscreen") {
    togglePlayerFullscreen().catch((error) => toastMessage("Fullscreen unavailable: ".concat(error.message)));
  } else if (action === "retry-stream-resolution") {
    if (state.currentTitle) {
      startPlayback(state.currentTitle, state.currentEpisode, playerCurrentTime() * 1e3);
    }
  } else if (action === "toggle-autoplay") {
    state.autoPlayNextEpisode = !state.autoPlayNextEpisode;
    button.classList.toggle("active", state.autoPlayNextEpisode);
    button.setAttribute("aria-label", "Auto-play next episode: ".concat(state.autoPlayNextEpisode ? "On" : "Off"));
    toastMessage("Auto-play next episode: ".concat(state.autoPlayNextEpisode ? "On" : "Off"));
    const dur = playerDuration();
    if (state.nextEpisode && dur > 0) {
      renderUpNext(playerCurrentTime(), dur);
    }
  } else if (action === "play-next") {
    playNextEpisode();
  } else if (action === "cancel-up-next") {
    state.upNextDismissed = true;
    document.getElementById("player-up-next").hidden = true;
    (_k = document.querySelector('[data-action="toggle-play"]')) == null ? void 0 : _k.focus();
  } else if (action === "skip-intro") {
    if (((_l = state.episodeTiming) == null ? void 0 : _l.introEndMs) != null) {
      playerSeekTo(state.episodeTiming.introEndMs / 1e3);
      setPlayerControlsVisible(true, true);
    }
  } else if (action === "skip-credits") {
    if (state.nextEpisode && state.currentTitle) {
      playNextEpisode();
    } else {
      const dur = playerDuration();
      if (dur > 0) playerSeekTo(Math.max(playerCurrentTime(), dur - 0.5));
    }
  } else if (action === "seek") {
    playerSeek(Number(button.dataset.seconds || 0));
    setPlayerControlsVisible(true, true);
  } else if (action === "toggle-play") {
    playerTogglePlay();
    setPlayerControlsVisible(true, isPlayerPlaying());
  } else if (action === "stop") {
    returnToTitleDetails();
  }
});
function isFocusableVisible(el) {
  if (!el || el.disabled || el.hidden) return false;
  if (el.offsetWidth === 0 && el.offsetHeight === 0 && el.getClientRects().length === 0) return false;
  const style = window.getComputedStyle(el);
  if (style.display === "none" || style.visibility === "hidden" || style.opacity === "0") return false;
  return true;
}
function navigatePlayerControls(direction) {
  const current = document.activeElement;
  const playerControls = document.getElementById("player-controls");
  const subDialog = document.getElementById("player-subtitle-dialog");
  const qualityMenu = document.getElementById("player-quality-menu");
  const upNext = document.getElementById("player-up-next");
  if (upNext && !upNext.hidden && upNext.contains(current)) {
    const btns = Array.from(upNext.querySelectorAll(".focusable")).filter(isFocusableVisible);
    const idx = btns.indexOf(current);
    if (direction === "right" && idx < btns.length - 1) return btns[idx + 1];
    if (direction === "left" && idx > 0) return btns[idx - 1];
    return current;
  }
  if (qualityMenu && !qualityMenu.hidden && qualityMenu.contains(current)) {
    const items = Array.from(qualityMenu.querySelectorAll(".quality-menu-item")).filter(isFocusableVisible);
    const idx = items.indexOf(current);
    if (direction === "down" && idx < items.length - 1) return items[idx + 1];
    if (direction === "up" && idx > 0) return items[idx - 1];
    if (direction === "left" || direction === "back") {
      qualityMenu.hidden = true;
      return document.querySelector('[data-action="toggle-quality-menu"]');
    }
    return current;
  }
  if (subDialog && !subDialog.hidden && subDialog.contains(current)) {
    const items = Array.from(subDialog.querySelectorAll(".focusable")).filter(isFocusableVisible);
    const idx = items.indexOf(current);
    if (idx >= 0) {
      if (direction === "right" && idx < items.length - 1) return items[idx + 1];
      if (direction === "left" && idx > 0) return items[idx - 1];
      if (direction === "down" && idx < items.length - 1) return items[Math.min(items.length - 1, idx + 2)];
      if (direction === "up" && idx > 0) return items[Math.max(0, idx - 2)];
    }
    return current;
  }
  const backBtn = document.querySelector(".player-back-btn");
  const progressBar = document.getElementById("player-progress");
  const bottomButtons = Array.from(document.querySelectorAll(".player-transport-controls .focusable, .player-utility-row .focusable")).filter(isFocusableVisible);
  const playBtn = document.querySelector('[data-action="toggle-play"]') || bottomButtons[0];
  if (!playerControls || !playerControls.classList.contains("player-controls-visible") || current && current.matches(".player-shell")) {
    setPlayerControlsVisible(true, true);
    return playBtn;
  }
  setPlayerControlsVisible(true, true);
  if (current === backBtn) {
    if (direction === "down") return progressBar || playBtn;
    return backBtn;
  }
  if (current === progressBar) {
    if (direction === "left" || direction === "right") {
      playerSeek(direction === "right" ? 10 : -10);
      setPlayerControlsVisible(true, true);
      return progressBar;
    }
    if (direction === "up") return backBtn || progressBar;
    if (direction === "down") return playBtn;
    return progressBar;
  }
  const btnIdx = bottomButtons.indexOf(current);
  if (btnIdx >= 0) {
    if (direction === "right") {
      return bottomButtons[Math.min(bottomButtons.length - 1, btnIdx + 1)];
    }
    if (direction === "left") {
      return bottomButtons[Math.max(0, btnIdx - 1)];
    }
    if (direction === "up") {
      return progressBar || backBtn;
    }
    if (direction === "down") {
      return current;
    }
  }
  return playBtn;
}
function nearestFocusable(direction) {
  const current = document.activeElement;
  if (!(current == null ? void 0 : current.classList.contains("focusable")) && !(current == null ? void 0 : current.matches(".player-shell"))) return null;
  const playerShell = document.querySelector(".player-shell");
  if (playerShell && state.page === "player") {
    return navigatePlayerControls(direction);
  }
  if (state.page === "search") {
    const searchInput = document.getElementById("search-query");
    const clearButton = main.querySelector('[data-action="clear-search"]');
    const firstResult = main.querySelector("#search-results .search-result-card .poster-card.focusable");
    const currentCard = current.closest(".search-result-card");
    const firstCard = firstResult && firstResult.closest(".search-result-card");
    if (current === searchInput && direction === "down" && clearButton && isFocusableVisible(clearButton)) return clearButton;
    if (current === clearButton && direction === "left" && searchInput) return searchInput;
    if (current === clearButton && direction === "down" && firstResult) return firstResult;
    if (currentCard && firstCard && direction === "up" &&
        Math.abs(currentCard.getBoundingClientRect().top - firstCard.getBoundingClientRect().top) < 2 &&
        clearButton && isFocusableVisible(clearButton)) return clearButton;
    if (current === searchInput && direction === "right" &&
        searchInput.selectionStart === searchInput.value.length &&
        searchInput.selectionEnd === searchInput.value.length &&
        clearButton && isFocusableVisible(clearButton)) return clearButton;
    if (current === clearButton && direction === "right" && firstResult) return firstResult;
  }
  const filterContainer = current.closest(".catalog-filters");
  if (filterContainer) {
    const filters = Array.from(filterContainer.querySelectorAll(".focusable")).filter(isFocusableVisible);
    const fIdx = filters.indexOf(current);
    if (direction === "right" && fIdx >= 0 && fIdx < filters.length - 1) {
      return filters[fIdx + 1];
    }
    if (direction === "left" && fIdx > 0) {
      return filters[fIdx - 1];
    }
    if (direction === "down") {
      const firstCard = main.querySelector(".catalog-row .card-row .focusable");
      if (firstCard && isFocusableVisible(firstCard)) return firstCard;
    }
    if (direction === "left" && fIdx === 0) {
      const activeNav = document.querySelector(".topbar .nav-button.active, .topbar .nav-button[data-action='home'], .topbar .focusable");
      if (activeNav) return activeNav;
    }
  }
  const rowContainer = current.closest(".card-row, .cast-row, .action-row, .details-actions, .catalog-filters, .profile-switcher-list, .profile-avatar-categories, .results-grid");
  if (rowContainer && (direction === "left" || direction === "right")) {
    const focusables = Array.from(rowContainer.querySelectorAll(".focusable")).filter(isFocusableVisible);
    const idx = focusables.indexOf(current);
    if (idx >= 0) {
      if (direction === "right" && idx < focusables.length - 1) {
        return focusables[idx + 1];
      }
      if (direction === "left") {
        if (idx > 0) return focusables[idx - 1];
        const activeNav = document.querySelector(".topbar .nav-button.active, .topbar .nav-button[data-action='home'], .topbar .focusable");
        if (activeNav) return activeNav;
      }
    }
  }
  const currentTopbar = current.closest(".topbar");
  if (currentTopbar) {
    if (direction === "right") {
      const firstTarget = firstFocusable(main);
      if (firstTarget) return firstTarget;
    }
    const navItems = Array.from(currentTopbar.querySelectorAll(".focusable")).filter(isFocusableVisible);
    const navIdx = navItems.indexOf(current);
    if (navIdx >= 0) {
      if (direction === "down" && navIdx < navItems.length - 1) return navItems[navIdx + 1];
      if (direction === "up" && navIdx > 0) return navItems[navIdx - 1];
    }
    return null;
  }
  if (rowContainer && (direction === "down" || direction === "up")) {
    const parentSection = current.closest(".hero, .section, .catalog-row, .details-section, .details-hero");
    if (parentSection) {
      const sections = Array.from(main.querySelectorAll(".hero, .section, .catalog-row, .details-section, .details-hero"));
      const secIdx = sections.indexOf(parentSection);
      if (secIdx >= 0) {
        if (direction === "up" && secIdx === 0) {
          const filter = main.querySelector(".catalog-sort-chips .catalog-filter-chip") || document.getElementById("catalog-genre-toggle");
          if (filter && isFocusableVisible(filter)) return filter;
        }
        const nextSec = direction === "down" ? sections[secIdx + 1] : sections[secIdx - 1];
        if (nextSec) {
          const cands = Array.from(nextSec.querySelectorAll(".focusable")).filter(isFocusableVisible);
          if (cands.length > 0) {
            const fromBox = current.getBoundingClientRect();
            const fromCenter = fromBox.left + fromBox.width / 2;
            let best = cands[0];
            let minDiff = Infinity;
            for (const c of cands) {
              const b = c.getBoundingClientRect();
              const diff = Math.abs(b.left + b.width / 2 - fromCenter);
              if (diff < minDiff) {
                minDiff = diff;
                best = c;
              }
            }
            return best;
          }
        }
      }
    }
  }
  const playerDialog = state.subtitleDialogOpen ? document.getElementById("player-subtitle-dialog") : null;
  const qualityMenu = document.getElementById("player-quality-menu");
  const qualityMenuOpen = qualityMenu && !qualityMenu.hidden;
  const upNext = document.getElementById("player-up-next");
  const modalOverlays = Array.from(document.querySelectorAll(
    ".profile-switcher-overlay, .profile-editor-overlay, .profile-avatar-picker-overlay"
  )).filter(isFocusableVisible);
  const activeModal = modalOverlays[modalOverlays.length - 1];
  const items = Array.from(document.querySelectorAll(".focusable")).filter((item) => {
    if (item === current || !isFocusableVisible(item)) return false;
    if (activeModal) return activeModal.contains(item);
    if (playerDialog && !playerDialog.hidden) return item.closest("#player-subtitle-dialog") === playerDialog;
    if (qualityMenuOpen) return item.closest("#player-quality-menu") === qualityMenu;
    if (upNext && !upNext.hidden) return item.closest("#player-up-next") === upNext;
    if (playerShell) return item.closest(".player-shell") === playerShell;
    return true;
  });
  const from = current.getBoundingClientRect();
  const origin = { x: from.left + from.width / 2, y: from.top + from.height / 2 };
  const bestInDirection = (candidates) => {
    var _a2;
    const directional = candidates.map((item) => {
      const box = item.getBoundingClientRect();
      const point = { x: box.left + box.width / 2, y: box.top + box.height / 2 };
      const dx = point.x - origin.x;
      const dy = point.y - origin.y;
      const horizontal = direction === "left" || direction === "right";
      const primary = direction === "left" ? -dx : direction === "right" ? dx : direction === "up" ? -dy : dy;
      const crossGap = horizontal ? Math.max(0, from.top - box.bottom, box.top - from.bottom) : Math.max(0, from.left - box.right, box.left - from.right);
      const secondary = horizontal ? Math.abs(dy) : Math.abs(dx);
      return { item, primary, crossGap, secondary };
    }).filter((candidate) => candidate.primary > 0);
    if (!directional.length) return null;
    return ((_a2 = directional.sort((a, b) => a.primary + a.crossGap * 1.5 + a.secondary * 0.1 - (b.primary + b.crossGap * 1.5 + b.secondary * 0.1))[0]) == null ? void 0 : _a2.item) || null;
  };
  if (activeModal) return bestInDirection(items);
  if (current.closest("#main")) {
    const inPage = bestInDirection(items.filter((item) => main.contains(item)));
    if (inPage || direction !== "left") return inPage;
    const activeNavigationItem = items.find((item) => item.matches(".nav-button[aria-current=page], .profile-button[aria-current=page]"));
    if (activeNavigationItem) return activeNavigationItem;
    return bestInDirection(items.filter((item) => item.closest(".topbar")));
  }
  if (current.closest(".topbar")) {
    if (direction === "right") return firstFocusable(main);
    return bestInDirection(items.filter((item) => item.closest(".topbar")));
  }
  return bestInDirection(items);
}
function dismissProfileOverlay() {
  const overlays = document.querySelectorAll(".profile-switcher-overlay, .profile-editor-overlay");
  const overlay = overlays[overlays.length - 1];
  if (!overlay) return false;
  const returnFocus = overlay.returnFocus;
  overlay.remove();
  if (overlay.classList.contains("profile-switcher-overlay")) {
    returnFocus == null ? void 0 : returnFocus.focus();
  } else if (document.querySelector(".profile-switcher-overlay") && !overlay.classList.contains("profile-avatar-picker-overlay")) {
    renderProfileSwitcher();
  } else {
    returnFocus == null ? void 0 : returnFocus.focus();
  }
  return true;
}
document.addEventListener("keydown", (event) => {
  var _a2, _b, _c, _d, _e, _f, _g, _h, _i, _j, _k;
  if (activeDpadSelectMenu) {
    if (["Escape", "Back", "Backspace", "BrowserBack", "GoBack"].includes(event.key)) {
      event.preventDefault();
      closeDpadSelectMenu();
      return;
    }
    if (isConfirmKey(event.key)) {
      event.preventDefault();
      if (!event.repeat) chooseDpadSelectOption(activeDpadSelectMenu.index);
      return;
    }
    if (["ArrowUp", "ArrowDown"].includes(event.key)) {
      event.preventDefault();
      const direction2 = event.key === "ArrowDown" ? "down" : "up";
      const moved = focusDpadSelectOption(activeDpadSelectMenu.index + (direction2 === "down" ? 1 : -1));
      if (!moved) {
        closeDpadSelectMenu();
        moveFocus(direction2);
      }
      return;
    }
    if (["ArrowLeft", "ArrowRight"].includes(event.key)) {
      event.preventDefault();
      closeDpadSelectMenu();
      const target = nearestFocusable(event.key === "ArrowLeft" ? "left" : "right");
      if (target) {
        target.focus();
        scrollElementIntoViewIfNeeded(target);
      }
      return;
    }
  }
  const select = event.target instanceof HTMLSelectElement ? event.target : null;
  const direction = { ArrowLeft: "left", ArrowRight: "right", ArrowUp: "up", ArrowDown: "down" }[event.key];
  const summary = (_b = (_a2 = event.target).closest) == null ? void 0 : _b.call(_a2, "summary.focusable");
  if (summary && state.page === "settings") {
    const details = summary.closest("details");
    if (isConfirmKey(event.key) && details) {
      event.preventDefault();
      if (event.repeat) return;
      setSettingsCategoryOpen(details, !details.open);
      return;
    }
    if (direction === "right") {
      event.preventDefault();
      activateSettingsCategory(summary, true);
      return;
    }
    if (direction === "left" && (details == null ? void 0 : details.open)) {
      event.preventDefault();
      setSettingsCategoryOpen(details, false);
      (_c = details.querySelector("summary")) == null ? void 0 : _c.focus();
      return;
    }
  }
  if (select) {
    if (isConfirmKey(event.key)) {
      event.preventDefault();
      if (!event.repeat) openDpadSelectMenu(select);
      return;
    }
    if (direction) {
      event.preventDefault();
      moveFocus(direction);
      return;
    }
  }
  const control = event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement ? event.target : null;
  const editableControl = control && !["button", "submit", "reset", "checkbox", "radio", "file", "color", "hidden", "image"].includes(control.type) ? control : event.target.isContentEditable ? event.target : null;
  if (editableControl) {
    const type = editableControl instanceof HTMLInputElement ? editableControl.type : "text";
    if (activeEditControl === editableControl) {
      if (event.key === "Escape") {
        event.preventDefault();
        finishControlEditing();
        return;
      }
      if (event.key === "Enter" || ["Back", "BrowserBack", "GoBack"].includes(event.key)) {
        finishControlEditing();
        if (!((_d = editableControl.form) == null ? void 0 : _d.id) || event.key !== "Enter") event.preventDefault();
        return;
      }
      if (direction) {
        const editsWithDirection = type === "range" && ["left", "right"].includes(direction) || type === "number" && ["up", "down"].includes(direction) || type !== "range" && type !== "number" && ["left", "right"].includes(direction);
        if (editsWithDirection) return;
        event.preventDefault();
        moveFocus(direction);
        return;
      }
    } else {
      if (direction) {
        event.preventDefault();
        moveFocus(direction);
        return;
      }
      if (event.key === "Enter" || ["OK", "Accept", "Select"].includes(event.key)) {
        event.preventDefault();
        if (!event.repeat) startControlEditing(editableControl);
        return;
      }
      if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
        startControlEditing(editableControl);
      }
    }
  }
  if (["OK", "Accept", "Select"].includes(event.key) && event.target instanceof HTMLButtonElement) {
    event.preventDefault();
    if (!event.repeat) event.target.click();
    return;
  }
  if (event.repeat && isConfirmKey(event.key) && event.target instanceof HTMLButtonElement) {
    event.preventDefault();
    return;
  }
  if (((_f = (_e = event.target).matches) == null ? void 0 : _f.call(_e, "input, textarea")) && direction) {
    event.preventDefault();
    moveFocus(direction);
    return;
  }
  let lastBackHandledTimestamp = 0;
  function handleBackNavigation2(e) {
    if (e && e.preventDefault) e.preventDefault();
    const now = Date.now();
    if (now - lastBackHandledTimestamp < 350) return true;
    lastBackHandledTimestamp = now;
    if (activeDpadSelectMenu) {
      closeDpadSelectMenu();
      return true;
    }
    if (activeEditControl) {
      finishControlEditing();
      return true;
    }
    if (dismissProfileOverlay()) {
      return true;
    }
    if (state.page === "settings") {
      const details = document.querySelector("details.settings-card[open]");
      if (details) {
        setSettingsCategoryOpen(details, false);
        const summary2 = details.querySelector("summary");
        if (summary2) summary2.focus();
        return true;
      }
    }
    const playerShell2 = document.querySelector(".player-shell");
    if (playerShell2 && state.page === "player") {
      if (document.fullscreenElement === playerShell2) {
        var _g2;
        (_g2 = document.exitFullscreen) == null ? void 0 : _g2.call(document).catch((error) => console.warn("Could not exit player fullscreen:", error.message));
        return true;
      }
      const qualityMenu = document.getElementById("player-quality-menu");
      if (qualityMenu && !qualityMenu.hidden) {
        qualityMenu.hidden = true;
        var _h2;
        (_h2 = document.querySelector('[data-action="toggle-quality-menu"]')) == null ? void 0 : _h2.focus();
        return true;
      }
      if (state.subtitleDialogOpen) {
        state.subtitleDialogOpen = false;
        const subDialog = document.getElementById("player-subtitle-dialog");
        if (subDialog) subDialog.hidden = true;
        playerShell2.classList.remove("subtitle-dialog-open");
        var _i2;
        (_i2 = document.querySelector('[data-action="player-subtitles"]')) == null ? void 0 : _i2.focus();
        return true;
      }
      returnToTitleDetails();
      return true;
    }
    if (state.page === "details") {
      renderPage(state.previousPage || "home");
      return true;
    }
    if (state.page !== "home") {
      renderHome();
      return true;
    }
    const inTopbar = document.activeElement && document.activeElement.closest(".topbar");
    if (!inTopbar) {
      const homeNav = document.querySelector('.nav-button[data-action="home"]');
      if (homeNav) homeNav.focus();
    }
    return true;
  }
  if (isBackNavigationKey(event)) {
    handleBackNavigation2(event);
    return;
  }
  const playerShell = document.querySelector(".player-shell");
  if (playerShell && state.page === "player") {
    const inControls = Boolean(event.target.closest("#player-controls"));
    const onSeekbar = event.target.id === "player-progress";
    const inOverlay = Boolean(event.target.closest(".player-skip-action, .up-next-overlay, #player-subtitle-dialog, #player-quality-menu"));
    const keyCode = event.keyCode || event.which;
    const isPlay = event.key === "MediaPlay" || keyCode === 415;
    const isPause = event.key === "MediaPause" || keyCode === 19;
    const isPlayPause = event.key === "MediaPlayPause" || keyCode === 10252;
    const isStop = event.key === "MediaStop" || keyCode === 413;
    const isFwd = event.key === "MediaFastForward" || keyCode === 417;
    const isRwd = event.key === "MediaRewind" || keyCode === 412;
    if (isPlay || isPause || isPlayPause || isStop || isFwd || isRwd) {
      event.preventDefault();
      if (isPlay) playerPlay();
      else if (isPause) playerPause();
      else if (isPlayPause) playerTogglePlay();
      else if (isStop) returnToTitleDetails();
      else if (isRwd) playerSeek(-10);
      else if (isFwd) playerSeek(10);
      setPlayerControlsVisible(true, isPlayerPlaying());
      return;
    }
    const playerControls = document.getElementById("player-controls");
    const controlsVisible = Boolean(playerControls && playerControls.classList.contains("player-controls-visible"));
    if (!controlsVisible || !inControls && !inOverlay) {
      if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(event.key)) {
        event.preventDefault();
        setPlayerControlsVisible(true, true);
        const playBtn = document.querySelector('[data-action="toggle-play"]');
        if (playBtn) {
          playBtn.focus();
          scrollElementIntoViewIfNeeded(playBtn);
        }
        return;
      }
    }
    if (onSeekbar && ["ArrowLeft", "ArrowRight"].includes(event.key)) {
      event.preventDefault();
      playerSeek(event.key === "ArrowLeft" ? -10 : 10);
      setPlayerControlsVisible(true, true);
      return;
    }
    if (isConfirmKey(event) && !event.target.closest("button, select, input, #player-subtitle-dialog, #player-quality-menu")) {
      event.preventDefault();
      if (!event.repeat) {
        playerTogglePlay();
        setPlayerControlsVisible(true, isPlayerPlaying());
      }
      return;
    }
  }
  if ((_k = (_j = event.target).matches) == null ? void 0 : _k.call(_j, "input, textarea, [contenteditable=true]")) return;
  if (direction) {
    event.preventDefault();
    moveFocus(direction);
  }
});
window.addEventListener("tizenhwkey", (event) => {
  if (event.keyName === "back") {
    handleBackNavigation(event);
  }
});
document.addEventListener("fullscreenchange", updatePlayerFullscreenButton);
try {
  if ((_a = window.tizen) == null ? void 0 : _a.tvinputdevice) {
    for (const key of ["MediaPlay", "MediaPause", "MediaPlayPause", "MediaStop", "MediaRewind", "MediaFastForward"]) {
      try {
        tizen.tvinputdevice.registerKey(key);
      } catch (error) {
        console.warn("Remote key ".concat(key, " unavailable:"), error.message);
      }
    }
  }
} catch (error) {
  console.warn("TV remote key registration unavailable:", error.message);
}
document.addEventListener("DOMContentLoaded", () => {
  applyPreferences();
  if (!state.profiles.length) {
    renderProfiles();
    return;
  }
  if (!state.profiles.some((profile) => profile.id === state.activeProfileId)) {
    state.activeProfileId = state.profiles[0].id;
    localStorage.setItem("echo.activeProfileId", state.activeProfileId);
  }
  const favoriteIdsKey = profileKey("favoriteIds");
  const favoriteTitlesKey = profileKey("favoriteTitles");
  const watchProgressKey = profileKey("watchProgress");
  state.favoriteIds = readJson(favoriteIdsKey, readJson("echo.favoriteIds", []));
  state.favoriteTitles = readJson(favoriteTitlesKey, readJson("echo.favoriteTitles", {}));
  state.progress[watchProgressKey] = readJson(watchProgressKey, state.progress["echo.watchProgress"] || {});
  if (!localStorage.getItem(favoriteIdsKey)) saveState(favoriteIdsKey, state.favoriteIds);
  if (!localStorage.getItem(favoriteTitlesKey)) saveState(favoriteTitlesKey, state.favoriteTitles);
  if (!localStorage.getItem(watchProgressKey)) saveState(watchProgressKey, state.progress[watchProgressKey]);
  const profilePreferences = readJson(profileKey("preferences"), null);
  if (profilePreferences) {
    state.preferences = __spreadValues(__spreadValues({}, preferenceDefaults), profilePreferences);
  } else {
    state.preferences = __spreadValues(__spreadValues({}, preferenceDefaults), readJson("echo.preferences", state.preferences));
    saveProfilePreferences();
  }
  applyPreferences();
  renderHome();
});
