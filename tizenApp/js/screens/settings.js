function readDeviceInfoValue(api, methodName) {
  const method = api == null ? void 0 : api[methodName];
  if (typeof method !== "function") return "";
  try {
    const value = method.call(api);
    return typeof value === "string" || typeof value === "number" ? String(value).trim() : "";
  } catch (e) {
    return "";
  }
}
function renderDeviceInfo() {
  var _a, _b, _c;
  const networkApi = (_a = window.webapis) == null ? void 0 : _a.network;
  const productApi = (_b = window.webapis) == null ? void 0 : _b.productinfo;
  const systemInfo = (_c = window.tizen) == null ? void 0 : _c.systeminfo;
  const hasTizenApi = Boolean(networkApi || productApi || systemInfo);
  const readCapability = (name) => {
    if (typeof (systemInfo == null ? void 0 : systemInfo.getCapability) !== "function") return "";
    try {
      return String(systemInfo.getCapability(name) || "").trim();
    } catch (e) {
      return "";
    }
  };
  const model = readDeviceInfoValue(productApi, "getModelCode");
  const firmware = readDeviceInfoValue(productApi, "getFirmware") || readDeviceInfoValue(productApi, "getFirmwareVersion");
  const rawNetwork = readDeviceInfoValue(networkApi, "getActiveConnectionType") || readDeviceInfoValue(networkApi, "getNetworkType");
  const networkType = {
    NETWORK_TYPE_WIFI: "Wi-Fi",
    NETWORK_TYPE_ETHERNET: "Ethernet",
    NETWORK_TYPE_UNKNOWN: "Unknown"
  }[rawNetwork.toUpperCase()] || rawNetwork.replace(/_/g, " ");
  const unavailable = hasTizenApi ? "Unavailable on this device" : "Unavailable in browser preview";
  const tizenDev = window.TIZEN_DEVICE;
  const versionString = tizenDev && tizenDev.version ? tizenDev.isTizen ? "Tizen " + tizenDev.version + (tizenDev.chromeVersion ? " (Chrome " + tizenDev.chromeVersion + ")" : "") : "Browser Preview \xB7 Emulating Tizen " + tizenDev.version : readCapability("http://tizen.org/feature/platform.version") || unavailable;
  const tierName = tizenDev && tizenDev.performanceTier ? {
    ultra: "Ultra Performance (Tizen 7.0+ \xB7 4K/8K HDR)",
    high: "High Performance (Tizen 6.0-6.5 \xB7 4K)",
    medium: "Balanced (Tizen 5.0-5.5)",
    low: "Legacy Baseline (Tizen 4.0 Compatible)"
  }[tizenDev.performanceTier] || tizenDev.performanceTier : "Standard Profile";
  const activeFeatures = [];
  if (tizenDev && tizenDev.features) {
    if (tizenDev.features.backdropFilter) activeFeatures.push("Glassmorphism");
    if (tizenDev.features.highResArtwork) activeFeatures.push("4K High-Res Art");
    if (tizenDev.features.smoothScroll) activeFeatures.push("Smooth Motion");
    if (tizenDev.features.enhancedBuffering) activeFeatures.push("Enhanced AVPlay Buffer");
    if (tizenDev.features.nativeFlexGap) activeFeatures.push("Native Flex Gap");
    if (tizenDev.features.reducedMemoryMode) activeFeatures.push("Low-Memory Optimization");
  }
  const rows = [
    ["Device model", model ? "Samsung TV \xB7 ".concat(model) : unavailable],
    ["Tizen version", versionString],
    ["Platform profile", tierName],
    ["Active optimizations", activeFeatures.length ? activeFeatures.join(", ") : "Standard 4.0 baseline"],
    ["Firmware", firmware || unavailable],
    ["CPU architecture", readCapability("http://tizen.org/feature/platform.core.cpu.arch") || unavailable],
    ["Network", rawNetwork ? networkType : unavailable],
    ["Device IP", readDeviceInfoValue(networkApi, "getIp") || unavailable],
    ["MAC address", readDeviceInfoValue(networkApi, "getMac") || unavailable]
  ];
  return '<section class="device-info" aria-label="Device information"><h3>Device info</h3>\n        <dl class="device-info-list">'.concat(rows.map(([label, value]) => "<div><dt>".concat(escapeHtml(label), "</dt><dd>").concat(escapeHtml(value), "</dd></div>")).join(""), '</dl>\n        <p class="hint">Values are read locally. Some details may be unavailable in browser preview or restricted by device privacy settings.</p></section>');
}
function enhanceSettingsMasterDetail(settingCategories) {
  const page = main.querySelector(".settings-page");
  const firstCategory = settingCategories[0];
  if (!page || !firstCategory) return null;

  const layout = document.createElement("div");
  layout.className = "settings-layout";
  const navigation = document.createElement("div");
  navigation.className = "settings-category-nav";
  navigation.setAttribute("role", "tablist");
  navigation.setAttribute("aria-label", "Settings categories");
  const panels = document.createElement("div");
  panels.className = "settings-panels";
  const buttons = [];
  const selectedIndex = Math.max(0, settingCategories.findIndex((category) =>
    category.querySelector("summary").textContent.trim() === "Appearance"
  ));

  page.insertBefore(layout, firstCategory);
  layout.appendChild(navigation);
  layout.appendChild(panels);
  settingCategories.forEach((details, index) => {
    const summary = details.querySelector("summary");
    const title = summary ? summary.textContent.trim() : "Settings";
    const panelId = "settings-panel-".concat(index);
    const button = document.createElement("button");
    button.type = "button";
    button.className = "settings-category-button focusable";
    button.textContent = title;
    button.id = "settings-tab-".concat(index);
    button.setAttribute("role", "tab");
    button.setAttribute("aria-controls", panelId);
    button.setAttribute("aria-selected", String(index === selectedIndex));
    details.id = panelId;
    details.setAttribute("role", "tabpanel");
    details.setAttribute("aria-labelledby", button.id);
    if (summary) summary.hidden = true;
    details.classList.add("settings-panel");
    navigation.appendChild(button);
    panels.appendChild(details);
    buttons.push(button);
    button.addEventListener("click", () => {
      setSettingsCategoryOpen(details, true);
      buttons.forEach((item, itemIndex) => item.setAttribute("aria-selected", String(itemIndex === index)));
      button.focus();
    });
  });

  const selected = settingCategories[selectedIndex];
  if (selected) setSettingsCategoryOpen(selected, true);
  return buttons[selectedIndex] || null;
}

function renderSettings() {
  var _a;
  state.page = "settings";
  navState("settings");
  const credentialSettings = '<p class="config-status">TMDB: '.concat(hasConfiguredTmdbCredentials ? "Loaded from config.cfg" : "Missing from config.cfg", "<br>SubDL: ").concat(hasConfiguredSubdlKey ? "Loaded from config.cfg" : "Missing from config.cfg", "</p>\n        ").concat(hasConfiguredSubdlKey ? '<div class="action-row"><button class="action-button primary focusable" type="button" data-action="test-subdl">Test SubDL connection</button></div>' : "");
  const deviceInfo = renderDeviceInfo();
  main.innerHTML = '<section class="page settings-page"><div class="page-title"><h1>Settings</h1><p class="muted">Configure catalog access on this TV</p></div>\n        <details class="settings-card"><summary class="focusable" tabindex="0"><h2>Catalog & playback</h2></summary>\n        <p class="hint">Catalog and subtitle services use the project config.cfg loaded at build or preview startup. Credentials are not entered or changed on this TV.</p>\n        '.concat(credentialSettings, '\n        </details>\n        <details class="settings-card"><summary class="focusable" tabindex="0"><h2>Appearance</h2></summary><div class="settings-grid">\n        <label class="setting-control">Theme<select id="pref-theme" class="focusable"><option>Dark</option><option>Light</option><option>System</option></select></label>\n        <label class="setting-control">Accent color<input id="pref-color" class="focusable" type="color"></label>\n        <label class="setting-control">Secondary color<input id="pref-secondary-color" class="focusable" type="color"></label>\n        <label class="setting-control">Third color<input id="pref-tertiary-color" class="focusable" type="color"></label>\n        <label class="setting-control">Button fill color<input id="pref-button-fill" class="focusable" type="color"></label>\n        <label class="setting-control">Button text color<input id="pref-button-text" class="focusable" type="color"></label>\n        <label class="setting-control">Button border color<input id="pref-button-border" class="focusable" type="color"></label>\n        <label class="setting-control">Button shape<select id="pref-shape" class="focusable"><option>Rounded</option><option>Pill</option><option>Square</option></select></label>\n        </div></details>\n        <details class="settings-card"><summary class="focusable" tabindex="0"><h2>User interface</h2></summary><div class="settings-grid">\n        <label class="setting-control">Interface mode<select id="pref-interface" class="focusable"><option value="TV">TV</option><option value="AUTO">Auto</option><option value="PHONE">Phone</option></select></label>\n        <label class="setting-control">Navigation rail<select id="pref-nav-collapse" class="focusable"><option value="true">Compact</option><option value="false">Expanded</option></select></label>\n        <label class="setting-control">Menu labels<select id="pref-menu-labels" class="focusable"><option value="false">Hide labels</option><option value="true">Show labels</option></select></label>\n        </div></details>\n        <details class="settings-card"><summary class="focusable" tabindex="0"><h2>Subtitles</h2></summary><div class="settings-grid">\n        <label class="setting-control">Subtitle language<select id="pref-subtitle-country" class="focusable">').concat(window.ECHO_SUBTITLE_COUNTRIES.map((country) => '<option value="'.concat(country.code, '" data-language="').concat(country.language, '">').concat(escapeHtml(country.name), "</option>")).join(""), '</select></label>\n        <label class="setting-control">Subtitle text size<input id="pref-subtitle-size" class="focusable" type="number" min="14" max="36"></label>\n        <label class="setting-control">Subtitle vertical position (%)<input id="pref-subtitle-position" class="focusable" type="range" min="0" max="25" step="1"></label>\n        <label class="setting-control">Subtitle timing offset (ms)<input id="pref-subtitle-offset" class="focusable" type="number" min="-10000" max="10000" step="250"></label>\n        <label class="setting-control">Subtitle text color<input id="pref-subtitle-text" class="focusable" type="color"></label>\n        <label class="setting-control">Subtitle box color<input id="pref-subtitle-background" class="focusable" type="color"></label>\n        <label class="setting-control">Subtitle font<select id="pref-subtitle-font" class="focusable">').concat(["Arial", "Verdana", "Georgia", "Tahoma", "Courier New"].map((font) => "<option>".concat(font, "</option>")).join(""), '</select></label>\n        <label class="setting-control">Subtitle box transparency (%)<input id="pref-subtitle-transparency" class="focusable" type="number" min="0" max="100"></label>\n        <label class="setting-control">Subtitle box<select id="pref-subtitle-box" class="focusable"><option value="true">On</option><option value="false">Off</option></select></label>\n        <label class="setting-control">Subtitle auto-apply<select id="pref-auto-subtitles" class="focusable"><option value="true">On</option><option value="false">Off</option></select></label>\n        </div><div class="settings-subtitle-preview"><span>Preview</span><div class="settings-subtitle-preview-screen"><span class="settings-subtitle-preview-text">This is a preview of your subtitle settings.</span></div></div></details>\n        <details class="settings-card"><summary class="focusable" tabindex="0"><h2>Date, time & language</h2></summary><div class="settings-grid">\n        <label class="setting-control">Show clock on home<select id="pref-clock" class="focusable"><option value="true">On</option><option value="false">Off</option></select></label>\n        <label class="setting-control">Show clock on other pages<select id="pref-clock-other" class="focusable"><option value="true">On</option><option value="false">Off</option></select></label>\n        <label class="setting-control">Clock format<select id="pref-clock-format" class="focusable"><option value="false">12-hour</option><option value="true">24-hour</option></select></label>\n        <label class="setting-control">Clock mode<select id="pref-clock-mode" class="focusable"><option value="AUTO">Automatic</option><option value="MANUAL">Manual</option></select></label>\n        <label class="setting-control">Manual clock value<input id="pref-clock-manual" class="focusable" type="text" placeholder="e.g. 8:30 PM"></label>\n        <label class="setting-control">Time zone<input id="pref-timezone" class="focusable" type="text" placeholder="AUTO or Europe/London"></label>\n        <label class="setting-control">Date/time display pattern<input id="pref-clock-pattern" class="focusable" type="text"></label>\n        <label class="setting-control">Clock text color<input id="pref-clock-color" class="focusable" type="color"></label>\n        <label class="setting-control">Clock size<input id="pref-clock-size" class="focusable" type="number" min="10" max="28"></label>\n        <label class="setting-control">Language<input id="pref-language" class="focusable" type="text" placeholder="SYSTEM or en-US"></label>\n        </div></details>\n        <details class="settings-card"><summary class="focusable" tabindex="0"><h2>Device</h2></summary><div class="settings-grid">\n        <label class="setting-control">Device name<input id="pref-device-name" class="focusable" type="text" maxlength="40"></label>\n        </div>').concat(deviceInfo, '<p class="hint">This Tizen build does not include Android\'s companion-device pairing service.</p></details>\n        <details class="settings-card"><summary class="focusable" tabindex="0"><h2>Subtitle providers</h2></summary><p class="hint">SubDL is available in the player. OpenSubtitles service sign-in is not supported in this Tizen build.</p></details>\n        <details class="settings-card"><summary class="focusable" tabindex="0"><h2>Application</h2></summary><p>').concat(escapeHtml(appName), " \xB7 version ").concat(escapeHtml(appVersion), ' \xB7 Samsung Tizen TV</p>\n        <p>Developer: Antonio Martinovi\u0107 @ ShadyDevelopment</p><p>GitHub: https://github.com/ShadyDevelopment</p>\n        <div class="action-row"><button class="action-button focusable" data-action="check-updates">Check for updates</button>\n        <button class="action-button focusable" data-action="reset-settings">Factory reset</button></div></details></section>');
  const subtitlePreview = main.querySelector(".settings-subtitle-preview");
  const subtitleCategory = Array.from(main.querySelectorAll("details.settings-card")).find((category) =>
    category.querySelector("summary").textContent.trim() === "Subtitles"
  );
  if (subtitlePreview && subtitleCategory) {
    subtitleCategory.insertBefore(subtitlePreview, subtitleCategory.firstChild);
  }
  const settingCategories = Array.from(main.querySelectorAll("details.settings-card"));
  settingCategories.forEach((details) => {
    const summary = details.querySelector("summary");
    const syncExpanded = () => summary == null ? void 0 : summary.setAttribute("aria-expanded", String(details.open));
    details.addEventListener("toggle", () => {
      if (details.open) {
        settingCategories.forEach((other) => {
          var _a2;
          if (other === details || !other.open) return;
          other.open = false;
          (_a2 = other.querySelector("summary")) == null ? void 0 : _a2.setAttribute("aria-expanded", "false");
        });
      }
      syncExpanded();
    });
    syncExpanded();
  });
  const initialCategoryButton = enhanceSettingsMasterDetail(settingCategories);
  document.getElementById("pref-theme").value = state.preferences.theme;
  document.getElementById("pref-color").value = state.preferences.primaryColor;
  document.getElementById("pref-secondary-color").value = state.preferences.secondaryColor;
  document.getElementById("pref-tertiary-color").value = state.preferences.tertiaryColor;
  document.getElementById("pref-button-fill").value = state.preferences.buttonFillColor || state.preferences.primaryColor;
  document.getElementById("pref-button-text").value = state.preferences.buttonTextColor || "#ffffff";
  document.getElementById("pref-button-border").value = state.preferences.buttonBorderColor || state.preferences.primaryColor;
  document.getElementById("pref-shape").value = state.preferences.buttonShape;
  document.getElementById("pref-interface").value = state.preferences.interfaceMode;
  document.getElementById("pref-nav-collapse").value = String(state.preferences.navigationBarCollapsed);
  document.getElementById("pref-menu-labels").value = String(state.preferences.showMenuLabels);
  const subtitleCountry = window.ECHO_SUBTITLE_COUNTRIES.find((country) => country.code === state.preferences.subtitleCountry) || window.ECHO_SUBTITLE_COUNTRIES.find((country) => country.language === state.preferences.subtitleLanguage) || window.ECHO_SUBTITLE_COUNTRIES[0];
  document.getElementById("pref-subtitle-country").value = subtitleCountry.code;
  document.getElementById("pref-subtitle-size").value = state.preferences.subtitleSize;
  document.getElementById("pref-subtitle-position").value = Math.max(0, Math.min(25, Number(state.preferences.subtitleVerticalPosition) || 0));
  document.getElementById("pref-subtitle-offset").value = state.preferences.subtitleOffset;
  document.getElementById("pref-subtitle-text").value = state.preferences.subtitleText;
  document.getElementById("pref-subtitle-background").value = state.preferences.subtitleBackground;
  document.getElementById("pref-subtitle-font").value = state.preferences.subtitleFont;
  document.getElementById("pref-subtitle-transparency").value = state.preferences.subtitleBoxTransparency;
  document.getElementById("pref-subtitle-box").value = String(state.preferences.subtitleBoxEnabled);
  document.getElementById("pref-auto-subtitles").value = String(state.preferences.autoApplySubtitles);
  document.getElementById("pref-clock").value = String(state.preferences.showClockOnHome);
  document.getElementById("pref-clock-other").value = String(state.preferences.showClockOnOtherPages);
  document.getElementById("pref-clock-format").value = String(state.preferences.timeFormat24Hour);
  document.getElementById("pref-clock-mode").value = state.preferences.clockMode;
  document.getElementById("pref-clock-manual").value = state.preferences.manualClockValue;
  document.getElementById("pref-timezone").value = state.preferences.timeZoneId;
  document.getElementById("pref-clock-pattern").value = state.preferences.clockPattern;
  document.getElementById("pref-clock-color").value = state.preferences.clockColor;
  document.getElementById("pref-clock-size").value = state.preferences.clockSize;
  document.getElementById("pref-language").value = state.preferences.languageTag;
  document.getElementById("pref-device-name").value = state.preferences.deviceName;
  (_a = initialCategoryButton) == null ? void 0 : _a.focus();
}
