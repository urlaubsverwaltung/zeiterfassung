import { vi } from "vitest";

vi.stubGlobal(
  "IntersectionObserver",
  class IntersectionObserver {
    constructor(private readonly root: Element) {}
    disconnect = vi.fn();
    observe = vi.fn();
    unobserve = vi.fn();
    takeRecords = vi.fn();
  },
);
