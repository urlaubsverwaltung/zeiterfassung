document.addEventListener("click", function (event) {
  const target = event.target as HTMLElement;

  if (
    event.shiftKey ||
    event.metaKey ||
    event.ctrlKey ||
    event.altKey ||
    target.getAttribute("target") === "_blank"
  ) {
    // shiftKey: new window
    // metaKey: new tab (macOS)
    // ctrlKey: new tab (not macOS)
    // altKey: download
    return;
  }

  navlink(target);
  subnavlink(target);
});

document.addEventListener("turbo:before-cache", function () {
  cleanupLoadingClasses();
});

window.addEventListener("pageshow", function (event) {
  if (event.persisted) {
    // bf-cache hit: https://developer.mozilla.org/en-US/docs/Glossary/bfcache
    cleanupLoadingClasses();
  }
});

function cleanupLoadingClasses() {
  removeClasses("navigation-link--loading");
  removeClasses("navigation-sublink--loading");
}

function removeClasses(className: string) {
  const elements = document.querySelectorAll(`.${className}`);
  for (const element of elements) {
    element.classList.remove(className);
  }
}

function navlink(element: HTMLElement) {
  const link = element.closest(".navigation-link");
  if (link) {
    // element is replaced on response and loading class removed
    link.classList.add("navigation-link--loading");
  }
}

function subnavlink(element: HTMLElement) {
  const link = element.closest(".navigation-sublink");
  if (link) {
    // element is replaced on response and loading class removed
    link.classList.add("navigation-sublink--loading");
  }
}
