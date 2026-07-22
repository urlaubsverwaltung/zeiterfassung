import { vi } from "vitest";

vi.stubGlobal(
  "IntersectionObserver",
  class IntersectionObserver {
    disconnect = vi.fn();
    observe = vi.fn();
    unobserve = vi.fn();
    takeRecords = vi.fn();
    constructor(private readonly root: Element) {}
  },
);
