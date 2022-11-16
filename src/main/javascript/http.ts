export interface ResponseWithData<T> extends Response {
  data?: T;
}

/**
 * This method does a POST request and internally handles app specific security aspects like CSRF.
 *
 * @param {string} url the endpoint for the request
 * @param {BodyInit} data data used as RequestBody
 * @param {RequestInit} [options]
 *              configure the request with meta info. please see [native fetch init parameter](https://developer.mozilla.org/en-US/docs/Web/API/WindowOrWorkerGlobalScope/fetch#Parameters)
 *              for more detail.
 * @returns {Promise<ResponseWithData>}
 *              returns a promise eventually resolve with the original `Response` object enriched
 *              with a `data` attribute which contains the deserialized response data.
 */
export function post<T>(
  url: string,
  data: BodyInit,
  options: RequestInit = {},
): Promise<ResponseWithData<T>> {
  return doFetch(url, {
    ...options,
    method: "post",
    body: data,
  });
}

/**
 * This method does a GET request and internally handles app specific aspects.
 *
 * @param url
 * @param options
 */
export function doGet<T>(
  url: string,
  options: RequestInit = {},
): Promise<ResponseWithData<T>> {
  return doFetch(url, {
    ...options,
    method: "get",
  });
}

// just an internal function to wrap native fetch
// public api of this module should expose function like `post`, `get`, `getJson`, ...
async function doFetch<T>(
  url,
  options: RequestInit = {},
): Promise<ResponseWithData<T>> {
  const fetchOptions: RequestInit = {
    ...options,
    credentials: "same-origin",
    headers: {
      ...options.headers,
      "X-Requested-With": "ajax",
    },
  };

  const response: ResponseWithData<T> = await fetch(url, fetchOptions);

  if (response.ok) {
    if (response.headers.get("Content-Type")?.includes("json")) {
      try {
        response.data = await response.json();
      } catch {
        // ignore error
      }
    } else {
      // currently, we only have json and text
      // no blob, arrayBuffer or formData

      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      response.data = await response.text();
    }
  }

  return response;
}
