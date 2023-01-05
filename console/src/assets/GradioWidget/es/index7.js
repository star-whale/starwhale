import { S as SvelteComponent, i as init, s as safe_not_equal, w as svg_element, b as attr, f as insert, g as append, x as noop, n as detach, e as element, a as space, a2 as HtmlTag, C as destroy_each, F as createEventDispatcher, a3 as beforeUpdate, a4 as afterUpdate, I as binding_callbacks, a5 as colors, P as Block, c as create_component, m as mount_component, j as transition_in, k as transition_out, o as destroy_component, R as assign, T as StatusTracker, U as get_spread_update, V as get_spread_object, D as group_outros, E as check_outros, K as bubble } from "./main.js";
import { B as BlockLabel } from "./BlockLabel.js";
function create_fragment$2(ctx) {
  let svg;
  let path0;
  let path1;
  return {
    c() {
      svg = svg_element("svg");
      path0 = svg_element("path");
      path1 = svg_element("path");
      attr(path0, "fill", "currentColor");
      attr(path0, "d", "M17.74 30L16 29l4-7h6a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h9v2H6a4 4 0 0 1-4-4V8a4 4 0 0 1 4-4h20a4 4 0 0 1 4 4v12a4 4 0 0 1-4 4h-4.84Z");
      attr(path1, "fill", "currentColor");
      attr(path1, "d", "M8 10h16v2H8zm0 6h10v2H8z");
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
      append(svg, path0);
      append(svg, path1);
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
class Chat extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$2, safe_not_equal, {});
  }
}
var ChatBot_svelte_svelte_type_style_lang = "";
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[9] = list[i];
  return child_ctx;
}
function create_each_block(ctx) {
  let div0;
  let raw0_value = ctx[9][0] + "";
  let div0_style_value;
  let t0;
  let div1;
  let html_tag;
  let raw1_value = ctx[9][1] + "";
  let t1;
  let div1_style_value;
  return {
    c() {
      div0 = element("div");
      t0 = space();
      div1 = element("div");
      html_tag = new HtmlTag(false);
      t1 = space();
      attr(div0, "data-testid", "user");
      attr(div0, "class", "px-3 py-2 rounded-[22px] rounded-br-none text-white text-sm chat-message svelte-1kgfmmo");
      attr(div0, "style", div0_style_value = "background-color:" + ctx[2][0]);
      html_tag.a = t1;
      attr(div1, "data-testid", "bot");
      attr(div1, "class", "px-3 py-2 rounded-[22px] rounded-bl-none place-self-start text-white text-sm chat-message svelte-1kgfmmo");
      attr(div1, "style", div1_style_value = "background-color:" + ctx[2][1]);
    },
    m(target, anchor) {
      insert(target, div0, anchor);
      div0.innerHTML = raw0_value;
      insert(target, t0, anchor);
      insert(target, div1, anchor);
      html_tag.m(raw1_value, div1);
      append(div1, t1);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && raw0_value !== (raw0_value = ctx2[9][0] + ""))
        div0.innerHTML = raw0_value;
      if (dirty & 4 && div0_style_value !== (div0_style_value = "background-color:" + ctx2[2][0])) {
        attr(div0, "style", div0_style_value);
      }
      if (dirty & 1 && raw1_value !== (raw1_value = ctx2[9][1] + ""))
        html_tag.p(raw1_value);
      if (dirty & 4 && div1_style_value !== (div1_style_value = "background-color:" + ctx2[2][1])) {
        attr(div1, "style", div1_style_value);
      }
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
function create_fragment$1(ctx) {
  let div1;
  let div0;
  let each_value = ctx[0];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div0, "class", "flex flex-col items-end space-y-4 p-3");
      attr(div1, "class", "overflow-y-auto h-[40vh]");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div0, null);
      }
      ctx[4](div1);
    },
    p(ctx2, [dirty]) {
      if (dirty & 5) {
        each_value = ctx2[0];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(div0, null);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_each(each_blocks, detaching);
      ctx[4](null);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let _colors;
  let { value } = $$props;
  let { style = {} } = $$props;
  let div;
  let autoscroll;
  const dispatch = createEventDispatcher();
  beforeUpdate(() => {
    autoscroll = div && div.offsetHeight + div.scrollTop > div.scrollHeight - 20;
  });
  afterUpdate(() => {
    if (autoscroll)
      div.scrollTo(0, div.scrollHeight);
  });
  function get_color(c) {
    if (c in colors) {
      return colors[c].primary;
    } else {
      return c;
    }
  }
  function get_colors() {
    if (!style.color_map) {
      return ["#fb923c", "#9ca3af"];
    } else {
      return [get_color(style.color_map[0]), get_color(style.color_map[1])];
    }
  }
  function div1_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      div = $$value;
      $$invalidate(1, div);
    });
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("style" in $$props2)
      $$invalidate(3, style = $$props2.style);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      value && dispatch("change");
    }
  };
  $$invalidate(2, _colors = get_colors());
  return [value, div, _colors, style, div1_binding];
}
class ChatBot extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { value: 0, style: 3 });
  }
}
function create_if_block(ctx) {
  let blocklabel;
  let current;
  blocklabel = new BlockLabel({
    props: {
      show_label: ctx[5],
      Icon: Chat,
      label: ctx[4] || "Chatbot",
      disable: typeof ctx[0].container === "boolean" && !ctx[0].container
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
      if (dirty & 32)
        blocklabel_changes.show_label = ctx2[5];
      if (dirty & 16)
        blocklabel_changes.label = ctx2[4] || "Chatbot";
      if (dirty & 1)
        blocklabel_changes.disable = typeof ctx2[0].container === "boolean" && !ctx2[0].container;
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
function create_default_slot(ctx) {
  let statustracker;
  let t0;
  let t1;
  let chatbot;
  let current;
  const statustracker_spread_levels = [ctx[6]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  let if_block = ctx[5] && create_if_block(ctx);
  chatbot = new ChatBot({
    props: {
      style: ctx[0],
      value: ctx[3]
    }
  });
  chatbot.$on("change", ctx[8]);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t0 = space();
      if (if_block)
        if_block.c();
      t1 = space();
      create_component(chatbot.$$.fragment);
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t0, anchor);
      if (if_block)
        if_block.m(target, anchor);
      insert(target, t1, anchor);
      mount_component(chatbot, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 64 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[6])]) : {};
      statustracker.$set(statustracker_changes);
      if (ctx2[5]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty & 32) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block(ctx2);
          if_block.c();
          transition_in(if_block, 1);
          if_block.m(t1.parentNode, t1);
        }
      } else if (if_block) {
        group_outros();
        transition_out(if_block, 1, 1, () => {
          if_block = null;
        });
        check_outros();
      }
      const chatbot_changes = {};
      if (dirty & 1)
        chatbot_changes.style = ctx2[0];
      if (dirty & 8)
        chatbot_changes.value = ctx2[3];
      chatbot.$set(chatbot_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(if_block);
      transition_in(chatbot.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(if_block);
      transition_out(chatbot.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t0);
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(t1);
      destroy_component(chatbot, detaching);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      padding: false,
      elem_id: ctx[1],
      visible: ctx[2],
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
        block_changes.elem_id = ctx2[1];
      if (dirty & 4)
        block_changes.visible = ctx2[2];
      if (dirty & 633) {
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
  let { value = [] } = $$props;
  let { style = {} } = $$props;
  let { label } = $$props;
  let { show_label = true } = $$props;
  let { color_map = {} } = $$props;
  let { loading_status } = $$props;
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(1, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(2, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(3, value = $$props2.value);
    if ("style" in $$props2)
      $$invalidate(0, style = $$props2.style);
    if ("label" in $$props2)
      $$invalidate(4, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(5, show_label = $$props2.show_label);
    if ("color_map" in $$props2)
      $$invalidate(7, color_map = $$props2.color_map);
    if ("loading_status" in $$props2)
      $$invalidate(6, loading_status = $$props2.loading_status);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 129) {
      if (!style.color_map && Object.keys(color_map).length) {
        $$invalidate(0, style.color_map = color_map, style);
      }
    }
  };
  return [
    style,
    elem_id,
    visible,
    value,
    label,
    show_label,
    loading_status,
    color_map,
    change_handler
  ];
}
class Chatbot extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 1,
      visible: 2,
      value: 3,
      style: 0,
      label: 4,
      show_label: 5,
      color_map: 7,
      loading_status: 6
    });
  }
}
var Chatbot$1 = Chatbot;
const modes = ["static"];
const document = (config) => {
  var _a;
  return {
    type: "Array<[string, string]>",
    description: "Represents list of message pairs of chat message.",
    example_data: (_a = config.value) != null ? _a : [
      ["Hi", "Hello"],
      ["1 + 1", "2"]
    ]
  };
};
export { Chatbot$1 as Component, document, modes };
//# sourceMappingURL=index7.js.map
