import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, c as create_component, b as attr, f as insert, g as append, m as mount_component, l as listen, j as transition_in, k as transition_out, n as detach, o as destroy_component, K as bubble, w as svg_element, Y as set_style, x as noop, a as space, d as toggle_class, D as group_outros, E as check_outros, F as createEventDispatcher } from "./main.js";
function create_fragment$3(ctx) {
  let button;
  let div;
  let icon;
  let current;
  let mounted;
  let dispose;
  icon = new ctx[0]({});
  return {
    c() {
      button = element("button");
      div = element("div");
      create_component(icon.$$.fragment);
      attr(div, "class", "m-t-1 w-[60%] h-[60%] opacity-80 dark:text-white");
      attr(button, "class", "text-gray-500 bg-white/90 h-5 w-5 flex items-center justify-center rounded shadow-sm hover:shadow-xl hover:ring-1 ring-inset ring-gray-200 z-10 dark:bg-gray-900 dark:ring-gray-600");
      attr(button, "aria-label", ctx[1]);
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, div);
      mount_component(icon, div, null);
      current = true;
      if (!mounted) {
        dispose = listen(button, "click", ctx[2]);
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (!current || dirty & 2) {
        attr(button, "aria-label", ctx2[1]);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(icon.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(icon.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(button);
      destroy_component(icon);
      mounted = false;
      dispose();
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { Icon } = $$props;
  let { label = "" } = $$props;
  function click_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("Icon" in $$props2)
      $$invalidate(0, Icon = $$props2.Icon);
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
  };
  return [Icon, label, click_handler];
}
class IconButton extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$3, safe_not_equal, { Icon: 0, label: 1 });
  }
}
function create_fragment$2(ctx) {
  let svg;
  let g;
  let path0;
  let path1;
  return {
    c() {
      svg = svg_element("svg");
      g = svg_element("g");
      path0 = svg_element("path");
      path1 = svg_element("path");
      attr(path0, "d", "M18,6L6.087,17.913");
      set_style(path0, "fill", "none");
      set_style(path0, "fill-rule", "nonzero");
      set_style(path0, "stroke-width", "2px");
      attr(g, "transform", "matrix(1.14096,-0.140958,-0.140958,1.14096,-0.0559523,0.0559523)");
      attr(path1, "d", "M4.364,4.364L19.636,19.636");
      set_style(path1, "fill", "none");
      set_style(path1, "fill-rule", "nonzero");
      set_style(path1, "stroke-width", "2px");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "viewBox", "0 0 24 24");
      attr(svg, "version", "1.1");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "xmlns:xlink", "http://www.w3.org/1999/xlink");
      attr(svg, "xml:space", "preserve");
      attr(svg, "stroke", "currentColor");
      set_style(svg, "fill-rule", "evenodd");
      set_style(svg, "clip-rule", "evenodd");
      set_style(svg, "stroke-linecap", "round");
      set_style(svg, "stroke-linejoin", "round");
    },
    m(target, anchor) {
      insert(target, svg, anchor);
      append(svg, g);
      append(g, path0);
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
class Clear extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$2, safe_not_equal, {});
  }
}
function create_fragment$1(ctx) {
  let svg;
  let path;
  return {
    c() {
      svg = svg_element("svg");
      path = svg_element("path");
      attr(path, "d", "M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "viewBox", "0 0 24 24");
      attr(svg, "fill", "none");
      attr(svg, "stroke", "currentColor");
      attr(svg, "stroke-width", "1.5");
      attr(svg, "stroke-linecap", "round");
      attr(svg, "stroke-linejoin", "round");
      attr(svg, "class", "feather feather-edit-2");
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
class Edit extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$1, safe_not_equal, {});
  }
}
function create_if_block(ctx) {
  let iconbutton;
  let current;
  iconbutton = new IconButton({ props: { Icon: Edit, label: "Edit" } });
  iconbutton.$on("click", ctx[3]);
  return {
    c() {
      create_component(iconbutton.$$.fragment);
    },
    m(target, anchor) {
      mount_component(iconbutton, target, anchor);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(iconbutton.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(iconbutton.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(iconbutton, detaching);
    }
  };
}
function create_fragment(ctx) {
  let div;
  let t;
  let iconbutton;
  let current;
  let if_block = ctx[0] && create_if_block(ctx);
  iconbutton = new IconButton({ props: { Icon: Clear, label: "Clear" } });
  iconbutton.$on("click", ctx[4]);
  return {
    c() {
      div = element("div");
      if (if_block)
        if_block.c();
      t = space();
      create_component(iconbutton.$$.fragment);
      attr(div, "class", "modify-upload z-10 top-2 right-2 justify-end flex gap-1");
      toggle_class(div, "absolute", ctx[1]);
      toggle_class(div, "m-1", !ctx[1]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if (if_block)
        if_block.m(div, null);
      append(div, t);
      mount_component(iconbutton, div, null);
      current = true;
    },
    p(ctx2, [dirty]) {
      if (ctx2[0]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty & 1) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block(ctx2);
          if_block.c();
          transition_in(if_block, 1);
          if_block.m(div, t);
        }
      } else if (if_block) {
        group_outros();
        transition_out(if_block, 1, 1, () => {
          if_block = null;
        });
        check_outros();
      }
      if (dirty & 2) {
        toggle_class(div, "absolute", ctx2[1]);
      }
      if (dirty & 2) {
        toggle_class(div, "m-1", !ctx2[1]);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block);
      transition_in(iconbutton.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      transition_out(iconbutton.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      if (if_block)
        if_block.d();
      destroy_component(iconbutton);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { editable = false } = $$props;
  let { absolute = true } = $$props;
  const dispatch = createEventDispatcher();
  const click_handler = () => dispatch("edit");
  const click_handler_1 = (event) => {
    dispatch("clear");
    event.stopPropagation();
  };
  $$self.$$set = ($$props2) => {
    if ("editable" in $$props2)
      $$invalidate(0, editable = $$props2.editable);
    if ("absolute" in $$props2)
      $$invalidate(1, absolute = $$props2.absolute);
  };
  return [editable, absolute, dispatch, click_handler, click_handler_1];
}
class ModifyUpload extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, { editable: 0, absolute: 1 });
  }
}
export { Clear as C, IconButton as I, ModifyUpload as M };
//# sourceMappingURL=ModifyUpload.js.map
