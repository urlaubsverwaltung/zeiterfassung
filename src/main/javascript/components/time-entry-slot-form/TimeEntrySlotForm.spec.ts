import { beforeEach, describe, expect, test } from "vitest";
import "./index";

function timeEntryFormHtml({
  date = "2025-02-16",
  start = "",
  end = "",
  duration = "",
  isBreak = false,
}: {
  date?: string;
  start?: string;
  end?: string;
  duration?: string;
  isBreak?: boolean;
} = {}) {
  return `
  <form is="z-time-entry-slot-form" data-testid="time-entry-form">
  <input type="hidden" name="id" value="" />
  <input type="date" name="date" value="${date}" />
  <input type="text" name="start" value="${start}" />
  <input type="text" name="end" value="${end}" />
  <input type="text" name="duration" value="${duration}" />
  <input type="checkbox" name="break" ${isBreak ? "checked" : ""} />
  <div data-error-container></div>
  </form>
  `;
}

beforeEach(() => {
  globalThis.zeiterfassung = globalThis.zeiterfassung || ({} as never);
  globalThis.zeiterfassung.i18n = {
    "time-entry.validation.timespan.overlaps":
    "Der Zeitraum 端berschneidet sich mit einem bestehenden Zeiteintrag.",
  };
});

describe("TimeEntrySlotForm overlap validation", () => {
  test("rejects submit when timespan partially overlaps an existing entry on the same day", () => {
    document.body.innerHTML =
    timeEntryFormHtml({ start: "08:00", end: "12:00" }) +
    timeEntryFormHtml({ start: "11:00", end: "14:00" });

    const forms = document.querySelectorAll("form");
    const submittedForm = forms[1] as HTMLFormElement;

    const event = new Event("submit", { cancelable: true });
    submittedForm.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);
    expect(
      submittedForm.querySelector("[data-error-container]")!.innerHTML,
    ).toContain("端berschneidet");
  });

  test("allows submit when timespan does not overlap any existing entry", () => {
    document.body.innerHTML =
    timeEntryFormHtml({ start: "08:00", end: "12:00" }) +
    timeEntryFormHtml({ start: "13:00", end: "17:00" });

    const forms = document.querySelectorAll("form");
    const submittedForm = forms[1] as HTMLFormElement;

    const event = new Event("submit", { cancelable: true });
    submittedForm.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(false);
  });

  test("allows submit when overlapping entry is on a different day", () => {
    document.body.innerHTML =
    timeEntryFormHtml({ date: "2025-02-15", start: "08:00", end: "12:00" }) +
    timeEntryFormHtml({ date: "2025-02-16", start: "08:00", end: "12:00" });

    const forms = document.querySelectorAll("form");
    const submittedForm = forms[1] as HTMLFormElement;

    const event = new Event("submit", { cancelable: true });
    submittedForm.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(false);
  });

  test("allows a break entry to overlap a work entry", () => {
    document.body.innerHTML =
    timeEntryFormHtml({ start: "08:00", end: "12:00" }) +
    timeEntryFormHtml({ start: "11:00", end: "11:30", isBreak: true });

    const forms = document.querySelectorAll("form");
    const submittedForm = forms[1] as HTMLFormElement;

    const event = new Event("submit", { cancelable: true });
    submittedForm.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(false);
  });
});
