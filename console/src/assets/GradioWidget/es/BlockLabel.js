import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, c as create_component, a as space, t as text, b as attr, d as toggle_class, f as insert, g as append, m as mount_component, h as set_data, j as transition_in, k as transition_out, n as detach, o as destroy_component, Z as get_styles } from "./main.js";
function create_fragment(ctx) {
  let div;
  let span;
  let icon;
  let t0;
  let t1;
  let div_class_value;
  let current;
  icon = new ctx[1]({});
  return {
    c() {
      div = element("div");
      span = element("span");
      create_component(icon.$$.fragment);
      t0 = space();
      t1 = text(ctx[0]);
      attr(span, "class", "mr-2 h-[12px] w-[12px] opacity-80");
      attr(div, "class", div_class_value = "absolute left-0 top-0 py-1 px-2 rounded-br-lg shadow-sm text-xs text-gray-500 flex items-center pointer-events-none bg-white z-20 border-b border-r border-gray-100 dark:bg-gray-900 " + ctx[3]);
      toggle_class(div, "h-0", !ctx[2]);
      toggle_class(div, "sr-only", !ctx[2]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, span);
      mount_component(icon, span, null);
      append(div, t0);
      append(div, t1);
      current = true;
    },
    p(ctx2, [dirty]) {
      if (!current || dirty & 1)
        set_data(t1, ctx2[0]);
      if (!current || dirty & 8 && div_class_value !== (div_class_value = "absolute left-0 top-0 py-1 px-2 rounded-br-lg shadow-sm text-xs text-gray-500 flex items-center pointer-events-none bg-white z-20 border-b border-r border-gray-100 dark:bg-gray-900 " + ctx2[3])) {
        attr(div, "class", div_class_value);
      }
      if (dirty & 12) {
        toggle_class(div, "h-0", !ctx2[2]);
      }
      if (dirty & 12) {
        toggle_class(div, "sr-only", !ctx2[2]);
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
        detach(div);
      destroy_component(icon);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let classes;
  let { label = null } = $$props;
  let { Icon } = $$props;
  let { show_label = true } = $$props;
  let { disable = false } = $$props;
  $$self.$$set = ($$props2) => {
    if ("label" in $$props2)
      $$invalidate(0, label = $$props2.label);
    if ("Icon" in $$props2)
      $$invalidate(1, Icon = $$props2.Icon);
    if ("show_label" in $$props2)
      $$invalidate(2, show_label = $$props2.show_label);
    if ("disable" in $$props2)
      $$invalidate(4, disable = $$props2.disable);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 16) {
      $$invalidate(3, { classes } = get_styles({ label_container: !disable }, ["label_container"]), classes);
    }
  };
  return [label, Icon, show_label, classes, disable];
}
class BlockLabel extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      label: 0,
      Icon: 1,
      show_label: 2,
      disable: 4
    });
  }
}
export { BlockLabel as B };
//# sourceMappingURL=BlockLabel.js.map
