import * as Turbo from "@hotwired/turbo";

// opt-in to turbo with `data-turbo="true"`
Turbo.session.drive = false;

document.addEventListener("turbo:click", function (event: CustomEvent) {
  const target = event.target as HTMLElement;
  if (target.classList.contains("ajax-loader")) {
    target.classList.add("ajax-loader--loading");
  }
});

document.addEventListener(
  "turbo:fetch-request-error",
  function (event: CustomEvent) {
    // e.g. happens when user does a request after session-timeout

    console.debug(
      "[turbo:fetch-request-error]. reloading current page to restore state.",
      {
        detail: event.detail,
      },
    );

    // GIVEN application session timed out but not the auth server one.
    // THEN user sees the same site again, at the same scroll position.
    // THEN user is confused and clicks the same turbo link again
    // THEN user is happy and keeps on using the app :o)

    window.location.reload();
  },
);
