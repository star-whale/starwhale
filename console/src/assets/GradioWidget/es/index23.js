import { S as SvelteComponent, i as init, s as safe_not_equal, w as svg_element, b as attr, f as insert, g as append, x as noop, n as detach, e as element, a as space, d as toggle_class, D as group_outros, k as transition_out, E as check_outros, j as transition_in, t as text, h as set_data, B as empty, c as create_component, m as mount_component, o as destroy_component, C as destroy_each, l as listen, ad as add_render_callback, am as create_in_transition, ak as fade, an as create_out_transition, J as onDestroy, P as Block, Q as component_subscribe, X, F as createEventDispatcher, R as assign, T as StatusTracker, U as get_spread_update, V as get_spread_object } from "./main.js";
import { B as BlockLabel } from "./BlockLabel.js";
function create_fragment$3(ctx) {
  let svg;
  let path;
  return {
    c() {
      svg = svg_element("svg");
      path = svg_element("path");
      attr(path, "fill", "currentColor");
      attr(path, "d", "M5 3h2v2H5v5a2 2 0 0 1-2 2a2 2 0 0 1 2 2v5h2v2H5c-1.07-.27-2-.9-2-2v-4a2 2 0 0 0-2-2H0v-2h1a2 2 0 0 0 2-2V5a2 2 0 0 1 2-2m14 0a2 2 0 0 1 2 2v4a2 2 0 0 0 2 2h1v2h-1a2 2 0 0 0-2 2v4a2 2 0 0 1-2 2h-2v-2h2v-5a2 2 0 0 1 2-2a2 2 0 0 1-2-2V5h-2V3h2m-7 12a1 1 0 0 1 1 1a1 1 0 0 1-1 1a1 1 0 0 1-1-1a1 1 0 0 1 1-1m-4 0a1 1 0 0 1 1 1a1 1 0 0 1-1 1a1 1 0 0 1-1-1a1 1 0 0 1 1-1m8 0a1 1 0 0 1 1 1a1 1 0 0 1-1 1a1 1 0 0 1-1-1a1 1 0 0 1 1-1Z");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "xmlns:xlink", "http://www.w3.org/1999/xlink");
      attr(svg, "aria-hidden", "true");
      attr(svg, "role", "img");
      attr(svg, "class", "iconify iconify--mdi");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "preserveAspectRatio", "xMidYMid meet");
      attr(svg, "viewBox", "0 0 24 24");
    },
    m(target, anchor) {
      insert(target, svg, anchor);
      append(svg, path);
    },
    p: noop,
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(svg);
    }
  };
}
class JSON$1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$3, safe_not_equal, {});
  }
}
function get_each_context_1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[5] = list[i];
  child_ctx[7] = i;
  return child_ctx;
}
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[5] = list[i];
  child_ctx[7] = i;
  return child_ctx;
}
function create_else_block_2(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[1]);
      attr(div, "class", "json-item inline");
      attr(div, "item-type", "other");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, dirty) {
      if (dirty & 2)
        set_data(t, ctx2[1]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_if_block_9(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[1]);
      attr(div, "class", "json-item inline text-blue-500");
      attr(div, "item-type", "number");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, dirty) {
      if (dirty & 2)
        set_data(t, ctx2[1]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_if_block_8(ctx) {
  let div;
  let t_value = ctx[1].toLocaleString() + "";
  let t;
  return {
    c() {
      div = element("div");
      t = text(t_value);
      attr(div, "class", "json-item inline text-red-500");
      attr(div, "item-type", "boolean");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, dirty) {
      if (dirty & 2 && t_value !== (t_value = ctx2[1].toLocaleString() + ""))
        set_data(t, t_value);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_if_block_7(ctx) {
  let div;
  let t0;
  let t1;
  let t2;
  return {
    c() {
      div = element("div");
      t0 = text('"');
      t1 = text(ctx[1]);
      t2 = text('"');
      attr(div, "class", "json-item inline text-green-500");
      attr(div, "item-type", "string");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t0);
      append(div, t1);
      append(div, t2);
    },
    p(ctx2, dirty) {
      if (dirty & 2)
        set_data(t1, ctx2[1]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_if_block_6(ctx) {
  let div;
  return {
    c() {
      div = element("div");
      div.textContent = "null";
      attr(div, "class", "json-item inline text-gray-500 dark:text-gray-400");
      attr(div, "item-type", "null");
    },
    m(target, anchor) {
      insert(target, div, anchor);
    },
    p: noop,
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_if_block_3(ctx) {
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  const if_block_creators = [create_if_block_4, create_else_block_1];
  const if_blocks = [];
  function select_block_type_2(ctx2, dirty) {
    if (ctx2[0])
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type_2(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type_2(ctx2);
      if (current_block_type_index === previous_block_index) {
        if_blocks[current_block_type_index].p(ctx2, dirty);
      } else {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        } else {
          if_block.p(ctx2, dirty);
        }
        transition_in(if_block, 1);
        if_block.m(if_block_anchor.parentNode, if_block_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block$2(ctx) {
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  const if_block_creators = [create_if_block_1$1, create_else_block$1];
  const if_blocks = [];
  function select_block_type_1(ctx2, dirty) {
    if (ctx2[0])
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type_1(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type_1(ctx2);
      if (current_block_type_index === previous_block_index) {
        if_blocks[current_block_type_index].p(ctx2, dirty);
      } else {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        } else {
          if_block.p(ctx2, dirty);
        }
        transition_in(if_block, 1);
        if_block.m(if_block_anchor.parentNode, if_block_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_else_block_1(ctx) {
  let t0;
  let div;
  let t1;
  let current;
  let each_value_1 = Object.entries(ctx[1]);
  let each_blocks = [];
  for (let i = 0; i < each_value_1.length; i += 1) {
    each_blocks[i] = create_each_block_1(get_each_context_1(ctx, each_value_1, i));
  }
  const out = (i) => transition_out(each_blocks[i], 1, 1, () => {
    each_blocks[i] = null;
  });
  return {
    c() {
      t0 = text("{\n			");
      div = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      t1 = text("\n			}");
      attr(div, "class", "json-children pl-4");
    },
    m(target, anchor) {
      insert(target, t0, anchor);
      insert(target, div, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
      insert(target, t1, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (dirty & 6) {
        each_value_1 = Object.entries(ctx2[1]);
        let i;
        for (i = 0; i < each_value_1.length; i += 1) {
          const child_ctx = get_each_context_1(ctx2, each_value_1, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
            transition_in(each_blocks[i], 1);
          } else {
            each_blocks[i] = create_each_block_1(child_ctx);
            each_blocks[i].c();
            transition_in(each_blocks[i], 1);
            each_blocks[i].m(div, null);
          }
        }
        group_outros();
        for (i = each_value_1.length; i < each_blocks.length; i += 1) {
          out(i);
        }
        check_outros();
      }
    },
    i(local) {
      if (current)
        return;
      for (let i = 0; i < each_value_1.length; i += 1) {
        transition_in(each_blocks[i]);
      }
      current = true;
    },
    o(local) {
      each_blocks = each_blocks.filter(Boolean);
      for (let i = 0; i < each_blocks.length; i += 1) {
        transition_out(each_blocks[i]);
      }
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(t0);
      if (detaching)
        detach(div);
      destroy_each(each_blocks, detaching);
      if (detaching)
        detach(t1);
    }
  };
}
function create_if_block_4(ctx) {
  let button;
  let t0;
  let t1_value = Object.keys(ctx[1]).length + "";
  let t1;
  let t2;
  let mounted;
  let dispose;
  return {
    c() {
      button = element("button");
      t0 = text("{+");
      t1 = text(t1_value);
      t2 = text(" items}");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, t0);
      append(button, t1);
      append(button, t2);
      if (!mounted) {
        dispose = listen(button, "click", ctx[4]);
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 2 && t1_value !== (t1_value = Object.keys(ctx2[1]).length + ""))
        set_data(t1, t1_value);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(button);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block_5(ctx) {
  let t;
  return {
    c() {
      t = text(",");
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_each_block_1(ctx) {
  let div;
  let t0_value = ctx[5][0] + "";
  let t0;
  let t1;
  let jsonnode;
  let show_if = ctx[7] !== Object.keys(ctx[1]).length - 1;
  let t2;
  let current;
  jsonnode = new JSONNode({
    props: {
      value: ctx[5][1],
      depth: ctx[2] + 1,
      key: ctx[7]
    }
  });
  let if_block = show_if && create_if_block_5();
  return {
    c() {
      div = element("div");
      t0 = text(t0_value);
      t1 = text(": ");
      create_component(jsonnode.$$.fragment);
      if (if_block)
        if_block.c();
      t2 = space();
      attr(div, "class", "json-item");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t0);
      append(div, t1);
      mount_component(jsonnode, div, null);
      if (if_block)
        if_block.m(div, null);
      append(div, t2);
      current = true;
    },
    p(ctx2, dirty) {
      if ((!current || dirty & 2) && t0_value !== (t0_value = ctx2[5][0] + ""))
        set_data(t0, t0_value);
      const jsonnode_changes = {};
      if (dirty & 2)
        jsonnode_changes.value = ctx2[5][1];
      if (dirty & 4)
        jsonnode_changes.depth = ctx2[2] + 1;
      jsonnode.$set(jsonnode_changes);
      if (dirty & 2)
        show_if = ctx2[7] !== Object.keys(ctx2[1]).length - 1;
      if (show_if) {
        if (if_block)
          ;
        else {
          if_block = create_if_block_5();
          if_block.c();
          if_block.m(div, t2);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(jsonnode.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(jsonnode.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(jsonnode);
      if (if_block)
        if_block.d();
    }
  };
}
function create_else_block$1(ctx) {
  let t0;
  let div;
  let t1;
  let current;
  let each_value = ctx[1];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  const out = (i) => transition_out(each_blocks[i], 1, 1, () => {
    each_blocks[i] = null;
  });
  return {
    c() {
      t0 = text("[\n			");
      div = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      t1 = text("\n			]");
      attr(div, "class", "json-children pl-4");
    },
    m(target, anchor) {
      insert(target, t0, anchor);
      insert(target, div, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
      insert(target, t1, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (dirty & 6) {
        each_value = ctx2[1];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
            transition_in(each_blocks[i], 1);
          } else {
            each_blocks[i] = create_each_block(child_ctx);
            each_blocks[i].c();
            transition_in(each_blocks[i], 1);
            each_blocks[i].m(div, null);
          }
        }
        group_outros();
        for (i = each_value.length; i < each_blocks.length; i += 1) {
          out(i);
        }
        check_outros();
      }
    },
    i(local) {
      if (current)
        return;
      for (let i = 0; i < each_value.length; i += 1) {
        transition_in(each_blocks[i]);
      }
      current = true;
    },
    o(local) {
      each_blocks = each_blocks.filter(Boolean);
      for (let i = 0; i < each_blocks.length; i += 1) {
        transition_out(each_blocks[i]);
      }
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(t0);
      if (detaching)
        detach(div);
      destroy_each(each_blocks, detaching);
      if (detaching)
        detach(t1);
    }
  };
}
function create_if_block_1$1(ctx) {
  let button;
  let span;
  let t0;
  let t1_value = ctx[1].length + "";
  let t1;
  let t2;
  let mounted;
  let dispose;
  return {
    c() {
      button = element("button");
      span = element("span");
      t0 = text("expand ");
      t1 = text(t1_value);
      t2 = text(" children");
      attr(span, "class", "bg-gray-50 hover:bg-gray-100 px-1 border rounded text-gray-700 dark:hover:bg-gray-800");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, span);
      append(span, t0);
      append(span, t1);
      append(span, t2);
      if (!mounted) {
        dispose = listen(button, "click", ctx[3]);
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 2 && t1_value !== (t1_value = ctx2[1].length + ""))
        set_data(t1, t1_value);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(button);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block_2(ctx) {
  let t;
  return {
    c() {
      t = text(",");
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_each_block(ctx) {
  let div;
  let t0;
  let t1;
  let jsonnode;
  let t2;
  let t3;
  let current;
  jsonnode = new JSONNode({
    props: {
      value: ctx[5],
      depth: ctx[2] + 1
    }
  });
  let if_block = ctx[7] !== ctx[1].length - 1 && create_if_block_2();
  return {
    c() {
      div = element("div");
      t0 = text(ctx[7]);
      t1 = text(": ");
      create_component(jsonnode.$$.fragment);
      t2 = space();
      if (if_block)
        if_block.c();
      t3 = space();
      attr(div, "class", "json-item");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t0);
      append(div, t1);
      mount_component(jsonnode, div, null);
      append(div, t2);
      if (if_block)
        if_block.m(div, null);
      append(div, t3);
      current = true;
    },
    p(ctx2, dirty) {
      const jsonnode_changes = {};
      if (dirty & 2)
        jsonnode_changes.value = ctx2[5];
      if (dirty & 4)
        jsonnode_changes.depth = ctx2[2] + 1;
      jsonnode.$set(jsonnode_changes);
      if (ctx2[7] !== ctx2[1].length - 1) {
        if (if_block)
          ;
        else {
          if_block = create_if_block_2();
          if_block.c();
          if_block.m(div, t3);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(jsonnode.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(jsonnode.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(jsonnode);
      if (if_block)
        if_block.d();
    }
  };
}
function create_fragment$2(ctx) {
  let span;
  let t;
  let div;
  let current_block_type_index;
  let if_block;
  let current;
  const if_block_creators = [
    create_if_block$2,
    create_if_block_3,
    create_if_block_6,
    create_if_block_7,
    create_if_block_8,
    create_if_block_9,
    create_else_block_2
  ];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[1] instanceof Array)
      return 0;
    if (ctx2[1] instanceof Object)
      return 1;
    if (ctx2[1] === null)
      return 2;
    if (typeof ctx2[1] === "string")
      return 3;
    if (typeof ctx2[1] === "boolean")
      return 4;
    if (typeof ctx2[1] === "number")
      return 5;
    return 6;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      span = element("span");
      t = space();
      div = element("div");
      if_block.c();
      attr(span, "class", "inline-block h-0 w-0");
      toggle_class(span, "mt-10", ctx[2] === 0);
      attr(div, "class", "json-node inline text-sm font-mono leading-tight dark:text-slate-200");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      insert(target, t, anchor);
      insert(target, div, anchor);
      if_blocks[current_block_type_index].m(div, null);
      current = true;
    },
    p(ctx2, [dirty]) {
      if (dirty & 4) {
        toggle_class(span, "mt-10", ctx2[2] === 0);
      }
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type(ctx2);
      if (current_block_type_index === previous_block_index) {
        if_blocks[current_block_type_index].p(ctx2, dirty);
      } else {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        } else {
          if_block.p(ctx2, dirty);
        }
        transition_in(if_block, 1);
        if_block.m(div, null);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(span);
      if (detaching)
        detach(t);
      if (detaching)
        detach(div);
      if_blocks[current_block_type_index].d();
    }
  };
}
function instance$2($$self, $$props, $$invalidate) {
  let { value } = $$props;
  let { depth } = $$props;
  let { collapsed = depth > 4 } = $$props;
  const click_handler = () => {
    $$invalidate(0, collapsed = false);
  };
  const click_handler_1 = () => {
    $$invalidate(0, collapsed = false);
  };
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(1, value = $$props2.value);
    if ("depth" in $$props2)
      $$invalidate(2, depth = $$props2.depth);
    if ("collapsed" in $$props2)
      $$invalidate(0, collapsed = $$props2.collapsed);
  };
  return [collapsed, value, depth, click_handler, click_handler_1];
}
class JSONNode extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$2, create_fragment$2, safe_not_equal, { value: 1, depth: 2, collapsed: 0 });
  }
}
function create_if_block$1(ctx) {
  let span;
  let span_intro;
  let span_outro;
  let current;
  return {
    c() {
      span = element("span");
      span.textContent = "COPIED";
      attr(span, "class", "font-bold dark:text-green-400 text-green-600 py-1 px-2 absolute block w-full text-left bg-white dark:bg-gray-900");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      current = true;
    },
    i(local) {
      if (current)
        return;
      add_render_callback(() => {
        if (span_outro)
          span_outro.end(1);
        span_intro = create_in_transition(span, fade, { duration: 100 });
        span_intro.start();
      });
      current = true;
    },
    o(local) {
      if (span_intro)
        span_intro.invalidate();
      span_outro = create_out_transition(span, fade, { duration: 350 });
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(span);
      if (detaching && span_outro)
        span_outro.end();
    }
  };
}
function create_fragment$1(ctx) {
  let button;
  let span;
  let t0;
  let t1;
  let t2;
  let jsonnode;
  let current;
  let mounted;
  let dispose;
  let if_block = ctx[2] && create_if_block$1();
  jsonnode = new JSONNode({
    props: { value: ctx[0], depth: 0 }
  });
  return {
    c() {
      button = element("button");
      span = element("span");
      t0 = text(ctx[1]);
      t1 = space();
      if (if_block)
        if_block.c();
      t2 = space();
      create_component(jsonnode.$$.fragment);
      attr(span, "class", "py-1 px-2");
      attr(button, "class", "transition-color overflow-hidden font-sans absolute right-0 top-0 rounded-bl-lg shadow-sm text-xs text-gray-500 flex items-center bg-white z-20 border-l border-b border-gray-100 dark:text-slate-200");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, span);
      append(span, t0);
      append(button, t1);
      if (if_block)
        if_block.m(button, null);
      insert(target, t2, anchor);
      mount_component(jsonnode, target, anchor);
      current = true;
      if (!mounted) {
        dispose = listen(button, "click", ctx[3]);
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (!current || dirty & 2)
        set_data(t0, ctx2[1]);
      if (ctx2[2]) {
        if (if_block) {
          if (dirty & 4) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block$1();
          if_block.c();
          transition_in(if_block, 1);
          if_block.m(button, null);
        }
      } else if (if_block) {
        group_outros();
        transition_out(if_block, 1, 1, () => {
          if_block = null;
        });
        check_outros();
      }
      const jsonnode_changes = {};
      if (dirty & 1)
        jsonnode_changes.value = ctx2[0];
      jsonnode.$set(jsonnode_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block);
      transition_in(jsonnode.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      transition_out(jsonnode.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(button);
      if (if_block)
        if_block.d();
      if (detaching)
        detach(t2);
      destroy_component(jsonnode, detaching);
      mounted = false;
      dispose();
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { value = {} } = $$props;
  let { copy_to_clipboard = "copy to clipboard" } = $$props;
  let copied = false;
  let timer;
  function copy_feedback() {
    $$invalidate(2, copied = true);
    if (timer)
      clearTimeout(timer);
    timer = setTimeout(() => {
      $$invalidate(2, copied = false);
    }, 1e3);
  }
  async function handle_copy() {
    if ("clipboard" in navigator) {
      await navigator.clipboard.writeText(JSON.stringify(value, null, 2));
      copy_feedback();
    }
  }
  onDestroy(() => {
    if (timer)
      clearTimeout(timer);
  });
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("copy_to_clipboard" in $$props2)
      $$invalidate(1, copy_to_clipboard = $$props2.copy_to_clipboard);
  };
  return [value, copy_to_clipboard, copied, handle_copy];
}
class JSON_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { value: 0, copy_to_clipboard: 1 });
  }
}
function create_if_block_1(ctx) {
  let blocklabel;
  let current;
  blocklabel = new BlockLabel({
    props: {
      Icon: JSON$1,
      label: ctx[4],
      disable: typeof ctx[5].container === "boolean" && !ctx[5].container
    }
  });
  return {
    c() {
      create_component(blocklabel.$$.fragment);
    },
    m(target, anchor) {
      mount_component(blocklabel, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const blocklabel_changes = {};
      if (dirty & 16)
        blocklabel_changes.label = ctx2[4];
      if (dirty & 32)
        blocklabel_changes.disable = typeof ctx2[5].container === "boolean" && !ctx2[5].container;
      blocklabel.$set(blocklabel_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(blocklabel.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocklabel.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(blocklabel, detaching);
    }
  };
}
function create_else_block(ctx) {
  let div1;
  let div0;
  let jsonicon;
  let current;
  jsonicon = new JSON$1({});
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      create_component(jsonicon.$$.fragment);
      attr(div0, "class", "h-7 dark:text-white opacity-50");
      attr(div1, "class", "h-full min-h-[6rem] flex justify-center items-center");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      mount_component(jsonicon, div0, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(jsonicon.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(jsonicon.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(jsonicon);
    }
  };
}
function create_if_block(ctx) {
  let json;
  let current;
  json = new JSON_1({
    props: {
      value: ctx[2],
      copy_to_clipboard: ctx[6]("interface.copy_to_clipboard")
    }
  });
  return {
    c() {
      create_component(json.$$.fragment);
    },
    m(target, anchor) {
      mount_component(json, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const json_changes = {};
      if (dirty & 4)
        json_changes.value = ctx2[2];
      if (dirty & 64)
        json_changes.copy_to_clipboard = ctx2[6]("interface.copy_to_clipboard");
      json.$set(json_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(json.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(json.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(json, detaching);
    }
  };
}
function create_default_slot(ctx) {
  let t0;
  let statustracker;
  let t1;
  let current_block_type_index;
  let if_block1;
  let if_block1_anchor;
  let current;
  let if_block0 = ctx[4] && create_if_block_1(ctx);
  const statustracker_spread_levels = [ctx[3]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  const if_block_creators = [create_if_block, create_else_block];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[2] && ctx2[2] !== '""')
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block1 = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      if (if_block0)
        if_block0.c();
      t0 = space();
      create_component(statustracker.$$.fragment);
      t1 = space();
      if_block1.c();
      if_block1_anchor = empty();
    },
    m(target, anchor) {
      if (if_block0)
        if_block0.m(target, anchor);
      insert(target, t0, anchor);
      mount_component(statustracker, target, anchor);
      insert(target, t1, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block1_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[4]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
          if (dirty & 16) {
            transition_in(if_block0, 1);
          }
        } else {
          if_block0 = create_if_block_1(ctx2);
          if_block0.c();
          transition_in(if_block0, 1);
          if_block0.m(t0.parentNode, t0);
        }
      } else if (if_block0) {
        group_outros();
        transition_out(if_block0, 1, 1, () => {
          if_block0 = null;
        });
        check_outros();
      }
      const statustracker_changes = dirty & 8 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[3])]) : {};
      statustracker.$set(statustracker_changes);
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type(ctx2);
      if (current_block_type_index === previous_block_index) {
        if_blocks[current_block_type_index].p(ctx2, dirty);
      } else {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block1 = if_blocks[current_block_type_index];
        if (!if_block1) {
          if_block1 = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block1.c();
        } else {
          if_block1.p(ctx2, dirty);
        }
        transition_in(if_block1, 1);
        if_block1.m(if_block1_anchor.parentNode, if_block1_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block0);
      transition_in(statustracker.$$.fragment, local);
      transition_in(if_block1);
      current = true;
    },
    o(local) {
      transition_out(if_block0);
      transition_out(statustracker.$$.fragment, local);
      transition_out(if_block1);
      current = false;
    },
    d(detaching) {
      if (if_block0)
        if_block0.d(detaching);
      if (detaching)
        detach(t0);
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t1);
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block1_anchor);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      visible: ctx[1],
      test_id: "json",
      elem_id: ctx[0],
      disable: typeof ctx[5].container === "boolean" && !ctx[5].container,
      $$slots: { default: [create_default_slot] },
      $$scope: { ctx }
    }
  });
  return {
    c() {
      create_component(block.$$.fragment);
    },
    m(target, anchor) {
      mount_component(block, target, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const block_changes = {};
      if (dirty & 2)
        block_changes.visible = ctx2[1];
      if (dirty & 1)
        block_changes.elem_id = ctx2[0];
      if (dirty & 32)
        block_changes.disable = typeof ctx2[5].container === "boolean" && !ctx2[5].container;
      if (dirty & 380) {
        block_changes.$$scope = { dirty, ctx: ctx2 };
      }
      block.$set(block_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(block.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(block.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(block, detaching);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let $_;
  component_subscribe($$self, X, ($$value) => $$invalidate(6, $_ = $$value));
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value } = $$props;
  let { loading_status } = $$props;
  let { label } = $$props;
  let { style = {} } = $$props;
  const dispatch = createEventDispatcher();
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(1, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(2, value = $$props2.value);
    if ("loading_status" in $$props2)
      $$invalidate(3, loading_status = $$props2.loading_status);
    if ("label" in $$props2)
      $$invalidate(4, label = $$props2.label);
    if ("style" in $$props2)
      $$invalidate(5, style = $$props2.style);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 4) {
      dispatch("change");
    }
  };
  return [elem_id, visible, value, loading_status, label, style, $_];
}
class Json extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 0,
      visible: 1,
      value: 2,
      loading_status: 3,
      label: 4,
      style: 5
    });
  }
}
var Json$1 = Json;
const modes = ["static"];
const document = (config) => ({
  type: "Object | Array",
  description: "JSON object"
});
export { Json$1 as Component, document, modes };
//# sourceMappingURL=index23.js.map
