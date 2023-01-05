import { S as SvelteComponent, i as init, s as safe_not_equal, p as create_slot, e as element, a as space, b as attr, d as toggle_class, f as insert, l as listen, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, j as transition_in, k as transition_out, n as detach, A as run_all, F as createEventDispatcher, Z as get_styles, I as binding_callbacks, c as create_component, m as mount_component, o as destroy_component, Q as component_subscribe, X, t as text, h as set_data, a9 as tick, K as bubble } from "./main.js";
function create_fragment$1(ctx) {
  let input;
  let input_multiple_value;
  let input_webkitdirectory_value;
  let input_mozdirectory_value;
  let t;
  let button;
  let button_class_value;
  let current;
  let mounted;
  let dispose;
  const default_slot_template = ctx[13].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[12], null);
  return {
    c() {
      input = element("input");
      t = space();
      button = element("button");
      if (default_slot)
        default_slot.c();
      attr(input, "class", "hidden-upload hidden");
      attr(input, "accept", ctx[5]);
      attr(input, "type", "file");
      input.multiple = input_multiple_value = ctx[3] === "multiple" || void 0;
      attr(input, "webkitdirectory", input_webkitdirectory_value = ctx[3] === "directory" || void 0);
      attr(input, "mozdirectory", input_mozdirectory_value = ctx[3] === "directory" || void 0);
      attr(button, "class", button_class_value = "gr-button gr-button-" + ctx[2] + " " + ctx[6]);
      attr(button, "id", ctx[0]);
      toggle_class(button, "!hidden", !ctx[1]);
    },
    m(target, anchor) {
      insert(target, input, anchor);
      ctx[14](input);
      insert(target, t, anchor);
      insert(target, button, anchor);
      if (default_slot) {
        default_slot.m(button, null);
      }
      current = true;
      if (!mounted) {
        dispose = [
          listen(input, "change", ctx[8]),
          listen(button, "click", ctx[7])
        ];
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (!current || dirty & 32) {
        attr(input, "accept", ctx2[5]);
      }
      if (!current || dirty & 8 && input_multiple_value !== (input_multiple_value = ctx2[3] === "multiple" || void 0)) {
        input.multiple = input_multiple_value;
      }
      if (!current || dirty & 8 && input_webkitdirectory_value !== (input_webkitdirectory_value = ctx2[3] === "directory" || void 0)) {
        attr(input, "webkitdirectory", input_webkitdirectory_value);
      }
      if (!current || dirty & 8 && input_mozdirectory_value !== (input_mozdirectory_value = ctx2[3] === "directory" || void 0)) {
        attr(input, "mozdirectory", input_mozdirectory_value);
      }
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 4096)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[12], !current ? get_all_dirty_from_scope(ctx2[12]) : get_slot_changes(default_slot_template, ctx2[12], dirty, null), null);
        }
      }
      if (!current || dirty & 68 && button_class_value !== (button_class_value = "gr-button gr-button-" + ctx2[2] + " " + ctx2[6])) {
        attr(button, "class", button_class_value);
      }
      if (!current || dirty & 1) {
        attr(button, "id", ctx2[0]);
      }
      if (dirty & 70) {
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
        detach(input);
      ctx[14](null);
      if (detaching)
        detach(t);
      if (detaching)
        detach(button);
      if (default_slot)
        default_slot.d(detaching);
      mounted = false;
      run_all(dispose);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let classes;
  let { $$slots: slots = {}, $$scope } = $$props;
  let { style = {} } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { size = "lg" } = $$props;
  let { file_count } = $$props;
  let { file_types = ["file"] } = $$props;
  let { include_file_metadata = true } = $$props;
  let hidden_upload;
  const dispatch = createEventDispatcher();
  let accept_file_types = "";
  try {
    file_types.forEach((type) => $$invalidate(5, accept_file_types += type + "/*, "));
  } catch (err) {
    if (err instanceof TypeError) {
      dispatch("error", "Please set file_types to a list.");
    } else {
      throw err;
    }
  }
  const openFileUpload = () => {
    hidden_upload.click();
  };
  const loadFiles = (files) => {
    let _files = Array.from(files);
    if (!files.length || !window.FileReader) {
      return;
    }
    if (file_count === "single") {
      _files = [files[0]];
    }
    var all_file_data = [];
    _files.forEach((f, i) => {
      let ReaderObj = new FileReader();
      ReaderObj.readAsDataURL(f);
      ReaderObj.onloadend = function() {
        all_file_data[i] = include_file_metadata ? {
          name: f.name,
          size: f.size,
          data: this.result
        } : this.result;
        if (all_file_data.filter((x) => x !== void 0).length === files.length) {
          dispatch("load", file_count == "single" ? all_file_data[0] : all_file_data);
        }
      };
    });
  };
  const loadFilesFromUpload = (e) => {
    const target = e.target;
    if (!target.files)
      return;
    loadFiles(target.files);
  };
  function input_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      hidden_upload = $$value;
      $$invalidate(4, hidden_upload);
    });
  }
  $$self.$$set = ($$props2) => {
    if ("style" in $$props2)
      $$invalidate(9, style = $$props2.style);
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(1, visible = $$props2.visible);
    if ("size" in $$props2)
      $$invalidate(2, size = $$props2.size);
    if ("file_count" in $$props2)
      $$invalidate(3, file_count = $$props2.file_count);
    if ("file_types" in $$props2)
      $$invalidate(10, file_types = $$props2.file_types);
    if ("include_file_metadata" in $$props2)
      $$invalidate(11, include_file_metadata = $$props2.include_file_metadata);
    if ("$$scope" in $$props2)
      $$invalidate(12, $$scope = $$props2.$$scope);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 512) {
      $$invalidate(6, { classes } = get_styles(style, ["full_width"]), classes);
    }
  };
  return [
    elem_id,
    visible,
    size,
    file_count,
    hidden_upload,
    accept_file_types,
    classes,
    openFileUpload,
    loadFilesFromUpload,
    style,
    file_types,
    include_file_metadata,
    $$scope,
    slots,
    input_binding
  ];
}
class UploadButton extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      style: 9,
      elem_id: 0,
      visible: 1,
      size: 2,
      file_count: 3,
      file_types: 10,
      include_file_metadata: 11
    });
  }
}
function create_default_slot(ctx) {
  let t_value = ctx[6](ctx[3]) + "";
  let t;
  return {
    c() {
      t = text(t_value);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 72 && t_value !== (t_value = ctx2[6](ctx2[3]) + ""))
        set_data(t, t_value);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_fragment(ctx) {
  let uploadbutton;
  let current;
  uploadbutton = new UploadButton({
    props: {
      elem_id: ctx[1],
      style: ctx[0],
      visible: ctx[2],
      file_count: ctx[4],
      file_types: ctx[5],
      $$slots: { default: [create_default_slot] },
      $$scope: { ctx }
    }
  });
  uploadbutton.$on("click", ctx[9]);
  uploadbutton.$on("load", ctx[7]);
  return {
    c() {
      create_component(uploadbutton.$$.fragment);
    },
    m(target, anchor) {
      mount_component(uploadbutton, target, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const uploadbutton_changes = {};
      if (dirty & 2)
        uploadbutton_changes.elem_id = ctx2[1];
      if (dirty & 1)
        uploadbutton_changes.style = ctx2[0];
      if (dirty & 4)
        uploadbutton_changes.visible = ctx2[2];
      if (dirty & 16)
        uploadbutton_changes.file_count = ctx2[4];
      if (dirty & 32)
        uploadbutton_changes.file_types = ctx2[5];
      if (dirty & 2120) {
        uploadbutton_changes.$$scope = { dirty, ctx: ctx2 };
      }
      uploadbutton.$set(uploadbutton_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(uploadbutton.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(uploadbutton.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(uploadbutton, detaching);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let $_;
  component_subscribe($$self, X, ($$value) => $$invalidate(6, $_ = $$value));
  let { style = {} } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { label } = $$props;
  let { value } = $$props;
  let { file_count } = $$props;
  let { file_types = ["file"] } = $$props;
  async function handle_upload({ detail }) {
    $$invalidate(8, value = detail);
    await tick();
    dispatch("change", value);
    dispatch("upload", detail);
  }
  const dispatch = createEventDispatcher();
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
    if ("label" in $$props2)
      $$invalidate(3, label = $$props2.label);
    if ("value" in $$props2)
      $$invalidate(8, value = $$props2.value);
    if ("file_count" in $$props2)
      $$invalidate(4, file_count = $$props2.file_count);
    if ("file_types" in $$props2)
      $$invalidate(5, file_types = $$props2.file_types);
  };
  return [
    style,
    elem_id,
    visible,
    label,
    file_count,
    file_types,
    $_,
    handle_upload,
    value,
    click_handler
  ];
}
class UploadButton_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      style: 0,
      elem_id: 1,
      visible: 2,
      label: 3,
      value: 8,
      file_count: 4,
      file_types: 5
    });
  }
}
var UploadButton_1$1 = UploadButton_1;
const modes = ["static"];
export { UploadButton_1$1 as Component, modes };
//# sourceMappingURL=index38.js.map
