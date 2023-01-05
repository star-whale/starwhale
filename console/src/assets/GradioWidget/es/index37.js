var __defProp = Object.defineProperty;
var __defProps = Object.defineProperties;
var __getOwnPropDescs = Object.getOwnPropertyDescriptors;
var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __spreadValues = (a, b) => {
  for (var prop in b || (b = {}))
    if (__hasOwnProp.call(b, prop))
      __defNormalProp(a, prop, b[prop]);
  if (__getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(b)) {
      if (__propIsEnum.call(b, prop))
        __defNormalProp(a, prop, b[prop]);
    }
  return a;
};
var __spreadProps = (a, b) => __defProps(a, __getOwnPropDescs(b));
import { S as SvelteComponent, i as init, s as safe_not_equal, w as svg_element, b as attr, f as insert, g as append, x as noop, n as detach, e as element, t as text, Y as set_style, ad as add_render_callback, ah as add_resize_listener, h as set_data, a as space, C as destroy_each, B as empty, N as action_destroyer, y as is_function, F as createEventDispatcher, ac as onMount, a5 as colors, P as Block, c as create_component, m as mount_component, j as transition_in, k as transition_out, o as destroy_component, Q as component_subscribe, X, R as assign, T as StatusTracker, U as get_spread_update, V as get_spread_object, D as group_outros, E as check_outros } from "./main.js";
import { U as Upload } from "./Upload.js";
import { M as ModifyUpload } from "./ModifyUpload.js";
import { B as BlockLabel } from "./BlockLabel.js";
import { g as get_next_color } from "./color.js";
import { _ as _line, m as curveLinear, Z as linear } from "./linear.js";
import { a as csvParse } from "./csv.js";
import "./dsv.js";
function create_fragment$3(ctx) {
  let svg;
  let path0;
  let path1;
  return {
    c() {
      svg = svg_element("svg");
      path0 = svg_element("path");
      path1 = svg_element("path");
      attr(path0, "d", "M28.828 3.172a4.094 4.094 0 0 0-5.656 0L4.05 22.292A6.954 6.954 0 0 0 2 27.242V30h2.756a6.952 6.952 0 0 0 4.95-2.05L28.828 8.829a3.999 3.999 0 0 0 0-5.657zM10.91 18.26l2.829 2.829l-2.122 2.121l-2.828-2.828zm-2.619 8.276A4.966 4.966 0 0 1 4.756 28H4v-.759a4.967 4.967 0 0 1 1.464-3.535l1.91-1.91l2.829 2.828zM27.415 7.414l-12.261 12.26l-2.829-2.828l12.262-12.26a2.047 2.047 0 0 1 2.828 0a2 2 0 0 1 0 2.828z");
      attr(path0, "fill", "currentColor");
      attr(path1, "d", "M6.5 15a3.5 3.5 0 0 1-2.475-5.974l3.5-3.5a1.502 1.502 0 0 0 0-2.121a1.537 1.537 0 0 0-2.121 0L3.415 5.394L2 3.98l1.99-1.988a3.585 3.585 0 0 1 4.95 0a3.504 3.504 0 0 1 0 4.949L5.439 10.44a1.502 1.502 0 0 0 0 2.121a1.537 1.537 0 0 0 2.122 0l4.024-4.024L13 9.95l-4.025 4.024A3.475 3.475 0 0 1 6.5 15z");
      attr(path1, "fill", "currentColor");
      attr(svg, "width", "1em");
      attr(svg, "height", "1em");
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
class Chart$1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$3, safe_not_equal, {});
  }
}
function get_domains(values) {
  let _vs;
  if (Array.isArray(values)) {
    _vs = values.reduce((acc, { values: values2 }) => {
      return [...acc, ...values2.map(({ y }) => y)];
    }, []);
  } else {
    _vs = values.values;
  }
  return [Math.min(..._vs), Math.max(..._vs)];
}
function transform_values(values, x, y) {
  const transformed_values = Object.entries(values[0]).reduce((acc, next, i) => {
    if (!x && i === 0 || x && next[0] === x) {
      acc.x.name = next[0];
    } else if (!y || y && y.includes(next[0])) {
      acc.y.push({ name: next[0], values: [] });
    }
    return acc;
  }, { x: { name: "", values: [] }, y: [] });
  for (let i = 0; i < values.length; i++) {
    const _a = Object.entries(values[i]);
    for (let j = 0; j < _a.length; j++) {
      let [name, x2] = _a[j];
      if (name === transformed_values.x.name) {
        transformed_values.x.values.push(parseFloat(x2));
      } else {
        transformed_values.y[j - 1].values.push({
          y: parseFloat(_a[j][1]),
          x: parseFloat(_a[0][1])
        });
      }
    }
  }
  return transformed_values;
}
function create_fragment$2(ctx) {
  let div;
  let span;
  let t;
  let div_resize_listener;
  return {
    c() {
      div = element("div");
      span = element("span");
      t = text(ctx[0]);
      attr(span, "class", "inline-block w-3 h-3 mr-1 rounded-sm");
      set_style(span, "background", ctx[3]);
      attr(div, "class", "bg-black bg-opacity-80 text-white py-1 px-[0.4rem] absolute text-xs flex items-center justify-center rounded");
      set_style(div, "top", ctx[2] - ctx[5] / 2 + "px");
      set_style(div, "left", ctx[1] - ctx[4] - 7 + "px");
      add_render_callback(() => ctx[6].call(div));
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, span);
      append(div, t);
      div_resize_listener = add_resize_listener(div, ctx[6].bind(div));
    },
    p(ctx2, [dirty]) {
      if (dirty & 8) {
        set_style(span, "background", ctx2[3]);
      }
      if (dirty & 1)
        set_data(t, ctx2[0]);
      if (dirty & 36) {
        set_style(div, "top", ctx2[2] - ctx2[5] / 2 + "px");
      }
      if (dirty & 18) {
        set_style(div, "left", ctx2[1] - ctx2[4] - 7 + "px");
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
      div_resize_listener();
    }
  };
}
function instance$2($$self, $$props, $$invalidate) {
  let { text: text2 } = $$props;
  let { x } = $$props;
  let { y } = $$props;
  let { color } = $$props;
  let w;
  let h;
  function div_elementresize_handler() {
    w = this.offsetWidth;
    h = this.offsetHeight;
    $$invalidate(4, w);
    $$invalidate(5, h);
  }
  $$self.$$set = ($$props2) => {
    if ("text" in $$props2)
      $$invalidate(0, text2 = $$props2.text);
    if ("x" in $$props2)
      $$invalidate(1, x = $$props2.x);
    if ("y" in $$props2)
      $$invalidate(2, y = $$props2.y);
    if ("color" in $$props2)
      $$invalidate(3, color = $$props2.color);
  };
  return [text2, x, y, color, w, h, div_elementresize_handler];
}
class Tooltip extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$2, create_fragment$2, safe_not_equal, { text: 0, x: 1, y: 2, color: 3 });
  }
}
function tooltip(element2, { color, text: text2 }) {
  let tooltipComponent;
  function mouse_over(event) {
    tooltipComponent = new Tooltip({
      props: {
        text: text2,
        x: event.pageX,
        y: event.pageY,
        color
      },
      target: document.body
    });
    return event;
  }
  function mouseMove(event) {
    tooltipComponent.$set({
      x: event.pageX,
      y: event.pageY
    });
  }
  function mouseLeave() {
    tooltipComponent.$destroy();
  }
  const el = element2;
  el.addEventListener("mouseover", mouse_over);
  el.addEventListener("mouseleave", mouseLeave);
  el.addEventListener("mousemove", mouseMove);
  return {
    destroy() {
      el.removeEventListener("mouseover", mouse_over);
      el.removeEventListener("mouseleave", mouseLeave);
      el.removeEventListener("mousemove", mouseMove);
    }
  };
}
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[16] = list[i].name;
  child_ctx[17] = list[i].values;
  const constants_0 = child_ctx[8][child_ctx[16]];
  child_ctx[18] = constants_0;
  return child_ctx;
}
function get_each_context_1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[0] = list[i].x;
  child_ctx[1] = list[i].y;
  return child_ctx;
}
function get_each_context_2(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[16] = list[i].name;
  child_ctx[17] = list[i].values;
  const constants_0 = child_ctx[8][child_ctx[16]];
  child_ctx[18] = constants_0;
  return child_ctx;
}
function get_each_context_3(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[0] = list[i].x;
  child_ctx[1] = list[i].y;
  return child_ctx;
}
function get_each_context_4(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[27] = list[i];
  return child_ctx;
}
function get_each_context_5(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[27] = list[i];
  return child_ctx;
}
function get_each_context_6(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[16] = list[i].name;
  return child_ctx;
}
function create_each_block_6(ctx) {
  let div;
  let span;
  let t0;
  let t1_value = ctx[16] + "";
  let t1;
  let t2;
  return {
    c() {
      div = element("div");
      span = element("span");
      t0 = space();
      t1 = text(t1_value);
      t2 = space();
      attr(span, "class", "inline-block w-[12px] h-[12px] rounded-sm ");
      set_style(span, "background-color", ctx[8][ctx[16]]);
      attr(div, "class", "mx-2 flex gap-1 items-center");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, span);
      append(div, t0);
      append(div, t1);
      append(div, t2);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 260) {
        set_style(span, "background-color", ctx2[8][ctx2[16]]);
      }
      if (dirty[0] & 4 && t1_value !== (t1_value = ctx2[16] + ""))
        set_data(t1, t1_value);
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_each_block_5(ctx) {
  let line;
  let line_x__value;
  let line_x__value_1;
  let line_y__value;
  let line_y__value_1;
  let text_1;
  let t_value = ctx[27] + "";
  let t;
  let text_1_x_value;
  let text_1_y_value;
  return {
    c() {
      line = svg_element("line");
      text_1 = svg_element("text");
      t = text(t_value);
      attr(line, "stroke-width", "0.5");
      attr(line, "x1", line_x__value = ctx[5](ctx[27]));
      attr(line, "x2", line_x__value_1 = ctx[5](ctx[27]));
      attr(line, "y1", line_y__value = ctx[4](ctx[9][0] < ctx[6][0] ? ctx[9][0] : ctx[6][0]) + 10);
      attr(line, "y2", line_y__value_1 = ctx[4](ctx[6][1] > ctx[9][ctx[9].length - 1] ? ctx[6][1] : ctx[9][ctx[9].length - 1]));
      attr(line, "stroke", "#aaa");
      attr(text_1, "class", "font-mono text-xs dark:fill-slate-200");
      attr(text_1, "text-anchor", "middle");
      attr(text_1, "x", text_1_x_value = ctx[5](ctx[27]));
      attr(text_1, "y", text_1_y_value = ctx[4](ctx[9][0]) + 30);
    },
    m(target, anchor) {
      insert(target, line, anchor);
      insert(target, text_1, anchor);
      append(text_1, t);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 1056 && line_x__value !== (line_x__value = ctx2[5](ctx2[27]))) {
        attr(line, "x1", line_x__value);
      }
      if (dirty[0] & 1056 && line_x__value_1 !== (line_x__value_1 = ctx2[5](ctx2[27]))) {
        attr(line, "x2", line_x__value_1);
      }
      if (dirty[0] & 592 && line_y__value !== (line_y__value = ctx2[4](ctx2[9][0] < ctx2[6][0] ? ctx2[9][0] : ctx2[6][0]) + 10)) {
        attr(line, "y1", line_y__value);
      }
      if (dirty[0] & 592 && line_y__value_1 !== (line_y__value_1 = ctx2[4](ctx2[6][1] > ctx2[9][ctx2[9].length - 1] ? ctx2[6][1] : ctx2[9][ctx2[9].length - 1]))) {
        attr(line, "y2", line_y__value_1);
      }
      if (dirty[0] & 1024 && t_value !== (t_value = ctx2[27] + ""))
        set_data(t, t_value);
      if (dirty[0] & 1056 && text_1_x_value !== (text_1_x_value = ctx2[5](ctx2[27]))) {
        attr(text_1, "x", text_1_x_value);
      }
      if (dirty[0] & 528 && text_1_y_value !== (text_1_y_value = ctx2[4](ctx2[9][0]) + 30)) {
        attr(text_1, "y", text_1_y_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(line);
      if (detaching)
        detach(text_1);
    }
  };
}
function create_each_block_4(ctx) {
  let line;
  let line_y__value;
  let line_y__value_1;
  let line_x__value;
  let line_x__value_1;
  let text_1;
  let t_value = ctx[27] + "";
  let t;
  let text_1_y_value;
  let text_1_x_value;
  return {
    c() {
      line = svg_element("line");
      text_1 = svg_element("text");
      t = text(t_value);
      attr(line, "stroke-width", "0.5");
      attr(line, "y1", line_y__value = ctx[4](ctx[27]));
      attr(line, "y2", line_y__value_1 = ctx[4](ctx[27]));
      attr(line, "x1", line_x__value = ctx[5](ctx[10][0] < ctx[7][0] ? ctx[10][0] : ctx[7][0]) - 10);
      attr(line, "x2", line_x__value_1 = ctx[5](ctx[7][1] > ctx[10][ctx[10].length - 1] ? ctx[7][1] : ctx[10][ctx[10].length - 1]));
      attr(line, "stroke", "#aaa");
      attr(text_1, "class", "font-mono text-xs dark:fill-slate-200");
      attr(text_1, "text-anchor", "end");
      attr(text_1, "y", text_1_y_value = ctx[4](ctx[27]) + 4);
      attr(text_1, "x", text_1_x_value = ctx[5](ctx[10][0]) - 20);
    },
    m(target, anchor) {
      insert(target, line, anchor);
      insert(target, text_1, anchor);
      append(text_1, t);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 528 && line_y__value !== (line_y__value = ctx2[4](ctx2[27]))) {
        attr(line, "y1", line_y__value);
      }
      if (dirty[0] & 528 && line_y__value_1 !== (line_y__value_1 = ctx2[4](ctx2[27]))) {
        attr(line, "y2", line_y__value_1);
      }
      if (dirty[0] & 1184 && line_x__value !== (line_x__value = ctx2[5](ctx2[10][0] < ctx2[7][0] ? ctx2[10][0] : ctx2[7][0]) - 10)) {
        attr(line, "x1", line_x__value);
      }
      if (dirty[0] & 1184 && line_x__value_1 !== (line_x__value_1 = ctx2[5](ctx2[7][1] > ctx2[10][ctx2[10].length - 1] ? ctx2[7][1] : ctx2[10][ctx2[10].length - 1]))) {
        attr(line, "x2", line_x__value_1);
      }
      if (dirty[0] & 512 && t_value !== (t_value = ctx2[27] + ""))
        set_data(t, t_value);
      if (dirty[0] & 528 && text_1_y_value !== (text_1_y_value = ctx2[4](ctx2[27]) + 4)) {
        attr(text_1, "y", text_1_y_value);
      }
      if (dirty[0] & 1056 && text_1_x_value !== (text_1_x_value = ctx2[5](ctx2[10][0]) - 20)) {
        attr(text_1, "x", text_1_x_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(line);
      if (detaching)
        detach(text_1);
    }
  };
}
function create_if_block$1(ctx) {
  let line;
  let line_y__value;
  let line_y__value_1;
  let line_x__value;
  let line_x__value_1;
  let text_1;
  let t_value = ctx[6][1] + "";
  let t;
  let text_1_y_value;
  let text_1_x_value;
  return {
    c() {
      line = svg_element("line");
      text_1 = svg_element("text");
      t = text(t_value);
      attr(line, "stroke-width", "0.5");
      attr(line, "y1", line_y__value = ctx[4](ctx[6][1]));
      attr(line, "y2", line_y__value_1 = ctx[4](ctx[6][1]));
      attr(line, "x1", line_x__value = ctx[5](ctx[10][0]));
      attr(line, "x2", line_x__value_1 = ctx[5](ctx[7][1]));
      attr(line, "stroke", "#aaa");
      attr(text_1, "class", "font-mono text-xs dark:fill-slate-200");
      attr(text_1, "text-anchor", "end");
      attr(text_1, "y", text_1_y_value = ctx[4](ctx[6][1]) + 4);
      attr(text_1, "x", text_1_x_value = ctx[5](ctx[10][0]) - 20);
    },
    m(target, anchor) {
      insert(target, line, anchor);
      insert(target, text_1, anchor);
      append(text_1, t);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 80 && line_y__value !== (line_y__value = ctx2[4](ctx2[6][1]))) {
        attr(line, "y1", line_y__value);
      }
      if (dirty[0] & 80 && line_y__value_1 !== (line_y__value_1 = ctx2[4](ctx2[6][1]))) {
        attr(line, "y2", line_y__value_1);
      }
      if (dirty[0] & 1056 && line_x__value !== (line_x__value = ctx2[5](ctx2[10][0]))) {
        attr(line, "x1", line_x__value);
      }
      if (dirty[0] & 160 && line_x__value_1 !== (line_x__value_1 = ctx2[5](ctx2[7][1]))) {
        attr(line, "x2", line_x__value_1);
      }
      if (dirty[0] & 64 && t_value !== (t_value = ctx2[6][1] + ""))
        set_data(t, t_value);
      if (dirty[0] & 80 && text_1_y_value !== (text_1_y_value = ctx2[4](ctx2[6][1]) + 4)) {
        attr(text_1, "y", text_1_y_value);
      }
      if (dirty[0] & 1056 && text_1_x_value !== (text_1_x_value = ctx2[5](ctx2[10][0]) - 20)) {
        attr(text_1, "x", text_1_x_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(line);
      if (detaching)
        detach(text_1);
    }
  };
}
function create_each_block_3(ctx) {
  let circle;
  let circle_cx_value;
  let circle_cy_value;
  let circle_stroke_value;
  return {
    c() {
      circle = svg_element("circle");
      attr(circle, "r", "3.5");
      attr(circle, "cx", circle_cx_value = ctx[5](ctx[0]));
      attr(circle, "cy", circle_cy_value = ctx[4](ctx[1]));
      attr(circle, "stroke-width", "1.5");
      attr(circle, "stroke", circle_stroke_value = ctx[18]);
      attr(circle, "fill", "none");
    },
    m(target, anchor) {
      insert(target, circle, anchor);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 36 && circle_cx_value !== (circle_cx_value = ctx2[5](ctx2[0]))) {
        attr(circle, "cx", circle_cx_value);
      }
      if (dirty[0] & 20 && circle_cy_value !== (circle_cy_value = ctx2[4](ctx2[1]))) {
        attr(circle, "cy", circle_cy_value);
      }
      if (dirty[0] & 260 && circle_stroke_value !== (circle_stroke_value = ctx2[18])) {
        attr(circle, "stroke", circle_stroke_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(circle);
    }
  };
}
function create_each_block_2(ctx) {
  let path;
  let path_d_value;
  let path_stroke_value;
  let each_value_3 = ctx[17];
  let each_blocks = [];
  for (let i = 0; i < each_value_3.length; i += 1) {
    each_blocks[i] = create_each_block_3(get_each_context_3(ctx, each_value_3, i));
  }
  return {
    c() {
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      path = svg_element("path");
      attr(path, "d", path_d_value = _line().curve(curveLinear)(ctx[17].map(ctx[13])));
      attr(path, "fill", "none");
      attr(path, "stroke", path_stroke_value = ctx[18]);
      attr(path, "stroke-width", "3");
    },
    m(target, anchor) {
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(target, anchor);
      }
      insert(target, path, anchor);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 308) {
        each_value_3 = ctx2[17];
        let i;
        for (i = 0; i < each_value_3.length; i += 1) {
          const child_ctx = get_each_context_3(ctx2, each_value_3, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block_3(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(path.parentNode, path);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value_3.length;
      }
      if (dirty[0] & 52 && path_d_value !== (path_d_value = _line().curve(curveLinear)(ctx2[17].map(ctx2[13])))) {
        attr(path, "d", path_d_value);
      }
      if (dirty[0] & 260 && path_stroke_value !== (path_stroke_value = ctx2[18])) {
        attr(path, "stroke", path_stroke_value);
      }
    },
    d(detaching) {
      destroy_each(each_blocks, detaching);
      if (detaching)
        detach(path);
    }
  };
}
function create_each_block_1(ctx) {
  let circle;
  let circle_cx_value;
  let circle_cy_value;
  let tooltip_action;
  let mounted;
  let dispose;
  return {
    c() {
      circle = svg_element("circle");
      attr(circle, "r", "7");
      attr(circle, "cx", circle_cx_value = ctx[5](ctx[0]));
      attr(circle, "cy", circle_cy_value = ctx[4](ctx[1]));
      attr(circle, "stroke", "black");
      attr(circle, "fill", "black");
      set_style(circle, "cursor", "pointer");
      set_style(circle, "opacity", "0");
    },
    m(target, anchor) {
      insert(target, circle, anchor);
      if (!mounted) {
        dispose = action_destroyer(tooltip_action = tooltip.call(null, circle, {
          color: ctx[18],
          text: `(${ctx[0]}, ${ctx[1]})`
        }));
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty[0] & 36 && circle_cx_value !== (circle_cx_value = ctx[5](ctx[0]))) {
        attr(circle, "cx", circle_cx_value);
      }
      if (dirty[0] & 20 && circle_cy_value !== (circle_cy_value = ctx[4](ctx[1]))) {
        attr(circle, "cy", circle_cy_value);
      }
      if (tooltip_action && is_function(tooltip_action.update) && dirty[0] & 260)
        tooltip_action.update.call(null, {
          color: ctx[18],
          text: `(${ctx[0]}, ${ctx[1]})`
        });
    },
    d(detaching) {
      if (detaching)
        detach(circle);
      mounted = false;
      dispose();
    }
  };
}
function create_each_block(ctx) {
  let each_1_anchor;
  let each_value_1 = ctx[17];
  let each_blocks = [];
  for (let i = 0; i < each_value_1.length; i += 1) {
    each_blocks[i] = create_each_block_1(get_each_context_1(ctx, each_value_1, i));
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
      if (dirty[0] & 308) {
        each_value_1 = ctx2[17];
        let i;
        for (i = 0; i < each_value_1.length; i += 1) {
          const child_ctx = get_each_context_1(ctx2, each_value_1, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block_1(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(each_1_anchor.parentNode, each_1_anchor);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value_1.length;
      }
    },
    d(detaching) {
      destroy_each(each_blocks, detaching);
      if (detaching)
        detach(each_1_anchor);
    }
  };
}
function create_fragment$1(ctx) {
  let div2;
  let div0;
  let t0;
  let svg;
  let g;
  let each1_anchor;
  let each2_anchor;
  let each3_anchor;
  let t1;
  let div1;
  let t2_value = ctx[3].name + "";
  let t2;
  let each_value_6 = ctx[2];
  let each_blocks_4 = [];
  for (let i = 0; i < each_value_6.length; i += 1) {
    each_blocks_4[i] = create_each_block_6(get_each_context_6(ctx, each_value_6, i));
  }
  let each_value_5 = ctx[10];
  let each_blocks_3 = [];
  for (let i = 0; i < each_value_5.length; i += 1) {
    each_blocks_3[i] = create_each_block_5(get_each_context_5(ctx, each_value_5, i));
  }
  let each_value_4 = ctx[9];
  let each_blocks_2 = [];
  for (let i = 0; i < each_value_4.length; i += 1) {
    each_blocks_2[i] = create_each_block_4(get_each_context_4(ctx, each_value_4, i));
  }
  let if_block = ctx[6][1] > ctx[9][ctx[9].length - 1] && create_if_block$1(ctx);
  let each_value_2 = ctx[2];
  let each_blocks_1 = [];
  for (let i = 0; i < each_value_2.length; i += 1) {
    each_blocks_1[i] = create_each_block_2(get_each_context_2(ctx, each_value_2, i));
  }
  let each_value = ctx[2];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  return {
    c() {
      div2 = element("div");
      div0 = element("div");
      for (let i = 0; i < each_blocks_4.length; i += 1) {
        each_blocks_4[i].c();
      }
      t0 = space();
      svg = svg_element("svg");
      g = svg_element("g");
      for (let i = 0; i < each_blocks_3.length; i += 1) {
        each_blocks_3[i].c();
      }
      each1_anchor = empty();
      for (let i = 0; i < each_blocks_2.length; i += 1) {
        each_blocks_2[i].c();
      }
      each2_anchor = empty();
      if (if_block)
        if_block.c();
      for (let i = 0; i < each_blocks_1.length; i += 1) {
        each_blocks_1[i].c();
      }
      each3_anchor = empty();
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      t1 = space();
      div1 = element("div");
      t2 = text(t2_value);
      attr(div0, "class", "flex justify-center items-center text-sm dark:text-slate-200");
      attr(svg, "class", "w-full");
      attr(svg, "viewBox", "-70 -20 700 420");
      attr(div1, "class", "flex justify-center align-items-center text-sm dark:text-slate-200");
      attr(div2, "class", "mt-3");
    },
    m(target, anchor) {
      insert(target, div2, anchor);
      append(div2, div0);
      for (let i = 0; i < each_blocks_4.length; i += 1) {
        each_blocks_4[i].m(div0, null);
      }
      append(div2, t0);
      append(div2, svg);
      append(svg, g);
      for (let i = 0; i < each_blocks_3.length; i += 1) {
        each_blocks_3[i].m(g, null);
      }
      append(g, each1_anchor);
      for (let i = 0; i < each_blocks_2.length; i += 1) {
        each_blocks_2[i].m(g, null);
      }
      append(g, each2_anchor);
      if (if_block)
        if_block.m(g, null);
      for (let i = 0; i < each_blocks_1.length; i += 1) {
        each_blocks_1[i].m(svg, null);
      }
      append(svg, each3_anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(svg, null);
      }
      append(div2, t1);
      append(div2, div1);
      append(div1, t2);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 260) {
        each_value_6 = ctx2[2];
        let i;
        for (i = 0; i < each_value_6.length; i += 1) {
          const child_ctx = get_each_context_6(ctx2, each_value_6, i);
          if (each_blocks_4[i]) {
            each_blocks_4[i].p(child_ctx, dirty);
          } else {
            each_blocks_4[i] = create_each_block_6(child_ctx);
            each_blocks_4[i].c();
            each_blocks_4[i].m(div0, null);
          }
        }
        for (; i < each_blocks_4.length; i += 1) {
          each_blocks_4[i].d(1);
        }
        each_blocks_4.length = each_value_6.length;
      }
      if (dirty[0] & 1648) {
        each_value_5 = ctx2[10];
        let i;
        for (i = 0; i < each_value_5.length; i += 1) {
          const child_ctx = get_each_context_5(ctx2, each_value_5, i);
          if (each_blocks_3[i]) {
            each_blocks_3[i].p(child_ctx, dirty);
          } else {
            each_blocks_3[i] = create_each_block_5(child_ctx);
            each_blocks_3[i].c();
            each_blocks_3[i].m(g, each1_anchor);
          }
        }
        for (; i < each_blocks_3.length; i += 1) {
          each_blocks_3[i].d(1);
        }
        each_blocks_3.length = each_value_5.length;
      }
      if (dirty[0] & 1712) {
        each_value_4 = ctx2[9];
        let i;
        for (i = 0; i < each_value_4.length; i += 1) {
          const child_ctx = get_each_context_4(ctx2, each_value_4, i);
          if (each_blocks_2[i]) {
            each_blocks_2[i].p(child_ctx, dirty);
          } else {
            each_blocks_2[i] = create_each_block_4(child_ctx);
            each_blocks_2[i].c();
            each_blocks_2[i].m(g, each2_anchor);
          }
        }
        for (; i < each_blocks_2.length; i += 1) {
          each_blocks_2[i].d(1);
        }
        each_blocks_2.length = each_value_4.length;
      }
      if (ctx2[6][1] > ctx2[9][ctx2[9].length - 1]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block$1(ctx2);
          if_block.c();
          if_block.m(g, null);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty[0] & 308) {
        each_value_2 = ctx2[2];
        let i;
        for (i = 0; i < each_value_2.length; i += 1) {
          const child_ctx = get_each_context_2(ctx2, each_value_2, i);
          if (each_blocks_1[i]) {
            each_blocks_1[i].p(child_ctx, dirty);
          } else {
            each_blocks_1[i] = create_each_block_2(child_ctx);
            each_blocks_1[i].c();
            each_blocks_1[i].m(svg, each3_anchor);
          }
        }
        for (; i < each_blocks_1.length; i += 1) {
          each_blocks_1[i].d(1);
        }
        each_blocks_1.length = each_value_2.length;
      }
      if (dirty[0] & 308) {
        each_value = ctx2[2];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(svg, null);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
      if (dirty[0] & 8 && t2_value !== (t2_value = ctx2[3].name + ""))
        set_data(t2, t2_value);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div2);
      destroy_each(each_blocks_4, detaching);
      destroy_each(each_blocks_3, detaching);
      destroy_each(each_blocks_2, detaching);
      if (if_block)
        if_block.d();
      destroy_each(each_blocks_1, detaching);
      destroy_each(each_blocks, detaching);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let _x;
  let _y;
  let x_domain;
  let y_domain;
  let scale_x;
  let scale_y;
  let x_ticks;
  let y_ticks;
  let { value } = $$props;
  let { x = void 0 } = $$props;
  let { y = void 0 } = $$props;
  let { colors: colors$1 = [] } = $$props;
  const dispatch = createEventDispatcher();
  let color_map;
  function get_color(index) {
    var _a;
    let current_color = colors$1[index % colors$1.length];
    if (current_color && current_color in colors) {
      return (_a = colors[current_color]) == null ? void 0 : _a.primary;
    } else if (!current_color) {
      return colors[get_next_color(index)].primary;
    } else {
      return current_color;
    }
  }
  onMount(() => {
    dispatch("process", { x: _x, y: _y });
  });
  const func = ({ x: x2, y: y2 }) => [scale_x(x2), scale_y(y2)];
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(11, value = $$props2.value);
    if ("x" in $$props2)
      $$invalidate(0, x = $$props2.x);
    if ("y" in $$props2)
      $$invalidate(1, y = $$props2.y);
    if ("colors" in $$props2)
      $$invalidate(12, colors$1 = $$props2.colors);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty[0] & 2051) {
      $$invalidate(3, { x: _x, y: _y } = typeof value === "string" ? transform_values(csvParse(value), x, y) : transform_values(value, x, y), _x, ($$invalidate(2, _y), $$invalidate(11, value), $$invalidate(0, x), $$invalidate(1, y)));
    }
    if ($$self.$$.dirty[0] & 8) {
      $$invalidate(7, x_domain = get_domains(_x));
    }
    if ($$self.$$.dirty[0] & 4) {
      $$invalidate(6, y_domain = get_domains(_y));
    }
    if ($$self.$$.dirty[0] & 128) {
      $$invalidate(5, scale_x = linear(x_domain, [0, 600]).nice());
    }
    if ($$self.$$.dirty[0] & 64) {
      $$invalidate(4, scale_y = linear(y_domain, [350, 0]).nice());
    }
    if ($$self.$$.dirty[0] & 32) {
      $$invalidate(10, x_ticks = scale_x.ticks(8));
    }
    if ($$self.$$.dirty[0] & 16) {
      $$invalidate(9, y_ticks = scale_y.ticks(8));
    }
    if ($$self.$$.dirty[0] & 4) {
      $$invalidate(8, color_map = _y.reduce((acc, next, i) => __spreadProps(__spreadValues({}, acc), { [next.name]: get_color(i) }), {}));
    }
  };
  return [
    x,
    y,
    _y,
    _x,
    scale_y,
    scale_x,
    y_domain,
    x_domain,
    color_map,
    y_ticks,
    x_ticks,
    value,
    colors$1,
    func
  ];
}
class Chart extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { value: 11, x: 0, y: 1, colors: 12 }, null, [-1, -1]);
  }
}
function create_if_block_3(ctx) {
  let div;
  let upload;
  let current;
  upload = new Upload({
    props: {
      filetype: "text/csv",
      include_file_metadata: false,
      $$slots: { default: [create_default_slot_1] },
      $$scope: { ctx }
    }
  });
  upload.$on("load", ctx[16]);
  return {
    c() {
      div = element("div");
      create_component(upload.$$.fragment);
      attr(div, "class", "h-full min-h-[8rem]");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(upload, div, null);
      current = true;
    },
    p(ctx2, dirty) {
      const upload_changes = {};
      if (dirty & 1052672) {
        upload_changes.$$scope = { dirty, ctx: ctx2 };
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
      if (detaching)
        detach(div);
      destroy_component(upload);
    }
  };
}
function create_if_block_2(ctx) {
  let div;
  let modifyupload;
  let t;
  let chart;
  let current;
  modifyupload = new ModifyUpload({});
  modifyupload.$on("clear", ctx[14]);
  chart = new Chart({
    props: {
      value: ctx[10],
      y: ctx[3],
      x: ctx[4],
      colors: ctx[8]
    }
  });
  chart.$on("process", ctx[15]);
  return {
    c() {
      div = element("div");
      create_component(modifyupload.$$.fragment);
      t = space();
      create_component(chart.$$.fragment);
      attr(div, "class", "input-model w-full h-60 flex justify-center items-center bg-gray-200 dark:bg-gray-600 relative");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(modifyupload, div, null);
      append(div, t);
      mount_component(chart, div, null);
      current = true;
    },
    p(ctx2, dirty) {
      const chart_changes = {};
      if (dirty & 1024)
        chart_changes.value = ctx2[10];
      if (dirty & 8)
        chart_changes.y = ctx2[3];
      if (dirty & 16)
        chart_changes.x = ctx2[4];
      if (dirty & 256)
        chart_changes.colors = ctx2[8];
      chart.$set(chart_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(modifyupload.$$.fragment, local);
      transition_in(chart.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(modifyupload.$$.fragment, local);
      transition_out(chart.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(modifyupload);
      destroy_component(chart);
    }
  };
}
function create_if_block(ctx) {
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  const if_block_creators = [create_if_block_1, create_else_block];
  const if_blocks = [];
  function select_block_type_1(ctx2, dirty) {
    if (ctx2[11])
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
function create_default_slot_1(ctx) {
  let t0_value = ctx[12]("interface.drop_csv") + "";
  let t0;
  let t1;
  let br0;
  let t2;
  let t3_value = ctx[12]("or") + "";
  let t3;
  let t4;
  let br1;
  let t5;
  let t6_value = ctx[12]("interface.click_to_upload") + "";
  let t6;
  return {
    c() {
      t0 = text(t0_value);
      t1 = space();
      br0 = element("br");
      t2 = text("- ");
      t3 = text(t3_value);
      t4 = text(" -");
      br1 = element("br");
      t5 = space();
      t6 = text(t6_value);
    },
    m(target, anchor) {
      insert(target, t0, anchor);
      insert(target, t1, anchor);
      insert(target, br0, anchor);
      insert(target, t2, anchor);
      insert(target, t3, anchor);
      insert(target, t4, anchor);
      insert(target, br1, anchor);
      insert(target, t5, anchor);
      insert(target, t6, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 4096 && t0_value !== (t0_value = ctx2[12]("interface.drop_csv") + ""))
        set_data(t0, t0_value);
      if (dirty & 4096 && t3_value !== (t3_value = ctx2[12]("or") + ""))
        set_data(t3, t3_value);
      if (dirty & 4096 && t6_value !== (t6_value = ctx2[12]("interface.click_to_upload") + ""))
        set_data(t6, t6_value);
    },
    d(detaching) {
      if (detaching)
        detach(t0);
      if (detaching)
        detach(t1);
      if (detaching)
        detach(br0);
      if (detaching)
        detach(t2);
      if (detaching)
        detach(t3);
      if (detaching)
        detach(t4);
      if (detaching)
        detach(br1);
      if (detaching)
        detach(t5);
      if (detaching)
        detach(t6);
    }
  };
}
function create_else_block(ctx) {
  let div1;
  let div0;
  let charticon;
  let current;
  charticon = new Chart$1({});
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      create_component(charticon.$$.fragment);
      attr(div0, "class", "h-5 dark:text-white opacity-50");
      attr(div1, "class", "h-full min-h-[15rem] flex justify-center items-center");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      mount_component(charticon, div0, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(charticon.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(charticon.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(charticon);
    }
  };
}
function create_if_block_1(ctx) {
  let chart;
  let current;
  chart = new Chart({
    props: {
      value: ctx[11],
      colors: ctx[8]
    }
  });
  return {
    c() {
      create_component(chart.$$.fragment);
    },
    m(target, anchor) {
      mount_component(chart, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const chart_changes = {};
      if (dirty & 2048)
        chart_changes.value = ctx2[11];
      if (dirty & 256)
        chart_changes.colors = ctx2[8];
      chart.$set(chart_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(chart.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(chart.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(chart, detaching);
    }
  };
}
function create_default_slot(ctx) {
  let blocklabel;
  let t0;
  let statustracker;
  let t1;
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  blocklabel = new BlockLabel({
    props: {
      show_label: ctx[7],
      Icon: Chart$1,
      label: ctx[6] || "TimeSeries"
    }
  });
  const statustracker_spread_levels = [ctx[9]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  const if_block_creators = [create_if_block, create_if_block_2, create_if_block_3];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[5] === "static")
      return 0;
    if (ctx2[10])
      return 1;
    if (ctx2[0] === void 0 || ctx2[0] === null)
      return 2;
    return -1;
  }
  if (~(current_block_type_index = select_block_type(ctx))) {
    if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  }
  return {
    c() {
      create_component(blocklabel.$$.fragment);
      t0 = space();
      create_component(statustracker.$$.fragment);
      t1 = space();
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(blocklabel, target, anchor);
      insert(target, t0, anchor);
      mount_component(statustracker, target, anchor);
      insert(target, t1, anchor);
      if (~current_block_type_index) {
        if_blocks[current_block_type_index].m(target, anchor);
      }
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const blocklabel_changes = {};
      if (dirty & 128)
        blocklabel_changes.show_label = ctx2[7];
      if (dirty & 64)
        blocklabel_changes.label = ctx2[6] || "TimeSeries";
      blocklabel.$set(blocklabel_changes);
      const statustracker_changes = dirty & 512 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[9])]) : {};
      statustracker.$set(statustracker_changes);
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type(ctx2);
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
      transition_in(blocklabel.$$.fragment, local);
      transition_in(statustracker.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(blocklabel.$$.fragment, local);
      transition_out(statustracker.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(blocklabel, detaching);
      if (detaching)
        detach(t0);
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t1);
      if (~current_block_type_index) {
        if_blocks[current_block_type_index].d(detaching);
      }
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
      visible: ctx[2],
      variant: ctx[5] === "dynamic" && !ctx[10] ? "dashed" : "solid",
      color: "grey",
      padding: false,
      elem_id: ctx[1],
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
      if (dirty & 1056)
        block_changes.variant = ctx2[5] === "dynamic" && !ctx2[10] ? "dashed" : "solid";
      if (dirty & 2)
        block_changes.elem_id = ctx2[1];
      if (dirty & 1056761) {
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
function format_value(val) {
  return val.data.map((r) => r.reduce((acc, next, i) => __spreadProps(__spreadValues({}, acc), { [val.headers[i]]: next }), {}));
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
function make_dict(x, y) {
  const headers = [];
  const data = [];
  headers.push(x.name);
  y.forEach(({ name }) => headers.push(name));
  for (let i = 0; i < x.values.length; i++) {
    let _data = [];
    _data.push(x.values[i]);
    y.forEach(({ values }) => _data.push(values[i].y));
    data.push(_data);
  }
  return { headers, data };
}
function instance($$self, $$props, $$invalidate) {
  let static_data;
  let $_;
  component_subscribe($$self, X, ($$value) => $$invalidate(12, $_ = $$value));
  const dispatch = createEventDispatcher();
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value } = $$props;
  let { y } = $$props;
  let { x } = $$props;
  let { mode } = $$props;
  let { label } = $$props;
  let { show_label } = $$props;
  let { colors: colors2 } = $$props;
  let { loading_status } = $$props;
  let _value;
  function blob_to_string(blob) {
    const reader = new FileReader();
    reader.addEventListener("loadend", (e) => {
      $$invalidate(10, _value = e.srcElement.result);
    });
    reader.readAsText(blob);
  }
  function dict_to_string(dict) {
    if (dict.headers)
      $$invalidate(10, _value = dict.headers.join(","));
    const data = dict.data;
    data.forEach((x2) => {
      $$invalidate(10, _value = _value + "\n");
      $$invalidate(10, _value = _value + x2.join(","));
    });
  }
  function handle_load(v) {
    $$invalidate(0, value = { data: v });
    return v;
  }
  function handle_clear({ detail }) {
    $$invalidate(0, value = null);
    dispatch("change");
    dispatch("clear");
  }
  const process_handler = ({ detail: { x: x2, y: y2 } }) => $$invalidate(0, value = make_dict(x2, y2));
  const load_handler = ({ detail }) => handle_load(detail);
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(1, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(2, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("y" in $$props2)
      $$invalidate(3, y = $$props2.y);
    if ("x" in $$props2)
      $$invalidate(4, x = $$props2.x);
    if ("mode" in $$props2)
      $$invalidate(5, mode = $$props2.mode);
    if ("label" in $$props2)
      $$invalidate(6, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(7, show_label = $$props2.show_label);
    if ("colors" in $$props2)
      $$invalidate(8, colors2 = $$props2.colors);
    if ("loading_status" in $$props2)
      $$invalidate(9, loading_status = $$props2.loading_status);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      {
        if (value && value.data && typeof value.data === "string") {
          if (!value)
            $$invalidate(10, _value = null);
          else
            blob_to_string(data_uri_to_blob(value.data));
        } else if (value && value.data && typeof value.data != "string") {
          if (!value)
            $$invalidate(10, _value = null);
          dict_to_string(value);
        }
      }
    }
    if ($$self.$$.dirty & 1025) {
      $$invalidate(10, _value = value == null ? null : _value);
    }
    if ($$self.$$.dirty & 33) {
      $$invalidate(11, static_data = mode === "static" && value && format_value(value));
    }
    if ($$self.$$.dirty & 1) {
      dispatch("change");
    }
  };
  return [
    value,
    elem_id,
    visible,
    y,
    x,
    mode,
    label,
    show_label,
    colors2,
    loading_status,
    _value,
    static_data,
    $_,
    handle_load,
    handle_clear,
    process_handler,
    load_handler
  ];
}
class TimeSeries extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 1,
      visible: 2,
      value: 0,
      y: 3,
      x: 4,
      mode: 5,
      label: 6,
      show_label: 7,
      colors: 8,
      loading_status: 9
    });
  }
}
var TimeSeries$1 = TimeSeries;
const modes = ["static", "dynamic"];
const document$1 = (config) => ({
  type: "{data: Array<Array<number>> | string; headers?: Array<string>;}",
  description: "dataset of series"
});
export { TimeSeries$1 as Component, document$1 as document, modes };
//# sourceMappingURL=index37.js.map
