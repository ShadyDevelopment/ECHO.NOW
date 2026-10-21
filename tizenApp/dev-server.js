"use strict";

const http = require("http");
const fs = require("fs");
const path = require("path");
const { URL } = require("url");

const PORT = 8766;
const HOST = "127.0.0.1";
const STATIC_ROOT = path.resolve(__dirname);
const APP_CONFIG = path.resolve(STATIC_ROOT, "..", "config.cfg");

function readAppConfig() {
    let contents;
    try {
        contents = fs.readFileSync(APP_CONFIG, "utf8");
    } catch {
        throw new Error("The project-root config.cfg is required to start the Tizen preview.");
    }

    const values = new Map();
    for (const line of contents.replace(/^\uFEFF/, "").split(/\r?\n/)) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith("#") || trimmed.startsWith(";")) continue;
        const match = /^([^:=\s]+)\s*[:=]\s*(.*)$/.exec(trimmed);
        if (match) values.set(match[1], match[2].trim().replace(/^(['"])(.*)\1$/, "$2"));
    }

    const required = ["APP_NAME", "APP_VERSION", "APP_WEB", "SUBDL_API_KEY", "THEMOVIEDB_API_KEY", "THEMOVIEDB_API_READ_ACCESS_TOKEN"];
    const missing = required.filter((key) => !values.has(key));
    if (missing.length) throw new Error(`Missing config.cfg keys: ${missing.join(", ")}`);
    if (!values.get("THEMOVIEDB_API_KEY") && !values.get("THEMOVIEDB_API_READ_ACCESS_TOKEN")) {
        throw new Error("config.cfg must include a TMDB API key or read access token.");
    }

    return {
        appName: values.get("APP_NAME"),
        appVersion: values.get("APP_VERSION"),
        appWeb: values.get("APP_WEB"),
        subdlApiKey: values.get("SUBDL_API_KEY"),
        tmdbApiKey: values.get("THEMOVIEDB_API_KEY"),
        tmdbReadAccessToken: values.get("THEMOVIEDB_API_READ_ACCESS_TOKEN"),
        credentialsLocked: true,
        uiColors: {
            menuBarBackground: values.get("UI_MENU_BAR_BACKGROUND") || "#0f0f0f",
            pageBackgroundStart: values.get("UI_PAGE_BACKGROUND_START") || "#0d0c0c",
            pageBackgroundEnd: values.get("UI_PAGE_BACKGROUND_END") || "#02070a",
            cardBackground: values.get("UI_CARD_BACKGROUND") || "#092224",
            surfaceColor: values.get("UI_SURFACE_COLOR") || "#092224",
            accentPrimary: values.get("UI_ACCENT_PRIMARY") || "#607d8b",
            accentSecondary: values.get("UI_ACCENT_SECONDARY") || "#00838f",
            accentTertiary: values.get("UI_ACCENT_TERTIARY") || "#31a7a0",
            accentFocus: values.get("UI_ACCENT_FOCUS") || "#31a7a0",
            textPrimary: values.get("UI_TEXT_PRIMARY") || "#f7f7fb",
            textMuted: values.get("UI_TEXT_MUTED") || "#a6a5b0",
            dropdownBackground: values.get("UI_DROPDOWN_BACKGROUND") || "#0d1117"
        }
    };
}

const MIME_TYPES = {
    ".html": "text/html; charset=utf-8",
    ".js": "application/javascript; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".svg": "image/svg+xml",
    ".ico": "image/x-icon",
    ".xml": "application/xml; charset=utf-8",
    ".wgt": "application/zip",
    ".vtt": "text/vtt; charset=utf-8",
    ".srt": "text/plain; charset=utf-8",
    ".m3u8": "application/vnd.apple.mpegurl",
    ".ts": "video/mp2t",
    ".mp4": "video/mp4"
};

function rewriteM3u8(playlistText, targetBaseUrl, reqHost, extraParams = "") {
    const proxyBase = `http://${reqHost}/api/proxy`;
    const lines = playlistText.split(/\r?\n/);
    const rewritten = [];

    for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed) {
            rewritten.push(line);
            continue;
        }

        if (trimmed.startsWith("#")) {
            // Rewrite URI="..." in tags like #EXT-X-KEY, #EXT-X-MEDIA, #EXT-X-MAP
            const replaced = line.replace(/URI="([^"]+)"/g, (match, rawUri) => {
                try {
                    const resolved = new URL(rawUri, targetBaseUrl).href;
                    return `URI="${proxyBase}?url=${encodeURIComponent(resolved)}${extraParams}"`;
                } catch {
                    return match;
                }
            });
            rewritten.push(replaced);
        } else {
            // Segment URL or nested rendition playlist URL
            try {
                const resolved = new URL(trimmed, targetBaseUrl).href;
                rewritten.push(`${proxyBase}?url=${encodeURIComponent(resolved)}${extraParams}`);
            } catch {
                rewritten.push(line);
            }
        }
    }
    return rewritten.join("\n");
}

async function handleProxy(req, res, parsedUrl) {
    const targetUrl = parsedUrl.searchParams.get("url");
    if (!targetUrl) {
        res.writeHead(400, { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" });
        res.end(JSON.stringify({ error: "Missing 'url' query parameter" }));
        return;
    }

    let parsedTarget;
    try {
        parsedTarget = new URL(targetUrl);
    } catch (err) {
        res.writeHead(400, { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" });
        res.end(JSON.stringify({ error: "Invalid target URL: " + err.message }));
        return;
    }

    const referer = parsedUrl.searchParams.get("referer") || "";
    const origin = parsedUrl.searchParams.get("origin") || "";
    const userAgent = parsedUrl.searchParams.get("user_agent") || "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    let extraHeaders = {};
    const headersParam = parsedUrl.searchParams.get("headers");
    if (headersParam) {
        try { extraHeaders = JSON.parse(headersParam); } catch (_) {}
    }

    const forwardHeaders = {
        "User-Agent": userAgent,
        "Accept": "*/*",
        ...extraHeaders
    };
    if (referer) forwardHeaders["Referer"] = referer;
    if (origin) forwardHeaders["Origin"] = origin;

    let body = undefined;
    if (req.method === "POST" || req.method === "PUT") {
        body = await new Promise((resolve) => {
            const chunks = [];
            req.on("data", (chunk) => chunks.push(chunk));
            req.on("end", () => resolve(Buffer.concat(chunks)));
        });
    }

    try {
        const upstream = await fetch(targetUrl, {
            method: req.method,
            headers: forwardHeaders,
            body: body && body.length > 0 ? body : undefined,
            redirect: "follow"
        });

        const contentType = upstream.headers.get("content-type") || "application/octet-stream";
        const isM3u8 = contentType.includes("mpegurl") ||
                       contentType.includes("application/x-mpegURL") ||
                       /\.m3u8($|\?)/i.test(targetUrl);

        const responseHeaders = {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Methods": "GET, POST, OPTIONS, HEAD",
            "Access-Control-Allow-Headers": "*",
            "Content-Type": contentType
        };

        if (isM3u8) {
            const text = await upstream.text();
            let extraParams = "";
            if (referer) extraParams += `&referer=${encodeURIComponent(referer)}`;
            if (origin) extraParams += `&origin=${encodeURIComponent(origin)}`;
            const rewritten = rewriteM3u8(text, targetUrl, req.headers.host || `${HOST}:${PORT}`, extraParams);
            const buffer = Buffer.from(rewritten, "utf8");
            responseHeaders["Content-Length"] = buffer.length;
            res.writeHead(upstream.status, responseHeaders);
            res.end(buffer);
        } else {
            const arrayBuf = await upstream.arrayBuffer();
            const buffer = Buffer.from(arrayBuf);
            responseHeaders["Content-Length"] = buffer.length;
            res.writeHead(upstream.status, responseHeaders);
            res.end(buffer);
        }
    } catch (err) {
        console.error("[DevProxy Error] Upstream request failed.");
        res.writeHead(502, {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*"
        });
        res.end(JSON.stringify({ error: "Proxy fetch failed: " + err.message }));
    }
}

const server = http.createServer(async (req, res) => {
    if (req.method === "OPTIONS") {
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, HEAD");
        res.setHeader("Access-Control-Allow-Headers", "*");
        res.writeHead(204);
        res.end();
        return;
    }

    const reqUrl = req.url || "/";
    const parsedUrl = new URL(reqUrl, `http://${req.headers.host || `${HOST}:${PORT}`}`);
    const pathname = parsedUrl.pathname;

    // API: Proxy
    if (pathname === "/api/proxy" || pathname === "/proxy") {
        await handleProxy(req, res, parsedUrl);
        return;
    }

    // API: Health / Ping
    if (pathname === "/api/ping" || pathname === "/ping") {
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ status: "ok", proxy: true, port: PORT }));
        return;
    }

    if (pathname === "/js/build-config.js") {
        if (req.method !== "GET" && req.method !== "HEAD") {
            res.writeHead(405, { "Allow": "GET, HEAD", "Content-Type": "text/plain; charset=utf-8" });
            res.end("Method Not Allowed");
            return;
        }
        const allowedHosts = new Set([`127.0.0.1:${PORT}`, `localhost:${PORT}`]);
        const origin = req.headers.origin;
        const fetchSite = req.headers["sec-fetch-site"];
        let originAllowed = true;
        if (origin) {
            try {
                const parsedOrigin = new URL(origin);
                originAllowed = parsedOrigin.protocol === "http:" && allowedHosts.has(parsedOrigin.host);
            } catch {
                originAllowed = false;
            }
        }
        if (!allowedHosts.has(req.headers.host) || !originAllowed || fetchSite && fetchSite !== "same-origin") {
            res.writeHead(403, { "Content-Type": "text/plain; charset=utf-8", "Cache-Control": "no-store" });
            res.end("Forbidden");
            return;
        }
        try {
            const config = JSON.stringify(readAppConfig()).replace(/</g, "\\u003c");
            const body = `"use strict";\nwindow.ECHO_BUILD_CONFIG = Object.freeze(${config});\n`;
            res.writeHead(200, {
                "Content-Type": "application/javascript; charset=utf-8",
                "Content-Length": Buffer.byteLength(body),
                "Cache-Control": "no-store",
                "X-Content-Type-Options": "nosniff",
                "Cross-Origin-Resource-Policy": "same-origin",
                "Vary": "Origin, Sec-Fetch-Site"
            });
            res.end(req.method === "HEAD" ? undefined : body);
        } catch (error) {
            console.error("[Tizen preview config]", error.message);
            res.writeHead(500, { "Content-Type": "text/plain; charset=utf-8", "Cache-Control": "no-store" });
            res.end("Tizen preview requires valid project-root config.cfg.");
        }
        return;
    }

    // Static files
    let relativePath = pathname;
    if (relativePath === "/" || relativePath === "") relativePath = "/index.html";

    const safePath = path.normalize(decodeURIComponent(relativePath)).replace(/^(\.\.[/\\])+/, "");
    const filePath = path.join(STATIC_ROOT, safePath);

    fs.stat(filePath, (err, stats) => {
        if (err || !stats.isFile()) {
            res.writeHead(404, { "Content-Type": "text/plain" });
            res.end("404 Not Found: " + pathname);
            return;
        }

        const ext = path.extname(filePath).toLowerCase();
        const contentType = MIME_TYPES[ext] || "application/octet-stream";
        res.writeHead(200, {
            "Content-Type": contentType,
            "Content-Length": stats.size,
            "Cache-Control": "no-cache"
        });
        fs.createReadStream(filePath).pipe(res);
    });
});

server.listen(PORT, HOST, () => {
    console.log(`[ECHO NOW Dev Server] Serving ${STATIC_ROOT} on http://${HOST}:${PORT}`);
    console.log(`[ECHO NOW Dev Server] CORS Proxy available at http://${HOST}:${PORT}/api/proxy?url=...`);
});
