"use strict";

// Global polyfills for Samsung Smart TV (Tizen 4.0 / Chromium 56)

// 1. globalThis
if (typeof globalThis === "undefined") {
    (function () {
        if (typeof self !== "undefined") { self.globalThis = self; }
        else if (typeof window !== "undefined") { window.globalThis = window; }
        else if (typeof global !== "undefined") { global.globalThis = global; }
    })();
}

// 2. String.prototype.padStart & padEnd
if (!String.prototype.padStart) {
    String.prototype.padStart = function (targetLength, padString) {
        targetLength = targetLength >> 0;
        padString = String(typeof padString !== "undefined" ? padString : " ");
        if (this.length > targetLength) return String(this);
        targetLength = targetLength - this.length;
        if (targetLength > padString.length) {
            padString += padString.repeat(targetLength / padString.length);
        }
        return padString.slice(0, targetLength) + String(this);
    };
}
if (!String.prototype.padEnd) {
    String.prototype.padEnd = function (targetLength, padString) {
        targetLength = targetLength >> 0;
        padString = String(typeof padString !== "undefined" ? padString : " ");
        if (this.length > targetLength) return String(this);
        targetLength = targetLength - this.length;
        if (targetLength > padString.length) {
            padString += padString.repeat(targetLength / padString.length);
        }
        return String(this) + padString.slice(0, targetLength);
    };
}

// 3. Array.prototype.flat & flatMap
if (!Array.prototype.flat) {
    Array.prototype.flat = function (depth) {
        depth = depth === undefined ? 1 : Number(depth);
        return depth > 0
            ? Array.prototype.reduce.call(this, function (acc, cur) {
                return acc.concat(Array.isArray(cur) ? cur.flat(depth - 1) : cur);
            }, [])
            : Array.prototype.slice.call(this);
    };
}
if (!Array.prototype.flatMap) {
    Array.prototype.flatMap = function (callback, thisArg) {
        return Array.prototype.map.call(this, callback, thisArg).flat();
    };
}

// 4. Object.fromEntries
if (!Object.fromEntries) {
    Object.fromEntries = function (entries) {
        if (!entries || !entries[Symbol.iterator]) {
            throw new TypeError("Object.fromEntries() requires a single iterable argument");
        }
        var obj = {};
        var iterator = entries[Symbol.iterator]();
        var step;
        while (!(step = iterator.next()).done) {
            var pair = step.value;
            if (Object(pair) !== pair) {
                throw new TypeError("iterator value is not an entry object");
            }
            obj[pair[0]] = pair[1];
        }
        return obj;
    };
}

// 5. Promise.allSettled
if (!Promise.allSettled) {
    Promise.allSettled = function (promises) {
        return Promise.all(
            Array.from(promises).map(function (p) {
                return Promise.resolve(p).then(
                    function (value) { return { status: "fulfilled", value: value }; },
                    function (reason) { return { status: "rejected", reason: reason }; }
                );
            })
        );
    };
}

// 6. Visible error banner on screen if any uncaught error occurs
window.addEventListener("error", function (e) {
    console.error("[ECHO Tizen Error]", e.message, e.filename, e.lineno, e.error);
});
window.addEventListener("unhandledrejection", function (e) {
    console.error("[ECHO Tizen Unhandled Rejection]", e.reason);
});
