import { S as SvelteComponent, i as init, s as safe_not_equal, w as svg_element, b as attr, f as insert, g as append, x as noop, n as detach, e as element, d as toggle_class, l as listen, y as is_function, z as prevent_default, t as text, h as set_data, A as run_all, B as empty, a as space, C as destroy_each, c as create_component, m as mount_component, j as transition_in, k as transition_out, o as destroy_component, D as group_outros, E as check_outros, F as createEventDispatcher, G as spring, H as subscribe, I as binding_callbacks, J as onDestroy, K as bubble, L as add_flush_callback, M as src_url_equal, N as action_destroyer, O as bind, P as Block, Q as component_subscribe, X, R as assign, T as StatusTracker, U as get_spread_update, V as get_spread_object } from "./main.js";
import { U as Upload } from "./Upload.js";
import { M as ModifyUpload } from "./ModifyUpload.js";
import { B as BlockLabel } from "./BlockLabel.js";
import { n as normalise_file } from "./utils.js";
function create_fragment$5(ctx) {
  let svg;
  let path;
  let circle0;
  let circle1;
  return {
    c() {
      svg = svg_element("svg");
      path = svg_element("path");
      circle0 = svg_element("circle");
      circle1 = svg_element("circle");
      attr(path, "d", "M9 18V5l12-2v13");
      attr(circle0, "cx", "6");
      attr(circle0, "cy", "18");
      attr(circle0, "r", "3");
      attr(circle1, "cx", "18");
      attr(circle1, "cy", "16");
      attr(circle1, "r", "3");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "viewBox", "0 0 24 24");
      attr(svg, "fill", "none");
      attr(svg, "stroke", "currentColor");
      attr(svg, "stroke-width", "1.5");
      attr(svg, "stroke-linecap", "round");
      attr(svg, "stroke-linejoin", "round");
      attr(svg, "class", "feather feather-music");
    },
    m(target, anchor) {
      insert(target, svg, anchor);
      append(svg, path);
      append(svg, circle0);
      append(svg, circle1);
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
class Music extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$5, safe_not_equal, {});
  }
}
var RangePips_svelte_svelte_type_style_lang = "";
function get_each_context$1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[27] = list[i];
  child_ctx[29] = i;
  return child_ctx;
}
function create_if_block_9(ctx) {
  let span;
  let span_style_value;
  let mounted;
  let dispose;
  let if_block = (ctx[6] === "label" || ctx[7] === "label") && create_if_block_10(ctx);
  return {
    c() {
      span = element("span");
      if (if_block)
        if_block.c();
      attr(span, "class", "pip first");
      attr(span, "style", span_style_value = ctx[14] + ": 0%;");
      toggle_class(span, "selected", ctx[17](ctx[0]));
      toggle_class(span, "in-range", ctx[16](ctx[0]));
    },
    m(target, anchor) {
      insert(target, span, anchor);
      if (if_block)
        if_block.m(span, null);
      if (!mounted) {
        dispose = [
          listen(span, "click", function() {
            if (is_function(ctx[20](ctx[0])))
              ctx[20](ctx[0]).apply(this, arguments);
          }),
          listen(span, "touchend", prevent_default(function() {
            if (is_function(ctx[20](ctx[0])))
              ctx[20](ctx[0]).apply(this, arguments);
          }))
        ];
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (ctx[6] === "label" || ctx[7] === "label") {
        if (if_block) {
          if_block.p(ctx, dirty);
        } else {
          if_block = create_if_block_10(ctx);
          if_block.c();
          if_block.m(span, null);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty & 16384 && span_style_value !== (span_style_value = ctx[14] + ": 0%;")) {
        attr(span, "style", span_style_value);
      }
      if (dirty & 131073) {
        toggle_class(span, "selected", ctx[17](ctx[0]));
      }
      if (dirty & 65537) {
        toggle_class(span, "in-range", ctx[16](ctx[0]));
      }
    },
    d(detaching) {
      if (detaching)
        detach(span);
      if (if_block)
        if_block.d();
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block_10(ctx) {
  let span;
  let t_value = ctx[12](ctx[0], 0, 0) + "";
  let t;
  let if_block0 = ctx[10] && create_if_block_12(ctx);
  let if_block1 = ctx[11] && create_if_block_11(ctx);
  return {
    c() {
      span = element("span");
      if (if_block0)
        if_block0.c();
      t = text(t_value);
      if (if_block1)
        if_block1.c();
      attr(span, "class", "pipVal");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      if (if_block0)
        if_block0.m(span, null);
      append(span, t);
      if (if_block1)
        if_block1.m(span, null);
    },
    p(ctx2, dirty) {
      if (ctx2[10]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_12(ctx2);
          if_block0.c();
          if_block0.m(span, t);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (dirty & 4097 && t_value !== (t_value = ctx2[12](ctx2[0], 0, 0) + ""))
        set_data(t, t_value);
      if (ctx2[11]) {
        if (if_block1) {
          if_block1.p(ctx2, dirty);
        } else {
          if_block1 = create_if_block_11(ctx2);
          if_block1.c();
          if_block1.m(span, null);
        }
      } else if (if_block1) {
        if_block1.d(1);
        if_block1 = null;
      }
    },
    d(detaching) {
      if (detaching)
        detach(span);
      if (if_block0)
        if_block0.d();
      if (if_block1)
        if_block1.d();
    }
  };
}
function create_if_block_12(ctx) {
  let span;
  let t;
  return {
    c() {
      span = element("span");
      t = text(ctx[10]);
      attr(span, "class", "pipVal-prefix");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      append(span, t);
    },
    p(ctx2, dirty) {
      if (dirty & 1024)
        set_data(t, ctx2[10]);
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_if_block_11(ctx) {
  let span;
  let t;
  return {
    c() {
      span = element("span");
      t = text(ctx[11]);
      attr(span, "class", "pipVal-suffix");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      append(span, t);
    },
    p(ctx2, dirty) {
      if (dirty & 2048)
        set_data(t, ctx2[11]);
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_if_block_4$2(ctx) {
  let each_1_anchor;
  let each_value = Array(ctx[19] + 1);
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block$1(get_each_context$1(ctx, each_value, i));
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
      if (dirty & 2088515) {
        each_value = Array(ctx2[19] + 1);
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context$1(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block$1(child_ctx);
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
function create_if_block_5(ctx) {
  let span;
  let t;
  let span_style_value;
  let mounted;
  let dispose;
  let if_block = (ctx[6] === "label" || ctx[9] === "label") && create_if_block_6(ctx);
  return {
    c() {
      span = element("span");
      if (if_block)
        if_block.c();
      t = space();
      attr(span, "class", "pip");
      attr(span, "style", span_style_value = ctx[14] + ": " + ctx[15](ctx[18](ctx[29])) + "%;");
      toggle_class(span, "selected", ctx[17](ctx[18](ctx[29])));
      toggle_class(span, "in-range", ctx[16](ctx[18](ctx[29])));
    },
    m(target, anchor) {
      insert(target, span, anchor);
      if (if_block)
        if_block.m(span, null);
      append(span, t);
      if (!mounted) {
        dispose = [
          listen(span, "click", function() {
            if (is_function(ctx[20](ctx[18](ctx[29]))))
              ctx[20](ctx[18](ctx[29])).apply(this, arguments);
          }),
          listen(span, "touchend", prevent_default(function() {
            if (is_function(ctx[20](ctx[18](ctx[29]))))
              ctx[20](ctx[18](ctx[29])).apply(this, arguments);
          }))
        ];
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (ctx[6] === "label" || ctx[9] === "label") {
        if (if_block) {
          if_block.p(ctx, dirty);
        } else {
          if_block = create_if_block_6(ctx);
          if_block.c();
          if_block.m(span, t);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty & 311296 && span_style_value !== (span_style_value = ctx[14] + ": " + ctx[15](ctx[18](ctx[29])) + "%;")) {
        attr(span, "style", span_style_value);
      }
      if (dirty & 393216) {
        toggle_class(span, "selected", ctx[17](ctx[18](ctx[29])));
      }
      if (dirty & 327680) {
        toggle_class(span, "in-range", ctx[16](ctx[18](ctx[29])));
      }
    },
    d(detaching) {
      if (detaching)
        detach(span);
      if (if_block)
        if_block.d();
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block_6(ctx) {
  let span;
  let t_value = ctx[12](ctx[18](ctx[29]), ctx[29], ctx[15](ctx[18](ctx[29]))) + "";
  let t;
  let if_block0 = ctx[10] && create_if_block_8(ctx);
  let if_block1 = ctx[11] && create_if_block_7(ctx);
  return {
    c() {
      span = element("span");
      if (if_block0)
        if_block0.c();
      t = text(t_value);
      if (if_block1)
        if_block1.c();
      attr(span, "class", "pipVal");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      if (if_block0)
        if_block0.m(span, null);
      append(span, t);
      if (if_block1)
        if_block1.m(span, null);
    },
    p(ctx2, dirty) {
      if (ctx2[10]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_8(ctx2);
          if_block0.c();
          if_block0.m(span, t);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (dirty & 299008 && t_value !== (t_value = ctx2[12](ctx2[18](ctx2[29]), ctx2[29], ctx2[15](ctx2[18](ctx2[29]))) + ""))
        set_data(t, t_value);
      if (ctx2[11]) {
        if (if_block1) {
          if_block1.p(ctx2, dirty);
        } else {
          if_block1 = create_if_block_7(ctx2);
          if_block1.c();
          if_block1.m(span, null);
        }
      } else if (if_block1) {
        if_block1.d(1);
        if_block1 = null;
      }
    },
    d(detaching) {
      if (detaching)
        detach(span);
      if (if_block0)
        if_block0.d();
      if (if_block1)
        if_block1.d();
    }
  };
}
function create_if_block_8(ctx) {
  let span;
  let t;
  return {
    c() {
      span = element("span");
      t = text(ctx[10]);
      attr(span, "class", "pipVal-prefix");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      append(span, t);
    },
    p(ctx2, dirty) {
      if (dirty & 1024)
        set_data(t, ctx2[10]);
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_if_block_7(ctx) {
  let span;
  let t;
  return {
    c() {
      span = element("span");
      t = text(ctx[11]);
      attr(span, "class", "pipVal-suffix");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      append(span, t);
    },
    p(ctx2, dirty) {
      if (dirty & 2048)
        set_data(t, ctx2[11]);
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_each_block$1(ctx) {
  let show_if = ctx[18](ctx[29]) !== ctx[0] && ctx[18](ctx[29]) !== ctx[1];
  let if_block_anchor;
  let if_block = show_if && create_if_block_5(ctx);
  return {
    c() {
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if (if_block)
        if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 262147)
        show_if = ctx2[18](ctx2[29]) !== ctx2[0] && ctx2[18](ctx2[29]) !== ctx2[1];
      if (show_if) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block_5(ctx2);
          if_block.c();
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
    d(detaching) {
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block$4(ctx) {
  let span;
  let span_style_value;
  let mounted;
  let dispose;
  let if_block = (ctx[6] === "label" || ctx[8] === "label") && create_if_block_1$2(ctx);
  return {
    c() {
      span = element("span");
      if (if_block)
        if_block.c();
      attr(span, "class", "pip last");
      attr(span, "style", span_style_value = ctx[14] + ": 100%;");
      toggle_class(span, "selected", ctx[17](ctx[1]));
      toggle_class(span, "in-range", ctx[16](ctx[1]));
    },
    m(target, anchor) {
      insert(target, span, anchor);
      if (if_block)
        if_block.m(span, null);
      if (!mounted) {
        dispose = [
          listen(span, "click", function() {
            if (is_function(ctx[20](ctx[1])))
              ctx[20](ctx[1]).apply(this, arguments);
          }),
          listen(span, "touchend", prevent_default(function() {
            if (is_function(ctx[20](ctx[1])))
              ctx[20](ctx[1]).apply(this, arguments);
          }))
        ];
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (ctx[6] === "label" || ctx[8] === "label") {
        if (if_block) {
          if_block.p(ctx, dirty);
        } else {
          if_block = create_if_block_1$2(ctx);
          if_block.c();
          if_block.m(span, null);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty & 16384 && span_style_value !== (span_style_value = ctx[14] + ": 100%;")) {
        attr(span, "style", span_style_value);
      }
      if (dirty & 131074) {
        toggle_class(span, "selected", ctx[17](ctx[1]));
      }
      if (dirty & 65538) {
        toggle_class(span, "in-range", ctx[16](ctx[1]));
      }
    },
    d(detaching) {
      if (detaching)
        detach(span);
      if (if_block)
        if_block.d();
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block_1$2(ctx) {
  let span;
  let t_value = ctx[12](ctx[1], ctx[19], 100) + "";
  let t;
  let if_block0 = ctx[10] && create_if_block_3$2(ctx);
  let if_block1 = ctx[11] && create_if_block_2$2(ctx);
  return {
    c() {
      span = element("span");
      if (if_block0)
        if_block0.c();
      t = text(t_value);
      if (if_block1)
        if_block1.c();
      attr(span, "class", "pipVal");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      if (if_block0)
        if_block0.m(span, null);
      append(span, t);
      if (if_block1)
        if_block1.m(span, null);
    },
    p(ctx2, dirty) {
      if (ctx2[10]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_3$2(ctx2);
          if_block0.c();
          if_block0.m(span, t);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (dirty & 528386 && t_value !== (t_value = ctx2[12](ctx2[1], ctx2[19], 100) + ""))
        set_data(t, t_value);
      if (ctx2[11]) {
        if (if_block1) {
          if_block1.p(ctx2, dirty);
        } else {
          if_block1 = create_if_block_2$2(ctx2);
          if_block1.c();
          if_block1.m(span, null);
        }
      } else if (if_block1) {
        if_block1.d(1);
        if_block1 = null;
      }
    },
    d(detaching) {
      if (detaching)
        detach(span);
      if (if_block0)
        if_block0.d();
      if (if_block1)
        if_block1.d();
    }
  };
}
function create_if_block_3$2(ctx) {
  let span;
  let t;
  return {
    c() {
      span = element("span");
      t = text(ctx[10]);
      attr(span, "class", "pipVal-prefix");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      append(span, t);
    },
    p(ctx2, dirty) {
      if (dirty & 1024)
        set_data(t, ctx2[10]);
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_if_block_2$2(ctx) {
  let span;
  let t;
  return {
    c() {
      span = element("span");
      t = text(ctx[11]);
      attr(span, "class", "pipVal-suffix");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      append(span, t);
    },
    p(ctx2, dirty) {
      if (dirty & 2048)
        set_data(t, ctx2[11]);
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_fragment$4(ctx) {
  let div;
  let t0;
  let t1;
  let if_block0 = (ctx[6] && ctx[7] !== false || ctx[7]) && create_if_block_9(ctx);
  let if_block1 = (ctx[6] && ctx[9] !== false || ctx[9]) && create_if_block_4$2(ctx);
  let if_block2 = (ctx[6] && ctx[8] !== false || ctx[8]) && create_if_block$4(ctx);
  return {
    c() {
      div = element("div");
      if (if_block0)
        if_block0.c();
      t0 = space();
      if (if_block1)
        if_block1.c();
      t1 = space();
      if (if_block2)
        if_block2.c();
      attr(div, "class", "rangePips");
      toggle_class(div, "disabled", ctx[5]);
      toggle_class(div, "hoverable", ctx[4]);
      toggle_class(div, "vertical", ctx[2]);
      toggle_class(div, "reversed", ctx[3]);
      toggle_class(div, "focus", ctx[13]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if (if_block0)
        if_block0.m(div, null);
      append(div, t0);
      if (if_block1)
        if_block1.m(div, null);
      append(div, t1);
      if (if_block2)
        if_block2.m(div, null);
    },
    p(ctx2, [dirty]) {
      if (ctx2[6] && ctx2[7] !== false || ctx2[7]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_9(ctx2);
          if_block0.c();
          if_block0.m(div, t0);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (ctx2[6] && ctx2[9] !== false || ctx2[9]) {
        if (if_block1) {
          if_block1.p(ctx2, dirty);
        } else {
          if_block1 = create_if_block_4$2(ctx2);
          if_block1.c();
          if_block1.m(div, t1);
        }
      } else if (if_block1) {
        if_block1.d(1);
        if_block1 = null;
      }
      if (ctx2[6] && ctx2[8] !== false || ctx2[8]) {
        if (if_block2) {
          if_block2.p(ctx2, dirty);
        } else {
          if_block2 = create_if_block$4(ctx2);
          if_block2.c();
          if_block2.m(div, null);
        }
      } else if (if_block2) {
        if_block2.d(1);
        if_block2 = null;
      }
      if (dirty & 32) {
        toggle_class(div, "disabled", ctx2[5]);
      }
      if (dirty & 16) {
        toggle_class(div, "hoverable", ctx2[4]);
      }
      if (dirty & 4) {
        toggle_class(div, "vertical", ctx2[2]);
      }
      if (dirty & 8) {
        toggle_class(div, "reversed", ctx2[3]);
      }
      if (dirty & 8192) {
        toggle_class(div, "focus", ctx2[13]);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
      if (if_block0)
        if_block0.d();
      if (if_block1)
        if_block1.d();
      if (if_block2)
        if_block2.d();
    }
  };
}
function instance$4($$self, $$props, $$invalidate) {
  let pipStep;
  let pipCount;
  let pipVal;
  let isSelected;
  let inRange;
  let { range = false } = $$props;
  let { min = 0 } = $$props;
  let { max = 100 } = $$props;
  let { step = 1 } = $$props;
  let { values = [(max + min) / 2] } = $$props;
  let { vertical = false } = $$props;
  let { reversed = false } = $$props;
  let { hoverable = true } = $$props;
  let { disabled = false } = $$props;
  let { pipstep = void 0 } = $$props;
  let { all = true } = $$props;
  let { first = void 0 } = $$props;
  let { last = void 0 } = $$props;
  let { rest = void 0 } = $$props;
  let { prefix = "" } = $$props;
  let { suffix = "" } = $$props;
  let { formatter = (v, i) => v } = $$props;
  let { focus = void 0 } = $$props;
  let { orientationStart = void 0 } = $$props;
  let { percentOf = void 0 } = $$props;
  let { moveHandle = void 0 } = $$props;
  function labelClick(val) {
    moveHandle(void 0, val);
  }
  $$self.$$set = ($$props2) => {
    if ("range" in $$props2)
      $$invalidate(21, range = $$props2.range);
    if ("min" in $$props2)
      $$invalidate(0, min = $$props2.min);
    if ("max" in $$props2)
      $$invalidate(1, max = $$props2.max);
    if ("step" in $$props2)
      $$invalidate(22, step = $$props2.step);
    if ("values" in $$props2)
      $$invalidate(23, values = $$props2.values);
    if ("vertical" in $$props2)
      $$invalidate(2, vertical = $$props2.vertical);
    if ("reversed" in $$props2)
      $$invalidate(3, reversed = $$props2.reversed);
    if ("hoverable" in $$props2)
      $$invalidate(4, hoverable = $$props2.hoverable);
    if ("disabled" in $$props2)
      $$invalidate(5, disabled = $$props2.disabled);
    if ("pipstep" in $$props2)
      $$invalidate(24, pipstep = $$props2.pipstep);
    if ("all" in $$props2)
      $$invalidate(6, all = $$props2.all);
    if ("first" in $$props2)
      $$invalidate(7, first = $$props2.first);
    if ("last" in $$props2)
      $$invalidate(8, last = $$props2.last);
    if ("rest" in $$props2)
      $$invalidate(9, rest = $$props2.rest);
    if ("prefix" in $$props2)
      $$invalidate(10, prefix = $$props2.prefix);
    if ("suffix" in $$props2)
      $$invalidate(11, suffix = $$props2.suffix);
    if ("formatter" in $$props2)
      $$invalidate(12, formatter = $$props2.formatter);
    if ("focus" in $$props2)
      $$invalidate(13, focus = $$props2.focus);
    if ("orientationStart" in $$props2)
      $$invalidate(14, orientationStart = $$props2.orientationStart);
    if ("percentOf" in $$props2)
      $$invalidate(15, percentOf = $$props2.percentOf);
    if ("moveHandle" in $$props2)
      $$invalidate(25, moveHandle = $$props2.moveHandle);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 20971527) {
      $$invalidate(26, pipStep = pipstep || ((max - min) / step >= (vertical ? 50 : 100) ? (max - min) / (vertical ? 10 : 20) : 1));
    }
    if ($$self.$$.dirty & 71303171) {
      $$invalidate(19, pipCount = parseInt((max - min) / (step * pipStep), 10));
    }
    if ($$self.$$.dirty & 71303169) {
      $$invalidate(18, pipVal = function(val) {
        return min + val * step * pipStep;
      });
    }
    if ($$self.$$.dirty & 8388608) {
      $$invalidate(17, isSelected = function(val) {
        return values.some((v) => v === val);
      });
    }
    if ($$self.$$.dirty & 10485760) {
      $$invalidate(16, inRange = function(val) {
        if (range === "min") {
          return values[0] > val;
        } else if (range === "max") {
          return values[0] < val;
        } else if (range) {
          return values[0] < val && values[1] > val;
        }
      });
    }
  };
  return [
    min,
    max,
    vertical,
    reversed,
    hoverable,
    disabled,
    all,
    first,
    last,
    rest,
    prefix,
    suffix,
    formatter,
    focus,
    orientationStart,
    percentOf,
    inRange,
    isSelected,
    pipVal,
    pipCount,
    labelClick,
    range,
    step,
    values,
    pipstep,
    moveHandle,
    pipStep
  ];
}
class RangePips extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$4, create_fragment$4, safe_not_equal, {
      range: 21,
      min: 0,
      max: 1,
      step: 22,
      values: 23,
      vertical: 2,
      reversed: 3,
      hoverable: 4,
      disabled: 5,
      pipstep: 24,
      all: 6,
      first: 7,
      last: 8,
      rest: 9,
      prefix: 10,
      suffix: 11,
      formatter: 12,
      focus: 13,
      orientationStart: 14,
      percentOf: 15,
      moveHandle: 25
    });
  }
}
var RangeSlider_svelte_svelte_type_style_lang = "";
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[63] = list[i];
  child_ctx[65] = i;
  return child_ctx;
}
function create_if_block_2$1(ctx) {
  let span;
  let t_value = ctx[21](ctx[63], ctx[65], ctx[23](ctx[63])) + "";
  let t;
  let if_block0 = ctx[18] && create_if_block_4$1(ctx);
  let if_block1 = ctx[19] && create_if_block_3$1(ctx);
  return {
    c() {
      span = element("span");
      if (if_block0)
        if_block0.c();
      t = text(t_value);
      if (if_block1)
        if_block1.c();
      attr(span, "class", "rangeFloat");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      if (if_block0)
        if_block0.m(span, null);
      append(span, t);
      if (if_block1)
        if_block1.m(span, null);
    },
    p(ctx2, dirty) {
      if (ctx2[18]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_4$1(ctx2);
          if_block0.c();
          if_block0.m(span, t);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (dirty[0] & 10485761 && t_value !== (t_value = ctx2[21](ctx2[63], ctx2[65], ctx2[23](ctx2[63])) + ""))
        set_data(t, t_value);
      if (ctx2[19]) {
        if (if_block1) {
          if_block1.p(ctx2, dirty);
        } else {
          if_block1 = create_if_block_3$1(ctx2);
          if_block1.c();
          if_block1.m(span, null);
        }
      } else if (if_block1) {
        if_block1.d(1);
        if_block1 = null;
      }
    },
    d(detaching) {
      if (detaching)
        detach(span);
      if (if_block0)
        if_block0.d();
      if (if_block1)
        if_block1.d();
    }
  };
}
function create_if_block_4$1(ctx) {
  let span;
  let t;
  return {
    c() {
      span = element("span");
      t = text(ctx[18]);
      attr(span, "class", "rangeFloat-prefix");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      append(span, t);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 262144)
        set_data(t, ctx2[18]);
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_if_block_3$1(ctx) {
  let span;
  let t;
  return {
    c() {
      span = element("span");
      t = text(ctx[19]);
      attr(span, "class", "rangeFloat-suffix");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      append(span, t);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 524288)
        set_data(t, ctx2[19]);
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_each_block(ctx) {
  let span1;
  let span0;
  let t;
  let span1_data_handle_value;
  let span1_style_value;
  let span1_aria_valuemin_value;
  let span1_aria_valuemax_value;
  let span1_aria_valuenow_value;
  let span1_aria_valuetext_value;
  let span1_aria_orientation_value;
  let span1_tabindex_value;
  let mounted;
  let dispose;
  let if_block = ctx[7] && create_if_block_2$1(ctx);
  return {
    c() {
      span1 = element("span");
      span0 = element("span");
      t = space();
      if (if_block)
        if_block.c();
      attr(span0, "class", "rangeNub");
      attr(span1, "role", "slider");
      attr(span1, "class", "rangeHandle");
      attr(span1, "data-handle", span1_data_handle_value = ctx[65]);
      attr(span1, "style", span1_style_value = ctx[28] + ": " + ctx[29][ctx[65]] + "%; z-index: " + (ctx[26] === ctx[65] ? 3 : 2) + ";");
      attr(span1, "aria-valuemin", span1_aria_valuemin_value = ctx[2] === true && ctx[65] === 1 ? ctx[0][0] : ctx[3]);
      attr(span1, "aria-valuemax", span1_aria_valuemax_value = ctx[2] === true && ctx[65] === 0 ? ctx[0][1] : ctx[4]);
      attr(span1, "aria-valuenow", span1_aria_valuenow_value = ctx[63]);
      attr(span1, "aria-valuetext", span1_aria_valuetext_value = "" + (ctx[18] + ctx[21](ctx[63], ctx[65], ctx[23](ctx[63])) + ctx[19]));
      attr(span1, "aria-orientation", span1_aria_orientation_value = ctx[6] ? "vertical" : "horizontal");
      attr(span1, "aria-disabled", ctx[10]);
      attr(span1, "disabled", ctx[10]);
      attr(span1, "tabindex", span1_tabindex_value = ctx[10] ? -1 : 0);
      toggle_class(span1, "active", ctx[24] && ctx[26] === ctx[65]);
      toggle_class(span1, "press", ctx[25] && ctx[26] === ctx[65]);
    },
    m(target, anchor) {
      insert(target, span1, anchor);
      append(span1, span0);
      append(span1, t);
      if (if_block)
        if_block.m(span1, null);
      if (!mounted) {
        dispose = [
          listen(span1, "blur", ctx[33]),
          listen(span1, "focus", ctx[34]),
          listen(span1, "keydown", ctx[35])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (ctx2[7]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block_2$1(ctx2);
          if_block.c();
          if_block.m(span1, null);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty[0] & 872415232 && span1_style_value !== (span1_style_value = ctx2[28] + ": " + ctx2[29][ctx2[65]] + "%; z-index: " + (ctx2[26] === ctx2[65] ? 3 : 2) + ";")) {
        attr(span1, "style", span1_style_value);
      }
      if (dirty[0] & 13 && span1_aria_valuemin_value !== (span1_aria_valuemin_value = ctx2[2] === true && ctx2[65] === 1 ? ctx2[0][0] : ctx2[3])) {
        attr(span1, "aria-valuemin", span1_aria_valuemin_value);
      }
      if (dirty[0] & 21 && span1_aria_valuemax_value !== (span1_aria_valuemax_value = ctx2[2] === true && ctx2[65] === 0 ? ctx2[0][1] : ctx2[4])) {
        attr(span1, "aria-valuemax", span1_aria_valuemax_value);
      }
      if (dirty[0] & 1 && span1_aria_valuenow_value !== (span1_aria_valuenow_value = ctx2[63])) {
        attr(span1, "aria-valuenow", span1_aria_valuenow_value);
      }
      if (dirty[0] & 11272193 && span1_aria_valuetext_value !== (span1_aria_valuetext_value = "" + (ctx2[18] + ctx2[21](ctx2[63], ctx2[65], ctx2[23](ctx2[63])) + ctx2[19]))) {
        attr(span1, "aria-valuetext", span1_aria_valuetext_value);
      }
      if (dirty[0] & 64 && span1_aria_orientation_value !== (span1_aria_orientation_value = ctx2[6] ? "vertical" : "horizontal")) {
        attr(span1, "aria-orientation", span1_aria_orientation_value);
      }
      if (dirty[0] & 1024) {
        attr(span1, "aria-disabled", ctx2[10]);
      }
      if (dirty[0] & 1024) {
        attr(span1, "disabled", ctx2[10]);
      }
      if (dirty[0] & 1024 && span1_tabindex_value !== (span1_tabindex_value = ctx2[10] ? -1 : 0)) {
        attr(span1, "tabindex", span1_tabindex_value);
      }
      if (dirty[0] & 83886080) {
        toggle_class(span1, "active", ctx2[24] && ctx2[26] === ctx2[65]);
      }
      if (dirty[0] & 100663296) {
        toggle_class(span1, "press", ctx2[25] && ctx2[26] === ctx2[65]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(span1);
      if (if_block)
        if_block.d();
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block_1$1(ctx) {
  let span;
  let span_style_value;
  return {
    c() {
      span = element("span");
      attr(span, "class", "rangeBar");
      attr(span, "style", span_style_value = ctx[28] + ": " + ctx[31](ctx[29]) + "%; " + ctx[27] + ": " + ctx[32](ctx[29]) + "%;");
    },
    m(target, anchor) {
      insert(target, span, anchor);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 939524096 && span_style_value !== (span_style_value = ctx2[28] + ": " + ctx2[31](ctx2[29]) + "%; " + ctx2[27] + ": " + ctx2[32](ctx2[29]) + "%;")) {
        attr(span, "style", span_style_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_if_block$3(ctx) {
  let rangepips;
  let current;
  rangepips = new RangePips({
    props: {
      values: ctx[0],
      min: ctx[3],
      max: ctx[4],
      step: ctx[5],
      range: ctx[2],
      vertical: ctx[6],
      reversed: ctx[8],
      orientationStart: ctx[28],
      hoverable: ctx[9],
      disabled: ctx[10],
      all: ctx[13],
      first: ctx[14],
      last: ctx[15],
      rest: ctx[16],
      pipstep: ctx[12],
      prefix: ctx[18],
      suffix: ctx[19],
      formatter: ctx[20],
      focus: ctx[24],
      percentOf: ctx[23],
      moveHandle: ctx[30]
    }
  });
  return {
    c() {
      create_component(rangepips.$$.fragment);
    },
    m(target, anchor) {
      mount_component(rangepips, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const rangepips_changes = {};
      if (dirty[0] & 1)
        rangepips_changes.values = ctx2[0];
      if (dirty[0] & 8)
        rangepips_changes.min = ctx2[3];
      if (dirty[0] & 16)
        rangepips_changes.max = ctx2[4];
      if (dirty[0] & 32)
        rangepips_changes.step = ctx2[5];
      if (dirty[0] & 4)
        rangepips_changes.range = ctx2[2];
      if (dirty[0] & 64)
        rangepips_changes.vertical = ctx2[6];
      if (dirty[0] & 256)
        rangepips_changes.reversed = ctx2[8];
      if (dirty[0] & 268435456)
        rangepips_changes.orientationStart = ctx2[28];
      if (dirty[0] & 512)
        rangepips_changes.hoverable = ctx2[9];
      if (dirty[0] & 1024)
        rangepips_changes.disabled = ctx2[10];
      if (dirty[0] & 8192)
        rangepips_changes.all = ctx2[13];
      if (dirty[0] & 16384)
        rangepips_changes.first = ctx2[14];
      if (dirty[0] & 32768)
        rangepips_changes.last = ctx2[15];
      if (dirty[0] & 65536)
        rangepips_changes.rest = ctx2[16];
      if (dirty[0] & 4096)
        rangepips_changes.pipstep = ctx2[12];
      if (dirty[0] & 262144)
        rangepips_changes.prefix = ctx2[18];
      if (dirty[0] & 524288)
        rangepips_changes.suffix = ctx2[19];
      if (dirty[0] & 1048576)
        rangepips_changes.formatter = ctx2[20];
      if (dirty[0] & 16777216)
        rangepips_changes.focus = ctx2[24];
      if (dirty[0] & 8388608)
        rangepips_changes.percentOf = ctx2[23];
      rangepips.$set(rangepips_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(rangepips.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(rangepips.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(rangepips, detaching);
    }
  };
}
function create_fragment$3(ctx) {
  let div;
  let t0;
  let t1;
  let current;
  let mounted;
  let dispose;
  let each_value = ctx[0];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  let if_block0 = ctx[2] && create_if_block_1$1(ctx);
  let if_block1 = ctx[11] && create_if_block$3(ctx);
  return {
    c() {
      div = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      t0 = space();
      if (if_block0)
        if_block0.c();
      t1 = space();
      if (if_block1)
        if_block1.c();
      attr(div, "id", ctx[17]);
      attr(div, "class", "rangeSlider");
      toggle_class(div, "range", ctx[2]);
      toggle_class(div, "disabled", ctx[10]);
      toggle_class(div, "hoverable", ctx[9]);
      toggle_class(div, "vertical", ctx[6]);
      toggle_class(div, "reversed", ctx[8]);
      toggle_class(div, "focus", ctx[24]);
      toggle_class(div, "min", ctx[2] === "min");
      toggle_class(div, "max", ctx[2] === "max");
      toggle_class(div, "pips", ctx[11]);
      toggle_class(div, "pip-labels", ctx[13] === "label" || ctx[14] === "label" || ctx[15] === "label" || ctx[16] === "label");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
      append(div, t0);
      if (if_block0)
        if_block0.m(div, null);
      append(div, t1);
      if (if_block1)
        if_block1.m(div, null);
      ctx[49](div);
      current = true;
      if (!mounted) {
        dispose = [
          listen(window, "mousedown", ctx[38]),
          listen(window, "touchstart", ctx[38]),
          listen(window, "mousemove", ctx[39]),
          listen(window, "touchmove", ctx[39]),
          listen(window, "mouseup", ctx[40]),
          listen(window, "touchend", ctx[41]),
          listen(window, "keydown", ctx[42]),
          listen(div, "mousedown", ctx[36]),
          listen(div, "mouseup", ctx[37]),
          listen(div, "touchstart", prevent_default(ctx[36])),
          listen(div, "touchend", prevent_default(ctx[37]))
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty[0] & 934020317 | dirty[1] & 28) {
        each_value = ctx2[0];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(div, t0);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
      if (ctx2[2]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_1$1(ctx2);
          if_block0.c();
          if_block0.m(div, t1);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (ctx2[11]) {
        if (if_block1) {
          if_block1.p(ctx2, dirty);
          if (dirty[0] & 2048) {
            transition_in(if_block1, 1);
          }
        } else {
          if_block1 = create_if_block$3(ctx2);
          if_block1.c();
          transition_in(if_block1, 1);
          if_block1.m(div, null);
        }
      } else if (if_block1) {
        group_outros();
        transition_out(if_block1, 1, 1, () => {
          if_block1 = null;
        });
        check_outros();
      }
      if (!current || dirty[0] & 131072) {
        attr(div, "id", ctx2[17]);
      }
      if (dirty[0] & 4) {
        toggle_class(div, "range", ctx2[2]);
      }
      if (dirty[0] & 1024) {
        toggle_class(div, "disabled", ctx2[10]);
      }
      if (dirty[0] & 512) {
        toggle_class(div, "hoverable", ctx2[9]);
      }
      if (dirty[0] & 64) {
        toggle_class(div, "vertical", ctx2[6]);
      }
      if (dirty[0] & 256) {
        toggle_class(div, "reversed", ctx2[8]);
      }
      if (dirty[0] & 16777216) {
        toggle_class(div, "focus", ctx2[24]);
      }
      if (dirty[0] & 4) {
        toggle_class(div, "min", ctx2[2] === "min");
      }
      if (dirty[0] & 4) {
        toggle_class(div, "max", ctx2[2] === "max");
      }
      if (dirty[0] & 2048) {
        toggle_class(div, "pips", ctx2[11]);
      }
      if (dirty[0] & 122880) {
        toggle_class(div, "pip-labels", ctx2[13] === "label" || ctx2[14] === "label" || ctx2[15] === "label" || ctx2[16] === "label");
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block1);
      current = true;
    },
    o(local) {
      transition_out(if_block1);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_each(each_blocks, detaching);
      if (if_block0)
        if_block0.d();
      if (if_block1)
        if_block1.d();
      ctx[49](null);
      mounted = false;
      run_all(dispose);
    }
  };
}
function index(el) {
  if (!el)
    return -1;
  var i = 0;
  while (el = el.previousElementSibling) {
    i++;
  }
  return i;
}
function normalisedClient(e) {
  if (e.type.includes("touch")) {
    return e.touches[0];
  } else {
    return e;
  }
}
function instance$3($$self, $$props, $$invalidate) {
  let percentOf;
  let clampValue;
  let alignValueToStep;
  let orientationStart;
  let orientationEnd;
  let $springPositions, $$unsubscribe_springPositions = noop, $$subscribe_springPositions = () => ($$unsubscribe_springPositions(), $$unsubscribe_springPositions = subscribe(springPositions, ($$value) => $$invalidate(29, $springPositions = $$value)), springPositions);
  $$self.$$.on_destroy.push(() => $$unsubscribe_springPositions());
  let { slider } = $$props;
  let { range = false } = $$props;
  let { pushy = false } = $$props;
  let { min = 0 } = $$props;
  let { max = 100 } = $$props;
  let { step = 1 } = $$props;
  let { values = [(max + min) / 2] } = $$props;
  let { vertical = false } = $$props;
  let { float = false } = $$props;
  let { reversed = false } = $$props;
  let { hoverable = true } = $$props;
  let { disabled = false } = $$props;
  let { pips = false } = $$props;
  let { pipstep = void 0 } = $$props;
  let { all = void 0 } = $$props;
  let { first = void 0 } = $$props;
  let { last = void 0 } = $$props;
  let { rest = void 0 } = $$props;
  let { id = void 0 } = $$props;
  let { prefix = "" } = $$props;
  let { suffix = "" } = $$props;
  let { formatter = (v, i, p) => v } = $$props;
  let { handleFormatter = formatter } = $$props;
  let { precision = 2 } = $$props;
  let { springValues = { stiffness: 0.15, damping: 0.4 } } = $$props;
  const dispatch = createEventDispatcher();
  let valueLength = 0;
  let focus = false;
  let handleActivated = false;
  let handlePressed = false;
  let keyboardActive = false;
  let activeHandle = values.length - 1;
  let startValue;
  let previousValue;
  let springPositions;
  function targetIsHandle(el) {
    const handles = slider.querySelectorAll(".handle");
    const isHandle = Array.prototype.includes.call(handles, el);
    const isChild = Array.prototype.some.call(handles, (e) => e.contains(el));
    return isHandle || isChild;
  }
  function trimRange(values2) {
    if (range === "min" || range === "max") {
      return values2.slice(0, 1);
    } else if (range) {
      return values2.slice(0, 2);
    } else {
      return values2;
    }
  }
  function getSliderDimensions() {
    return slider.getBoundingClientRect();
  }
  function getClosestHandle(clientPos) {
    const dims = getSliderDimensions();
    let handlePos = 0;
    let handlePercent = 0;
    let handleVal = 0;
    if (vertical) {
      handlePos = clientPos.clientY - dims.top;
      handlePercent = handlePos / dims.height * 100;
      handlePercent = reversed ? handlePercent : 100 - handlePercent;
    } else {
      handlePos = clientPos.clientX - dims.left;
      handlePercent = handlePos / dims.width * 100;
      handlePercent = reversed ? 100 - handlePercent : handlePercent;
    }
    handleVal = (max - min) / 100 * handlePercent + min;
    let closest;
    if (range === true && values[0] === values[1]) {
      if (handleVal > values[1]) {
        return 1;
      } else {
        return 0;
      }
    } else {
      closest = values.indexOf([...values].sort((a, b) => Math.abs(handleVal - a) - Math.abs(handleVal - b))[0]);
    }
    return closest;
  }
  function handleInteract(clientPos) {
    const dims = getSliderDimensions();
    let handlePos = 0;
    let handlePercent = 0;
    let handleVal = 0;
    if (vertical) {
      handlePos = clientPos.clientY - dims.top;
      handlePercent = handlePos / dims.height * 100;
      handlePercent = reversed ? handlePercent : 100 - handlePercent;
    } else {
      handlePos = clientPos.clientX - dims.left;
      handlePercent = handlePos / dims.width * 100;
      handlePercent = reversed ? 100 - handlePercent : handlePercent;
    }
    handleVal = (max - min) / 100 * handlePercent + min;
    moveHandle(activeHandle, handleVal);
  }
  function moveHandle(index2, value) {
    value = alignValueToStep(value);
    if (typeof index2 === "undefined") {
      index2 = activeHandle;
    }
    if (range) {
      if (index2 === 0 && value > values[1]) {
        if (pushy) {
          $$invalidate(0, values[1] = value, values);
        } else {
          value = values[1];
        }
      } else if (index2 === 1 && value < values[0]) {
        if (pushy) {
          $$invalidate(0, values[0] = value, values);
        } else {
          value = values[0];
        }
      }
    }
    if (values[index2] !== value) {
      $$invalidate(0, values[index2] = value, values);
    }
    if (previousValue !== value) {
      eChange();
      previousValue = value;
    }
    return value;
  }
  function rangeStart(values2) {
    if (range === "min") {
      return 0;
    } else {
      return values2[0];
    }
  }
  function rangeEnd(values2) {
    if (range === "max") {
      return 0;
    } else if (range === "min") {
      return 100 - values2[0];
    } else {
      return 100 - values2[1];
    }
  }
  function sliderBlurHandle(e) {
    if (keyboardActive) {
      $$invalidate(24, focus = false);
      handleActivated = false;
      $$invalidate(25, handlePressed = false);
    }
  }
  function sliderFocusHandle(e) {
    if (!disabled) {
      $$invalidate(26, activeHandle = index(e.target));
      $$invalidate(24, focus = true);
    }
  }
  function sliderKeydown(e) {
    if (!disabled) {
      const handle = index(e.target);
      let jump = e.ctrlKey || e.metaKey || e.shiftKey ? step * 10 : step;
      let prevent = false;
      switch (e.key) {
        case "PageDown":
          jump *= 10;
        case "ArrowRight":
        case "ArrowUp":
          moveHandle(handle, values[handle] + jump);
          prevent = true;
          break;
        case "PageUp":
          jump *= 10;
        case "ArrowLeft":
        case "ArrowDown":
          moveHandle(handle, values[handle] - jump);
          prevent = true;
          break;
        case "Home":
          moveHandle(handle, min);
          prevent = true;
          break;
        case "End":
          moveHandle(handle, max);
          prevent = true;
          break;
      }
      if (prevent) {
        e.preventDefault();
        e.stopPropagation();
      }
    }
  }
  function sliderInteractStart(e) {
    if (!disabled) {
      const el = e.target;
      const clientPos = normalisedClient(e);
      $$invalidate(24, focus = true);
      handleActivated = true;
      $$invalidate(25, handlePressed = true);
      $$invalidate(26, activeHandle = getClosestHandle(clientPos));
      startValue = previousValue = alignValueToStep(values[activeHandle]);
      eStart();
      if (e.type === "touchstart" && !el.matches(".pipVal")) {
        handleInteract(clientPos);
      }
    }
  }
  function sliderInteractEnd(e) {
    if (e.type === "touchend") {
      eStop();
    }
    $$invalidate(25, handlePressed = false);
  }
  function bodyInteractStart(e) {
    keyboardActive = false;
    if (focus && e.target !== slider && !slider.contains(e.target)) {
      $$invalidate(24, focus = false);
    }
  }
  function bodyInteract(e) {
    if (!disabled) {
      if (handleActivated) {
        handleInteract(normalisedClient(e));
      }
    }
  }
  function bodyMouseUp(e) {
    if (!disabled) {
      const el = e.target;
      if (handleActivated) {
        if (el === slider || slider.contains(el)) {
          $$invalidate(24, focus = true);
          if (!targetIsHandle(el) && !el.matches(".pipVal")) {
            handleInteract(normalisedClient(e));
          }
        }
        eStop();
      }
    }
    handleActivated = false;
    $$invalidate(25, handlePressed = false);
  }
  function bodyTouchEnd(e) {
    handleActivated = false;
    $$invalidate(25, handlePressed = false);
  }
  function bodyKeyDown(e) {
    if (!disabled) {
      if (e.target === slider || slider.contains(e.target)) {
        keyboardActive = true;
      }
    }
  }
  function eStart() {
    !disabled && dispatch("start", {
      activeHandle,
      value: startValue,
      values: values.map((v) => alignValueToStep(v))
    });
  }
  function eStop() {
    !disabled && dispatch("stop", {
      activeHandle,
      startValue,
      value: values[activeHandle],
      values: values.map((v) => alignValueToStep(v))
    });
  }
  function eChange() {
    !disabled && dispatch("change", {
      activeHandle,
      startValue,
      previousValue: typeof previousValue === "undefined" ? startValue : previousValue,
      value: values[activeHandle],
      values: values.map((v) => alignValueToStep(v))
    });
  }
  function div_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      slider = $$value;
      $$invalidate(1, slider);
    });
  }
  $$self.$$set = ($$props2) => {
    if ("slider" in $$props2)
      $$invalidate(1, slider = $$props2.slider);
    if ("range" in $$props2)
      $$invalidate(2, range = $$props2.range);
    if ("pushy" in $$props2)
      $$invalidate(43, pushy = $$props2.pushy);
    if ("min" in $$props2)
      $$invalidate(3, min = $$props2.min);
    if ("max" in $$props2)
      $$invalidate(4, max = $$props2.max);
    if ("step" in $$props2)
      $$invalidate(5, step = $$props2.step);
    if ("values" in $$props2)
      $$invalidate(0, values = $$props2.values);
    if ("vertical" in $$props2)
      $$invalidate(6, vertical = $$props2.vertical);
    if ("float" in $$props2)
      $$invalidate(7, float = $$props2.float);
    if ("reversed" in $$props2)
      $$invalidate(8, reversed = $$props2.reversed);
    if ("hoverable" in $$props2)
      $$invalidate(9, hoverable = $$props2.hoverable);
    if ("disabled" in $$props2)
      $$invalidate(10, disabled = $$props2.disabled);
    if ("pips" in $$props2)
      $$invalidate(11, pips = $$props2.pips);
    if ("pipstep" in $$props2)
      $$invalidate(12, pipstep = $$props2.pipstep);
    if ("all" in $$props2)
      $$invalidate(13, all = $$props2.all);
    if ("first" in $$props2)
      $$invalidate(14, first = $$props2.first);
    if ("last" in $$props2)
      $$invalidate(15, last = $$props2.last);
    if ("rest" in $$props2)
      $$invalidate(16, rest = $$props2.rest);
    if ("id" in $$props2)
      $$invalidate(17, id = $$props2.id);
    if ("prefix" in $$props2)
      $$invalidate(18, prefix = $$props2.prefix);
    if ("suffix" in $$props2)
      $$invalidate(19, suffix = $$props2.suffix);
    if ("formatter" in $$props2)
      $$invalidate(20, formatter = $$props2.formatter);
    if ("handleFormatter" in $$props2)
      $$invalidate(21, handleFormatter = $$props2.handleFormatter);
    if ("precision" in $$props2)
      $$invalidate(44, precision = $$props2.precision);
    if ("springValues" in $$props2)
      $$invalidate(45, springValues = $$props2.springValues);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty[0] & 24) {
      $$invalidate(48, clampValue = function(val) {
        return val <= min ? min : val >= max ? max : val;
      });
    }
    if ($$self.$$.dirty[0] & 56 | $$self.$$.dirty[1] & 139264) {
      $$invalidate(47, alignValueToStep = function(val) {
        if (val <= min) {
          return min;
        } else if (val >= max) {
          return max;
        }
        let remainder = (val - min) % step;
        let aligned = val - remainder;
        if (Math.abs(remainder) * 2 >= step) {
          aligned += remainder > 0 ? step : -step;
        }
        aligned = clampValue(aligned);
        return parseFloat(aligned.toFixed(precision));
      });
    }
    if ($$self.$$.dirty[0] & 24 | $$self.$$.dirty[1] & 8192) {
      $$invalidate(23, percentOf = function(val) {
        let perc = (val - min) / (max - min) * 100;
        if (isNaN(perc) || perc <= 0) {
          return 0;
        } else if (perc >= 100) {
          return 100;
        } else {
          return parseFloat(perc.toFixed(precision));
        }
      });
    }
    if ($$self.$$.dirty[0] & 12582937 | $$self.$$.dirty[1] & 114688) {
      {
        if (!Array.isArray(values)) {
          $$invalidate(0, values = [(max + min) / 2]);
          console.error("'values' prop should be an Array (https://github.com/simeydotme/svelte-range-slider-pips#slider-props)");
        }
        $$invalidate(0, values = trimRange(values.map((v) => alignValueToStep(v))));
        if (valueLength !== values.length) {
          $$subscribe_springPositions($$invalidate(22, springPositions = spring(values.map((v) => percentOf(v)), springValues)));
        } else {
          springPositions.set(values.map((v) => percentOf(v)));
        }
        $$invalidate(46, valueLength = values.length);
      }
    }
    if ($$self.$$.dirty[0] & 320) {
      $$invalidate(28, orientationStart = vertical ? reversed ? "top" : "bottom" : reversed ? "right" : "left");
    }
    if ($$self.$$.dirty[0] & 320) {
      $$invalidate(27, orientationEnd = vertical ? reversed ? "bottom" : "top" : reversed ? "left" : "right");
    }
  };
  return [
    values,
    slider,
    range,
    min,
    max,
    step,
    vertical,
    float,
    reversed,
    hoverable,
    disabled,
    pips,
    pipstep,
    all,
    first,
    last,
    rest,
    id,
    prefix,
    suffix,
    formatter,
    handleFormatter,
    springPositions,
    percentOf,
    focus,
    handlePressed,
    activeHandle,
    orientationEnd,
    orientationStart,
    $springPositions,
    moveHandle,
    rangeStart,
    rangeEnd,
    sliderBlurHandle,
    sliderFocusHandle,
    sliderKeydown,
    sliderInteractStart,
    sliderInteractEnd,
    bodyInteractStart,
    bodyInteract,
    bodyMouseUp,
    bodyTouchEnd,
    bodyKeyDown,
    pushy,
    precision,
    springValues,
    valueLength,
    alignValueToStep,
    clampValue,
    div_binding
  ];
}
class RangeSlider extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$3, create_fragment$3, safe_not_equal, {
      slider: 1,
      range: 2,
      pushy: 43,
      min: 3,
      max: 4,
      step: 5,
      values: 0,
      vertical: 6,
      float: 7,
      reversed: 8,
      hoverable: 9,
      disabled: 10,
      pips: 11,
      pipstep: 12,
      all: 13,
      first: 14,
      last: 15,
      rest: 16,
      id: 17,
      prefix: 18,
      suffix: 19,
      formatter: 20,
      handleFormatter: 21,
      precision: 44,
      springValues: 45
    }, null, [-1, -1, -1]);
  }
}
function create_else_block_1(ctx) {
  var _a;
  let modifyupload;
  let t0;
  let audio;
  let audio_src_value;
  let t1;
  let if_block_anchor;
  let current;
  let mounted;
  let dispose;
  modifyupload = new ModifyUpload({
    props: { editable: true, absolute: false }
  });
  modifyupload.$on("clear", ctx[15]);
  modifyupload.$on("edit", ctx[28]);
  let if_block = ctx[10] === "edit" && ((_a = ctx[11]) == null ? void 0 : _a.duration) && create_if_block_4(ctx);
  return {
    c() {
      create_component(modifyupload.$$.fragment);
      t0 = space();
      audio = element("audio");
      t1 = space();
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
      attr(audio, "class", "w-full h-14 p-2");
      audio.controls = true;
      attr(audio, "preload", "metadata");
      if (!src_url_equal(audio.src, audio_src_value = ctx[1].data))
        attr(audio, "src", audio_src_value);
    },
    m(target, anchor) {
      mount_component(modifyupload, target, anchor);
      insert(target, t0, anchor);
      insert(target, audio, anchor);
      ctx[29](audio);
      insert(target, t1, anchor);
      if (if_block)
        if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
      if (!mounted) {
        dispose = [
          action_destroyer(ctx[16].call(null, audio)),
          listen(audio, "play", ctx[24]),
          listen(audio, "pause", ctx[25]),
          listen(audio, "ended", ctx[26])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      var _a2;
      if (!current || dirty[0] & 2 && !src_url_equal(audio.src, audio_src_value = ctx2[1].data)) {
        attr(audio, "src", audio_src_value);
      }
      if (ctx2[10] === "edit" && ((_a2 = ctx2[11]) == null ? void 0 : _a2.duration)) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty[0] & 3072) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block_4(ctx2);
          if_block.c();
          transition_in(if_block, 1);
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        }
      } else if (if_block) {
        group_outros();
        transition_out(if_block, 1, 1, () => {
          if_block = null;
        });
        check_outros();
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(modifyupload.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(modifyupload.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(modifyupload, detaching);
      if (detaching)
        detach(t0);
      if (detaching)
        detach(audio);
      ctx[29](null);
      if (detaching)
        detach(t1);
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block$2(ctx) {
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  const if_block_creators = [create_if_block_1, create_if_block_3];
  const if_blocks = [];
  function select_block_type_1(ctx2, dirty) {
    if (ctx2[4] === "microphone")
      return 0;
    if (ctx2[4] === "upload")
      return 1;
    return -1;
  }
  if (~(current_block_type_index = select_block_type_1(ctx))) {
    if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  }
  return {
    c() {
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if (~current_block_type_index) {
        if_blocks[current_block_type_index].m(target, anchor);
      }
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type_1(ctx2);
      if (current_block_type_index === previous_block_index) {
        if (~current_block_type_index) {
          if_blocks[current_block_type_index].p(ctx2, dirty);
        }
      } else {
        if (if_block) {
          group_outros();
          transition_out(if_blocks[previous_block_index], 1, 1, () => {
            if_blocks[previous_block_index] = null;
          });
          check_outros();
        }
        if (~current_block_type_index) {
          if_block = if_blocks[current_block_type_index];
          if (!if_block) {
            if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
            if_block.c();
          } else {
            if_block.p(ctx2, dirty);
          }
          transition_in(if_block, 1);
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        } else {
          if_block = null;
        }
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
      if (~current_block_type_index) {
        if_blocks[current_block_type_index].d(detaching);
      }
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block_4(ctx) {
  let range;
  let updating_values;
  let current;
  function range_values_binding(value) {
    ctx[30](value);
  }
  let range_props = { range: true, min: 0, max: 100, step: 1 };
  if (ctx[12] !== void 0) {
    range_props.values = ctx[12];
  }
  range = new RangeSlider({ props: range_props });
  binding_callbacks.push(() => bind(range, "values", range_values_binding));
  range.$on("change", ctx[17]);
  return {
    c() {
      create_component(range.$$.fragment);
    },
    m(target, anchor) {
      mount_component(range, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const range_changes = {};
      if (!updating_values && dirty[0] & 4096) {
        updating_values = true;
        range_changes.values = ctx2[12];
        add_flush_callback(() => updating_values = false);
      }
      range.$set(range_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(range.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(range.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(range, detaching);
    }
  };
}
function create_if_block_3(ctx) {
  let upload;
  let updating_dragging;
  let current;
  function upload_dragging_binding(value) {
    ctx[27](value);
  }
  let upload_props = {
    filetype: "audio/*",
    $$slots: { default: [create_default_slot$1] },
    $$scope: { ctx }
  };
  if (ctx[0] !== void 0) {
    upload_props.dragging = ctx[0];
  }
  upload = new Upload({ props: upload_props });
  binding_callbacks.push(() => bind(upload, "dragging", upload_dragging_binding));
  upload.$on("load", ctx[18]);
  return {
    c() {
      create_component(upload.$$.fragment);
    },
    m(target, anchor) {
      mount_component(upload, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const upload_changes = {};
      if (dirty[0] & 448 | dirty[1] & 512) {
        upload_changes.$$scope = { dirty, ctx: ctx2 };
      }
      if (!updating_dragging && dirty[0] & 1) {
        updating_dragging = true;
        upload_changes.dragging = ctx2[0];
        add_flush_callback(() => updating_dragging = false);
      }
      upload.$set(upload_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(upload.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(upload.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(upload, detaching);
    }
  };
}
function create_if_block_1(ctx) {
  let div;
  function select_block_type_2(ctx2, dirty) {
    if (ctx2[9])
      return create_if_block_2;
    return create_else_block$2;
  }
  let current_block_type = select_block_type_2(ctx);
  let if_block = current_block_type(ctx);
  return {
    c() {
      div = element("div");
      if_block.c();
      attr(div, "class", "mt-6 p-2");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if_block.m(div, null);
    },
    p(ctx2, dirty) {
      if (current_block_type === (current_block_type = select_block_type_2(ctx2)) && if_block) {
        if_block.p(ctx2, dirty);
      } else {
        if_block.d(1);
        if_block = current_block_type(ctx2);
        if (if_block) {
          if_block.c();
          if_block.m(div, null);
        }
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
      if_block.d();
    }
  };
}
function create_default_slot$1(ctx) {
  let div;
  let t0;
  let t1;
  let span;
  let t2;
  let t3;
  let t4;
  let t5;
  let t6;
  return {
    c() {
      div = element("div");
      t0 = text(ctx[6]);
      t1 = space();
      span = element("span");
      t2 = text("- ");
      t3 = text(ctx[7]);
      t4 = text(" -");
      t5 = space();
      t6 = text(ctx[8]);
      attr(span, "class", "text-gray-300");
      attr(div, "class", "flex flex-col");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t0);
      append(div, t1);
      append(div, span);
      append(span, t2);
      append(span, t3);
      append(span, t4);
      append(div, t5);
      append(div, t6);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 64)
        set_data(t0, ctx2[6]);
      if (dirty[0] & 128)
        set_data(t3, ctx2[7]);
      if (dirty[0] & 256)
        set_data(t6, ctx2[8]);
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_else_block$2(ctx) {
  let button;
  let mounted;
  let dispose;
  return {
    c() {
      button = element("button");
      button.innerHTML = `<span class="flex h-1.5 w-1.5 relative mr-2"><span class="relative inline-flex rounded-full h-1.5 w-1.5 bg-red-500"></span></span> 
					<div class="whitespace-nowrap">Record from microphone</div>`;
      attr(button, "class", "gr-button text-gray-800");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      if (!mounted) {
        dispose = listen(button, "click", ctx[13]);
        mounted = true;
      }
    },
    p: noop,
    d(detaching) {
      if (detaching)
        detach(button);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block_2(ctx) {
  let button;
  let mounted;
  let dispose;
  return {
    c() {
      button = element("button");
      button.innerHTML = `<span class="flex h-1.5 w-1.5 relative mr-2 "><span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span> 
						<span class="relative inline-flex rounded-full h-1.5 w-1.5 bg-red-500"></span></span> 
					<div class="whitespace-nowrap text-red-500">Stop recording</div>`;
      attr(button, "class", "gr-button !bg-red-500/10");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      if (!mounted) {
        dispose = listen(button, "click", ctx[14]);
        mounted = true;
      }
    },
    p: noop,
    d(detaching) {
      if (detaching)
        detach(button);
      mounted = false;
      dispose();
    }
  };
}
function create_fragment$2(ctx) {
  let blocklabel;
  let t;
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  blocklabel = new BlockLabel({
    props: {
      show_label: ctx[3],
      Icon: Music,
      label: ctx[2] || "Audio"
    }
  });
  const if_block_creators = [create_if_block$2, create_else_block_1];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[1] === null || ctx2[5])
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(blocklabel.$$.fragment);
      t = space();
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(blocklabel, target, anchor);
      insert(target, t, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const blocklabel_changes = {};
      if (dirty[0] & 8)
        blocklabel_changes.show_label = ctx2[3];
      if (dirty[0] & 4)
        blocklabel_changes.label = ctx2[2] || "Audio";
      blocklabel.$set(blocklabel_changes);
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
        if_block.m(if_block_anchor.parentNode, if_block_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(blocklabel.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(blocklabel.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(blocklabel, detaching);
      if (detaching)
        detach(t);
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
const STREAM_TIMESLICE = 500;
const NUM_HEADER_BYTES = 44;
function blob_to_data_url(blob) {
  return new Promise((fulfill, reject) => {
    let reader = new FileReader();
    reader.onerror = reject;
    reader.onload = () => fulfill(reader.result);
    reader.readAsDataURL(blob);
  });
}
function instance$2($$self, $$props, $$invalidate) {
  let { value = null } = $$props;
  let { label } = $$props;
  let { show_label } = $$props;
  let { name } = $$props;
  let { source } = $$props;
  let { pending = false } = $$props;
  let { streaming = false } = $$props;
  let { drop_text = "Drop an audio file" } = $$props;
  let { or_text = "or" } = $$props;
  let { upload_text = "click to upload" } = $$props;
  let recording = false;
  let recorder;
  let mode = "";
  let header = void 0;
  let pending_stream = [];
  let submit_pending_stream_on_pending_end = false;
  let player;
  let inited = false;
  let crop_values = [0, 100];
  let audio_chunks = [];
  let module_promises;
  function get_modules() {
    module_promises = [
      import("./module.js"),
      import("./module3.js")
    ];
  }
  if (streaming) {
    get_modules();
  }
  const dispatch = createEventDispatcher();
  const dispatch_blob = async (blobs, event) => {
    let audio_blob = new Blob(blobs, { type: "audio/wav" });
    $$invalidate(1, value = {
      data: await blob_to_data_url(audio_blob),
      name
    });
    dispatch(event, value);
  };
  async function prepare_audio() {
    let stream;
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch (err) {
      if (err instanceof DOMException && err.name == "NotAllowedError") {
        dispatch("error", "Please allow access to the microphone for recording.");
        return;
      } else {
        throw err;
      }
    }
    if (stream == null)
      return;
    if (streaming) {
      const [{ MediaRecorder: MediaRecorder2, register }, { connect }] = await Promise.all(module_promises);
      await register(await connect());
      recorder = new MediaRecorder2(stream, { mimeType: "audio/wav" });
      async function handle_chunk(event) {
        let buffer = await event.data.arrayBuffer();
        let payload = new Uint8Array(buffer);
        if (!header) {
          $$invalidate(21, header = new Uint8Array(buffer.slice(0, NUM_HEADER_BYTES)));
          payload = new Uint8Array(buffer.slice(NUM_HEADER_BYTES));
        }
        if (pending) {
          pending_stream.push(payload);
        } else {
          let blobParts = [header].concat(pending_stream, [payload]);
          dispatch_blob(blobParts, "stream");
          $$invalidate(22, pending_stream = []);
        }
      }
      recorder.addEventListener("dataavailable", handle_chunk);
    } else {
      recorder = new MediaRecorder(stream);
      recorder.addEventListener("dataavailable", (event) => {
        audio_chunks.push(event.data);
      });
      recorder.addEventListener("stop", async () => {
        $$invalidate(9, recording = false);
        await dispatch_blob(audio_chunks, "change");
        audio_chunks = [];
      });
    }
    inited = true;
  }
  async function record() {
    $$invalidate(9, recording = true);
    if (!inited)
      await prepare_audio();
    $$invalidate(21, header = void 0);
    if (streaming) {
      recorder.start(STREAM_TIMESLICE);
    } else {
      recorder.start();
    }
  }
  onDestroy(() => {
    if (recorder && recorder.state !== "inactive") {
      recorder.stop();
    }
  });
  const stop = async () => {
    recorder.stop();
    if (streaming) {
      $$invalidate(9, recording = false);
      if (pending) {
        $$invalidate(23, submit_pending_stream_on_pending_end = true);
      }
    }
  };
  function clear() {
    dispatch("change");
    dispatch("clear");
    $$invalidate(10, mode = "");
    $$invalidate(1, value = null);
  }
  function loaded(node) {
    function clamp_playback() {
      const start_time = crop_values[0] / 100 * node.duration;
      const end_time = crop_values[1] / 100 * node.duration;
      if (node.currentTime < start_time) {
        node.currentTime = start_time;
      }
      if (node.currentTime > end_time) {
        node.currentTime = start_time;
        node.pause();
      }
    }
    node.addEventListener("timeupdate", clamp_playback);
    return {
      destroy: () => node.removeEventListener("timeupdate", clamp_playback)
    };
  }
  function handle_change({ detail: { values } }) {
    if (!value)
      return;
    dispatch("change", {
      data: value.data,
      name,
      crop_min: values[0],
      crop_max: values[1]
    });
    dispatch("edit");
  }
  function handle_load({ detail }) {
    $$invalidate(1, value = detail);
    dispatch("change", { data: detail.data, name: detail.name });
    dispatch("upload", detail);
  }
  let { dragging = false } = $$props;
  function play_handler(event) {
    bubble.call(this, $$self, event);
  }
  function pause_handler(event) {
    bubble.call(this, $$self, event);
  }
  function ended_handler(event) {
    bubble.call(this, $$self, event);
  }
  function upload_dragging_binding(value2) {
    dragging = value2;
    $$invalidate(0, dragging);
  }
  const edit_handler = () => $$invalidate(10, mode = "edit");
  function audio_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      player = $$value;
      $$invalidate(11, player);
    });
  }
  function range_values_binding(value2) {
    crop_values = value2;
    $$invalidate(12, crop_values);
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(1, value = $$props2.value);
    if ("label" in $$props2)
      $$invalidate(2, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(3, show_label = $$props2.show_label);
    if ("name" in $$props2)
      $$invalidate(19, name = $$props2.name);
    if ("source" in $$props2)
      $$invalidate(4, source = $$props2.source);
    if ("pending" in $$props2)
      $$invalidate(20, pending = $$props2.pending);
    if ("streaming" in $$props2)
      $$invalidate(5, streaming = $$props2.streaming);
    if ("drop_text" in $$props2)
      $$invalidate(6, drop_text = $$props2.drop_text);
    if ("or_text" in $$props2)
      $$invalidate(7, or_text = $$props2.or_text);
    if ("upload_text" in $$props2)
      $$invalidate(8, upload_text = $$props2.upload_text);
    if ("dragging" in $$props2)
      $$invalidate(0, dragging = $$props2.dragging);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty[0] & 15728640) {
      if (submit_pending_stream_on_pending_end && pending === false) {
        $$invalidate(23, submit_pending_stream_on_pending_end = false);
        if (header && pending_stream) {
          let blobParts = [header].concat(pending_stream);
          $$invalidate(22, pending_stream = []);
          dispatch_blob(blobParts, "stream");
        }
      }
    }
    if ($$self.$$.dirty[0] & 1) {
      dispatch("drag", dragging);
    }
  };
  return [
    dragging,
    value,
    label,
    show_label,
    source,
    streaming,
    drop_text,
    or_text,
    upload_text,
    recording,
    mode,
    player,
    crop_values,
    record,
    stop,
    clear,
    loaded,
    handle_change,
    handle_load,
    name,
    pending,
    header,
    pending_stream,
    submit_pending_stream_on_pending_end,
    play_handler,
    pause_handler,
    ended_handler,
    upload_dragging_binding,
    edit_handler,
    audio_binding,
    range_values_binding
  ];
}
class Audio extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$2, create_fragment$2, safe_not_equal, {
      value: 1,
      label: 2,
      show_label: 3,
      name: 19,
      source: 4,
      pending: 20,
      streaming: 5,
      drop_text: 6,
      or_text: 7,
      upload_text: 8,
      dragging: 0
    }, null, [-1, -1]);
  }
}
function create_else_block$1(ctx) {
  let audio;
  let audio_src_value;
  let mounted;
  let dispose;
  return {
    c() {
      audio = element("audio");
      attr(audio, "class", "w-full h-14 p-2 mt-7");
      audio.controls = true;
      attr(audio, "preload", "metadata");
      if (!src_url_equal(audio.src, audio_src_value = ctx[0].data))
        attr(audio, "src", audio_src_value);
    },
    m(target, anchor) {
      insert(target, audio, anchor);
      if (!mounted) {
        dispose = [
          listen(audio, "play", ctx[4]),
          listen(audio, "pause", ctx[5]),
          listen(audio, "ended", ctx[6])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 1 && !src_url_equal(audio.src, audio_src_value = ctx2[0].data)) {
        attr(audio, "src", audio_src_value);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(audio);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block$1(ctx) {
  let div1;
  let div0;
  let music;
  let current;
  music = new Music({});
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      create_component(music.$$.fragment);
      attr(div0, "class", "h-5 dark:text-white opacity-50");
      attr(div1, "class", "h-full min-h-[8rem] flex justify-center items-center");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      mount_component(music, div0, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(music.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(music.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(music);
    }
  };
}
function create_fragment$1(ctx) {
  let blocklabel;
  let t;
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  blocklabel = new BlockLabel({
    props: {
      show_label: ctx[2],
      Icon: Music,
      label: ctx[1] || "Audio"
    }
  });
  const if_block_creators = [create_if_block$1, create_else_block$1];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[0] === null)
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(blocklabel.$$.fragment);
      t = space();
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(blocklabel, target, anchor);
      insert(target, t, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocklabel_changes = {};
      if (dirty & 4)
        blocklabel_changes.show_label = ctx2[2];
      if (dirty & 2)
        blocklabel_changes.label = ctx2[1] || "Audio";
      blocklabel.$set(blocklabel_changes);
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
        if_block.m(if_block_anchor.parentNode, if_block_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(blocklabel.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(blocklabel.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(blocklabel, detaching);
      if (detaching)
        detach(t);
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { value = null } = $$props;
  let { label } = $$props;
  let { name } = $$props;
  let { show_label } = $$props;
  const dispatch = createEventDispatcher();
  function play_handler(event) {
    bubble.call(this, $$self, event);
  }
  function pause_handler(event) {
    bubble.call(this, $$self, event);
  }
  function ended_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
    if ("name" in $$props2)
      $$invalidate(3, name = $$props2.name);
    if ("show_label" in $$props2)
      $$invalidate(2, show_label = $$props2.show_label);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 9) {
      value && dispatch("change", { name, data: value == null ? void 0 : value.data });
    }
  };
  return [value, label, show_label, name, play_handler, pause_handler, ended_handler];
}
class StaticAudio extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      value: 0,
      label: 1,
      name: 3,
      show_label: 2
    });
  }
}
function create_else_block(ctx) {
  var _a;
  let staticaudio;
  let current;
  staticaudio = new StaticAudio({
    props: {
      show_label: ctx[8],
      value: ctx[11],
      name: ((_a = ctx[11]) == null ? void 0 : _a.name) || "audio_file",
      label: ctx[7]
    }
  });
  return {
    c() {
      create_component(staticaudio.$$.fragment);
    },
    m(target, anchor) {
      mount_component(staticaudio, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      var _a2;
      const staticaudio_changes = {};
      if (dirty & 256)
        staticaudio_changes.show_label = ctx2[8];
      if (dirty & 2048)
        staticaudio_changes.value = ctx2[11];
      if (dirty & 2048)
        staticaudio_changes.name = ((_a2 = ctx2[11]) == null ? void 0 : _a2.name) || "audio_file";
      if (dirty & 128)
        staticaudio_changes.label = ctx2[7];
      staticaudio.$set(staticaudio_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(staticaudio.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(staticaudio.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(staticaudio, detaching);
    }
  };
}
function create_if_block(ctx) {
  let audio;
  let current;
  audio = new Audio({
    props: {
      label: ctx[7],
      show_label: ctx[8],
      value: ctx[11],
      name: ctx[5],
      source: ctx[6],
      pending: ctx[9],
      streaming: ctx[10],
      drop_text: ctx[13]("interface.drop_audio"),
      or_text: ctx[13]("or"),
      upload_text: ctx[13]("interface.click_to_upload")
    }
  });
  audio.$on("change", ctx[18]);
  audio.$on("stream", ctx[19]);
  audio.$on("drag", ctx[20]);
  audio.$on("edit", ctx[21]);
  audio.$on("play", ctx[22]);
  audio.$on("pause", ctx[23]);
  audio.$on("ended", ctx[24]);
  audio.$on("upload", ctx[25]);
  audio.$on("error", ctx[26]);
  return {
    c() {
      create_component(audio.$$.fragment);
    },
    m(target, anchor) {
      mount_component(audio, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const audio_changes = {};
      if (dirty & 128)
        audio_changes.label = ctx2[7];
      if (dirty & 256)
        audio_changes.show_label = ctx2[8];
      if (dirty & 2048)
        audio_changes.value = ctx2[11];
      if (dirty & 32)
        audio_changes.name = ctx2[5];
      if (dirty & 64)
        audio_changes.source = ctx2[6];
      if (dirty & 512)
        audio_changes.pending = ctx2[9];
      if (dirty & 1024)
        audio_changes.streaming = ctx2[10];
      if (dirty & 8192)
        audio_changes.drop_text = ctx2[13]("interface.drop_audio");
      if (dirty & 8192)
        audio_changes.or_text = ctx2[13]("or");
      if (dirty & 8192)
        audio_changes.upload_text = ctx2[13]("interface.click_to_upload");
      audio.$set(audio_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(audio.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(audio.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(audio, detaching);
    }
  };
}
function create_default_slot(ctx) {
  let statustracker;
  let t;
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  const statustracker_spread_levels = [ctx[1]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  const if_block_creators = [create_if_block, create_else_block];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[4] === "dynamic")
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t = space();
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 2 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[1])]) : {};
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
      transition_in(statustracker.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      variant: ctx[4] === "dynamic" && ctx[0] === null && ctx[6] === "upload" ? "dashed" : "solid",
      color: ctx[12] ? "green" : "grey",
      padding: false,
      elem_id: ctx[2],
      visible: ctx[3],
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
      if (dirty & 81)
        block_changes.variant = ctx2[4] === "dynamic" && ctx2[0] === null && ctx2[6] === "upload" ? "dashed" : "solid";
      if (dirty & 4096)
        block_changes.color = ctx2[12] ? "green" : "grey";
      if (dirty & 4)
        block_changes.elem_id = ctx2[2];
      if (dirty & 8)
        block_changes.visible = ctx2[3];
      if (dirty & 134234099) {
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
  component_subscribe($$self, X, ($$value) => $$invalidate(13, $_ = $$value));
  let { style = {} } = $$props;
  const dispatch = createEventDispatcher();
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { mode } = $$props;
  let { value = null } = $$props;
  let { name } = $$props;
  let { source } = $$props;
  let { label } = $$props;
  let { root } = $$props;
  let { show_label } = $$props;
  let { pending } = $$props;
  let { streaming } = $$props;
  let { root_url } = $$props;
  let { loading_status } = $$props;
  let _value;
  let dragging;
  const change_handler = ({ detail }) => {
    $$invalidate(0, value = detail);
    dispatch("change", value);
  };
  const stream_handler = ({ detail }) => {
    $$invalidate(0, value = detail);
    dispatch("stream", value);
  };
  const drag_handler = ({ detail }) => $$invalidate(12, dragging = detail);
  function edit_handler(event) {
    bubble.call(this, $$self, event);
  }
  function play_handler(event) {
    bubble.call(this, $$self, event);
  }
  function pause_handler(event) {
    bubble.call(this, $$self, event);
  }
  function ended_handler(event) {
    bubble.call(this, $$self, event);
  }
  function upload_handler(event) {
    bubble.call(this, $$self, event);
  }
  const error_handler = ({ detail }) => {
    $$invalidate(1, loading_status = loading_status || {});
    $$invalidate(1, loading_status.status = "error", loading_status);
    $$invalidate(1, loading_status.message = detail, loading_status);
  };
  $$self.$$set = ($$props2) => {
    if ("style" in $$props2)
      $$invalidate(15, style = $$props2.style);
    if ("elem_id" in $$props2)
      $$invalidate(2, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(3, visible = $$props2.visible);
    if ("mode" in $$props2)
      $$invalidate(4, mode = $$props2.mode);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("name" in $$props2)
      $$invalidate(5, name = $$props2.name);
    if ("source" in $$props2)
      $$invalidate(6, source = $$props2.source);
    if ("label" in $$props2)
      $$invalidate(7, label = $$props2.label);
    if ("root" in $$props2)
      $$invalidate(16, root = $$props2.root);
    if ("show_label" in $$props2)
      $$invalidate(8, show_label = $$props2.show_label);
    if ("pending" in $$props2)
      $$invalidate(9, pending = $$props2.pending);
    if ("streaming" in $$props2)
      $$invalidate(10, streaming = $$props2.streaming);
    if ("root_url" in $$props2)
      $$invalidate(17, root_url = $$props2.root_url);
    if ("loading_status" in $$props2)
      $$invalidate(1, loading_status = $$props2.loading_status);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 196609) {
      $$invalidate(11, _value = normalise_file(value, root_url != null ? root_url : root));
    }
  };
  return [
    value,
    loading_status,
    elem_id,
    visible,
    mode,
    name,
    source,
    label,
    show_label,
    pending,
    streaming,
    _value,
    dragging,
    $_,
    dispatch,
    style,
    root,
    root_url,
    change_handler,
    stream_handler,
    drag_handler,
    edit_handler,
    play_handler,
    pause_handler,
    ended_handler,
    upload_handler,
    error_handler
  ];
}
class Audio_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      style: 15,
      elem_id: 2,
      visible: 3,
      mode: 4,
      value: 0,
      name: 5,
      source: 6,
      label: 7,
      root: 16,
      show_label: 8,
      pending: 9,
      streaming: 10,
      root_url: 17,
      loading_status: 1
    });
  }
}
var Audio_1$1 = Audio_1;
const modes = ["static", "dynamic"];
const document = () => ({
  type: "{ name: string; data: string }",
  description: "audio data as base64 string",
  example_data: {
    name: "audio.wav",
    data: "data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA="
  }
});
export { Audio_1$1 as Component, document, modes };
//# sourceMappingURL=index2.js.map
