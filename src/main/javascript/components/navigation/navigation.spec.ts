import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { patchJson } from "../../http";

// the module attaches a document-level click listener on import; the tooltip module
// is stubbed so it doesn't create real popover elements / global listeners during the test
vi.mock("../../http", () => ({
  patchJson: vi.fn(() => Promise.resolve({ ok: true })),
}));
vi.mock("../tooltip/nav-tooltip", () => ({
  setup: vi.fn(),
  prepareTooltip: vi.fn(),
  disposeTooltip: vi.fn(),
}));

// importing registers the click listener exactly once for the whole test file
await import("./navigation");

const SETTINGS_URL = "/api/users/me/settings";

describe("navigation collapse", () => {
  beforeEach(() => {
    delete document.documentElement.dataset.navCollapsed;
    document.body.innerHTML = `
      <button id="nav-toggle" class="nav-collapse-btn navigation-link" aria-label="toggle" aria-expanded="true">
        <span class="nav-link-text">toggle</span>
      </button>
    `;
    vi.mocked(patchJson).mockClear();
  });

  afterEach(() => {
    delete document.documentElement.dataset.navCollapsed;
  });

  test("clicking the toggle collapses the navigation and persists it", () => {
    document.querySelector<HTMLButtonElement>("#nav-toggle")!.click();

    expect(document.documentElement.dataset.navCollapsed).toBeDefined();
    expect(
      document.querySelector("#nav-toggle")!.getAttribute("aria-expanded"),
    ).toBe("false");
    expect(patchJson).toHaveBeenCalledWith(SETTINGS_URL, {
      navigationCollapsed: true,
    });
  });

  test("clicking the toggle while collapsed expands the navigation and persists it", () => {
    document.documentElement.dataset.navCollapsed = "";

    document.querySelector<HTMLButtonElement>("#nav-toggle")!.click();

    expect(document.documentElement.dataset.navCollapsed).toBeUndefined();
    expect(
      document.querySelector("#nav-toggle")!.getAttribute("aria-expanded"),
    ).toBe("true");
    expect(patchJson).toHaveBeenCalledWith(SETTINGS_URL, {
      navigationCollapsed: false,
    });
  });
});
