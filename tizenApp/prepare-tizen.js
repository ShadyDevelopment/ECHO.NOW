"use strict";

const fs = require("fs");
const path = require("path");

// Resolve esbuild from npm cache or global
let esbuild;
try {
    esbuild = require("esbuild");
} catch (e) {
    const globCache = path.join(process.env.LOCALAPPDATA, "npm-cache/_npx");
    if (fs.existsSync(globCache)) {
        for (const sub of fs.readdirSync(globCache)) {
            const candidate = path.join(globCache, sub, "node_modules", "esbuild");
            if (fs.existsSync(candidate)) {
                esbuild = require(candidate);
                break;
            }
        }
    }
}

if (!esbuild) {
    throw new Error("esbuild not found. Please install esbuild or run npx esbuild.");
}

function convertHexAlpha(css) {
    // 8-digit hex: #RRGGBBAA
    css = css.replace(/#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})\b/g, (m, r, g, b, a) => {
        const red = parseInt(r, 16);
        const green = parseInt(g, 16);
        const blue = parseInt(b, 16);
        const alpha = +(parseInt(a, 16) / 255).toFixed(3);
        return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
    });

    // 4-digit hex: #RGBA (careful not to match CSS IDs or 6-digit hex)
    css = css.replace(/#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])\b/g, (m, r, g, b, a) => {
        const red = parseInt(r + r, 16);
        const green = parseInt(g + g, 16);
        const blue = parseInt(b + b, 16);
        const alpha = +(parseInt(a + a, 16) / 255).toFixed(3);
        return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
    });

    return css;
}

function transformCss(css) {
    // 1. Convert hex colors with alpha to rgba(...)
    css = convertHexAlpha(css);

    // 2. Inset fallbacks
    css = css.replace(/\binset:\s*0\s*;/g, "top: 0; right: 0; bottom: 0; left: 0; inset: 0;");
    css = css.replace(/\binset:\s*30%\s+0\s+0\s*;/g, "top: 30%; right: 0; bottom: 0; left: 0; inset: 30% 0 0;");

    // 3. Aspect-ratio fallbacks with fixed dimensions
    css = css.replace(
        /(\.continue-watching-card\s*\{[^}]*?)(?:height:\s*\d+px;\s*)*\baspect-ratio:\s*16\s*\/\s*9\s*;/g,
        "$1height: 158px; aspect-ratio: 16 / 9;"
    );
    css = css.replace(
        /(\.poster-art\s*\{[^}]*?)(?:height:\s*\d+px;\s*)*\baspect-ratio:\s*\.?68\s*;/g,
        "$1height: 236px; aspect-ratio: .68;"
    );
    css = css.replace(
        /(\.catalog-media-card\s*\{[^}]*?)(?:height:\s*\d+px;\s*)*\baspect-ratio:\s*\.?68\s*;/g,
        "$1height: 279px; aspect-ratio: .68;"
    );
    css = css.replace(
        /(\.settings-subtitle-preview-screen\s*\{[^}]*?)(?:height:\s*\d+px;\s*)*\baspect-ratio:\s*16\s*\/\s*9\s*;/g,
        "$1height: 382px; max-height: 40vh; aspect-ratio: 16 / 9;"
    );

    // 4. Clamp & Min static fallbacks
    css = css.replace(
        /grid-template-rows:\s*repeat\(4,\s*minmax\(44px,\s*clamp\([^)]+\)\)\);/g,
        "grid-template-rows: repeat(4, minmax(48px, 60px)); grid-gap: 10px; gap: 10px;"
    );
    css = css.replace(
        /\.hero h1\s*\{\s*font-size:\s*clamp\([^)]+\);\s*\}/g,
        ".hero h1 { font-size: 40px; font-size: clamp(36px, 2.5vw, 44px); }"
    );
    css = css.replace(
        /\.details-hero-copy h1\s*\{\s*margin:\s*0 0 8px;\s*color:\s*#fff;\s*font-size:\s*clamp\([^)]+\);/g,
        ".details-hero-copy h1 { margin: 0 0 8px; color: #fff; font-size: 42px; font-size: clamp(36px, 4vw, 56px);"
    );
    css = css.replace(
        /\.player-heading strong\s*\{\s*font-size:\s*clamp\([^)]+\);/g,
        ".player-heading strong { font-size: 20px; font-size: clamp(18px, 1.8vw, 24px);"
    );
    css = css.replace(
        /width:\s*min\((\d+px),\s*[^)]+\);/g,
        "width: $1; max-width: 92vw; width: min($1, calc(100vw - 32px));"
    );
    css = css.replace(
        /right:\s*clamp\(16px,\s*4vw,\s*56px\);\s*bottom:\s*clamp\(20px,\s*4vw,\s*56px\);/g,
        "right: 32px; bottom: 32px; right: clamp(16px, 4vw, 56px); bottom: clamp(20px, 4vw, 56px);"
    );
    css = css.replace(
        /font-size:\s*clamp\(14px,\s*2\.5vw,\s*var\(--subtitle-size,\s*22px\)\);/g,
        "font-size: 22px; font-size: clamp(14px, 2.5vw, var(--subtitle-size, 22px));"
    );

    // 5. CSS Grid grid-gap fallbacks
    css = css.replace(/display:\s*grid;\s*gap:\s*([^;]+);/g, "display: grid; grid-gap: $1; gap: $1;");

    // 6. Comprehensive Chromium 56 Flex Gap fallbacks & TV styling
    const flexGapFallbacks = `
/* Chromium 56 Flex Gap Fallbacks & TV Optimizations */
.topbar > * + * { margin-top: 8px; }
.topbar.compact > * + * { margin-top: 8px; }
.brand > * + * { margin-left: 8px; }
.nav-button > * + * { margin-left: 14px; }
.profile-button > * + * { margin-left: 12px; }
.card-row > * { margin-right: 14px; flex-shrink: 0; }
.continue-watching-row > * { margin-right: 14px; flex-shrink: 0; }
.cast-row > * { margin-right: 14px; flex-shrink: 0; }
.settings-category-nav > * + * { margin-top: 8px; }
.settings-category-nav > * + * { margin-left: 8px; }
.settings-category-nav > * { flex-shrink: 0; }
.meta > * + *, .details-meta > * + * { margin-left: 8px; }
@media (max-width: 900px) {
    .settings-category-nav > * + * { margin-top: 0; }
}
.action-row > * { margin-right: 12px; margin-bottom: 12px; }
.details-actions > * { margin-right: 10px; margin-bottom: 8px; }
.details-actions .action-button > * + * { margin-left: 8px; }
.catalog-filters > * + * { margin-left: 8px; }
.player-transport-controls > * + * { margin-left: 14px; }
.player-utility-row > * + * { margin-left: 10px; }
.player-skip-action > * + * { margin-left: 8px; }
.subtitle-dialog-actions > * { margin-right: 9px; margin-bottom: 8px; }
.subtitle-adjustments > * { margin-right: 9px; margin-bottom: 8px; }
.up-next-actions > * + * { margin-left: 8px; }
.search-form > * + * { margin-left: 12px; }
.episode-chip > * + * { margin-left: 14px; }
.home-error > * + * { margin-left: 10px; }
.profile-list > * { margin-right: 18px; margin-bottom: 18px; }
.profile-switcher-list > * { margin-right: 12px; flex-shrink: 0; }
.profile-avatar-categories > * { margin-right: 8px; flex-shrink: 0; }
.profile-editor-actions > * + *, .profile-switcher-actions > * + * { margin-left: 10px; }
.details-section-heading > * + * { margin-left: 14px; }
.catalog-filters > * + * { margin-top: 10px; margin-left: 0; }
.catalog-sort-chips > *, .catalog-genre-options > * { margin-right: 8px; margin-bottom: 8px; }
.catalog-genre-filter > * + * { margin-top: 8px; }
.episode-copy > * + * { margin-top: 7px; }

/* AVPlay Hardware Video Layer Transparency */
:root.avplay-active,
html.avplay-active,
body.avplay-active,
.app-shell.avplay-active,
#main.avplay-active,
.player-shell.avplay-active,
.player-shell.avplay-active .player-media {
    background: transparent !important;
    background-color: transparent !important;
}
.player-shell.avplay-active video {
    display: none !important;
}

/* Shape-matched filled focus state; avoid a generic ring overlay. */
.continue-watching-card:focus,
.poster-card.focusable:focus,
.catalog-media-card:focus,
.episode-chip:focus {
    border-color: #31a7a0 !important;
    background-color: rgba(49, 167, 160, 0.24) !important;
    box-shadow: 0 16px 32px rgba(0, 0, 0, 0.72) !important;
}
.focusable:focus,
.focusable:focus-visible {
    position: relative;
    z-index: 5;
    outline: none !important;
    border-color: #31a7a0 !important;
    background-color: rgba(49, 167, 160, 0.28) !important;
    box-shadow: none !important;
}
`;
    if (!css.includes("Chromium 56 Flex Gap Fallbacks")) {
        css += flexGapFallbacks;
    }

    return css;
}

function processDirectory(targetDir) {
    const walk = (dir) => {
        const files = fs.readdirSync(dir);
        for (const file of files) {
            const fullPath = path.join(dir, file);
            const stat = fs.statSync(fullPath);
            if (stat.isDirectory()) {
                walk(fullPath);
            } else if (file.endsWith(".js") && file !== "hls.min.js" && file !== "tizen-polyfills.js" && file !== "prepare-tizen.js" && file !== "dev-server.js") {
                const code = fs.readFileSync(fullPath, "utf8");
                try {
                    const result = esbuild.transformSync(code, {
                        target: "chrome56",
                        loader: "js"
                    });
                    fs.writeFileSync(fullPath, result.code, "utf8");
                    console.log(`[Transpiled JS] ${path.relative(targetDir, fullPath)}`);
                } catch (err) {
                    if (err.errors && err.errors[0]) {
                        console.error("EXACT ERROR:", err.errors[0].text, "AT LINE:", err.errors[0].location.line, "COL:", err.errors[0].location.column);
                        console.error("LINE TEXT:", err.errors[0].location.lineText);
                    }
                    console.error(`[Error Transpiling] ${fullPath}:`, err);
                    throw err;
                }
            } else if (file === "app.css") {
                const css = fs.readFileSync(fullPath, "utf8");
                const transformed = transformCss(css);
                fs.writeFileSync(fullPath, transformed, "utf8");
                console.log(`[Transformed CSS] ${path.relative(targetDir, fullPath)}`);
            }
        }
    };

    walk(targetDir);
}

const targetDir = process.argv[2] ? path.resolve(process.argv[2]) : __dirname;
console.log(`Preparing Tizen 4.0 compatibility in: ${targetDir}`);
processDirectory(targetDir);
console.log("Tizen preparation complete.");
