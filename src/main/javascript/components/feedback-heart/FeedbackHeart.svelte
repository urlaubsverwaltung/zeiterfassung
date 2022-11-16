<script lang="ts">
  import HeartIcon from "./HeartIcon.svelte";
  import FloatingHeart from "./FloatingHeart.svelte";

  export let showInitialHeart = false;

  let movingHeartsCount = showInitialHeart ? 1 : 0;

  function floatingHeart() {
    movingHeartsCount = movingHeartsCount + 1;
  }

  function handleMouseClick() {
    floatingHeart();
  }
</script>

{#each { length: movingHeartsCount } as _}
  <FloatingHeart />
{/each}

<div class="fixed left-0 bottom-0 p-2">
  <div on:click={handleMouseClick} class="cursor-pointer">
    <HeartIcon class="w-8 h-8" />
  </div>
</div>
