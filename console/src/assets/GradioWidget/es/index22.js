import { S as SvelteComponent, i as init, s as safe_not_equal, a6 as BlockTitle, e as element, t as text, a as space, b as attr, f as insert, g as append, h as set_data, n as detach, c as create_component, m as mount_component, j as transition_in, k as transition_out, o as destroy_component, C as destroy_each, w as svg_element, d as toggle_class, M as src_url_equal, a4 as afterUpdate, I as binding_callbacks, B as empty, U as get_spread_update, V as get_spread_object, D as group_outros, E as check_outros, R as assign } from "./main.js";
const getSaliencyColor = (value) => {
  var color = null;
  if (value < 0) {
    color = [52, 152, 219];
  } else {
    color = [231, 76, 60];
  }
  return colorToString(interpolate(Math.abs(value), [255, 255, 255], color));
};
const interpolate = (val, rgb1, rgb2) => {
  if (val > 1) {
    val = 1;
  }
  val = Math.sqrt(val);
  var rgb = [0, 0, 0];
  var i;
  for (i = 0; i < 3; i++) {
    rgb[i] = Math.round(rgb1[i] * (1 - val) + rgb2[i] * val);
  }
  return rgb;
};
const colorToString = (rgb) => {
  return "rgb(" + rgb[0] + ", " + rgb[1] + ", " + rgb[2] + ")";
};
const getObjectFitSize = (contains, containerWidth, containerHeight, width, height) => {
  var doRatio = width / height;
  var cRatio = containerWidth / containerHeight;
  var targetWidth = 0;
  var targetHeight = 0;
  var test = contains ? doRatio > cRatio : doRatio < cRatio;
  if (test) {
    targetWidth = containerWidth;
    targetHeight = targetWidth / doRatio;
  } else {
    targetHeight = containerHeight;
    targetWidth = targetHeight * doRatio;
  }
  return {
    width: targetWidth,
    height: targetHeight,
    x: (containerWidth - targetWidth) / 2,
    y: (containerHeight - targetHeight) / 2
  };
};
var Number_svelte_svelte_type_style_lang = "";
function get_each_context$6(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[2] = list[i];
  return child_ctx;
}
function create_default_slot$8(ctx) {
  let t;
  return {
    c() {
      t = text(ctx[1]);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 2)
        set_data(t, ctx2[1]);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_each_block$6(ctx) {
  let div;
  let t0_value = ctx[2][0] + "";
  let t0;
  let t1;
  let div_style_value;
  return {
    c() {
      div = element("div");
      t0 = text(t0_value);
      t1 = space();
      attr(div, "class", "flex-1");
      attr(div, "style", div_style_value = "background-color: " + getSaliencyColor(ctx[2][1]));
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t0);
      append(div, t1);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t0_value !== (t0_value = ctx2[2][0] + ""))
        set_data(t0, t0_value);
      if (dirty & 1 && div_style_value !== (div_style_value = "background-color: " + getSaliencyColor(ctx2[2][1]))) {
        attr(div, "style", div_style_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_fragment$9(ctx) {
  let div1;
  let blocktitle;
  let t;
  let div0;
  let current;
  blocktitle = new BlockTitle({
    props: {
      $$slots: { default: [create_default_slot$8] },
      $$scope: { ctx }
    }
  });
  let each_value = ctx[0];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block$6(get_each_context$6(ctx, each_value, i));
  }
  return {
    c() {
      div1 = element("div");
      create_component(blocktitle.$$.fragment);
      t = space();
      div0 = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div0, "class", "interpret_range flex");
      attr(div1, "class", "input-number");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      mount_component(blocktitle, div1, null);
      append(div1, t);
      append(div1, div0);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div0, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 34) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (dirty & 1) {
        each_value = ctx2[0];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context$6(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block$6(child_ctx);
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
    i(local) {
      if (current)
        return;
      transition_in(blocktitle.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocktitle.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(blocktitle);
      destroy_each(each_blocks, detaching);
    }
  };
}
function instance$9($$self, $$props, $$invalidate) {
  let { interpretation } = $$props;
  let { label = "" } = $$props;
  $$self.$$set = ($$props2) => {
    if ("interpretation" in $$props2)
      $$invalidate(0, interpretation = $$props2.interpretation);
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
  };
  return [interpretation, label];
}
class Number extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$9, create_fragment$9, safe_not_equal, { interpretation: 0, label: 1 });
  }
}
var Dropdown_svelte_svelte_type_style_lang = "";
function get_each_context$5(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[3] = list[i];
  child_ctx[5] = i;
  return child_ctx;
}
function create_default_slot$7(ctx) {
  let t;
  return {
    c() {
      t = text(ctx[2]);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 4)
        set_data(t, ctx2[2]);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_each_block$5(ctx) {
  let li;
  let t0_value = ctx[3] + "";
  let t0;
  let t1;
  let li_style_value;
  return {
    c() {
      li = element("li");
      t0 = text(t0_value);
      t1 = space();
      attr(li, "class", "dropdown-item first:rounded-t transition last:rounded-b py-2 px-3 block whitespace-nowrap cursor-pointer");
      attr(li, "style", li_style_value = "background-color: " + getSaliencyColor(ctx[0][ctx[5]]));
    },
    m(target, anchor) {
      insert(target, li, anchor);
      append(li, t0);
      append(li, t1);
    },
    p(ctx2, dirty) {
      if (dirty & 2 && t0_value !== (t0_value = ctx2[3] + ""))
        set_data(t0, t0_value);
      if (dirty & 1 && li_style_value !== (li_style_value = "background-color: " + getSaliencyColor(ctx2[0][ctx2[5]]))) {
        attr(li, "style", li_style_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(li);
    }
  };
}
function create_fragment$8(ctx) {
  let div;
  let blocktitle;
  let t;
  let ul;
  let current;
  blocktitle = new BlockTitle({
    props: {
      $$slots: { default: [create_default_slot$7] },
      $$scope: { ctx }
    }
  });
  let each_value = ctx[1];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block$5(get_each_context$5(ctx, each_value, i));
  }
  return {
    c() {
      div = element("div");
      create_component(blocktitle.$$.fragment);
      t = space();
      ul = element("ul");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(ul, "class", "dropdown-menu");
      attr(div, "class", "input-dropdown");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(blocktitle, div, null);
      append(div, t);
      append(div, ul);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(ul, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 68) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (dirty & 3) {
        each_value = ctx2[1];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context$5(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block$5(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(ul, null);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(blocktitle.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocktitle.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(blocktitle);
      destroy_each(each_blocks, detaching);
    }
  };
}
function instance$8($$self, $$props, $$invalidate) {
  let { interpretation } = $$props;
  let { choices } = $$props;
  let { label = "" } = $$props;
  $$self.$$set = ($$props2) => {
    if ("interpretation" in $$props2)
      $$invalidate(0, interpretation = $$props2.interpretation);
    if ("choices" in $$props2)
      $$invalidate(1, choices = $$props2.choices);
    if ("label" in $$props2)
      $$invalidate(2, label = $$props2.label);
  };
  return [interpretation, choices, label];
}
class Dropdown extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$8, create_fragment$8, safe_not_equal, { interpretation: 0, choices: 1, label: 2 });
  }
}
var Checkbox_svelte_svelte_type_style_lang = "";
function create_default_slot$6(ctx) {
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
function create_fragment$7(ctx) {
  let div2;
  let blocktitle;
  let t0;
  let button;
  let div0;
  let div0_style_value;
  let t1;
  let div1;
  let svg;
  let line0;
  let line1;
  let div1_style_value;
  let current;
  blocktitle = new BlockTitle({
    props: {
      $$slots: { default: [create_default_slot$6] },
      $$scope: { ctx }
    }
  });
  return {
    c() {
      div2 = element("div");
      create_component(blocktitle.$$.fragment);
      t0 = space();
      button = element("button");
      div0 = element("div");
      t1 = space();
      div1 = element("div");
      svg = svg_element("svg");
      line0 = svg_element("line");
      line1 = svg_element("line");
      attr(div0, "class", "checkbox w-4 h-4 bg-white flex items-center justify-center border border-gray-400 box-border");
      attr(div0, "style", div0_style_value = "background-color: " + getSaliencyColor(ctx[2][0]));
      attr(line0, "x1", "-7.5");
      attr(line0, "y1", "0");
      attr(line0, "x2", "-2.5");
      attr(line0, "y2", "5");
      attr(line0, "stroke", "black");
      attr(line0, "stroke-width", "4");
      attr(line0, "stroke-linecap", "round");
      attr(line1, "x1", "-2.5");
      attr(line1, "y1", "5");
      attr(line1, "x2", "7.5");
      attr(line1, "y2", "-7.5");
      attr(line1, "stroke", "black");
      attr(line1, "stroke-width", "4");
      attr(line1, "stroke-linecap", "round");
      attr(svg, "class", "check h-3 w-4 svelte-r8ethh");
      attr(svg, "viewBox", "-10 -10 20 20");
      attr(div1, "class", "checkbox w-4 h-4 bg-white flex items-center justify-center border border-gray-400 box-border");
      attr(div1, "style", div1_style_value = "background-color: " + getSaliencyColor(ctx[2][1]));
      attr(button, "class", "checkbox-item py-2 px-3 rounded cursor-pointer flex gap-1 svelte-r8ethh");
      toggle_class(button, "selected", ctx[1]);
      attr(div2, "class", "input-checkbox inline-block svelte-r8ethh");
    },
    m(target, anchor) {
      insert(target, div2, anchor);
      mount_component(blocktitle, div2, null);
      append(div2, t0);
      append(div2, button);
      append(button, div0);
      append(button, t1);
      append(button, div1);
      append(div1, svg);
      append(svg, line0);
      append(svg, line1);
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 9) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (!current || dirty & 4 && div0_style_value !== (div0_style_value = "background-color: " + getSaliencyColor(ctx2[2][0]))) {
        attr(div0, "style", div0_style_value);
      }
      if (!current || dirty & 4 && div1_style_value !== (div1_style_value = "background-color: " + getSaliencyColor(ctx2[2][1]))) {
        attr(div1, "style", div1_style_value);
      }
      if (dirty & 2) {
        toggle_class(button, "selected", ctx2[1]);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(blocktitle.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocktitle.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div2);
      destroy_component(blocktitle);
    }
  };
}
function instance$7($$self, $$props, $$invalidate) {
  let { label = "" } = $$props;
  let { original } = $$props;
  let { interpretation } = $$props;
  $$self.$$set = ($$props2) => {
    if ("label" in $$props2)
      $$invalidate(0, label = $$props2.label);
    if ("original" in $$props2)
      $$invalidate(1, original = $$props2.original);
    if ("interpretation" in $$props2)
      $$invalidate(2, interpretation = $$props2.interpretation);
  };
  return [label, original, interpretation];
}
class Checkbox extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$7, create_fragment$7, safe_not_equal, { label: 0, original: 1, interpretation: 2 });
  }
}
var CheckboxGroup_svelte_svelte_type_style_lang = "";
function get_each_context$4(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[4] = list[i];
  child_ctx[6] = i;
  return child_ctx;
}
function create_default_slot$5(ctx) {
  let t;
  return {
    c() {
      t = text(ctx[3]);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 8)
        set_data(t, ctx2[3]);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_each_block$4(ctx) {
  let button;
  let div0;
  let div0_style_value;
  let t0;
  let div1;
  let svg;
  let line0;
  let line1;
  let div1_style_value;
  let t1;
  let t2_value = ctx[4] + "";
  let t2;
  let t3;
  return {
    c() {
      button = element("button");
      div0 = element("div");
      t0 = space();
      div1 = element("div");
      svg = svg_element("svg");
      line0 = svg_element("line");
      line1 = svg_element("line");
      t1 = space();
      t2 = text(t2_value);
      t3 = space();
      attr(div0, "class", "checkbox w-4 h-4 bg-white flex items-center justify-center border border-gray-400 box-border svelte-h5sk3f");
      attr(div0, "style", div0_style_value = "background-color: " + getSaliencyColor(ctx[1][ctx[6]][0]));
      attr(line0, "x1", "-7.5");
      attr(line0, "y1", "0");
      attr(line0, "x2", "-2.5");
      attr(line0, "y2", "5");
      attr(line0, "stroke", "black");
      attr(line0, "stroke-width", "4");
      attr(line0, "stroke-linecap", "round");
      attr(line1, "x1", "-2.5");
      attr(line1, "y1", "5");
      attr(line1, "x2", "7.5");
      attr(line1, "y2", "-7.5");
      attr(line1, "stroke", "black");
      attr(line1, "stroke-width", "4");
      attr(line1, "stroke-linecap", "round");
      attr(svg, "class", "check h-3 w-4 svelte-h5sk3f");
      attr(svg, "viewBox", "-10 -10 20 20");
      attr(div1, "class", "checkbox w-4 h-4 bg-white flex items-center justify-center border border-gray-400 box-border svelte-h5sk3f");
      attr(div1, "style", div1_style_value = "background-color: " + getSaliencyColor(ctx[1][ctx[6]][1]));
      attr(button, "class", "checkbox-item py-2 px-3 font-semibold rounded cursor-pointer flex items-center gap-1 svelte-h5sk3f");
      toggle_class(button, "selected", ctx[0].includes(ctx[4]));
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, div0);
      append(button, t0);
      append(button, div1);
      append(div1, svg);
      append(svg, line0);
      append(svg, line1);
      append(button, t1);
      append(button, t2);
      append(button, t3);
    },
    p(ctx2, dirty) {
      if (dirty & 2 && div0_style_value !== (div0_style_value = "background-color: " + getSaliencyColor(ctx2[1][ctx2[6]][0]))) {
        attr(div0, "style", div0_style_value);
      }
      if (dirty & 2 && div1_style_value !== (div1_style_value = "background-color: " + getSaliencyColor(ctx2[1][ctx2[6]][1]))) {
        attr(div1, "style", div1_style_value);
      }
      if (dirty & 4 && t2_value !== (t2_value = ctx2[4] + ""))
        set_data(t2, t2_value);
      if (dirty & 5) {
        toggle_class(button, "selected", ctx2[0].includes(ctx2[4]));
      }
    },
    d(detaching) {
      if (detaching)
        detach(button);
    }
  };
}
function create_fragment$6(ctx) {
  let div;
  let blocktitle;
  let t;
  let current;
  blocktitle = new BlockTitle({
    props: {
      $$slots: { default: [create_default_slot$5] },
      $$scope: { ctx }
    }
  });
  let each_value = ctx[2];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block$4(get_each_context$4(ctx, each_value, i));
  }
  return {
    c() {
      div = element("div");
      create_component(blocktitle.$$.fragment);
      t = space();
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", "input-checkbox-group flex flex-wrap gap-2 svelte-h5sk3f");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(blocktitle, div, null);
      append(div, t);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 136) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (dirty & 7) {
        each_value = ctx2[2];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context$4(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block$4(child_ctx);
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
    i(local) {
      if (current)
        return;
      transition_in(blocktitle.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocktitle.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(blocktitle);
      destroy_each(each_blocks, detaching);
    }
  };
}
function instance$6($$self, $$props, $$invalidate) {
  let { original } = $$props;
  let { interpretation } = $$props;
  let { choices } = $$props;
  let { label = "" } = $$props;
  $$self.$$set = ($$props2) => {
    if ("original" in $$props2)
      $$invalidate(0, original = $$props2.original);
    if ("interpretation" in $$props2)
      $$invalidate(1, interpretation = $$props2.interpretation);
    if ("choices" in $$props2)
      $$invalidate(2, choices = $$props2.choices);
    if ("label" in $$props2)
      $$invalidate(3, label = $$props2.label);
  };
  return [original, interpretation, choices, label];
}
class CheckboxGroup extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$6, create_fragment$6, safe_not_equal, {
      original: 0,
      interpretation: 1,
      choices: 2,
      label: 3
    });
  }
}
var Slider_svelte_svelte_type_style_lang = "";
function get_each_context$3(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[6] = list[i];
  return child_ctx;
}
function create_default_slot$4(ctx) {
  let t;
  return {
    c() {
      t = text(ctx[5]);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 32)
        set_data(t, ctx2[5]);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_each_block$3(ctx) {
  let div;
  let div_style_value;
  return {
    c() {
      div = element("div");
      attr(div, "class", "flex-1 h-4");
      attr(div, "style", div_style_value = "background-color: " + getSaliencyColor(ctx[6]));
    },
    m(target, anchor) {
      insert(target, div, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 2 && div_style_value !== (div_style_value = "background-color: " + getSaliencyColor(ctx2[6]))) {
        attr(div, "style", div_style_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_fragment$5(ctx) {
  let div2;
  let blocktitle;
  let t0;
  let input;
  let t1;
  let div0;
  let t2;
  let div1;
  let t3;
  let current;
  blocktitle = new BlockTitle({
    props: {
      $$slots: { default: [create_default_slot$4] },
      $$scope: { ctx }
    }
  });
  let each_value = ctx[1];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block$3(get_each_context$3(ctx, each_value, i));
  }
  return {
    c() {
      div2 = element("div");
      create_component(blocktitle.$$.fragment);
      t0 = space();
      input = element("input");
      t1 = space();
      div0 = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      t2 = space();
      div1 = element("div");
      t3 = text(ctx[0]);
      attr(input, "type", "range");
      attr(input, "class", "range w-full appearance-none transition rounded h-4 bg-blue-400 svelte-3aijhr");
      input.disabled = true;
      attr(input, "min", ctx[2]);
      attr(input, "max", ctx[3]);
      attr(input, "step", ctx[4]);
      attr(div0, "class", "interpret_range flex");
      attr(div1, "class", "original inline-block mx-auto mt-1 px-2 py-0.5 rounded");
      attr(div2, "class", "input-slider text-center svelte-3aijhr");
    },
    m(target, anchor) {
      insert(target, div2, anchor);
      mount_component(blocktitle, div2, null);
      append(div2, t0);
      append(div2, input);
      append(div2, t1);
      append(div2, div0);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div0, null);
      }
      append(div2, t2);
      append(div2, div1);
      append(div1, t3);
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 544) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (!current || dirty & 4) {
        attr(input, "min", ctx2[2]);
      }
      if (!current || dirty & 8) {
        attr(input, "max", ctx2[3]);
      }
      if (!current || dirty & 16) {
        attr(input, "step", ctx2[4]);
      }
      if (dirty & 2) {
        each_value = ctx2[1];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context$3(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block$3(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(div0, null);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
      if (!current || dirty & 1)
        set_data(t3, ctx2[0]);
    },
    i(local) {
      if (current)
        return;
      transition_in(blocktitle.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocktitle.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div2);
      destroy_component(blocktitle);
      destroy_each(each_blocks, detaching);
    }
  };
}
function instance$5($$self, $$props, $$invalidate) {
  let { original } = $$props;
  let { interpretation } = $$props;
  let { minimum } = $$props;
  let { maximum } = $$props;
  let { step } = $$props;
  let { label = "" } = $$props;
  $$self.$$set = ($$props2) => {
    if ("original" in $$props2)
      $$invalidate(0, original = $$props2.original);
    if ("interpretation" in $$props2)
      $$invalidate(1, interpretation = $$props2.interpretation);
    if ("minimum" in $$props2)
      $$invalidate(2, minimum = $$props2.minimum);
    if ("maximum" in $$props2)
      $$invalidate(3, maximum = $$props2.maximum);
    if ("step" in $$props2)
      $$invalidate(4, step = $$props2.step);
    if ("label" in $$props2)
      $$invalidate(5, label = $$props2.label);
  };
  return [original, interpretation, minimum, maximum, step, label];
}
class Slider extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$5, create_fragment$5, safe_not_equal, {
      original: 0,
      interpretation: 1,
      minimum: 2,
      maximum: 3,
      step: 4,
      label: 5
    });
  }
}
var Radio_svelte_svelte_type_style_lang = "";
function get_each_context$2(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[4] = list[i];
  child_ctx[6] = i;
  return child_ctx;
}
function create_default_slot$3(ctx) {
  let t;
  return {
    c() {
      t = text(ctx[3]);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 8)
        set_data(t, ctx2[3]);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_each_block$2(ctx) {
  let button;
  let div;
  let div_style_value;
  let t0;
  let t1_value = ctx[4] + "";
  let t1;
  let t2;
  return {
    c() {
      button = element("button");
      div = element("div");
      t0 = space();
      t1 = text(t1_value);
      t2 = space();
      attr(div, "class", "radio-circle w-4 h-4 rounded-full box-border svelte-145r163");
      attr(div, "style", div_style_value = "background-color: " + getSaliencyColor(ctx[1][ctx[6]]));
      attr(button, "class", "radio-item py-2 px-3 font-semibold rounded cursor-pointer flex items-center gap-2 svelte-145r163");
      toggle_class(button, "selected", ctx[0] === ctx[4]);
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, div);
      append(button, t0);
      append(button, t1);
      append(button, t2);
    },
    p(ctx2, dirty) {
      if (dirty & 2 && div_style_value !== (div_style_value = "background-color: " + getSaliencyColor(ctx2[1][ctx2[6]]))) {
        attr(div, "style", div_style_value);
      }
      if (dirty & 4 && t1_value !== (t1_value = ctx2[4] + ""))
        set_data(t1, t1_value);
      if (dirty & 5) {
        toggle_class(button, "selected", ctx2[0] === ctx2[4]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(button);
    }
  };
}
function create_fragment$4(ctx) {
  let div;
  let blocktitle;
  let t;
  let current;
  blocktitle = new BlockTitle({
    props: {
      $$slots: { default: [create_default_slot$3] },
      $$scope: { ctx }
    }
  });
  let each_value = ctx[2];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block$2(get_each_context$2(ctx, each_value, i));
  }
  return {
    c() {
      div = element("div");
      create_component(blocktitle.$$.fragment);
      t = space();
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", "input-radio flex flex-wrap gap-2 svelte-145r163");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(blocktitle, div, null);
      append(div, t);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 136) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (dirty & 7) {
        each_value = ctx2[2];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context$2(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block$2(child_ctx);
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
    i(local) {
      if (current)
        return;
      transition_in(blocktitle.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocktitle.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(blocktitle);
      destroy_each(each_blocks, detaching);
    }
  };
}
function instance$4($$self, $$props, $$invalidate) {
  let { original } = $$props;
  let { interpretation } = $$props;
  let { choices } = $$props;
  let { label = "" } = $$props;
  $$self.$$set = ($$props2) => {
    if ("original" in $$props2)
      $$invalidate(0, original = $$props2.original);
    if ("interpretation" in $$props2)
      $$invalidate(1, interpretation = $$props2.interpretation);
    if ("choices" in $$props2)
      $$invalidate(2, choices = $$props2.choices);
    if ("label" in $$props2)
      $$invalidate(3, label = $$props2.label);
  };
  return [original, interpretation, choices, label];
}
class Radio extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$4, create_fragment$4, safe_not_equal, {
      original: 0,
      interpretation: 1,
      choices: 2,
      label: 3
    });
  }
}
function create_default_slot$2(ctx) {
  let t;
  return {
    c() {
      t = text(ctx[1]);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 2)
        set_data(t, ctx2[1]);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_fragment$3(ctx) {
  let div2;
  let blocktitle;
  let t0;
  let div1;
  let div0;
  let canvas;
  let t1;
  let img;
  let img_src_value;
  let current;
  blocktitle = new BlockTitle({
    props: {
      $$slots: { default: [create_default_slot$2] },
      $$scope: { ctx }
    }
  });
  return {
    c() {
      div2 = element("div");
      create_component(blocktitle.$$.fragment);
      t0 = space();
      div1 = element("div");
      div0 = element("div");
      canvas = element("canvas");
      t1 = space();
      img = element("img");
      attr(div0, "class", "interpretation w-full h-full absolute top-0 left-0 flex justify-center items-center opacity-90 hover:opacity-20 transition");
      attr(img, "class", "w-full h-full object-contain");
      if (!src_url_equal(img.src, img_src_value = ctx[0]))
        attr(img, "src", img_src_value);
      attr(div1, "class", "image-preview w-full h-60 flex justify-center items-center bg-gray-200 dark:bg-gray-600 relative");
      attr(div2, "class", "input-image");
    },
    m(target, anchor) {
      insert(target, div2, anchor);
      mount_component(blocktitle, div2, null);
      append(div2, t0);
      append(div2, div1);
      append(div1, div0);
      append(div0, canvas);
      ctx[6](canvas);
      append(div1, t1);
      append(div1, img);
      ctx[7](img);
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 514) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (!current || dirty & 1 && !src_url_equal(img.src, img_src_value = ctx2[0])) {
        attr(img, "src", img_src_value);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(blocktitle.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocktitle.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div2);
      destroy_component(blocktitle);
      ctx[6](null);
      ctx[7](null);
    }
  };
}
function instance$3($$self, $$props, $$invalidate) {
  let { original } = $$props;
  let { interpretation } = $$props;
  let { shape } = $$props;
  let { label = "" } = $$props;
  let saliency_layer;
  let image;
  const paintSaliency = (data, ctx, width, height) => {
    var cell_width = width / data[0].length;
    var cell_height = height / data.length;
    var r = 0;
    data.forEach(function(row) {
      var c = 0;
      row.forEach(function(cell) {
        ctx.fillStyle = getSaliencyColor(cell);
        ctx.fillRect(c * cell_width, r * cell_height, cell_width, cell_height);
        c++;
      });
      r++;
    });
  };
  afterUpdate(() => {
    let size = getObjectFitSize(true, image.width, image.height, image.naturalWidth, image.naturalHeight);
    if (shape) {
      size = getObjectFitSize(true, size.width, size.height, shape[0], shape[1]);
    }
    let width = size.width;
    let height = size.height;
    saliency_layer.setAttribute("height", `${height}`);
    saliency_layer.setAttribute("width", `${width}`);
    paintSaliency(interpretation, saliency_layer.getContext("2d"), width, height);
  });
  function canvas_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      saliency_layer = $$value;
      $$invalidate(2, saliency_layer);
    });
  }
  function img_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      image = $$value;
      $$invalidate(3, image);
    });
  }
  $$self.$$set = ($$props2) => {
    if ("original" in $$props2)
      $$invalidate(0, original = $$props2.original);
    if ("interpretation" in $$props2)
      $$invalidate(4, interpretation = $$props2.interpretation);
    if ("shape" in $$props2)
      $$invalidate(5, shape = $$props2.shape);
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
  };
  return [
    original,
    label,
    saliency_layer,
    image,
    interpretation,
    shape,
    canvas_binding,
    img_binding
  ];
}
class Image extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$3, create_fragment$3, safe_not_equal, {
      original: 0,
      interpretation: 4,
      shape: 5,
      label: 1
    });
  }
}
function get_each_context$1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[2] = list[i];
  return child_ctx;
}
function create_default_slot$1(ctx) {
  let t;
  return {
    c() {
      t = text(ctx[1]);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 2)
        set_data(t, ctx2[1]);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_each_block$1(ctx) {
  let div;
  let div_style_value;
  return {
    c() {
      div = element("div");
      attr(div, "class", "flex-1 h-4");
      attr(div, "style", div_style_value = "background-color: " + getSaliencyColor(ctx[2]));
    },
    m(target, anchor) {
      insert(target, div, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && div_style_value !== (div_style_value = "background-color: " + getSaliencyColor(ctx2[2]))) {
        attr(div, "style", div_style_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_fragment$2(ctx) {
  let div1;
  let blocktitle;
  let t;
  let div0;
  let current;
  blocktitle = new BlockTitle({
    props: {
      $$slots: { default: [create_default_slot$1] },
      $$scope: { ctx }
    }
  });
  let each_value = ctx[0];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block$1(get_each_context$1(ctx, each_value, i));
  }
  return {
    c() {
      div1 = element("div");
      create_component(blocktitle.$$.fragment);
      t = space();
      div0 = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div0, "class", "interpret_range flex");
      attr(div1, "class", "input-audio");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      mount_component(blocktitle, div1, null);
      append(div1, t);
      append(div1, div0);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div0, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 34) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (dirty & 1) {
        each_value = ctx2[0];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context$1(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block$1(child_ctx);
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
    i(local) {
      if (current)
        return;
      transition_in(blocktitle.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocktitle.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(blocktitle);
      destroy_each(each_blocks, detaching);
    }
  };
}
function instance$2($$self, $$props, $$invalidate) {
  let { interpretation } = $$props;
  let { label = "" } = $$props;
  $$self.$$set = ($$props2) => {
    if ("interpretation" in $$props2)
      $$invalidate(0, interpretation = $$props2.interpretation);
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
  };
  return [interpretation, label];
}
class Audio extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$2, create_fragment$2, safe_not_equal, { interpretation: 0, label: 1 });
  }
}
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[2] = list[i][0];
  child_ctx[3] = list[i][1];
  return child_ctx;
}
function create_default_slot(ctx) {
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
function create_each_block(ctx) {
  let span;
  let t0_value = ctx[2] + "";
  let t0;
  let t1;
  let span_style_value;
  return {
    c() {
      span = element("span");
      t0 = text(t0_value);
      t1 = space();
      attr(span, "class", "textspan p-1 bg-opacity-20 dark:bg-opacity-80");
      attr(span, "style", span_style_value = "background-color: " + getSaliencyColor(ctx[3]));
    },
    m(target, anchor) {
      insert(target, span, anchor);
      append(span, t0);
      append(span, t1);
    },
    p(ctx2, dirty) {
      if (dirty & 2 && t0_value !== (t0_value = ctx2[2] + ""))
        set_data(t0, t0_value);
      if (dirty & 2 && span_style_value !== (span_style_value = "background-color: " + getSaliencyColor(ctx2[3]))) {
        attr(span, "style", span_style_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_fragment$1(ctx) {
  let div;
  let blocktitle;
  let t;
  let current;
  blocktitle = new BlockTitle({
    props: {
      $$slots: { default: [create_default_slot] },
      $$scope: { ctx }
    }
  });
  let each_value = ctx[1];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  return {
    c() {
      div = element("div");
      create_component(blocktitle.$$.fragment);
      t = space();
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", "input-text w-full rounded box-border p-2 break-word");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(blocktitle, div, null);
      append(div, t);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 65) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (dirty & 2) {
        each_value = ctx2[1];
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
    i(local) {
      if (current)
        return;
      transition_in(blocktitle.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocktitle.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(blocktitle);
      destroy_each(each_blocks, detaching);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { label = "" } = $$props;
  let { interpretation } = $$props;
  $$self.$$set = ($$props2) => {
    if ("label" in $$props2)
      $$invalidate(0, label = $$props2.label);
    if ("interpretation" in $$props2)
      $$invalidate(1, interpretation = $$props2.interpretation);
  };
  return [label, interpretation];
}
class Textbox extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { label: 0, interpretation: 1 });
  }
}
const component_map = {
  audio: Audio,
  dropdown: Dropdown,
  checkbox: Checkbox,
  checkboxgroup: CheckboxGroup,
  number: Number,
  slider: Slider,
  radio: Radio,
  image: Image,
  textbox: Textbox
};
function create_if_block(ctx) {
  let switch_instance;
  let switch_instance_anchor;
  let current;
  const switch_instance_spread_levels = [
    ctx[0],
    { original: ctx[1].original },
    {
      interpretation: ctx[1].interpretation
    }
  ];
  var switch_value = ctx[2];
  function switch_props(ctx2) {
    let switch_instance_props = {};
    for (let i = 0; i < switch_instance_spread_levels.length; i += 1) {
      switch_instance_props = assign(switch_instance_props, switch_instance_spread_levels[i]);
    }
    return { props: switch_instance_props };
  }
  if (switch_value) {
    switch_instance = new switch_value(switch_props());
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
      const switch_instance_changes = dirty & 3 ? get_spread_update(switch_instance_spread_levels, [
        dirty & 1 && get_spread_object(ctx2[0]),
        dirty & 2 && { original: ctx2[1].original },
        dirty & 2 && {
          interpretation: ctx2[1].interpretation
        }
      ]) : {};
      if (switch_value !== (switch_value = ctx2[2])) {
        if (switch_instance) {
          group_outros();
          const old_component = switch_instance;
          transition_out(old_component.$$.fragment, 1, 0, () => {
            destroy_component(old_component, 1);
          });
          check_outros();
        }
        if (switch_value) {
          switch_instance = new switch_value(switch_props());
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
function create_fragment(ctx) {
  let if_block_anchor;
  let current;
  let if_block = ctx[1] && create_if_block(ctx);
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
    p(ctx2, [dirty]) {
      if (ctx2[1]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty & 2) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block(ctx2);
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
function instance($$self, $$props, $$invalidate) {
  let _component;
  let { component } = $$props;
  let { component_props } = $$props;
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("component" in $$props2)
      $$invalidate(3, component = $$props2.component);
    if ("component_props" in $$props2)
      $$invalidate(0, component_props = $$props2.component_props);
    if ("value" in $$props2)
      $$invalidate(1, value = $$props2.value);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 8) {
      $$invalidate(2, _component = component_map[component]);
    }
  };
  return [component_props, value, _component, component];
}
class Interpretation extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      component: 3,
      component_props: 0,
      value: 1
    });
  }
}
var Interpretation$1 = Interpretation;
const modes = ["dynamic"];
export { Interpretation$1 as Component, modes };
//# sourceMappingURL=index22.js.map
