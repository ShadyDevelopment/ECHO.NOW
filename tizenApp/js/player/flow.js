async function startPlayback(media, episode, resumePosition = 0) {
  var _a, _b, _c, _d;
  try {
    const requestId = Number(state.playbackRequestId || 0) + 1;
    state.playbackRequestId = requestId;
    clearTimeout(state.playerTimer);
    state.playerTimer = null;
    const mediaType = String((media == null ? void 0 : media.media_type) || (media == null ? void 0 : media.type) || "").toLowerCase();
    const isSeries = mediaType === "tv" || mediaType === "series" || Boolean((media == null ? void 0 : media.seasons) || (media == null ? void 0 : media.number_of_seasons) || (media == null ? void 0 : media.first_air_date));
    if (isSeries && !episode) {
      const progress = progressFor(media);
      const seasonNum = (progress == null ? void 0 : progress.season) || ((_b = (_a = media.seasons) == null ? void 0 : _a.find((s) => s.season_number > 0)) == null ? void 0 : _b.season_number) || 1;
      const episodeNum = (progress == null ? void 0 : progress.episode) || 1;
      episode = {
        season: seasonNum,
        episode: episodeNum,
        position: resumePosition || (progress == null ? void 0 : progress.position) || 0
      };
    }
    state.currentEpisode = episode;
    state.currentTitle = media;
    state.playerSources = [];
    state.playerSourceIndex = 0;
    state.failedPlayerSources = [];
    state.nextEpisode = null;
    state.episodeTiming = null;
    state.advancingToNextEpisode = false;
    state.upNextDismissed = false;
    state.autoPlayNextEpisode = true;
    state.playerAspectRatio = 0;
    state.playerFullscreen = false;
    state.subtitleDialogOpen = false;
    state.subtitleCues = [];
    state.subtitleCueTrackIndex = -1;
    state.avplaySubtitlesEnabled = false;
    const subtitleLoadRequestId = ++state.subtitleLoadRequestId;
    const currentSeason = Number((episode == null ? void 0 : episode.season) || 0);
    const currentEpisode = Number((episode == null ? void 0 : episode.episode) || 0);
    const trackTags = "";
    const browserFullscreenAvailable = Boolean(
      document.fullscreenEnabled && !/(Tizen|SmartTV|webOS)/i.test(navigator.userAgent || "")
    );
    state.page = "player";
    navState("");
    main.innerHTML = '<section class="player-shell" tabindex="-1">\n                <video id="video-player" class="player-media" autoplay playsinline webkit-playsinline preload="auto">'.concat(trackTags, '</video>\n                <div id="player-subtitle-overlay" class="player-subtitle-overlay" aria-live="off" hidden></div>\n                <div class="player-buffering" id="player-buffering"><div class="player-spinner"></div></div>\n                <section class="player-resolution-error" id="player-resolution-error" role="alert" hidden>\n                    <strong id="player-resolution-error-title">Playback unavailable</strong>\n                    <p id="player-resolution-error-message"></p>\n                    <div class="player-resolution-actions">\n                        <button class="action-button primary focusable" data-action="retry-stream-resolution">Retry</button>\n                        <button class="action-button focusable" data-action="stop">Back</button>\n                    </div>\n                </section>\n                <div class="player-controls player-controls-visible" id="player-controls">\n                    <div class="player-topbar">\n                        <button class="player-icon-button player-back-btn focusable" data-action="stop" aria-label="Back">\n                            <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>\n                        </button>\n                        <div class="player-heading">\n                            <strong>').concat(escapeHtml(media.title || media.name || appName)).concat(episode ? " - S".concat(episode.season, ":E").concat(episode.episode) : "", '</strong>\n                        </div>\n                        <div class="player-top-spacer"></div>\n                    </div>\n                    <div class="player-bottom-controls">\n                        <div class="player-seek">\n                            <input id="player-progress" class="focusable" type="range" min="0" max="1000" value="0" aria-label="Playback position">\n                        </div>\n                        <div class="player-controls-row">\n                            <div class="player-time-label" id="player-time-label">00:00 / --:--</div>\n                            <div class="player-transport-controls">\n                                <button class="player-icon-button focusable" data-action="seek" data-seconds="-10" aria-label="Back 10 seconds">\n                                    <svg viewBox="0 0 24 24"><path d="M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8zm-1.1 11h-.85v-3.26l-1.01.32v-.65l1.76-.59h.1V16zm4.24-1.66c0 .54-.14.95-.41 1.23-.27.27-.66.41-1.15.41-.5 0-.88-.14-1.15-.41-.27-.28-.41-.69-.41-1.23v-1.13c0-.54.14-.95.41-1.23.27-.28.66-.41 1.15-.41.5 0 .88.14 1.15.41.27.28.41.69.41 1.23v1.13zm-.83-1.23c0-.36-.07-.63-.2-.81-.13-.18-.34-.27-.63-.27-.29 0-.5.09-.63.27-.13.18-.2.45-.2.81v1.33c0 .36.07.63.2.81.13.18.34.27.63.27.29 0 .5-.09.63-.27.13-.18.2-.45.2-.81V12.11z"/></svg>\n                                </button>\n                                <button class="player-icon-button player-play-button focusable" data-action="toggle-play" aria-label="Play">\n                                    <svg viewBox="0 0 24 24" id="play-pause-icon"><path d="M8 6.82v10.36c0 .79.87 1.27 1.54.84l8.14-5.18c.62-.39.62-1.29 0-1.69L9.54 5.98C8.87 5.55 8 6.03 8 6.82z"/></svg>\n                                </button>\n                                <button class="player-icon-button focusable" data-action="seek" data-seconds="10" aria-label="Forward 10 seconds">\n                                    <svg viewBox="0 0 24 24"><path d="M12 5V1l5 5-5 5V7c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6h2c0 4.42-3.58 8-8 8s-8-3.58-8-8 3.58-8 8-8zm-1.1 11h-.85v-3.26l-1.01.32v-.65l1.76-.59h.1V16zm4.24-1.66c0 .54-.14.95-.41 1.23-.27.27-.66.41-1.15.41-.5 0-.88-.14-1.15-.41-.27-.28-.41-.69-.41-1.23v-1.13c0-.54.14-.95.41-1.23.27-.28.66-.41 1.15-.41.5 0 .88.14 1.15.41.27.28.41.69.41 1.23v1.13zm-.83-1.23c0-.36-.07-.63-.2-.81-.13-.18-.34-.27-.63-.27-.29 0-.5.09-.63.27-.13.18-.2.45-.2.81v1.33c0 .36.07.63.2.81.13.18.34.27.63.27.29 0 .5-.09.63-.27.13-.18.2-.45.2-.81V12.11z"/></svg>\n                                </button>\n                            </div>\n                            <div class="player-utility-row">\n                                <button class="player-icon-button player-subtitles-button focusable" data-action="player-subtitles" aria-label="Subtitle controls">\n                                    <svg viewBox="0 0 24 24"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-1 14H5c-.55 0-1-.45-1-1V7c0-.55.45-1 1-1h14c.55 0 1 .45 1 1v10c0 .55-.45 1-1 1zm-9-4H6c-.55 0-1-.45-1-1s.45-1 1-1h4c.55 0 1 .45 1 1s-.45 1-1 1zm8 0h-4c-.55 0-1-.45-1-1s.45-1 1-1h4c.55 0 1 .45 1 1s-.45 1-1 1zm-4-4H6c-.55 0-1-.45-1-1s.45-1 1-1h8c.55 0 1 .45 1 1s-.45 1-1 1zm6 0h-2c-.55 0-1-.45-1-1s.45-1 1-1h2c.55 0 1 .45 1 1s-.45 1-1 1z"/></svg>\n                                </button>\n                                <div class="player-quality-container">\n                                    <button class="player-icon-button player-quality-button focusable" data-action="toggle-quality-menu" aria-label="Video quality">\n                                        <svg viewBox="0 0 24 24"><path d="M19 4H5c-1.11 0-2 .9-2 2v12c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-8 11H9.5v-2h-2v2H6V9h1.5v2.5h2V9H11v6zm7-1c0 .55-.45 1-1 1h-.75v1.5h-1.5V15H14c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v4zm-3.5-.5h2v-3h-2v3z"/></svg>\n                                        <span class="player-quality-label" id="player-current-quality">Loading sources\u2026</span>\n                                    </button>\n                                    <div class="player-quality-menu" id="player-quality-menu" hidden>\n                                        <span class="quality-menu-status">Sources are being resolved\u2026</span>\n                                    </div>\n                                </div>\n                                ').concat(episode ? '\n                                <button class="player-icon-button player-autoplay-button focusable active" data-action="toggle-autoplay" aria-label="Auto-play next episode: On">\n                                    <svg viewBox="0 0 24 24"><path d="M7.58 16.89l5.77-4.07c.56-.4.56-1.24 0-1.63L7.58 7.11C6.91 6.65 6 7.12 6 7.93v8.14c0 .81.91 1.28 1.58.82zM16 7v10c0 .55.45 1 1 1s1-.45 1-1V7c0-.55-.45-1-1-1s-1 .45-1 1z"/></svg>\n                                </button>' : "", '\n                                <button class="player-icon-button player-aspect-button focusable" data-action="aspect-ratio" aria-label="Adjust screen ratio">\n                                    <svg viewBox="0 0 24 24"><path d="M19 12h-2v3h-3v2h5v-5zM7 9h3V7H5v5h2V9zm14-6H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16.01H3V4.99h18v14.02z"/></svg>\n                                </button>\n                                <button class="player-icon-button player-fullscreen-button focusable" data-action="toggle-fullscreen" aria-label="Enter fullscreen" title="Fullscreen"').concat(browserFullscreenAvailable ? "" : " hidden", '>\n                                    <svg viewBox="0 0 24 24"><path d="M4 4h6v2H6v4H4V4zm10 0h6v6h-2V6h-4V4zM4 14h2v4h4v2H4v-6zm14 4v-4h2v6h-6v-2h4z"/></svg>\n                                </button>\n                            </div>\n                        </div>\n                    </div>\n                </div>\n                <button class="player-skip-action focusable" id="skip-intro" data-action="skip-intro" hidden>\n                    <svg viewBox="0 0 24 24" class="skip-icon"><path d="M7.58 16.89l5.77-4.07c.56-.4.56-1.24 0-1.63L7.58 7.11C6.91 6.65 6 7.12 6 7.93v8.14c0 .81.91 1.28 1.58.82zM16 7v10c0 .55.45 1 1 1s1-.45 1-1V7c0-.55-.45-1-1-1s-1 .45-1 1z"/></svg>\n                    <span>Skip Intro</span>\n                </button>\n                <button class="player-skip-action focusable" id="skip-credits" data-action="skip-credits" hidden>\n                    <svg viewBox="0 0 24 24" class="skip-icon"><path d="M7.58 16.89l5.77-4.07c.56-.4.56-1.24 0-1.63L7.58 7.11C6.91 6.65 6 7.12 6 7.93v8.14c0 .81.91 1.28 1.58.82zM16 7v10c0 .55.45 1 1 1s1-.45 1-1V7c0-.55-.45-1-1-1s-1 .45-1 1z"/></svg>\n                    <span>Skip Credits</span>\n                </button>\n                <section class="subtitle-dialog" id="player-subtitle-dialog" aria-label="Subtitle controls" hidden>\n                    <div class="subtitle-dialog-heading">\n                        <div><strong>Subtitles & appearance</strong><span>Preferred language: ').concat(escapeHtml(state.preferences.subtitleLanguage), '</span></div>\n                        <button class="player-icon-button focusable" data-action="close-subtitles" aria-label="Close subtitle controls">\n                            <svg viewBox="0 0 24 24" style="width:20px;height:20px;fill:currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z"/></svg>\n                        </button>\n                    </div>\n                    <div class="subtitle-preview" aria-label="Subtitle appearance preview"><span class="subtitle-preview-text">The quick brown fox jumps over the lazy dog.</span></div>\n                    <div class="subtitle-dialog-group">\n                        <h3>Track</h3>\n                        <div class="subtitle-dialog-actions">\n                        <button class="action-button primary focusable" data-action="search-subtitles">Search subtitles</button>\n                        <label class="player-file action-button focusable">Load subtitle<input id="subtitle-file" type="file" accept=".srt,.vtt,text/vtt,application/x-subrip" aria-label="Load subtitle file"></label>\n                        <button class="action-button focusable" data-action="toggle-subtitles" aria-pressed="false">Turn subtitles on</button>\n                        <label class="player-select">Subtitle track<select id="player-track" class="focusable"><option value="-1">Off</option></select></label>\n                        <label class="player-select">Playback speed<select id="player-speed" class="focusable"><option>1</option><option>0.75</option><option>1.25</option><option>1.5</option><option>2</option></select></label>\n                        </div>\n                    </div>\n                    <div class="subtitle-dialog-group">\n                        <h3>Timing</h3>\n                        <div class="subtitle-adjustments">\n                            <button class="action-button focusable" data-action="subtitle-offset" data-offset="-1000">\u22121 s</button>\n                            <button class="action-button focusable" data-action="subtitle-offset" data-offset="-250">\u2212250 ms</button>\n                            <span id="subtitle-offset-label">Offset: ').concat(Number(state.preferences.subtitleOffset) || 0, ' ms</span>\n                            <button class="action-button focusable" data-action="subtitle-offset" data-offset="250">+250 ms</button>\n                            <button class="action-button focusable" data-action="subtitle-offset" data-offset="1000">+1 s</button>\n                            <button class="action-button outlined focusable" data-action="subtitle-offset-reset">Reset</button>\n                        </div>\n                    </div>\n                    <div class="subtitle-dialog-group">\n                        <h3>Appearance</h3>\n                        <div class="subtitle-adjustments">\n                            <button class="action-button focusable" data-action="subtitle-size" data-change="-2" aria-label="Decrease subtitle size">A\u2212</button>\n                            <span id="subtitle-size-label">').concat(Number(state.preferences.subtitleSize) || 22, 'px</span>\n                            <button class="action-button focusable" data-action="subtitle-size" data-change="2" aria-label="Increase subtitle size">A+</button>\n                            <label class="subtitle-position-control">Vertical position <span id="player-subtitle-position-label">').concat(Math.max(0, Math.min(25, Number(state.preferences.subtitleVerticalPosition) || 0)), '%</span>\n                                <input id="player-subtitle-position" class="focusable" type="range" min="0" max="25" step="1" value="').concat(Math.max(0, Math.min(25, Number(state.preferences.subtitleVerticalPosition) || 0)), '" aria-label="Subtitle vertical position">\n                            </label>\n                            <button class="action-button focusable" data-action="subtitle-background" aria-pressed="').concat(state.preferences.subtitleBoxEnabled, '">Subtitle background: ').concat(state.preferences.subtitleBoxEnabled ? "On" : "Off", '</button>\n                            <button class="action-button focusable" data-action="subtitle-preset" data-preset="dark">Dark</button>\n                            <button class="action-button focusable" data-action="subtitle-preset" data-preset="light">Light</button>\n                            <button class="action-button focusable" data-action="subtitle-preset" data-preset="contrast">High contrast</button>\n                        </div>\n                    </div>\n                    <p id="subtitle-status" class="hint" aria-live="polite">Choose a subtitle track, search SubDL, or load an SRT/VTT file.</p>\n                    <div id="subtitle-search-results" class="subtitle-search-results" aria-live="polite"></div>\n                </section>\n                <section class="up-next-overlay" id="player-up-next" hidden>\n                    <img class="up-next-image" src="').concat(escapeHtml(imageUrl(media.backdrop_path) || imageUrl(media.poster_path) || ""), '" alt="">\n                    <strong data-up-next-title>Up Next</strong>\n                    <span class="up-next-series" data-up-next-series></span>\n                    <div class="up-next-progress-track"><div class="up-next-progress-bar"></div></div>\n                    <span class="up-next-countdown" data-up-next-countdown></span>\n                    <div class="up-next-actions">\n                        <button class="action-button primary focusable" data-action="play-next">\n                            <svg viewBox="0 0 24 24" class="btn-icon"><path d="M8 6.82v10.36c0 .79.87 1.27 1.54.84l8.14-5.18c.62-.39.62-1.29 0-1.69L9.54 5.98C8.87 5.55 8 6.03 8 6.82z"/></svg>\n                            <span>Play Now</span>\n                        </button>\n                        <button class="action-button focusable outlined" data-action="cancel-up-next">Cancel</button>\n                    </div>\n                </section>\n            </section>');
    const video = document.getElementById("video-player");
    video.addEventListener("play", () => {
      startStallMonitor();
      updatePlayerPlayButton();
      setPlayerControlsVisible(true, true);
    });
    video.addEventListener("pause", () => {
      stopStallMonitor();
      updatePlayerPlayButton();
      setPlayerControlsVisible(true, false);
    });
    video.addEventListener("waiting", () => {
      const b = document.getElementById("player-buffering");
      if (b) b.hidden = false;
    });
    video.addEventListener("playing", () => {
      const b = document.getElementById("player-buffering");
      if (b) b.hidden = true;
      updatePlayerPlayButton();
    });
    video.addEventListener("canplay", () => {
      const b = document.getElementById("player-buffering");
      if (b) b.hidden = true;
      updatePlayerPlayButton();
    });
    video.addEventListener("loadeddata", () => updateTimeDisplay());
    video.addEventListener("durationchange", () => updateTimeDisplay());
    video.addEventListener("timeupdate", () => {
      updateTimeDisplay();
      savePlaybackProgress();
      if (typeof renderSubtitle === "function") renderSubtitle(video.currentTime);
    });
    video.addEventListener("ended", () => {
      handlePlaybackEnded();
    });
    video.addEventListener("error", () => {
      if (activeHls || isAvplayActive) {
        console.warn("Video DOM error event suppressed; managed by player engine.");
        return;
      }
      handlePlayerError();
    });
    const playerShell = document.querySelector(".player-shell");
    playerShell.addEventListener("pointermove", () => setPlayerControlsVisible(true, true));
    playerShell.addEventListener("click", (event) => {
      if (event.target === playerShell || event.target === video) {
        setPlayerControlsVisible(true, true);
        if (event.target === video) playerTogglePlay();
      }
    });
    document.getElementById("player-controls").addEventListener("focusin", () => setPlayerControlsVisible(true, true));
    (_c = document.querySelector('[data-action="toggle-play"]')) == null ? void 0 : _c.focus();
    updatePlayerAspectRatio();
    const startPosition = Number(resumePosition || (episode == null ? void 0 : episode.position) || 0) / 1e3;
    if (episode) {
      findNextEpisode(media, episode).then((next) => {
        var _a2, _b2, _c2;
        if (((_a2 = state.currentTitle) == null ? void 0 : _a2.id) === media.id && Number((_b2 = state.currentEpisode) == null ? void 0 : _b2.season) === currentSeason && Number((_c2 = state.currentEpisode) == null ? void 0 : _c2.episode) === currentEpisode) {
          state.nextEpisode = next;
          const dur = playerDuration();
          if (next && dur > 0) {
            renderUpNext(playerCurrentTime(), dur);
          }
        }
      });
    }
    video.addEventListener("loadedmetadata", () => {
      var _a2;
      updateTimeDisplay();
      const progress = progressFor(media, episode);
      const target = startPosition || Number((progress == null ? void 0 : progress.position) || 0) / 1e3;
      if (!activeHls && target > 0 && target < playerDuration()) {
        try {
          video.currentTime = target;
        } catch (_) {
        }
      }
      Array.from(video.textTracks || []).forEach((item) => {
        item.mode = "hidden";
      });
      const trackList = video.textTracks;
      const syncTrackOptions = () => {
        const selector = document.getElementById("player-track");
        if (!selector) return;
        const selectedValue = selector.value;
        selector.innerHTML = '<option value="-1">Off</option>'.concat(Array.from(trackList).map(
          (item, index) => '<option value="'.concat(index, '">').concat(escapeHtml(item.label || item.language || "Track ".concat(index + 1)), "</option>")
        ).join(""));
        if (Array.from(trackList).some((_item, index) => String(index) === selectedValue)) selector.value = selectedValue;
      };
      syncTrackOptions();
      (_a2 = trackList.addEventListener) == null ? void 0 : _a2.call(trackList, "addtrack", syncTrackOptions);
      applyPlayerSubtitleStyle();
      if (state.preferences.autoApplySubtitles) autoLoadPreferredSubtitle(subtitleLoadRequestId);
      if (episode) {
        loadEpisodeTiming(media, episode, playerDuration() * 1e3).then((timing) => {
          var _a3, _b2, _c2;
          if (((_a3 = state.currentTitle) == null ? void 0 : _a3.id) === media.id && Number((_b2 = state.currentEpisode) == null ? void 0 : _b2.season) === currentSeason && Number((_c2 = state.currentEpisode) == null ? void 0 : _c2.episode) === currentEpisode) {
            state.episodeTiming = timing;
            renderSkipActions(playerCurrentTime() * 1e3, playerDuration() * 1e3);
            if (state.nextEpisode) renderUpNext(playerCurrentTime(), playerDuration());
          }
        });
      }
    }, { once: true });
    setPlayerControlsVisible(true, true);
    (_d = document.querySelector(".player-shell")) == null ? void 0 : _d.setAttribute("aria-busy", "true");
    resolveStreams(media, episode).then((sources) => {
      const shell = document.querySelector(".player-shell");
      if (state.playbackRequestId !== requestId || !(shell == null ? void 0 : shell.isConnected)) return;
      if (!Array.isArray(sources) || sources.length === 0) {
        state.advancingToNextEpisode = false;
        showPlayerResolutionError("Configured providers did not return a playable source for this selection.");
        return;
      }
      state.playerSources = sources;
      const menu = document.getElementById("player-quality-menu");
      if (menu) {
        menu.innerHTML = sources.map(
          (item, index) => '<button class="quality-menu-item focusable'.concat(index === 0 ? " active" : "", '" data-action="select-quality" data-index="').concat(index, '">').concat(escapeHtml(item.label || item.quality || "Auto")).concat(item.providerName ? " \xB7 ".concat(escapeHtml(item.providerName)) : "", "</button>")
        ).join("");
      }
      const qualityLabel = document.getElementById("player-current-quality");
      if (qualityLabel) qualityLabel.textContent = sources[0].label || sources[0].quality || "Auto";
      const videoElement = document.getElementById("video-player");
      for (const subtitle of sources[0].subtitles || []) {
        const track = document.createElement("track");
        track.kind = "subtitles";
        track.src = subtitle.url;
        track.srclang = subtitle.language || "en";
        track.label = subtitle.label || subtitle.language || "Subtitle";
        videoElement == null ? void 0 : videoElement.appendChild(track);
      }
      shell.setAttribute("aria-busy", "false");
      setPlayerSource(0, startPosition);
    }).catch((error) => {
      if (state.playbackRequestId !== requestId) return;
      state.advancingToNextEpisode = false;
      showPlayerResolutionError("Could not resolve configured playback sources: ".concat(error.message || "Unknown error"));
    });
  } catch (error) {
    state.advancingToNextEpisode = false;
    showPlayerResolutionError("Could not start playback: ".concat(error.message || "Unknown error"));
  }
}
function showPlayerResolutionError(message) {
  var _a;
  const buffering = document.getElementById("player-buffering");
  if (buffering) buffering.hidden = true;
  const shell = document.querySelector(".player-shell");
  shell == null ? void 0 : shell.setAttribute("aria-busy", "false");
  const panel = document.getElementById("player-resolution-error");
  const output = document.getElementById("player-resolution-error-message");
  if (output) output.textContent = message;
  if (panel) {
    panel.hidden = false;
    (_a = panel.querySelector('[data-action="retry-stream-resolution"]')) == null ? void 0 : _a.focus();
  } else {
    toastMessage(message);
  }
}
