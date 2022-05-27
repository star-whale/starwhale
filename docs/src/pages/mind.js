import React from "react";
import Layout from "@theme/Layout";
import { Graph } from "@antv/x6";
import { useEffect } from "react";

function Mind() {
  useEffect(() => {
    const container = document.getElementById("container");
    const graph = new Graph({
      container: container,
      grid: true,
      width: 1200,
      height: 600,
    });

    const source = graph.addNode({
      //   x: 180,
      //   y: 60,
      width: 100,
      height: 40,
      attrs: {
        body: {
          stroke: "#5F95FF",
          fill: "#EFF4FF",
          strokeWidth: 1,
        },
      },
    });

    const target = graph.addNode({
      x: 320,
      y: 250,
      width: 100,
      height: 40,
      attrs: {
        body: {
          stroke: "#5F95FF",
          fill: "#EFF4FF",
          strokeWidth: 1,
        },
      },
    });

    graph.addEdge({
      source,
      target,
      attrs: {
        line: {
          stroke: "#A2B1C3",
          strokeWidth: 2,
        },
      },
    });
    graph.addEdge({
      source,
      target,
      attrs: {
        line: {
          stroke: "#A2B1C3",
          strokeWidth: 2,
        },
      },
    });
    graph.addEdge({
      source,
      target,
      attrs: {
        line: {
          stroke: "#A2B1C3",
          strokeWidth: 2,
        },
      },
    });
    graph.addEdge({
      source,
      target,
      attrs: {
        line: {
          stroke: "#A2B1C3",
          strokeWidth: 2,
        },
      },
    });
    graph.addEdge({
      source,
      target,
      attrs: {
        line: {
          stroke: "#A2B1C3",
          strokeWidth: 2,
        },
      },
    });
  }, []);

  return (
    <Layout
      title={`Hello from`}
      description="Description will go into a meta tag in <head />"
    >
      <main
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flex: "0 1 auto",
        }}
      >
        <div id="container"></div>
      </main>
    </Layout>
  );
}

export default Mind;
