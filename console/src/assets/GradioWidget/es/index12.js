import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, b as attr, d as toggle_class, f as insert, a7 as set_input_value, l as listen, n as detach, A as run_all, a as space, x as noop, K as bubble, I as binding_callbacks, t as text, h as set_data, a2 as HtmlTag, B as empty, g as append, O as bind, c as create_component, m as mount_component, L as add_flush_callback, j as transition_in, k as transition_out, o as destroy_component, F as createEventDispatcher, a9 as tick, w as svg_element, D as group_outros, aa as update_keyed_each, ab as outro_and_destroy_block, E as check_outros, R as assign, T as StatusTracker, U as get_spread_update, V as get_spread_object } from "./main.js";
import { U as Upload } from "./Upload.js";
import { d as dsvFormat } from "./dsv.js";
var has = Object.prototype.hasOwnProperty;
function dequal(foo, bar) {
  var ctor, len;
  if (foo === bar)
    return true;
  if (foo && bar && (ctor = foo.constructor) === bar.constructor) {
    if (ctor === Date)
      return foo.getTime() === bar.getTime();
    if (ctor === RegExp)
      return foo.toString() === bar.toString();
    if (ctor === Array) {
      if ((len = foo.length) === bar.length) {
        while (len-- && dequal(foo[len], bar[len]))
          ;
      }
      return len === -1;
    }
    if (!ctor || typeof foo === "object") {
      len = 0;
      for (ctor in foo) {
        if (has.call(foo, ctor) && ++len && !has.call(bar, ctor))
          return false;
        if (!(ctor in bar) || !dequal(foo[ctor], bar[ctor]))
          return false;
      }
      return Object.keys(bar).length === len;
    }
  }
  return foo !== foo && bar !== bar;
}
function create_if_block_1$1(ctx) {
  let input;
  let mounted;
  let dispose;
  return {
    c() {
      input = element("input");
      attr(input, "class", "absolute outline-none inset-2 bg-transparent border-0 translate-x-px flex-1 ");
      attr(input, "tabindex", "-1");
      toggle_class(input, "translate-x-px", !ctx[3]);
      toggle_class(input, "font-bold", ctx[3]);
    },
    m(target, anchor) {
      insert(target, input, anchor);
      set_input_value(input, ctx[0]);
      ctx[8](input);
      if (!mounted) {
        dispose = [
          listen(input, "input", ctx[7]),
          listen(input, "keydown", ctx[6]),
          listen(input, "blur", blur_handler)
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 1 && input.value !== ctx2[0]) {
        set_input_value(input, ctx2[0]);
      }
      if (dirty & 8) {
        toggle_class(input, "translate-x-px", !ctx2[3]);
      }
      if (dirty & 8) {
        toggle_class(input, "font-bold", ctx2[3]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(input);
      ctx[8](null);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_else_block(ctx) {
  let t;
  return {
    c() {
      t = text(ctx[0]);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_if_block$1(ctx) {
  let html_tag;
  let html_anchor;
  return {
    c() {
      html_tag = new HtmlTag(false);
      html_anchor = empty();
      html_tag.a = html_anchor;
    },
    m(target, anchor) {
      html_tag.m(ctx[0], target, anchor);
      insert(target, html_anchor, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 1)
        html_tag.p(ctx2[0]);
    },
    d(detaching) {
      if (detaching)
        detach(html_anchor);
      if (detaching)
        html_tag.d();
    }
  };
}
function create_fragment$2(ctx) {
  let t;
  let span;
  let mounted;
  let dispose;
  let if_block0 = ctx[2] && create_if_block_1$1(ctx);
  function select_block_type(ctx2, dirty) {
    if (ctx2[4] === "markdown" || ctx2[4] === "html")
      return create_if_block$1;
    return create_else_block;
  }
  let current_block_type = select_block_type(ctx);
  let if_block1 = current_block_type(ctx);
  return {
    c() {
      if (if_block0)
        if_block0.c();
      t = space();
      span = element("span");
      if_block1.c();
      attr(span, "tabindex", "-1");
      attr(span, "role", "button");
      attr(span, "class", "p-2 outline-none border-0 flex-1");
      toggle_class(span, "opacity-0", ctx[2]);
      toggle_class(span, "pointer-events-none", ctx[2]);
    },
    m(target, anchor) {
      if (if_block0)
        if_block0.m(target, anchor);
      insert(target, t, anchor);
      insert(target, span, anchor);
      if_block1.m(span, null);
      if (!mounted) {
        dispose = listen(span, "dblclick", ctx[5]);
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (ctx2[2]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_1$1(ctx2);
          if_block0.c();
          if_block0.m(t.parentNode, t);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (current_block_type === (current_block_type = select_block_type(ctx2)) && if_block1) {
        if_block1.p(ctx2, dirty);
      } else {
        if_block1.d(1);
        if_block1 = current_block_type(ctx2);
        if (if_block1) {
          if_block1.c();
          if_block1.m(span, null);
        }
      }
      if (dirty & 4) {
        toggle_class(span, "opacity-0", ctx2[2]);
      }
      if (dirty & 4) {
        toggle_class(span, "pointer-events-none", ctx2[2]);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (if_block0)
        if_block0.d(detaching);
      if (detaching)
        detach(t);
      if (detaching)
        detach(span);
      if_block1.d();
      mounted = false;
      dispose();
    }
  };
}
const blur_handler = ({ currentTarget }) => currentTarget.setAttribute("tabindex", "-1");
function instance$2($$self, $$props, $$invalidate) {
  let { edit } = $$props;
  let { value = "" } = $$props;
  let { el } = $$props;
  let { header = false } = $$props;
  let { datatype = "str" } = $$props;
  function dblclick_handler(event) {
    bubble.call(this, $$self, event);
  }
  function keydown_handler(event) {
    bubble.call(this, $$self, event);
  }
  function input_input_handler() {
    value = this.value;
    $$invalidate(0, value);
  }
  function input_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      el = $$value;
      $$invalidate(1, el);
    });
  }
  $$self.$$set = ($$props2) => {
    if ("edit" in $$props2)
      $$invalidate(2, edit = $$props2.edit);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("el" in $$props2)
      $$invalidate(1, el = $$props2.el);
    if ("header" in $$props2)
      $$invalidate(3, header = $$props2.header);
    if ("datatype" in $$props2)
      $$invalidate(4, datatype = $$props2.datatype);
  };
  return [
    value,
    el,
    edit,
    header,
    datatype,
    dblclick_handler,
    keydown_handler,
    input_input_handler,
    input_binding
  ];
}
class EditableCell extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$2, create_fragment$2, safe_not_equal, {
      edit: 2,
      value: 0,
      el: 1,
      header: 3,
      datatype: 4
    });
  }
}
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[52] = list[i];
  child_ctx[54] = i;
  return child_ctx;
}
function get_each_context_1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[55] = list[i].value;
  child_ctx[56] = list[i].id;
  child_ctx[57] = list;
  child_ctx[58] = i;
  return child_ctx;
}
function get_each_context_2(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[55] = list[i].value;
  child_ctx[56] = list[i].id;
  child_ctx[59] = list;
  child_ctx[54] = i;
  return child_ctx;
}
function create_if_block_4(ctx) {
  let p;
  let t;
  return {
    c() {
      p = element("p");
      t = text(ctx[1]);
      attr(p, "class", "text-gray-600 text-[0.855rem] mb-2 block dark:text-gray-200 relative z-40");
    },
    m(target, anchor) {
      insert(target, p, anchor);
      append(p, t);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 2)
        set_data(t, ctx2[1]);
    },
    d(detaching) {
      if (detaching)
        detach(p);
    }
  };
}
function create_if_block_3(ctx) {
  let caption;
  let t;
  return {
    c() {
      caption = element("caption");
      t = text(ctx[1]);
      attr(caption, "class", "sr-only");
    },
    m(target, anchor) {
      insert(target, caption, anchor);
      append(caption, t);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 2)
        set_data(t, ctx2[1]);
    },
    d(detaching) {
      if (detaching)
        detach(caption);
    }
  };
}
function create_each_block_2(key_1, ctx) {
  let th;
  let div1;
  let editablecell;
  let updating_el;
  let t0;
  let div0;
  let svg;
  let path;
  let div0_class_value;
  let t1;
  let th_aria_sort_value;
  let id = ctx[56];
  let current;
  let mounted;
  let dispose;
  function editablecell_el_binding(value) {
    ctx[30](value, ctx[56]);
  }
  function dblclick_handler() {
    return ctx[31](ctx[56]);
  }
  let editablecell_props = {
    value: ctx[55],
    edit: ctx[13] === ctx[56],
    header: true
  };
  if (ctx[10][ctx[56]].input !== void 0) {
    editablecell_props.el = ctx[10][ctx[56]].input;
  }
  editablecell = new EditableCell({ props: editablecell_props });
  binding_callbacks.push(() => bind(editablecell, "el", editablecell_el_binding));
  editablecell.$on("keydown", ctx[21]);
  editablecell.$on("dblclick", dblclick_handler);
  function click_handler() {
    return ctx[32](ctx[54]);
  }
  const assign_th = () => ctx[33](th, id);
  const unassign_th = () => ctx[33](null, id);
  return {
    key: key_1,
    first: null,
    c() {
      th = element("th");
      div1 = element("div");
      create_component(editablecell.$$.fragment);
      t0 = space();
      div0 = element("div");
      svg = svg_element("svg");
      path = svg_element("path");
      t1 = space();
      attr(path, "d", "M4.49999 0L8.3971 6.75H0.602875L4.49999 0Z");
      attr(svg, "width", "1em");
      attr(svg, "height", "1em");
      attr(svg, "class", "fill-current text-[10px]");
      attr(svg, "viewBox", "0 0 9 7");
      attr(svg, "fill", "none");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(div0, "class", div0_class_value = "flex flex-none items-center justify-center p-2 cursor-pointer leading-snug transform transition-all " + (ctx[12] !== ctx[54] ? "text-gray-200 hover:text-gray-500" : "text-orange-500") + " " + (ctx[12] === ctx[54] && ctx[11] === "des" ? "-scale-y-[1]" : ""));
      toggle_class(div0, "text-gray-200", ctx[12] !== ctx[54]);
      attr(div1, "class", "min-h-[2.3rem] flex outline-none");
      attr(th, "class", "p-0 relative focus-within:ring-1 ring-orange-500 ring-inset outline-none");
      attr(th, "aria-sort", th_aria_sort_value = ctx[15](ctx[55], ctx[12], ctx[11]));
      toggle_class(th, "bg-orange-50", ctx[13] === ctx[56]);
      toggle_class(th, "dark:bg-transparent", ctx[13] === ctx[56]);
      toggle_class(th, "rounded-tl-lg", ctx[54] === 0);
      toggle_class(th, "rounded-tr-lg", ctx[54] === ctx[8].length - 1);
      this.first = th;
    },
    m(target, anchor) {
      insert(target, th, anchor);
      append(th, div1);
      mount_component(editablecell, div1, null);
      append(div1, t0);
      append(div1, div0);
      append(div0, svg);
      append(svg, path);
      append(th, t1);
      assign_th();
      current = true;
      if (!mounted) {
        dispose = listen(div0, "click", click_handler);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      const editablecell_changes = {};
      if (dirty[0] & 256)
        editablecell_changes.value = ctx[55];
      if (dirty[0] & 8448)
        editablecell_changes.edit = ctx[13] === ctx[56];
      if (!updating_el && dirty[0] & 1280) {
        updating_el = true;
        editablecell_changes.el = ctx[10][ctx[56]].input;
        add_flush_callback(() => updating_el = false);
      }
      editablecell.$set(editablecell_changes);
      if (!current || dirty[0] & 6400 && div0_class_value !== (div0_class_value = "flex flex-none items-center justify-center p-2 cursor-pointer leading-snug transform transition-all " + (ctx[12] !== ctx[54] ? "text-gray-200 hover:text-gray-500" : "text-orange-500") + " " + (ctx[12] === ctx[54] && ctx[11] === "des" ? "-scale-y-[1]" : ""))) {
        attr(div0, "class", div0_class_value);
      }
      if (dirty[0] & 6400) {
        toggle_class(div0, "text-gray-200", ctx[12] !== ctx[54]);
      }
      if (!current || dirty[0] & 6400 && th_aria_sort_value !== (th_aria_sort_value = ctx[15](ctx[55], ctx[12], ctx[11]))) {
        attr(th, "aria-sort", th_aria_sort_value);
      }
      if (id !== ctx[56]) {
        unassign_th();
        id = ctx[56];
        assign_th();
      }
      if (dirty[0] & 8448) {
        toggle_class(th, "bg-orange-50", ctx[13] === ctx[56]);
      }
      if (dirty[0] & 8448) {
        toggle_class(th, "dark:bg-transparent", ctx[13] === ctx[56]);
      }
      if (dirty[0] & 256) {
        toggle_class(th, "rounded-tl-lg", ctx[54] === 0);
      }
      if (dirty[0] & 256) {
        toggle_class(th, "rounded-tr-lg", ctx[54] === ctx[8].length - 1);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(editablecell.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(editablecell.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(th);
      destroy_component(editablecell);
      unassign_th();
      mounted = false;
      dispose();
    }
  };
}
function create_each_block_1(key_1, ctx) {
  let td;
  let div;
  let editablecell;
  let updating_value;
  let updating_el;
  let id = ctx[56];
  let current;
  let mounted;
  let dispose;
  function editablecell_value_binding(value) {
    ctx[34](value, ctx[55], ctx[57], ctx[58]);
  }
  function editablecell_el_binding_1(value) {
    ctx[35](value, ctx[56]);
  }
  let editablecell_props = {
    edit: ctx[6] === ctx[56],
    datatype: Array.isArray(ctx[0]) ? ctx[0][ctx[58]] : ctx[0]
  };
  if (ctx[55] !== void 0) {
    editablecell_props.value = ctx[55];
  }
  if (ctx[10][ctx[56]].input !== void 0) {
    editablecell_props.el = ctx[10][ctx[56]].input;
  }
  editablecell = new EditableCell({ props: editablecell_props });
  binding_callbacks.push(() => bind(editablecell, "value", editablecell_value_binding));
  binding_callbacks.push(() => bind(editablecell, "el", editablecell_el_binding_1));
  const assign_td = () => ctx[36](td, id);
  const unassign_td = () => ctx[36](null, id);
  function touchstart_handler() {
    return ctx[37](ctx[56]);
  }
  function click_handler_1() {
    return ctx[38](ctx[56]);
  }
  function dblclick_handler_1() {
    return ctx[39](ctx[56]);
  }
  function keydown_handler(...args) {
    return ctx[40](ctx[54], ctx[58], ctx[56], ...args);
  }
  return {
    key: key_1,
    first: null,
    c() {
      td = element("td");
      div = element("div");
      create_component(editablecell.$$.fragment);
      attr(div, "class", "min-h-[2.3rem] h-full outline-none flex items-center");
      toggle_class(div, "border-transparent", ctx[7] !== ctx[56]);
      attr(td, "tabindex", "0");
      attr(td, "class", "outline-none focus-within:ring-1 ring-orange-500 ring-inset focus-within:bg-orange-50 dark:focus-within:bg-gray-800 group-last:first:rounded-bl-lg group-last:last:rounded-br-lg relative");
      this.first = td;
    },
    m(target, anchor) {
      insert(target, td, anchor);
      append(td, div);
      mount_component(editablecell, div, null);
      assign_td();
      current = true;
      if (!mounted) {
        dispose = [
          listen(td, "touchstart", touchstart_handler, { passive: true }),
          listen(td, "click", click_handler_1),
          listen(td, "dblclick", dblclick_handler_1),
          listen(td, "keydown", keydown_handler)
        ];
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      const editablecell_changes = {};
      if (dirty[0] & 576)
        editablecell_changes.edit = ctx[6] === ctx[56];
      if (dirty[0] & 513)
        editablecell_changes.datatype = Array.isArray(ctx[0]) ? ctx[0][ctx[58]] : ctx[0];
      if (!updating_value && dirty[0] & 512) {
        updating_value = true;
        editablecell_changes.value = ctx[55];
        add_flush_callback(() => updating_value = false);
      }
      if (!updating_el && dirty[0] & 1536) {
        updating_el = true;
        editablecell_changes.el = ctx[10][ctx[56]].input;
        add_flush_callback(() => updating_el = false);
      }
      editablecell.$set(editablecell_changes);
      if (dirty[0] & 640) {
        toggle_class(div, "border-transparent", ctx[7] !== ctx[56]);
      }
      if (id !== ctx[56]) {
        unassign_td();
        id = ctx[56];
        assign_td();
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(editablecell.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(editablecell.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(td);
      destroy_component(editablecell);
      unassign_td();
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_each_block(key_1, ctx) {
  let tr;
  let each_blocks = [];
  let each_1_lookup = /* @__PURE__ */ new Map();
  let t;
  let current;
  let each_value_1 = ctx[52];
  const get_key = (ctx2) => ctx2[56];
  for (let i = 0; i < each_value_1.length; i += 1) {
    let child_ctx = get_each_context_1(ctx, each_value_1, i);
    let key = get_key(child_ctx);
    each_1_lookup.set(key, each_blocks[i] = create_each_block_1(key, child_ctx));
  }
  return {
    key: key_1,
    first: null,
    c() {
      tr = element("tr");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      t = space();
      attr(tr, "class", "group border-b dark:border-gray-700 last:border-none divide-x dark:divide-gray-700 space-x-4 odd:bg-gray-50 dark:odd:bg-gray-900 group focus:bg-gradient-to-b focus:from-blue-100 dark:focus:from-blue-900 focus:to-blue-50 dark:focus:to-gray-900 focus:odd:bg-white");
      this.first = tr;
    },
    m(target, anchor) {
      insert(target, tr, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(tr, null);
      }
      append(tr, t);
      current = true;
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty[0] & 460481) {
        each_value_1 = ctx[52];
        group_outros();
        each_blocks = update_keyed_each(each_blocks, dirty, get_key, 1, ctx, each_value_1, each_1_lookup, tr, outro_and_destroy_block, create_each_block_1, t, get_each_context_1);
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
      for (let i = 0; i < each_blocks.length; i += 1) {
        transition_out(each_blocks[i]);
      }
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(tr);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].d();
      }
    }
  };
}
function create_default_slot(ctx) {
  let table;
  let t0;
  let thead;
  let tr;
  let each_blocks_1 = [];
  let each0_lookup = /* @__PURE__ */ new Map();
  let t1;
  let tbody;
  let each_blocks = [];
  let each1_lookup = /* @__PURE__ */ new Map();
  let current;
  let if_block = ctx[1] && ctx[1].length !== 0 && create_if_block_3(ctx);
  let each_value_2 = ctx[8];
  const get_key = (ctx2) => ctx2[56];
  for (let i = 0; i < each_value_2.length; i += 1) {
    let child_ctx = get_each_context_2(ctx, each_value_2, i);
    let key = get_key(child_ctx);
    each0_lookup.set(key, each_blocks_1[i] = create_each_block_2(key, child_ctx));
  }
  let each_value = ctx[9];
  const get_key_1 = (ctx2) => ctx2[52];
  for (let i = 0; i < each_value.length; i += 1) {
    let child_ctx = get_each_context(ctx, each_value, i);
    let key = get_key_1(child_ctx);
    each1_lookup.set(key, each_blocks[i] = create_each_block(key, child_ctx));
  }
  return {
    c() {
      table = element("table");
      if (if_block)
        if_block.c();
      t0 = space();
      thead = element("thead");
      tr = element("tr");
      for (let i = 0; i < each_blocks_1.length; i += 1) {
        each_blocks_1[i].c();
      }
      t1 = space();
      tbody = element("tbody");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(tr, "class", "border-b dark:border-gray-700 divide-x dark:divide-gray-700 text-left");
      attr(thead, "class", "sticky top-0 left-0 right-0 bg-white shadow-sm z-10");
      attr(tbody, "class", "overflow-y-scroll");
      attr(table, "class", "table-auto font-mono w-full text-gray-900 text-sm transition-opacity overflow-hidden");
      toggle_class(table, "opacity-40", ctx[14]);
    },
    m(target, anchor) {
      insert(target, table, anchor);
      if (if_block)
        if_block.m(table, null);
      append(table, t0);
      append(table, thead);
      append(thead, tr);
      for (let i = 0; i < each_blocks_1.length; i += 1) {
        each_blocks_1[i].m(tr, null);
      }
      append(table, t1);
      append(table, tbody);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(tbody, null);
      }
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[1] && ctx2[1].length !== 0) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block_3(ctx2);
          if_block.c();
          if_block.m(table, t0);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty[0] & 3718400) {
        each_value_2 = ctx2[8];
        group_outros();
        each_blocks_1 = update_keyed_each(each_blocks_1, dirty, get_key, 1, ctx2, each_value_2, each0_lookup, tr, outro_and_destroy_block, create_each_block_2, null, get_each_context_2);
        check_outros();
      }
      if (dirty[0] & 460481) {
        each_value = ctx2[9];
        group_outros();
        each_blocks = update_keyed_each(each_blocks, dirty, get_key_1, 1, ctx2, each_value, each1_lookup, tbody, outro_and_destroy_block, create_each_block, null, get_each_context);
        check_outros();
      }
      if (dirty[0] & 16384) {
        toggle_class(table, "opacity-40", ctx2[14]);
      }
    },
    i(local) {
      if (current)
        return;
      for (let i = 0; i < each_value_2.length; i += 1) {
        transition_in(each_blocks_1[i]);
      }
      for (let i = 0; i < each_value.length; i += 1) {
        transition_in(each_blocks[i]);
      }
      current = true;
    },
    o(local) {
      for (let i = 0; i < each_blocks_1.length; i += 1) {
        transition_out(each_blocks_1[i]);
      }
      for (let i = 0; i < each_blocks.length; i += 1) {
        transition_out(each_blocks[i]);
      }
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(table);
      if (if_block)
        if_block.d();
      for (let i = 0; i < each_blocks_1.length; i += 1) {
        each_blocks_1[i].d();
      }
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].d();
      }
    }
  };
}
function create_if_block(ctx) {
  let div;
  let t;
  let if_block0 = ctx[3][1] === "dynamic" && create_if_block_2(ctx);
  let if_block1 = ctx[2][1] === "dynamic" && create_if_block_1(ctx);
  return {
    c() {
      div = element("div");
      if (if_block0)
        if_block0.c();
      t = space();
      if (if_block1)
        if_block1.c();
      attr(div, "class", "flex justify-end space-x-1 pt-2 text-gray-800");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if (if_block0)
        if_block0.m(div, null);
      append(div, t);
      if (if_block1)
        if_block1.m(div, null);
    },
    p(ctx2, dirty) {
      if (ctx2[3][1] === "dynamic") {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_2(ctx2);
          if_block0.c();
          if_block0.m(div, t);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (ctx2[2][1] === "dynamic") {
        if (if_block1) {
          if_block1.p(ctx2, dirty);
        } else {
          if_block1 = create_if_block_1(ctx2);
          if_block1.c();
          if_block1.m(div, null);
        }
      } else if (if_block1) {
        if_block1.d(1);
        if_block1 = null;
      }
    },
    d(detaching) {
      if (detaching)
        detach(div);
      if (if_block0)
        if_block0.d();
      if (if_block1)
        if_block1.d();
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
      button.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" aria-hidden="true" role="img" class="mr-1 group-hover:text-orange-500" width="1em" height="1em" preserveAspectRatio="xMidYMid meet" viewBox="0 0 32 32"><path fill="currentColor" d="M24.59 16.59L17 24.17V4h-2v20.17l-7.59-7.58L6 18l10 10l10-10l-1.41-1.41z"></path></svg>New row`;
      attr(button, "class", "!flex-none gr-button group");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      if (!mounted) {
        dispose = listen(button, "click", ctx[43]);
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
function create_if_block_1(ctx) {
  let button;
  let mounted;
  let dispose;
  return {
    c() {
      button = element("button");
      button.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" aria-hidden="true" role="img" class="mr-1 group-hover:text-orange-500" width="1em" height="1em" preserveAspectRatio="xMidYMid meet" viewBox="0 0 32 32"><path fill="currentColor" d="m18 6l-1.43 1.393L24.15 15H4v2h20.15l-7.58 7.573L18 26l10-10L18 6z"></path></svg>
					New column`;
      attr(button, "class", "!flex-none gr-button group");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      if (!mounted) {
        dispose = listen(button, "click", ctx[23]);
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
function create_fragment$1(ctx) {
  let div1;
  let t0;
  let div0;
  let upload;
  let updating_dragging;
  let t1;
  let current;
  let mounted;
  let dispose;
  let if_block0 = ctx[1] && ctx[1].length !== 0 && create_if_block_4(ctx);
  function upload_dragging_binding(value) {
    ctx[41](value);
  }
  let upload_props = {
    flex: false,
    center: false,
    boundedheight: false,
    disable_click: true,
    $$slots: { default: [create_default_slot] },
    $$scope: { ctx }
  };
  if (ctx[14] !== void 0) {
    upload_props.dragging = ctx[14];
  }
  upload = new Upload({ props: upload_props });
  binding_callbacks.push(() => bind(upload, "dragging", upload_dragging_binding));
  upload.$on("load", ctx[42]);
  let if_block1 = ctx[4] && create_if_block(ctx);
  return {
    c() {
      div1 = element("div");
      if (if_block0)
        if_block0.c();
      t0 = space();
      div0 = element("div");
      create_component(upload.$$.fragment);
      t1 = space();
      if (if_block1)
        if_block1.c();
      attr(div0, "class", "scroll-hide overflow-hidden rounded-lg relative border transition-colors overflow-x-scroll");
      toggle_class(div0, "border-green-400", ctx[14]);
      toggle_class(div0, "whitespace-nowrap", !ctx[5]);
      toggle_class(div1, "mt-6", ctx[1] && ctx[1].length !== 0);
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      if (if_block0)
        if_block0.m(div1, null);
      append(div1, t0);
      append(div1, div0);
      mount_component(upload, div0, null);
      append(div1, t1);
      if (if_block1)
        if_block1.m(div1, null);
      current = true;
      if (!mounted) {
        dispose = [
          listen(window, "click", ctx[24]),
          listen(window, "touchstart", ctx[24])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (ctx2[1] && ctx2[1].length !== 0) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_4(ctx2);
          if_block0.c();
          if_block0.m(div1, t0);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      const upload_changes = {};
      if (dirty[0] & 32707 | dirty[1] & 536870912) {
        upload_changes.$$scope = { dirty, ctx: ctx2 };
      }
      if (!updating_dragging && dirty[0] & 16384) {
        updating_dragging = true;
        upload_changes.dragging = ctx2[14];
        add_flush_callback(() => updating_dragging = false);
      }
      upload.$set(upload_changes);
      if (dirty[0] & 16384) {
        toggle_class(div0, "border-green-400", ctx2[14]);
      }
      if (dirty[0] & 32) {
        toggle_class(div0, "whitespace-nowrap", !ctx2[5]);
      }
      if (ctx2[4]) {
        if (if_block1) {
          if_block1.p(ctx2, dirty);
        } else {
          if_block1 = create_if_block(ctx2);
          if_block1.c();
          if_block1.m(div1, null);
        }
      } else if (if_block1) {
        if_block1.d(1);
        if_block1 = null;
      }
      if (dirty[0] & 2) {
        toggle_class(div1, "mt-6", ctx2[1] && ctx2[1].length !== 0);
      }
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
      if (detaching)
        detach(div1);
      if (if_block0)
        if_block0.d();
      destroy_component(upload);
      if (if_block1)
        if_block1.d();
      mounted = false;
      run_all(dispose);
    }
  };
}
function guess_delimitaor(text2, possibleDelimiters) {
  return possibleDelimiters.filter(weedOut);
  function weedOut(delimiter) {
    var cache = -1;
    return text2.split("\n").every(checkLength);
    function checkLength(line) {
      if (!line) {
        return true;
      }
      var length = line.split(delimiter).length;
      if (cache < 0) {
        cache = length;
      }
      return cache === length && length > 1;
    }
  }
}
function data_uri_to_blob(data_uri) {
  const byte_str = atob(data_uri.split(",")[1]);
  const mime_str = data_uri.split(",")[0].split(":")[1].split(";")[0];
  const ab = new ArrayBuffer(byte_str.length);
  const ia = new Uint8Array(ab);
  for (let i = 0; i < byte_str.length; i++) {
    ia[i] = byte_str.charCodeAt(i);
  }
  return new Blob([ab], { type: mime_str });
}
function instance$1($$self, $$props, $$invalidate) {
  let { datatype } = $$props;
  let { label = null } = $$props;
  let { headers = [] } = $$props;
  let { values = [[]] } = $$props;
  let { col_count } = $$props;
  let { row_count } = $$props;
  let { editable = true } = $$props;
  let { wrap = false } = $$props;
  const dispatch = createEventDispatcher();
  let editing = false;
  let selected = false;
  let els = {};
  function make_headers(_head) {
    let _h = _head || [];
    if (col_count[1] === "fixed" && _h.length < col_count[0]) {
      const fill = Array(col_count[0] - _h.length).fill("").map((_, i) => `${i + _h.length}`);
      _h = _h.concat(fill);
    }
    if (!_h || _h.length === 0) {
      return Array(col_count[0]).fill(0).map((_, i) => {
        const _id = `h-${i}`;
        $$invalidate(10, els[_id] = { cell: null, input: null }, els);
        return { id: _id, value: JSON.stringify(i + 1) };
      });
    } else {
      return _h.map((h, i) => {
        const _id = `h-${i}`;
        $$invalidate(10, els[_id] = { cell: null, input: null }, els);
        return { id: _id, value: h != null ? h : "" };
      });
    }
  }
  function process_data(_values) {
    const data_row_length = _values.length > 0 ? _values.length : row_count[0];
    return Array(row_count[1] === "fixed" ? row_count[0] : data_row_length < row_count[0] ? row_count[0] : data_row_length).fill(0).map((_, i) => Array(col_count[1] === "fixed" ? col_count[0] : _values[0].length).fill(0).map((_2, j) => {
      var _a, _b;
      const id = `${i}-${j}`;
      $$invalidate(10, els[id] = { input: null, cell: null }, els);
      return { value: (_b = (_a = _values == null ? void 0 : _values[i]) == null ? void 0 : _a[j]) != null ? _b : "", id };
    }));
  }
  let _headers = make_headers(headers);
  let old_headers;
  async function refresh_focus() {
    var _a, _b, _c, _d;
    if (typeof editing === "string") {
      await tick();
      (_b = (_a = els[editing]) == null ? void 0 : _a.input) == null ? void 0 : _b.focus();
    } else if (typeof selected === "string") {
      await tick();
      (_d = (_c = els[selected]) == null ? void 0 : _c.input) == null ? void 0 : _d.focus();
    }
  }
  let data = [[]];
  let old_val = void 0;
  function get_sort_status(name, sort2, direction) {
    if (!sort2)
      return "none";
    if (headers[sort2] === name) {
      if (direction === "asc")
        return "ascending";
      if (direction === "des")
        return "descending";
    }
  }
  function get_current_indices(id) {
    return data.reduce((acc, arr, i) => {
      const j = arr.reduce((acc2, data2, j2) => id === data2.id ? j2 : acc2, -1);
      return j === -1 ? acc : [i, j];
    }, [-1, -1]);
  }
  async function start_edit(id, clear) {
    if (!editable || editing === id)
      return;
    if (clear) {
      const [i, j] = get_current_indices(id);
      $$invalidate(9, data[i][j].value = "", data);
    }
    $$invalidate(6, editing = id);
    await tick();
    const { input } = els[id];
    input == null ? void 0 : input.focus();
  }
  async function handle_keydown(event, i, j, id) {
    var _a;
    let is_data;
    switch (event.key) {
      case "ArrowRight":
        if (editing)
          break;
        event.preventDefault();
        is_data = data[i][j + 1];
        $$invalidate(7, selected = is_data ? is_data.id : selected);
        break;
      case "ArrowLeft":
        if (editing)
          break;
        event.preventDefault();
        is_data = data[i][j - 1];
        $$invalidate(7, selected = is_data ? is_data.id : selected);
        break;
      case "ArrowDown":
        if (editing)
          break;
        event.preventDefault();
        is_data = data[i + 1];
        $$invalidate(7, selected = is_data ? is_data[j].id : selected);
        break;
      case "ArrowUp":
        if (editing)
          break;
        event.preventDefault();
        is_data = data[i - 1];
        $$invalidate(7, selected = is_data ? is_data[j].id : selected);
        break;
      case "Escape":
        if (!editable)
          break;
        event.preventDefault();
        $$invalidate(7, selected = editing);
        $$invalidate(6, editing = false);
        break;
      case "Enter":
        if (!editable)
          break;
        event.preventDefault();
        if (event.shiftKey) {
          add_row(i);
          await tick();
          const [pos] = get_current_indices(id);
          $$invalidate(7, selected = data[pos + 1][j].id);
        } else {
          if (editing === id) {
            $$invalidate(6, editing = false);
          } else {
            start_edit(id);
          }
        }
        break;
      case "Backspace":
        if (!editable)
          break;
        if (!editing) {
          event.preventDefault();
          $$invalidate(9, data[i][j].value = "", data);
        }
        break;
      case "Delete":
        if (!editable)
          break;
        if (!editing) {
          event.preventDefault();
          $$invalidate(9, data[i][j].value = "", data);
        }
        break;
      case "Tab":
        let direction = event.shiftKey ? -1 : 1;
        let is_data_x = data[i][j + direction];
        let is_data_y = (_a = data == null ? void 0 : data[i + direction]) == null ? void 0 : _a[direction > 0 ? 0 : _headers.length - 1];
        let _selected = is_data_x || is_data_y;
        if (_selected) {
          event.preventDefault();
          $$invalidate(7, selected = _selected ? _selected.id : selected);
        }
        $$invalidate(6, editing = false);
        break;
      default:
        if ((!editing || editing && editing !== id) && event.key.length === 1) {
          start_edit(id, true);
        }
        break;
    }
  }
  async function handle_cell_click(id) {
    if (editing === id)
      return;
    if (selected === id)
      return;
    $$invalidate(6, editing = false);
    $$invalidate(7, selected = id);
  }
  async function set_focus(id, type) {
    var _a, _b;
    if (type === "edit" && typeof id == "string") {
      await tick();
      (_a = els[id].input) == null ? void 0 : _a.focus();
    }
    if (type === "edit" && typeof id == "boolean" && typeof selected === "string") {
      let cell = (_b = els[selected]) == null ? void 0 : _b.cell;
      await tick();
      cell == null ? void 0 : cell.focus();
    }
    if (type === "select" && typeof id == "string") {
      const { cell } = els[id];
      await tick();
      cell == null ? void 0 : cell.focus();
    }
  }
  let sort_direction;
  let sort_by;
  function sort(col, dir) {
    if (dir === "asc") {
      $$invalidate(9, data = data.sort((a, b) => a[col].value < b[col].value ? -1 : 1));
    } else if (dir === "des") {
      $$invalidate(9, data = data.sort((a, b) => a[col].value > b[col].value ? -1 : 1));
    }
  }
  function handle_sort(col) {
    if (typeof sort_by !== "number" || sort_by !== col) {
      $$invalidate(11, sort_direction = "asc");
      $$invalidate(12, sort_by = col);
    } else {
      if (sort_direction === "asc") {
        $$invalidate(11, sort_direction = "des");
      } else if (sort_direction === "des") {
        $$invalidate(11, sort_direction = "asc");
      }
    }
    sort(col, sort_direction);
  }
  let header_edit;
  function update_headers_data() {
    var _a;
    if (typeof selected === "string") {
      const new_header = (_a = els[selected].input) == null ? void 0 : _a.value;
      if (_headers.find((i) => i.id === selected)) {
        let obj = _headers.find((i) => i.id === selected);
        if (new_header)
          obj["value"] = new_header;
      } else {
        if (new_header)
          _headers.push({ id: selected, value: new_header });
      }
    }
  }
  async function edit_header(_id, select) {
    var _a, _b;
    if (!editable || col_count[1] !== "dynamic" || editing === _id)
      return;
    $$invalidate(13, header_edit = _id);
    await tick();
    (_a = els[_id].input) == null ? void 0 : _a.focus();
    if (select)
      (_b = els[_id].input) == null ? void 0 : _b.select();
  }
  function end_header_edit(event) {
    if (!editable)
      return;
    switch (event.key) {
      case "Escape":
      case "Enter":
      case "Tab":
        event.preventDefault();
        $$invalidate(7, selected = header_edit);
        $$invalidate(13, header_edit = false);
        update_headers_data();
        break;
    }
  }
  function add_row(index) {
    if (row_count[1] !== "dynamic")
      return;
    data.splice(index ? index + 1 : data.length, 0, Array(data[0].length).fill(0).map((_, i) => {
      const _id = `${data.length}-${i}`;
      $$invalidate(10, els[_id] = { cell: null, input: null }, els);
      return { id: _id, value: "" };
    }));
    $$invalidate(9, data), $$invalidate(27, values), $$invalidate(29, old_val), $$invalidate(26, headers);
  }
  async function add_col() {
    if (col_count[1] !== "dynamic")
      return;
    for (let i = 0; i < data.length; i++) {
      const _id2 = `${i}-${data[i].length}`;
      $$invalidate(10, els[_id2] = { cell: null, input: null }, els);
      data[i].push({ id: _id2, value: "" });
    }
    const _id = `h-${_headers.length}`;
    $$invalidate(10, els[_id] = { cell: null, input: null }, els);
    _headers.push({
      id: _id,
      value: `Header ${_headers.length + 1}`
    });
    $$invalidate(9, data), $$invalidate(27, values), $$invalidate(29, old_val), $$invalidate(26, headers);
    $$invalidate(8, _headers), $$invalidate(26, headers), $$invalidate(28, old_headers), $$invalidate(27, values);
    await tick();
    edit_header(_id, true);
  }
  function handle_click_outside(event) {
    var _a, _b;
    if (typeof editing === "string" && els[editing]) {
      if (els[editing].cell !== event.target && !((_a = els[editing].cell) == null ? void 0 : _a.contains(event == null ? void 0 : event.target))) {
        $$invalidate(6, editing = false);
      }
    }
    if (typeof header_edit === "string" && els[header_edit]) {
      if (els[header_edit].cell !== event.target && !((_b = els[header_edit].cell) == null ? void 0 : _b.contains(event.target))) {
        $$invalidate(7, selected = header_edit);
        $$invalidate(13, header_edit = false);
        update_headers_data();
        $$invalidate(13, header_edit = false);
      }
    }
  }
  function blob_to_string(blob) {
    const reader = new FileReader();
    function handle_read(e) {
      var _a;
      if (!((_a = e == null ? void 0 : e.target) == null ? void 0 : _a.result) || typeof e.target.result !== "string")
        return;
      const [delimiter] = guess_delimitaor(e.target.result, [",", "	"]);
      const [head, ...rest] = dsvFormat(delimiter).parseRows(e.target.result);
      $$invalidate(8, _headers = make_headers(col_count[1] === "fixed" ? head.slice(0, col_count[0]) : head));
      $$invalidate(27, values = rest);
      reader.removeEventListener("loadend", handle_read);
    }
    reader.addEventListener("loadend", handle_read);
    reader.readAsText(blob);
  }
  let dragging = false;
  function editablecell_el_binding(value, id) {
    if ($$self.$$.not_equal(els[id].input, value)) {
      els[id].input = value;
      $$invalidate(10, els);
    }
  }
  const dblclick_handler = (id) => edit_header(id);
  const click_handler = (i) => handle_sort(i);
  function th_binding($$value, id) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      els[id].cell = $$value;
      $$invalidate(10, els);
    });
  }
  function editablecell_value_binding(value$1, value, each_value_1, j) {
    each_value_1[j].value = value$1;
    $$invalidate(9, data), $$invalidate(27, values), $$invalidate(29, old_val), $$invalidate(26, headers);
  }
  function editablecell_el_binding_1(value, id) {
    if ($$self.$$.not_equal(els[id].input, value)) {
      els[id].input = value;
      $$invalidate(10, els);
    }
  }
  function td_binding($$value, id) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      els[id].cell = $$value;
      $$invalidate(10, els);
    });
  }
  const touchstart_handler = (id) => start_edit(id);
  const click_handler_1 = (id) => handle_cell_click(id);
  const dblclick_handler_1 = (id) => start_edit(id);
  const keydown_handler = (i, j, id, e) => handle_keydown(e, i, j, id);
  function upload_dragging_binding(value) {
    dragging = value;
    $$invalidate(14, dragging);
  }
  const load_handler = (e) => blob_to_string(data_uri_to_blob(e.detail.data));
  const click_handler_2 = () => add_row();
  $$self.$$set = ($$props2) => {
    if ("datatype" in $$props2)
      $$invalidate(0, datatype = $$props2.datatype);
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
    if ("headers" in $$props2)
      $$invalidate(26, headers = $$props2.headers);
    if ("values" in $$props2)
      $$invalidate(27, values = $$props2.values);
    if ("col_count" in $$props2)
      $$invalidate(2, col_count = $$props2.col_count);
    if ("row_count" in $$props2)
      $$invalidate(3, row_count = $$props2.row_count);
    if ("editable" in $$props2)
      $$invalidate(4, editable = $$props2.editable);
    if ("wrap" in $$props2)
      $$invalidate(5, wrap = $$props2.wrap);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty[0] & 201326592) {
      {
        if (values && !Array.isArray(values)) {
          $$invalidate(26, headers = values.headers);
          $$invalidate(27, values = values.data.length === 0 ? [Array(headers.length).fill("")] : values.data);
        } else if (values === null) {
          $$invalidate(27, values = [Array(headers.length).fill("")]);
        } else {
          $$invalidate(27, values), $$invalidate(26, headers);
        }
      }
    }
    if ($$self.$$.dirty[0] & 335544320) {
      {
        if (!dequal(headers, old_headers)) {
          $$invalidate(8, _headers = make_headers(headers));
          $$invalidate(28, old_headers = headers);
          refresh_focus();
        }
      }
    }
    if ($$self.$$.dirty[0] & 671088640) {
      if (!dequal(values, old_val)) {
        $$invalidate(9, data = process_data(values));
        $$invalidate(29, old_val = values);
        refresh_focus();
      }
    }
    if ($$self.$$.dirty[0] & 768) {
      _headers && dispatch("change", {
        data: data.map((r) => r.map(({ value }) => value)),
        headers: _headers.map((h) => h.value)
      });
    }
    if ($$self.$$.dirty[0] & 64) {
      set_focus(editing, "edit");
    }
    if ($$self.$$.dirty[0] & 128) {
      set_focus(selected, "select");
    }
  };
  return [
    datatype,
    label,
    col_count,
    row_count,
    editable,
    wrap,
    editing,
    selected,
    _headers,
    data,
    els,
    sort_direction,
    sort_by,
    header_edit,
    dragging,
    get_sort_status,
    start_edit,
    handle_keydown,
    handle_cell_click,
    handle_sort,
    edit_header,
    end_header_edit,
    add_row,
    add_col,
    handle_click_outside,
    blob_to_string,
    headers,
    values,
    old_headers,
    old_val,
    editablecell_el_binding,
    dblclick_handler,
    click_handler,
    th_binding,
    editablecell_value_binding,
    editablecell_el_binding_1,
    td_binding,
    touchstart_handler,
    click_handler_1,
    dblclick_handler_1,
    keydown_handler,
    upload_dragging_binding,
    load_handler,
    click_handler_2
  ];
}
class Table extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      datatype: 0,
      label: 1,
      headers: 26,
      values: 27,
      col_count: 2,
      row_count: 3,
      editable: 4,
      wrap: 5
    }, null, [-1, -1]);
  }
}
function create_fragment(ctx) {
  let div;
  let statustracker;
  let t;
  let table;
  let current;
  const statustracker_spread_levels = [ctx[10]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  table = new Table({
    props: {
      label: ctx[7],
      row_count: ctx[6],
      col_count: ctx[5],
      values: ctx[0],
      headers: ctx[1],
      editable: ctx[4] === "dynamic",
      wrap: ctx[8],
      datatype: ctx[9]
    }
  });
  table.$on("change", ctx[12]);
  return {
    c() {
      div = element("div");
      create_component(statustracker.$$.fragment);
      t = space();
      create_component(table.$$.fragment);
      attr(div, "id", ctx[2]);
      attr(div, "class", "relative overflow-hidden");
      toggle_class(div, "!hidden", !ctx[3]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(statustracker, div, null);
      append(div, t);
      mount_component(table, div, null);
      current = true;
    },
    p(ctx2, [dirty]) {
      const statustracker_changes = dirty & 1024 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[10])]) : {};
      statustracker.$set(statustracker_changes);
      const table_changes = {};
      if (dirty & 128)
        table_changes.label = ctx2[7];
      if (dirty & 64)
        table_changes.row_count = ctx2[6];
      if (dirty & 32)
        table_changes.col_count = ctx2[5];
      if (dirty & 1)
        table_changes.values = ctx2[0];
      if (dirty & 2)
        table_changes.headers = ctx2[1];
      if (dirty & 16)
        table_changes.editable = ctx2[4] === "dynamic";
      if (dirty & 256)
        table_changes.wrap = ctx2[8];
      if (dirty & 512)
        table_changes.datatype = ctx2[9];
      table.$set(table_changes);
      if (!current || dirty & 4) {
        attr(div, "id", ctx2[2]);
      }
      if (dirty & 8) {
        toggle_class(div, "!hidden", !ctx2[3]);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(table.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(table.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(statustracker);
      destroy_component(table);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { headers = [] } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = {
    data: [["", "", ""]],
    headers: ["1", "2", "3"]
  } } = $$props;
  let { mode } = $$props;
  let { col_count } = $$props;
  let { row_count } = $$props;
  let { label = null } = $$props;
  let { wrap } = $$props;
  let { datatype } = $$props;
  const dispatch = createEventDispatcher();
  let { loading_status } = $$props;
  async function handle_change(detail) {
    $$invalidate(0, value = detail);
    await tick();
    dispatch("change", detail);
  }
  const change_handler = ({ detail }) => handle_change(detail);
  $$self.$$set = ($$props2) => {
    if ("headers" in $$props2)
      $$invalidate(1, headers = $$props2.headers);
    if ("elem_id" in $$props2)
      $$invalidate(2, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(3, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("mode" in $$props2)
      $$invalidate(4, mode = $$props2.mode);
    if ("col_count" in $$props2)
      $$invalidate(5, col_count = $$props2.col_count);
    if ("row_count" in $$props2)
      $$invalidate(6, row_count = $$props2.row_count);
    if ("label" in $$props2)
      $$invalidate(7, label = $$props2.label);
    if ("wrap" in $$props2)
      $$invalidate(8, wrap = $$props2.wrap);
    if ("datatype" in $$props2)
      $$invalidate(9, datatype = $$props2.datatype);
    if ("loading_status" in $$props2)
      $$invalidate(10, loading_status = $$props2.loading_status);
  };
  return [
    value,
    headers,
    elem_id,
    visible,
    mode,
    col_count,
    row_count,
    label,
    wrap,
    datatype,
    loading_status,
    handle_change,
    change_handler
  ];
}
class DataFrame extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      headers: 1,
      elem_id: 2,
      visible: 3,
      value: 0,
      mode: 4,
      col_count: 5,
      row_count: 6,
      label: 7,
      wrap: 8,
      datatype: 9,
      loading_status: 10
    });
  }
}
var DataFrame$1 = DataFrame;
const modes = ["static", "dynamic"];
const document = (config) => {
  var _a;
  return {
    type: " { data: Array<Array<string | number>>; headers: Array<string> }",
    description: "hex color code",
    example_data: (_a = config.value) != null ? _a : "#000000"
  };
};
export { DataFrame$1 as Component, document, modes };
//# sourceMappingURL=index12.js.map
