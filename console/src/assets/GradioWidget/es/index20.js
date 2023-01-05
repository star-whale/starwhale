import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, b as attr, d as toggle_class, f as insert, x as noop, n as detach, F as createEventDispatcher, P as Block, c as create_component, m as mount_component, j as transition_in, k as transition_out, o as destroy_component, R as assign, T as StatusTracker, a as space, U as get_spread_update, V as get_spread_object, K as bubble } from "./main.js";
function create_fragment$1(ctx) {
  let div;
  return {
    c() {
      div = element("div");
      attr(div, "class", "output-html");
      attr(div, "id", ctx[0]);
      toggle_class(div, "min-h-[6rem]", ctx[3]);
      toggle_class(div, "!hidden", !ctx[2]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      div.innerHTML = ctx[1];
    },
    p(ctx2, [dirty]) {
      if (dirty & 2)
        div.innerHTML = ctx2[1];
      if (dirty & 1) {
        attr(div, "id", ctx2[0]);
      }
      if (dirty & 8) {
        toggle_class(div, "min-h-[6rem]", ctx2[3]);
      }
      if (dirty & 4) {
        toggle_class(div, "!hidden", !ctx2[2]);
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
function instance$1($$self, $$props, $$invalidate) {
  let { elem_id = "" } = $$props;
  let { value } = $$props;
  let { visible = true } = $$props;
  let { min_height = false } = $$props;
  const dispatch = createEventDispatcher();
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("value" in $$props2)
      $$invalidate(1, value = $$props2.value);
    if ("visible" in $$props2)
      $$invalidate(2, visible = $$props2.visible);
    if ("min_height" in $$props2)
      $$invalidate(3, min_height = $$props2.min_height);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 2) {
      dispatch("change");
    }
  };
  return [elem_id, value, visible, min_height];
}
class HTML extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      elem_id: 0,
      value: 1,
      visible: 2,
      min_height: 3
    });
  }
}
function create_default_slot(ctx) {
  var _a;
  let statustracker;
  let t;
  let div;
  let html;
  let current;
  const statustracker_spread_levels = [ctx[3], { variant: "center" }];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  html = new HTML({
    props: {
      min_height: ctx[3] && ((_a = ctx[3]) == null ? void 0 : _a.status) !== "complete",
      value: ctx[2],
      elem_id: ctx[0],
      visible: ctx[1]
    }
  });
  html.$on("change", ctx[5]);
  return {
    c() {
      var _a2;
      create_component(statustracker.$$.fragment);
      t = space();
      div = element("div");
      create_component(html.$$.fragment);
      attr(div, "class", "transition");
      toggle_class(div, "opacity-20", ((_a2 = ctx[3]) == null ? void 0 : _a2.status) === "pending");
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      insert(target, div, anchor);
      mount_component(html, div, null);
      current = true;
    },
    p(ctx2, dirty) {
      var _a2, _b;
      const statustracker_changes = dirty & 8 ? get_spread_update(statustracker_spread_levels, [
        get_spread_object(ctx2[3]),
        statustracker_spread_levels[1]
      ]) : {};
      statustracker.$set(statustracker_changes);
      const html_changes = {};
      if (dirty & 8)
        html_changes.min_height = ctx2[3] && ((_a2 = ctx2[3]) == null ? void 0 : _a2.status) !== "complete";
      if (dirty & 4)
        html_changes.value = ctx2[2];
      if (dirty & 1)
        html_changes.elem_id = ctx2[0];
      if (dirty & 2)
        html_changes.visible = ctx2[1];
      html.$set(html_changes);
      if (dirty & 8) {
        toggle_class(div, "opacity-20", ((_b = ctx2[3]) == null ? void 0 : _b.status) === "pending");
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(html.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(html.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      if (detaching)
        detach(div);
      destroy_component(html);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      visible: ctx[1],
      elem_id: ctx[0],
      disable: true,
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
      if (dirty & 2)
        block_changes.visible = ctx2[1];
      if (dirty & 1)
        block_changes.elem_id = ctx2[0];
      if (dirty & 143) {
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
  let { label } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = "" } = $$props;
  let { loading_status } = $$props;
  const dispatch = createEventDispatcher();
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("label" in $$props2)
      $$invalidate(4, label = $$props2.label);
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(1, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(2, value = $$props2.value);
    if ("loading_status" in $$props2)
      $$invalidate(3, loading_status = $$props2.loading_status);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 16) {
      dispatch("change");
    }
  };
  return [elem_id, visible, value, loading_status, label, change_handler];
}
class HTML_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      label: 4,
      elem_id: 0,
      visible: 1,
      value: 2,
      loading_status: 3
    });
  }
}
var HTML_1$1 = HTML_1;
const modes = ["static"];
const document = (config) => ({
  type: "string",
  description: "HTML output"
});
export { HTML_1$1 as Component, document, modes };
//# sourceMappingURL=index20.js.map
