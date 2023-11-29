<script lang="ts">
  import Sankey from "../graph/Sankey.svelte"
  import type {SankeyExtraProperties, SankeyGraph, SankeyLink, SankeyNode} from "d3-sankey";
  import {onMount} from "svelte";

  type Node = SankeyExtraProperties & {
    name: string;
    asd?: number;
  };
  type Link = SankeyExtraProperties & {
      //
  };
  type Data = SankeyGraph<Node, Link>;

  const data: Data = {
    nodes: [
      { name: "Montag 20.11.2023", asd: 8 },
      { name: "Dienstag 21.11.2023", asd: 8 },
      { name: "Mittwoch 22.11.2023", asd: 0 },
      { name: "Donnerstag 23.11.2023", asd: 8 },
      { name: "Freitag 24.11.2023", asd: 4 },
      { name: "Soll" },
      { name: "Minusstunden" },
      { name: "Überstunden" },
    ],
    links: [
      { source: "Montag 20.11.2023", target: "Soll", value: 8 },
      { source: "Montag 20.11.2023", target: "Minusstunden", value: 0 },
      { source: "Montag 20.11.2023", target: "Überstunden", value: 0 },
      { source: "Dienstag 21.11.2023", target: "Soll", value: 0 },
      { source: "Dienstag 21.11.2023", target: "Minusstunden", value: 0 },
      { source: "Dienstag 21.11.2023", target: "Überstunden", value: 0 },
      { source: "Mittwoch 22.11.2023", target: "Soll", value: 7 },
      { source: "Mittwoch 22.11.2023", target: "Minusstunden", value: 1 },
      { source: "Mittwoch 22.11.2023", target: "Überstunden", value: 0 },
      { source: "Donnerstag 23.11.2023", target: "Soll", value: 8 },
      { source: "Donnerstag 23.11.2023", target: "Minusstunden", value: 0 },
      { source: "Donnerstag 23.11.2023", target: "Überstunden", value: 2 },
      { source: "Freitag 24.11.2023", target: "Soll", value: 8 },
      { source: "Freitag 24.11.2023", target: "Minusstunden", value: 0 },
      { source: "Freitag 24.11.2023", target: "Überstunden", value: 1 },
    ],
  };

  const nodeId = (node: SankeyNode<Node, Link>) => node.name;

  const nodeColor = (node: SankeyNode<Node, Link>) => node.name === "Überstunden"
      ? "#134e4a"
      : (node.name === "Minusstunden"
          ? "#dcfce7"
          : "#49de80");
    const linkColor = (link: SankeyLink<Node, Link>) => {
        let name = (link.target as Node).name;
        return name === "Überstunden"
            ? "#134e4a"
            : (name === "Minusstunden"
                ? "#7eb796"
                : "#49de80");
    }

    onMount(() => console.log("overtime mount"))
</script>

<Sankey {data} {nodeColor} {linkColor} {nodeId} />
