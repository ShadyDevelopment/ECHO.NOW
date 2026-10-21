function titleCard(media) {
  var _a;
  const title = media.title || media.name || "Untitled";
  const image = imageUrl(media.poster_path);
  const backdrop = media.backdrop_path ? imageUrl(media.backdrop_path, "w780") : "";
  const type = media.media_type || media.type || (media.name ? "tv" : "movie");
  const id = "".concat(type, ":").concat(media.id);
  const metadata = mediaCardMetadataMarkup(media, type);
  const progress = (_a = state.progress[profileKey("watchProgress")]) == null ? void 0 : _a[id];
  const asyncAttr = window.TIZEN_DEVICE && window.TIZEN_DEVICE.features.asyncImageDecode ? ' decoding="async"' : "";
  const imageMarkup = image ? '<img src="'.concat(escapeHtml(image), '" alt="').concat(escapeHtml(title), ' poster" loading="lazy"').concat(asyncAttr, ">") : '<div class="poster-fallback">'.concat(escapeHtml(title), "</div>");
  return '<button class="poster-card focusable" data-action="details" data-id="'.concat(escapeHtml(media.id), '" data-type="').concat(escapeHtml(type), '"').concat(backdrop ? ' data-preview-src="'.concat(escapeHtml(backdrop), '"') : "", '>\n        <span class="poster-art">').concat(imageMarkup, backdrop ? '<img class="poster-preview" alt="" loading="lazy">' : "", '<span class="media-type">').concat(type === "movie" ? "FILM" : "SERIES", '</span><span class="poster-info"><span class="poster-title"><span class="card-title-text">').concat(escapeHtml(title), '</span></span>\n        <span class="poster-meta" data-media-metadata data-media-id="').concat(escapeHtml(media.id), '" data-media-type="').concat(escapeHtml(type), '">').concat(metadata, "</span>\n        ").concat((progress == null ? void 0 : progress.duration) ? '<span class="progress-track"><span style="width:'.concat(Math.min(100, Math.round(progress.position / progress.duration * 100)), '%"></span></span>') : "", '</span></span>\n    </button>');
}
function attachCardPreviews(container) {
  container.querySelectorAll(".poster-card[data-preview-src], .catalog-media-card[data-preview-src]").forEach((card) => {
    let timer = null;
    let focused = false;
    let loadHandler = null;
    let errorHandler = null;
    const preview = card.querySelector(".poster-preview, .catalog-media-preview");
    const removePreviewListeners = () => {
      if (!preview) return;
      if (loadHandler) preview.removeEventListener("load", loadHandler);
      if (errorHandler) preview.removeEventListener("error", errorHandler);
      loadHandler = null;
      errorHandler = null;
    };
    card.addEventListener("focusin", () => {
      focused = true;
      clearTimeout(timer);
      timer = setTimeout(() => {
        if (!preview) return;
        const activatePreview = () => {
          if (focused) card.classList.add("preview-active");
        };
        if (preview.complete && preview.naturalWidth > 0) {
          activatePreview();
          return;
        }
        loadHandler = () => {
          removePreviewListeners();
          activatePreview();
        };
        errorHandler = removePreviewListeners;
        preview.addEventListener("load", loadHandler);
        preview.addEventListener("error", errorHandler);
        if (!preview.getAttribute("src")) preview.setAttribute("src", card.dataset.previewSrc);
      }, 650);
    });
    card.addEventListener("focusout", () => {
      focused = false;
      clearTimeout(timer);
      card.classList.remove("preview-active");
      removePreviewListeners();
      timer = setTimeout(() => {
        if (!focused && preview) preview.removeAttribute("src");
      }, 500);
    });
  });
  container.querySelectorAll(".poster-card, .catalog-media-card").forEach((card) => {
    const titles = Array.from(card.querySelectorAll(".poster-title, .catalog-card-title"));
    let timer = null;
    let active = false;
    const stopMarquee = () => {
      active = false;
      clearTimeout(timer);
      titles.forEach((title) => {
        const text = title.querySelector(".card-title-text");
        if (!text) return;
        text.style.transition = "transform 180ms ease";
        text.style.transform = "translateX(0)";
      });
    };
    const startMarquee = () => {
      stopMarquee();
      active = true;
      const scrollTitle = () => {
        if (!active) return;
        const title = card.classList.contains("catalog-media-card")
          ? card.querySelector(".catalog-card-title")
          : card.querySelector(".poster-title");
        if (!title || title.clientWidth === 0) return;
        const text = title.querySelector(".card-title-text");
        if (!text || text.scrollWidth <= title.clientWidth + 4) return;
        const icon = title.querySelector('[aria-hidden="true"]');
        const distance = text.scrollWidth - title.clientWidth + (icon ? icon.clientWidth + 6 : 0);
        const duration = Math.max(2200, Math.min(6500, distance * 24));
        text.style.transition = "transform ".concat(duration, "ms linear");
        text.style.transform = "translateX(-".concat(distance, "px)");
        timer = setTimeout(() => {
          if (!active) return;
          text.style.transition = "transform 500ms ease";
          text.style.transform = "translateX(0)";
          timer = setTimeout(scrollTitle, 2_000);
        }, duration + 900);
      };
      timer = setTimeout(scrollTitle, 1800);
    };
    card.addEventListener("focusin", startMarquee);
    card.addEventListener("focusout", stopMarquee);
  });
}
function searchResultCard(media) {
  const type = media.media_type || "movie";
  const id = "".concat(type, ":").concat(media.id);
  const saved = state.favoriteIds.includes(id);
  return '<div class="search-result-card">'.concat(titleCard(media), '\n        <button class="search-favorite focusable" data-action="search-favorite" data-id="').concat(escapeHtml(id), '" aria-label="').concat(saved ? "Remove from My list" : "Add to My list", '">').concat(saved ? "\u2665" : "\u2661", "</button></div>");
}
function renderRows(title, subtitle, items, showWhenEmpty = false) {
  if (!(items == null ? void 0 : items.length) && !showWhenEmpty) return "";
  return '<section class="section"><div class="section-heading"><h2>'.concat(escapeHtml(title), "</h2>").concat(subtitle ? "<p>".concat(escapeHtml(subtitle), "</p>") : "", "</div>\n        ").concat((items == null ? void 0 : items.length) ? '<div class="card-row">'.concat(items.map(catalogMediaCard).join(""), "</div>") : "", "</section>");
}
