import { describe, expect, test } from "vitest";
import "./index";

describe("TimeEntryDurationInput", () => {
  test("adds semicolon when two chars has been entered", () => {
    document.body.innerHTML = `<input type="text" is="z-time-entry-duration-input">`;

    const sut = document.querySelector("input");

    sut.value = "01";
    sut.dispatchEvent(new InputEvent("input", { data: "1" }));

    expect(sut.value).toEqual("01:");
  });

  test("does not add semicolon when a meta key has been pressed", () => {
    document.body.innerHTML = `<input type="text" is="z-time-entry-duration-input">`;

    const sut = document.querySelector("input");

    sut.value = "01";
    sut.dispatchEvent(new InputEvent("input", { data: undefined }));

    expect(sut.value).toEqual("01");
  });

  test("prevents multiple semicolon on after another", () => {
    document.body.innerHTML = `<input type="text" is="z-time-entry-duration-input">`;

    const sut = document.querySelector("input");

    sut.value = "01::";
    sut.dispatchEvent(new InputEvent("input", { data: ":" }));

    expect(sut.value).toEqual("01:");
  });

  test("adds semicolon when input has been deleted and updated again", () => {
    document.body.innerHTML = `<input type="text" is="z-time-entry-duration-input">`;

    const sut = document.querySelector("input");

    sut.value = "014";
    sut.dispatchEvent(new InputEvent("input", { data: "4" }));

    expect(sut.value).toEqual("01:4");
  });
});
