<script lang="ts">
</script>

<div></div>

<style>
  div {
    background-color: red;
    position: absolute;
    height: 32px;
    min-height: 16px;
    width: 32px;
    min-width: 16px;
    opacity: 0;
    -webkit-mask-image: url('data:image/svg+xml;charset=UTF-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path fill="currentColor" d="M462.3 62.6C407.5 15.9 326 24.3 275.7 76.2L256 96.5l-19.7-20.3C186.1 24.3 104.5 15.9 49.7 62.6c-62.8 53.6-66.1 149.8-9.9 207.9l193.5 199.8c12.5 12.9 32.8 12.9 45.3 0l193.5-199.8c56.3-58.1 53-154.3-9.8-207.9z"></path></svg>');
    mask-image: url('data:image/svg+xml;charset=UTF-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path fill="currentColor" d="M462.3 62.6C407.5 15.9 326 24.3 275.7 76.2L256 96.5l-19.7-20.3C186.1 24.3 104.5 15.9 49.7 62.6c-62.8 53.6-66.1 149.8-9.9 207.9l193.5 199.8c12.5 12.9 32.8 12.9 45.3 0l193.5-199.8c56.3-58.1 53-154.3-9.8-207.9z"></path></svg>');
    -webkit-mask-repeat: no-repeat;
    mask-repeat: no-repeat;
    -webkit-mask-position: bottom;
    mask-position: bottom;
    animation: floating-heart 10s normal cubic-bezier(0.5, 0.5, 0.5, 0.5);
  }

  @keyframes floating-heart {
    0% {
      opacity: 0;
      bottom: 0%;
      left: 1em;
    }
    10% {
      opacity: 1;
      bottom: 20%;
      left: 1.6em;
      z-index: 999;
    }
    20% {
      bottom: 40%;
      left: 2.4em;
    }
    30% {
      bottom: 60%;
      left: 1.8em;
    }
    40% {
      opacity: 1;
      bottom: 80%;
      left: 2em;
    }
    48% {
      opacity: 0;
      bottom: 100%;
      left: 2.6em;
    }
    100% {
      opacity: 0;
      bottom: 100%;
      left: 2.4em;
    }
  }
</style>
