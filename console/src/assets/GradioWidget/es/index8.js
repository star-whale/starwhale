import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, a as space, t as text, b as attr, d as toggle_class, f as insert, g as append, l as listen, h as set_data, x as noop, n as detach, F as createEventDispatcher, P as Block, c as create_component, m as mount_component, j as transition_in, k as transition_out, o as destroy_component, R as assign, T as StatusTracker, I as binding_callbacks, O as bind, U as get_spread_update, V as get_spread_object, L as add_flush_callback, K as bubble } from "./main.js";
function create_fragment$1(ctx) {
  let label_1;
  let input;
  let t0;
  let span;
  let t1;
  let mounted;
  let dispose;
  return {
    c() {
      label_1 = element("label");
      input = element("input");
      t0 = space();
      span = element("span");
      t1 = text(ctx[2]);
      input.disabled = ctx[1];
      input.checked = ctx[0];
      attr(input, "type", "checkbox");
      attr(input, "name", "test");
      attr(input, "class", "gr-check-radio gr-checkbox");
      attr(span, "class", "ml-2");
      attr(label_1, "class", "flex items-center text-gray-700 text-sm space-x-2 rounded-lg cursor-pointer dark:bg-transparent ");
      toggle_class(label_1, "!cursor-not-allowed", ctx[1]);
    },
    m(target, anchor) {
      insert(target, label_1, anchor);
      append(label_1, input);
      append(label_1, t0);
      append(label_1, span);
      append(span, t1);
      if (!mounted) {
        dispose = listen(input, "change", ctx[4]);
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (dirty & 2) {
        input.disabled = ctx2[1];
      }
      if (dirty & 1) {
        input.checked = ctx2[0];
      }
      if (dirty & 4)
        set_data(t1, ctx2[2]);
      if (dirty & 2) {
        toggle_class(label_1, "!cursor-not-allowed", ctx2[1]);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(label_1);
      mounted = false;
      dispose();
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { value } = $$props;
  let { disabled = false } = $$props;
  let { label } = $$props;
  const dispatch = createEventDispatcher();
  function handle_change(evt) {
    $$invalidate(0, value = evt.currentTarget.checked);
    dispatch("change", value);
  }
  const change_handler = (evt) => handle_change(evt);
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("disabled" in $$props2)
      $$invalidate(1, disabled = $$props2.disabled);
    if ("label" in $$props2)
      $$invalidate(2, label = $$props2.label);
  };
  return [value, disabled, label, handle_change, change_handler];
}
class Checkbox extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { value: 0, disabled: 1, label: 2 });
  }
}
function create_default_slot(ctx) {
  let statustracker;
  let t;
  let checkbox;
  let updating_value;
  let current;
  const statustracker_spread_levels = [ctx[6]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  function checkbox_value_binding(value) {
    ctx[7](value);
  }
  let checkbox_props = {
    label: ctx[3],
    disabled: ctx[4] === "static"
  };
  if (ctx[0] !== void 0) {
    checkbox_props.value = ctx[0];
  }
  checkbox = new Checkbox({ props: checkbox_props });
  binding_callbacks.push(() => bind(checkbox, "value", checkbox_value_binding));
  checkbox.$on("change", ctx[8]);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t = space();
      create_component(checkbox.$$.fragment);
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      mount_component(checkbox, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 64 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[6])]) : {};
      statustracker.$set(statustracker_changes);
      const checkbox_changes = {};
      if (dirty & 8)
        checkbox_changes.label = ctx2[3];
      if (dirty & 16)
        checkbox_changes.disabled = ctx2[4] === "static";
      if (!updating_value && dirty & 1) {
        updating_value = true;
        checkbox_changes.value = ctx2[0];
        add_flush_callback(() => updating_value = false);
      }
      checkbox.$set(checkbox_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(checkbox.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(checkbox.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      destroy_component(checkbox, detaching);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      visible: ctx[2],
      elem_id: ctx[1],
      disable: typeof ctx[5].container === "boolean" && !ctx[5].container,
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
      if (dirty & 32)
        block_changes.disable = typeof ctx2[5].container === "boolean" && !ctx2[5].container;
      if (dirty & 601) {
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
  let { value = false } = $$props;
  let { label = "Checkbox" } = $$props;
  let { mode } = $$props;
  let { style = {} } = $$props;
  let { loading_status } = $$props;
  function checkbox_value_binding(value$1) {
    value = value$1;
    $$invalidate(0, value);
  }
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(1, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(2, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("label" in $$props2)
      $$invalidate(3, label = $$props2.label);
    if ("mode" in $$props2)
      $$invalidate(4, mode = $$props2.mode);
    if ("style" in $$props2)
      $$invalidate(5, style = $$props2.style);
    if ("loading_status" in $$props2)
      $$invalidate(6, loading_status = $$props2.loading_status);
  };
  return [
    value,
    elem_id,
    visible,
    label,
    mode,
    style,
    loading_status,
    checkbox_value_binding,
    change_handler
  ];
}
class Checkbox_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 1,
      visible: 2,
      value: 0,
      label: 3,
      mode: 4,
      style: 5,
      loading_status: 6
    });
  }
}
var Checkbox_1$1 = Checkbox_1;
const modes = ["static", "dynamic"];
const document = (config) => {
  var _a;
  return {
    type: "boolean",
    description: "checked status",
    example_data: (_a = config.value) != null ? _a : true
  };
};
export { Checkbox_1$1 as Component, document, modes };
//# sourceMappingURL=index8.js.map
