<script lang="ts">
  import type {SankeyExtraProperties, SankeyGraph, SankeyLink, SankeyNode} from "d3-sankey";
  import {sankey, sankeyJustify, sankeyLinkHorizontal} from "d3-sankey";
  import {onMount} from "svelte";
  import {tweened} from 'svelte/motion';
  import {linear} from 'svelte/easing';

  interface Node extends SankeyExtraProperties {
      name: string;
  }
  interface Link extends SankeyExtraProperties {}
  type NodeColor = (node: SankeyNode<Node, Link>) => string;
  type LinkColor = (link: SankeyLink<Node, Link>) => string;
  type Data = SankeyGraph<Node, Link>;

  export let data: Data;
  export let nodeId: (node: SankeyNode<Node, Link>) => number | string;
  export let width = 740;
  export let height = 400;
  export let nodeColor: NodeColor = () => "#a53253";
  export let linkColor: LinkColor = () => "#a53253";

  onMount(() => {
    document.addEventListener("graph-day-selected", handleGraphDaySelected);
    return () => {
      document.removeEventListener("graph-day-selected", handleGraphDaySelected);
    }
  });

  const MARGIN_Y = 25;
  const MARGIN_X = 5;
  const fontSize = 12;

  const sankeyGenerator = sankey()
    .nodeWidth(26)
    .nodePadding(16)
    .nodeId(nodeId)
    .extent([
      [MARGIN_X, MARGIN_Y],
      [width - MARGIN_X, height - MARGIN_Y],
    ])
    .nodeAlign(sankeyJustify);

  const linkGenerator = sankeyLinkHorizontal();

  let graph = sankeyGenerator(data);
  let nodes = graph.nodes as Array<SankeyNode<Node, Link>>;
  let links = graph.links as Array<{ source: Node, target: Node } & SankeyLink<Node, Link>>;

  let targetText = [];

  $: targetText = links.reduce((result, link) => {
    if (!result.some(resultLink => resultLink.target.name === link.target.name)) {
      result.push(link);
    }
    return result
  }, []);

  function handleLinkClick(event) {
    console.log("clicked", event.target);
  }

  function handleLinkMouseenter(event: MouseEvent) {
    animateStrokeOpacityFromTo(event.target as HTMLElement, 0.1, 0.5);
  }

  function handleLinkMouseleave(event: MouseEvent) {
    animateStrokeOpacityFromTo(event.target as HTMLElement, 0.5, 0.1);
  }

  function animateStrokeOpacityFromTo(target: HTMLElement, from: number, to: number) {
    const opacity = tweened(from);
    const unsub = opacity.subscribe((value) => {
      target.setAttribute("stroke-opacity", String(value));
      if (value === to) {
        unsub();
      }
    });
    opacity.set(to, { duration: 200, easing: linear });
  }

  function handleGraphDaySelected(event: CustomEvent) {
    console.log(event.detail);
  }
</script>

<div>
  <svg {width} {height} role="presentation">
    {#each nodes as node}
      <rect
        height={node.y1 - node.y0}
        width={sankeyGenerator.nodeWidth()}
        x={node.x0}
        y={node.y0}
        stroke="black"
        fill={nodeColor(node)}
        fill-opacity="0.8"
        rx={0.9}
      />
      {#if node.sourceLinks.some(link => link.value) || node.targetLinks.some(link => link.value)}
        <text
          x={node.x0 < width / 2 ? node.x1 + 6 : node.x0 - 6}
          y={(node.y1 + node.y0) / 2 - 6}
          dy="0.35rem"
          text-anchor={node.x0 < width / 2 ? "start" : "end"}
          font-size={fontSize}
        >
          {node.name}
        </text>
      {/if}
    {/each}
    {#each links as link}
      <path
        role="presentation"
        on:mouseenter={handleLinkMouseenter}
        on:mouseleave={handleLinkMouseleave}
        on:click={handleLinkClick}
        d={linkGenerator(link)}
        stroke={linkColor(link)}
        fill="none"
        stroke-opacity="0.1"
        stroke-width={link.width}
      />
      {#if link.source.value}
        <text
          x={link.source.x0 < width / 2 ? link.source.x1 + 6 : link.source.x0 - 6}
          y={(link.source.y1 + link.source.y0) / 2 + fontSize}
          dy="0.35rem"
          text-anchor={link.source.x0 < width / 2 ? "start" : "end"}
          font-size={fontSize}
        >
          {link.source.value}
        </text>
      {/if}
    {/each}
    {#each targetText as link}
      {#if link.target.value}
        <text
          x={link.target.x0 < width / 2 ? link.target.x1 + 6 : link.target.x0 - 6}
          y={(link.target.y1 + link.target.y0) / 2 + fontSize}
          dy="0.35rem"
          text-anchor={link.target.x0 < width / 2 ? "start" : "end"}
          font-size={fontSize}
        >
          {link.target.value}
        </text>
      {/if}
    {/each}
  </svg>
</div>
