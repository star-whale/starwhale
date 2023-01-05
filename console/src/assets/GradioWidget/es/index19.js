import { S as SvelteComponent, i as init, s as safe_not_equal, w as svg_element, b as attr, f as insert, g as append, x as noop, n as detach, B as empty, a5 as colors, e as element, Y as set_style, t as text, a as space, h as set_data, C as destroy_each, l as listen, A as run_all, d as toggle_class, P as Block, c as create_component, m as mount_component, j as transition_in, k as transition_out, o as destroy_component, F as createEventDispatcher, R as assign, T as StatusTracker, U as get_spread_update, V as get_spread_object, D as group_outros, E as check_outros } from "./main.js";
import { g as get_next_color } from "./color.js";
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
      attr(path0, "d", "M12 15H5a3 3 0 0 1-3-3v-2a3 3 0 0 1 3-3h5V5a1 1 0 0 0-1-1H3V2h6a3 3 0 0 1 3 3zM5 9a1 1 0 0 0-1 1v2a1 1 0 0 0 1 1h5V9zm15 14v2a1 1 0 0 0 1 1h5v-4h-5a1 1 0 0 0-1 1z");
      attr(path1, "fill", "currentColor");
      attr(path1, "d", "M2 30h28V2Zm26-2h-7a3 3 0 0 1-3-3v-2a3 3 0 0 1 3-3h5v-2a1 1 0 0 0-1-1h-6v-2h6a3 3 0 0 1 3 3Z");
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
class TextHighlight extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$2, safe_not_equal, {});
  }
}
var HighlightedText_svelte_svelte_type_style_lang = "";
function get_each_context_2(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[15] = list[i][0];
  child_ctx[22] = list[i][1];
  return child_ctx;
}
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[15] = list[i][0];
  child_ctx[16] = list[i][1];
  return child_ctx;
}
function get_each_context_1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[16] = list[i][0];
  child_ctx[19] = list[i][1];
  child_ctx[21] = i;
  return child_ctx;
}
function create_else_block$1(ctx) {
  let t;
  let div;
  let if_block = ctx[1] && create_if_block_3();
  let each_value_2 = ctx[0];
  let each_blocks = [];
  for (let i = 0; i < each_value_2.length; i += 1) {
    each_blocks[i] = create_each_block_2(get_each_context_2(ctx, each_value_2, i));
  }
  return {
    c() {
      if (if_block)
        if_block.c();
      t = space();
      div = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", "textfield p-2 bg-white dark:bg-gray-800 rounded box-border max-w-full break-word leading-7");
      attr(div, "data-testid", "highlighted-text:textfield");
    },
    m(target, anchor) {
      if (if_block)
        if_block.m(target, anchor);
      insert(target, t, anchor);
      insert(target, div, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
    },
    p(ctx2, dirty) {
      if (ctx2[1]) {
        if (if_block)
          ;
        else {
          if_block = create_if_block_3();
          if_block.c();
          if_block.m(t.parentNode, t);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty & 1) {
        each_value_2 = ctx2[0];
        let i;
        for (i = 0; i < each_value_2.length; i += 1) {
          const child_ctx = get_each_context_2(ctx2, each_value_2, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block_2(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(div, null);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value_2.length;
      }
    },
    d(detaching) {
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(t);
      if (detaching)
        detach(div);
      destroy_each(each_blocks, detaching);
    }
  };
}
function create_if_block$1(ctx) {
  let t;
  let div;
  let if_block = ctx[1] && create_if_block_2(ctx);
  let each_value = ctx[0];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  return {
    c() {
      if (if_block)
        if_block.c();
      t = space();
      div = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", "textfield bg-white dark:bg-transparent rounded-sm text-sm box-border max-w-full break-word leading-7 mt-7");
      attr(div, "data-testid", "highlighted-text:textfield");
    },
    m(target, anchor) {
      if (if_block)
        if_block.m(target, anchor);
      insert(target, t, anchor);
      insert(target, div, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
    },
    p(ctx2, dirty) {
      if (ctx2[1]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block_2(ctx2);
          if_block.c();
          if_block.m(t.parentNode, t);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty & 15) {
        each_value = ctx2[0];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(div, null);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
    },
    d(detaching) {
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(t);
      if (detaching)
        detach(div);
      destroy_each(each_blocks, detaching);
    }
  };
}
function create_if_block_3(ctx) {
  let div;
  return {
    c() {
      div = element("div");
      div.innerHTML = `<span>-1</span> 
			<span>0</span> 
			<span>+1</span>`;
      attr(div, "class", "color_legend flex px-2 py-1 justify-between rounded mb-3 font-semibold mt-7");
      attr(div, "data-testid", "highlighted-text:color-legend");
      set_style(div, "background", "-webkit-linear-gradient(to right,#8d83d6,(255,255,255,0),#eb4d4b)");
      set_style(div, "background", "linear-gradient(to right,#8d83d6,rgba(255,255,255,0),#eb4d4b)");
    },
    m(target, anchor) {
      insert(target, div, anchor);
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_each_block_2(ctx) {
  let span1;
  let span0;
  let t0_value = ctx[15] + "";
  let t0;
  let t1;
  let span1_style_value;
  return {
    c() {
      span1 = element("span");
      span0 = element("span");
      t0 = text(t0_value);
      t1 = space();
      attr(span0, "class", "text dark:text-white");
      attr(span1, "class", "textspan p-1 mr-0.5 bg-opacity-20 dark:bg-opacity-80 rounded-sm");
      attr(span1, "style", span1_style_value = "background-color: rgba(" + (ctx[22] < 0 ? "141, 131, 214," + -ctx[22] : "235, 77, 75," + ctx[22]) + ")");
    },
    m(target, anchor) {
      insert(target, span1, anchor);
      append(span1, span0);
      append(span0, t0);
      append(span1, t1);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t0_value !== (t0_value = ctx2[15] + ""))
        set_data(t0, t0_value);
      if (dirty & 1 && span1_style_value !== (span1_style_value = "background-color: rgba(" + (ctx2[22] < 0 ? "141, 131, 214," + -ctx2[22] : "235, 77, 75," + ctx2[22]) + ")")) {
        attr(span1, "style", span1_style_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(span1);
    }
  };
}
function create_if_block_2(ctx) {
  let div;
  let each_value_1 = Object.entries(ctx[2]);
  let each_blocks = [];
  for (let i = 0; i < each_value_1.length; i += 1) {
    each_blocks[i] = create_each_block_1(get_each_context_1(ctx, each_value_1, i));
  }
  return {
    c() {
      div = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", "category-legend flex flex-wrap gap-1 mb-2 text-black mt-7");
      attr(div, "data-testid", "highlighted-text:category-legend");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
    },
    p(ctx2, dirty) {
      if (dirty & 100) {
        each_value_1 = Object.entries(ctx2[2]);
        let i;
        for (i = 0; i < each_value_1.length; i += 1) {
          const child_ctx = get_each_context_1(ctx2, each_value_1, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block_1(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(div, null);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value_1.length;
      }
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_each(each_blocks, detaching);
    }
  };
}
function create_each_block_1(ctx) {
  let div;
  let t0_value = ctx[16] + "";
  let t0;
  let t1;
  let div_style_value;
  let mounted;
  let dispose;
  function mouseover_handler() {
    return ctx[8](ctx[16]);
  }
  function focus_handler() {
    return ctx[9](ctx[16]);
  }
  return {
    c() {
      div = element("div");
      t0 = text(t0_value);
      t1 = space();
      attr(div, "class", "category-label px-2 rounded-sm font-semibold cursor-pointer");
      attr(div, "style", div_style_value = "background-color:" + ctx[19].secondary);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t0);
      append(div, t1);
      if (!mounted) {
        dispose = [
          listen(div, "mouseover", mouseover_handler),
          listen(div, "focus", focus_handler),
          listen(div, "mouseout", ctx[10]),
          listen(div, "blur", ctx[11])
        ];
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty & 4 && t0_value !== (t0_value = ctx[16] + ""))
        set_data(t0, t0_value);
      if (dirty & 4 && div_style_value !== (div_style_value = "background-color:" + ctx[19].secondary)) {
        attr(div, "style", div_style_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(div);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block_1$1(ctx) {
  let t0;
  let span;
  let t1_value = ctx[16] + "";
  let t1;
  let t2;
  return {
    c() {
      t0 = text("\xA0");
      span = element("span");
      t1 = text(t1_value);
      t2 = space();
      attr(span, "class", "label mr-[-4px] font-bold uppercase text-xs inline-category text-white rounded-sm px-[0.325rem] mt-[0.05rem] py-[0.05rem] transition-colors svelte-o4yfdm");
      set_style(span, "background-color", ctx[16] === null || ctx[3] && ctx[3] !== ctx[16] ? "" : ctx[2][ctx[16]].primary, false);
    },
    m(target, anchor) {
      insert(target, t0, anchor);
      insert(target, span, anchor);
      append(span, t1);
      insert(target, t2, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t1_value !== (t1_value = ctx2[16] + ""))
        set_data(t1, t1_value);
      if (dirty & 13) {
        set_style(span, "background-color", ctx2[16] === null || ctx2[3] && ctx2[3] !== ctx2[16] ? "" : ctx2[2][ctx2[16]].primary, false);
      }
    },
    d(detaching) {
      if (detaching)
        detach(t0);
      if (detaching)
        detach(span);
      if (detaching)
        detach(t2);
    }
  };
}
function create_each_block(ctx) {
  let span1;
  let span0;
  let t0_value = ctx[15] + "";
  let t0;
  let t1;
  let if_block = !ctx[1] && ctx[16] !== null && create_if_block_1$1(ctx);
  return {
    c() {
      span1 = element("span");
      span0 = element("span");
      t0 = text(t0_value);
      t1 = space();
      if (if_block)
        if_block.c();
      attr(span0, "class", "text ");
      attr(span1, "class", "textspan rounded-sm px-1 transition-colors text-black pb-[0.225rem] pt-[0.15rem] svelte-o4yfdm");
      toggle_class(span1, "dark:text-white", ctx[16] === null || ctx[3] && ctx[3] !== ctx[16]);
      toggle_class(span1, "hl", ctx[16] !== null);
      set_style(span1, "background-color", ctx[16] === null || ctx[3] && ctx[3] !== ctx[16] ? "" : ctx[2][ctx[16]].secondary, false);
    },
    m(target, anchor) {
      insert(target, span1, anchor);
      append(span1, span0);
      append(span0, t0);
      append(span1, t1);
      if (if_block)
        if_block.m(span1, null);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t0_value !== (t0_value = ctx2[15] + ""))
        set_data(t0, t0_value);
      if (!ctx2[1] && ctx2[16] !== null) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block_1$1(ctx2);
          if_block.c();
          if_block.m(span1, null);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty & 9) {
        toggle_class(span1, "dark:text-white", ctx2[16] === null || ctx2[3] && ctx2[3] !== ctx2[16]);
      }
      if (dirty & 1) {
        toggle_class(span1, "hl", ctx2[16] !== null);
      }
      if (dirty & 13) {
        set_style(span1, "background-color", ctx2[16] === null || ctx2[3] && ctx2[3] !== ctx2[16] ? "" : ctx2[2][ctx2[16]].secondary, false);
      }
    },
    d(detaching) {
      if (detaching)
        detach(span1);
      if (if_block)
        if_block.d();
    }
  };
}
function create_fragment$1(ctx) {
  let if_block_anchor;
  function select_block_type(ctx2, dirty) {
    if (ctx2[4] === "categories")
      return create_if_block$1;
    return create_else_block$1;
  }
  let current_block_type = select_block_type(ctx);
  let if_block = current_block_type(ctx);
  return {
    c() {
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
    },
    p(ctx2, [dirty]) {
      if (current_block_type === (current_block_type = select_block_type(ctx2)) && if_block) {
        if_block.p(ctx2, dirty);
      } else {
        if_block.d(1);
        if_block = current_block_type(ctx2);
        if (if_block) {
          if_block.c();
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        }
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  const browser = typeof document !== "undefined";
  let { value = [] } = $$props;
  let { show_legend = false } = $$props;
  let { color_map = {} } = $$props;
  let ctx;
  let _color_map = {};
  let active = "";
  function name_to_rgba(name, a) {
    if (!ctx) {
      var canvas = document.createElement("canvas");
      ctx = canvas.getContext("2d");
    }
    ctx.fillStyle = name;
    ctx.fillRect(0, 0, 1, 1);
    const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data;
    ctx.clearRect(0, 0, 1, 1);
    return `rgba(${r}, ${g}, ${b}, ${255 / a})`;
  }
  let mode;
  function handle_mouseover(label) {
    $$invalidate(3, active = label);
  }
  function handle_mouseout() {
    $$invalidate(3, active = "");
  }
  const mouseover_handler = (category) => handle_mouseover(category);
  const focus_handler = (category) => handle_mouseover(category);
  const mouseout_handler = () => handle_mouseout();
  const blur_handler = () => handle_mouseout();
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("show_legend" in $$props2)
      $$invalidate(1, show_legend = $$props2.show_legend);
    if ("color_map" in $$props2)
      $$invalidate(7, color_map = $$props2.color_map);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 129) {
      {
        let correct_color_map = function() {
          for (const col in color_map) {
            const _c = color_map[col].trim();
            if (_c in colors) {
              $$invalidate(2, _color_map[col] = colors[_c], _color_map);
            } else {
              $$invalidate(2, _color_map[col] = {
                primary: browser ? name_to_rgba(color_map[col], 1) : color_map[col],
                secondary: browser ? name_to_rgba(color_map[col], 0.5) : color_map[col]
              }, _color_map);
            }
          }
        };
        if (!color_map) {
          $$invalidate(7, color_map = {});
        }
        if (value.length > 0) {
          for (let [_, label] of value) {
            if (label !== null) {
              if (typeof label === "string") {
                $$invalidate(4, mode = "categories");
                if (!(label in color_map)) {
                  let color = get_next_color(Object.keys(color_map).length);
                  $$invalidate(7, color_map[label] = color, color_map);
                }
              } else {
                $$invalidate(4, mode = "scores");
              }
            }
          }
        }
        correct_color_map();
      }
    }
  };
  return [
    value,
    show_legend,
    _color_map,
    active,
    mode,
    handle_mouseover,
    handle_mouseout,
    color_map,
    mouseover_handler,
    focus_handler,
    mouseout_handler,
    blur_handler
  ];
}
class HighlightedText extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { value: 0, show_legend: 1, color_map: 7 });
  }
}
function create_if_block_1(ctx) {
  let blocklabel;
  let current;
  blocklabel = new BlockLabel({
    props: {
      Icon: TextHighlight,
      label: ctx[5],
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
        blocklabel_changes.label = ctx2[5];
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
function create_else_block(ctx) {
  let div1;
  let div0;
  let texthighlight;
  let current;
  texthighlight = new TextHighlight({});
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      create_component(texthighlight.$$.fragment);
      attr(div0, "class", "h-5 dark:text-white opacity-50");
      attr(div1, "class", "h-full min-h-[6rem] flex justify-center items-center");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      mount_component(texthighlight, div0, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(texthighlight.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(texthighlight.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(texthighlight);
    }
  };
}
function create_if_block(ctx) {
  let highlightedtext;
  let current;
  highlightedtext = new HighlightedText({
    props: {
      value: ctx[3],
      show_legend: ctx[4],
      color_map: ctx[0].color_map
    }
  });
  return {
    c() {
      create_component(highlightedtext.$$.fragment);
    },
    m(target, anchor) {
      mount_component(highlightedtext, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const highlightedtext_changes = {};
      if (dirty & 8)
        highlightedtext_changes.value = ctx2[3];
      if (dirty & 16)
        highlightedtext_changes.show_legend = ctx2[4];
      if (dirty & 1)
        highlightedtext_changes.color_map = ctx2[0].color_map;
      highlightedtext.$set(highlightedtext_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(highlightedtext.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(highlightedtext.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(highlightedtext, detaching);
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
  let if_block0 = ctx[5] && create_if_block_1(ctx);
  const if_block_creators = [create_if_block, create_else_block];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[3])
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
      if (ctx2[5]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
          if (dirty & 32) {
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
      test_id: "highlighted-text",
      visible: ctx[2],
      elem_id: ctx[1],
      disable: typeof ctx[0].container === "boolean" && !ctx[0].container,
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
      if (dirty & 4)
        block_changes.visible = ctx2[2];
      if (dirty & 2)
        block_changes.elem_id = ctx2[1];
      if (dirty & 1)
        block_changes.disable = typeof ctx2[0].container === "boolean" && !ctx2[0].container;
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
  let { value } = $$props;
  let { show_legend } = $$props;
  let { color_map = {} } = $$props;
  let { label } = $$props;
  let { style = {} } = $$props;
  let { loading_status } = $$props;
  const dispatch = createEventDispatcher();
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(1, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(2, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(3, value = $$props2.value);
    if ("show_legend" in $$props2)
      $$invalidate(4, show_legend = $$props2.show_legend);
    if ("color_map" in $$props2)
      $$invalidate(7, color_map = $$props2.color_map);
    if ("label" in $$props2)
      $$invalidate(5, label = $$props2.label);
    if ("style" in $$props2)
      $$invalidate(0, style = $$props2.style);
    if ("loading_status" in $$props2)
      $$invalidate(6, loading_status = $$props2.loading_status);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 129) {
      if (!style.color_map && Object.keys(color_map).length) {
        $$invalidate(0, style.color_map = color_map, style);
      }
    }
    if ($$self.$$.dirty & 8) {
      dispatch("change");
    }
  };
  return [style, elem_id, visible, value, show_legend, label, loading_status, color_map];
}
class HighlightedText_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 1,
      visible: 2,
      value: 3,
      show_legend: 4,
      color_map: 7,
      label: 5,
      style: 0,
      loading_status: 6
    });
  }
}
var HighlightedText_1$1 = HighlightedText_1;
const modes = ["static"];
const document$1 = (config) => ({
  type: "Array<[string, string | number]>",
  description: "list of text spans and corresponding label / value"
});
export { HighlightedText_1$1 as Component, document$1 as document, modes };
//# sourceMappingURL=index19.js.map
