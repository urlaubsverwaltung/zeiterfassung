<script lang="ts">
  import Notification from "./Notification.svelte";
  import { notificationStore, removeNotification } from "./notificationStore";
</script>

{#if $notificationStore.length > 0}
  <div class="fixed bottom-2 right-2">
    <div class="z-50 flex flex-col-reverse gap-4">
      {#each $notificationStore as notification}
        <Notification
          on:close={() => {
            if (notification.onClose) {
              notification.onClose();
            }
            removeNotification(notification);
          }}
          title={notification.title}
          iconHtml={notification.iconHtml}
          bodyHtml={notification.bodyHtml}
          autoClose={notification.autoClose}
        />
      {/each}
    </div>
  </div>
{/if}
