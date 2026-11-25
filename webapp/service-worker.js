/*
 * Lutece PWA plugin service worker.
 *
 * - Pages (navigations) always come from the network and are never cached, so a page
 *   personal to a signed-in user is never shown to anyone else. When the network fails,
 *   the offline page precached at install is shown instead. The stylesheets it links (the
 *   site theme), the stylesheets they import and the web fonts they load are precached with
 *   it, in the offline cache that is never trimmed: they are the fallback of these assets
 *   when the network fails.
 * - Static assets of the site (styles, scripts, images, fonts under themes/, css/, js/
 *   and images/) are served stale-while-revalidate from a versioned cache of bounded
 *   size. JSPs and servlets are never cached, as their content may depend on the user.
 *   Failing to store an asset never fails its delivery.
 * - Only GET requests of the site's own origin are handled; anything else goes to the
 *   network untouched.
 *
 * Updates: a new version of this file waits until the page asks it to take over (the
 * "reload" banner of sw-registration.js posts SKIP_WAITING). It takes over at once on a
 * first install, and when it replaces the worker of plugin-pwa 1.x, which cached pages.
 *
 * Bump VERSION with each release that changes this file: the new worker deletes the
 * caches of the previous one when it activates.
 */
const VERSION = "2.0.0";
const CACHE_PREFIX = "lutece-pwa-";
const OFFLINE_CACHE = `${CACHE_PREFIX}offline-${VERSION}`;
const ASSETS_CACHE = `${CACHE_PREFIX}assets-${VERSION}`;
const LEGACY_CACHES = ["Lutece PWA Plugin - ShellCache", "Lutece PWA Plugin - ContentCache"];
const OFFLINE_URL = new URL("servlet/plugins/pwa/offline", self.registration.scope).href;
const ASSET_DESTINATIONS = ["style", "script", "image", "font"];
const MAX_ASSETS = 150;
const MAX_IMPORT_DEPTH = 4;
const STATIC_PATHS = ["themes/", "css/", "js/", "images/"].map(path => new URL(path, self.registration.scope).pathname);

self.addEventListener("install", event => {
  event.waitUntil(
    (async () => {
      await precacheOffline();
      const replacesLegacy = (await Promise.all(LEGACY_CACHES.map(name => caches.has(name)))).some(Boolean);
      if (!self.registration.active || replacesLegacy) {
        await self.skipWaiting();
      }
    })()
  );
});

self.addEventListener("message", event => {
  if (event.data && event.data.type === "SKIP_WAITING") {
    self.skipWaiting();
  }
});

self.addEventListener("activate", event => {
  event.waitUntil(
    (async () => {
      const keep = [OFFLINE_CACHE, ASSETS_CACHE];
      const names = await caches.keys();
      await Promise.all(
        names
          .filter(name => (name.startsWith(CACHE_PREFIX) || LEGACY_CACHES.includes(name)) && !keep.includes(name))
          .map(name => caches.delete(name))
      );
      if (self.registration.navigationPreload) {
        await self.registration.navigationPreload.enable();
      }
      await self.clients.claim();
    })()
  );
});

self.addEventListener("fetch", event => {
  const request = event.request;
  if (request.method !== "GET" || new URL(request.url).origin !== self.location.origin) {
    return;
  }
  if (request.mode === "navigate") {
    event.respondWith(networkOrOffline(event));
    return;
  }
  if (ASSET_DESTINATIONS.includes(request.destination) && isStaticPath(request.url)) {
    event.respondWith(staleWhileRevalidate(event));
  }
});

/**
 * Precaches the offline page, then the stylesheets it links, the stylesheets they import
 * (a theme stylesheet is often only a list of @import) and the web fonts they load, so the
 * page keeps the site theme even when no page of the site was visited before.
 * Failing to precache an asset never fails the install: the page has its own fallback styles.
 */
async function precacheOffline() {
  const cache = await caches.open(OFFLINE_CACHE);
  await cache.add(new Request(OFFLINE_URL, { cache: "reload" }));
  try {
    const page = await (await cache.match(OFFLINE_URL)).text();
    const base = attribute(page.match(/<base\b[^>]*>/i)?.[0] || "", "href") || self.registration.scope;
    const styles = (page.match(/<link\b[^>]*>/gi) || [])
      .filter(link => /^stylesheet$/i.test(attribute(link, "rel")))
      .map(link => attribute(link, "href"));
    const seen = new Set();
    await Promise.all(styles.map(href => precacheStyle(cache, href, base, seen, 0)));
  } catch (error) {
    // The offline page is cached: it stays usable with its fallback styles
  }
}

/**
 * Stores one static asset of the site in the cache.
 * @returns the stored response with the URL it was stored under, or nothing
 */
async function precacheAsset(cache, href, base) {
  if (!href) {
    return undefined;
  }
  const url = new URL(href, base);
  if (url.origin !== self.location.origin || !isStaticPath(url.href)) {
    return undefined;
  }
  try {
    // From the HTTP cache when the browser holds it: the site pages already loaded these files
    const response = await fetch(url.href);
    if (!isCacheable(response)) {
      return undefined;
    }
    await cache.put(url.href, response.clone());
    return { url: url.href, response };
  } catch (error) {
    return undefined;
  }
}

/**
 * Stores a stylesheet, then the stylesheets it imports and the woff2 fonts it loads, each URL once.
 */
async function precacheStyle(cache, href, base, seen, depth) {
  const url = href && new URL(href, base).href;
  if (!url || seen.has(url) || depth > MAX_IMPORT_DEPTH) {
    return;
  }
  seen.add(url);
  const stored = await precacheAsset(cache, url, base);
  if (!stored) {
    return;
  }
  const css = await stored.response.text();
  const refs = [...css.matchAll(/url\(\s*["']?([^"')]+?)["']?\s*\)|@import\s+["']([^"']+)["']/g)].map(match => match[1] || match[2]);
  await Promise.all(
    refs.map(ref => {
      if (/\.css(?:[?#]|$)/i.test(ref)) {
        return precacheStyle(cache, ref, url, seen, depth + 1);
      }
      if (/\.woff2(?:[?#]|$)/i.test(ref)) {
        const font = new URL(ref, url).href;
        if (!seen.has(font)) {
          seen.add(font);
          return precacheAsset(cache, font, url);
        }
      }
      return undefined;
    })
  );
}

/**
 * The value of an attribute of an HTML tag, with its entities &amp; decoded.
 */
function attribute(tag, name) {
  const match = tag.match(new RegExp(`\\b${name}\\s*=\\s*(?:"([^"]*)"|'([^']*)'|([^\\s>]+))`, "i"));
  return match ? (match[1] ?? match[2] ?? match[3]).replace(/&amp;/g, "&") : "";
}

/**
 * A page from the network (through navigation preload when the browser offers it),
 * or the offline page when the network fails. HTTP errors (404, 500) are shown as they are.
 */
async function networkOrOffline(event) {
  try {
    const preloaded = await event.preloadResponse;
    return preloaded || (await fetch(event.request));
  } catch (error) {
    const offline = await caches.match(OFFLINE_URL, { cacheName: OFFLINE_CACHE });
    return offline || Response.error();
  }
}

/**
 * An asset from the cache when present, refreshed from the network in the background;
 * from the network otherwise.
 */
async function staleWhileRevalidate(event) {
  const cache = await caches.open(ASSETS_CACHE);
  const cached = await cache.match(event.request);
  const refresh = fetch(event.request)
    .then(response => {
      if (isCacheable(response)) {
        event.waitUntil(
          cache
            .put(event.request, response.clone())
            .then(() => trim(cache))
            .catch(() => undefined)
        );
      }
      return response;
    })
    .catch(async error => {
      if (cached) {
        return cached;
      }
      const offline = await caches.match(event.request, { cacheName: OFFLINE_CACHE });
      if (offline) {
        return offline;
      }
      throw error;
    });
  if (cached) {
    event.waitUntil(refresh.catch(() => undefined));
    return cached;
  }
  return refresh;
}

/**
 * Whether a URL is a static file of the site (themes, styles, scripts, images), never a
 * JSP or a servlet whose content may change for the same URL or depend on the user.
 */
function isStaticPath(url) {
  const pathname = new URL(url).pathname;
  return STATIC_PATHS.some(path => pathname.startsWith(path));
}

/**
 * Whether a response may be stored: a complete response of the site itself that the
 * server did not mark as private or not to be stored.
 */
function isCacheable(response) {
  if (!response || response.status !== 200 || response.type !== "basic") {
    return false;
  }
  const cacheControl = (response.headers.get("Cache-Control") || "").toLowerCase();
  return !cacheControl.includes("no-store") && !cacheControl.includes("private");
}

/**
 * Keeps the assets cache under MAX_ASSETS entries, removing the oldest ones first.
 */
async function trim(cache) {
  const keys = await cache.keys();
  const excess = keys.length - MAX_ASSETS;
  for (let i = 0; i < excess; i++) {
    await cache.delete(keys[i]);
  }
}
