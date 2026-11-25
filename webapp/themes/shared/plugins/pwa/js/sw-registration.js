/*
 * Lutece PWA plugin: registers the service worker, offers the installation and announces
 * the updates.
 *
 * - Install: elements marked data-pwa-install stay hidden until the browser offers the
 *   installation (beforeinstallprompt, Chromium browsers); a click on data-pwa-install-button
 *   opens the browser prompt. On iOS, where no prompt exists, data-pwa-ios-hint is shown
 *   instead. Nothing is shown once the site runs as an installed app.
 * - Update: when a new service worker is waiting, a banner offers to reload; the new worker
 *   takes over only on that click. Its texts come from the data-update-message and
 *   data-update-reload attributes of this script tag.
 */
(() => {
  const script = document.currentScript;
  const texts = {
    message: (script && script.dataset.updateMessage) || "A new version of the site is available.",
    reload: (script && script.dataset.updateReload) || "Reload"
  };
  const standalone =
    window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone === true;

  const show = selector => document.querySelectorAll(selector).forEach(el => (el.hidden = false));
  const hide = selector => document.querySelectorAll(selector).forEach(el => (el.hidden = true));

  let deferredPrompt = null;

  window.addEventListener("beforeinstallprompt", event => {
    event.preventDefault();
    deferredPrompt = event;
    if (!standalone) {
      show("[data-pwa-install]");
    }
  });

  window.addEventListener("appinstalled", () => {
    deferredPrompt = null;
    hide("[data-pwa-install]");
    hide("[data-pwa-ios-hint]");
  });

  document.addEventListener("click", async event => {
    const button = event.target.closest("[data-pwa-install-button]");
    if (!button || !deferredPrompt) {
      return;
    }
    const prompt = deferredPrompt;
    deferredPrompt = null;
    hide("[data-pwa-install]");
    await prompt.prompt();
    await prompt.userChoice;
  });

  const isIos = /iphone|ipad|ipod/i.test(navigator.userAgent) ||
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);

  const onReady = callback =>
    document.readyState === "loading" ? document.addEventListener("DOMContentLoaded", callback) : callback();

  onReady(() => {
    if (isIos && !standalone) {
      show("[data-pwa-ios-hint]");
    }
  });

  if (!("serviceWorker" in navigator)) {
    return;
  }

  const showUpdateBanner = worker => {
    if (document.querySelector(".pwa-update-banner")) {
      return;
    }
    const banner = document.createElement("div");
    banner.className = "pwa-update-banner";
    banner.setAttribute("role", "status");
    const message = document.createElement("span");
    message.textContent = texts.message;
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = texts.reload;
    button.addEventListener("click", () => {
      button.disabled = true;
      worker.postMessage({ type: "SKIP_WAITING" });
    });
    banner.append(message, button);
    document.body.append(banner);
  };

  const hadController = Boolean(navigator.serviceWorker.controller);
  let reloading = false;
  navigator.serviceWorker.addEventListener("controllerchange", () => {
    if (hadController && !reloading) {
      reloading = true;
      window.location.reload();
    }
  });

  window.addEventListener("load", () => {
    navigator.serviceWorker
      .register("./service-worker.js", { scope: "./" })
      .then(registration => {
        if (registration.waiting && navigator.serviceWorker.controller) {
          showUpdateBanner(registration.waiting);
        }
        registration.addEventListener("updatefound", () => {
          const worker = registration.installing;
          if (!worker) {
            return;
          }
          worker.addEventListener("statechange", () => {
            if (worker.state === "installed" && navigator.serviceWorker.controller) {
              showUpdateBanner(worker);
            }
          });
        });
      })
      .catch(error => console.error("[pwa] Service worker registration failed:", error));
  });
})();
