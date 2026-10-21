// Composes EmuUI's store assets from the real UI renders in store/renders.
//
//   ./gradlew :lemuroid-app:testFreeDynamicDebugUnitTest --tests '*StoreScreenshots*' -PemuuiScreenshots
//   NODE_PATH=$(npm root -g) node store/tools/compose.mjs
//
// Needs Playwright's Chromium and the OFL fonts Inter, Inter Display and JetBrains Mono.
// Every screen shown is a render of the app's own Compose UI with original placeholder games.
import { createRequire } from 'node:module'
import { mkdirSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const require = createRequire(import.meta.url)
const { chromium } = require('playwright')

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const RENDERS = join(ROOT, 'store/renders')
const OUT = join(ROOT, 'store')
const img = (name) => pathToFileURL(join(RENDERS, name)).href

const C = {
  ink: '#252D2B', shell: '#F2F0E6', paper: '#FCFBF5', muted: '#647069', lime: '#D0EB82',
  moss: '#486B35', lilac: '#DAD6EF', peach: '#F0CCAD', screen: '#202D29', teal: '#BADAD6',
}

// The same quiet two-layer shadow the app uses: a tight contact shadow and a faint wide one.
const SHADOW = '0 2px 6px rgba(37,45,43,.06), 0 28px 64px -18px rgba(37,45,43,.16)'

const base = (w, h, body, extraCss = '') => `<!doctype html><html><head><meta charset="utf-8"><style>
  html,body{margin:0;width:${w}px;height:${h}px;overflow:hidden}
  body{position:relative;background:${C.shell};font-family:'Inter',sans-serif;color:${C.ink};
    -webkit-font-smoothing:antialiased}
  .wash{position:absolute;inset:0;background:
    radial-gradient(60% 70% at 8% 8%, rgba(208,235,130,.62), transparent 70%),
    radial-gradient(55% 65% at 96% 92%, rgba(218,214,239,.75), transparent 70%),
    radial-gradient(40% 50% at 70% 10%, rgba(240,204,173,.35), transparent 70%)}
  .label{font-family:'JetBrains Mono',monospace;font-weight:500;letter-spacing:.14em;text-transform:uppercase;color:${C.moss}}
  .head{font-family:'Inter Display','Inter',sans-serif;font-weight:800;letter-spacing:-.035em;line-height:.98;margin:0}
  .sub{font-weight:400;color:${C.muted};line-height:1.32;margin:0}
  .shot{position:absolute;border-radius:44px;overflow:hidden;box-shadow:${SHADOW};background:${C.paper}}
  .shot img{display:block;width:100%;height:100%}
  .dot{display:inline-block;width:.32em;height:.32em;border-radius:50%;background:${C.moss};margin-right:.3em;vertical-align:.18em}
  ${extraCss}
</style></head><body><div class="wash"></div>${body}</body></html>`

/** A 16:9 phone listing image: a short caption beside one real screen. Text stays under 15%. */
function phoneSingle({ label, head, sub, shot, shotW, shotH }) {
  const h = 920
  const w = Math.round(shotW * h / shotH)
  return base(1920, 1080, `
    <div style="position:absolute;left:96px;top:0;bottom:0;width:${1920 - w - 96 - 96 - 72}px;display:flex;flex-direction:column;justify-content:center;gap:28px">
      <div class="label" style="font-size:22px">${label}</div>
      <h1 class="head" style="font-size:78px">${head}</h1>
      <p class="sub" style="font-size:29px">${sub}</p>
    </div>
    <div class="shot" style="right:96px;top:80px;width:${w}px;height:${h}px"><img src="${img(shot)}"></div>`)
}

/** Two foldable form factors side by side: a clamshell in portrait and a book-style fold. */
function phoneDuo({ label, head, sub }) {
  const h = 760
  const tallW = Math.round(1078 * h / 2520)
  const wideW = Math.round(2184 * h / 1968)
  const gap = 36
  const right = 96
  const textW = 1920 - right - wideW - gap - tallW - 96 - 64
  return base(1920, 1080, `
    <div style="position:absolute;left:96px;top:0;bottom:0;width:${textW}px;display:flex;flex-direction:column;justify-content:center;gap:26px">
      <div class="label" style="font-size:22px">${label}</div>
      <h1 class="head" style="font-size:64px">${head}</h1>
      <p class="sub" style="font-size:27px">${sub}</p>
    </div>
    <div class="shot" style="right:${right + wideW + gap}px;top:160px;width:${tallW}px;height:${h}px;border-radius:36px"><img src="${img('clamshell-home.png')}"></div>
    <div class="shot" style="right:${right}px;top:160px;width:${wideW}px;height:${h}px"><img src="${img('bookfold-inner-home.png')}"></div>`)
}

/** A full-bleed screen with nothing added, for large-screen sets where Play forbids overlay text. */
const bare = (shot, w, h) =>
  `<!doctype html><html><head><style>html,body{margin:0;width:${w}px;height:${h}px;overflow:hidden;background:${C.shell}}
  img{display:block;width:${w}px;height:${h}px}</style></head><body><img src="${img(shot)}"></body></html>`

function wordmark(size) {
  return `<div class="head" style="font-size:${size}px;letter-spacing:-.05em"><span class="dot"></span>emuui</div>`
}

/** 1024 x 500, no alpha. Brand colour field, centred focal point, no icon repeat, no badges. */
function featureGraphic() {
  return base(1024, 500, `
    <div style="position:absolute;inset:0;background:linear-gradient(115deg, ${C.lime} 0%, #E3F2B4 46%, ${C.lilac} 100%)"></div>
    <div style="position:absolute;left:72px;top:0;bottom:0;width:430px;display:flex;flex-direction:column;justify-content:center;gap:18px">
      ${wordmark(104)}
      <p class="sub" style="font-size:27px;color:${C.ink};opacity:.82">Play your own games on a foldable. Game above, controls below.</p>
    </div>
    <div class="shot" style="left:548px;top:52px;width:${Math.round(2152 * 396 / 2076)}px;height:396px;border-radius:26px"><img src="${img('fold-inner-home.png')}"></div>`)
}

/** Wide brand banners (Galaxy Store hero, TV banner, GitHub social preview). */
function banner(w, h, scale = 1) {
  const shotH = Math.round(h * 0.78)
  const shotW = Math.round(2152 * shotH / 2076)
  return base(w, h, `
    <div style="position:absolute;inset:0;background:linear-gradient(115deg, ${C.lime} 0%, #E3F2B4 46%, ${C.lilac} 100%)"></div>
    <div style="position:absolute;left:${Math.round(w * 0.07)}px;top:0;bottom:0;width:${Math.round(w * 0.42)}px;display:flex;flex-direction:column;justify-content:center;gap:${Math.round(18 * scale)}px">
      ${wordmark(Math.round(118 * scale))}
      <p class="sub" style="font-size:${Math.round(30 * scale)}px;color:${C.ink};opacity:.82">An emulator front end for foldable phones.</p>
    </div>
    <div class="shot" style="right:${Math.round(w * 0.06)}px;top:${Math.round((h - shotH) / 2)}px;width:${shotW}px;height:${shotH}px;border-radius:${Math.round(30 * scale)}px"><img src="${img('fold-inner-home.png')}"></div>`)
}

/** Store icon: full-bleed square, no rounded corners or outer shadow (the stores add both). */
function icon(size) {
  // Paths from lemuroid-app/src/main/res/drawable/emuui_launcher.xml, cropped to its keyline.
  return `<!doctype html><html><head><style>html,body{margin:0;width:${size}px;height:${size}px;overflow:hidden;background:#20291E}</style></head><body>
  <svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="13 13 82 82">
    <path fill="#20291E" d="M0,0h108v108h-108z"/>
    <path fill="#D3F581" d="M26,23h56v28h-56zM26,57h56v28h-56z"/>
    <path fill="#20291E" d="M33,30h42v14h-42zM36,64h6v17h-6zM31,69h16v6h-16zM65,65h6v6h-6zM73,74h6v6h-6z"/>
  </svg></body></html>`
}

const jobs = [
  // Google Play phone set, 16:9 landscape (the app is landscape-first). Real UI leads every image.
  ['play/phone/01-library.png', 1920, 1080, phoneSingle({
    label: '01 · Library', head: 'Your game library on a foldable.',
    sub: 'Pick a game on the lower half. It appears on the upper half, ready to play.',
    shot: 'fold-inner-home.png', shotW: 2152, shotH: 2076 })],
  ['play/phone/02-play.png', 1920, 1080, phoneSingle({
    label: '02 · Play', head: 'Game above, controls below.',
    sub: 'Hold a half-open foldable sideways. The controls stay clear of the crease.',
    shot: 'fold-inner-game.png', shotW: 2152, shotH: 2076 })],
  ['play/phone/03-pause.png', 1920, 1080, phoneSingle({
    label: '03 · Pause', head: 'A pause menu that blurs the game.',
    sub: 'Save, load, restart or change settings. The paused game stays behind the menu.',
    shot: 'fold-inner-menu.png', shotW: 2152, shotH: 2076 })],
  ['play/phone/04-every-fold.png', 1920, 1080, phoneDuo({
    label: '04 · Foldables', head: 'Book-style and flip foldables.',
    sub: 'EmuUI finds the crease and keeps buttons and text off it.' })],
  ['play/phone/05-welcome.png', 1920, 1080, phoneSingle({
    label: '05 · Your games', head: 'Use your own game files.',
    sub: 'Choose a folder of games you have the right to use. No games are included.',
    shot: 'fold-inner-welcome.png', shotW: 2152, shotH: 2076 })],
  ['play/phone/06-phones.png', 1920, 1080, phoneSingle({
    label: '06 · Phones', head: 'Works on regular phones too.',
    sub: 'Without a crease, the screen splits into the same two halves.',
    shot: 'phone-16x9-home.png', shotW: 1920, shotH: 1080 })],
  // Large-screen sets: 16:9, no overlay text (Play may crop it on large-screen surfaces).
  ...['home', 'game', 'menu', 'welcome'].flatMap((scene, i) => [
    [`play/tenInch/0${i + 1}-${scene}.png`, 2560, 1440, bare(`tablet-16x9-${scene}.png`, 2560, 1440)],
    [`play/sevenInch/0${i + 1}-${scene}.png`, 1920, 1080, bare(`tablet-16x9-${scene}.png`, 1920, 1080)],
  ]),
  ['play/featureGraphic.png', 1024, 500, featureGraphic()],
  ['play/icon.png', 512, 512, icon(512)],
  ['play/tvBanner.png', 1280, 720, banner(1280, 720, 1)],
  // Samsung Galaxy Store: native foldable captures pass its 2:1 limit as they are.
  ...['home', 'game', 'menu', 'welcome'].map((scene, i) =>
    [`galaxy/screenshots/0${i + 1}-fold-${scene}.png`, 2152, 2076, bare(`fold-inner-${scene}.png`, 2152, 2076)]),
  ['galaxy/screenshots/05-bookfold-home.png', 2184, 1968, bare('bookfold-inner-home.png', 2184, 1968)],
  ['galaxy/screenshots/06-phone-game.png', 1920, 1080, bare('phone-16x9-game.png', 1920, 1080)],
  ['galaxy/header.png', 1200, 675, banner(1200, 675, 0.95)],
  ['galaxy/icon.png', 512, 512, icon(512)],
  ['social/github-social-preview.png', 1280, 640, banner(1280, 640, 1)],
]

const browser = await chromium.launch()
const page = await browser.newPage({ deviceScaleFactor: 1 })
for (const [path, w, h, html] of jobs) {
  const out = join(OUT, path)
  mkdirSync(dirname(out), { recursive: true })
  await page.setViewportSize({ width: w, height: h })
  const file = join(OUT, '.page.html')
  writeFileSync(file, html)
  await page.goto(pathToFileURL(file).href)
  await page.evaluate(() => document.fonts.ready)
  // Opaque PNGs: Play rejects alpha on screenshots and the feature graphic.
  await page.screenshot({ path: out, omitBackground: false, clip: { x: 0, y: 0, width: w, height: h } })
  console.log(`${path}  ${w}x${h}`)
}
await browser.close()
rmSync(join(OUT, '.page.html'), { force: true })
