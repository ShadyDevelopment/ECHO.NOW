function formatTime(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) return "00:00";
  const total = Math.floor(seconds);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor(total % 3600 / 60);
  const remainder = total % 60;
  return hours ? "".concat(hours, ":").concat(String(minutes).padStart(2, "0"), ":").concat(String(remainder).padStart(2, "0")) : "".concat(minutes, ":").concat(String(remainder).padStart(2, "0"));
}
async function loadLocalSubtitle(file) {
  if (!file) return;
  const text = await file.text();
  attachSubtitleText(text, file.name);
  toastMessage("Subtitle loaded: ".concat(file.name));
}
function attachSubtitleText(text, label) {
  const video = document.getElementById("video-player");
  if (!video) throw new Error("The player is not open.");
  const cleaned = text.replace(/^\uFEFF/, "").trim();
  const vtt = cleaned.startsWith("WEBVTT") ? cleaned : "WEBVTT\n\n".concat(cleaned.replace(/^\s*\d+\s*$/gm, "").replace(/(\d{2}:\d{2}:\d{2}),(\d{1,3})\b/g, (_match, timestamp, millis) => "".concat(timestamp, ".").concat(millis.padEnd(3, "0"))));
  if (!vtt.includes("-->")) throw new Error("The selected file does not contain supported SRT/VTT subtitle cues.");
  const track = document.createElement("track");
  track.kind = "subtitles";
  track.label = label;
  track.srclang = state.preferences.subtitleLanguage.toLowerCase();
  track.src = URL.createObjectURL(new Blob([vtt], { type: "text/vtt" }));
  state.subtitleCues = parseSubtitleCues(vtt);
  state.subtitlesEnabled = state.preferences.autoApplySubtitles;
  state.avplaySubtitlesEnabled = state.preferences.autoApplySubtitles;
  applyPlayerSubtitleStyle();
  video.appendChild(track);
  state.subtitleCueTrackIndex = Array.from(video.textTracks || []).indexOf(track.track);
  track.addEventListener("load", () => {
    track.track.mode = "hidden";
    const selector = document.getElementById("player-track");
    const index = Array.from(video.textTracks).indexOf(track.track);
    selector == null ? void 0 : selector.insertAdjacentHTML("beforeend", '<option value="'.concat(index, '">').concat(escapeHtml(label), "</option>"));
    if (selector && state.preferences.autoApplySubtitles) selector.value = String(index);
    const trackCues = subtitleCuesFromTrack(track.track);
    if (trackCues.length) state.subtitleCues = trackCues;
    state.subtitleCueTrackIndex = index;
    if (state.preferences.autoApplySubtitles) {
      state.subtitlesEnabled = true;
      state.avplaySubtitlesEnabled = true;
      renderSubtitle(playerCurrentTime());
    }
    const toggle = document.querySelector('[data-action="toggle-subtitles"]');
    if (toggle && state.preferences.autoApplySubtitles) toggle.textContent = "Turn subtitles off";
    const status = document.getElementById("subtitle-status");
    if (status) status.textContent = "".concat(state.preferences.autoApplySubtitles ? "Applied" : "Loaded", " ").concat(label, ".");
    applyPlayerSubtitleStyle();
  }, { once: true });
  return track;
}
function subtitleCuesFromTrack(track) {
  return Array.from((track == null ? void 0 : track.cues) || []).filter((cue) => Number.isFinite(cue.startTime) && Number.isFinite(cue.endTime) && cue.endTime > cue.startTime).map((cue) => ({
    start: cue.startTime,
    end: cue.endTime,
    text: String(cue.text || "").replace(/<[^>]*>/g, "").trim()
  })).filter((cue) => cue.text);
}
function setSubtitleTrack(index, enabled = index >= 0) {
  const video = document.getElementById("video-player");
  if (!video) return false;
  const tracks = Array.from(video.textTracks || []);
  const selected = index >= 0 ? tracks[index] : null;
  tracks.forEach((track) => {
    track.mode = "hidden";
  });
  const trackCues = enabled && selected ? subtitleCuesFromTrack(selected) : [];
  if (trackCues.length) {
    state.subtitleCues = trackCues;
    state.subtitleCueTrackIndex = index;
  } else if (!enabled || state.subtitleCueTrackIndex !== index && state.subtitleCues.length === 0) {
    state.subtitleCues = [];
    state.subtitleCueTrackIndex = -1;
  }
  state.subtitlesEnabled = Boolean(enabled && selected && state.subtitleCues.length);
  state.avplaySubtitlesEnabled = state.subtitlesEnabled;
  renderSubtitle(playerCurrentTime());
  if (enabled && selected && !state.subtitleCues.length) {
    const status2 = document.getElementById("subtitle-status");
    if (status2) status2.textContent = "This track has no readable cues yet.";
  }
  const selector = document.getElementById("player-track");
  if (selector) selector.value = enabled && selected ? String(index) : "-1";
  const toggle = document.querySelector('[data-action="toggle-subtitles"]');
  if (toggle) {
    const active = Boolean(enabled && selected && state.subtitlesEnabled);
    toggle.textContent = active ? "Turn subtitles off" : "Turn subtitles on";
    toggle.setAttribute("aria-pressed", String(active));
  }
  const status = document.getElementById("subtitle-status");
  if (status && selected && state.subtitlesEnabled) {
    status.textContent = "Subtitles enabled: ".concat(selected.label || selected.language || "Track ".concat(index + 1), ".");
  } else if (status && !enabled) {
    status.textContent = "Subtitles are off.";
  }
  return Boolean(selected);
}
async function autoLoadPreferredSubtitle(requestId) {
  const video = document.getElementById("video-player");
  if (!video || !state.preferences.autoApplySubtitles) return;
  const language = normalizeLanguage(state.preferences.subtitleLanguage) || String(state.preferences.subtitleLanguage || "en").toLowerCase();
  const subtitleTracks = () => Array.from(video.textTracks || []).filter((track) => track.kind === "subtitles" || track.kind === "captions");
  const matchesPreferredLanguage = (track) => (normalizeLanguage(track.language) || String(track.language || "").toLowerCase()) === language;
  const selectTrack = (track) => {
    if (!track) return;
    const tracks = Array.from(video.textTracks || []);
    setSubtitleTrack(tracks.indexOf(track), true);
  };
  const usePreferredTrack = () => {
    const preferredTrack = subtitleTracks().find(matchesPreferredLanguage);
    if (!preferredTrack) return false;
    if (isAvplayActive && !subtitleCuesFromTrack(preferredTrack).length) return false;
    selectTrack(preferredTrack);
    const status = document.getElementById("subtitle-status");
    if (status) status.textContent = "Automatically applied ".concat(state.preferences.subtitleLanguage, " subtitles.");
    return true;
  };
  if (usePreferredTrack()) return;
  const trackElements = Array.from(video.querySelectorAll("track"));
  await Promise.all(trackElements.map((track) => track.readyState === 2 ? Promise.resolve() : new Promise((resolve) => {
    const timeout = setTimeout(resolve, 2500);
    const finish = () => {
      clearTimeout(timeout);
      resolve();
    };
    track.addEventListener("load", finish, { once: true });
    track.addEventListener("error", finish, { once: true });
  })));
  if (requestId !== state.subtitleLoadRequestId || video !== document.getElementById("video-player")) return;
  if (usePreferredTrack()) return;
  if (hasConfiguredSubdlKey) {
    await searchSubdlSubtitles();
    if (requestId !== state.subtitleLoadRequestId || video !== document.getElementById("video-player")) return;
    const preferredOption = state.subtitleOptions.findIndex(
      (option) => matchesPreferredLanguage({ language: option.language })
    );
    const subtitleIndex = preferredOption >= 0 ? preferredOption : 0;
    if (state.subtitleOptions.length) {
      try {
        await downloadSubdlSubtitle(subtitleIndex, { quiet: true });
        if (requestId !== state.subtitleLoadRequestId || video !== document.getElementById("video-player")) return;
        const status = document.getElementById("subtitle-status");
        if (status) status.textContent = "Automatically loaded ".concat(state.subtitleOptions[subtitleIndex].language, " subtitles.");
        return;
      } catch (error) {
        const status = document.getElementById("subtitle-status");
        if (status) status.textContent = "Automatic subtitle load failed: ".concat(error.message);
        console.warn("Automatic subtitle load failed:", error.message);
      }
    }
  }
  const fallbackTrack = subtitleTracks()[0];
  if (fallbackTrack && (!isAvplayActive || subtitleCuesFromTrack(fallbackTrack).length)) {
    selectTrack(fallbackTrack);
    const status = document.getElementById("subtitle-status");
    if (status) status.textContent = "No ".concat(state.preferences.subtitleLanguage, " track was found; using ").concat(fallbackTrack.language || "the available", " subtitles.");
  } else if (isAvplayActive && !hasConfiguredSubdlKey) {
    const status = document.getElementById("subtitle-status");
    if (status) status.textContent = "Automatic subtitle loading on the Tizen hardware player requires a configured SubDL API key.";
  } else if (!hasConfiguredSubdlKey) {
    const status = document.getElementById("subtitle-status");
    if (status) status.textContent = "No embedded subtitles were found. Configure a SubDL key to load ".concat(state.preferences.subtitleLanguage, " subtitles automatically.");
  }
}
async function searchSubdlSubtitles() {
  const output = document.getElementById("subtitle-search-results");
  if (!output) return;
  state.subtitleOptions = [];
  const apiKey = configuredSubdlApiKey();
  if (!apiKey) {
    output.textContent = "No SubDL key is available in this configured app build.";
    return;
  }
  const media = state.currentTitle;
  if (!media) {
    output.textContent = "Open a movie or series before searching subtitles.";
    return;
  }
  output.textContent = "Searching SubDL\u2026";
  const status = document.getElementById("subtitle-status");
  if (status) status.textContent = "Searching SubDL\u2026";
  try {
    const params = new URLSearchParams({
      api_key: apiKey,
      tmdb_id: String(media.id),
      film_name: media.title || media.name || "",
      type: media.media_type === "tv" ? "tv" : "movie",
      year: (media.release_date || media.first_air_date || "").slice(0, 4),
      languages: state.preferences.subtitleLanguage.toUpperCase(),
      subs_per_page: "30",
      unpack: "1",
      client: "ECHO_NOW"
    });
    if (state.currentEpisode && media.media_type === "tv") {
      params.set("season_number", String(state.currentEpisode.season));
      params.set("episode_number", String(state.currentEpisode.episode));
    }
    const response = await fetch("https://api.subdl.com/api/v1/subtitles?".concat(params));
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.error || "SubDL request failed (".concat(response.status, ")."));
    if (!result.status) throw new Error(result.error || "SubDL rejected the subtitle search.");
    state.subtitleOptions = (result.subtitles || []).flatMap(
      (subtitle) => (subtitle.unpack_files || []).filter((file) => ["srt", "vtt"].includes(String(file.format || "").toLowerCase())).filter((file) => !state.currentEpisode || (file.season == null || Number(file.season) === state.currentEpisode.season) && (file.episode == null || Number(file.episode) === state.currentEpisode.episode)).map((file) => {
        var _a;
        const url = /^https?:\/\//i.test(file.url || "") ? file.url : ((_a = file.url) == null ? void 0 : _a.startsWith("/")) ? "https://dl.subdl.com".concat(file.url) : "";
        return url ? {
          url,
          label: file.release_name || subtitle.release_name || file.name || subtitle.name || "Subtitle",
          language: file.language || subtitle.language || state.preferences.subtitleLanguage,
          format: String(file.format).toLowerCase()
        } : null;
      }).filter(Boolean)
    );
    output.innerHTML = state.subtitleOptions.length ? state.subtitleOptions.map((option, index) => '<button class="episode-chip focusable" data-action="download-subtitle" data-index="'.concat(index, '">').concat(escapeHtml(option.label), " \xB7 ").concat(escapeHtml(option.language), " \xB7 ").concat(escapeHtml(option.format.toUpperCase()), "</button>")).join("") : '<span class="muted">No SRT/VTT subtitles were found.</span>';
    if (status) status.textContent = state.subtitleOptions.length ? "Found ".concat(state.subtitleOptions.length, " subtitle tracks.") : "No matching subtitle tracks were found.";
  } catch (error) {
    output.textContent = "Subtitle search failed: ".concat(error.message);
    if (status) status.textContent = "Subtitle search failed: ".concat(error.message);
  }
}
async function testSubdlConnection() {
  const apiKey = configuredSubdlApiKey();
  if (!apiKey) throw new Error("No SubDL API key is configured.");
  const params = new URLSearchParams({
    api_key: apiKey,
    film_name: "ECHO NOW connection test",
    languages: "EN"
  });
  const response = await fetch("https://api.subdl.com/api/v1/subtitles?".concat(params));
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result.status) {
    throw new Error(result.error || "SubDL connection test failed (".concat(response.status, ")."));
  }
}
async function downloadSubdlSubtitle(index, { quiet = false } = {}) {
  const option = state.subtitleOptions[index];
  if (!option) throw new Error("Subtitle selection is no longer available.");
  const response = await fetch(option.url, { headers: { "x-api-key": configuredSubdlApiKey() } });
  if (!response.ok) throw new Error("Subtitle download failed (".concat(response.status, ")."));
  const text = await response.text();
  const track = attachSubtitleText(text, option.label);
  const selector = document.getElementById("player-track");
  if (selector && track) {
    const selectTrack = () => {
      const trackIndex = Array.from(document.getElementById("video-player").textTracks).indexOf(track.track);
      const optionElement = selector.querySelector('option[value="'.concat(trackIndex, '"]'));
      if (optionElement) optionElement.textContent = "".concat(option.label, " \xB7 ").concat(option.language);
      if (state.preferences.autoApplySubtitles) setSubtitleTrack(trackIndex, true);
    };
    track.addEventListener("load", selectTrack, { once: true });
  }
  if (!quiet) toastMessage("Subtitle loaded: ".concat(option.label));
}
