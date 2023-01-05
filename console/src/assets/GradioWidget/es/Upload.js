import { S as SvelteComponent, i as init, s as safe_not_equal, p as create_slot, e as element, a as space, b as attr, d as toggle_class, f as insert, g as append, l as listen, W as stop_propagation, z as prevent_default, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, j as transition_in, k as transition_out, n as detach, A as run_all, F as createEventDispatcher, K as bubble, I as binding_callbacks } from "./main.js";
function create_fragment(ctx) {
  let div;
  let t;
  let input;
  let input_multiple_value;
  let input_webkitdirectory_value;
  let input_mozdirectory_value;
  let div_class_value;
  let current;
  let mounted;
  let dispose;
  const default_slot_template = ctx[14].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[13], null);
  return {
    c() {
      div = element("div");
      if (default_slot)
        default_slot.c();
      t = space();
      input = element("input");
      attr(input, "class", "hidden-upload hidden");
      attr(input, "type", "file");
      attr(input, "accept", ctx[0]);
      input.multiple = input_multiple_value = ctx[4] === "multiple" || void 0;
      attr(input, "webkitdirectory", input_webkitdirectory_value = ctx[4] === "directory" || void 0);
      attr(input, "mozdirectory", input_mozdirectory_value = ctx[4] === "directory" || void 0);
      attr(div, "class", div_class_value = "w-full cursor-pointer h-full items-center justify-center text-gray-400 md:text-xl " + (ctx[1] ? "min-h-[10rem] md:min-h-[15rem] max-h-[15rem] xl:max-h-[18rem] 2xl:max-h-[20rem]" : ""));
      toggle_class(div, "text-center", ctx[2]);
      toggle_class(div, "flex", ctx[3]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if (default_slot) {
        default_slot.m(div, null);
      }
      append(div, t);
      append(div, input);
      ctx[22](input);
      current = true;
      if (!mounted) {
        dispose = [
          listen(input, "change", ctx[8]),
          listen(div, "drag", stop_propagation(prevent_default(ctx[15]))),
          listen(div, "dragstart", stop_propagation(prevent_default(ctx[16]))),
          listen(div, "dragend", stop_propagation(prevent_default(ctx[17]))),
          listen(div, "dragover", stop_propagation(prevent_default(ctx[18]))),
          listen(div, "dragenter", stop_propagation(prevent_default(ctx[19]))),
          listen(div, "dragleave", stop_propagation(prevent_default(ctx[20]))),
          listen(div, "drop", stop_propagation(prevent_default(ctx[21]))),
          listen(div, "click", ctx[7]),
          listen(div, "drop", ctx[9]),
          listen(div, "dragenter", ctx[6]),
          listen(div, "dragleave", ctx[6])
        ];
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 8192)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[13], !current ? get_all_dirty_from_scope(ctx2[13]) : get_slot_changes(default_slot_template, ctx2[13], dirty, null), null);
        }
      }
      if (!current || dirty & 1) {
        attr(input, "accept", ctx2[0]);
      }
      if (!current || dirty & 16 && input_multiple_value !== (input_multiple_value = ctx2[4] === "multiple" || void 0)) {
        input.multiple = input_multiple_value;
      }
      if (!current || dirty & 16 && input_webkitdirectory_value !== (input_webkitdirectory_value = ctx2[4] === "directory" || void 0)) {
        attr(input, "webkitdirectory", input_webkitdirectory_value);
      }
      if (!current || dirty & 16 && input_mozdirectory_value !== (input_mozdirectory_value = ctx2[4] === "directory" || void 0)) {
        attr(input, "mozdirectory", input_mozdirectory_value);
      }
      if (!current || dirty & 2 && div_class_value !== (div_class_value = "w-full cursor-pointer h-full items-center justify-center text-gray-400 md:text-xl " + (ctx2[1] ? "min-h-[10rem] md:min-h-[15rem] max-h-[15rem] xl:max-h-[18rem] 2xl:max-h-[20rem]" : ""))) {
        attr(div, "class", div_class_value);
      }
      if (dirty & 6) {
        toggle_class(div, "text-center", ctx2[2]);
      }
      if (dirty & 10) {
        toggle_class(div, "flex", ctx2[3]);
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
        detach(div);
      if (default_slot)
        default_slot.d(detaching);
      ctx[22](null);
      mounted = false;
      run_all(dispose);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { $$slots: slots = {}, $$scope } = $$props;
  let { filetype = void 0 } = $$props;
  let { include_file_metadata = true } = $$props;
  let { dragging = false } = $$props;
  let { boundedheight = true } = $$props;
  let { center = true } = $$props;
  let { flex = true } = $$props;
  let { file_count = "single" } = $$props;
  let { disable_click = false } = $$props;
  let hidden_upload;
  const dispatch = createEventDispatcher();
  const updateDragging = () => {
    $$invalidate(10, dragging = !dragging);
  };
  const openFileUpload = () => {
    if (disable_click)
      return;
    $$invalidate(5, hidden_upload.value = "", hidden_upload);
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
  const loadFilesFromDrop = (e) => {
    var _a;
    $$invalidate(10, dragging = false);
    if (!((_a = e.dataTransfer) == null ? void 0 : _a.files))
      return;
    loadFiles(e.dataTransfer.files);
  };
  function drag_handler(event) {
    bubble.call(this, $$self, event);
  }
  function dragstart_handler(event) {
    bubble.call(this, $$self, event);
  }
  function dragend_handler(event) {
    bubble.call(this, $$self, event);
  }
  function dragover_handler(event) {
    bubble.call(this, $$self, event);
  }
  function dragenter_handler(event) {
    bubble.call(this, $$self, event);
  }
  function dragleave_handler(event) {
    bubble.call(this, $$self, event);
  }
  function drop_handler(event) {
    bubble.call(this, $$self, event);
  }
  function input_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      hidden_upload = $$value;
      $$invalidate(5, hidden_upload);
    });
  }
  $$self.$$set = ($$props2) => {
    if ("filetype" in $$props2)
      $$invalidate(0, filetype = $$props2.filetype);
    if ("include_file_metadata" in $$props2)
      $$invalidate(11, include_file_metadata = $$props2.include_file_metadata);
    if ("dragging" in $$props2)
      $$invalidate(10, dragging = $$props2.dragging);
    if ("boundedheight" in $$props2)
      $$invalidate(1, boundedheight = $$props2.boundedheight);
    if ("center" in $$props2)
      $$invalidate(2, center = $$props2.center);
    if ("flex" in $$props2)
      $$invalidate(3, flex = $$props2.flex);
    if ("file_count" in $$props2)
      $$invalidate(4, file_count = $$props2.file_count);
    if ("disable_click" in $$props2)
      $$invalidate(12, disable_click = $$props2.disable_click);
    if ("$$scope" in $$props2)
      $$invalidate(13, $$scope = $$props2.$$scope);
  };
  return [
    filetype,
    boundedheight,
    center,
    flex,
    file_count,
    hidden_upload,
    updateDragging,
    openFileUpload,
    loadFilesFromUpload,
    loadFilesFromDrop,
    dragging,
    include_file_metadata,
    disable_click,
    $$scope,
    slots,
    drag_handler,
    dragstart_handler,
    dragend_handler,
    dragover_handler,
    dragenter_handler,
    dragleave_handler,
    drop_handler,
    input_binding
  ];
}
class Upload extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      filetype: 0,
      include_file_metadata: 11,
      dragging: 10,
      boundedheight: 1,
      center: 2,
      flex: 3,
      file_count: 4,
      disable_click: 12
    });
  }
}
export { Upload as U };
//# sourceMappingURL=Upload.js.map
