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
function isPlayerPlaying() {
  var _a;
  if (isAvplayActive && ((_a = window.webapis) == null ? void 0 : _a.avplay)) {
    try {
      return webapis.avplay.getState() === "PLAYING";
    } catch (_) {
    }
  }
  const video = document.getElementById("video-player");
  return Boolean(video && !video.paused);
}
function playerPlay() {
  var _a;
  if (isAvplayActive && ((_a = window.webapis) == null ? void 0 : _a.avplay)) {
    try {
      webapis.avplay.play();
      updatePlayerPlayButton();
      return;
    } catch (_) {
    }
  }
  const video = document.getElementById("video-player");
  if (video) {
    const p = video.play();
    if (p !== void 0) {
      p.then(() => updatePlayerPlayButton()).catch((err) => {
        console.warn("Autoplay deferred or prevented:", err.message);
        updatePlayerPlayButton();
      });
    }
  }
}
function playerPause() {
  var _a;
  if (isAvplayActive && ((_a = window.webapis) == null ? void 0 : _a.avplay)) {
    try {
      webapis.avplay.pause();
      updatePlayerPlayButton();
      return;
    } catch (_) {
    }
  }
  const video = document.getElementById("video-player");
  if (video) {
    video.pause();
    updatePlayerPlayButton();
  }
}
function playerTogglePlay() {
  if (isPlayerPlaying()) {
    playerPause();
  } else {
    playerPlay();
  }
}
function playerCurrentTime() {
  var _a;
  if (isAvplayActive && ((_a = window.webapis) == null ? void 0 : _a.avplay)) {
    try {
      const ms = webapis.avplay.getCurrentTime();
      if (Number.isFinite(ms) && ms >= 0) return ms / 1e3;
    } catch (_) {
    }
  }
  const video = document.getElementById("video-player");
  return video && Number.isFinite(video.currentTime) && video.currentTime >= 0 ? video.currentTime : 0;
}
function playerDuration() {
  var _a;
  if (isAvplayActive && ((_a = window.webapis) == null ? void 0 : _a.avplay)) {
    try {
      const ms = webapis.avplay.getDuration();
      if (Number.isFinite(ms) && ms > 0) return ms / 1e3;
    } catch (_) {
    }
  }
  const video = document.getElementById("video-player");
  return video && Number.isFinite(video.duration) && video.duration > 0 ? video.duration : 0;
}
function playerSeekTo(seconds) {
  var _a;
  const dur = playerDuration();
  const clamped = Math.max(0, dur > 0 ? Math.min(dur - 0.5, seconds) : seconds);
  if (isAvplayActive && ((_a = window.webapis) == null ? void 0 : _a.avplay)) {
    try {
      webapis.avplay.seekTo(Math.round(clamped * 1e3));
      updateTimeDisplay(clamped, dur);
      return;
    } catch (_) {
    }
  }
  const video = document.getElementById("video-player");
  if (video) {
    try {
      video.currentTime = clamped;
    } catch (_) {
    }
    updateTimeDisplay(clamped, dur);
  }
}
function playerSeek(deltaSeconds) {
  playerSeekTo(playerCurrentTime() + deltaSeconds);
}
let stallCheckInterval = null;
function startStallMonitor() {
  stopStallMonitor();
  stallCheckInterval = setInterval(() => {
    if (!isPlayerPlaying()) return;
    const video = document.getElementById("video-player");
    if (!video || !video.buffered || video.buffered.length === 0) return;
    if (video.currentTime === 0 || video.currentTime < video.buffered.start(0) - 0.05) {
      const start = video.buffered.start(0);
      if (start > 0.02 && start < 4) {
        console.log("[Player] PTS stall detected at 0:00, advancing to", start);
        try {
          video.currentTime = start + 0.04;
        } catch (_) {
        }
      }
    }
  }, 1e3);
}
function stopStallMonitor() {
  if (stallCheckInterval) {
    clearInterval(stallCheckInterval);
    stallCheckInterval = null;
  }
}
function cleanupAvplayTransparency() {
  document.documentElement.classList.remove("avplay-active");
  document.body.classList.remove("avplay-active");
  const appShell = document.querySelector(".app-shell");
  if (appShell) appShell.classList.remove("avplay-active");
  const mainEl = document.getElementById("main");
  if (mainEl) mainEl.classList.remove("avplay-active");
  const playerShell = document.querySelector(".player-shell");
  if (playerShell) playerShell.classList.remove("avplay-active");
}
function applyAvplayTransparency() {
  document.documentElement.classList.add("avplay-active");
  document.body.classList.add("avplay-active");
  const appShell = document.querySelector(".app-shell");
  if (appShell) appShell.classList.add("avplay-active");
  const mainEl = document.getElementById("main");
  if (mainEl) mainEl.classList.add("avplay-active");
  const playerShell = document.querySelector(".player-shell");
  if (playerShell) playerShell.classList.add("avplay-active");
}
function stopCurrentPlayback() {
  var _a;
  stopStallMonitor();
  cleanupAvplayTransparency();
  if (activeHls) {
    try {
      activeHls.destroy();
    } catch (_) {
    }
    activeHls = null;
  }
  if ((_a = window.webapis) == null ? void 0 : _a.avplay) {
    try {
      const s = webapis.avplay.getState();
      if (s !== "NONE") {
        if (s === "PLAYING" || s === "PAUSED") {
          try {
            webapis.avplay.stop();
          } catch (_) {
          }
        }
        try {
          webapis.avplay.close();
        } catch (_) {
        }
      }
    } catch (_) {
    }
    isAvplayActive = false;
  }
  const video = document.getElementById("video-player");
  if (video) {
    try {
      video.pause();
      video.removeAttribute("src");
      video.load();
    } catch (_) {
    }
  }
}
function setPlayerControlsVisible(visible, autoHide = false) {
  var _a;
  const controls = document.getElementById("player-controls");
  if (!controls) return;
  controls.classList.toggle("player-controls-visible", visible);
  (_a = controls.closest(".player-shell")) == null ? void 0 : _a.classList.toggle("player-controls-active", visible);
  clearTimeout(state.playerTimer);
  state.playerTimer = null;
  if (visible && autoHide && isPlayerPlaying()) {
    state.playerTimer = setTimeout(() => {
      var _a2;
      controls.classList.remove("player-controls-visible");
      const shell = controls.closest(".player-shell");
      shell == null ? void 0 : shell.classList.remove("player-controls-active");
      state.playerTimer = null;
      renderSkipActions(playerCurrentTime() * 1e3, playerDuration() * 1e3);
      if ((_a2 = document.activeElement) == null ? void 0 : _a2.closest("#player-controls")) shell == null ? void 0 : shell.focus();
    }, 5e3);
  }
}
function updatePlayerPlayButton() {
  const button = document.querySelector('[data-action="toggle-play"]');
  if (!button) return;
  const playing = isPlayerPlaying();
  button.setAttribute("aria-label", playing ? "Pause" : "Play");
  button.innerHTML = playing ? '<svg viewBox="0 0 24 24" id="play-pause-icon"><path d="M8 19c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2s-2 .9-2 2v10c0 1.1.9 2 2 2zm6-12v10c0 1.1.9 2 2 2s2-.9 2-2V7c0-1.1-.9-2-2-2s-2 .9-2 2z"/></svg>' : '<svg viewBox="0 0 24 24" id="play-pause-icon"><path d="M8 6.82v10.36c0 .79.87 1.27 1.54.84l8.14-5.18c.62-.39.62-1.29 0-1.69L9.54 5.98C8.87 5.55 8 6.03 8 6.82z"/></svg>';
}
function updatePlayerAspectRatio() {
  var _a;
  const video = document.getElementById("video-player");
  const button = document.querySelector('[data-action="aspect-ratio"]');
  const modes = ["contain", "cover", "fill"];
  const labels = ["Fit", "Crop", "Stretch"];
  const currentMode = modes[state.playerAspectRatio % modes.length];
  const currentLabel = labels[state.playerAspectRatio % labels.length];
  if (video) video.style.objectFit = currentMode;
  if (isAvplayActive && ((_a = window.webapis) == null ? void 0 : _a.avplay)) {
    try {
      const method = currentMode === "fill" ? "PLAYER_DISPLAY_MODE_FULL_SCREEN" : currentMode === "cover" ? "PLAYER_DISPLAY_MODE_AUTO_ASPECT_RATIO" : "PLAYER_DISPLAY_MODE_LETTER_BOX";
      webapis.avplay.setDisplayMethod(method);
    } catch (_) {
    }
  }
  if (button) button.setAttribute("aria-label", "Screen ratio: ".concat(currentLabel));
}
function updatePlayerFullscreenButton() {
  const shell = document.querySelector(".player-shell");
  const button = document.querySelector('[data-action="toggle-fullscreen"]');
  state.playerFullscreen = Boolean(shell && document.fullscreenElement === shell);
  if (!button || button.hidden) return;
  button.setAttribute("aria-label", state.playerFullscreen ? "Exit fullscreen" : "Enter fullscreen");
  button.title = state.playerFullscreen ? "Exit fullscreen" : "Fullscreen";
}
async function togglePlayerFullscreen() {
  const shell = document.querySelector(".player-shell");
  if (!shell) return;
  if (!document.fullscreenEnabled || !shell.requestFullscreen) {
    throw new Error("Fullscreen is not supported by this browser.");
  }
  if (document.fullscreenElement === shell) {
    if (!document.exitFullscreen) throw new Error("Exiting fullscreen is not supported by this browser.");
    await document.exitFullscreen();
  } else {
    await shell.requestFullscreen();
  }
  updatePlayerFullscreenButton();
}
document.addEventListener("fullscreenchange", updatePlayerFullscreenButton);
function updateTimeDisplay(curTime, totalDur) {
  const cur = Number.isFinite(curTime) ? curTime : playerCurrentTime();
  const dur = Number.isFinite(totalDur) && totalDur > 0 ? totalDur : playerDuration();
  const timeLabel = document.getElementById("player-time-label");
  if (timeLabel) timeLabel.textContent = "".concat(formatTime(cur), " / ").concat(dur > 0 ? formatTime(dur) : "--:--");
  const seek = document.getElementById("player-progress");
  if (seek && dur > 0) seek.value = String(Math.round(cur / dur * 1e3));
}
function savePlaybackProgress(curTime, totalDur) {
  const cur = Number.isFinite(curTime) ? curTime : playerCurrentTime();
  const dur = Number.isFinite(totalDur) && totalDur > 0 ? totalDur : playerDuration();
  const media = state.currentTitle;
  const episode = state.currentEpisode;
  if (!media || !(dur > 0)) return;
  const duration = dur * 1e3;
  const position = cur * 1e3;
  const entryId = episode ? "".concat(media.media_type, ":").concat(media.id, ":").concat(episode.season, ":").concat(episode.episode) : "".concat(media.media_type, ":").concat(media.id);
  const profileProgress = state.progress[profileKey("watchProgress")] || {};
  const record = {
    media,
    title: media.title || media.name,
    season: (episode == null ? void 0 : episode.season) || 0,
    episode: (episode == null ? void 0 : episode.episode) || 0,
    position,
    duration,
    updatedAt: Date.now()
  };
  profileProgress[entryId] = record;
  if (episode) {
    profileProgress["".concat(media.media_type, ":").concat(media.id)] = record;
  }
  state.progress[profileKey("watchProgress")] = profileProgress;
  saveState(profileKey("watchProgress"), profileProgress);
  if (state.nextEpisode && dur > 0) {
    renderUpNext(cur, dur);
  }
  renderSkipActions(position, duration);
}
function handlePlaybackEnded() {
  updatePlayerPlayButton();
  if (state.nextEpisode && state.autoPlayNextEpisode && !state.upNextDismissed) {
    playNextEpisode();
  } else {
    renderUpNext(playerCurrentTime(), playerDuration());
    setPlayerControlsVisible(true, false);
  }
}
function handlePlayerError() {
  stopCurrentPlayback();
  const failedIndex = Number(state.playerSourceIndex);
  if (!state.failedPlayerSources.includes(failedIndex)) state.failedPlayerSources.push(failedIndex);
  const sources = state.playerSources || [];
  const fallbackIndex = sources.findIndex((_, index) => !state.failedPlayerSources.includes(index));
  if (fallbackIndex >= 0) {
    toastMessage("Stream unavailable. Trying ".concat(sources[fallbackIndex].providerName || "", " ").concat(sources[fallbackIndex].quality || "another source", "\u2026"));
    setPlayerSource(fallbackIndex, Number(state.targetPlaybackPosition || 0));
  } else {
    showPlayerResolutionError("All returned sources failed to play. Check provider availability and try again.");
  }
}
function playDirectMedia(url, position = 0) {
  const video = document.getElementById("video-player");
  if (!video) return;
  try {
    video.pause();
    video.removeAttribute("src");
    video.load();
  } catch (_) {
  }
  video.src = url;
  video.dataset.targetPosition = String(position);
  const onMeta = () => {
    if (position > 0 && Number.isFinite(position) && position < (video.duration || Infinity)) {
      try {
        video.currentTime = position;
      } catch (_) {
      }
    }
    const p = video.play();
    if (p !== void 0) {
      p.then(() => {
        updatePlayerPlayButton();
        const b = document.getElementById("player-buffering");
        if (b) b.hidden = true;
      }).catch((err) => {
        console.warn("Direct video play deferred:", err.message);
        updatePlayerPlayButton();
      });
    }
  };
  video.addEventListener("loadedmetadata", onMeta, { once: true });
  video.load();
}
function playWithAvplay(source, position = 0) {
  var _a, _b, _c;
  if (!((_a = window.webapis) == null ? void 0 : _a.avplay)) return false;
  const avplay = window.webapis.avplay;
  const video = document.getElementById("video-player");
  const bufferingEl = document.getElementById("player-buffering");
  try {
    stopCurrentPlayback();
    if (video) {
      video.style.display = "none";
      video.removeAttribute("src");
    }
    if (bufferingEl) bufferingEl.hidden = false;
    applyAvplayTransparency();
    avplay.open(source.url);
    try {
      avplay.setDisplayRect(0, 0, 1920, 1080);
    } catch (e) {
      console.warn("AVPlay setDisplayRect in IDLE failed:", e);
    }
    try {
      avplay.setDisplayMethod("PLAYER_DISPLAY_MODE_LETTER_BOX");
    } catch (_) {
    }
    if (source.headers) {
      const ua = source.headers["User-Agent"] || source.headers["user-agent"];
      if (ua) {
        try {
          avplay.setStreamingProperty("USER_AGENT", ua);
        } catch (_) {
        }
      }
      const cookie = source.headers["Cookie"] || source.headers["cookie"];
      if (cookie) {
        try {
          avplay.setStreamingProperty("COOKIE", cookie);
        } catch (_) {
        }
      }
      const customHeaders = [];
      for (const [k, v] of Object.entries(source.headers)) {
        if (/^(user-agent|cookie)$/i.test(k)) continue;
        customHeaders.push("".concat(k, ": ").concat(v));
      }
      if (customHeaders.length > 0) {
        try {
          avplay.setStreamingProperty("CUSTOM_MESSAGE", customHeaders.join("\r\n") + "\r\n");
        } catch (_) {
        }
      }
    }
    try {
      avplay.setStreamingProperty("ADAPTIVE_INFO", "BITRATES=500000~25000000|STARTBITRATE=HIGHEST|SKIPBITRATE=LOWEST");
    } catch (_) {
    }
    try {
      const buf = window.TIZEN_DEVICE && typeof window.TIZEN_DEVICE.getRecommendedBufferingParams === "function" ? window.TIZEN_DEVICE.getRecommendedBufferingParams() : { initialPlay: 2, resume: 2 };
      if (typeof avplay.setBufferingParamWithMode === "function") {
        avplay.setBufferingParamWithMode(false, "PLAYER_BUFFER_FOR_PLAY", buf.initialPlay);
        avplay.setBufferingParamWithMode(false, "PLAYER_BUFFER_FOR_RESUME", buf.resume);
      }
    } catch (_) {
    }
    avplay.setListener({
      onbufferingstart: () => {
        const b = document.getElementById("player-buffering");
        if (b) b.hidden = false;
      },
      onbufferingcomplete: () => {
        const b = document.getElementById("player-buffering");
        if (b) b.hidden = true;
        updatePlayerPlayButton();
      },
      oncurrentplaytime: (timeMs) => {
        const cur = timeMs / 1e3;
        let dur = 0;
        try {
          dur = avplay.getDuration() / 1e3;
        } catch (_) {
        }
        updateTimeDisplay(cur, dur);
        savePlaybackProgress(cur, dur);
        renderAvplaySubtitle(cur);
      },
      onerror: (eventType) => {
        console.warn("AVPlay hardware error:", eventType, "falling back to Hls.js\u2026");
        cleanupAvplayTransparency();
        isAvplayActive = false;
        try {
          avplay.close();
        } catch (_) {
        }
        playWithHls(source, position);
      },
      onstreamcompleted: () => {
        handlePlaybackEnded();
      }
    });
    avplay.prepareAsync(() => {
      isAvplayActive = true;
      applyAvplayTransparency();
      try {
        avplay.setDisplayRect(0, 0, 1920, 1080);
      } catch (e) {
        console.warn("AVPlay setDisplayRect in READY failed:", e);
      }
      try {
        updatePlayerAspectRatio();
      } catch (e) {
      }
      if (position > 0) {
        try {
          avplay.seekTo(Math.round(position * 1e3));
        } catch (_) {
        }
      }
      try {
        avplay.play();
      } catch (e) {
        console.warn("AVPlay play() failed:", e);
      }
      if (state.preferences.autoApplySubtitles) autoLoadPreferredSubtitle(state.subtitleLoadRequestId);
      renderAvplaySubtitle(playerCurrentTime());
      if (bufferingEl) bufferingEl.hidden = true;
      updatePlayerPlayButton();
      setPlayerControlsVisible(true, true);
    }, (err) => {
      console.warn("AVPlay prepareAsync error:", err, "falling back to Hls.js\u2026");
      cleanupAvplayTransparency();
      isAvplayActive = false;
      try {
        avplay.close();
      } catch (_) {
      }
      playWithHls(source, position);
    });
    return true;
  } catch (err) {
    console.warn("AVPlay open failed:", err, "falling back to Hls.js\u2026");
    cleanupAvplayTransparency();
    isAvplayActive = false;
    try {
      (_c = (_b = window.webapis) == null ? void 0 : _b.avplay) == null ? void 0 : _c.close();
    } catch (_) {
    }
    return false;
  }
}
function getPlayableStreamUrl(source) {
  var _a;
  let url = source.url;
  if (typeof window !== "undefined" && window.location && window.location.protocol.startsWith("http") && !((_a = window.webapis) == null ? void 0 : _a.avplay)) {
    const headers = source.headers || {};
    if (headers.Referer || headers.referer || headers.Origin || headers.origin) {
      const origin = window.location.origin;
      let proxyUrl = "".concat(origin, "/api/proxy?url=").concat(encodeURIComponent(url));
      const ref = headers.Referer || headers.referer;
      if (ref) proxyUrl += "&referer=".concat(encodeURIComponent(ref));
      const orig = headers.Origin || headers.origin;
      if (orig) proxyUrl += "&origin=".concat(encodeURIComponent(orig));
      return proxyUrl;
    }
  }
  return url;
}
function playWithHls(source, position = 0) {
  const video = document.getElementById("video-player");
  const bufferingEl = document.getElementById("player-buffering");
  if (!video) return;
  video.style.display = "block";
  if (bufferingEl) bufferingEl.hidden = false;
  if (activeHls) {
    try {
      activeHls.destroy();
    } catch (_) {
    }
    activeHls = null;
  }
  const isHls = Boolean(
    source.isM3u8 || source.format === "hls" || /\.m3u8($|\?)/i.test(source.url)
  );
  let streamUrl = getPlayableStreamUrl(source);
  if (!isHls || !window.Hls && video.canPlayType("application/vnd.apple.mpegurl")) {
    playDirectMedia(streamUrl, position);
    return;
  }
  if (window.Hls && Hls.isSupported()) {
    let mediaRecoveryAttempts = 0;
    let networkRecoveryAttempts = 0;
    let englishAudioApplied = false;
    try {
      activeHls = new Hls({
        enableWorker: false,
        autoStartLoad: true,
        startPosition: position > 0 ? position : -1,
        lowLatencyMode: false,
        capLevelToPlayerSize: false,
        startLevel: -1,
        maxBufferLength: 30,
        maxMaxBufferLength: 60,
        maxBufferSize: 60 * 1e3 * 1e3,
        backBufferLength: 20,
        maxBufferHole: 0.5,
        nudgeOffset: 0.2,
        nudgeMaxRetry: 10,
        fragLoadingTimeOut: 25e3,
        manifestLoadingTimeOut: 2e4,
        levelLoadingTimeOut: 2e4,
        fragLoadingMaxRetry: 6,
        levelLoadingMaxRetry: 5,
        defaultAudioCodec: "mp4a.40.2",
        xhrSetup: function(xhr) {
          if (source.headers) {
            for (const [k, v] of Object.entries(source.headers)) {
              if (/^(referer|origin|user-agent|cookie|host)$/i.test(k)) continue;
              try {
                xhr.setRequestHeader(k, v);
              } catch (_) {
              }
            }
          }
        }
      });
      activeHls.attachMedia(video);
      activeHls.on(Hls.Events.MEDIA_ATTACHED, () => {
        activeHls.loadSource(streamUrl);
      });
      const applyEnglishAudio = () => {
        if (englishAudioApplied || !activeHls || !activeHls.audioTracks || activeHls.audioTracks.length <= 1) return;
        const engIdx = activeHls.audioTracks.findIndex(
          (t) => t.lang && t.lang.toLowerCase().startsWith("en") || t.name && /english/i.test(t.name)
        );
        if (engIdx >= 0 && engIdx !== activeHls.audioTrack) {
          englishAudioApplied = true;
          console.log("Switching to English audio track:", engIdx, activeHls.audioTracks[engIdx].name);
          try {
            activeHls.audioTrack = engIdx;
          } catch (_) {
          }
        }
      };
      activeHls.on(Hls.Events.MANIFEST_PARSED, () => {
        const playPromise = video.play();
        if (playPromise !== void 0) {
          playPromise.then(() => {
            startStallMonitor();
            updatePlayerPlayButton();
            if (bufferingEl) bufferingEl.hidden = true;
          }).catch((error) => {
            console.warn("Autoplay deferred or prevented:", error.message);
            updatePlayerPlayButton();
          });
        }
      });
      activeHls.on(Hls.Events.FRAG_BUFFERED, () => {
        if (bufferingEl) bufferingEl.hidden = true;
        applyEnglishAudio();
        if (video.currentTime === 0 && video.buffered.length > 0) {
          const start = video.buffered.start(0);
          if (start > 0.02 && start < 4) {
            console.log("Nudging past initial PTS gap to", start);
            try {
              video.currentTime = start + 0.04;
            } catch (_) {
            }
          }
        }
      });
      activeHls.on(Hls.Events.ERROR, (event, data) => {
        var _a, _b, _c;
        console.warn("HLS error:", data.type, data.details, data.fatal);
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              networkRecoveryAttempts++;
              if (networkRecoveryAttempts <= 3) {
                console.warn("HLS network error, retrying startLoad\u2026");
                activeHls.startLoad();
              } else if (!streamUrl.includes("/api/proxy") && typeof window !== "undefined" && ((_a = window.location) == null ? void 0 : _a.protocol.startsWith("http"))) {
                console.warn("HLS network error, attempting dev proxy failover\u2026");
                streamUrl = "".concat(window.location.origin, "/api/proxy?url=").concat(encodeURIComponent(source.url), "&referer=").concat(encodeURIComponent(((_b = source.headers) == null ? void 0 : _b.Referer) || ((_c = source.headers) == null ? void 0 : _c.referer) || ""));
                activeHls.loadSource(streamUrl);
                networkRecoveryAttempts = 0;
              } else {
                console.error("HLS network recovery exceeded, advancing to next stream\u2026");
                handlePlayerError();
              }
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              mediaRecoveryAttempts++;
              if (mediaRecoveryAttempts === 1) {
                console.warn("HLS media error, recovering\u2026");
                activeHls.recoverMediaError();
              } else if (mediaRecoveryAttempts === 2) {
                console.warn("HLS media error, swapping audio codec\u2026");
                activeHls.swapAudioCodec();
                activeHls.recoverMediaError();
              } else {
                console.error("HLS media recovery exceeded, advancing to next stream\u2026");
                handlePlayerError();
              }
              break;
            default:
              console.error("HLS unrecoverable error, advancing to next stream\u2026");
              handlePlayerError();
              break;
          }
        }
      });
    } catch (hlsInitErr) {
      console.error("Failed to initialize Hls.js:", hlsInitErr);
      handlePlayerError();
    }
  } else if (video.canPlayType("application/vnd.apple.mpegurl")) {
    playDirectMedia(streamUrl, position);
  } else {
    console.error("Neither Hls.js nor native HLS is supported on this engine");
    handlePlayerError();
  }
}
function setPlayerSource(index, position = 0) {
  var _a;
  const source = state.playerSources[index];
  if (!source) return;
  state.playerSourceIndex = index;
  state.targetPlaybackPosition = position;
  const qualityLabelEl = document.getElementById("player-current-quality");
  if (qualityLabelEl) {
    qualityLabelEl.textContent = source.label || source.quality || "Auto";
  }
  document.querySelectorAll(".quality-menu-item").forEach((item) => {
    item.classList.toggle("active", Number(item.dataset.index) === index);
  });
  const bufferingEl = document.getElementById("player-buffering");
  if (bufferingEl) bufferingEl.hidden = false;
  if ((_a = window.webapis) == null ? void 0 : _a.avplay) {
    const started = playWithAvplay(source, position);
    if (started) return;
  }
  playWithHls(source, position);
}
async function findNextEpisode(media, episode) {
  if (!episode || !media) return null;
  const mediaType = String(media.media_type || media.type || "").toLowerCase();
  const isSeries = mediaType === "tv" || mediaType === "series" || Boolean(media.seasons || media.number_of_seasons || media.first_air_date);
  if (!isSeries) return null;
  const seasons = (media.seasons || []).map((item) => Number(item.season_number)).filter((number) => number > 0).sort((a, b) => a - b);
  const readSeason = async (season) => {
    const cacheKey = "".concat(media.id, ":").concat(season);
    if (!state.episodeCache[cacheKey]) {
      const result = await tmdb("/tv/".concat(media.id, "/season/").concat(season));
      state.episodeCache[cacheKey] = result.episodes || [];
    }
    return state.episodeCache[cacheKey];
  };
  try {
    const currentEpisodes = await readSeason(episode.season);
    const next = currentEpisodes.find((item) => Number(item.episode_number) === Number(episode.episode) + 1);
    if (next) return { season: episode.season, episode: Number(next.episode_number), title: next.name || "", still: next.still_path || "" };
    const nextSeason = seasons.find((number) => number > episode.season);
    if (!nextSeason) return null;
    const nextSeasonEpisodes = await readSeason(nextSeason);
    const first = nextSeasonEpisodes[0];
    return first ? { season: nextSeason, episode: Number(first.episode_number), title: first.name || "", still: first.still_path || "" } : null;
  } catch (error) {
    console.warn("Could not load the next episode:", error.message);
    return null;
  }
}
async function loadEpisodeTiming(media, episode, durationMillis) {
  if (!episode || media.media_type !== "tv") return null;
  const request = async (duration) => {
    const params = new URLSearchParams({
      tmdb_id: String(media.id),
      season: String(episode.season),
      episode: String(episode.episode)
    });
    if (duration > 0) params.set("duration_ms", String(Math.round(duration)));
    const response = await fetch("https://api.theintrodb.org/v3/media?".concat(params));
    if (!response.ok) throw new Error("Intro timing request failed (".concat(response.status, ")."));
    return response.json();
  };
  try {
    let result;
    try {
      result = await request(durationMillis);
    } catch (error) {
      if (!(durationMillis > 0)) throw error;
      result = await request(0);
    }
    let intro = (result.intro || []).find((segment) => segment.start_ms != null || segment.end_ms != null);
    let credits = (result.credits || []).find((segment) => segment.start_ms != null);
    if (!intro && !credits && durationMillis > 0) {
      result = await request(0);
      intro = (result.intro || []).find((segment) => segment.start_ms != null || segment.end_ms != null);
      credits = (result.credits || []).find((segment) => segment.start_ms != null);
    }
    const milliseconds = (value) => value == null || !Number.isFinite(Number(value)) ? null : Number(value);
    return {
      introStartMs: milliseconds(intro == null ? void 0 : intro.start_ms),
      introEndMs: milliseconds(intro == null ? void 0 : intro.end_ms),
      creditsStartMs: milliseconds(credits == null ? void 0 : credits.start_ms)
    };
  } catch (error) {
    console.warn("Could not load episode skip timings:", error.message);
    return null;
  }
}
function renderUpNext(positionSeconds, durationSeconds) {
  var _a, _b, _c, _d, _e, _f, _g;
  const overlay = document.getElementById("player-up-next");
  if (!overlay || !state.nextEpisode || durationSeconds <= 0) return;
  const endingWindowStart = Math.max(0, durationSeconds - 10);
  const creditsStart = (_a = state.episodeTiming) == null ? void 0 : _a.creditsStartMs;
  const triggerPosition = Math.max(
    endingWindowStart,
    creditsStart != null && creditsStart < durationSeconds * 1e3 ? creditsStart / 1e3 : endingWindowStart
  ) - 10;
  const trigger = Math.max(0, triggerPosition);
  const secondsRemaining = Math.min(10, Math.max(0, Math.ceil(trigger + 10 - positionSeconds)));
  const shouldShow = positionSeconds >= trigger && !state.upNextDismissed;
  const wasHidden = overlay.hidden;
  if (shouldShow) {
    overlay.hidden = false;
    const image = overlay.querySelector(".up-next-image");
    if (image) {
      const nextStill = state.nextEpisode.still ? imageUrl(state.nextEpisode.still) : "";
      const fallback = imageUrl((_b = state.currentTitle) == null ? void 0 : _b.backdrop_path) || imageUrl((_c = state.currentTitle) == null ? void 0 : _c.poster_path) || "";
      image.src = nextStill || fallback;
    }
    const title = overlay.querySelector("[data-up-next-title]");
    if (title) title.textContent = "Up Next: S".concat(state.nextEpisode.season, ":E").concat(state.nextEpisode.episode);
    const series = overlay.querySelector("[data-up-next-series]");
    if (series) {
      const mediaTitle = ((_d = state.currentTitle) == null ? void 0 : _d.title) || ((_e = state.currentTitle) == null ? void 0 : _e.name) || "";
      series.textContent = "".concat(mediaTitle).concat(state.nextEpisode.title ? " - ".concat(state.nextEpisode.title) : "");
    }
    const countdown = overlay.querySelector("[data-up-next-countdown]");
    if (countdown) countdown.textContent = state.autoPlayNextEpisode ? "Playing in ".concat(secondsRemaining, " seconds") : "Auto-play is off";
    const progressFill = overlay.querySelector(".up-next-progress-bar");
    if (progressFill) {
      const fraction = Math.max(0, Math.min(1, secondsRemaining / 10));
      progressFill.style.width = "".concat((fraction * 100).toFixed(1), "%");
    }
  } else {
    overlay.hidden = true;
  }
  (_f = overlay.closest(".player-shell")) == null ? void 0 : _f.classList.toggle("player-up-next-active", shouldShow);
  renderSkipActions(positionSeconds * 1e3, durationSeconds * 1e3);
  if (shouldShow && wasHidden) (_g = document.querySelector('[data-action="play-next"]')) == null ? void 0 : _g.focus();
  if (shouldShow && secondsRemaining <= 0 && state.autoPlayNextEpisode) {
    playNextEpisode();
  }
}
function playNextEpisode() {
  if (state.advancingToNextEpisode || !state.nextEpisode || !state.currentTitle) return;
  state.advancingToNextEpisode = true;
  startPlayback(state.currentTitle, __spreadProps(__spreadValues({}, state.nextEpisode), { position: 0 }), 0);
}
function renderSkipActions(positionMillis, durationMillis) {
  var _a, _b, _c;
  const introButton = document.getElementById("skip-intro");
  const creditsButton = document.getElementById("skip-credits");
  const timing = state.episodeTiming;
  if (introButton) {
    const start = timing == null ? void 0 : timing.introStartMs;
    const end = timing == null ? void 0 : timing.introEndMs;
    const visible = start != null && end != null && start <= end && positionMillis >= start && positionMillis < Math.min(end, start + 1e4);
    introButton.hidden = !visible;
  }
  if (creditsButton) {
    const start = timing == null ? void 0 : timing.creditsStartMs;
    creditsButton.hidden = start == null || positionMillis < start || !durationMillis;
  }
  const skipButton = introButton && !introButton.hidden ? introButton : creditsButton && !creditsButton.hidden ? creditsButton : null;
  if (!skipButton) {
    state.focusedSkipAction = "";
  } else if (!((_a = document.getElementById("player-controls")) == null ? void 0 : _a.classList.contains("player-controls-visible")) && state.focusedSkipAction !== skipButton.id && (((_b = document.activeElement) == null ? void 0 : _b.closest("#player-controls")) || ((_c = document.activeElement) == null ? void 0 : _c.matches(".player-shell")))) {
    skipButton.focus();
    state.focusedSkipAction = skipButton.id;
  }
}
function parseSubtitleCues(text) {
  const parseTime = (value) => {
    const match = /^(?:(\d{2,}):)?(\d{2}):(\d{2})[,.](\d{1,3})$/.exec(value.trim());
    if (!match) return null;
    return Number(match[1] || 0) * 3600 + Number(match[2]) * 60 + Number(match[3]) + Number(match[4].padEnd(3, "0")) / 1e3;
  };
  return text.replace(/^\uFEFF/, "").split(/\r?\n\s*\r?\n/).flatMap((block) => {
    const lines = block.split(/\r?\n/);
    const timingIndex = lines.findIndex((line) => line.includes("-->"));
    if (timingIndex < 0) return [];
    const times = lines[timingIndex].split("-->").map((value) => value.trim().split(/\s+/)[0]);
    const start = parseTime(times[0] || "");
    const end = parseTime(times[1] || "");
    const content = lines.slice(timingIndex + 1).join("\n").replace(/<[^>]*>/g, "").trim();
    return start != null && end != null && end > start && content ? [{ start, end, text: content }] : [];
  });
}
function renderSubtitle(positionSeconds) {
  const overlay = document.getElementById("player-subtitle-overlay");
  if (!overlay) return;
  const enabled = isAvplayActive ? state.avplaySubtitlesEnabled : state.subtitlesEnabled;
  if (!enabled || !state.subtitleCues || !state.subtitleCues.length) {
    overlay.hidden = true;
    return;
  }
  const time = positionSeconds - Number(state.preferences.subtitleOffset || 0) / 1e3;
  const cue = state.subtitleCues.find((item) => time >= item.start && time < item.end);
  if (cue && cue.text) {
    overlay.textContent = cue.text;
    overlay.hidden = false;
  } else {
    overlay.textContent = "";
    overlay.hidden = true;
  }
}
function renderAvplaySubtitle(positionSeconds) {
  renderSubtitle(positionSeconds);
}
window.renderSubtitle = renderSubtitle;
window.renderAvplaySubtitle = renderAvplaySubtitle;
function applyPlayerSubtitleStyle() {
  const video = document.getElementById("video-player");
  const verticalPosition = Math.max(0, Math.min(25, Number(state.preferences.subtitleVerticalPosition) || 0));
  document.documentElement.style.setProperty("--subtitle-position", "".concat(verticalPosition, "%"));
  if (video && !isAvplayActive) {
    for (const track of Array.from(video.textTracks || [])) {
      for (const cue of track.cues || []) {
        if (typeof cue.originalStartTime !== "number") {
          cue.originalStartTime = cue.startTime;
          cue.originalEndTime = cue.endTime;
        }
        cue.startTime = Math.max(0, cue.originalStartTime + state.preferences.subtitleOffset / 1e3);
        cue.endTime = Math.max(0, cue.originalEndTime + state.preferences.subtitleOffset / 1e3);
        if ("line" in cue && "snapToLines" in cue) {
          cue.snapToLines = false;
          cue.line = 100 - verticalPosition;
        }
      }
    }
  }
  const overlay = document.getElementById("player-subtitle-overlay");
  if (overlay) {
    const transparency = Math.max(0, Math.min(100, Number(state.preferences.subtitleBoxTransparency) || 0));
    overlay.style.color = state.preferences.subtitleText || "#ffffff";
    overlay.style.fontFamily = "".concat(state.preferences.subtitleFont || "Arial", ", sans-serif");
    overlay.style.fontSize = "".concat(Math.min(36, Math.max(14, Number(state.preferences.subtitleSize) || 22)), "px");
    overlay.style.backgroundColor = state.preferences.subtitleBoxEnabled ? colorWithAlpha(state.preferences.subtitleBackground, (100 - transparency) / 100) : "transparent";
  }
  renderAvplaySubtitle(playerCurrentTime());
}
function returnToTitleDetails() {
  var _a, _b;
  state.playbackRequestId = Number(state.playbackRequestId || 0) + 1;
  const shell = document.querySelector(".player-shell");
  if (document.fullscreenElement === shell) {
    (_a = document.exitFullscreen) == null ? void 0 : _a.call(document).catch((error) => console.warn("Could not exit player fullscreen:", error.message));
  }
  shell == null ? void 0 : shell.classList.remove("player-fullscreen-fallback");
  state.playerFullscreen = false;
  stopCurrentPlayback();
  clearTimeout(state.playerTimer);
  state.playerTimer = null;
  state.subtitleDialogOpen = false;
  renderDetails(state.currentTitle, ((_b = state.currentTitle) == null ? void 0 : _b.media_type) || "movie");
}
