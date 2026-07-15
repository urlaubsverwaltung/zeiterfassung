import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { doGet, patchJson, post } from "./http";

describe("http module", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  describe("post", () => {
    it("sends POST request with correct method and body", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: () => Promise.resolve({ id: 1 }),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      const result = await post("/api/users", JSON.stringify({ name: "John" }));

      expect(fetch).toHaveBeenCalledWith(
        "/api/users",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ name: "John" }),
          credentials: "same-origin",
        }),
      );
      expect(result.ok).toBe(true);
    });

    it("includes CSRF header", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers(),
        text: () => Promise.resolve(""),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      await post("/api/users", "{}");

      expect(fetch).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          headers: expect.objectContaining({
            "X-Requested-With": "ajax",
          }),
        }),
      );
    });

    it("merges custom headers with defaults", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers(),
        text: () => Promise.resolve(""),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      await post("/api/users", "{}", {
        headers: { Authorization: "Bearer token" },
      });

      expect(fetch).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          headers: expect.objectContaining({
            "X-Requested-With": "ajax",
            Authorization: "Bearer token",
          }),
        }),
      );
    });

    it("parses JSON response", async () => {
      const responseData = { id: 1, name: "John" };
      const mockResponse = {
        ok: true,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: () => Promise.resolve(responseData),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      const result = await post("/api/users", "{}");

      expect(result.data).toEqual(responseData);
    });
  });

  describe("patchJson", () => {
    it("sends PATCH request with JSON method and stringified body", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: () => Promise.resolve({}),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      const payload = { status: "active" };
      await patchJson("/api/users/1", payload);

      expect(fetch).toHaveBeenCalledWith(
        "/api/users/1",
        expect.objectContaining({
          method: "PATCH",
          body: JSON.stringify(payload),
        }),
      );
    });

    it("sets Content-Type to application/json", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers(),
        text: () => Promise.resolve(""),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      await patchJson("/api/users/1", { status: "active" });

      expect(fetch).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          headers: expect.objectContaining({
            "Content-Type": "application/json",
          }),
        }),
      );
    });

    it("merges headers with Content-Type", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers(),
        text: () => Promise.resolve(""),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      await patchJson(
        "/api/users/1",
        {},
        {
          headers: { Authorization: "Bearer token" },
        },
      );

      expect(fetch).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          headers: expect.objectContaining({
            "Content-Type": "application/json",
            Authorization: "Bearer token",
          }),
        }),
      );
    });

    it("parses JSON response", async () => {
      const responseData = { id: 1, status: "active" };
      const mockResponse = {
        ok: true,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: () => Promise.resolve(responseData),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      const result = await patchJson("/api/users/1", {});

      expect(result.data).toEqual(responseData);
    });
  });

  describe("doGet", () => {
    it("sends GET request", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: () => Promise.resolve([]),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      await doGet("/api/users");

      expect(fetch).toHaveBeenCalledWith(
        "/api/users",
        expect.objectContaining({
          method: "GET",
          credentials: "same-origin",
        }),
      );
    });

    it("includes CSRF header on GET", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers(),
        text: () => Promise.resolve(""),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      await doGet("/api/users");

      expect(fetch).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          headers: expect.objectContaining({
            "X-Requested-With": "ajax",
          }),
        }),
      );
    });

    it("parses JSON response", async () => {
      const responseData = [{ id: 1, name: "John" }];
      const mockResponse = {
        ok: true,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: () => Promise.resolve(responseData),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      const result = await doGet("/api/users");

      expect(result.data).toEqual(responseData);
    });
  });

  describe("response parsing", () => {
    it("parses JSON when Content-Type includes json", async () => {
      const responseData = { message: "success" };
      const mockResponse = {
        ok: true,
        headers: new Headers({
          "Content-Type": "application/json; charset=utf-8",
        }),
        json: () => Promise.resolve(responseData),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      const result = await doGet("/api/data");

      expect(result.data).toEqual(responseData);
    });

    it("parses text when Content-Type is not json", async () => {
      const responseText = "plain text response";
      const mockResponse = {
        ok: true,
        headers: new Headers({ "Content-Type": "text/plain" }),
        text: () => Promise.resolve(responseText),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      const result = await doGet("/api/data");

      expect(result.data).toEqual(responseText);
    });

    it("does not parse response when status is not ok", async () => {
      const mockResponse = {
        ok: false,
        status: 404,
        headers: new Headers({ "Content-Type": "application/json" }),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      const result = await doGet("/api/notfound");

      expect(result.ok).toBe(false);
      expect(result.data).toBeUndefined();
    });

    it("handles JSON parse error gracefully", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: () => Promise.reject(new Error("Invalid JSON")),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      const result = await doGet("/api/data");

      expect(result.ok).toBe(true);
      expect(result.data).toBeUndefined();
    });

    it("handles missing Content-Type header", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers(),
        text: () => Promise.resolve("response"),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      const result = await doGet("/api/data");

      expect(result.data).toEqual("response");
    });
  });

  describe("credentials and origin", () => {
    it("sets credentials to same-origin on all requests", async () => {
      const mockResponse = {
        ok: true,
        headers: new Headers(),
        text: () => Promise.resolve(""),
      } as Response;
      vi.mocked(fetch).mockResolvedValue(mockResponse);

      await post("/api/data", "{}");
      expect(fetch).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ credentials: "same-origin" }),
      );

      await patchJson("/api/data", {});
      expect(fetch).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ credentials: "same-origin" }),
      );

      await doGet("/api/data");
      expect(fetch).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ credentials: "same-origin" }),
      );
    });
  });
});
