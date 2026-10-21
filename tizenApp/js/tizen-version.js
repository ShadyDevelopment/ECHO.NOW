"use strict";
(function() {
  function detectTizenInfo() {
    var rawVersion = "";
    var isTizen = false;
    if (typeof window.tizen !== "undefined" && window.tizen.systeminfo && typeof window.tizen.systeminfo.getCapability === "function") {
      try {
        rawVersion = String(window.tizen.systeminfo.getCapability("http://tizen.org/feature/platform.version") || "").trim();
        if (rawVersion) isTizen = true;
      } catch (e) {
      }
    }
    var ua = navigator.userAgent || "";
    var tizenUaMatch = ua.match(/Tizen[ /]([0-9]+(?:\.[0-9]+)?)/i);
    if (tizenUaMatch && tizenUaMatch[1]) {
      isTizen = true;
      if (!rawVersion) {
        rawVersion = tizenUaMatch[1];
      }
    } else if (/SmartTV|SMART-TV|Tizen/i.test(ua)) {
      isTizen = true;
    }
    var chromeVersion = 0;
    var chromeMatch = ua.match(/Chrome\/([0-9]+)/i);
    if (chromeMatch && chromeMatch[1]) {
      chromeVersion = parseInt(chromeMatch[1], 10);
    }
    if (isTizen && !rawVersion) {
      if (chromeVersion >= 108) rawVersion = "8.0";
      else if (chromeVersion >= 94) rawVersion = "7.0";
      else if (chromeVersion >= 85) rawVersion = "6.5";
      else if (chromeVersion >= 76) rawVersion = "6.0";
      else if (chromeVersion >= 69) rawVersion = "5.5";
      else if (chromeVersion >= 63) rawVersion = "5.0";
      else rawVersion = "4.0";
    }
    if (!isTizen) {
      rawVersion = chromeVersion >= 90 ? "8.0" : "4.0";
    }
    var parts = (rawVersion || "4.0").split(".");
    var major = parseInt(parts[0], 10) || 4;
    var minor = parseInt(parts[1], 10) || 0;
    if (major < 4) {
      major = 4;
      minor = 0;
      rawVersion = "4.0";
    }
    return {
      isTizen,
      version: rawVersion || "4.0",
      major,
      minor,
      chromeVersion
    };
  }
  var info = detectTizenInfo();
  function isAtLeast(minMajor, minMinor) {
    minMinor = minMinor || 0;
    if (info.major > minMajor) return true;
    if (info.major === minMajor && info.minor >= minMinor) return true;
    return false;
  }
  var isLegacy = !isAtLeast(5, 5);
  var isModern = isAtLeast(5, 5);
  var isNextGen = isAtLeast(7, 0);
  var performanceTier = "low";
  if (isAtLeast(7, 0)) {
    performanceTier = "ultra";
  } else if (isAtLeast(6, 0)) {
    performanceTier = "high";
  } else if (isAtLeast(5, 0)) {
    performanceTier = "medium";
  }
  var supportsBackdropFilter = false;
  try {
    supportsBackdropFilter = isModern && typeof window.CSS !== "undefined" && typeof window.CSS.supports === "function" && (window.CSS.supports("backdrop-filter", "blur(10px)") || window.CSS.supports("-webkit-backdrop-filter", "blur(10px)"));
  } catch (_) {
    supportsBackdropFilter = false;
  }
  var supportsFlexGap = false;
  try {
    supportsFlexGap = (info.chromeVersion >= 84 || isAtLeast(6, 5)) && typeof window.CSS !== "undefined" && typeof window.CSS.supports === "function" && window.CSS.supports("gap", "10px");
  } catch (_) {
    supportsFlexGap = false;
  }
  var features = {
    // Tizen 4.0 Baseline
    basicPlayer: true,
    legacyFlexFallback: !supportsFlexGap,
    reducedMemoryMode: isLegacy,
    // Tizen 5.0+ Enhancements
    enhancedBuffering: isAtLeast(5, 0),
    // Tizen 5.5+ Enhancements
    smoothScroll: isAtLeast(5, 5),
    asyncImageDecode: isAtLeast(5, 5),
    backdropFilter: supportsBackdropFilter,
    // Tizen 6.0+ Modern Smart TV Features
    highResArtwork: isAtLeast(6, 0),
    fluidAnimations: isAtLeast(6, 0),
    nativeFlexGap: supportsFlexGap,
    webAnimations: isAtLeast(6, 0) && typeof Element !== "undefined" && typeof Element.prototype.animate === "function",
    fastCatalogCache: isAtLeast(6, 0),
    // Tizen 7.0+ Ultra TV Features
    ultraHighResArtwork: isAtLeast(7, 0),
    modernAudioCaps: isAtLeast(7, 0),
    performanceTier
  };
  window.TIZEN_DEVICE = {
    isTizen: info.isTizen,
    version: info.version,
    major: info.major,
    minor: info.minor,
    chromeVersion: info.chromeVersion,
    isLegacy,
    isModern,
    isNextGen,
    performanceTier,
    features,
    isAtLeast,
    // Dynamic helper functions for components
    getOptimizedPosterSize: function() {
      return features.highResArtwork ? "w500" : isLegacy ? "w342" : "w500";
    },
    getOptimizedBackdropSize: function() {
      if (features.ultraHighResArtwork) return "w1280";
      if (features.highResArtwork || isModern) return "w1280";
      return "w780";
    },
    getRecommendedBufferingParams: function() {
      if (isAtLeast(6, 0)) {
        return { initialPlay: 6, resume: 4 };
      } else if (isAtLeast(5, 0)) {
        return { initialPlay: 4, resume: 3 };
      }
      return { initialPlay: 2, resume: 2 };
    }
  };
  function applyDocumentClasses() {
    var root = document.documentElement;
    if (!root) return;
    root.classList.add("tizen-v" + info.major);
    root.classList.add(isLegacy ? "tizen-legacy" : "tizen-modern");
    if (isNextGen) root.classList.add("tizen-nextgen");
    root.classList.add("tizen-tier-" + performanceTier);
    if (features.backdropFilter) root.classList.add("has-backdrop-filter");
    if (features.smoothScroll) root.classList.add("has-smooth-scroll");
    if (features.nativeFlexGap) root.classList.add("has-flex-gap");
    if (features.highResArtwork) root.classList.add("high-res-ui");
    if (features.fluidAnimations) root.classList.add("has-fluid-motion");
  }
  function updateProportionalScale() {
    var winW = window.innerWidth || document.documentElement && document.documentElement.clientWidth || 1920;
    var scale = winW / 1920;
    var clampedScale = Math.max(0.65, Math.min(2.2, scale));
    var root = document.documentElement;
    if (!root) return;
    root.style.setProperty("--ui-scale", clampedScale.toFixed(4));
    var fontPx = Math.max(12, Math.min(26, Math.round(16 * clampedScale)));
    root.style.setProperty("--root-font-size", fontPx + "px");
  }
  window.addEventListener("resize", updateProportionalScale);
  updateProportionalScale();
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", function() {
      applyDocumentClasses();
      updateProportionalScale();
    });
  } else {
    applyDocumentClasses();
    updateProportionalScale();
  }
  console.log("[ECHO Tizen Engine] Detected:", {
    platform: info.isTizen ? "Samsung Tizen TV" : "Web Preview",
    tizenVersion: info.version,
    chromiumVersion: info.chromeVersion,
    tier: performanceTier,
    backdropFilter: features.backdropFilter,
    smoothScroll: features.smoothScroll,
    flexGap: features.nativeFlexGap,
    highResArt: features.highResArtwork
  });
})();
