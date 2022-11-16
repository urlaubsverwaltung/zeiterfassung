<script lang="ts">
  import { fade } from "svelte/transition";
  import { createEventDispatcher } from "svelte";
  import CloseIcon from "./CloseIcon.svelte";

  const dispatch = createEventDispatcher();

  export let title: string;
  export let iconHtml: string;
  export let bodyHtml: string;
  export let autoClose: boolean;

  let hovered = false;

  let timeoutId: number;
  let delay = 5000;
  let timerStartedStamp: number;

  $: {
    if (hovered && autoClose) {
      pauseAutoCloseTimer();
    } else if (autoClose && !timeoutId) {
      startAutoCloseTimer();
    }
  }

  const dispatchClose = () => dispatch("close");

  function startAutoCloseTimer() {
    timerStartedStamp = Date.now();
    // use at least one second as the new timeout delay
    // to prevent immediate notification closing
    timeoutId = setTimeout(dispatchClose, Math.max(delay, 1000));
  }

  function pauseAutoCloseTimer() {
    delay = Date.now() - timerStartedStamp;
    clearTimeout(timeoutId);
    timeoutId = undefined;
  }

  function handleButtonClose() {
    dispatchClose();
  }
</script>

<div
  class="relative z-40 w-80 max-w-full"
  transition:fade={{ duration: 150 }}
  on:mouseenter={() => (hovered = true)}
  on:mouseleave={() => (hovered = false)}
>
  <div class="bg-white rounded-md border-l-8 border-blue-200 shadow-lg">
    <button
      on:click={handleButtonClose}
      class="absolute top-2 right-2 text-gray-300 p-1 hover:text-gray-500 hover:bg-blue-50 transition-colors rounded-full"
    >
      <CloseIcon class="w-6 h-6" />
      <span class="sr-only">Schließen</span>
    </button>
    <div class="p-4">
      <div class="mb-2 flex items-center">
        <span
          class="rounded-full bg-blue-200 text-white flex items-center justify-center w-9 h-9 p-1.5 mr-4"
        >
          {@html iconHtml}
        </span>
        <span class="font-semibold text-sm">
          {@html title}
        </span>
      </div>
      <div class="ml-13 text-sm">
        {@html bodyHtml}
      </div>
    </div>
  </div>
</div>
