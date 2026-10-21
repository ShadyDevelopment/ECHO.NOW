const fs = require("fs");
const path = require("path");
const cssPath = "c:/Users/Administrator/Documents/DEV/ECHO-NOW/ECHO NOW/tizenApp/css/app.css";
let css = fs.readFileSync(cssPath, "utf8");
css = css.replace(/background:\s*#09090d;/g, "background: #050505;");
css = css.replace(/body\s*\{\s*background:\s*#09090d;\s*\}/g, "body { background: #050505; }");
css = css.replace(/linear-gradient\(0deg,\s*#09090d\s*0%,\s*rgba\(9,\s*9,\s*13,\s*0\.733\)\s*38%,\s*rgba\(9,\s*9,\s*13,\s*0\.071\)\s*100%\)/g, "linear-gradient(0deg, #050505 0%, rgba(5, 5, 5, 0.733) 38%, rgba(5, 5, 5, 0.071) 100%)");
css = css.replace(/background:\s*#101014;/g, "background: #0f0f0f;");
const overlayRules = "\n/* Web App Matching Navigation Rail & Layout */\n.topbar {\n    z-index: 2;\n    display: flex;\n    flex: 0 0 84px !important;\n    width: 84px !important;\n    flex-direction: column;\n    align-items: center;\n    gap: 8px;\n    height: 100vh;\n    padding: 18px 8px 16px;\n    border-right: 1px solid rgba(255, 255, 255, 0.063);\n    background: #0f0f0f !important;\n}\n.topbar .brand-name,\n.topbar .brand-wordmark,\n.topbar .nav-button > span:not(.nav-icon),\n.topbar .profile-name { display: none !important; }\n.topbar .nav-button,\n.topbar .profile-button {\n    justify-content: center !important;\n    width: 56px !important;\n    height: 56px !important;\n    min-height: 56px !important;\n    padding: 0 !important;\n    border-radius: 14px !important;\n}\n.topbar .brand {\n    justify-content: center !important;\n    padding: 0 !important;\n    width: 52px !important;\n    height: 52px !important;\n    min-height: 52px !important;\n    border-radius: 12px !important;\n}\nnav {\n    display: flex;\n    flex: 1;\n    flex-direction: column;\n    align-items: center;\n    justify-content: space-between;\n    gap: 10px;\n    width: 100%;\n    min-height: 0;\n    margin-top: 10px;\n}\n.nav-search {\n    justify-content: center !important;\n    width: 56px !important;\n    height: 56px !important;\n    min-height: 56px !important;\n    padding: 0 !important;\n    border-radius: 14px !important;\n    margin-bottom: 24px !important;\n}\n.nav-links {\n    display: flex !important;\n    flex-direction: column !important;\n    align-items: center !important;\n    justify-content: center !important;\n    gap: 14px !important;\n    flex: 1 !important;\n    width: 100% !important;\n}\n.profile-button {\n    margin-top: auto !important;\n    justify-content: center !important;\n    width: 52px !important;\n    height: 52px !important;\n    min-height: 52px !important;\n    border-radius: 50% !important;\n    padding: 0 !important;\n}\n.profile-nav-icon {\n    display: inline-grid;\n    place-items: center;\n    width: 44px;\n    height: 44px;\n    border: 2px solid rgba(255, 255, 255, 0.333);\n    border-radius: 50%;\n    background: #1b1b22;\n    font-size: 15px;\n    overflow: hidden;\n}\n.profile-nav-icon img {\n    width: 100%;\n    height: 100%;\n    object-fit: cover;\n}\n\n/* Translucent Glowing Focus Overlay for Cards */\n.continue-watching-card:focus::after,\n.poster-card.focusable:focus::after,\n.catalog-media-card:focus::after,\n.episode-chip:focus::after {\n    content: '';\n    position: absolute;\n    inset: 0;\n    border-radius: inherit;\n    background: rgba(49, 167, 160, 0.28) !important;\n    border: 3px solid #31a7a0 !important;\n    box-shadow: 0 0 20px rgba(49, 167, 160, 0.6), inset 0 0 12px rgba(49, 167, 160, 0.25) !important;\n    pointer-events: none;\n    z-index: 10;\n}\n.focusable:focus, .focusable:focus-visible {\n    position: relative;\n    z-index: 5;\n    outline: 0;\n    border-color: #31a7a0 !important;\n    background-color: rgba(49, 167, 160, 0.28) !important;\n    box-shadow: 0 0 16px rgba(49, 167, 160, 0.55) !important;\n}\n.nav-button.active {\n    background: rgba(49, 167, 160, 0.24) !important;\n    border: 2px solid rgba(49, 167, 160, 0.65) !important;\n    color: #fff !important;\n}\n.nav-button.active .nav-icon {\n    fill: #31a7a0 !important;\n}\n";
if (!css.includes(".catalog-media-card:focus::after")) {
  css += "\n" + overlayRules;
}
fs.writeFileSync(cssPath, css, "utf8");
console.log("Updated tizenApp/css/app.css");
const composeThemePath = "c:/Users/Administrator/Documents/DEV/ECHO-NOW/ECHO NOW/composeApp/src/commonMain/kotlin/com/echonow/theme/EchoTheme.kt";
if (fs.existsSync(composeThemePath)) {
  let kt = fs.readFileSync(composeThemePath, "utf8");
  kt = kt.replace(/background = Color\(0xFF09090D\)/g, "background = Color(0xFF050505)");
  fs.writeFileSync(composeThemePath, kt, "utf8");
  console.log("Updated EchoTheme.kt");
}
const composeNavPath = "c:/Users/Administrator/Documents/DEV/ECHO-NOW/ECHO NOW/composeApp/src/commonMain/kotlin/com/echonow/presentation/navigation/EchoNowApp.kt";
if (fs.existsSync(composeNavPath)) {
  let kt = fs.readFileSync(composeNavPath, "utf8");
  kt = kt.replace(/val navigationSurface = Color\(0xFF101014\)/g, "val navigationSurface = Color(0xFF0F0F0F)");
  fs.writeFileSync(composeNavPath, kt, "utf8");
  console.log("Updated EchoNowApp.kt");
}
const composeHomePath = "c:/Users/Administrator/Documents/DEV/ECHO-NOW/ECHO NOW/composeApp/src/commonMain/kotlin/com/echonow/presentation/screens/HomeScreen.kt";
if (fs.existsSync(composeHomePath)) {
  let kt = fs.readFileSync(composeHomePath, "utf8");
  kt = kt.replace(/Color\(0xFF09090D\)\.copy\(alpha = \.72f\)/g, "Color(0xFF050505).copy(alpha = .72f)");
  fs.writeFileSync(composeHomePath, kt, "utf8");
  console.log("Updated HomeScreen.kt");
}
const composeDetailsPath = "c:/Users/Administrator/Documents/DEV/ECHO-NOW/ECHO NOW/composeApp/src/commonMain/kotlin/com/echonow/presentation/screens/DetailsScreen.kt";
if (fs.existsSync(composeDetailsPath)) {
  let kt = fs.readFileSync(composeDetailsPath, "utf8");
  kt = kt.replace(/Color\(0xFF09090D\)/g, "Color(0xFF050505)");
  fs.writeFileSync(composeDetailsPath, kt, "utf8");
  console.log("Updated DetailsScreen.kt");
}
