"use strict";
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
function getSubtleCrypto() {
  if (typeof window !== "undefined" && window.crypto && window.crypto.subtle) return window.crypto.subtle;
  if (typeof globalThis !== "undefined" && globalThis.crypto && globalThis.crypto.subtle) return globalThis.crypto.subtle;
  return null;
}
const CryptoUtils = {
  base64Encode(data) {
    const bytes = typeof data === "string" ? new TextEncoder().encode(data) : data;
    let binary = "";
    const len = bytes.length;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
  },
  base64Decode(str) {
    let clean = (str || "").replace(/[\r\n\s]/g, "").trim();
    const mod = clean.length % 4;
    if (mod === 2) clean += "==";
    else if (mod === 3) clean += "=";
    const binary = atob(clean);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
  },
  base64UrlDecode(str) {
    const std = (str || "").replace(/-/g, "+").replace(/_/g, "/");
    return this.base64Decode(std);
  },
  base64UrlEncodeNoPadding(bytes) {
    return this.base64Encode(bytes).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  },
  hexToBytes(hex) {
    const clean = (hex || "").trim();
    const len = clean.length;
    const result = new Uint8Array(Math.floor(len / 2));
    for (let i = 0; i < len; i += 2) {
      result[i / 2] = parseInt(clean.substring(i, i + 2), 16);
    }
    return result;
  },
  bytesToHex(bytes) {
    return Array.from(bytes).map((b) => b.toString(16).padStart(2, "0")).join("");
  },
  md5(bytes) {
    if (typeof bytes === "string") bytes = new TextEncoder().encode(bytes);
    function safeAdd(x, y) {
      const lsw = (x & 65535) + (y & 65535);
      const msw = (x >> 16) + (y >> 16) + (lsw >> 16);
      return msw << 16 | lsw & 65535;
    }
    function bitRol(num, cnt) {
      return num << cnt | num >>> 32 - cnt;
    }
    function md5cmn(q, a2, b2, x, s, t) {
      return safeAdd(bitRol(safeAdd(safeAdd(a2, q), safeAdd(x, t)), s), b2);
    }
    function md5ff(a2, b2, c2, d2, x, s, t) {
      return md5cmn(b2 & c2 | ~b2 & d2, a2, b2, x, s, t);
    }
    function md5gg(a2, b2, c2, d2, x, s, t) {
      return md5cmn(b2 & d2 | c2 & ~d2, a2, b2, x, s, t);
    }
    function md5hh(a2, b2, c2, d2, x, s, t) {
      return md5cmn(b2 ^ c2 ^ d2, a2, b2, x, s, t);
    }
    function md5ii(a2, b2, c2, d2, x, s, t) {
      return md5cmn(c2 ^ (b2 | ~d2), a2, b2, x, s, t);
    }
    const n = bytes.length;
    const words = [];
    for (let i = 0; i < n; i++) words[i >> 2] |= (bytes[i] & 255) << i % 4 * 8;
    words[n >> 2] |= 128 << n % 4 * 8;
    words[(n + 8 >> 6 << 4) + 14] = n * 8;
    let a = 1732584193, b = -271733879, c = -1732584194, d = 271733878;
    for (let i = 0; i < words.length; i += 16) {
      const olda = a, oldb = b, oldc = c, oldd = d;
      a = md5ff(a, b, c, d, words[i] || 0, 7, -680876936);
      d = md5ff(d, a, b, c, words[i + 1] || 0, 12, -389564586);
      c = md5ff(c, d, a, b, words[i + 2] || 0, 17, 606105819);
      b = md5ff(b, c, d, a, words[i + 3] || 0, 22, -1044525330);
      a = md5ff(a, b, c, d, words[i + 4] || 0, 7, -176418897);
      d = md5ff(d, a, b, c, words[i + 5] || 0, 12, 1200080426);
      c = md5ff(c, d, a, b, words[i + 6] || 0, 17, -1473231341);
      b = md5ff(b, c, d, a, words[i + 7] || 0, 22, -45705983);
      a = md5ff(a, b, c, d, words[i + 8] || 0, 7, 1770035416);
      d = md5ff(d, a, b, c, words[i + 9] || 0, 12, -1958414417);
      c = md5ff(c, d, a, b, words[i + 10] || 0, 17, -42063);
      b = md5ff(b, c, d, a, words[i + 11] || 0, 22, -1990404162);
      a = md5ff(a, b, c, d, words[i + 12] || 0, 7, 1804603682);
      d = md5ff(d, a, b, c, words[i + 13] || 0, 12, -40341101);
      c = md5ff(c, d, a, b, words[i + 14] || 0, 17, -1502002290);
      b = md5ff(b, c, d, a, words[i + 15] || 0, 22, 1236535329);
      a = md5gg(a, b, c, d, words[i + 1] || 0, 5, -165796510);
      d = md5gg(d, a, b, c, words[i + 6] || 0, 9, -1069501632);
      c = md5gg(c, d, a, b, words[i + 11] || 0, 14, 643717713);
      b = md5gg(b, c, d, a, words[i] || 0, 20, -373897302);
      a = md5gg(a, b, c, d, words[i + 5] || 0, 5, -701558691);
      d = md5gg(d, a, b, c, words[i + 10] || 0, 9, 38016083);
      c = md5gg(c, d, a, b, words[i + 15] || 0, 14, -660478335);
      b = md5gg(b, c, d, a, words[i + 4] || 0, 20, -405537848);
      a = md5gg(a, b, c, d, words[i + 9] || 0, 5, 568446438);
      d = md5gg(d, a, b, c, words[i + 14] || 0, 9, -1019803690);
      c = md5gg(c, d, a, b, words[i + 3] || 0, 14, -187363961);
      b = md5gg(b, c, d, a, words[i + 8] || 0, 20, 1163531501);
      a = md5gg(a, b, c, d, words[i + 13] || 0, 5, -1444681467);
      d = md5gg(d, a, b, c, words[i + 2] || 0, 9, -51403784);
      c = md5gg(c, d, a, b, words[i + 7] || 0, 14, 1735328473);
      b = md5gg(b, c, d, a, words[i + 12] || 0, 20, -1926607734);
      a = md5hh(a, b, c, d, words[i + 5] || 0, 4, -378558);
      d = md5hh(d, a, b, c, words[i + 8] || 0, 11, -2022574463);
      c = md5hh(c, d, a, b, words[i + 11] || 0, 16, 1839030562);
      b = md5hh(b, c, d, a, words[i + 14] || 0, 23, -35309556);
      a = md5hh(a, b, c, d, words[i + 1] || 0, 4, -1530992060);
      d = md5hh(d, a, b, c, words[i + 4] || 0, 11, 1272893353);
      c = md5hh(c, d, a, b, words[i + 7] || 0, 16, -155497632);
      b = md5hh(b, c, d, a, words[i + 10] || 0, 23, -1094730640);
      a = md5hh(a, b, c, d, words[i + 13] || 0, 4, 681279174);
      d = md5hh(d, a, b, c, words[i] || 0, 11, -358537222);
      c = md5hh(c, d, a, b, words[i + 3] || 0, 16, -722521979);
      b = md5hh(b, c, d, a, words[i + 6] || 0, 23, 76029189);
      a = md5hh(a, b, c, d, words[i + 9] || 0, 4, -640364487);
      d = md5hh(d, a, b, c, words[i + 12] || 0, 11, -421815835);
      c = md5hh(c, d, a, b, words[i + 15] || 0, 16, 530742520);
      b = md5hh(b, c, d, a, words[i + 2] || 0, 23, -995338651);
      a = md5ii(a, b, c, d, words[i] || 0, 6, -198630844);
      d = md5ii(d, a, b, c, words[i + 7] || 0, 10, 1126891415);
      c = md5ii(c, d, a, b, words[i + 14] || 0, 15, -1416354905);
      b = md5ii(b, c, d, a, words[i + 5] || 0, 21, -57434055);
      a = md5ii(a, b, c, d, words[i + 12] || 0, 6, 1700485571);
      d = md5ii(d, a, b, c, words[i + 3] || 0, 10, -1894986606);
      c = md5ii(c, d, a, b, words[i + 10] || 0, 15, -1051523);
      b = md5ii(b, c, d, a, words[i + 1] || 0, 21, -2054922799);
      a = md5ii(a, b, c, d, words[i + 8] || 0, 6, 1873313359);
      d = md5ii(d, a, b, c, words[i + 15] || 0, 10, -30611744);
      c = md5ii(c, d, a, b, words[i + 6] || 0, 15, -1560198380);
      b = md5ii(b, c, d, a, words[i + 13] || 0, 21, 1309151649);
      a = md5ii(a, b, c, d, words[i + 4] || 0, 6, -145523070);
      d = md5ii(d, a, b, c, words[i + 11] || 0, 10, -1120210379);
      c = md5ii(c, d, a, b, words[i + 2] || 0, 15, 718787259);
      b = md5ii(b, c, d, a, words[i + 9] || 0, 21, -343485551);
      a = safeAdd(a, olda);
      b = safeAdd(b, oldb);
      c = safeAdd(c, oldc);
      d = safeAdd(d, oldd);
    }
    const res = new Uint8Array(16);
    const state = [a, b, c, d];
    for (let i = 0; i < 4; i++) {
      res[i * 4] = state[i] & 255;
      res[i * 4 + 1] = state[i] >> 8 & 255;
      res[i * 4 + 2] = state[i] >> 16 & 255;
      res[i * 4 + 3] = state[i] >> 24 & 255;
    }
    return res;
  },
  evpBytesToKey(password, salt, keyLen = 32, ivLen = 16) {
    if (typeof password === "string") password = new TextEncoder().encode(password);
    const keyIv = new Uint8Array(keyLen + ivLen);
    let lastDigest = new Uint8Array(0);
    let offset = 0;
    while (offset < keyIv.length) {
      const combined = new Uint8Array(lastDigest.length + password.length + salt.length);
      combined.set(lastDigest, 0);
      combined.set(password, lastDigest.length);
      combined.set(salt, lastDigest.length + password.length);
      lastDigest = this.md5(combined);
      const count = Math.min(lastDigest.length, keyIv.length - offset);
      keyIv.set(lastDigest.slice(0, count), offset);
      offset += count;
    }
    return {
      key: keyIv.slice(0, keyLen),
      iv: keyIv.slice(keyLen, keyLen + ivLen)
    };
  },
  async sha256(data) {
    const bytes = typeof data === "string" ? new TextEncoder().encode(data) : data;
    const subtle = getSubtleCrypto();
    if (!subtle) throw new Error("SubtleCrypto is not available in this environment");
    const hash = await subtle.digest("SHA-256", bytes);
    return new Uint8Array(hash);
  },
  async sha512(data) {
    const bytes = typeof data === "string" ? new TextEncoder().encode(data) : data;
    const subtle = getSubtleCrypto();
    if (!subtle) throw new Error("SubtleCrypto is not available in this environment");
    const hash = await subtle.digest("SHA-512", bytes);
    return new Uint8Array(hash);
  },
  async pbkdf2(password, salt, iterations, keyLengthBytes, algorithm) {
    const passBytes = typeof password === "string" ? new TextEncoder().encode(password) : password;
    const saltBytes = typeof salt === "string" ? new TextEncoder().encode(salt) : salt;
    const hashName = algorithm.toUpperCase().includes("512") ? "SHA-512" : "SHA-256";
    const subtle = getSubtleCrypto();
    if (!subtle) throw new Error("SubtleCrypto is not available in this environment");
    const keyMat = await subtle.importKey(
      "raw",
      passBytes,
      { name: "PBKDF2" },
      false,
      ["deriveBits"]
    );
    const derived = await subtle.deriveBits(
      { name: "PBKDF2", salt: saltBytes, iterations, hash: hashName },
      keyMat,
      keyLengthBytes * 8
    );
    return new Uint8Array(derived);
  },
  async aesDecryptCbc(ciphertext, key, iv) {
    const subtle = getSubtleCrypto();
    if (!subtle) throw new Error("SubtleCrypto is not available in this environment");
    const keyObj = await subtle.importKey(
      "raw",
      key,
      { name: "AES-CBC" },
      false,
      ["decrypt"]
    );
    const plain = await subtle.decrypt(
      { name: "AES-CBC", iv },
      keyObj,
      ciphertext
    );
    return new Uint8Array(plain);
  },
  async aesEncryptCbc(plaintext, key, iv) {
    const subtle = getSubtleCrypto();
    if (!subtle) throw new Error("SubtleCrypto is not available in this environment");
    const keyObj = await subtle.importKey(
      "raw",
      key,
      { name: "AES-CBC" },
      false,
      ["encrypt"]
    );
    const cipher = await subtle.encrypt(
      { name: "AES-CBC", iv },
      keyObj,
      plaintext
    );
    return new Uint8Array(cipher);
  },
  async aesDecryptGcm(ciphertextWithTag, key, iv) {
    const subtle = getSubtleCrypto();
    if (!subtle) throw new Error("SubtleCrypto is not available in this environment");
    const keyObj = await subtle.importKey(
      "raw",
      key,
      { name: "AES-GCM" },
      false,
      ["decrypt"]
    );
    const plain = await subtle.decrypt(
      { name: "AES-GCM", iv, tagLength: 128 },
      keyObj,
      ciphertextWithTag
    );
    return new Uint8Array(plain);
  }
};
const ProxyUnwrapper = {
  knownThirdPartyProxies: {
    "https://hls1.vid1.site": [/\/proxy\/(.+)$/],
    "https://madplay.site": [/\/api\/[^/]+\/proxy\?url=(.+)$/],
    "https://streams.smashystream.top": [/\/proxy\/m3u8\/(.+?)\/[^/]+$/],
    "*": [
      /^https:\/\/[^/]+\.workers\.dev\/((?:https?:\/\/|https?%3A%2F%2F).+)$/,
      /^https:\/\/[^/]+\.workers\.dev\/((?:https?:\/\/)?[^/]+\/file2\/.+)$/,
      /^https:\/\/.+?\.workers\.dev\/((?:https?:\/\/).+)$/,
      /\/proxy\/(.+)$/,
      /\/(?:m3u8|mp4)-proxy\?url=(.+?)(?:&|$)/,
      /\/api\/[^/]+\/proxy\?url=(.+)$/,
      /\/proxy\?.*url=([^&]+)/,
      /\/stream\/proxy\/(.+)$/,
      /^https:\/\/[^/]+\/((?:https?:\/\/)?[a-zA-Z0-9.-]+\/file2\/.+)$/,
      /^https:\/\/[^/]+\.workers\.dev\/(?:m3u8|mp4)-proxy\?url=(.+?)(?:&|$)/
    ]
  },
  unwrapUrl(url) {
    if (!url || typeof url !== "string") return url;
    let origin = "";
    try {
      const parsed = new URL(url);
      origin = "".concat(parsed.protocol, "//").concat(parsed.host);
    } catch (_) {
    }
    const patterns = [
      ...this.knownThirdPartyProxies[origin] || [],
      ...this.knownThirdPartyProxies["*"] || []
    ];
    for (const pattern of patterns) {
      const match = url.match(pattern);
      if (match && match[1]) {
        let decoded = match[1];
        let previous = "";
        let attempts = 0;
        while (decoded !== previous && attempts < 5) {
          previous = decoded;
          try {
            decoded = decodeURIComponent(decoded);
          } catch (_) {
            break;
          }
          attempts++;
        }
        return decoded;
      }
    }
    return url;
  }
};
function sanitizeHeaders(headers = {}) {
  const isBrowser = typeof window !== "undefined";
  const clean = {};
  for (const [key, value] of Object.entries(headers)) {
    if (value !== void 0 && value !== null) {
      if (isBrowser && /^(user-agent|referer|origin|cookie|cookie2|host|connection|keep-alive|content-length|accept-encoding|date|dnt|te|trailer|upgrade|via)$/i.test(key)) {
        continue;
      }
      clean[key] = String(value);
    }
  }
  return clean;
}
function xhrFetch(url, options = {}, timeoutMs = 6e3) {
  return new Promise((resolve, reject) => {
    try {
      const xhr = new XMLHttpRequest();
      const method = (options.method || "GET").toUpperCase();
      xhr.open(method, url, true);
      xhr.timeout = timeoutMs;
      const isBrowser = typeof window !== "undefined";
      if (options.headers) {
        for (const [k, v] of Object.entries(options.headers)) {
          if (isBrowser && /^(user-agent|referer|origin|cookie|cookie2|host|connection|content-length|accept-encoding|date|dnt|te|trailer|upgrade|via)$/i.test(k)) {
            continue;
          }
          try {
            xhr.setRequestHeader(k, v);
          } catch (_) {
          }
        }
      }
      xhr.onload = () => {
        const response = {
          ok: xhr.status >= 200 && xhr.status < 300,
          status: xhr.status,
          statusText: xhr.statusText,
          text: () => Promise.resolve(xhr.responseText || ""),
          json: () => {
            try {
              return Promise.resolve(JSON.parse(xhr.responseText || "null"));
            } catch (err) {
              return Promise.reject(err);
            }
          },
          headers: {
            get: (name) => xhr.getResponseHeader(name)
          }
        };
        resolve(response);
      };
      xhr.onerror = () => reject(new Error("XHR network error for ".concat(url)));
      xhr.ontimeout = () => reject(new Error("XHR timeout (".concat(timeoutMs, "ms) for ").concat(url)));
      xhr.onabort = () => reject(new Error("XHR aborted for ".concat(url)));
      xhr.send(options.body || null);
    } catch (err) {
      reject(err);
    }
  });
}
function getProxyUrl(targetUrl, headers = {}) {
  if (typeof window !== "undefined" && window.location && window.location.protocol.startsWith("http")) {
    const origin = window.location.origin;
    let proxyUrl = "".concat(origin, "/api/proxy?url=").concat(encodeURIComponent(targetUrl));
    const ref = headers["Referer"] || headers["referer"];
    if (ref) proxyUrl += "&referer=".concat(encodeURIComponent(ref));
    const orig = headers["Origin"] || headers["origin"];
    if (orig) proxyUrl += "&origin=".concat(encodeURIComponent(orig));
    return proxyUrl;
  }
  return targetUrl;
}
async function fetchWithTimeout(url, options = {}, timeoutMs = 6e3) {
  const isBrowser = typeof window !== "undefined";
  const controller = typeof AbortController !== "undefined" ? new AbortController() : null;
  const timeoutId = controller ? setTimeout(() => controller.abort(), timeoutMs) : null;
  try {
    const defaultHeaders = isBrowser ? { "Accept-Language": "en-US,en;q=0.9" } : {
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
      "Accept-Language": "en-US,en;q=0.9"
    };
    const fetchOptions = __spreadProps(__spreadValues({}, options), {
      headers: sanitizeHeaders(__spreadValues(__spreadValues({}, defaultHeaders), options.headers || {}))
    });
    if (controller) fetchOptions.signal = controller.signal;
    try {
      const response = await fetch(url, fetchOptions);
      return response;
    } catch (fetchErr) {
      if (typeof XMLHttpRequest !== "undefined") {
        try {
          const xhrRes = await xhrFetch(url, options, timeoutMs);
          return xhrRes;
        } catch (_) {
        }
      }
      if (isBrowser && window.location && window.location.protocol.startsWith("http") && !url.includes("/api/proxy")) {
        try {
          const proxiedUrl = getProxyUrl(url, options.headers || {});
          const proxiedOptions = __spreadProps(__spreadValues({}, options), {
            headers: sanitizeHeaders(options.headers || {})
          });
          if (controller) proxiedOptions.signal = controller.signal;
          const proxiedResponse = await fetch(proxiedUrl, proxiedOptions);
          return proxiedResponse;
        } catch (_) {
        }
      }
      throw fetchErr;
    }
  } finally {
    if (timeoutId) clearTimeout(timeoutId);
  }
}
async function fetchText(url, options = {}, timeoutMs = 6e3) {
  try {
    const res = await fetchWithTimeout(url, options, timeoutMs);
    if (!res.ok) return null;
    return await res.text();
  } catch (_) {
    return null;
  }
}
async function fetchJson(url, options = {}, timeoutMs = 6e3) {
  try {
    const res = await fetchWithTimeout(url, options, timeoutMs);
    if (!res.ok) return null;
    return await res.json();
  } catch (_) {
    return null;
  }
}
function timeoutPromise(ms, promise) {
  let timer;
  return Promise.race([
    promise.then(
      (val) => {
        clearTimeout(timer);
        return val;
      },
      (err) => {
        clearTimeout(timer);
        throw err;
      }
    ),
    new Promise((_, reject) => {
      timer = setTimeout(() => reject(new Error("Timeout")), ms);
    })
  ]);
}
class VidSrcProvider {
  constructor() {
    this.name = "VidSrc";
    this.baseUrl = "https://vsembed.ru";
  }
  async extractStreams(request) {
    var _a;
    try {
      const pageUrl = request.type === "movie" ? "".concat(this.baseUrl, "/embed/movie?tmdb=").concat(request.tmdbId) : "".concat(this.baseUrl, "/embed/tv?tmdb=").concat(request.tmdbId, "&season=").concat(request.season || 1, "&episode=").concat(request.episode || 1);
      const html = await fetchText(pageUrl, { headers: { Referer: this.baseUrl } });
      if (!html) return null;
      const iframeMatch = html.match(/<iframe[^>]*\s+src=["']([^"']+)["'][^>]*>/i);
      const secondSrc = iframeMatch == null ? void 0 : iframeMatch[1];
      if (!secondSrc) return null;
      const secondUrl = secondSrc.startsWith("//") ? "https:".concat(secondSrc) : secondSrc;
      const secondHtml = await fetchText(secondUrl, { headers: { Referer: pageUrl } });
      if (!secondHtml) return null;
      const thirdMatch = secondHtml.match(/src:\s*['"]([^'"]+)['"]/i);
      const relSrc = thirdMatch == null ? void 0 : thirdMatch[1];
      if (!relSrc) return null;
      let thirdUrl;
      if (relSrc.startsWith("http")) {
        thirdUrl = relSrc;
      } else {
        const baseParsed = new URL(secondUrl);
        thirdUrl = "".concat(baseParsed.protocol, "//").concat(baseParsed.host).concat(relSrc.startsWith("/") ? "" : "/").concat(relSrc);
      }
      const thirdHtml = await fetchText(thirdUrl, { headers: { Referer: secondUrl } });
      if (!thirdHtml) return null;
      const fileField = (_a = thirdHtml.match(/file\s*:\s*["']([^"']+)["']/i)) == null ? void 0 : _a[1];
      if (!fileField) return null;
      const replaced = fileField.replace(/{v1}/g, "neonhorizonworkshops.com").replace(/{v2}/g, "wanderlynest.com").replace(/{v3}/g, "orchidpixelgardens.com").replace(/{v4}/g, "cloudnestra.com");
      const urls = replaced.split(/\s+or\s+/i).map((u) => u.trim()).filter((u) => u.length > 0 && !u.includes("{") && !u.includes("}"));
      if (!urls.length) return null;
      return {
        providerName: this.name,
        sources: urls.map((u) => ({
          url: u,
          quality: "Auto",
          format: "hls",
          isM3u8: true
        })),
        headers: {
          Referer: "https://cloudnestra.com/",
          Origin: "https://cloudnestra.com"
        }
      };
    } catch (_) {
      return null;
    }
  }
}
class VixSrcProvider {
  constructor() {
    this.name = "VixSrc";
    this.baseUrl = "https://vixsrc.to";
  }
  async extractStreams(request) {
    var _a, _b, _c;
    try {
      const pageUrl = request.type === "movie" ? "".concat(this.baseUrl, "/api/movie/").concat(request.tmdbId) : "".concat(this.baseUrl, "/api/tv/").concat(request.tmdbId, "/").concat(request.season || 1, "/").concat(request.episode || 1);
      const sublink = await fetchJson(pageUrl);
      const suburl = sublink == null ? void 0 : sublink.src;
      if (!suburl) return null;
      const fullSubUrl = suburl.startsWith("http") ? suburl : "".concat(this.baseUrl).concat(suburl);
      const html = await fetchText(fullSubUrl);
      if (!html) return null;
      const token = (_a = html.match(/token["']\s*:\s*["']([^"']+)/)) == null ? void 0 : _a[1];
      const expires = (_b = html.match(/expires["']\s*:\s*["']([^"']+)/)) == null ? void 0 : _b[1];
      const playlist = (_c = html.match(/url\s*:\s*["']([^"']+)/)) == null ? void 0 : _c[1];
      if (!token || !expires || !playlist) return null;
      const sep = playlist.includes("?") ? "&" : "?";
      const masterUrl = "".concat(playlist).concat(sep, "token=").concat(token, "&expires=").concat(expires, "&h=1");
      return {
        providerName: this.name,
        sources: [{
          url: masterUrl,
          quality: "1080p",
          label: "1080p",
          format: "hls",
          isM3u8: true
        }],
        headers: {}
      };
    } catch (_) {
      return null;
    }
  }
}
class CineSuProvider {
  constructor() {
    this.name = "CineSu";
    this.baseUrl = "https://cine.su";
  }
  async extractStreams(request) {
    try {
      const streamUrl = request.type === "movie" ? "".concat(this.baseUrl, "/v1/stream/master/movie/").concat(request.tmdbId, ".m3u8") : "".concat(this.baseUrl, "/v1/stream/master/tv/").concat(request.tmdbId, "/").concat(request.season || 1, "/").concat(request.episode || 1, ".m3u8");
      const res = await fetchWithTimeout(streamUrl, {
        headers: { Referer: "".concat(this.baseUrl, "/en/watch"), Origin: this.baseUrl }
      }, 5e3);
      if (res && res.status === 200) {
        return {
          providerName: this.name,
          sources: [{
            url: streamUrl,
            quality: "1080",
            format: "hls",
            isM3u8: true
          }],
          headers: { Referer: "".concat(this.baseUrl, "/en/watch"), Origin: this.baseUrl }
        };
      }
      return null;
    } catch (_) {
      return null;
    }
  }
}
class IcefyProvider {
  constructor() {
    this.name = "Icefy";
    this.baseUrl = "https://streams.icefy.top";
  }
  async extractStreams(request) {
    try {
      const url = request.type === "movie" ? "".concat(this.baseUrl, "/movie/").concat(request.tmdbId) : "".concat(this.baseUrl, "/tv/").concat(request.tmdbId, "/").concat(request.season || 1, "/").concat(request.episode || 1);
      const resp = await fetchJson(url, {
        headers: { Referer: this.baseUrl, Origin: this.baseUrl }
      });
      const streamUrl = resp == null ? void 0 : resp.stream;
      if (!streamUrl) return null;
      return {
        providerName: this.name,
        sources: [{
          url: streamUrl,
          quality: "1080",
          format: "hls",
          isM3u8: true
        }],
        headers: { Referer: this.baseUrl, Origin: this.baseUrl }
      };
    } catch (_) {
      return null;
    }
  }
}
class PoprProvider {
  constructor() {
    this.name = "Popr";
    this.baseUrl = "https://popr.ink";
    this.servers = ["default", "catflix", "hexa", "Gama", "Liligoon", "Sigma", "Prime", "Alfa", "Lamda", "ynx_vidsrc"];
  }
  async extractStreams(request) {
    var _a, _b, _c;
    for (const server of this.servers) {
      try {
        const url = request.type === "movie" ? "".concat(this.baseUrl, "/api/vidnest?id=").concat(request.tmdbId, "&type=movie").concat(server !== "default" ? "&server=".concat(server) : "") : "".concat(this.baseUrl, "/api/vidnest?id=").concat(request.tmdbId, "&type=tv&server=").concat(server, "&season=").concat(request.season || 1, "&episode=").concat(request.episode || 1);
        const resp = await fetchJson(url, { headers: { Referer: "".concat(this.baseUrl, "/") } });
        const firstRes = (_a = resp == null ? void 0 : resp.results) == null ? void 0 : _a[0];
        if (!firstRes) continue;
        const stream = (_c = (_b = firstRes.streams) == null ? void 0 : _b[0]) == null ? void 0 : _c.url;
        if (!stream) continue;
        const subs = (firstRes.subtitles || []).filter((s) => s && s.url).map((sub) => ({
          url: sub.url,
          language: sub.lang || "eng",
          label: sub.lang
        }));
        return {
          providerName: this.name,
          sources: [{
            url: stream,
            quality: "Auto",
            format: stream.includes(".m3u8") ? "hls" : "mp4",
            isM3u8: stream.includes(".m3u8")
          }],
          subtitles: subs,
          headers: { Referer: "".concat(this.baseUrl, "/") }
        };
      } catch (_) {
        continue;
      }
    }
    return null;
  }
}
class VidApiProvider {
  constructor() {
    this.name = "VidApi";
    this.apiUrl = "https://streamdata.vaplayer.ru/api.php";
  }
  async extractStreams(request) {
    var _a;
    try {
      const typeStr = request.type === "movie" ? "movie" : "tv";
      let url = "".concat(this.apiUrl, "?tmdb=").concat(encodeURIComponent(request.tmdbId), "&type=").concat(typeStr);
      if (request.type !== "movie") {
        url += "&season=".concat(encodeURIComponent(request.season || 1), "&episode=").concat(encodeURIComponent(request.episode || 1));
      }
      const response = await fetchJson(url);
      if ((response == null ? void 0 : response.status_code) !== "200" || !Array.isArray((_a = response.data) == null ? void 0 : _a.stream_urls) || !response.data.stream_urls.length) {
        return null;
      }
      const qualities = ["1080p", "720p", "Auto"];
      const sources = response.data.stream_urls.map((u, idx) => {
        const isMp4 = u.includes(".mp4") || u.includes(".mkv");
        const quality = qualities[idx] || "Auto";
        return {
          url: u,
          quality,
          label: quality,
          format: isMp4 ? "mp4" : "hls",
          isM3u8: !isMp4
        };
      });
      const subs = (response.default_subs || []).map((s) => ({
        url: s.url,
        language: s.lang,
        label: s.lang
      }));
      return {
        providerName: this.name,
        sources,
        subtitles: subs,
        headers: {}
      };
    } catch (_) {
      return null;
    }
  }
}
class FShareProvider {
  constructor() {
    this.name = "FShare";
    this.baseUrl = "https://fsharetv.cc";
    this.trailer = "Png81APqcxU";
  }
  async extractStreams(request) {
    var _a, _b, _c, _d, _e;
    try {
      if (request.type !== "movie") return null;
      const imdbId = request.imdbId;
      if (!imdbId) return null;
      const moviePage = await fetchText("".concat(this.baseUrl, "/movie/").concat(imdbId), {
        headers: { Referer: this.baseUrl }
      });
      if (!moviePage) return null;
      const watchPath = (_a = moviePage.match(/href="(\/w\/[^"]+)"/)) == null ? void 0 : _a[1];
      if (!watchPath) return null;
      const watchPage = await fetchText("".concat(this.baseUrl).concat(watchPath), {
        headers: { Referer: this.baseUrl }
      });
      if (!watchPage) return null;
      const patterns = [
        /Movie\.setSource\(["']([^"']+)["']/,
        /setSource\(["']([^"']+)["']/,
        /"source_id"\s*:\s*["']([^"']+)["']/,
        /source_id\s*=\s*["']([^"']+)["']/,
        /"file_id"\s*:\s*["']([^"']+)["']/
      ];
      let sourceId = null;
      for (const pat of patterns) {
        const match = watchPage.match(pat);
        if (match == null ? void 0 : match[1]) {
          sourceId = match[1];
          break;
        }
      }
      if (!sourceId) return null;
      const apiResp = await fetchJson("".concat(this.baseUrl, "/api/file/").concat(sourceId, "/source?trailer=").concat(this.trailer, "&type=watch"), {
        headers: {
          Referer: "".concat(this.baseUrl, "/"),
          "X-Requested-With": "XMLHttpRequest"
        }
      });
      const items = [
        ...((_c = (_b = apiResp == null ? void 0 : apiResp.data) == null ? void 0 : _b.file) == null ? void 0 : _c.sources) || [],
        ...((_e = (_d = apiResp == null ? void 0 : apiResp.data) == null ? void 0 : _d.file) == null ? void 0 : _e.backups) || []
      ];
      const sources = items.filter((s) => s && s.src).map((s) => {
        const fullUrl = s.src.startsWith("http") ? s.src : "".concat(this.baseUrl).concat(s.src);
        return {
          url: fullUrl,
          quality: s.label || "Auto",
          format: "mp4",
          isM3u8: false
        };
      });
      if (!sources.length) return null;
      return {
        providerName: this.name,
        sources,
        headers: { Referer: this.baseUrl }
      };
    } catch (_) {
      return null;
    }
  }
}
class VidNestProvider {
  constructor() {
    this.name = "VidNest";
    this.apiBaseUrl = "https://new.vidnest.fun";
    this.baseUrl = "https://vidnest.fun";
    this.servers = [
      ["moviebox", ""],
      ["allmovies", ""],
      ["catflix", ""],
      ["purstream", ""],
      ["hollymoviehd", ""],
      ["lamda", ""],
      ["flixhq", ""],
      ["vidlink", ""],
      ["onehd", "?server=upcloud"],
      ["klikxxi", ""]
    ];
    this.alphabet = "RB0fpH8ZEyVLkv7c2i6MAJ5u3IKFDxlS1NTsnGaqmXYdUrtzjwObCgQP94hoeW+/=";
    this.reverseMap = new Int32Array(256).fill(64);
    for (let i = 0; i < this.alphabet.length; i++) {
      this.reverseMap[this.alphabet.charCodeAt(i)] = i;
    }
  }
  decodeCustomBase64(input) {
    let padded = input.trim();
    const mod = padded.length % 4;
    if (mod !== 0) padded += "=".repeat(4 - mod);
    const bytes = [];
    let i = 0;
    while (i < padded.length) {
      const c0 = this.reverseMap[padded.charCodeAt(i)];
      const c1 = this.reverseMap[padded.charCodeAt(i + 1)];
      const c2 = padded[i + 2] === "=" ? 64 : this.reverseMap[padded.charCodeAt(i + 2)];
      const c3 = padded[i + 3] === "=" ? 64 : this.reverseMap[padded.charCodeAt(i + 3)];
      bytes.push(c0 << 2 | c1 >> 4);
      if (c2 !== 64) bytes.push((c1 & 15) << 4 | c2 >> 2);
      if (c3 !== 64) bytes.push((c2 & 3) << 6 | c3);
      i += 4;
    }
    return new TextDecoder().decode(new Uint8Array(bytes));
  }
  async extractStreams(request) {
    var _a, _b;
    for (const [server, query] of this.servers) {
      try {
        const url = request.type === "movie" ? "".concat(this.apiBaseUrl, "/").concat(server, "/movie/").concat(request.tmdbId).concat(query) : "".concat(this.apiBaseUrl, "/").concat(server, "/tv/").concat(request.tmdbId, "/").concat(request.season || 1, "/").concat(request.episode || 1).concat(query);
        const encResp = await fetchJson(url, {
          headers: { Referer: "".concat(this.baseUrl, "/"), Origin: this.baseUrl }
        });
        if (!(encResp == null ? void 0 : encResp.data)) continue;
        const decodedJson = this.decodeCustomBase64(encResp.data);
        const jsonTree = JSON.parse(decodedJson);
        const foundUrls = [];
        if (typeof jsonTree.url === "string" && jsonTree.url.startsWith("http")) {
          foundUrls.push(jsonTree.url);
        }
        const vidlinkUrl = (_b = (_a = jsonTree.data) == null ? void 0 : _a.stream) == null ? void 0 : _b.playlist;
        if (typeof vidlinkUrl === "string" && vidlinkUrl.startsWith("http")) {
          foundUrls.push(vidlinkUrl);
        }
        const sourcesArray = Array.isArray(jsonTree.sources) ? jsonTree.sources : Array.isArray(jsonTree.streams) ? jsonTree.streams : Array.isArray(jsonTree.url) ? jsonTree.url : null;
        if (sourcesArray) {
          for (const elem of sourcesArray) {
            const link = (elem == null ? void 0 : elem.url) || (elem == null ? void 0 : elem.file) || (elem == null ? void 0 : elem.link);
            if (typeof link === "string" && link.startsWith("http")) {
              foundUrls.push(link);
            }
          }
        }
        if (foundUrls.length) {
          return {
            providerName: this.name,
            sources: foundUrls.map((u) => ({
              url: u,
              quality: "Auto",
              format: u.includes(".m3u8") ? "hls" : "mp4",
              isM3u8: u.includes(".m3u8")
            })),
            headers: { Referer: "".concat(this.baseUrl, "/") }
          };
        }
      } catch (_) {
        continue;
      }
    }
    return null;
  }
}
class AnyEmbedProvider {
  constructor() {
    this.name = "AnyEmbed";
    this.baseUrl = "https://api.anyembed.xyz";
    this.frontendUrl = "https://anyembed.xyz";
  }
  async extractStreams(request) {
    try {
      const sessionResp = await fetchJson("".concat(this.baseUrl, "/api/v1/session"), {
        headers: { Referer: this.frontendUrl, Origin: this.frontendUrl }
      });
      const token = sessionResp == null ? void 0 : sessionResp.token;
      if (!token) return null;
      const response = await fetchJson("".concat(this.baseUrl, "/api/v1/stream/").concat(request.tmdbId), {
        headers: {
          Referer: this.frontendUrl,
          Origin: this.frontendUrl,
          "x-session-token": token
        }
      });
      const streams = ((response == null ? void 0 : response.sources) || []).flatMap((s) => s.streams || []);
      if (!streams.length) return null;
      return {
        providerName: this.name,
        sources: streams.map((s) => ({
          url: s.url,
          quality: s.quality || "Auto",
          format: s.url.includes(".m3u8") ? "hls" : "mp4",
          isM3u8: s.url.includes(".m3u8")
        }))
      };
    } catch (_) {
      return null;
    }
  }
}
class VidEasyProvider {
  constructor() {
    this.name = "VidEasy";
    this.endpoints = [
      "https://api.videasy.net/mb-flix/sources-with-title",
      "https://api2.videasy.net/cuevana/sources-with-title",
      "https://api.videasy.net/1movies/sources-with-title",
      "https://api.videasy.net/superflix/sources-with-title",
      "https://api.videasy.net/lamovie/sources-with-title",
      "https://api.videasy.net/cdn/sources-with-title"
    ];
  }
  async extractStreams(request) {
    var _a;
    const params = new URLSearchParams({
      title: request.title || "",
      mediaType: request.type === "movie" ? "movie" : "tv",
      tmdbId: String(request.tmdbId),
      imdbId: request.imdbId || "",
      episodeId: String(request.episode || 1),
      seasonId: String(request.season || 1)
    });
    if (request.type === "movie" && request.releaseYear && request.releaseYear > 0) {
      params.append("year", String(request.releaseYear));
    }
    const queryStr = params.toString();
    const promises = this.endpoints.map(async (endpoint) => {
      var _a2, _b;
      try {
        const blob = await fetchText("".concat(endpoint, "?").concat(queryStr), {
          headers: { Referer: "https://player.videasy.net/", Origin: "https://player.videasy.net" }
        }, 2500);
        if (!blob || blob.length < 10) return null;
        const dec = await fetchJson("https://enc-dec.app/api/dec-videasy", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ text: blob, id: String(request.tmdbId) })
        }, 3e3);
        const rawSources = ((_a2 = dec == null ? void 0 : dec.result) == null ? void 0 : _a2.sources) || [];
        const rawSubs = ((_b = dec == null ? void 0 : dec.result) == null ? void 0 : _b.subtitles) || [];
        const sources = rawSources.map((s) => ({
          url: s.url,
          quality: s.quality || "Auto",
          format: s.url.includes(".m3u8") ? "hls" : "mp4",
          isM3u8: s.url.includes(".m3u8")
        }));
        const subs = rawSubs.map((s) => ({
          url: s.url,
          language: s.lang || "eng",
          label: s.lang
        }));
        if (sources.length) {
          return {
            providerName: this.name,
            sources,
            subtitles: subs
          };
        }
      } catch (_) {
      }
      return null;
    });
    const results = await Promise.allSettled(promises);
    for (const res of results) {
      if (res.status === "fulfilled" && res.value && ((_a = res.value.sources) == null ? void 0 : _a.length)) {
        return res.value;
      }
    }
    return null;
  }
}
class FMovies4UProvider {
  constructor() {
    this.name = "FMovies4U";
    this.baseUrl = "https://fmovies4u.com";
    this.key = new TextEncoder().encode("Fm0v1es4U_2026_SecureKey_DoNotShare_v2_Protected");
  }
  async extractStreams(request) {
    try {
      const path = request.type === "movie" ? "/api/movie/".concat(request.tmdbId) : "/api/tv/".concat(request.tmdbId, "/").concat(request.season || 1, "/").concat(request.episode || 1);
      const referer = request.type === "movie" ? "".concat(this.baseUrl, "/embed/movie/").concat(request.tmdbId, "?autoPlay=false") : "".concat(this.baseUrl, "/embed/tv/").concat(request.tmdbId, "/").concat(request.season || 1, "/").concat(request.episode || 1, "?autoPlay=false");
      const encryptedText = await fetchText("".concat(this.baseUrl).concat(path), {
        headers: { Referer: referer }
      });
      if (!encryptedText) return null;
      const rawBytes = CryptoUtils.base64Decode(encryptedText);
      let decryptedJson = "";
      if (rawBytes.length > 16 && new TextDecoder().decode(rawBytes.slice(0, 8)) === "Salted__") {
        const salt = rawBytes.slice(8, 16);
        const cipher = rawBytes.slice(16);
        const { key, iv } = CryptoUtils.evpBytesToKey(this.key, salt, 32, 16);
        const plain = await CryptoUtils.aesDecryptCbc(cipher, key, iv);
        decryptedJson = new TextDecoder().decode(plain);
      } else {
        const paddedKey = new Uint8Array(32);
        paddedKey.set(this.key.slice(0, 32));
        const plain = await CryptoUtils.aesDecryptCbc(rawBytes, paddedKey, new Uint8Array(16));
        decryptedJson = new TextDecoder().decode(plain);
      }
      const parsed = JSON.parse(decryptedJson);
      const sources = (parsed.sources || []).flatMap((g) => (g.url || []).map((u) => ({
        url: u.link,
        quality: u.quality || "Auto",
        format: u.link.includes(".m3u8") ? "hls" : "mp4",
        isM3u8: u.link.includes(".m3u8")
      })));
      if (!sources.length) return null;
      return {
        providerName: this.name,
        sources
      };
    } catch (_) {
      return null;
    }
  }
}
class VidRockProvider {
  constructor() {
    this.name = "VidRock";
    this.baseUrl = "https://vidrock.net/";
    this.subBaseUrl = "https://sub.vdrk.site";
    this.passphrase = "x7k9mPqT2rWvY8zA5bC3nF6hJ2lK4mN9";
  }
  async extractStreams(request) {
    try {
      const itemId = request.type === "movie" ? String(request.tmdbId) : "".concat(request.tmdbId, "_").concat(request.season || 1, "_").concat(request.episode || 1);
      const keyBytes = new TextEncoder().encode(this.passphrase);
      const ivBytes = new TextEncoder().encode(this.passphrase.substring(0, 16));
      const encrypted = await CryptoUtils.aesEncryptCbc(new TextEncoder().encode(itemId), keyBytes, ivBytes);
      const urlSafeEncrypted = CryptoUtils.base64UrlEncodeNoPadding(encrypted);
      const typeStr = request.type === "movie" ? "movie" : "tv";
      const streamsJson = await fetchText("".concat(this.baseUrl, "api/").concat(typeStr, "/").concat(urlSafeEncrypted), {
        headers: { Referer: this.baseUrl, Origin: this.baseUrl }
      });
      if (!streamsJson) return null;
      const jsonTree = JSON.parse(streamsJson);
      const sources = [];
      for (const item of Object.values(jsonTree)) {
        if (!item || !item.url) continue;
        const streamUrl = item.url;
        const audioLanguage = item.language || null;
        if (streamUrl.includes("hls2.vdrk.site")) {
          try {
            const secondJson = await fetchText(streamUrl, {
              headers: { Referer: this.baseUrl }
            });
            const cdnArray = JSON.parse(secondJson);
            for (const cdnElem of cdnArray) {
              let rawUrl = cdnElem == null ? void 0 : cdnElem.url;
              if (!rawUrl) continue;
              if (rawUrl.startsWith("https://proxy.vidrock.store/")) {
                rawUrl = decodeURIComponent(rawUrl.replace("https://proxy.vidrock.store/", "").replace(/^\//, ""));
              }
              const res = cdnElem.resolution || "1080";
              const isMp4 = rawUrl.includes(".mp4");
              sources.push({
                url: rawUrl,
                quality: "".concat(res, "p"),
                format: isMp4 ? "mp4" : "hls",
                isM3u8: !isMp4,
                audioLanguage: cdnElem.language || audioLanguage
              });
            }
          } catch (_) {
          }
        } else {
          sources.push({
            url: streamUrl,
            quality: "1080",
            format: streamUrl.includes(".mp4") ? "mp4" : "hls",
            isM3u8: !streamUrl.includes(".mp4"),
            audioLanguage
          });
        }
      }
      let subs = [];
      try {
        const subUrl = request.type === "movie" ? "".concat(this.subBaseUrl, "/v2/movie/").concat(request.tmdbId) : "".concat(this.subBaseUrl, "/v2/tv/").concat(request.tmdbId, "/").concat(request.season || 1, "/").concat(request.episode || 1);
        const subList = await fetchJson(subUrl, { headers: { Referer: this.baseUrl } });
        if (Array.isArray(subList)) {
          subs = subList.map((it) => ({
            url: it.file,
            language: it.label,
            label: it.label
          }));
        }
      } catch (_) {
      }
      if (!sources.length) return null;
      return {
        providerName: this.name,
        sources,
        subtitles: subs
      };
    } catch (_) {
      return null;
    }
  }
}
class StreamMafiaProvider {
  constructor() {
    this.name = "StreamMafia";
    this.baseUrl = "https://player.nhdapi.com";
    this.secret = "Z9#rL!v2K*5qP&7mXw";
  }
  async extractStreams(request) {
    var _a, _b, _c;
    try {
      const sessRes = await fetchWithTimeout("".concat(this.baseUrl, "/api/session"), {
        method: "POST",
        headers: { Referer: "".concat(this.baseUrl, "/"), Origin: this.baseUrl }
      }, 4e3);
      const setCookie = ((_a = sessRes == null ? void 0 : sessRes.headers) == null ? void 0 : _a.get("set-cookie")) || "";
      const cookie = setCookie ? setCookie.split(";")[0] : "vid_session=" + CryptoUtils.base64Encode(JSON.stringify({ id: String(request.tmdbId), iat: Math.floor(Date.now() / 1e3) }));
      const tokenHeaders = { Referer: "".concat(this.baseUrl, "/"), Origin: this.baseUrl };
      if (cookie) tokenHeaders["Cookie"] = cookie;
      const tokenResp = await fetchJson("".concat(this.baseUrl, "/api/token"), { headers: tokenHeaders });
      const token = (tokenResp == null ? void 0 : tokenResp.token) || "";
      const pageUrl = request.type === "movie" ? "".concat(this.baseUrl, "/api/movie/?id=").concat(request.tmdbId) : "".concat(this.baseUrl, "/api/?tv=").concat(request.tmdbId, "&season=").concat(request.season || 1, "&episode=").concat(request.episode || 1);
      const encHeaders = {
        Referer: "".concat(this.baseUrl, "/"),
        Origin: this.baseUrl,
        "x-api-token": token,
        "x-content-id": String(request.tmdbId)
      };
      if (cookie) encHeaders["Cookie"] = cookie;
      const encPayload = await fetchJson(pageUrl, { headers: encHeaders });
      if (!(encPayload == null ? void 0 : encPayload.iv) || !(encPayload == null ? void 0 : encPayload.tag) || !(encPayload == null ? void 0 : encPayload.data)) return null;
      const iv = CryptoUtils.base64Decode(encPayload.iv);
      const tag = CryptoUtils.base64Decode(encPayload.tag);
      const ciphertext = CryptoUtils.base64Decode(encPayload.data);
      const cipherWithTag = new Uint8Array(ciphertext.length + tag.length);
      cipherWithTag.set(ciphertext, 0);
      cipherWithTag.set(tag, ciphertext.length);
      const key = await CryptoUtils.sha256(new TextEncoder().encode(this.secret));
      const plainBytes = await CryptoUtils.aesDecryptGcm(cipherWithTag, key, iv);
      const api = JSON.parse(new TextDecoder().decode(plainBytes));
      const sources = [];
      if ((_b = api == null ? void 0 : api.stream) == null ? void 0 : _b.hls_streaming) {
        sources.push({
          url: api.stream.hls_streaming,
          quality: "Auto",
          format: "hls",
          isM3u8: true
        });
      }
      if (Array.isArray((_c = api == null ? void 0 : api.stream) == null ? void 0 : _c.download)) {
        for (const dl of api.stream.download) {
          if (dl == null ? void 0 : dl.url) {
            sources.push({
              url: dl.url,
              quality: dl.quality || "Auto",
              format: "mp4",
              isM3u8: dl.url.includes(".m3u8")
            });
          }
        }
      }
      if (!sources.length) return null;
      return {
        providerName: this.name,
        sources
      };
    } catch (_) {
      return null;
    }
  }
}
class PeachifyProvider {
  constructor() {
    this.name = "Peachify";
    this.keyB64 = "YThmMmExYjVlOWM0NzA4MTRmNmIyYzNhNWQ4ZTdmOWMxYTJiM2M0ZDVlM2Y3YThiOGNhZDFlMmQwYTRkNWM1Yg==";
    this.servers = [
      "https://uwu.eat-peach.sbs/moviebox",
      "https://usa.eat-peach.sbs/holly",
      "https://usa.eat-peach.sbs/air",
      "https://usa.eat-peach.sbs/multi",
      "https://uwu.eat-peach.sbs/net",
      "https://uwu.eat-peach.sbs/bmb"
    ];
  }
  async extractStreams(request) {
    var _a;
    try {
      const keyHex = new TextDecoder().decode(CryptoUtils.base64Decode(this.keyB64));
      const keyBytes = CryptoUtils.hexToBytes(keyHex);
      const promises = this.servers.map(async (server) => {
        try {
          const url = request.type === "movie" ? "".concat(server, "/movie/").concat(request.tmdbId) : "".concat(server, "/tv/").concat(request.tmdbId, "/").concat(request.season || 1, "/").concat(request.episode || 1);
          const resp = await fetchJson(url, {
            headers: { Referer: "https://peachify.top/", Origin: "https://peachify.top" }
          }, 2500);
          if (!resp) return null;
          let sources = [];
          if (resp.isEncrypted && typeof resp.data === "string") {
            const parts = resp.data.split(".");
            if (parts.length === 3) {
              const iv = CryptoUtils.base64UrlDecode(parts[0]);
              const ct = CryptoUtils.base64UrlDecode(parts[1]);
              const tag = CryptoUtils.base64UrlDecode(parts[2]);
              const cipherWithTag = new Uint8Array(ct.length + tag.length);
              cipherWithTag.set(ct, 0);
              cipherWithTag.set(tag, ct.length);
              const decrypted = await CryptoUtils.aesDecryptGcm(cipherWithTag, keyBytes, iv);
              const decResp = JSON.parse(new TextDecoder().decode(decrypted));
              sources = decResp.sources || [];
            }
          } else if (Array.isArray(resp.sources)) {
            sources = resp.sources;
          }
          const validSources = [];
          for (const item of sources) {
            const finalUrl = (item == null ? void 0 : item.url) || (item == null ? void 0 : item.src) || (item == null ? void 0 : item.file) || (item == null ? void 0 : item.stream) || (item == null ? void 0 : item.streamUrl) || (item == null ? void 0 : item.playbackUrl);
            if (finalUrl) {
              validSources.push({
                url: finalUrl,
                quality: item.quality || "Auto",
                format: finalUrl.includes(".m3u8") ? "hls" : "mp4",
                isM3u8: finalUrl.includes(".m3u8")
              });
            }
          }
          if (validSources.length) {
            return {
              providerName: this.name,
              sources: validSources
            };
          }
        } catch (_) {
        }
        return null;
      });
      const results = await Promise.allSettled(promises);
      for (const res of results) {
        if (res.status === "fulfilled" && res.value && ((_a = res.value.sources) == null ? void 0 : _a.length)) {
          return res.value;
        }
      }
    } catch (_) {
    }
    return null;
  }
}
class VidZeeProvider {
  constructor() {
    this.name = "VidZee";
    this.coreUrl = "https://core.vidzee.wtf";
    this.playerUrl = "https://player.vidzee.wtf";
  }
  async extractStreams(request) {
    try {
      const keyBlob = await fetchText("".concat(this.coreUrl, "/api-key"), {
        headers: { Referer: this.playerUrl, Origin: this.playerUrl }
      });
      if (!keyBlob) return null;
      const rawBytes = CryptoUtils.base64Decode(keyBlob);
      if (rawBytes.length <= 28) return null;
      const iv = rawBytes.slice(0, 12);
      const tag = rawBytes.slice(12, 28);
      const cipher = rawBytes.slice(28);
      const cipherWithTag = new Uint8Array(cipher.length + tag.length);
      cipherWithTag.set(cipher, 0);
      cipherWithTag.set(tag, cipher.length);
      const deriveKeySecret = await CryptoUtils.sha256(new TextEncoder().encode("4f2a9c7d1e8b3a6f0d5c2e9a7b1f4d8c"));
      const derivedDecrypted = await CryptoUtils.aesDecryptGcm(cipherWithTag, deriveKeySecret, iv);
      const aesKeyStr = new TextDecoder().decode(derivedDecrypted);
      const aesKeyBytes = new Uint8Array(32);
      const strBytes = new TextEncoder().encode(aesKeyStr);
      aesKeyBytes.set(strBytes.slice(0, 32));
      for (let sr = 0; sr <= 6; sr++) {
        try {
          const serverUrl = request.type === "movie" ? "".concat(this.playerUrl, "/api/server?id=").concat(request.tmdbId, "&sr=").concat(sr) : "".concat(this.playerUrl, "/api/server?id=").concat(request.tmdbId, "&sr=").concat(sr, "&ss=").concat(request.season || 1, "&ep=").concat(request.episode || 1);
          const sResp = await fetchJson(serverUrl, {
            headers: { Referer: this.playerUrl, Origin: this.playerUrl }
          });
          if (!(sResp == null ? void 0 : sResp.url) || !Array.isArray(sResp.url)) continue;
          const links = [];
          for (const item of sResp.url) {
            try {
              const decoded = new TextDecoder().decode(CryptoUtils.base64Decode(item.link));
              const parts = decoded.split(":");
              if (parts.length === 2) {
                const streamIv = CryptoUtils.base64Decode(parts[0]);
                const streamCipher = CryptoUtils.base64Decode(parts[1]);
                const plain = await CryptoUtils.aesDecryptCbc(streamCipher, aesKeyBytes, streamIv);
                const plainStr = new TextDecoder().decode(plain);
                if (plainStr.startsWith("http")) links.push(plainStr);
              }
            } catch (_) {
            }
          }
          if (links.length) {
            return {
              providerName: this.name,
              sources: links.map((u) => ({
                url: u,
                quality: "Auto",
                format: u.includes(".m3u8") ? "hls" : "mp4",
                isM3u8: u.includes(".m3u8")
              })),
              subtitles: (sResp.tracks || []).map((t) => ({
                url: t.url,
                language: t.lang,
                label: t.lang
              }))
            };
          }
        } catch (_) {
          continue;
        }
      }
    } catch (_) {
    }
    return null;
  }
}
class MovieDownloader02Provider {
  constructor() {
    this.name = "02MovieDownloader";
    this.baseUrl = "https://02moviedownloader.site";
  }
  async extractStreams(request) {
    var _a, _b, _c;
    try {
      const tokenResp = await fetchJson("".concat(this.baseUrl, "/api/verify-robot"), {
        method: "POST",
        headers: { Origin: this.baseUrl, Referer: "".concat(this.baseUrl, "/") }
      });
      const token = tokenResp == null ? void 0 : tokenResp.token;
      if (!token) return null;
      const path = request.type === "movie" ? "/api/download/movie/".concat(request.tmdbId) : "/api/download/tv/".concat(request.tmdbId, "/").concat(request.season || 1, "/").concat(request.episode || 1);
      const respText = await fetchText("".concat(this.baseUrl).concat(path), {
        headers: { "x-session-token": token, Referer: "".concat(this.baseUrl).concat(path) }
      });
      if (!respText) return null;
      let decryptedJson = respText;
      if (respText.includes('"encrypted":true')) {
        const tree = JSON.parse(respText);
        const bundle = tree.data || "";
        const parts = bundle.split(":");
        const iv = CryptoUtils.base64Decode(parts[0]);
        const cipher = CryptoUtils.base64Decode(parts[1]);
        const key = await CryptoUtils.sha256(new TextEncoder().encode(token));
        const plain = await CryptoUtils.aesDecryptCbc(cipher, key, iv);
        decryptedJson = new TextDecoder().decode(plain);
      }
      const parsed = JSON.parse(decryptedJson);
      const downloads = ((_c = (_b = (_a = parsed == null ? void 0 : parsed.data) == null ? void 0 : _a.downloadData) == null ? void 0 : _b.data) == null ? void 0 : _c.downloads) || [];
      const sources = downloads.filter((d) => d && d.url).map((d) => ({
        url: d.url,
        quality: String(d.resolution || "1080"),
        format: "mp4",
        isM3u8: false
      }));
      if (!sources.length) return null;
      return {
        providerName: this.name,
        sources
      };
    } catch (_) {
      return null;
    }
  }
}
class TulnexProvider {
  constructor() {
    this.name = "Tulnex";
    this.baseUrl = "https://api.tulnex.com";
    this.servers = [
      "onion",
      "vidzee",
      "icefy",
      "tik",
      "vaplayer",
      "vidfast-alpha",
      "uniquestream",
      "vidfast-mega",
      "vidfast-vrapid",
      "allmovies",
      "vidlink",
      "vidfast-vedge",
      "vidfast-vfast",
      "moviebox"
    ];
  }
  extractTulnexUrl(obj) {
    var _a, _b, _c;
    if (!obj || typeof obj !== "object") return null;
    const direct = obj.url || obj.stream || obj.playlist || obj.streamUrl || obj.stream_url || obj.streaming_url || obj.video_url || obj.m3u8;
    if (typeof direct === "string" && direct.startsWith("http")) return direct;
    if (Array.isArray(obj.sources)) {
      for (const elem of obj.sources) {
        if (typeof (elem == null ? void 0 : elem.url) === "string" && elem.url.startsWith("http")) return elem.url;
      }
    }
    if (Array.isArray(obj.links)) {
      for (const elem of obj.links) {
        if (typeof (elem == null ? void 0 : elem.url) === "string" && elem.url.startsWith("http")) return elem.url;
      }
    }
    if (obj.data) {
      const dataStream = (_b = (_a = obj.data) == null ? void 0 : _a.stream) == null ? void 0 : _b.playlist;
      if (typeof dataStream === "string" && dataStream.startsWith("http")) return dataStream;
      const dataUrl = (_c = obj.data) == null ? void 0 : _c.url;
      if (typeof dataUrl === "string" && dataUrl.startsWith("http")) return dataUrl;
    }
    if (Array.isArray(obj.streams)) {
      for (const elem of obj.streams) {
        const u = (elem == null ? void 0 : elem.url) || (elem == null ? void 0 : elem.link);
        if (typeof u === "string" && u.startsWith("http")) return u;
      }
    }
    return null;
  }
  async extractStreams(request) {
    for (const server of this.servers) {
      try {
        const url = request.type === "movie" ? "".concat(this.baseUrl, "/").concat(server, "/movie/").concat(request.tmdbId) : "".concat(this.baseUrl, "/").concat(server, "/tv/").concat(request.tmdbId, "/").concat(request.season || 1, "/").concat(request.episode || 1);
        const resp = await fetchJson(url);
        const payload = resp == null ? void 0 : resp.payload;
        if (!payload) continue;
        const sep = payload.indexOf("|");
        if (sep === -1) continue;
        const rawPayloadB64 = payload.substring(sep + 1);
        const l4out = new TextDecoder().decode(CryptoUtils.base64Decode(rawPayloadB64));
        const l3parts = l4out.split(".");
        if (l3parts.length !== 3) continue;
        const iv = CryptoUtils.base64Decode(l3parts[0]);
        const salt = CryptoUtils.base64Decode(l3parts[1]);
        const cipher = CryptoUtils.base64Decode(l3parts[2]);
        const l3KeyPass = CryptoUtils.base64Decode("U24wMHBEMGcjTDNfQUVTX1MzY3VyM0szeUAyMDI2JHNleA==");
        const keyBytes = await CryptoUtils.pbkdf2(l3KeyPass, salt, 1e5, 32, "SHA-512");
        const l3Decrypted = new TextDecoder().decode(await CryptoUtils.aesDecryptCbc(cipher, keyBytes, iv));
        const binDecoded = new TextDecoder().decode(CryptoUtils.base64Decode(l3Decrypted)).split(" ").map((b) => String.fromCharCode(parseInt(b, 2))).join("");
        const l1Key = CryptoUtils.base64Decode("U24wMHBEMGcjTDFfWDBSX000c3QzckszeSEyMDI2c2V4");
        const l1Salt = CryptoUtils.base64Decode("eEs5IW1SMkBwTDUjblE4c2V4");
        const xorKey = await CryptoUtils.pbkdf2(l1Key, l1Salt, 5e4, 32, "SHA-256");
        const hexBytes = CryptoUtils.hexToBytes(binDecoded);
        const outBytes = new Uint8Array(hexBytes.length);
        for (let i = 0; i < hexBytes.length; i++) {
          outBytes[i] = hexBytes[i] ^ xorKey[i % 32];
        }
        const jsonStr = new TextDecoder().decode(outBytes);
        const parsedTree = JSON.parse(jsonStr);
        const streamUrl = this.extractTulnexUrl(parsedTree);
        if (streamUrl) {
          return {
            providerName: this.name,
            sources: [{
              url: streamUrl,
              quality: "Auto",
              format: streamUrl.includes(".m3u8") ? "hls" : "mp4",
              isM3u8: streamUrl.includes(".m3u8")
            }]
          };
        }
      } catch (_) {
        continue;
      }
    }
    return null;
  }
}
const streamProviders = Object.freeze([
  new VidApiProvider(),
  new VixSrcProvider(),
  new VidSrcProvider(),
  new FMovies4UProvider(),
  new VidZeeProvider(),
  new AnyEmbedProvider(),
  new CineSuProvider(),
  new FShareProvider(),
  new IcefyProvider(),
  new PeachifyProvider(),
  new PoprProvider(),
  new StreamMafiaProvider(),
  new TulnexProvider(),
  new VidEasyProvider(),
  new VidNestProvider(),
  new VidRockProvider(),
  new MovieDownloader02Provider()
]);
function normalizeQuality(raw, url) {
  const text = "".concat(raw || "", " ").concat((url || "").split("?")[0].split("/").pop() || "").toLowerCase();
  if (text.includes("2160") || text.includes("4k") || text.includes("uhd")) return "2160p";
  if (text.includes("1440") || text.includes("2k")) return "1440p";
  if (text.includes("1080") || text.includes("fhd")) return "1080p";
  if (text.includes("720") || /(^|[^a-z])hd([^a-z]|$)/.test(text)) return "720p";
  if (text.includes("480")) return "480p";
  if (text.includes("360")) return "360p";
  return "Auto";
}
function qualityRank(label) {
  const num = parseInt(String(label).replace(/p$/, ""), 10);
  return Number.isFinite(num) ? num : 0;
}
function normalizeLanguage(language) {
  if (!language || typeof language !== "string") return null;
  const value = language.trim().toLowerCase().split("-")[0].split("_")[0];
  if (!value) return null;
  switch (value) {
    case "original audio":
    case "original language":
      return "original";
    case "english":
      return "en";
    case "spanish":
      return "es";
    case "french":
      return "fr";
    case "german":
      return "de";
    case "italian":
      return "it";
    case "japanese":
      return "ja";
    case "korean":
      return "ko";
    case "chinese":
    case "mandarin":
      return "zh";
    case "hindi":
      return "hi";
    case "portuguese":
      return "pt";
    case "russian":
      return "ru";
    case "arabic":
      return "ar";
    default:
      return value;
  }
}
function languagePreferenceRank(language) {
  const normalized = normalizeLanguage(language);
  if (!normalized) return 1;
  if (normalized === "en") return 0;
  return 2;
}
async function resolveStreams(media, episode, customProviders) {
  var _a;
  if (!media) return [];
  const rawId = String(media.id || "");
  const tmdbId = rawId.replace(/^(movie|tv|series):/, "");
  const hasEpisode = Boolean(episode && (episode.season || episode.episode || episode.season_number || episode.episode_number));
  const mType = String(media.media_type || media.type || (hasEpisode ? "tv" : media.title && !media.name ? "movie" : "tv")).toLowerCase();
  const isMovie = !hasEpisode && mType === "movie";
  const releaseDate = media.release_date || media.first_air_date || "";
  const releaseYear = releaseDate ? parseInt(releaseDate.slice(0, 4), 10) : null;
  let season = null;
  let epNum = null;
  if (!isMovie) {
    if (typeof episode === "number") {
      season = 1;
      epNum = episode;
    } else if (episode && typeof episode === "object") {
      season = Number(episode.season || episode.season_number || 1);
      epNum = Number(episode.episode || episode.episode_number || 1);
    } else {
      season = 1;
      epNum = 1;
    }
  }
  const title = media.title || media.name || "";
  let imdbId = media.imdb_id || media.imdbId || ((_a = media.external_ids) == null ? void 0 : _a.imdb_id) || null;
  if (!imdbId) {
    const tmdbFn = typeof window !== "undefined" && typeof window.tmdb === "function" ? window.tmdb : typeof tmdb === "function" ? tmdb : null;
    if (tmdbFn) {
      try {
        const ext = await tmdbFn("/".concat(isMovie ? "movie" : "tv", "/").concat(tmdbId, "/external_ids"));
        if (ext == null ? void 0 : ext.imdb_id) imdbId = ext.imdb_id;
      } catch (_) {
      }
    }
  }
  const request = {
    tmdbId,
    imdbId,
    title,
    releaseYear,
    season,
    episode: epNum,
    type: isMovie ? "movie" : "series"
  };
  const providersToUse = Array.isArray(customProviders) && customProviders.length > 0 ? customProviders : streamProviders;
  const results = await Promise.allSettled(
    providersToUse.map(async (provider) => {
      try {
        return await timeoutPromise(6e3, provider.extractStreams(request));
      } catch (_) {
        return null;
      }
    })
  );
  const successfulResults = results.filter((r) => r.status === "fulfilled" && r.value && Array.isArray(r.value.sources) && r.value.sources.length > 0).map((r) => r.value);
  const allSubtitles = [];
  successfulResults.forEach((res) => {
    if (Array.isArray(res.subtitles)) {
      for (const sub of res.subtitles) {
        if ((sub == null ? void 0 : sub.url) && !allSubtitles.some((existing) => existing.url === sub.url)) {
          allSubtitles.push(sub);
        }
      }
    }
  });
  const flattened = [];
  for (const res of successfulResults) {
    for (const src of res.sources) {
      if (!src || !src.url) continue;
      const unwrappedUrl = ProxyUnwrapper.unwrapUrl(src.url);
      try {
        const parsed = new URL(unwrappedUrl);
        if (parsed.protocol !== "http:" && parsed.protocol !== "https:") continue;
      } catch (e) {
        continue;
      }
      const label = normalizeQuality(src.quality, unwrappedUrl);
      flattened.push({
        url: unwrappedUrl,
        label,
        quality: label,
        format: src.format || (unwrappedUrl.includes(".mp4") ? "mp4" : "hls"),
        isM3u8: typeof src.isM3u8 === "boolean" ? src.isM3u8 : !unwrappedUrl.includes(".mp4"),
        providerName: res.providerName || "Stream",
        audioLanguage: src.audioLanguage || null,
        headers: res.headers || {},
        subtitles: allSubtitles
      });
    }
  }
  const seenUrls = /* @__PURE__ */ new Set();
  const unique = [];
  for (const s of flattened) {
    if (!seenUrls.has(s.url)) {
      seenUrls.add(s.url);
      unique.push(s);
    }
  }
  unique.sort((a, b) => {
    const langDiff = languagePreferenceRank(a.audioLanguage) - languagePreferenceRank(b.audioLanguage);
    if (langDiff !== 0) return langDiff;
    return qualityRank(b.label) - qualityRank(a.label);
  });
  return unique;
}
const exportedSymbols = {
  CryptoUtils,
  ProxyUnwrapper,
  getProxyUrl,
  VidSrcProvider,
  VixSrcProvider,
  FMovies4UProvider,
  VidZeeProvider,
  AnyEmbedProvider,
  CineSuProvider,
  FShareProvider,
  IcefyProvider,
  PeachifyProvider,
  PoprProvider,
  StreamMafiaProvider,
  TulnexProvider,
  VidApiProvider,
  VidEasyProvider,
  VidNestProvider,
  VidRockProvider,
  MovieDownloader02Provider,
  streamProviders,
  resolveStreams,
  normalizeQuality,
  qualityRank,
  normalizeLanguage,
  languagePreferenceRank
};
if (typeof window !== "undefined") {
  Object.assign(window, exportedSymbols);
}
if (typeof globalThis !== "undefined") {
  Object.assign(globalThis, exportedSymbols);
}
if (typeof module !== "undefined" && module.exports) {
  module.exports = exportedSymbols;
}
