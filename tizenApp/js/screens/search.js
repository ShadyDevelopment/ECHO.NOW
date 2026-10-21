function renderSearch() {
  state.page = "search";
  navState("search");
  main.innerHTML = '<section class="page search-page"><div class="page-title"><h1>Search</h1><p class="muted">Find your next movie or series</p></div>\n' +
    '<form class="search-form" id="search-form"><input id="search-query" class="text-input focusable" type="search" autocomplete="off" placeholder="Search titles" aria-label="Search titles" value="' + escapeHtml(state.searchQuery || "") + '">\n' +
    '<button class="action-button focusable" type="button" data-action="clear-search" aria-label="Clear search">Clear</button></form><p class="search-result-count muted" id="search-result-count" aria-live="polite"></p><div id="search-results"></div></section>';
  const input = document.getElementById("search-query");
  input.focus();
  if ((state.searchQuery || "").trim()) searchTitles(state.searchQuery);
  else renderSearchPrompt();
}

function renderSearchPrompt() {
  const output = document.getElementById("search-results");
  if (!output) return;
  output.innerHTML = '<div class="empty-state"><div><h2>Search ECHO NOW</h2><p>Enter a title to get started.</p></div></div>';
  setSearchStatus("Search movies and series");
}

function setSearchStatus(message) {
  const count = document.getElementById("search-result-count");
  if (!count) return;
  count.classList.remove("is-updating");
  count.textContent = message;
  void count.offsetWidth;
  count.classList.add("is-updating");
}

function searchSkeletonCards(count) {
  let cards = "";
  for (let i = 0; i < count; i += 1) {
    cards += '<div class="search-skeleton-card" aria-hidden="true"><div class="search-skeleton-poster"></div><div class="search-skeleton-line"></div><div class="search-skeleton-line short"></div></div>';
  }
  return cards;
}

async function searchTitles(query, page) {
  const requestedPage = page || 1;
  const output = document.getElementById("search-results");
  if (!output) return;
  const normalizedQuery = (query || "").trim();
  if (!normalizedQuery) {
    state.searchRequestId = (state.searchRequestId || 0) + 1;
    state.searchResults = [];
    state.searchPage = 0;
    state.searchTotalPages = 0;
    state.searchLoading = false;
    state.searchLoadingMore = false;
    state.searchError = "";
    output.innerHTML = "";
    renderSearchPrompt();
    return;
  }

  const firstPage = requestedPage === 1;
  if (firstPage) {
    state.searchRequestId = (state.searchRequestId || 0) + 1;
    if (state.searchObserver) {
      state.searchObserver.disconnect();
      state.searchObserver = null;
    }
    state.searchQuery = normalizedQuery;
    state.searchResults = [];
    state.searchPage = 0;
    state.searchTotalPages = 0;
    state.searchLoading = true;
    state.searchLoadingMore = false;
    state.searchError = "";
    output.innerHTML = '<div class="results-grid search-skeleton-grid">' + searchSkeletonCards(8) + "</div>";
    setSearchStatus("Searching…");
  } else {
    if (state.searchLoading || state.searchLoadingMore || requestedPage !== state.searchPage + 1 || requestedPage > state.searchTotalPages) return;
    if (state.searchObserver) {
      state.searchObserver.disconnect();
      state.searchObserver = null;
    }
    state.searchLoadingMore = true;
    state.searchError = "";
    renderSearchPagination();
    setSearchStatus("Loading more results…");
  }

  const requestId = state.searchRequestId;
  try {
    const result = await tmdb("/search/multi", { query: normalizedQuery, include_adult: "false", page: String(requestedPage) });
    if (requestId !== state.searchRequestId || normalizedQuery !== (state.searchQuery || "").trim()) return;
    const pageResults = (result.results || []).filter((item) => item.media_type === "movie" || item.media_type === "tv");
    let appendedResults = pageResults;
    if (firstPage) state.searchResults = pageResults;
    else {
      const existing = new Set(state.searchResults.map((item) => (item.media_type || "movie") + ":" + item.id));
      appendedResults = pageResults.filter((item) => {
        const key = (item.media_type || "movie") + ":" + item.id;
        if (existing.has(key)) return false;
        existing.add(key);
        return true;
      });
      state.searchResults = state.searchResults.concat(appendedResults);
    }
    state.searchPage = Number(result.page) || requestedPage;
    state.searchTotalPages = Math.min(Number(result.total_pages) || 0, 500);
    state.searchLoading = false;
    state.searchLoadingMore = false;
    state.searchError = "";
    if (firstPage) renderSearchResults();
    else appendSearchResults(appendedResults);
    setSearchStatus(state.searchResults.length + (state.searchResults.length === 1 ? " result found" : " results found"));
    if (firstPage) focusSearchFirstResultAfterSubmit();
  } catch (error) {
    if (requestId !== state.searchRequestId) return;
    state.searchLoading = false;
    state.searchLoadingMore = false;
    state.searchError = error && error.message ? error.message : "Search failed.";
    if (firstPage) {
      output.innerHTML = '<div class="empty-state"><div><h2>Search unavailable</h2><p>' + escapeHtml(state.searchError) + '</p><button class="action-button focusable" type="button" data-action="retry-search">Try again</button></div></div>';
    } else {
      renderSearchPagination();
    }
    setSearchStatus("Search unavailable");
  }
}

function appendSearchResults(results) {
  const output = document.getElementById("search-results");
  const grid = output && output.querySelector(".results-grid");
  if (!output || !grid || !results.length) {
    renderSearchResults();
    return;
  }
  const pagination = output.querySelector(".search-pagination");
  if (pagination) pagination.remove();
  const newCards = document.createElement("div");
  newCards.innerHTML = results.map(searchResultCard).join("");
  attachCardPreviews(newCards);
  while (newCards.firstChild) grid.appendChild(newCards.firstChild);
  observeMediaCardMetadata(grid);
  renderSearchPagination();
}

function renderSearchPagination() {
  const output = document.getElementById("search-results");
  if (!output) return;
  let pagination = output.querySelector(".search-pagination");
  if (!pagination) {
    pagination = document.createElement("div");
    pagination.className = "search-pagination";
    pagination.setAttribute("aria-live", "polite");
    output.appendChild(pagination);
  }
  if (state.searchLoadingMore) {
    pagination.innerHTML = '<div class="search-more-skeleton" aria-label="Loading more results">' + searchSkeletonCards(4) + "</div>";
  } else if (state.searchError && state.searchPage < state.searchTotalPages) {
    pagination.innerHTML = '<p class="muted">' + escapeHtml(state.searchError) + '</p><button class="action-button focusable" type="button" data-action="retry-search-more">Retry</button>';
  } else if (state.searchPage < state.searchTotalPages) {
    pagination.innerHTML = '<button class="action-button focusable" type="button" data-action="load-search-more">Load more</button>';
    observeSearchPagination(pagination);
  } else {
    pagination.innerHTML = "";
  }
}

function observeSearchPagination(pagination) {
  if (!("IntersectionObserver" in window) || pagination.dataset.observing === "true") return;
  state.searchObserver = new IntersectionObserver((entries) => {
    if (entries.some((entry) => entry.isIntersecting)) searchTitles(state.searchQuery, state.searchPage + 1);
  }, { root: document.querySelector("main"), rootMargin: "320px 0px" });
  state.searchObserver.observe(pagination);
  pagination.dataset.observing = "true";
}

function focusSearchFirstResultAfterSubmit() {
  if (!state.searchFocusFirstAfterSubmit || !state.searchResults.length) return;
  state.searchFocusFirstAfterSubmit = false;
  const firstResult = document.querySelector("#search-results .search-result-card .poster-card.focusable");
  if (firstResult) firstResult.focus();
}
