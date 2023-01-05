import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, t as text, b as attr, f as insert, g as append, h as set_data, x as noop, n as detach, B as empty, ac as onMount, I as binding_callbacks, M as src_url_equal, l as listen, y as is_function, A as run_all, a as space, C as destroy_each, d as toggle_class, Y as set_style, w as svg_element, j as transition_in, k as transition_out, F as createEventDispatcher, D as group_outros, o as destroy_component, E as check_outros, c as create_component, m as mount_component } from "./main.js";
import { E as ExampleImage } from "./Image.js";
import { c as csvParseRows } from "./csv.js";
import { d as dsvFormat } from "./dsv.js";
import { E as ExampleModel3D } from "./Model3D.js";
var tsv = dsvFormat("	");
var tsvParseRows = tsv.parseRows;
function create_fragment$f(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[0]);
      attr(div, "class", "gr-sample-number");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$f($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class Number extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$f, create_fragment$f, safe_not_equal, { value: 0 });
  }
}
function create_fragment$e(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[0]);
      attr(div, "class", "gr-sample-dropdown");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$e($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class Dropdown extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$e, create_fragment$e, safe_not_equal, { value: 0 });
  }
}
function create_fragment$d(ctx) {
  let div;
  let t_value = ctx[0].toLocaleString() + "";
  let t;
  return {
    c() {
      div = element("div");
      t = text(t_value);
      attr(div, "class", "gr-sample-checkbox");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1 && t_value !== (t_value = ctx2[0].toLocaleString() + ""))
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
function instance$d($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class Checkbox extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$d, create_fragment$d, safe_not_equal, { value: 0 });
  }
}
function create_fragment$c(ctx) {
  let div;
  let t_value = ctx[0].join(", ") + "";
  let t;
  return {
    c() {
      div = element("div");
      t = text(t_value);
      attr(div, "class", "gr-sample-checkboxgroup");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1 && t_value !== (t_value = ctx2[0].join(", ") + ""))
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
function instance$c($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class CheckboxGroup extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$c, create_fragment$c, safe_not_equal, { value: 0 });
  }
}
function create_fragment$b(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[0]);
      attr(div, "class", "gr-sample-slider");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$b($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class Slider extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$b, create_fragment$b, safe_not_equal, { value: 0 });
  }
}
function create_fragment$a(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[0]);
      attr(div, "class", "gr-sample-radio");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$a($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class Radio extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$a, create_fragment$a, safe_not_equal, { value: 0 });
  }
}
function create_fragment$9(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[0]);
      attr(div, "class", "gr-sample-textbox");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$9($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class Textbox extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$9, create_fragment$9, safe_not_equal, { value: 0 });
  }
}
function create_fragment$8(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[0]);
      attr(div, "class", "gr-sample-audio");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$8($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class Audio extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$8, create_fragment$8, safe_not_equal, { value: 0 });
  }
}
function create_if_block$3(ctx) {
  let video_1;
  let video_1_src_value;
  let mounted;
  let dispose;
  return {
    c() {
      video_1 = element("video");
      video_1.muted = true;
      video_1.playsInline = true;
      attr(video_1, "class", "gr-sample-video");
      if (!src_url_equal(video_1.src, video_1_src_value = ctx[1] + ctx[0]))
        attr(video_1, "src", video_1_src_value);
    },
    m(target, anchor) {
      insert(target, video_1, anchor);
      ctx[3](video_1);
      if (!mounted) {
        dispose = [
          listen(video_1, "mouseover", function() {
            if (is_function(ctx[2].play))
              ctx[2].play.apply(this, arguments);
          }),
          listen(video_1, "mouseout", function() {
            if (is_function(ctx[2].pause))
              ctx[2].pause.apply(this, arguments);
          })
        ];
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty & 3 && !src_url_equal(video_1.src, video_1_src_value = ctx[1] + ctx[0])) {
        attr(video_1, "src", video_1_src_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(video_1);
      ctx[3](null);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_fragment$7(ctx) {
  let if_block_anchor;
  function select_block_type(ctx2, dirty) {
    return create_if_block$3;
  }
  let current_block_type = select_block_type();
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
      if_block.p(ctx2, dirty);
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
function instance$7($$self, $$props, $$invalidate) {
  let { value } = $$props;
  let { samples_dir } = $$props;
  let video;
  onMount(() => {
    $$invalidate(2, video.muted = true, video);
    $$invalidate(2, video.playsInline = true, video);
    $$invalidate(2, video.controls = false, video);
    video.setAttribute("muted", "");
    video.play();
    video.pause();
  });
  function video_1_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      video = $$value;
      $$invalidate(2, video);
    });
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("samples_dir" in $$props2)
      $$invalidate(1, samples_dir = $$props2.samples_dir);
  };
  return [value, samples_dir, video, video_1_binding];
}
class Video extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$7, create_fragment$7, safe_not_equal, { value: 0, samples_dir: 1 });
  }
}
function create_else_block$1(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[0]);
      attr(div, "class", "truncate");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, dirty) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_if_block$2(ctx) {
  let div;
  let t_value = ctx[0].join(", ") + "";
  let t;
  return {
    c() {
      div = element("div");
      t = text(t_value);
      attr(div, "class", "truncate");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t_value !== (t_value = ctx2[0].join(", ") + ""))
        set_data(t, t_value);
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_fragment$6(ctx) {
  let show_if;
  let if_block_anchor;
  function select_block_type(ctx2, dirty) {
    if (dirty & 1)
      show_if = null;
    if (show_if == null)
      show_if = !!Array.isArray(ctx2[0]);
    if (show_if)
      return create_if_block$2;
    return create_else_block$1;
  }
  let current_block_type = select_block_type(ctx, -1);
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
      if (current_block_type === (current_block_type = select_block_type(ctx2, dirty)) && if_block) {
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
function instance$6($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class File extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$6, create_fragment$6, safe_not_equal, { value: 0 });
  }
}
function get_each_context$1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[7] = list[i];
  child_ctx[9] = i;
  return child_ctx;
}
function get_each_context_1$1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[10] = list[i];
  child_ctx[12] = i;
  return child_ctx;
}
function create_if_block$1(ctx) {
  let div;
  let table;
  let t;
  let mounted;
  let dispose;
  let each_value = ctx[3].slice(0, 3);
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block$1(get_each_context$1(ctx, each_value, i));
  }
  let if_block = ctx[0].length > 3 && create_if_block_1$1(ctx);
  return {
    c() {
      div = element("div");
      table = element("table");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      t = space();
      if (if_block)
        if_block.c();
      attr(table, "class", "gr-sample-dataframe relative");
      attr(div, "class", "gr-sample-dataframe");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, table);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(table, null);
      }
      append(table, t);
      if (if_block)
        if_block.m(table, null);
      if (!mounted) {
        dispose = [
          listen(div, "mouseenter", ctx[5]),
          listen(div, "mouseleave", ctx[6])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 8) {
        each_value = ctx2[3].slice(0, 3);
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context$1(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block$1(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(table, t);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
      if (ctx2[0].length > 3) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block_1$1(ctx2);
          if_block.c();
          if_block.m(table, null);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_each(each_blocks, detaching);
      if (if_block)
        if_block.d();
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_each_block_1$1(ctx) {
  let td;
  let t_value = ctx[10] + "";
  let t;
  let td_class_value;
  return {
    c() {
      td = element("td");
      t = text(t_value);
      attr(td, "class", td_class_value = "p-2 " + (ctx[9] < 3 ? "border-b border-b-slate-300 dark:border-b-slate-700" : "") + " " + (ctx[12] < 3 ? "border-r border-r-slate-300 dark:border-r-slate-700 " : ""));
    },
    m(target, anchor) {
      insert(target, td, anchor);
      append(td, t);
    },
    p(ctx2, dirty) {
      if (dirty & 8 && t_value !== (t_value = ctx2[10] + ""))
        set_data(t, t_value);
    },
    d(detaching) {
      if (detaching)
        detach(td);
    }
  };
}
function create_if_block_2$1(ctx) {
  let td;
  return {
    c() {
      td = element("td");
      td.textContent = "\u2026";
      attr(td, "class", "p-2 border-r border-b border-r-slate-300 dark:border-r-slate-700 border-b-slate-300 dark:border-b-slate-700");
    },
    m(target, anchor) {
      insert(target, td, anchor);
    },
    d(detaching) {
      if (detaching)
        detach(td);
    }
  };
}
function create_each_block$1(ctx) {
  let tr;
  let t;
  let each_value_1 = ctx[7].slice(0, 3);
  let each_blocks = [];
  for (let i = 0; i < each_value_1.length; i += 1) {
    each_blocks[i] = create_each_block_1$1(get_each_context_1$1(ctx, each_value_1, i));
  }
  let if_block = ctx[7].length > 3 && create_if_block_2$1();
  return {
    c() {
      tr = element("tr");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      t = space();
      if (if_block)
        if_block.c();
    },
    m(target, anchor) {
      insert(target, tr, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(tr, null);
      }
      append(tr, t);
      if (if_block)
        if_block.m(tr, null);
    },
    p(ctx2, dirty) {
      if (dirty & 8) {
        each_value_1 = ctx2[7].slice(0, 3);
        let i;
        for (i = 0; i < each_value_1.length; i += 1) {
          const child_ctx = get_each_context_1$1(ctx2, each_value_1, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block_1$1(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(tr, t);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value_1.length;
      }
      if (ctx2[7].length > 3) {
        if (if_block)
          ;
        else {
          if_block = create_if_block_2$1();
          if_block.c();
          if_block.m(tr, null);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
    d(detaching) {
      if (detaching)
        detach(tr);
      destroy_each(each_blocks, detaching);
      if (if_block)
        if_block.d();
    }
  };
}
function create_if_block_1$1(ctx) {
  let div;
  return {
    c() {
      div = element("div");
      attr(div, "class", "absolute w-full h-[50%] bottom-0 bg-gradient-to-b from-[rgba(255,255,255,0)] dark:from-[rgba(0,0,0,0)] to-white");
      toggle_class(div, "dark:to-gray-950", !ctx[2]);
      toggle_class(div, "dark:to-gray-800", ctx[2]);
      toggle_class(div, "to-gray-50", ctx[2]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 4) {
        toggle_class(div, "dark:to-gray-950", !ctx2[2]);
      }
      if (dirty & 4) {
        toggle_class(div, "dark:to-gray-800", ctx2[2]);
      }
      if (dirty & 4) {
        toggle_class(div, "to-gray-50", ctx2[2]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_fragment$5(ctx) {
  let if_block_anchor;
  let if_block = ctx[1] && create_if_block$1(ctx);
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
    p(ctx2, [dirty]) {
      if (ctx2[1]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block$1(ctx2);
          if_block.c();
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function instance$5($$self, $$props, $$invalidate) {
  let { value } = $$props;
  let { samples_dir } = $$props;
  let hovered = false;
  let loaded_value = value;
  let loaded = Array.isArray(loaded_value);
  const mouseenter_handler = () => $$invalidate(2, hovered = true);
  const mouseleave_handler = () => $$invalidate(2, hovered = false);
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("samples_dir" in $$props2)
      $$invalidate(4, samples_dir = $$props2.samples_dir);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 19) {
      if (!loaded && typeof value === "string" && /\.[a-zA-Z]+$/.test(value)) {
        fetch(samples_dir + value).then((v) => v.text()).then((v) => {
          try {
            if (value.endsWith("csv")) {
              const small_df = v.split("\n").slice(0, 4).map((v2) => v2.split(",").slice(0, 4).join(",")).join("\n");
              $$invalidate(3, loaded_value = csvParseRows(small_df));
            } else if (value.endsWith("tsv")) {
              const small_df = v.split("\n").slice(0, 4).map((v2) => v2.split("	").slice(0, 4).join("	")).join("\n");
              $$invalidate(3, loaded_value = tsvParseRows(small_df));
            } else {
              throw new Error("Incorrect format, only CSV and TSV files are supported");
            }
            $$invalidate(1, loaded = true);
          } catch (e) {
            console.error(e);
          }
        });
      }
    }
  };
  return [
    value,
    loaded,
    hovered,
    loaded_value,
    samples_dir,
    mouseenter_handler,
    mouseleave_handler
  ];
}
class Dataframe extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$5, create_fragment$5, safe_not_equal, { value: 0, samples_dir: 4 });
  }
}
function create_fragment$4(ctx) {
  let div;
  return {
    c() {
      div = element("div");
      attr(div, "class", "w-10 h-10 border dark:border-slate-300");
      set_style(div, "background-color", ctx[0]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1) {
        set_style(div, "background-color", ctx2[0]);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$4($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class ColorPicker extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$4, create_fragment$4, safe_not_equal, { value: 0 });
  }
}
function create_fragment$3(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[0]);
      attr(div, "class", "truncate");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$3($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class TimeSeries extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$3, create_fragment$3, safe_not_equal, { value: 0 });
  }
}
function create_fragment$2(ctx) {
  let div;
  return {
    c() {
      div = element("div");
      attr(div, "class", "gr-sample-markdown");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      div.innerHTML = ctx[0];
    },
    p(ctx2, [dirty]) {
      if (dirty & 1)
        div.innerHTML = ctx2[0];
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$2($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class Markdown extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$2, create_fragment$2, safe_not_equal, { value: 0 });
  }
}
function create_fragment$1(ctx) {
  let div;
  return {
    c() {
      div = element("div");
      attr(div, "class", "gr-sample-html");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      div.innerHTML = ctx[0];
    },
    p(ctx2, [dirty]) {
      if (dirty & 1)
        div.innerHTML = ctx2[0];
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class HTML extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { value: 0 });
  }
}
const component_map = {
  dropdown: Dropdown,
  checkbox: Checkbox,
  checkboxgroup: CheckboxGroup,
  number: Number,
  slider: Slider,
  radio: Radio,
  image: ExampleImage,
  textbox: Textbox,
  audio: Audio,
  video: Video,
  file: File,
  dataframe: Dataframe,
  model3d: ExampleModel3D,
  colorpicker: ColorPicker,
  timeseries: TimeSeries,
  markdown: Markdown,
  html: HTML
};
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[22] = list[i];
  return child_ctx;
}
function get_each_context_2(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[25] = list[i];
  child_ctx[27] = i;
  return child_ctx;
}
function get_each_context_3(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[0] = list[i].value;
  child_ctx[29] = list[i].component;
  child_ctx[31] = i;
  return child_ctx;
}
function get_each_context_4(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[32] = list[i];
  return child_ctx;
}
function get_each_context_1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[25] = list[i];
  child_ctx[27] = i;
  return child_ctx;
}
function create_else_block_1(ctx) {
  let div;
  let table;
  let thead;
  let tr;
  let t;
  let tbody;
  let current;
  let each_value_4 = ctx[3];
  let each_blocks_1 = [];
  for (let i = 0; i < each_value_4.length; i += 1) {
    each_blocks_1[i] = create_each_block_4(get_each_context_4(ctx, each_value_4, i));
  }
  let each_value_2 = ctx[10];
  let each_blocks = [];
  for (let i = 0; i < each_value_2.length; i += 1) {
    each_blocks[i] = create_each_block_2(get_each_context_2(ctx, each_value_2, i));
  }
  const out = (i) => transition_out(each_blocks[i], 1, 1, () => {
    each_blocks[i] = null;
  });
  return {
    c() {
      div = element("div");
      table = element("table");
      thead = element("thead");
      tr = element("tr");
      for (let i = 0; i < each_blocks_1.length; i += 1) {
        each_blocks_1[i].c();
      }
      t = space();
      tbody = element("tbody");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(tr, "class", "border-b dark:border-gray-800 divide-x dark:divide-gray-800 shadow-sm");
      attr(table, "class", "gr-samples-table");
      attr(div, "class", "overflow-x-auto border table-auto rounded-lg w-full text-sm");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, table);
      append(table, thead);
      append(thead, tr);
      for (let i = 0; i < each_blocks_1.length; i += 1) {
        each_blocks_1[i].m(tr, null);
      }
      append(table, t);
      append(table, tbody);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(tbody, null);
      }
      current = true;
    },
    p(ctx2, dirty) {
      if (dirty[0] & 8) {
        each_value_4 = ctx2[3];
        let i;
        for (i = 0; i < each_value_4.length; i += 1) {
          const child_ctx = get_each_context_4(ctx2, each_value_4, i);
          if (each_blocks_1[i]) {
            each_blocks_1[i].p(child_ctx, dirty);
          } else {
            each_blocks_1[i] = create_each_block_4(child_ctx);
            each_blocks_1[i].c();
            each_blocks_1[i].m(tr, null);
          }
        }
        for (; i < each_blocks_1.length; i += 1) {
          each_blocks_1[i].d(1);
        }
        each_blocks_1.length = each_value_4.length;
      }
      if (dirty[0] & 7363) {
        each_value_2 = ctx2[10];
        let i;
        for (i = 0; i < each_value_2.length; i += 1) {
          const child_ctx = get_each_context_2(ctx2, each_value_2, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
            transition_in(each_blocks[i], 1);
          } else {
            each_blocks[i] = create_each_block_2(child_ctx);
            each_blocks[i].c();
            transition_in(each_blocks[i], 1);
            each_blocks[i].m(tbody, null);
          }
        }
        group_outros();
        for (i = each_value_2.length; i < each_blocks.length; i += 1) {
          out(i);
        }
        check_outros();
      }
    },
    i(local) {
      if (current)
        return;
      for (let i = 0; i < each_value_2.length; i += 1) {
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
        detach(div);
      destroy_each(each_blocks_1, detaching);
      destroy_each(each_blocks, detaching);
    }
  };
}
function create_if_block_2(ctx) {
  let div;
  let current;
  let each_value_1 = ctx[8];
  let each_blocks = [];
  for (let i = 0; i < each_value_1.length; i += 1) {
    each_blocks[i] = create_each_block_1(get_each_context_1(ctx, each_value_1, i));
  }
  const out = (i) => transition_out(each_blocks[i], 1, 1, () => {
    each_blocks[i] = null;
  });
  return {
    c() {
      div = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", "gr-samples-gallery");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
      current = true;
    },
    p(ctx2, dirty) {
      if (dirty[0] & 7619) {
        each_value_1 = ctx2[8];
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
        detach(div);
      destroy_each(each_blocks, detaching);
    }
  };
}
function create_each_block_4(ctx) {
  let th;
  let t0_value = ctx[32] + "";
  let t0;
  let t1;
  return {
    c() {
      th = element("th");
      t0 = text(t0_value);
      t1 = space();
      attr(th, "class", "p-2 whitespace-nowrap min-w-lg text-left");
    },
    m(target, anchor) {
      insert(target, th, anchor);
      append(th, t0);
      append(th, t1);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 8 && t0_value !== (t0_value = ctx2[32] + ""))
        set_data(t0, t0_value);
    },
    d(detaching) {
      if (detaching)
        detach(th);
    }
  };
}
function create_if_block_4(ctx) {
  let td;
  let switch_instance;
  let current;
  var switch_value = ctx[29];
  function switch_props(ctx2) {
    return {
      props: {
        value: ctx2[0],
        samples_dir: ctx2[12]
      }
    };
  }
  if (switch_value) {
    switch_instance = new switch_value(switch_props(ctx));
  }
  return {
    c() {
      td = element("td");
      if (switch_instance)
        create_component(switch_instance.$$.fragment);
      attr(td, "class", "p-2");
    },
    m(target, anchor) {
      insert(target, td, anchor);
      if (switch_instance) {
        mount_component(switch_instance, td, null);
      }
      current = true;
    },
    p(ctx2, dirty) {
      const switch_instance_changes = {};
      if (dirty[0] & 1024)
        switch_instance_changes.value = ctx2[0];
      if (switch_value !== (switch_value = ctx2[29])) {
        if (switch_instance) {
          group_outros();
          const old_component = switch_instance;
          transition_out(old_component.$$.fragment, 1, 0, () => {
            destroy_component(old_component, 1);
          });
          check_outros();
        }
        if (switch_value) {
          switch_instance = new switch_value(switch_props(ctx2));
          create_component(switch_instance.$$.fragment);
          transition_in(switch_instance.$$.fragment, 1);
          mount_component(switch_instance, td, null);
        } else {
          switch_instance = null;
        }
      } else if (switch_value) {
        switch_instance.$set(switch_instance_changes);
      }
    },
    i(local) {
      if (current)
        return;
      if (switch_instance)
        transition_in(switch_instance.$$.fragment, local);
      current = true;
    },
    o(local) {
      if (switch_instance)
        transition_out(switch_instance.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(td);
      if (switch_instance)
        destroy_component(switch_instance);
    }
  };
}
function create_each_block_3(ctx) {
  let if_block_anchor;
  let current;
  let if_block = ctx[1][ctx[31]] !== void 0 && component_map[ctx[1][ctx[31]]] !== void 0 && create_if_block_4(ctx);
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
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[1][ctx2[31]] !== void 0 && component_map[ctx2[1][ctx2[31]]] !== void 0) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty[0] & 2) {
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
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_each_block_2(ctx) {
  let tr;
  let t;
  let current;
  let mounted;
  let dispose;
  let each_value_3 = ctx[25];
  let each_blocks = [];
  for (let i = 0; i < each_value_3.length; i += 1) {
    each_blocks[i] = create_each_block_3(get_each_context_3(ctx, each_value_3, i));
  }
  const out = (i) => transition_out(each_blocks[i], 1, 1, () => {
    each_blocks[i] = null;
  });
  function click_handler_1() {
    return ctx[20](ctx[27]);
  }
  return {
    c() {
      tr = element("tr");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      t = space();
      attr(tr, "class", "group cursor-pointer odd:bg-gray-50 border-b dark:border-gray-800 divide-x dark:divide-gray-800 last:border-none hover:bg-orange-50 hover:divide-orange-100 dark:hover:bg-gray-700");
    },
    m(target, anchor) {
      insert(target, tr, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(tr, null);
      }
      append(tr, t);
      current = true;
      if (!mounted) {
        dispose = listen(tr, "click", click_handler_1);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty[0] & 5122) {
        each_value_3 = ctx[25];
        let i;
        for (i = 0; i < each_value_3.length; i += 1) {
          const child_ctx = get_each_context_3(ctx, each_value_3, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
            transition_in(each_blocks[i], 1);
          } else {
            each_blocks[i] = create_each_block_3(child_ctx);
            each_blocks[i].c();
            transition_in(each_blocks[i], 1);
            each_blocks[i].m(tr, t);
          }
        }
        group_outros();
        for (i = each_value_3.length; i < each_blocks.length; i += 1) {
          out(i);
        }
        check_outros();
      }
    },
    i(local) {
      if (current)
        return;
      for (let i = 0; i < each_value_3.length; i += 1) {
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
        detach(tr);
      destroy_each(each_blocks, detaching);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block_3(ctx) {
  let switch_instance;
  let switch_instance_anchor;
  let current;
  var switch_value = ctx[10][0][0].component;
  function switch_props(ctx2) {
    return {
      props: {
        value: ctx2[25][0],
        samples_dir: ctx2[12]
      }
    };
  }
  if (switch_value) {
    switch_instance = new switch_value(switch_props(ctx));
  }
  return {
    c() {
      if (switch_instance)
        create_component(switch_instance.$$.fragment);
      switch_instance_anchor = empty();
    },
    m(target, anchor) {
      if (switch_instance) {
        mount_component(switch_instance, target, anchor);
      }
      insert(target, switch_instance_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const switch_instance_changes = {};
      if (dirty[0] & 256)
        switch_instance_changes.value = ctx2[25][0];
      if (switch_value !== (switch_value = ctx2[10][0][0].component)) {
        if (switch_instance) {
          group_outros();
          const old_component = switch_instance;
          transition_out(old_component.$$.fragment, 1, 0, () => {
            destroy_component(old_component, 1);
          });
          check_outros();
        }
        if (switch_value) {
          switch_instance = new switch_value(switch_props(ctx2));
          create_component(switch_instance.$$.fragment);
          transition_in(switch_instance.$$.fragment, 1);
          mount_component(switch_instance, switch_instance_anchor.parentNode, switch_instance_anchor);
        } else {
          switch_instance = null;
        }
      } else if (switch_value) {
        switch_instance.$set(switch_instance_changes);
      }
    },
    i(local) {
      if (current)
        return;
      if (switch_instance)
        transition_in(switch_instance.$$.fragment, local);
      current = true;
    },
    o(local) {
      if (switch_instance)
        transition_out(switch_instance.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(switch_instance_anchor);
      if (switch_instance)
        destroy_component(switch_instance, detaching);
    }
  };
}
function create_each_block_1(ctx) {
  let button;
  let show_if = Object.keys(component_map).includes(ctx[1][0]) && component_map[ctx[1][0]];
  let t;
  let current;
  let mounted;
  let dispose;
  let if_block = show_if && create_if_block_3(ctx);
  function click_handler() {
    return ctx[19](ctx[27]);
  }
  return {
    c() {
      button = element("button");
      if (if_block)
        if_block.c();
      t = space();
      attr(button, "class", "group rounded-lg");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      if (if_block)
        if_block.m(button, null);
      append(button, t);
      current = true;
      if (!mounted) {
        dispose = listen(button, "click", click_handler);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty[0] & 2)
        show_if = Object.keys(component_map).includes(ctx[1][0]) && component_map[ctx[1][0]];
      if (show_if) {
        if (if_block) {
          if_block.p(ctx, dirty);
          if (dirty[0] & 2) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block_3(ctx);
          if_block.c();
          transition_in(if_block, 1);
          if_block.m(button, t);
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
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(button);
      if (if_block)
        if_block.d();
      mounted = false;
      dispose();
    }
  };
}
function create_if_block(ctx) {
  let div;
  let t;
  let each_value = ctx[9];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  return {
    c() {
      div = element("div");
      t = text("Pages:\n		");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", "flex gap-2 items-center justify-center text-sm");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
    },
    p(ctx2, dirty) {
      if (dirty[0] & 640) {
        each_value = ctx2[9];
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
      if (detaching)
        detach(div);
      destroy_each(each_blocks, detaching);
    }
  };
}
function create_else_block(ctx) {
  let button;
  let t0_value = ctx[22] + 1 + "";
  let t0;
  let t1;
  let mounted;
  let dispose;
  function click_handler_2() {
    return ctx[21](ctx[22]);
  }
  return {
    c() {
      button = element("button");
      t0 = text(t0_value);
      t1 = space();
      toggle_class(button, "font-bold", ctx[7] === ctx[22]);
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, t0);
      append(button, t1);
      if (!mounted) {
        dispose = listen(button, "click", click_handler_2);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty[0] & 512 && t0_value !== (t0_value = ctx[22] + 1 + ""))
        set_data(t0, t0_value);
      if (dirty[0] & 640) {
        toggle_class(button, "font-bold", ctx[7] === ctx[22]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(button);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block_1(ctx) {
  let div;
  return {
    c() {
      div = element("div");
      div.textContent = "...";
    },
    m(target, anchor) {
      insert(target, div, anchor);
    },
    p: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_each_block(ctx) {
  let if_block_anchor;
  function select_block_type_1(ctx2, dirty) {
    if (ctx2[22] === -1)
      return create_if_block_1;
    return create_else_block;
  }
  let current_block_type = select_block_type_1(ctx);
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
    p(ctx2, dirty) {
      if (current_block_type === (current_block_type = select_block_type_1(ctx2)) && if_block) {
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
    d(detaching) {
      if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_fragment(ctx) {
  let div1;
  let div0;
  let svg;
  let path;
  let t0;
  let t1;
  let t2;
  let current_block_type_index;
  let if_block0;
  let t3;
  let if_block1_anchor;
  let current;
  const if_block_creators = [create_if_block_2, create_else_block_1];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[13])
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block0 = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  let if_block1 = ctx[14] && create_if_block(ctx);
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      svg = svg_element("svg");
      path = svg_element("path");
      t0 = space();
      t1 = text(ctx[2]);
      t2 = space();
      if_block0.c();
      t3 = space();
      if (if_block1)
        if_block1.c();
      if_block1_anchor = empty();
      attr(path, "fill", "currentColor");
      attr(path, "d", "M10 6h18v2H10zm0 18h18v2H10zm0-9h18v2H10zm-6 0h2v2H4zm0-9h2v2H4zm0 18h2v2H4z");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "xmlns:xlink", "http://www.w3.org/1999/xlink");
      attr(svg, "aria-hidden", "true");
      attr(svg, "role", "img");
      attr(svg, "class", "mr-1");
      attr(svg, "width", "1em");
      attr(svg, "height", "1em");
      attr(svg, "preserveAspectRatio", "xMidYMid meet");
      attr(svg, "viewBox", "0 0 32 32");
      attr(div0, "class", "text-xs mb-2 flex items-center text-gray-500");
      attr(div1, "id", ctx[4]);
      attr(div1, "class", "mt-4 inline-block max-w-full text-gray-700 w-full");
      toggle_class(div1, "!hidden", !ctx[5]);
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      append(div0, svg);
      append(svg, path);
      append(div0, t0);
      append(div0, t1);
      append(div1, t2);
      if_blocks[current_block_type_index].m(div1, null);
      insert(target, t3, anchor);
      if (if_block1)
        if_block1.m(target, anchor);
      insert(target, if_block1_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (!current || dirty[0] & 4)
        set_data(t1, ctx2[2]);
      if_block0.p(ctx2, dirty);
      if (!current || dirty[0] & 16) {
        attr(div1, "id", ctx2[4]);
      }
      if (dirty[0] & 32) {
        toggle_class(div1, "!hidden", !ctx2[5]);
      }
      if (ctx2[14])
        if_block1.p(ctx2, dirty);
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block0);
      current = true;
    },
    o(local) {
      transition_out(if_block0);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      if_blocks[current_block_type_index].d();
      if (detaching)
        detach(t3);
      if (if_block1)
        if_block1.d(detaching);
      if (detaching)
        detach(if_block1_anchor);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let component_meta;
  let { components } = $$props;
  let { label = "Examples" } = $$props;
  let { headers } = $$props;
  let { samples } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = null } = $$props;
  let { root } = $$props;
  let { root_url } = $$props;
  let { samples_per_page = 10 } = $$props;
  const dispatch = createEventDispatcher();
  let samples_dir = (root_url != null ? root_url : root) + "file=";
  let page = 0;
  let gallery = headers.length === 1;
  let paginate = samples.length > samples_per_page;
  let selected_samples;
  let page_count;
  let visible_pages = [];
  const click_handler = (i) => {
    $$invalidate(0, value = i + page * samples_per_page);
    dispatch("click", value);
  };
  const click_handler_1 = (i) => {
    $$invalidate(0, value = i + page * samples_per_page);
    dispatch("click", value);
  };
  const click_handler_2 = (visible_page) => $$invalidate(7, page = visible_page);
  $$self.$$set = ($$props2) => {
    if ("components" in $$props2)
      $$invalidate(1, components = $$props2.components);
    if ("label" in $$props2)
      $$invalidate(2, label = $$props2.label);
    if ("headers" in $$props2)
      $$invalidate(3, headers = $$props2.headers);
    if ("samples" in $$props2)
      $$invalidate(15, samples = $$props2.samples);
    if ("elem_id" in $$props2)
      $$invalidate(4, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(5, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("root" in $$props2)
      $$invalidate(16, root = $$props2.root);
    if ("root_url" in $$props2)
      $$invalidate(17, root_url = $$props2.root_url);
    if ("samples_per_page" in $$props2)
      $$invalidate(6, samples_per_page = $$props2.samples_per_page);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty[0] & 295616) {
      {
        if (paginate) {
          $$invalidate(9, visible_pages = []);
          $$invalidate(8, selected_samples = samples.slice(page * samples_per_page, (page + 1) * samples_per_page));
          $$invalidate(18, page_count = Math.ceil(samples.length / samples_per_page));
          [0, page, page_count - 1].forEach((anchor) => {
            for (let i = anchor - 2; i <= anchor + 2; i++) {
              if (i >= 0 && i < page_count && !visible_pages.includes(i)) {
                if (visible_pages.length > 0 && i - visible_pages[visible_pages.length - 1] > 1) {
                  visible_pages.push(-1);
                }
                visible_pages.push(i);
              }
            }
          });
        } else {
          $$invalidate(8, selected_samples = samples.slice());
        }
      }
    }
    if ($$self.$$.dirty[0] & 258) {
      $$invalidate(10, component_meta = selected_samples.map((sample_row) => sample_row.map((sample_cell, j) => ({
        value: sample_cell,
        component: component_map[components[j]]
      }))));
    }
  };
  return [
    value,
    components,
    label,
    headers,
    elem_id,
    visible,
    samples_per_page,
    page,
    selected_samples,
    visible_pages,
    component_meta,
    dispatch,
    samples_dir,
    gallery,
    paginate,
    samples,
    root,
    root_url,
    page_count,
    click_handler,
    click_handler_1,
    click_handler_2
  ];
}
class Dataset extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      components: 1,
      label: 2,
      headers: 3,
      samples: 15,
      elem_id: 4,
      visible: 5,
      value: 0,
      root: 16,
      root_url: 17,
      samples_per_page: 6
    }, null, [-1, -1]);
  }
}
var Dataset$1 = Dataset;
const modes = ["dynamic"];
const document = () => ({
  type: "number",
  description: "index of selected row",
  example_data: 0
});
export { Dataset$1 as Component, document, modes };
//# sourceMappingURL=index13.js.map
