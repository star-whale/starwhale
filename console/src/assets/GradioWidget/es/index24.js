import { S as SvelteComponent, i as init, s as safe_not_equal, w as svg_element, b as attr, f as insert, g as append, x as noop, n as detach, B as empty, e as element, a as space, t as text, Y as set_style, h as set_data, C as destroy_each, d as toggle_class, P as Block, c as create_component, m as mount_component, j as transition_in, k as transition_out, o as destroy_component, F as createEventDispatcher, R as assign, T as StatusTracker, U as get_spread_update, V as get_spread_object, D as group_outros, E as check_outros } from "./main.js";
import { B as BlockLabel } from "./BlockLabel.js";
function create_fragment$2(ctx) {
  let svg;
  let path;
  return {
    c() {
      svg = svg_element("svg");
      path = svg_element("path");
      attr(path, "fill", "currentColor");
      attr(path, "d", "M4 2H2v26a2 2 0 0 0 2 2h26v-2H4v-3h22v-8H4v-4h14V5H4Zm20 17v4H4v-4ZM16 7v4H4V7Z");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "xmlns:xlink", "http://www.w3.org/1999/xlink");
      attr(svg, "aria-hidden", "true");
      attr(svg, "role", "img");
      attr(svg, "class", "iconify iconify--carbon");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "preserveAspectRatio", "xMidYMid meet");
      attr(svg, "viewBox", "0 0 32 32");
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
class LineChart extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$2, safe_not_equal, {});
  }
}
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[3] = list[i];
  return child_ctx;
}
function create_if_block$1(ctx) {
  let each_1_anchor;
  let each_value = ctx[0].confidences;
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  return {
    c() {
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      each_1_anchor = empty();
    },
    m(target, anchor) {
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(target, anchor);
      }
      insert(target, each_1_anchor, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 1) {
        each_value = ctx2[0].confidences;
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(each_1_anchor.parentNode, each_1_anchor);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
    },
    d(detaching) {
      destroy_each(each_blocks, detaching);
      if (detaching)
        detach(each_1_anchor);
    }
  };
}
function create_if_block_1$1(ctx) {
  let div0;
  let t0;
  let div1;
  let t1_value = Math.round(ctx[3].confidence * 100) + "";
  let t1;
  let t2;
  return {
    c() {
      div0 = element("div");
      t0 = space();
      div1 = element("div");
      t1 = text(t1_value);
      t2 = text("%");
      attr(div0, "class", "flex-1 border border-dashed border-gray-100 px-4");
      attr(div1, "class", "text-right ml-auto");
    },
    m(target, anchor) {
      insert(target, div0, anchor);
      insert(target, t0, anchor);
      insert(target, div1, anchor);
      append(div1, t1);
      append(div1, t2);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t1_value !== (t1_value = Math.round(ctx2[3].confidence * 100) + ""))
        set_data(t1, t1_value);
    },
    d(detaching) {
      if (detaching)
        detach(div0);
      if (detaching)
        detach(t0);
      if (detaching)
        detach(div1);
    }
  };
}
function create_each_block(ctx) {
  let div4;
  let div3;
  let div0;
  let t0;
  let div2;
  let div1;
  let t1_value = ctx[3].label + "";
  let t1;
  let t2;
  let t3;
  let if_block = ctx[0].confidences && create_if_block_1$1(ctx);
  return {
    c() {
      div4 = element("div");
      div3 = element("div");
      div0 = element("div");
      t0 = space();
      div2 = element("div");
      div1 = element("div");
      t1 = text(t1_value);
      t2 = space();
      if (if_block)
        if_block.c();
      t3 = space();
      attr(div0, "class", "h-1 mb-1 rounded bg-gradient-to-r group-hover:from-orange-500 from-orange-400 to-orange-200 dark:from-orange-400 dark:to-orange-600");
      set_style(div0, "width", ctx[3].confidence * 100 + "%");
      attr(div1, "class", "leading-snug");
      attr(div2, "class", "flex items-baseline space-x-2 group-hover:text-orange-500");
      attr(div3, "class", "flex-1");
      attr(div4, "class", "flex items-start justify-between font-mono text-sm leading-none group mb-2 last:mb-0 dark:text-slate-300");
    },
    m(target, anchor) {
      insert(target, div4, anchor);
      append(div4, div3);
      append(div3, div0);
      append(div3, t0);
      append(div3, div2);
      append(div2, div1);
      append(div1, t1);
      append(div2, t2);
      if (if_block)
        if_block.m(div2, null);
      append(div4, t3);
    },
    p(ctx2, dirty) {
      if (dirty & 1) {
        set_style(div0, "width", ctx2[3].confidence * 100 + "%");
      }
      if (dirty & 1 && t1_value !== (t1_value = ctx2[3].label + ""))
        set_data(t1, t1_value);
      if (ctx2[0].confidences) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block_1$1(ctx2);
          if_block.c();
          if_block.m(div2, null);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
    d(detaching) {
      if (detaching)
        detach(div4);
      if (if_block)
        if_block.d();
    }
  };
}
function create_fragment$1(ctx) {
  let div1;
  let div0;
  let t0_value = ctx[0].label + "";
  let t0;
  let t1;
  let if_block = typeof ctx[0] === "object" && ctx[0].confidences && create_if_block$1(ctx);
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      t0 = text(t0_value);
      t1 = space();
      if (if_block)
        if_block.c();
      attr(div0, "class", "output-class font-bold text-2xl py-6 px-4 flex-grow flex items-center justify-center dark:text-slate-200");
      toggle_class(div0, "sr-only", !ctx[1]);
      toggle_class(div0, "no-confidence", !("confidences" in ctx[0]));
      set_style(div0, "background-color", ctx[2] || "transparent", false);
      attr(div1, "class", "output-label");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      append(div0, t0);
      append(div1, t1);
      if (if_block)
        if_block.m(div1, null);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1 && t0_value !== (t0_value = ctx2[0].label + ""))
        set_data(t0, t0_value);
      if (dirty & 2) {
        toggle_class(div0, "sr-only", !ctx2[1]);
      }
      if (dirty & 1) {
        toggle_class(div0, "no-confidence", !("confidences" in ctx2[0]));
      }
      if (dirty & 4) {
        set_style(div0, "background-color", ctx2[2] || "transparent", false);
      }
      if (typeof ctx2[0] === "object" && ctx2[0].confidences) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block$1(ctx2);
          if_block.c();
          if_block.m(div1, null);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div1);
      if (if_block)
        if_block.d();
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { value } = $$props;
  let { show_label } = $$props;
  let { color } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("show_label" in $$props2)
      $$invalidate(1, show_label = $$props2.show_label);
    if ("color" in $$props2)
      $$invalidate(2, color = $$props2.color);
  };
  return [value, show_label, color];
}
class Label extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { value: 0, show_label: 1, color: 2 });
  }
}
function create_if_block_1(ctx) {
  let blocklabel;
  let current;
  blocklabel = new BlockLabel({
    props: {
      Icon: LineChart,
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
  let labelicon;
  let current;
  labelicon = new LineChart({});
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      create_component(labelicon.$$.fragment);
      attr(div0, "class", "h-5 dark:text-white opacity-50");
      attr(div1, "class", "h-full min-h-[6rem] flex justify-center items-center");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      mount_component(labelicon, div0, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(labelicon.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(labelicon.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(labelicon);
    }
  };
}
function create_if_block(ctx) {
  let label_1;
  let current;
  label_1 = new Label({
    props: {
      value: ctx[3],
      show_label: ctx[7],
      color: ctx[2]
    }
  });
  return {
    c() {
      create_component(label_1.$$.fragment);
    },
    m(target, anchor) {
      mount_component(label_1, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const label_1_changes = {};
      if (dirty & 8)
        label_1_changes.value = ctx2[3];
      if (dirty & 128)
        label_1_changes.show_label = ctx2[7];
      if (dirty & 4)
        label_1_changes.color = ctx2[2];
      label_1.$set(label_1_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(label_1.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(label_1.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(label_1, detaching);
    }
  };
}
function create_default_slot(ctx) {
  let statustracker;
  let t0;
  let t1;
  let current_block_type_index;
  let if_block1;
  let if_block1_anchor;
  let current;
  const statustracker_spread_levels = [ctx[6]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  let if_block0 = ctx[7] && create_if_block_1(ctx);
  const if_block_creators = [create_if_block, create_else_block];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (typeof ctx2[3] === "object" && ctx2[3] !== void 0 && ctx2[3] !== null)
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block1 = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t0 = space();
      if (if_block0)
        if_block0.c();
      t1 = space();
      if_block1.c();
      if_block1_anchor = empty();
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t0, anchor);
      if (if_block0)
        if_block0.m(target, anchor);
      insert(target, t1, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block1_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 64 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[6])]) : {};
      statustracker.$set(statustracker_changes);
      if (ctx2[7]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
          if (dirty & 128) {
            transition_in(if_block0, 1);
          }
        } else {
          if_block0 = create_if_block_1(ctx2);
          if_block0.c();
          transition_in(if_block0, 1);
          if_block0.m(t1.parentNode, t1);
        }
      } else if (if_block0) {
        group_outros();
        transition_out(if_block0, 1, 1, () => {
          if_block0 = null;
        });
        check_outros();
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
      transition_in(statustracker.$$.fragment, local);
      transition_in(if_block0);
      transition_in(if_block1);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(if_block0);
      transition_out(if_block1);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t0);
      if (if_block0)
        if_block0.d(detaching);
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
      test_id: "label",
      visible: ctx[1],
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
      if (dirty & 764) {
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
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { color = void 0 } = $$props;
  let { value } = $$props;
  let { label = "Label" } = $$props;
  let { style = {} } = $$props;
  let { loading_status } = $$props;
  let { show_label } = $$props;
  const dispatch = createEventDispatcher();
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(1, visible = $$props2.visible);
    if ("color" in $$props2)
      $$invalidate(2, color = $$props2.color);
    if ("value" in $$props2)
      $$invalidate(3, value = $$props2.value);
    if ("label" in $$props2)
      $$invalidate(4, label = $$props2.label);
    if ("style" in $$props2)
      $$invalidate(5, style = $$props2.style);
    if ("loading_status" in $$props2)
      $$invalidate(6, loading_status = $$props2.loading_status);
    if ("show_label" in $$props2)
      $$invalidate(7, show_label = $$props2.show_label);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 8) {
      dispatch("change");
    }
  };
  return [elem_id, visible, color, value, label, style, loading_status, show_label];
}
class Label_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 0,
      visible: 1,
      color: 2,
      value: 3,
      label: 4,
      style: 5,
      loading_status: 6,
      show_label: 7
    });
  }
}
var Label_1$1 = Label_1;
const modes = ["static"];
const document = (config) => ({
  type: "{ label: string; confidences?: Array<{ label: string; confidence: number }>",
  description: "output label and optional set of confidences per label"
});
export { Label_1$1 as Component, document, modes };
//# sourceMappingURL=index24.js.map
