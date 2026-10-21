function navState(page) {
  const navigationHidden = state.page === "details" || state.page === "player" || state.page === "profiles" && state.profiles.length === 0;
  const topbar = document.querySelector(".topbar");
  topbar == null ? void 0 : topbar.classList.toggle("navigation-hidden", navigationHidden);
  topbar == null ? void 0 : topbar.setAttribute("aria-hidden", String(navigationHidden));
  document.querySelectorAll(".nav-button").forEach((button) => {
    const active = button.dataset.action === page;
    button.classList.toggle("active", active);
    if (active) button.setAttribute("aria-current", "page");
    else button.removeAttribute("aria-current");
  });
  const profileButton = document.querySelector(".profile-button");
  const profileActive = state.page === "profiles";
  profileButton == null ? void 0 : profileButton.classList.toggle("active", profileActive);
  if (profileActive) profileButton == null ? void 0 : profileButton.setAttribute("aria-current", "page");
  else profileButton == null ? void 0 : profileButton.removeAttribute("aria-current");
  renderClock();
}
function renderHome() {
  var _a, _b, _c;
  state.page = "home";
  navState("home");
  const movies = ((_a = state.homeData) == null ? void 0 : _a.movies) || [];
  const series = ((_b = state.homeData) == null ? void 0 : _b.series) || [];
  const hero = movies.filter((item) => item.backdrop_path).reduce((best, item) => !best || Number(item.vote_average || 0) > Number(best.vote_average || 0) ? item : best, null);
  const background = (hero == null ? void 0 : hero.backdrop_path) ? "background-image:linear-gradient(0deg,#09090d 0%,#09090dbb 38%,#09090d12 100%),url('".concat(escapeHtml(imageUrl(hero.backdrop_path, "w1280")), "')") : "";
  const heroMeta = hero ? [
    (hero.release_date || "").slice(0, 4),
    "\u2605 ".concat(Number(hero.vote_average || 0).toFixed(1)),
    (hero.genre_ids || []).map((id) => movieGenres.get(Number(id))).filter(Boolean).join(" / ")
  ].filter(Boolean).map((label) => '<span class="metadata-pill">'.concat(escapeHtml(label), "</span>")).join("") : "";
  main.innerHTML = '<div class="home-page"><section class="hero" style="'.concat(background, '"><div class="hero-copy">\n        <span class="eyebrow">').concat(escapeHtml(appName.toUpperCase()), " CHOICE</span><h1>").concat(escapeHtml((hero == null ? void 0 : hero.title) || (hero == null ? void 0 : hero.name) || "Stories worth staying for"), "</h1>\n        ").concat(hero ? '<p class="meta">'.concat(heroMeta, '</p>\n        <p class="overview">').concat(escapeHtml(hero.overview || "Discover something new to watch."), '</p>\n        <div class="action-row"><button class="action-button primary focusable" data-action="hero-play" data-id="').concat(escapeHtml(hero.id), '" data-type="movie">\u25B6 Play now</button></div>') : '<p class="overview">Discover something new to watch.</p>', "\n        ").concat(state.homeError ? '<p class="home-error" role="alert">'.concat(escapeHtml(state.homeError), ' <button class="action-button focusable" data-action="retry-home">Try again</button></p>') : "", "</div></section>\n        ").concat(renderContinueWatching(), "\n        ").concat(renderRows("My favorites", "Saved movies and series", state.favoriteIds.map((id) => state.favoriteTitles[id]).filter(Boolean)), "\n        ").concat(renderRows("Trending movies", "A good story starts here", movies, true), "\n        ").concat(renderRows("Series worth staying in for", "Your next favorite, episode after episode", series, true), "\n        </div>");
  const heroActions = main.querySelector(".hero .action-row");
  if (hero && heroActions) {
    const heroProgress = progressFor({ id: hero.id, media_type: "movie" });
    const canResume = heroProgress && heroProgress.duration > 0 && heroProgress.position / heroProgress.duration < 0.99;
    const favoriteId = "movie:".concat(hero.id);
    const isFavorite = state.favoriteIds.includes(favoriteId);
    heroActions.innerHTML = '<button class="action-button primary focusable" data-action="hero-play" data-id="'.concat(escapeHtml(hero.id), '" data-type="movie" data-position="').concat(canResume ? heroProgress.position : 0, '">\u25B6 ').concat(canResume ? "Resume" : "Play now", '</button>\n        <button class="action-button outlined focusable" data-action="details" data-id="').concat(escapeHtml(hero.id), '" data-type="movie">More info</button>\n        <button class="action-button outlined focusable" data-action="hero-favorite" data-id="').concat(escapeHtml(favoriteId), '" aria-pressed="').concat(String(isFavorite), '">').concat(isFavorite ? "Remove from My list" : "Add to My list", "</button>");
  }
  attachCardPreviews(main);
  observeMediaCardMetadata(main);
  (_c = document.querySelector('.nav-button[data-action="home"]')) == null ? void 0 : _c.focus();
  if (!state.homeData && !state.homeLoading && !state.homeError) loadHome();
}
function renderContinueWatching() {
  const ordered = Object.values(state.progress[profileKey("watchProgress")] || {}).sort((left, right) => Number(right.updatedAt || 0) - Number(left.updatedAt || 0));
  const seenSeries = /* @__PURE__ */ new Set();
  const items = ordered.filter((entry) => {
    var _a, _b;
    if (!(entry.duration > 0)) return false;
    if (((_a = entry.media) == null ? void 0 : _a.media_type) === "tv" || ((_b = entry.media) == null ? void 0 : _b.type) === "SERIES") {
      if (!entry.episode || entry.position / entry.duration >= 0.99 || seenSeries.has(entry.media.id)) return false;
      seenSeries.add(entry.media.id);
      return true;
    }
    return entry.position / entry.duration < 0.99;
  });
  if (!items.length) return "";
  return '<section class="section"><div class="section-heading"><h2>Continue watching</h2><p>Pick up where you left off</p></div>\n        <div class="card-row continue-watching-row">'.concat(items.map((entry) => {
    const title = entry.media.title || entry.media.name || entry.title || "Continue watching";
    const image = imageUrl(entry.media.poster_path);
    const episodeContext = entry.media.media_type === "movie" ? "Continue movie" : entry.season && entry.episode ? "S".concat(entry.season, ":E").concat(entry.episode, " \xB7 Continue episode") : "Continue episode";
    return '<button class="continue-watching-card focusable" data-action="resume" data-id="'.concat(escapeHtml(entry.media.id), '" data-type="').concat(escapeHtml(entry.media.media_type), '" data-season="').concat(entry.season || "", '" data-episode="').concat(entry.episode || "", '" data-position="').concat(entry.position, '">\n            ').concat(image ? '<img src="'.concat(escapeHtml(image), '" alt="').concat(escapeHtml(title), ' thumbnail" loading="lazy">') : '<span class="continue-watching-fallback">'.concat(escapeHtml(title), "</span>"), '\n            <span class="continue-watching-shade"></span><span class="continue-watching-play" aria-hidden="true">\u25B6</span>\n            <span class="continue-watching-copy"><strong>').concat(escapeHtml(title), "</strong><span>").concat(escapeHtml(episodeContext), '</span></span>\n            <span class="progress-track"><span style="width:').concat(Math.min(100, Math.round(entry.position / entry.duration * 100)), '%"></span></span></button>');
  }).join(""), "</div></section>");
}
const movieGenres = /* @__PURE__ */ new Map([
  [12, "Adventure"],
  [14, "Fantasy"],
  [16, "Animation"],
  [18, "Drama"],
  [27, "Horror"],
  [28, "Action"],
  [35, "Comedy"],
  [36, "History"],
  [37, "Western"],
  [53, "Thriller"],
  [80, "Crime"],
  [99, "Documentary"],
  [878, "Science Fiction"],
  [9648, "Mystery"],
  [10402, "Music"],
  [10749, "Romance"],
  [10751, "Family"],
  [10752, "War"],
  [10770, "TV Movie"]
]);
async function loadHome() {
  if (state.homeLoading) return;
  state.homeLoading = true;
  try {
    const [movies, series] = await Promise.all([
      tmdb("/movie/popular"),
      tmdb("/tv/popular")
    ]);
    state.homeData = { movies: movies.results || [], series: series.results || [] };
    state.homeError = "";
  } catch (error) {
    state.homeError = error.message;
  } finally {
    state.homeLoading = false;
    if (state.page === "home") renderHome();
  }
}
