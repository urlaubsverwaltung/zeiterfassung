<script lang="ts">
  import HeartIcon from "./HeartIcon.svelte";
  import FloatingHeart from "./FloatingHeart.svelte";

  let { showInitialHeart = false } = $props();

  let movingHeartsCount = $state(showInitialHeart ? 1 : 0);

  function floatingHeart() {
    movingHeartsCount = movingHeartsCount + 1;
  }

  function onclick() {
    floatingHeart();
  }
</script>

<!-- eslint-disable-next-line -->
{#each { length: movingHeartsCount } as _}
  <FloatingHeart />
{/each}

<div class="fixed left-0 bottom-0 p-2">
  <div
    {onclick}
    onkeypress={onclick}
    role="button"
    tabindex="0"
    class="cursor-pointer"
  >
    <HeartIcon class="w-8 h-8" />
  </div>
</div>
