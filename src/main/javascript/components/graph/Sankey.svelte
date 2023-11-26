<script lang="ts">
    import type {SankeyExtraProperties, SankeyGraph, SankeyLink, SankeyNode} from "d3-sankey"
    import {sankey, sankeyJustify, sankeyLinkHorizontal} from "d3-sankey"

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

  const graph = sankeyGenerator(data);
  const nodes = graph.nodes as Array<SankeyNode<Node, Link>>;
  const links = graph.links as Array<{ source: Node, target: Node } & SankeyLink<Node, Link>>;
</script>

<div>
  <svg {width} {height}>
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
      <text
        x={node.x0 < width / 2 ? node.x1 + 6 : node.x0 - 6}
        y={(node.y1 + node.y0) / 2 - 6}
        dy="0.35rem"
        text-anchor={node.x0 < width / 2 ? "start" : "end"}
        font-size={fontSize}
      >
        {node.name}
      </text>
    {/each}
    {#each links as link}
      <path
        d={linkGenerator(link)}
        stroke={linkColor(link)}
        fill="none"
        stroke-opacity="0.1"
        stroke-width={link.width}
      />
      <text
        x={link.source.x0 < width / 2 ? link.source.x1 + 6 : link.source.x0 - 6}
        y={(link.source.y1 + link.source.y0) / 2 + fontSize}
        dy="0.35rem"
        text-anchor={link.source.x0 < width / 2 ? "start" : "end"}
        font-size={fontSize}
      >
        {link.source.value}
      </text>
    {/each}
  </svg>
</div>
