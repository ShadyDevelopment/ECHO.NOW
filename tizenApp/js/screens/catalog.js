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
const catalogConfiguration = {
  movies: { type: "movie", title: "Movies" },
  series: { type: "tv", title: "Series" }
};
const catalogSortOptions = ["Recommended", "Title A–Z", "Year: newest", "Year: oldest", "Top rated", "Shortest runtime", "Longest runtime"];
const mediaDetailsCache = /* @__PURE__ */ new Map();
function catalogFilterControls(catalog) {
  const genres = catalog.genres || [];
  const selectedGenre = genres.find((genre) => String(genre.id) === catalog.genre);
  const sortChips = catalogSortOptions.map((option) =>
    '<button type="button" class="catalog-filter-chip focusable'.concat(option === catalog.sort ? " selected" : "", '" data-catalog-filter="sort" data-filter-value="').concat(escapeHtml(option), '" aria-pressed="').concat(String(option === catalog.sort), '">').concat(escapeHtml(option), "</button>")
  ).join("");
  const genreChips = ['<button type="button" class="catalog-filter-chip focusable'.concat(catalog.genre ? "" : " selected", '" data-catalog-filter="genre" data-filter-value="" aria-pressed="').concat(String(!catalog.genre), '">All genres</button>')].concat(
    genres.map((genre) =>
      '<button type="button" class="catalog-filter-chip focusable'.concat(String(genre.id) === catalog.genre ? " selected" : "", '" data-catalog-filter="genre" data-filter-value="').concat(escapeHtml(String(genre.id)), '" aria-pressed="').concat(String(String(genre.id) === catalog.genre), '">').concat(escapeHtml(genre.name), "</button>")
    )
  ).join("");
  const genreOptions = ['<option value="">All genres</option>'].concat(
    genres.map((genre) => '<option value="'.concat(escapeHtml(String(genre.id)), '"').concat(String(genre.id) === catalog.genre ? " selected" : "", ">").concat(escapeHtml(genre.name), "</option>"))
  ).join("");
  const sortOptions = catalogSortOptions.map((option) =>
    "<option".concat(option === catalog.sort ? " selected" : "", ">").concat(escapeHtml(option), "</option>")
  ).join("");
  return '<div class="catalog-filters"><div class="catalog-sort-chips" role="group" aria-label="Sort titles">'.concat(sortChips, '</div>\n        <div class="catalog-genre-filter"><button type="button" id="catalog-genre-toggle" class="catalog-genre-toggle focusable" aria-expanded="false" aria-controls="catalog-genre-options">Genres: ').concat(escapeHtml(selectedGenre ? selectedGenre.name : "All"), '</button>\n        <div id="catalog-genre-options" class="catalog-genre-options" hidden role="group" aria-label="Filter by genre">').concat(genreChips, '</div></div>\n        <select id="catalog-genre" class="catalog-filter-state" aria-hidden="true" tabindex="-1">').concat(genreOptions, '</select>\n        <select id="catalog-sort" class="catalog-filter-state" aria-hidden="true" tabindex="-1">').concat(sortOptions, "</select></div>");
}
function mediaCardMetadata(media, type) {
  const year = (media.release_date || media.first_air_date || "").slice(0, 4);
  const rating = Number(media.vote_average || media.rating || 0);
  const seasonCount = Number(media.number_of_seasons || media.season_count || (media.seasons || []).filter((season) => Number(season.season_number) > 0).length || 0);
  const values = [year || "Year unavailable"];
  if (type !== "movie") values.push(seasonCount > 0 ? seasonCount === 1 ? "1 Season" : "".concat(seasonCount, " Seasons") : "Seasons…");
  return { details: values.join(" · "), rating: rating > 0 ? rating.toFixed(1) : "N/A" };
}
function mediaCardMetadataMarkup(media, type) {
  const metadata = mediaCardMetadata(media, type);
  const rating = '<span class="card-meta-rating" aria-label="Rating '.concat(
    escapeHtml(metadata.rating),
    '">\u2605 ',
    escapeHtml(metadata.rating),
    "</span>",
  );
  return '<span class="card-meta-copy">'.concat(escapeHtml(metadata.details), "</span>", rating);
}
function catalogMediaCard(media) {
  const title = media.title || media.name || "Untitled";
  const type = media.media_type || media.type || (media.name ? "tv" : "movie");
  const image = imageUrl(media.poster_path);
  const backdrop = media.backdrop_path ? imageUrl(media.backdrop_path, "w780") : "";
  const metadata = mediaCardMetadataMarkup(media, type);
  const artwork = image ? '<img src="'.concat(escapeHtml(image), '" alt="').concat(escapeHtml(title), ' poster" loading="lazy">') : '<span class="catalog-media-fallback">'.concat(escapeHtml(title), "</span>");
  return `<button class="catalog-media-card focusable" data-action="details" data-id="${escapeHtml(media.id)}" data-type="${escapeHtml(type)}"${backdrop ? ` data-preview-src="${escapeHtml(backdrop)}"` : ""} aria-label="Open ${escapeHtml(title)}">
        <span class="catalog-media-art">${artwork}${backdrop ? '<img class="catalog-media-preview" alt="" loading="lazy">' : ""}</span>
        <span class="catalog-card-title"><span class="card-title-text">${escapeHtml(title)}</span></span>
        <span class="catalog-card-meta" data-media-metadata data-media-id="${escapeHtml(media.id)}" data-media-type="${escapeHtml(type)}">${metadata}</span>
    </button>`;
}
function observeMediaCardMetadata(container) {
  var _a;
  (_a = state.mediaMetadataObserver) == null ? void 0 : _a.disconnect();
  const cards = Array.from(container.querySelectorAll("[data-media-metadata]"));
  const loadMetadata = (element) => {
    const id = element.dataset.mediaId;
    const type = element.dataset.mediaType === "tv" || element.dataset.mediaType === "series" ? "tv" : "movie";
    if (!id) return;
    const key = "".concat(type, ":").concat(id);
    if (!mediaDetailsCache.has(key)) {
      mediaDetailsCache.set(key, tmdb("/".concat(type, "/").concat(encodeURIComponent(id))));
    }
    mediaDetailsCache.get(key).then((detail) => {
      const metadata = mediaCardMetadataMarkup(detail, type);
      container.querySelectorAll("[data-media-metadata]").forEach((item) => {
        if (item.dataset.mediaId === id && (item.dataset.mediaType === type || item.dataset.mediaType === "series" && type === "tv")) {
          item.innerHTML = metadata;
        }
      });
    }).catch(() => {
    });
  };
  if (!("IntersectionObserver" in window)) {
    cards.forEach(loadMetadata);
    return;
  }
  state.mediaMetadataObserver = new IntersectionObserver((entries) => {
    entries.filter((entry) => entry.isIntersecting).forEach((entry) => {
      var _a2;
      (_a2 = state.mediaMetadataObserver) == null ? void 0 : _a2.unobserve(entry.target);
      loadMetadata(entry.target);
    });
  }, { root: main, rootMargin: "320px" });
  cards.forEach((card) => state.mediaMetadataObserver.observe(card));
}
const DEFAULT_MOVIE_GENRES = [
  { id: 28, name: "Action" },
  { id: 12, name: "Adventure" },
  { id: 16, name: "Animation" },
  { id: 35, name: "Comedy" },
  { id: 80, name: "Crime" },
  { id: 99, name: "Documentary" },
  { id: 18, name: "Drama" },
  { id: 10751, name: "Family" },
  { id: 14, name: "Fantasy" },
  { id: 36, name: "History" },
  { id: 27, name: "Horror" },
  { id: 10402, name: "Music" },
  { id: 9648, name: "Mystery" },
  { id: 10749, name: "Romance" },
  { id: 878, name: "Science Fiction" },
  { id: 53, name: "Thriller" },
  { id: 10752, name: "War" },
  { id: 37, name: "Western" }
];
const DEFAULT_TV_GENRES = [
  { id: 10759, name: "Action & Adventure" },
  { id: 16, name: "Animation" },
  { id: 35, name: "Comedy" },
  { id: 80, name: "Crime" },
  { id: 99, name: "Documentary" },
  { id: 18, name: "Drama" },
  { id: 10751, name: "Family" },
  { id: 10762, name: "Kids" },
  { id: 9648, name: "Mystery" },
  { id: 10764, name: "Reality" },
  { id: 10765, name: "Sci-Fi & Fantasy" },
  { id: 10768, name: "War & Politics" },
  { id: 37, name: "Western" }
];
function catalogCategories(items, kind, catalog) {
  if (!items.length) return [];
  const defaultGenres = kind === "movies" ? DEFAULT_MOVIE_GENRES : DEFAULT_TV_GENRES;
  const activeGenres = catalog.genres && catalog.genres.length ? catalog.genres : defaultGenres;
  const categoryMap = new Map(activeGenres.map((genre) => [String(genre.id), genre.name]));
  if (catalog.genre) {
    const selectedName = categoryMap.get(String(catalog.genre)) || "Filtered Titles";
    return [{ title: selectedName, items }];
  }
  const trendingItems = [...items].sort((a, b) => Number(b.popularity || 0) - Number(a.popularity || 0));
  const topRatedItems = [...items].filter((item) => Number(item.vote_count || 0) >= 15).sort((a, b) => Number(b.vote_average || 0) - Number(a.vote_average || 0));
  const categories = [
    { title: "Trending Now", items: trendingItems.slice(0, 16) }
  ];
  if (topRatedItems.length >= 4) {
    categories.push({
      title: kind === "movies" ? "Top Rated Movies" : "Top Rated Series",
      items: topRatedItems.slice(0, 16)
    });
  }
  const genreCounts = /* @__PURE__ */ new Map();
  for (const item of items) {
    for (const genreId of item.genre_ids || []) {
      const name = categoryMap.get(String(genreId));
      if (name) genreCounts.set(name, (genreCounts.get(name) || 0) + 1);
    }
  }
  const sortedGenres = [...genreCounts.entries()].filter(([_, count]) => count >= 3).sort((a, b) => b[1] - a[1]).slice(0, 5);
  sortedGenres.forEach(([name]) => {
    const genreEntry = [...categoryMap.entries()].find(([, genreName]) => genreName === name);
    if (!genreEntry) return;
    const genreId = genreEntry[0];
    const genreItems = items.filter((item) => (item.genre_ids || []).some((id) => String(id) === genreId));
    if (genreItems.length >= 3) {
      categories.push({ title: name, items: genreItems.slice(0, 16) });
    }
  });
  return categories;
}
async function loadCatalog(kind, reset = false) {
  var _a;
  const config = catalogConfiguration[kind];
  const catalog = state.catalog[kind];
  if (!catalog.genres) {
    catalog.genres = kind === "movies" ? DEFAULT_MOVIE_GENRES : DEFAULT_TV_GENRES;
  }
  if (catalog.loading && !reset) return;
  if (catalog.loading) catalog.requestId += 1;
  (_a = catalog.observer) == null ? void 0 : _a.disconnect();
  if (reset) {
    catalog.items = [];
    catalog.page = 0;
    catalog.error = null;
  } else catalog.error = null;
  catalog.loading = true;
  catalog.scrollTop = reset ? 0 : main.scrollTop;
  const requestId = ++catalog.requestId;
  const nextPage = catalog.page + 1;
  const output = document.getElementById("catalog-results");
  if (output && !reset) {
    output.insertAdjacentHTML("beforeend", '<p class="loading" id="catalog-loading">Loading more '.concat(escapeHtml(config.title.toLowerCase()), "\u2026</p>"));
  }
  try {
    const sortBy = catalog.sort === "Top rated" ? "vote_average.desc" : catalog.sort === "Title A\u2013Z" ? config.type === "movie" ? "original_title.asc" : "name.asc" : catalog.sort === "Year: newest" ? config.type === "movie" ? "primary_release_date.desc" : "first_air_date.desc" : catalog.sort === "Year: oldest" ? config.type === "movie" ? "primary_release_date.asc" : "first_air_date.asc" : catalog.sort === "Shortest runtime" ? "runtime.asc" : catalog.sort === "Longest runtime" ? "runtime.desc" : "popularity.desc";
    const fetchParams = (pageNumber) => __spreadProps(__spreadValues({
      sort_by: sortBy,
      page: pageNumber
    }, catalog.genre ? { with_genres: catalog.genre } : {}), {
      "vote_count.gte": catalog.sort === "Top rated" ? 100 : 0
    });
    let newItems = [];
    let latestTotalPages = 1;
    if (reset && nextPage === 1) {
      const [res1, res2] = await Promise.all([
        tmdb("/discover/".concat(config.type), fetchParams(1)),
        tmdb("/discover/".concat(config.type), fetchParams(2)).catch(() => ({ results: [] }))
      ]);
      if (requestId !== catalog.requestId) return;
      newItems = (res1.results || []).concat(res2.results || []);
      catalog.page = res2.results && res2.results.length ? 2 : 1;
      latestTotalPages = Math.min(res1.total_pages || 1, 500);
    } else {
      const result = await tmdb("/discover/".concat(config.type), fetchParams(nextPage));
      if (requestId !== catalog.requestId) return;
      newItems = result.results || [];
      catalog.page = nextPage;
      latestTotalPages = Math.min(result.total_pages || 1, 500);
    }
    catalog.items = reset ? newItems : catalog.items.concat(newItems);
    catalog.totalPages = latestTotalPages;
  } catch (error) {
    if (requestId !== catalog.requestId) return;
    catalog.error = error.message;
    if (catalog.items.length) toastMessage(error.message);
  } finally {
    if (requestId === catalog.requestId) {
      catalog.loading = false;
      renderCatalog(kind);
    }
  }
}
async function renderCatalog(kind) {
  var _a;
  const config = catalogConfiguration[kind];
  const catalog = state.catalog[kind];
  if (!catalog.genres) {
    catalog.genres = kind === "movies" ? DEFAULT_MOVIE_GENRES : DEFAULT_TV_GENRES;
  }
  (_a = catalog.observer) == null ? void 0 : _a.disconnect();
  state.page = kind;
  navState(kind);
  const sortedItems = [...catalog.items].sort((left, right) => {
    if (catalog.sort === "Title A\u2013Z") return (left.title || left.name || "").localeCompare(right.title || right.name || "");
    if (catalog.sort === "Year: newest") return (right.release_date || right.first_air_date || "").localeCompare(left.release_date || left.first_air_date || "");
    if (catalog.sort === "Year: oldest") return (left.release_date || left.first_air_date || "").localeCompare(right.release_date || right.first_air_date || "");
    if (catalog.sort === "Top rated") return Number(right.vote_average || 0) - Number(left.vote_average || 0);
    return 0;
  });
  let output = "";
  if (sortedItems.length) {
    output = catalogCategories(sortedItems, kind, catalog).map((category) => '<section class="catalog-row"><h2>'.concat(escapeHtml(category.title), '</h2><div class="card-row">').concat(category.items.map(catalogMediaCard).join(""), "</div></section>")).join("");
  } else if (catalog.loading) {
    output = '<div class="empty-state"><div><div class="player-spinner" style="margin: 0 auto 16px;"></div><h2>Loading '.concat(escapeHtml(config.title.toLowerCase()), "\u2026</h2><p>Fetching catalog titles</p></div></div>");
  } else {
    output = '<div class="empty-state"><div><h2>No titles to show</h2><p>'.concat(escapeHtml(catalog.error || (storedCredential() ? "Try refreshing the catalog." : "TMDB credentials are missing from config.cfg.")), "</p>").concat(catalog.error ? '<button class="action-button focusable" data-action="catalog-retry" data-kind="'.concat(kind, '" data-reset="true">Retry</button>') : "", "</div></div>");
  }
  main.innerHTML = '<section class="page catalog-page"><div class="catalog-page-header">\n        <h1>'.concat(config.title, '</h1><p class="muted">Explore something worth watching</p>\n        <div class="catalog-filters">\n        <label class="catalog-filter-control"><span class="visually-hidden">Filter by genre</span><select id="catalog-genre" class="select-input focusable"><option value="">Filter</option></select><span class="catalog-filter-chevron" aria-hidden="true">\u2304</span></label>\n        <label class="catalog-filter-control"><span class="visually-hidden">Sort titles</span><select id="catalog-sort" class="select-input focusable">').concat(["Recommended", "Title A\u2013Z", "Year: newest", "Year: oldest", "Top rated", "Shortest runtime", "Longest runtime"].map((option) => "<option".concat(option === catalog.sort ? " selected" : "", ">").concat(option, "</option>")).join(""), '</select><span class="catalog-filter-chevron" aria-hidden="true">\u2304</span></label></div></div>\n        <div id="catalog-results">').concat(output, "</div>\n        ").concat(catalog.page < catalog.totalPages && sortedItems.length ? '<div class="catalog-load-more">'.concat(catalog.error ? '<p class="hint">More titles could not be loaded: '.concat(escapeHtml(catalog.error), '</p><button class="action-button focusable" data-action="catalog-retry" data-kind="').concat(kind, '">Retry loading more</button>') : '<button class="action-button focusable" data-action="load-more" data-kind="'.concat(kind, '">Load more</button>'), "</div>") : "", "</section>");
  const nativeFilterControls = main.querySelector(".catalog-filters");
  if (nativeFilterControls) nativeFilterControls.outerHTML = catalogFilterControls(catalog);
  main.querySelectorAll("[data-catalog-filter]").forEach((button) => {
    button.addEventListener("click", () => {
      const select = document.getElementById(button.dataset.catalogFilter === "genre" ? "catalog-genre" : "catalog-sort");
      if (!select || select.value === button.dataset.filterValue) return;
      select.value = button.dataset.filterValue;
      select.dispatchEvent(new Event("change", { bubbles: true }));
    });
  });
  const genreToggle = document.getElementById("catalog-genre-toggle");
  const genreOptions = document.getElementById("catalog-genre-options");
  if (genreToggle && genreOptions) {
    genreToggle.addEventListener("click", () => {
      genreOptions.hidden = !genreOptions.hidden;
      genreToggle.setAttribute("aria-expanded", String(!genreOptions.hidden));
      if (!genreOptions.hidden) {
        const firstGenre = genreOptions.querySelector(".focusable");
        if (firstGenre) firstGenre.focus();
      }
    });
  }
  attachCardPreviews(main);
  observeMediaCardMetadata(main);
  const loadMoreButton = main.querySelector('[data-action="load-more"]');
  if (loadMoreButton && !catalog.error && "IntersectionObserver" in window) {
    catalog.observer = new IntersectionObserver((entries) => {
      var _a2;
      if (entries.some((entry) => entry.isIntersecting) && !catalog.loading) {
        (_a2 = catalog.observer) == null ? void 0 : _a2.disconnect();
        loadCatalog(kind);
      }
    }, { root: main, rootMargin: "480px 0px" });
    catalog.observer.observe(loadMoreButton);
  }
  const genreSelect = document.getElementById("catalog-genre");
  if (genreSelect && catalog.genres) {
    genreSelect.innerHTML = '<option value="">Filter</option>'.concat(catalog.genres.map((genre) => '<option value="'.concat(genre.id, '"').concat(String(genre.id) === catalog.genre ? " selected" : "", ">").concat(escapeHtml(genre.name), "</option>")).join(""));
  }
  if (genreSelect && !catalog.genresLoading && (!catalog.genres || catalog.genres.length <= 13)) {
    catalog.genresLoading = true;
    tmdb("/genre/".concat(config.type, "/list")).then((response) => {
      if (response.genres && response.genres.length) {
        catalog.genres = response.genres;
        const currentSelect = state.page === kind ? document.getElementById("catalog-genre") : null;
        if (currentSelect) {
          currentSelect.innerHTML = '<option value="">Filter</option>'.concat(catalog.genres.map((genre) => '<option value="'.concat(genre.id, '"').concat(String(genre.id) === catalog.genre ? " selected" : "", ">").concat(escapeHtml(genre.name), "</option>")).join(""));
          if (catalog.items.length) renderCatalog(kind);
        }
      }
    }).catch(() => {
    }).finally(() => {
      catalog.genresLoading = false;
    });
  }
  if (catalog.page === 0 && !catalog.loading && !catalog.error) loadCatalog(kind, true);
  else if (catalog.scrollTop > 0) requestAnimationFrame(() => {
    main.scrollTop = catalog.scrollTop;
  });
}
main.addEventListener("scroll", () => {
  const kind = state.page;
  const catalog = catalogConfiguration[kind] ? state.catalog[kind] : null;
  if (!catalog || catalog.loading || catalog.error || catalog.page >= catalog.totalPages) return;
  if (main.scrollHeight - main.scrollTop - main.clientHeight < 480) loadCatalog(kind);
}, { passive: true });
