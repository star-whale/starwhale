import { S as SvelteComponent, i as init, s as safe_not_equal, a6 as BlockTitle, e as element, c as create_component, a as space, b as attr, f as insert, g as append, m as mount_component, a7 as set_input_value, l as listen, al as to_number, j as transition_in, k as transition_out, n as detach, o as destroy_component, A as run_all, F as createEventDispatcher, t as text, h as set_data, P as Block, R as assign, T as StatusTracker, I as binding_callbacks, O as bind, U as get_spread_update, V as get_spread_object, L as add_flush_callback, K as bubble } from "./main.js";
function create_default_slot$1(ctx) {
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
function create_fragment$1(ctx) {
  let div1;
  let div0;
  let label_1;
  let blocktitle;
  let t0;
  let input0;
  let t1;
  let input1;
  let current;
  let mounted;
  let dispose;
  blocktitle = new BlockTitle({
    props: {
      show_label: ctx[6],
      $$slots: { default: [create_default_slot$1] },
      $$scope: { ctx }
    }
  });
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      label_1 = element("label");
      create_component(blocktitle.$$.fragment);
      t0 = space();
      input0 = element("input");
      t1 = space();
      input1 = element("input");
      attr(label_1, "for", ctx[7]);
      attr(input0, "type", "number");
      attr(input0, "class", "gr-box gr-input gr-text-input text-center h-6");
      attr(input0, "min", ctx[1]);
      attr(input0, "max", ctx[2]);
      attr(input0, "step", ctx[3]);
      input0.disabled = ctx[4];
      attr(div0, "class", "flex justify-between");
      attr(div1, "class", "w-full flex flex-col ");
      attr(input1, "type", "range");
      attr(input1, "id", ctx[7]);
      attr(input1, "name", "cowbell");
      attr(input1, "class", "w-full disabled:cursor-not-allowed");
      attr(input1, "min", ctx[1]);
      attr(input1, "max", ctx[2]);
      attr(input1, "step", ctx[3]);
      input1.disabled = ctx[4];
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      append(div0, label_1);
      mount_component(blocktitle, label_1, null);
      append(div0, t0);
      append(div0, input0);
      set_input_value(input0, ctx[0]);
      insert(target, t1, anchor);
      insert(target, input1, anchor);
      set_input_value(input1, ctx[0]);
      current = true;
      if (!mounted) {
        dispose = [
          listen(input0, "input", ctx[10]),
          listen(input0, "blur", ctx[8]),
          listen(input1, "change", ctx[11]),
          listen(input1, "input", ctx[11])
        ];
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 64)
        blocktitle_changes.show_label = ctx2[6];
      if (dirty & 8224) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (!current || dirty & 2) {
        attr(input0, "min", ctx2[1]);
      }
      if (!current || dirty & 4) {
        attr(input0, "max", ctx2[2]);
      }
      if (!current || dirty & 8) {
        attr(input0, "step", ctx2[3]);
      }
      if (!current || dirty & 16) {
        input0.disabled = ctx2[4];
      }
      if (dirty & 1 && to_number(input0.value) !== ctx2[0]) {
        set_input_value(input0, ctx2[0]);
      }
      if (!current || dirty & 2) {
        attr(input1, "min", ctx2[1]);
      }
      if (!current || dirty & 4) {
        attr(input1, "max", ctx2[2]);
      }
      if (!current || dirty & 8) {
        attr(input1, "step", ctx2[3]);
      }
      if (!current || dirty & 16) {
        input1.disabled = ctx2[4];
      }
      if (dirty & 1) {
        set_input_value(input1, ctx2[0]);
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
      if (detaching)
        detach(t1);
      if (detaching)
        detach(input1);
      mounted = false;
      run_all(dispose);
    }
  };
}
let _id = 0;
function instance$1($$self, $$props, $$invalidate) {
  let { value = 0 } = $$props;
  let { style = {} } = $$props;
  let { minimum = 0 } = $$props;
  let { maximum = 100 } = $$props;
  let { step = 1 } = $$props;
  let { disabled = false } = $$props;
  let { label } = $$props;
  let { show_label } = $$props;
  const id = `range_id_${_id++}`;
  const dispatch = createEventDispatcher();
  const clamp = () => $$invalidate(0, value = Math.min(Math.max(value, minimum), maximum));
  function input0_input_handler() {
    value = to_number(this.value);
    $$invalidate(0, value);
  }
  function input1_change_input_handler() {
    value = to_number(this.value);
    $$invalidate(0, value);
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("style" in $$props2)
      $$invalidate(9, style = $$props2.style);
    if ("minimum" in $$props2)
      $$invalidate(1, minimum = $$props2.minimum);
    if ("maximum" in $$props2)
      $$invalidate(2, maximum = $$props2.maximum);
    if ("step" in $$props2)
      $$invalidate(3, step = $$props2.step);
    if ("disabled" in $$props2)
      $$invalidate(4, disabled = $$props2.disabled);
    if ("label" in $$props2)
      $$invalidate(5, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(6, show_label = $$props2.show_label);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      dispatch("change", value);
    }
  };
  return [
    value,
    minimum,
    maximum,
    step,
    disabled,
    label,
    show_label,
    id,
    clamp,
    style,
    input0_input_handler,
    input1_change_input_handler
  ];
}
class Range extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      value: 0,
      style: 9,
      minimum: 1,
      maximum: 2,
      step: 3,
      disabled: 4,
      label: 5,
      show_label: 6
    });
  }
}
function create_default_slot(ctx) {
  let statustracker;
  let t;
  let range;
  let updating_value;
  let current;
  const statustracker_spread_levels = [ctx[10]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  function range_value_binding(value) {
    ctx[11](value);
  }
  let range_props = {
    label: ctx[3],
    show_label: ctx[9],
    minimum: ctx[5],
    maximum: ctx[6],
    step: ctx[7],
    style: ctx[4],
    disabled: ctx[8] === "static"
  };
  if (ctx[0] !== void 0) {
    range_props.value = ctx[0];
  }
  range = new Range({ props: range_props });
  binding_callbacks.push(() => bind(range, "value", range_value_binding));
  range.$on("change", ctx[12]);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t = space();
      create_component(range.$$.fragment);
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      mount_component(range, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 1024 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[10])]) : {};
      statustracker.$set(statustracker_changes);
      const range_changes = {};
      if (dirty & 8)
        range_changes.label = ctx2[3];
      if (dirty & 512)
        range_changes.show_label = ctx2[9];
      if (dirty & 32)
        range_changes.minimum = ctx2[5];
      if (dirty & 64)
        range_changes.maximum = ctx2[6];
      if (dirty & 128)
        range_changes.step = ctx2[7];
      if (dirty & 16)
        range_changes.style = ctx2[4];
      if (dirty & 256)
        range_changes.disabled = ctx2[8] === "static";
      if (!updating_value && dirty & 1) {
        updating_value = true;
        range_changes.value = ctx2[0];
        add_flush_callback(() => updating_value = false);
      }
      range.$set(range_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(range.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(range.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      destroy_component(range, detaching);
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
      if (dirty & 4)
        block_changes.visible = ctx2[2];
      if (dirty & 2)
        block_changes.elem_id = ctx2[1];
      if (dirty & 16)
        block_changes.disable = typeof ctx2[4].container === "boolean" && !ctx2[4].container;
      if (dirty & 10233) {
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
  let { value = 0 } = $$props;
  let { label = "Slider" } = $$props;
  let { style = {} } = $$props;
  let { minimum } = $$props;
  let { maximum } = $$props;
  let { step } = $$props;
  let { mode } = $$props;
  let { show_label } = $$props;
  let { loading_status } = $$props;
  function range_value_binding(value$1) {
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
    if ("style" in $$props2)
      $$invalidate(4, style = $$props2.style);
    if ("minimum" in $$props2)
      $$invalidate(5, minimum = $$props2.minimum);
    if ("maximum" in $$props2)
      $$invalidate(6, maximum = $$props2.maximum);
    if ("step" in $$props2)
      $$invalidate(7, step = $$props2.step);
    if ("mode" in $$props2)
      $$invalidate(8, mode = $$props2.mode);
    if ("show_label" in $$props2)
      $$invalidate(9, show_label = $$props2.show_label);
    if ("loading_status" in $$props2)
      $$invalidate(10, loading_status = $$props2.loading_status);
  };
  return [
    value,
    elem_id,
    visible,
    label,
    style,
    minimum,
    maximum,
    step,
    mode,
    show_label,
    loading_status,
    range_value_binding,
    change_handler
  ];
}
class Slider extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 1,
      visible: 2,
      value: 0,
      label: 3,
      style: 4,
      minimum: 5,
      maximum: 6,
      step: 7,
      mode: 8,
      show_label: 9,
      loading_status: 10
    });
  }
}
var Slider$1 = Slider;
const modes = ["static", "dynamic"];
const document = (config) => {
  var _a;
  return {
    type: "number",
    description: "selected value",
    example_data: (_a = config.value) != null ? _a : config.minimum
  };
};
export { Slider$1 as Component, document, modes };
//# sourceMappingURL=index31.js.map
