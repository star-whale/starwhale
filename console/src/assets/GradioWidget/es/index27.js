import { S as SvelteComponent, i as init, s as safe_not_equal, a6 as BlockTitle, e as element, c as create_component, a as space, b as attr, f as insert, m as mount_component, g as append, a7 as set_input_value, l as listen, al as to_number, j as transition_in, k as transition_out, n as detach, o as destroy_component, A as run_all, F as createEventDispatcher, t as text, h as set_data, a9 as tick, P as Block, R as assign, T as StatusTracker, I as binding_callbacks, O as bind, U as get_spread_update, V as get_spread_object, L as add_flush_callback, K as bubble } from "./main.js";
function create_default_slot$1(ctx) {
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
function create_fragment$1(ctx) {
  let label_1;
  let blocktitle;
  let t;
  let input;
  let current;
  let mounted;
  let dispose;
  blocktitle = new BlockTitle({
    props: {
      show_label: ctx[3],
      $$slots: { default: [create_default_slot$1] },
      $$scope: { ctx }
    }
  });
  return {
    c() {
      label_1 = element("label");
      create_component(blocktitle.$$.fragment);
      t = space();
      input = element("input");
      attr(input, "type", "number");
      attr(input, "class", "gr-box gr-input w-full gr-text-input");
      input.disabled = ctx[1];
      attr(label_1, "class", "block");
    },
    m(target, anchor) {
      insert(target, label_1, anchor);
      mount_component(blocktitle, label_1, null);
      append(label_1, t);
      append(label_1, input);
      set_input_value(input, ctx[0]);
      current = true;
      if (!mounted) {
        dispose = [
          listen(input, "input", ctx[6]),
          listen(input, "keypress", ctx[4]),
          listen(input, "blur", ctx[5])
        ];
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 8)
        blocktitle_changes.show_label = ctx2[3];
      if (dirty & 516) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (!current || dirty & 2) {
        input.disabled = ctx2[1];
      }
      if (dirty & 1 && to_number(input.value) !== ctx2[0]) {
        set_input_value(input, ctx2[0]);
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
        detach(label_1);
      destroy_component(blocktitle);
      mounted = false;
      run_all(dispose);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { value = 0 } = $$props;
  let { disabled = false } = $$props;
  let { label } = $$props;
  let { show_label } = $$props;
  const dispatch = createEventDispatcher();
  function handle_change(n) {
    if (!isNaN(n) && n !== null) {
      dispatch("change", n);
    }
  }
  async function handle_keypress(e) {
    await tick();
    if (e.key === "Enter") {
      e.preventDefault();
      dispatch("submit");
    }
  }
  function handle_blur(e) {
    dispatch("blur");
  }
  function input_input_handler() {
    value = to_number(this.value);
    $$invalidate(0, value);
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("disabled" in $$props2)
      $$invalidate(1, disabled = $$props2.disabled);
    if ("label" in $$props2)
      $$invalidate(2, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(3, show_label = $$props2.show_label);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      handle_change(value);
    }
  };
  return [
    value,
    disabled,
    label,
    show_label,
    handle_keypress,
    handle_blur,
    input_input_handler
  ];
}
class Number extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      value: 0,
      disabled: 1,
      label: 2,
      show_label: 3
    });
  }
}
function create_default_slot(ctx) {
  let statustracker;
  let t;
  let number;
  let updating_value;
  let current;
  const statustracker_spread_levels = [ctx[6]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  function number_value_binding(value) {
    ctx[8](value);
  }
  let number_props = {
    label: ctx[1],
    show_label: ctx[5],
    disabled: ctx[7] === "static"
  };
  if (ctx[0] !== void 0) {
    number_props.value = ctx[0];
  }
  number = new Number({ props: number_props });
  binding_callbacks.push(() => bind(number, "value", number_value_binding));
  number.$on("change", ctx[9]);
  number.$on("submit", ctx[10]);
  number.$on("blur", ctx[11]);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t = space();
      create_component(number.$$.fragment);
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      mount_component(number, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 64 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[6])]) : {};
      statustracker.$set(statustracker_changes);
      const number_changes = {};
      if (dirty & 2)
        number_changes.label = ctx2[1];
      if (dirty & 32)
        number_changes.show_label = ctx2[5];
      if (dirty & 128)
        number_changes.disabled = ctx2[7] === "static";
      if (!updating_value && dirty & 1) {
        updating_value = true;
        number_changes.value = ctx2[0];
        add_flush_callback(() => updating_value = false);
      }
      number.$set(number_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(number.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(number.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      destroy_component(number, detaching);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      visible: ctx[3],
      elem_id: ctx[2],
      disable: typeof ctx[4].container === "boolean" && !ctx[4].container,
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
      if (dirty & 8)
        block_changes.visible = ctx2[3];
      if (dirty & 4)
        block_changes.elem_id = ctx2[2];
      if (dirty & 16)
        block_changes.disable = typeof ctx2[4].container === "boolean" && !ctx2[4].container;
      if (dirty & 4323) {
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
  let { label = "Number" } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { style = {} } = $$props;
  let { value = 0 } = $$props;
  let { show_label } = $$props;
  let { loading_status } = $$props;
  let { mode } = $$props;
  function number_value_binding(value$1) {
    value = value$1;
    $$invalidate(0, value);
  }
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  function submit_handler(event) {
    bubble.call(this, $$self, event);
  }
  function blur_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
    if ("elem_id" in $$props2)
      $$invalidate(2, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(3, visible = $$props2.visible);
    if ("style" in $$props2)
      $$invalidate(4, style = $$props2.style);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("show_label" in $$props2)
      $$invalidate(5, show_label = $$props2.show_label);
    if ("loading_status" in $$props2)
      $$invalidate(6, loading_status = $$props2.loading_status);
    if ("mode" in $$props2)
      $$invalidate(7, mode = $$props2.mode);
  };
  return [
    value,
    label,
    elem_id,
    visible,
    style,
    show_label,
    loading_status,
    mode,
    number_value_binding,
    change_handler,
    submit_handler,
    blur_handler
  ];
}
class Number_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      label: 1,
      elem_id: 2,
      visible: 3,
      style: 4,
      value: 0,
      show_label: 5,
      loading_status: 6,
      mode: 7
    });
  }
}
var Number_1$1 = Number_1;
const modes = ["static", "dynamic"];
const document = (config) => {
  var _a;
  return {
    type: "number",
    description: "numeric value",
    example_data: (_a = config.value) != null ? _a : 1
  };
};
export { Number_1$1 as Component, document, modes };
//# sourceMappingURL=index27.js.map
