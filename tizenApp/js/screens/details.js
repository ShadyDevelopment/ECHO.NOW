async function openDetails(id, type) {
  if (!id) return;
  state.previousPage = state.page === "details" ? state.previousPage : state.page;
  state.page = "details";
  navState("");
  main.innerHTML = '<div class="page"><p class="loading">Loading title details\u2026</p></div>';
  try {
    const mediaType = type === "tv" || type === "series" ? "tv" : "movie";
    const detail = await tmdb("/".concat(mediaType, "/").concat(id));
    detail.media_type = mediaType;
    if (!detail.imdb_id) {
      try {
        const ext = await tmdb("/".concat(mediaType, "/").concat(id, "/external_ids"));
        if (ext == null ? void 0 : ext.imdb_id) detail.imdb_id = ext.imdb_id;
      } catch (_) {
      }
    }
    state.currentTitle = detail;
    state.currentDetails = detail;
    const savedId = "".concat(mediaType, ":").concat(detail.id);
    if (!state.favoriteTitles[savedId]) {
      state.favoriteTitles[savedId] = detail;
      saveState(profileKey("favoriteTitles"), state.favoriteTitles);
    }
    renderDetails(detail, mediaType);
    loadTitleExtras(detail, mediaType);
  } catch (error) {
    main.innerHTML = '<div class="page"><h1>Could not load details</h1><p class="muted">'.concat(escapeHtml(error.message), '</p><button class="action-button focusable" data-action="home">Home</button></div>');
  }
}
async function loadTitleExtras(media, type) {
  var _a;
  const output = document.getElementById("title-extras");
  if (!output) return;
  output.innerHTML = '<section class="details-section"><h2>Cast</h2><p class="loading">Loading cast\u2026</p></section>';
  const trailerButton = document.getElementById("details-trailer");
  if (trailerButton) {
    trailerButton.disabled = true;
    trailerButton.textContent = "Loading trailer\u2026";
  }
  const [credits, videos] = await Promise.allSettled([
    tmdb("/".concat(type, "/").concat(media.id, "/credits")),
    tmdb("/".concat(type, "/").concat(media.id, "/videos"))
  ]);
  if (((_a = state.currentTitle) == null ? void 0 : _a.id) !== media.id) return;
  const cast = credits.status === "fulfilled" ? credits.value.cast || [] : [];
  const clips = videos.status === "fulfilled" ? videos.value.results || [] : [];
  const trailer = clips.find((video) => video.site === "YouTube" && video.type === "Trailer" && video.official) || clips.find((video) => video.site === "YouTube" && video.type === "Trailer") || clips.find((video) => video.site === "YouTube" && video.type === "Teaser");
  state.currentTrailerUrl = trailer ? "https://www.youtube.com/watch?v=".concat(encodeURIComponent(trailer.key)) : "";
  if (trailerButton) {
    trailerButton.disabled = !state.currentTrailerUrl;
    trailerButton.textContent = state.currentTrailerUrl ? "Watch trailer" : "No trailer available";
  }
  const castCards = cast.slice(0, 20).map((member) => {
    const castLabel = [member.name, member.character].filter(Boolean).join(", ");
    const profile = member.profile_path
      ? '<img src="'.concat(escapeHtml(imageUrl(member.profile_path, "w185")), '" alt="">')
      : '<span class="cast-avatar-fallback" aria-hidden="true">\u25CF</span>';
    return '<button type="button" class="cast-card focusable" aria-label="'.concat(
      escapeHtml(castLabel),
      '">',
      profile,
      "<strong>",
      escapeHtml(member.name),
      '</strong><span class="muted">',
      escapeHtml(member.character || ""),
      "</span></button>",
    );
  }).join("");
  output.innerHTML = '<section class="details-section"><h2>Cast</h2>'.concat(
    cast.length
      ? '<div class="cast-row" role="group" aria-label="Cast">'.concat(castCards, "</div>")
      : '<p class="muted">'.concat(credits.status === "rejected" ? "Cast could not be loaded." : "No cast information available.", "</p>"),
    "</section>",
  );
}
function renderDetails(media, type) {
  var _a;
  state.page = "details";
  navState("");
  const id = "".concat(type, ":").concat(media.id);
  const saved = state.favoriteIds.includes(id);
  const title = media.title || media.name || "Untitled";
  const progress = progressFor(media);
  const seasons = (media.seasons || []).filter((season) => season.season_number > 0);
  const selectedSeason = seasons.find((season) => season.season_number === state.selectedSeason) || seasons[0];
  const backdrop = media.backdrop_path ? imageUrl(media.backdrop_path, "w1280") : "";
  const released = (media.release_date || media.first_air_date || "").slice(0, 4);
  const genres = (media.genres || []).map((genre) => genre.name).filter(Boolean).join("  /  ");
  const metaItems = [released, "\u2605 ".concat(Number(media.vote_average || 0).toFixed(1)), genres].filter(Boolean);
  const meta = metaItems.join("  \xB7  ");
  main.innerHTML = '<section class="details-page">\n        <header class="details-hero'.concat(backdrop ? "" : " details-hero-no-backdrop", '" style="').concat(backdrop ? "background-image:linear-gradient(0deg,#09090d 0%,#09090db8 40%,#09090d12 100%),url('".concat(escapeHtml(backdrop), "')") : "", '">\n            <button class="details-back focusable" data-action="back" aria-label="Back">\n                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 12H4m0 0 7-7m-7 7 7 7" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>\n            </button>\n            <div class="details-hero-copy">\n                <h1>').concat(escapeHtml(title), '</h1>\n                <p class="details-meta">').concat(escapeHtml(meta), '</p>\n            </div>\n        </header>\n        <div class="details-content">\n            <p class="overview">').concat(escapeHtml(media.overview || "No overview is available."), '</p>\n            <div class="details-actions">\n        ').concat(type === "movie" ? progress && progress.position / progress.duration < 0.99 ? '<button class="action-button primary focusable" data-action="play" data-type="movie" data-position="'.concat(progress.position, '"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5v14l11-7z"/></svg> Continue watching</button><button class="action-button outlined focusable" data-action="play" data-type="movie" data-position="0">Watch again</button>') : '<button class="action-button primary focusable" data-action="play" data-type="movie" data-position="0"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5v14l11-7z"/></svg> '.concat(progress ? "Watch again" : "Play", "</button>") : '<button class="action-button primary focusable" data-action="play-series" data-type="tv" data-season="'.concat((progress == null ? void 0 : progress.season) || (selectedSeason == null ? void 0 : selectedSeason.season_number) || 1, '" data-episode="').concat((progress == null ? void 0 : progress.episode) || 1, '" data-position="').concat(progress ? progress.position : 0, '"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5v14l11-7z"/></svg> ').concat(progress ? "Continue watching \xB7 S".concat(progress.season, ":E").concat(progress.episode) : "Play \xB7 S".concat((selectedSeason == null ? void 0 : selectedSeason.season_number) || 1, ":E1"), "</button>"), '\n                    <button class="action-button outlined focusable" id="details-trailer" data-action="details-trailer" type="button" disabled>Loading trailer\u2026</button>\n                    <button class="details-favorite focusable').concat(saved ? " is-favorite" : "", '" data-action="favorite" data-id="').concat(escapeHtml(id), '" aria-label="').concat(saved ? "Remove from favorites" : "Add to favorites", '" title="').concat(saved ? "Remove from favorites" : "Add to favorites", '">\n                        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="').concat(saved ? "M12 21.35 10.55 20C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09A6 6 0 0 1 16.5 3C19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.51z" : "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78ZM12 18.4l-6.36-6.37-1.06-1.06a3.5 3.5 0 1 1 4.95-4.95L12 8.5l2.47-2.48a3.5 3.5 0 1 1 4.95 4.95l-1.06 1.06z", '"/></svg>\n                    </button>\n            </div>\n            <div id="title-extras"></div>\n            ').concat(type === "tv" ? '<section class="details-section details-episodes"><div class="details-section-heading"><h2>Episodes</h2><label class="details-season-select"><span class="visually-hidden">Choose season</span><select id="season-select" class="select-input focusable">'.concat(seasons.map((season) => '<option value="'.concat(season.season_number, '"').concat(season.season_number === (selectedSeason == null ? void 0 : selectedSeason.season_number) ? " selected" : "", ">Season ").concat(season.season_number, "</option>")).join(""), '</select><span aria-hidden="true">\u2304</span></label></div><div id="episodes" class="episode-list"></div></section>') : "", "\n        </div>\n    </section>");
  const metaContainer = main.querySelector(".details-meta");
  if (metaContainer) {
    metaContainer.innerHTML = metaItems.map((value) => '<span class="metadata-pill">'.concat(escapeHtml(value), "</span>")).join("");
  }
  (_a = main.querySelector('[data-action^="play"]')) == null ? void 0 : _a.focus();
  if (type === "tv" && selectedSeason) loadEpisodes(media.id, selectedSeason.season_number);
}
async function loadEpisodes(seriesId, season) {
  const output = document.getElementById("episodes");
  if (!output) return;
  output.innerHTML = '<span class="muted">Loading episodes\u2026</span>';
  try {
    const result = await tmdb("/tv/".concat(seriesId, "/season/").concat(season));
    output.innerHTML = (result.episodes || []).map((episode) => {
      const episodeProgress = progressFor(state.currentTitle, { season: Number(season), episode: Number(episode.episode_number) });
      const resume = (episodeProgress == null ? void 0 : episodeProgress.position) / (episodeProgress == null ? void 0 : episodeProgress.duration) < 0.99;
      const progressWidth = episodeProgress && episodeProgress.duration > 0
        ? Math.max(0, Math.min(100, Math.round(episodeProgress.position / episodeProgress.duration * 100)))
        : 0;
      const duration = episode.runtime ? "".concat(episode.runtime, " min") : "Runtime unavailable";
      const still = episode.still_path ? imageUrl(episode.still_path, "w500") : "";
      const synopsis = episode.overview || "Play this episode.";
      return `<button class="episode-chip focusable${episodeProgress ? " watched" : ""}" data-action="play-episode" data-season="${Number(season)}" data-episode="${Number(episode.episode_number)}" data-position="${resume ? episodeProgress.position : 0}">
                <span class="episode-art">${still ? `<img src="${escapeHtml(still)}" alt="" loading="lazy">` : '<span class="episode-art-fallback">▶</span>'}${episodeProgress ? `<span class="episode-progress-track"><span style="width:${progressWidth}%"></span></span>` : ""}</span>
                <span class="episode-copy"><strong>E${Number(episode.episode_number)} · ${escapeHtml(episode.name || "Episode")}</strong><span class="episode-runtime">${escapeHtml(duration)}${resume ? " · Continue watching" : episodeProgress ? " · Watched" : ""}</span><span class="episode-overview">${escapeHtml(synopsis)}</span></span>
                <span class="episode-play-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg></span>
            </button>`;
    }).join("") || '<span class="muted">No episodes available.</span>';
  } catch (error) {
    output.innerHTML = '<span class="muted">'.concat(escapeHtml(error.message), "</span>");
  }
}
function toggleFavorite(id, media = state.currentTitle) {
  if (!media) return;
  const normalizedId = id || "".concat(media.media_type, ":").concat(media.id);
  if (state.favoriteIds.includes(normalizedId)) {
    state.favoriteIds = state.favoriteIds.filter((favorite) => favorite !== normalizedId);
    delete state.favoriteTitles[normalizedId];
  } else {
    state.favoriteIds.push(normalizedId);
    state.favoriteTitles[normalizedId] = media;
  }
  saveState(profileKey("favoriteIds"), state.favoriteIds);
  saveState(profileKey("favoriteTitles"), state.favoriteTitles);
  if (state.page === "details") {
    const button = main.querySelector('[data-action="favorite"]');
    const saved = state.favoriteIds.includes(normalizedId);
    if (button) {
      const label = saved ? "Remove from favorites" : "Add to favorites";
      button.classList.toggle("is-favorite", saved);
      button.setAttribute("aria-label", label);
      button.title = label;
      button.focus();
    }
  } else if (state.page === "search") renderSearchResults();
  toastMessage(state.favoriteIds.includes(normalizedId) ? "Added to My list." : "Removed from My list.");
}
function renderSearchResults() {
  const output = document.getElementById("search-results");
  if (!output) return;
  if (state.searchObserver) {
    state.searchObserver.disconnect();
    state.searchObserver = null;
  }
  output.innerHTML = state.searchResults.length ? '<div class="results-grid">'.concat(state.searchResults.map(searchResultCard).join(""), "</div>") : '<div class="empty-state"><div><h2>No results found</h2><p>Try another title.</p></div></div>';
  attachCardPreviews(output);
  observeMediaCardMetadata(output);
  renderSearchPagination();
}
