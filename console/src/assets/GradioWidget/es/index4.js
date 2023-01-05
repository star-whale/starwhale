import { S as SvelteComponent, i as init, s as safe_not_equal, p as create_slot, e as element, b as attr, d as toggle_class, f as insert, l as listen, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, j as transition_in, k as transition_out, n as detach, Z as get_styles, K as bubble, c as create_component, m as mount_component, o as destroy_component, Q as component_subscribe, X, t as text, h as set_data } from "./main.js";
function create_fragment$1(ctx) {
  let button;
  let button_class_value;
  let current;
  let mounted;
  let dispose;
  const default_slot_template = ctx[7].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[6], null);
  return {
    c() {
      button = element("button");
      if (default_slot)
        default_slot.c();
      attr(button, "class", button_class_value = "gr-button gr-button-" + ctx[3] + " gr-button-" + ctx[2] + " " + ctx[4]);
      attr(button, "id", ctx[0]);
      toggle_class(button, "!hidden", !ctx[1]);
    },
    m(target, anchor) {
      insert(target, button, anchor);
      if (default_slot) {
        default_slot.m(button, null);
      }
      current = true;
      if (!mounted) {
        dispose = listen(button, "click", ctx[8]);
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 64)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[6], !current ? get_all_dirty_from_scope(ctx2[6]) : get_slot_changes(default_slot_template, ctx2[6], dirty, null), null);
        }
      }
      if (!current || dirty & 28 && button_class_value !== (button_class_value = "gr-button gr-button-" + ctx2[3] + " gr-button-" + ctx2[2] + " " + ctx2[4])) {
        attr(button, "class", button_class_value);
      }
      if (!current || dirty & 1) {
        attr(button, "id", ctx2[0]);
      }
      if (dirty & 30) {
        toggle_class(button, "!hidden", !ctx2[1]);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(default_slot, local);
      current = true;
    },
    o(local) {
      transition_out(default_slot, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(button);
      if (default_slot)
        default_slot.d(detaching);
      mounted = false;
      dispose();
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let classes;
  let { $$slots: slots = {}, $$scope } = $$props;
  let { style = {} } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { variant = "secondary" } = $$props;
  let { size = "sm" } = $$props;
  function click_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("style" in $$props2)
      $$invalidate(5, style = $$props2.style);
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(1, visible = $$props2.visible);
    if ("variant" in $$props2)
      $$invalidate(2, variant = $$props2.variant);
    if ("size" in $$props2)
      $$invalidate(3, size = $$props2.size);
    if ("$$scope" in $$props2)
      $$invalidate(6, $$scope = $$props2.$$scope);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 32) {
      $$invalidate(4, { classes } = get_styles(style, ["full_width"]), classes);
    }
  };
  return [elem_id, visible, variant, size, classes, style, $$scope, slots, click_handler];
}
class Button extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      style: 5,
      elem_id: 0,
      visible: 1,
      variant: 2,
      size: 3
    });
  }
}
function create_default_slot(ctx) {
  let t_value = ctx[5](ctx[3]) + "";
  let t;
  return {
    c() {
      t = text(t_value);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 40 && t_value !== (t_value = ctx2[5](ctx2[3]) + ""))
        set_data(t, t_value);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_fragment(ctx) {
  let button;
  let current;
  button = new Button({
    props: {
      variant: ctx[4],
      elem_id: ctx[1],
      style: ctx[0],
      visible: ctx[2],
      $$slots: { default: [create_default_slot] },
      $$scope: { ctx }
    }
  });
  button.$on("click", ctx[6]);
  return {
    c() {
      create_component(button.$$.fragment);
    },
    m(target, anchor) {
      mount_component(button, target, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const button_changes = {};
      if (dirty & 16)
        button_changes.variant = ctx2[4];
      if (dirty & 2)
        button_changes.elem_id = ctx2[1];
      if (dirty & 1)
        button_changes.style = ctx2[0];
      if (dirty & 4)
        button_changes.visible = ctx2[2];
      if (dirty & 168) {
        button_changes.$$scope = { dirty, ctx: ctx2 };
      }
      button.$set(button_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(button.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(button.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(button, detaching);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let $_;
  component_subscribe($$self, X, ($$value) => $$invalidate(5, $_ = $$value));
  let { style = {} } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value } = $$props;
  let { variant = "primary" } = $$props;
  function click_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("style" in $$props2)
      $$invalidate(0, style = $$props2.style);
    if ("elem_id" in $$props2)
      $$invalidate(1, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(2, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(3, value = $$props2.value);
    if ("variant" in $$props2)
      $$invalidate(4, variant = $$props2.variant);
  };
  return [style, elem_id, visible, value, variant, $_, click_handler];
}
class Button_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      style: 0,
      elem_id: 1,
      visible: 2,
      value: 3,
      variant: 4
    });
  }
}
var Button_1$1 = Button_1;
const modes = ["static"];
const document = (config) => ({
  type: "string",
  description: "button label",
  example_data: config.value || "Run"
});
export { Button_1$1 as Component, document, modes };
//# sourceMappingURL=index4.js.map
