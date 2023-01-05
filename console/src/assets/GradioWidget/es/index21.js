import { S as SvelteComponent, i as init, s as safe_not_equal, w as svg_element, b as attr, f as insert, g as append, x as noop, n as detach, e as element, M as src_url_equal, F as createEventDispatcher, ac as onMount, I as binding_callbacks, ad as add_render_callback, aj as create_bidirectional_transition, Y as set_style, l as listen, W as stop_propagation, A as run_all, a as space, ah as add_resize_listener, j as transition_in, D as group_outros, k as transition_out, E as check_outros, C as destroy_each, a9 as tick, J as onDestroy, ak as fade, K as bubble, c as create_component, m as mount_component, o as destroy_component, a7 as set_input_value, al as to_number, d as toggle_class, B as empty, L as add_flush_callback, O as bind, t as text, h as set_data, P as Block, Q as component_subscribe, X, R as assign, T as StatusTracker, U as get_spread_update, V as get_spread_object } from "./main.js";
import { B as BlockLabel } from "./BlockLabel.js";
import { I as Image } from "./Image2.js";
import { C as Cropper, i as index, U as Undo, W as Webcam } from "./Webcam.js";
import { I as IconButton, C as Clear, M as ModifyUpload } from "./ModifyUpload.js";
import { U as Upload } from "./Upload.js";
export { E as ExampleComponent } from "./Image.js";
function create_fragment$9(ctx) {
  let svg;
  let path0;
  let path1;
  return {
    c() {
      svg = svg_element("svg");
      path0 = svg_element("path");
      path1 = svg_element("path");
      attr(path0, "d", "M28.828 3.172a4.094 4.094 0 0 0-5.656 0L4.05 22.292A6.954 6.954 0 0 0 2 27.242V30h2.756a6.952 6.952 0 0 0 4.95-2.05L28.828 8.829a3.999 3.999 0 0 0 0-5.657zM10.91 18.26l2.829 2.829l-2.122 2.121l-2.828-2.828zm-2.619 8.276A4.966 4.966 0 0 1 4.756 28H4v-.759a4.967 4.967 0 0 1 1.464-3.535l1.91-1.91l2.829 2.828zM27.415 7.414l-12.261 12.26l-2.829-2.828l12.262-12.26a2.047 2.047 0 0 1 2.828 0a2 2 0 0 1 0 2.828z");
      attr(path0, "fill", "currentColor");
      attr(path1, "d", "M6.5 15a3.5 3.5 0 0 1-2.475-5.974l3.5-3.5a1.502 1.502 0 0 0 0-2.121a1.537 1.537 0 0 0-2.121 0L3.415 5.394L2 3.98l1.99-1.988a3.585 3.585 0 0 1 4.95 0a3.504 3.504 0 0 1 0 4.949L5.439 10.44a1.502 1.502 0 0 0 0 2.121a1.537 1.537 0 0 0 2.122 0l4.024-4.024L13 9.95l-4.025 4.024A3.475 3.475 0 0 1 6.5 15z");
      attr(path1, "fill", "currentColor");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "viewBox", "0 0 32 32");
    },
    m(target, anchor) {
      insert(target, svg, anchor);
      append(svg, path0);
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
class Brush extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$9, safe_not_equal, {});
  }
}
function create_fragment$8(ctx) {
  let svg;
  let circle0;
  let circle1;
  let circle2;
  let circle3;
  let circle4;
  let path;
  return {
    c() {
      svg = svg_element("svg");
      circle0 = svg_element("circle");
      circle1 = svg_element("circle");
      circle2 = svg_element("circle");
      circle3 = svg_element("circle");
      circle4 = svg_element("circle");
      path = svg_element("path");
      attr(circle0, "cx", "10");
      attr(circle0, "cy", "12");
      attr(circle0, "r", "2");
      attr(circle0, "fill", "currentColor");
      attr(circle1, "cx", "16");
      attr(circle1, "cy", "9");
      attr(circle1, "r", "2");
      attr(circle1, "fill", "currentColor");
      attr(circle2, "cx", "22");
      attr(circle2, "cy", "12");
      attr(circle2, "r", "2");
      attr(circle2, "fill", "currentColor");
      attr(circle3, "cx", "23");
      attr(circle3, "cy", "18");
      attr(circle3, "r", "2");
      attr(circle3, "fill", "currentColor");
      attr(circle4, "cx", "19");
      attr(circle4, "cy", "23");
      attr(circle4, "r", "2");
      attr(circle4, "fill", "currentColor");
      attr(path, "fill", "currentColor");
      attr(path, "d", "M16.54 2A14 14 0 0 0 2 16a4.82 4.82 0 0 0 6.09 4.65l1.12-.31a3 3 0 0 1 3.79 2.9V27a3 3 0 0 0 3 3a14 14 0 0 0 14-14.54A14.05 14.05 0 0 0 16.54 2Zm8.11 22.31A11.93 11.93 0 0 1 16 28a1 1 0 0 1-1-1v-3.76a5 5 0 0 0-5-5a5.07 5.07 0 0 0-1.33.18l-1.12.31A2.82 2.82 0 0 1 4 16A12 12 0 0 1 16.47 4A12.18 12.18 0 0 1 28 15.53a11.89 11.89 0 0 1-3.35 8.79Z");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "viewBox", "0 0 32 32");
    },
    m(target, anchor) {
      insert(target, svg, anchor);
      append(svg, circle0);
      append(svg, circle1);
      append(svg, circle2);
      append(svg, circle3);
      append(svg, circle4);
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
class Color extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$8, safe_not_equal, {});
  }
}
function create_fragment$7(ctx) {
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
class Sketch$1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$7, safe_not_equal, {});
  }
}
function create_fragment$6(ctx) {
  let img;
  let img_src_value;
  return {
    c() {
      img = element("img");
      if (!src_url_equal(img.src, img_src_value = ctx[0]))
        attr(img, "src", img_src_value);
      attr(img, "alt", "");
    },
    m(target, anchor) {
      insert(target, img, anchor);
      ctx[2](img);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1 && !src_url_equal(img.src, img_src_value = ctx2[0])) {
        attr(img, "src", img_src_value);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(img);
      ctx[2](null);
    }
  };
}
function instance$6($$self, $$props, $$invalidate) {
  let { image } = $$props;
  let el;
  const dispatch = createEventDispatcher();
  onMount(() => {
    const cropper = new Cropper(el, {
      autoCropArea: 1,
      cropend() {
        const image_data = cropper.getCroppedCanvas().toDataURL();
        dispatch("crop", image_data);
      }
    });
    dispatch("crop", image);
    return () => {
      cropper.destroy();
    };
  });
  function img_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      el = $$value;
      $$invalidate(1, el);
    });
  }
  $$self.$$set = ($$props2) => {
    if ("image" in $$props2)
      $$invalidate(0, image = $$props2.image);
  };
  return [image, el, img_binding];
}
class Cropper_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$6, create_fragment$6, safe_not_equal, { image: 0 });
  }
}
class Point {
  constructor(x, y) {
    this.x = x;
    this.y = y;
  }
}
class LazyPoint extends Point {
  update(point) {
    this.x = point.x;
    this.y = point.y;
  }
  moveByAngle(angle, distance) {
    const angleRotated = angle + Math.PI / 2;
    this.x = this.x + Math.sin(angleRotated) * distance, this.y = this.y - Math.cos(angleRotated) * distance;
  }
  equalsTo(point) {
    return this.x === point.x && this.y === point.y;
  }
  getDifferenceTo(point) {
    return new Point(this.x - point.x, this.y - point.y);
  }
  getDistanceTo(point) {
    const diff = this.getDifferenceTo(point);
    return Math.sqrt(Math.pow(diff.x, 2) + Math.pow(diff.y, 2));
  }
  getAngleTo(point) {
    const diff = this.getDifferenceTo(point);
    return Math.atan2(diff.y, diff.x);
  }
  toObject() {
    return {
      x: this.x,
      y: this.y
    };
  }
}
const RADIUS_DEFAULT = 30;
class LazyBrush {
  constructor({ radius = RADIUS_DEFAULT, enabled = true, initialPoint = { x: 0, y: 0 } } = {}) {
    this.radius = radius;
    this._isEnabled = enabled;
    this.pointer = new LazyPoint(initialPoint.x, initialPoint.y);
    this.brush = new LazyPoint(initialPoint.x, initialPoint.y);
    this.angle = 0;
    this.distance = 0;
    this._hasMoved = false;
  }
  enable() {
    this._isEnabled = true;
  }
  disable() {
    this._isEnabled = false;
  }
  isEnabled() {
    return this._isEnabled;
  }
  setRadius(radius) {
    this.radius = radius;
  }
  getRadius() {
    return this.radius;
  }
  getBrushCoordinates() {
    return this.brush.toObject();
  }
  getPointerCoordinates() {
    return this.pointer.toObject();
  }
  getBrush() {
    return this.brush;
  }
  getPointer() {
    return this.pointer;
  }
  getAngle() {
    return this.angle;
  }
  getDistance() {
    return this.distance;
  }
  brushHasMoved() {
    return this._hasMoved;
  }
  update(newPointerPoint, { both = false } = {}) {
    this._hasMoved = false;
    if (this.pointer.equalsTo(newPointerPoint) && !both) {
      return false;
    }
    this.pointer.update(newPointerPoint);
    if (both) {
      this._hasMoved = true;
      this.brush.update(newPointerPoint);
      return true;
    }
    if (this._isEnabled) {
      this.distance = this.pointer.getDistanceTo(this.brush);
      this.angle = this.pointer.getAngleTo(this.brush);
      if (this.distance > this.radius) {
        this.brush.moveByAngle(this.angle, this.distance - this.radius);
        this._hasMoved = true;
      }
    } else {
      this.distance = 0;
      this.angle = 0;
      this.brush.update(newPointerPoint);
      this._hasMoved = true;
    }
    return true;
  }
}
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[56] = list[i].name;
  child_ctx[57] = list[i].zIndex;
  child_ctx[58] = list;
  child_ctx[59] = i;
  return child_ctx;
}
function create_if_block$4(ctx) {
  let div;
  let div_transition;
  let current;
  return {
    c() {
      div = element("div");
      div.textContent = "Start drawing";
      attr(div, "class", "absolute inset-0 flex items-center justify-center z-40 pointer-events-none touch-none text-gray-400 md:text-xl");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      current = true;
    },
    i(local) {
      if (current)
        return;
      add_render_callback(() => {
        if (!div_transition)
          div_transition = create_bidirectional_transition(div, fade, { duration: 50 }, true);
        div_transition.run(1);
      });
      current = true;
    },
    o(local) {
      if (!div_transition)
        div_transition = create_bidirectional_transition(div, fade, { duration: 50 }, false);
      div_transition.run(0);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      if (detaching && div_transition)
        div_transition.end();
    }
  };
}
function create_each_block(ctx) {
  let canvas_1;
  let canvas_1_key_value;
  let name = ctx[56];
  let mounted;
  let dispose;
  const assign_canvas_1 = () => ctx[27](canvas_1, name);
  const unassign_canvas_1 = () => ctx[27](null, name);
  return {
    c() {
      canvas_1 = element("canvas");
      attr(canvas_1, "key", canvas_1_key_value = ctx[56]);
      attr(canvas_1, "class", "inset-0 m-auto hover:cursor-none");
      set_style(canvas_1, "display", "block");
      set_style(canvas_1, "position", "absolute");
      set_style(canvas_1, "z-index", ctx[57]);
    },
    m(target, anchor) {
      insert(target, canvas_1, anchor);
      assign_canvas_1();
      if (!mounted) {
        dispose = [
          listen(canvas_1, "mousedown", ctx[56] === "interface" ? ctx[6] : void 0),
          listen(canvas_1, "mousemove", ctx[56] === "interface" ? ctx[7] : void 0),
          listen(canvas_1, "mouseup", ctx[56] === "interface" ? ctx[8] : void 0),
          listen(canvas_1, "mouseout", ctx[56] === "interface" ? ctx[8] : void 0),
          listen(canvas_1, "blur", ctx[56] === "interface" ? ctx[8] : void 0),
          listen(canvas_1, "touchstart", ctx[56] === "interface" ? ctx[6] : void 0),
          listen(canvas_1, "touchmove", ctx[56] === "interface" ? ctx[7] : void 0),
          listen(canvas_1, "touchend", ctx[56] === "interface" ? ctx[8] : void 0),
          listen(canvas_1, "touchcancel", ctx[56] === "interface" ? ctx[8] : void 0),
          listen(canvas_1, "click", stop_propagation(ctx[26]))
        ];
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (name !== ctx[56]) {
        unassign_canvas_1();
        name = ctx[56];
        assign_canvas_1();
      }
    },
    d(detaching) {
      if (detaching)
        detach(canvas_1);
      unassign_canvas_1();
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_fragment$5(ctx) {
  let div;
  let t;
  let div_resize_listener;
  let current;
  let if_block = ctx[4] === 0 && create_if_block$4();
  let each_value = ctx[5];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  return {
    c() {
      div = element("div");
      if (if_block)
        if_block.c();
      t = space();
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", "touch-none relative h-full w-full");
      add_render_callback(() => ctx[29].call(div));
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if (if_block)
        if_block.m(div, null);
      append(div, t);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
      ctx[28](div);
      div_resize_listener = add_resize_listener(div, ctx[29].bind(div));
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[4] === 0) {
        if (if_block) {
          if (dirty[0] & 16) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block$4();
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
      if (dirty[0] & 481) {
        each_value = ctx2[5];
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
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      if (if_block)
        if_block.d();
      destroy_each(each_blocks, detaching);
      ctx[28](null);
      div_resize_listener();
    }
  };
}
let catenary_color = "#aaa";
function mid_point(p1, p2) {
  return {
    x: p1.x + (p2.x - p1.x) / 2,
    y: p1.y + (p2.y - p1.y) / 2
  };
}
function instance$5($$self, $$props, $$invalidate) {
  let brush_dot;
  const dispatch = createEventDispatcher();
  let { value } = $$props;
  let { value_img } = $$props;
  let { mode = "sketch" } = $$props;
  let { brush_color = "#0b0f19" } = $$props;
  let { brush_radius = 50 } = $$props;
  let { source } = $$props;
  let { width = 400 } = $$props;
  let { height = 200 } = $$props;
  let { container_height = 200 } = $$props;
  let mounted;
  let canvas_width = width;
  let canvas_height = height;
  let last_value_img;
  const canvas_types = [
    { name: "interface", zIndex: 15 },
    { name: "drawing", zIndex: 11 },
    { name: "temp", zIndex: 12 },
    { name: "mask", zIndex: -1 },
    { name: "temp_fake", zIndex: -2 }
  ];
  let canvas = {};
  let ctx = {};
  let points = [];
  let lines = [];
  let mouse_has_moved = true;
  let values_changed = true;
  let is_drawing = false;
  let is_pressing = false;
  let lazy = null;
  let canvas_container = null;
  let canvas_observer = null;
  let line_count = 0;
  onMount(async () => {
    Object.keys(canvas).forEach((key) => {
      $$invalidate(23, ctx[key] = canvas[key].getContext("2d"), ctx);
    });
    await tick();
    if (value_img) {
      value_img.addEventListener("load", (_) => {
        if (source === "webcam") {
          ctx.temp.save();
          ctx.temp.translate(width, 0);
          ctx.temp.scale(-1, 1);
          ctx.temp.drawImage(value_img, 0, 0);
          ctx.temp.restore();
        } else {
          ctx.temp.drawImage(value_img, 0, 0);
        }
        ctx.drawing.drawImage(canvas.temp, 0, 0, width, height);
        trigger_on_change();
      });
      setTimeout(() => {
        if (source === "webcam") {
          ctx.temp.save();
          ctx.temp.translate(width, 0);
          ctx.temp.scale(-1, 1);
          ctx.temp.drawImage(value_img, 0, 0);
          ctx.temp.restore();
        } else {
          ctx.temp.drawImage(value_img, 0, 0);
        }
        ctx.drawing.drawImage(canvas.temp, 0, 0, width, height);
        draw_lines({ lines: lines.slice() });
        trigger_on_change();
      }, 100);
    }
    $$invalidate(25, lazy = new LazyBrush({
      radius: brush_radius * 0.05,
      enabled: true,
      initialPoint: { x: width / 2, y: height / 2 }
    }));
    canvas_observer = new index((entries, observer, ...rest) => {
      handle_canvas_resize(entries, observer);
    });
    canvas_observer.observe(canvas_container);
    loop();
    $$invalidate(21, mounted = true);
    requestAnimationFrame(() => {
      init2();
      requestAnimationFrame(() => {
        clear();
      });
    });
  });
  function init2() {
    const initX = width / 2;
    const initY = height / 2;
    lazy.update({ x: initX, y: initY }, { both: true });
    lazy.update({ x: initX, y: initY }, { both: false });
    mouse_has_moved = true;
    values_changed = true;
  }
  onDestroy(() => {
    $$invalidate(21, mounted = false);
    canvas_observer.unobserve(canvas_container);
  });
  function undo() {
    const _lines = lines.slice(0, -1);
    clear_canvas();
    if (value_img) {
      if (source === "webcam") {
        ctx.temp.save();
        ctx.temp.translate(width, 0);
        ctx.temp.scale(-1, 1);
        ctx.temp.drawImage(value_img, 0, 0);
        ctx.temp.restore();
      } else {
        ctx.temp.drawImage(value_img, 0, 0);
      }
      if (!lines || !lines.length) {
        ctx.drawing.drawImage(canvas.temp, 0, 0, width, height);
      }
    }
    draw_lines({ lines: _lines });
    $$invalidate(4, line_count = _lines.length);
    if (lines.length) {
      $$invalidate(24, lines = _lines);
    }
    trigger_on_change();
  }
  let draw_lines = ({ lines: lines2 }) => {
    lines2.forEach((line) => {
      const { points: _points, brush_color: brush_color2, brush_radius: brush_radius2 } = line;
      draw_points({
        points: _points,
        brush_color: brush_color2,
        brush_radius: brush_radius2
      });
      if (mode === "mask") {
        draw_fake_points({
          points: _points,
          brush_color: brush_color2,
          brush_radius: brush_radius2
        });
      }
      points = _points;
      return;
    });
    saveLine({ brush_color, brush_radius });
    if (mode === "mask") {
      save_mask_line();
    }
  };
  let handle_draw_start = (e) => {
    e.preventDefault();
    is_pressing = true;
    const { x, y } = get_pointer_pos(e);
    if (e.touches && e.touches.length > 0) {
      lazy.update({ x, y }, { both: true });
    }
    handle_pointer_move(x, y);
    $$invalidate(4, line_count += 1);
  };
  let handle_draw_move = (e) => {
    e.preventDefault();
    const { x, y } = get_pointer_pos(e);
    handle_pointer_move(x, y);
  };
  let handle_draw_end = (e) => {
    e.preventDefault();
    handle_draw_move(e);
    is_drawing = false;
    is_pressing = false;
    saveLine();
    if (mode === "mask") {
      save_mask_line();
    }
  };
  let old_width = 0;
  let old_height = 0;
  let old_container_height = 0;
  let handle_canvas_resize = async () => {
    if (width === old_width && height === old_height && old_container_height === container_height) {
      return;
    }
    const dimensions = { width, height };
    const container_dimensions = {
      height: container_height,
      width: container_height * (dimensions.width / dimensions.height)
    };
    await Promise.all([
      set_canvas_size(canvas.interface, dimensions, container_dimensions),
      set_canvas_size(canvas.drawing, dimensions, container_dimensions),
      set_canvas_size(canvas.temp, dimensions, container_dimensions),
      set_canvas_size(canvas.temp_fake, dimensions, container_dimensions),
      set_canvas_size(canvas.mask, dimensions, container_dimensions, false)
    ]);
    $$invalidate(9, brush_radius = 20 * (dimensions.width / container_dimensions.width));
    loop({ once: true });
    setTimeout(() => {
      old_height = height;
      old_width = width;
      old_container_height = container_height;
    }, 100);
    clear();
  };
  let set_canvas_size = async (canvas2, dimensions, container, scale = true) => {
    if (!mounted)
      return;
    await tick();
    const dpr = window.devicePixelRatio || 1;
    canvas2.width = dimensions.width * (scale ? dpr : 1);
    canvas2.height = dimensions.height * (scale ? dpr : 1);
    const ctx2 = canvas2.getContext("2d");
    scale && ctx2.scale(dpr, dpr);
    canvas2.style.width = `${container.width}px`;
    canvas2.style.height = `${container.height}px`;
  };
  let get_pointer_pos = (e) => {
    const rect = canvas.interface.getBoundingClientRect();
    let clientX = e.clientX;
    let clientY = e.clientY;
    if (e.changedTouches && e.changedTouches.length > 0) {
      clientX = e.changedTouches[0].clientX;
      clientY = e.changedTouches[0].clientY;
    }
    return {
      x: (clientX - rect.left) / rect.width * width,
      y: (clientY - rect.top) / rect.height * height
    };
  };
  let handle_pointer_move = (x, y) => {
    lazy.update({ x, y });
    const is_disabled = !lazy.isEnabled();
    if (is_pressing && !is_drawing || is_disabled && is_pressing) {
      is_drawing = true;
      points.push(lazy.brush.toObject());
    }
    if (is_drawing) {
      points.push(lazy.brush.toObject());
      draw_points({ points, brush_color, brush_radius });
      if (mode === "mask") {
        draw_fake_points({ points, brush_color, brush_radius });
      }
    }
    mouse_has_moved = true;
  };
  let draw_points = ({ points: points2, brush_color: brush_color2, brush_radius: brush_radius2 }) => {
    if (!points2 || points2.length < 2)
      return;
    $$invalidate(23, ctx.temp.lineJoin = "round", ctx);
    $$invalidate(23, ctx.temp.lineCap = "round", ctx);
    $$invalidate(23, ctx.temp.strokeStyle = brush_color2, ctx);
    $$invalidate(23, ctx.temp.lineWidth = brush_radius2, ctx);
    if (!points2 || points2.length < 2)
      return;
    let p1 = points2[0];
    let p2 = points2[1];
    ctx.temp.moveTo(p2.x, p2.y);
    ctx.temp.beginPath();
    for (var i = 1, len = points2.length; i < len; i++) {
      var midPoint = mid_point(p1, p2);
      ctx.temp.quadraticCurveTo(p1.x, p1.y, midPoint.x, midPoint.y);
      p1 = points2[i];
      p2 = points2[i + 1];
    }
    ctx.temp.lineTo(p1.x, p1.y);
    ctx.temp.stroke();
  };
  let draw_fake_points = ({ points: points2, brush_color: brush_color2, brush_radius: brush_radius2 }) => {
    if (!points2 || points2.length < 2)
      return;
    $$invalidate(23, ctx.temp_fake.lineJoin = "round", ctx);
    $$invalidate(23, ctx.temp_fake.lineCap = "round", ctx);
    $$invalidate(23, ctx.temp_fake.strokeStyle = "#fff", ctx);
    $$invalidate(23, ctx.temp_fake.lineWidth = brush_radius2, ctx);
    let p1 = points2[0];
    let p2 = points2[1];
    ctx.temp_fake.moveTo(p2.x, p2.y);
    ctx.temp_fake.beginPath();
    for (var i = 1, len = points2.length; i < len; i++) {
      var midPoint = mid_point(p1, p2);
      ctx.temp_fake.quadraticCurveTo(p1.x, p1.y, midPoint.x, midPoint.y);
      p1 = points2[i];
      p2 = points2[i + 1];
    }
    ctx.temp_fake.lineTo(p1.x, p1.y);
    ctx.temp_fake.stroke();
  };
  let save_mask_line = () => {
    if (points.length < 1)
      return;
    points.length = 0;
    ctx.mask.drawImage(canvas.temp_fake, 0, 0, width, height);
    trigger_on_change();
  };
  let saveLine = () => {
    if (points.length < 1)
      return;
    lines.push({
      points: points.slice(),
      brush_color,
      brush_radius
    });
    if (mode !== "mask") {
      points.length = 0;
    }
    ctx.drawing.drawImage(canvas.temp, 0, 0, width, height);
    trigger_on_change();
  };
  let trigger_on_change = () => {
    const x = get_image_data();
    dispatch("change", x);
  };
  function clear() {
    $$invalidate(24, lines = []);
    clear_canvas();
    $$invalidate(4, line_count = 0);
    return true;
  }
  function clear_canvas() {
    values_changed = true;
    ctx.temp.clearRect(0, 0, width, height);
    $$invalidate(23, ctx.temp.fillStyle = mode === "mask" ? "transparent" : "#FFFFFF", ctx);
    ctx.temp.fillRect(0, 0, width, height);
    if (mode === "mask") {
      ctx.temp_fake.clearRect(0, 0, canvas.temp_fake.width, canvas.temp_fake.height);
      ctx.mask.clearRect(0, 0, width, height);
      $$invalidate(23, ctx.mask.fillStyle = "#000", ctx);
      ctx.mask.fillRect(0, 0, width, height);
    }
  }
  let loop = ({ once = false } = {}) => {
    if (mouse_has_moved || values_changed) {
      const pointer = lazy.getPointerCoordinates();
      const brush = lazy.getBrushCoordinates();
      draw_interface(ctx.interface, pointer, brush);
      mouse_has_moved = false;
      values_changed = false;
    }
    if (!once) {
      window.requestAnimationFrame(() => {
        loop();
      });
    }
  };
  let draw_interface = (ctx2, pointer, brush) => {
    ctx2.clearRect(0, 0, width, height);
    ctx2.beginPath();
    ctx2.fillStyle = brush_color;
    ctx2.arc(brush.x, brush.y, brush_radius / 2, 0, Math.PI * 2, true);
    ctx2.fill();
    ctx2.beginPath();
    ctx2.fillStyle = catenary_color;
    ctx2.arc(brush.x, brush.y, brush_dot, 0, Math.PI * 2, true);
    ctx2.fill();
  };
  function get_image_data() {
    return mode === "mask" ? canvas.mask.toDataURL("image/jpg") : canvas.drawing.toDataURL("image/jpg");
  }
  function click_handler(event) {
    bubble.call(this, $$self, event);
  }
  function canvas_1_binding($$value, name) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      canvas[name] = $$value;
      $$invalidate(0, canvas);
    });
  }
  function div_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      canvas_container = $$value;
      $$invalidate(3, canvas_container);
    });
  }
  function div_elementresize_handler() {
    canvas_width = this.offsetWidth;
    canvas_height = this.offsetHeight;
    $$invalidate(1, canvas_width);
    $$invalidate(2, canvas_height);
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(10, value = $$props2.value);
    if ("value_img" in $$props2)
      $$invalidate(11, value_img = $$props2.value_img);
    if ("mode" in $$props2)
      $$invalidate(12, mode = $$props2.mode);
    if ("brush_color" in $$props2)
      $$invalidate(13, brush_color = $$props2.brush_color);
    if ("brush_radius" in $$props2)
      $$invalidate(9, brush_radius = $$props2.brush_radius);
    if ("source" in $$props2)
      $$invalidate(14, source = $$props2.source);
    if ("width" in $$props2)
      $$invalidate(15, width = $$props2.width);
    if ("height" in $$props2)
      $$invalidate(16, height = $$props2.height);
    if ("container_height" in $$props2)
      $$invalidate(17, container_height = $$props2.container_height);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty[0] & 2098176) {
      mounted && !value && clear();
    }
    if ($$self.$$.dirty[0] & 31574017) {
      {
        if (mounted && value_img !== last_value_img) {
          $$invalidate(22, last_value_img = value_img);
          clear();
          setTimeout(() => {
            if (source === "webcam") {
              ctx.temp.save();
              ctx.temp.translate(width, 0);
              ctx.temp.scale(-1, 1);
              ctx.temp.drawImage(value_img, 0, 0);
              ctx.temp.restore();
            } else {
              ctx.temp.drawImage(value_img, 0, 0);
            }
            ctx.drawing.drawImage(canvas.temp, 0, 0, width, height);
            draw_lines({ lines: lines.slice() });
            trigger_on_change();
          }, 50);
        }
      }
    }
    if ($$self.$$.dirty[0] & 33554944) {
      {
        if (lazy) {
          init2();
          lazy.setRadius(brush_radius * 0.05);
        }
      }
    }
    if ($$self.$$.dirty[0] & 98304) {
      {
        if (width || height) {
          handle_canvas_resize();
        }
      }
    }
    if ($$self.$$.dirty[0] & 512) {
      brush_dot = brush_radius * 0.075;
    }
  };
  return [
    canvas,
    canvas_width,
    canvas_height,
    canvas_container,
    line_count,
    canvas_types,
    handle_draw_start,
    handle_draw_move,
    handle_draw_end,
    brush_radius,
    value,
    value_img,
    mode,
    brush_color,
    source,
    width,
    height,
    container_height,
    undo,
    clear,
    get_image_data,
    mounted,
    last_value_img,
    ctx,
    lines,
    lazy,
    click_handler,
    canvas_1_binding,
    div_binding,
    div_elementresize_handler
  ];
}
class Sketch extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$5, create_fragment$5, safe_not_equal, {
      value: 10,
      value_img: 11,
      mode: 12,
      brush_color: 13,
      brush_radius: 9,
      source: 14,
      width: 15,
      height: 16,
      container_height: 17,
      undo: 18,
      clear: 19,
      get_image_data: 20
    }, null, [-1, -1]);
  }
  get undo() {
    return this.$$.ctx[18];
  }
  get clear() {
    return this.$$.ctx[19];
  }
  get get_image_data() {
    return this.$$.ctx[20];
  }
}
function create_fragment$4(ctx) {
  let div;
  let iconbutton0;
  let t;
  let iconbutton1;
  let current;
  iconbutton0 = new IconButton({ props: { Icon: Undo, label: "Undo" } });
  iconbutton0.$on("click", ctx[1]);
  iconbutton1 = new IconButton({ props: { Icon: Clear, label: "Clear" } });
  iconbutton1.$on("click", ctx[2]);
  return {
    c() {
      div = element("div");
      create_component(iconbutton0.$$.fragment);
      t = space();
      create_component(iconbutton1.$$.fragment);
      attr(div, "class", "z-50 top-2 right-2 justify-end flex gap-1 absolute");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(iconbutton0, div, null);
      append(div, t);
      mount_component(iconbutton1, div, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(iconbutton0.$$.fragment, local);
      transition_in(iconbutton1.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(iconbutton0.$$.fragment, local);
      transition_out(iconbutton1.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(iconbutton0);
      destroy_component(iconbutton1);
    }
  };
}
function instance$4($$self) {
  const dispatch = createEventDispatcher();
  const click_handler = () => dispatch("undo");
  const click_handler_1 = (event) => {
    dispatch("clear");
    event.stopPropagation();
  };
  return [dispatch, click_handler, click_handler_1];
}
class ModifySketch extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$4, create_fragment$4, safe_not_equal, {});
  }
}
function create_if_block_2$1(ctx) {
  let input;
  let input_min_value;
  let input_max_value;
  let mounted;
  let dispose;
  return {
    c() {
      input = element("input");
      attr(input, "aria-label", "Brush radius");
      attr(input, "class", "absolute top-[2px] right-6");
      attr(input, "type", "range");
      attr(input, "min", input_min_value = 0.5 * (ctx[2] / ctx[6]));
      attr(input, "max", input_max_value = 75 * (ctx[2] / ctx[6]));
    },
    m(target, anchor) {
      insert(target, input, anchor);
      set_input_value(input, ctx[0]);
      if (!mounted) {
        dispose = [
          listen(input, "change", ctx[10]),
          listen(input, "input", ctx[10])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 68 && input_min_value !== (input_min_value = 0.5 * (ctx2[2] / ctx2[6]))) {
        attr(input, "min", input_min_value);
      }
      if (dirty & 68 && input_max_value !== (input_max_value = 75 * (ctx2[2] / ctx2[6]))) {
        attr(input, "max", input_max_value);
      }
      if (dirty & 1) {
        set_input_value(input, ctx2[0]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(input);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block$3(ctx) {
  let span;
  let iconbutton;
  let t;
  let current;
  iconbutton = new IconButton({
    props: { Icon: Color, label: "Select brush color" }
  });
  iconbutton.$on("click", ctx[11]);
  let if_block = ctx[5] && create_if_block_1$1(ctx);
  return {
    c() {
      span = element("span");
      create_component(iconbutton.$$.fragment);
      t = space();
      if (if_block)
        if_block.c();
      attr(span, "class", "absolute top-6 right-0");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      mount_component(iconbutton, span, null);
      append(span, t);
      if (if_block)
        if_block.m(span, null);
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[5]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block_1$1(ctx2);
          if_block.c();
          if_block.m(span, null);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
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
      if (detaching)
        detach(span);
      destroy_component(iconbutton);
      if (if_block)
        if_block.d();
    }
  };
}
function create_if_block_1$1(ctx) {
  let input;
  let mounted;
  let dispose;
  return {
    c() {
      input = element("input");
      attr(input, "aria-label", "Brush color");
      attr(input, "class", "absolute top-[-3px] right-6");
      attr(input, "type", "color");
    },
    m(target, anchor) {
      insert(target, input, anchor);
      set_input_value(input, ctx[1]);
      if (!mounted) {
        dispose = listen(input, "input", ctx[12]);
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 2) {
        set_input_value(input, ctx2[1]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(input);
      mounted = false;
      dispose();
    }
  };
}
function create_fragment$3(ctx) {
  let div;
  let span;
  let iconbutton;
  let t0;
  let t1;
  let current;
  iconbutton = new IconButton({
    props: { Icon: Brush, label: "Use brush" }
  });
  iconbutton.$on("click", ctx[9]);
  let if_block0 = ctx[4] && create_if_block_2$1(ctx);
  let if_block1 = ctx[3] !== "mask" && create_if_block$3(ctx);
  return {
    c() {
      div = element("div");
      span = element("span");
      create_component(iconbutton.$$.fragment);
      t0 = space();
      if (if_block0)
        if_block0.c();
      t1 = space();
      if (if_block1)
        if_block1.c();
      attr(span, "class", "absolute top-0 right-0");
      attr(div, "class", "z-50 top-10 right-2 justify-end flex gap-1 absolute");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, span);
      mount_component(iconbutton, span, null);
      append(span, t0);
      if (if_block0)
        if_block0.m(span, null);
      append(div, t1);
      if (if_block1)
        if_block1.m(div, null);
      current = true;
    },
    p(ctx2, [dirty]) {
      if (ctx2[4]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_2$1(ctx2);
          if_block0.c();
          if_block0.m(span, null);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (ctx2[3] !== "mask") {
        if (if_block1) {
          if_block1.p(ctx2, dirty);
          if (dirty & 8) {
            transition_in(if_block1, 1);
          }
        } else {
          if_block1 = create_if_block$3(ctx2);
          if_block1.c();
          transition_in(if_block1, 1);
          if_block1.m(div, null);
        }
      } else if (if_block1) {
        group_outros();
        transition_out(if_block1, 1, 1, () => {
          if_block1 = null;
        });
        check_outros();
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(iconbutton.$$.fragment, local);
      transition_in(if_block1);
      current = true;
    },
    o(local) {
      transition_out(iconbutton.$$.fragment, local);
      transition_out(if_block1);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(iconbutton);
      if (if_block0)
        if_block0.d();
      if (if_block1)
        if_block1.d();
    }
  };
}
function instance$3($$self, $$props, $$invalidate) {
  let width;
  createEventDispatcher();
  let show_size = false;
  let show_col = false;
  let { brush_radius = 20 } = $$props;
  let { brush_color = "#000" } = $$props;
  let { container_height } = $$props;
  let { img_width } = $$props;
  let { img_height } = $$props;
  let { mode = "other" } = $$props;
  const click_handler = () => $$invalidate(4, show_size = !show_size);
  function input_change_input_handler() {
    brush_radius = to_number(this.value);
    $$invalidate(0, brush_radius);
  }
  const click_handler_1 = () => $$invalidate(5, show_col = !show_col);
  function input_input_handler() {
    brush_color = this.value;
    $$invalidate(1, brush_color);
  }
  $$self.$$set = ($$props2) => {
    if ("brush_radius" in $$props2)
      $$invalidate(0, brush_radius = $$props2.brush_radius);
    if ("brush_color" in $$props2)
      $$invalidate(1, brush_color = $$props2.brush_color);
    if ("container_height" in $$props2)
      $$invalidate(7, container_height = $$props2.container_height);
    if ("img_width" in $$props2)
      $$invalidate(2, img_width = $$props2.img_width);
    if ("img_height" in $$props2)
      $$invalidate(8, img_height = $$props2.img_height);
    if ("mode" in $$props2)
      $$invalidate(3, mode = $$props2.mode);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 388) {
      $$invalidate(6, width = container_height * (img_width / img_height));
    }
  };
  return [
    brush_radius,
    brush_color,
    img_width,
    mode,
    show_size,
    show_col,
    width,
    container_height,
    img_height,
    click_handler,
    input_change_input_handler,
    click_handler_1,
    input_input_handler
  ];
}
class SketchSettings extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$3, create_fragment$3, safe_not_equal, {
      brush_radius: 0,
      brush_color: 1,
      container_height: 7,
      img_width: 2,
      img_height: 8,
      mode: 3
    });
  }
}
function create_else_block_1(ctx) {
  let img;
  let img_src_value;
  return {
    c() {
      img = element("img");
      attr(img, "class", "w-full h-full object-contain");
      if (!src_url_equal(img.src, img_src_value = ctx[0].image || ctx[0]))
        attr(img, "src", img_src_value);
      attr(img, "alt", "");
      toggle_class(img, "scale-x-[-1]", ctx[4] === "webcam" && ctx[10]);
    },
    m(target, anchor) {
      insert(target, img, anchor);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 1 && !src_url_equal(img.src, img_src_value = ctx2[0].image || ctx2[0])) {
        attr(img, "src", img_src_value);
      }
      if (dirty[0] & 1040) {
        toggle_class(img, "scale-x-[-1]", ctx2[4] === "webcam" && ctx2[10]);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(img);
    }
  };
}
function create_if_block_13(ctx) {
  let previous_key = ctx[21];
  let t;
  let if_block_anchor;
  let current;
  let key_block = create_key_block_1(ctx);
  let if_block = ctx[15] > 0 && create_if_block_14(ctx);
  return {
    c() {
      key_block.c();
      t = space();
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      key_block.m(target, anchor);
      insert(target, t, anchor);
      if (if_block)
        if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (dirty[0] & 2097152 && safe_not_equal(previous_key, previous_key = ctx2[21])) {
        key_block.d(1);
        key_block = create_key_block_1(ctx2);
        key_block.c();
        key_block.m(t.parentNode, t);
      } else {
        key_block.p(ctx2, dirty);
      }
      if (ctx2[15] > 0) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty[0] & 32768) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block_14(ctx2);
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
      key_block.d(detaching);
      if (detaching)
        detach(t);
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block_12(ctx) {
  let modifyupload;
  let t;
  let img;
  let img_src_value;
  let current;
  modifyupload = new ModifyUpload({ props: { editable: true } });
  modifyupload.$on("edit", ctx[48]);
  modifyupload.$on("clear", ctx[24]);
  return {
    c() {
      create_component(modifyupload.$$.fragment);
      t = space();
      img = element("img");
      attr(img, "class", "w-full h-full object-contain");
      if (!src_url_equal(img.src, img_src_value = ctx[0]))
        attr(img, "src", img_src_value);
      attr(img, "alt", "");
      toggle_class(img, "scale-x-[-1]", ctx[4] === "webcam" && ctx[10]);
    },
    m(target, anchor) {
      mount_component(modifyupload, target, anchor);
      insert(target, t, anchor);
      insert(target, img, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (!current || dirty[0] & 1 && !src_url_equal(img.src, img_src_value = ctx2[0])) {
        attr(img, "src", img_src_value);
      }
      if (dirty[0] & 1040) {
        toggle_class(img, "scale-x-[-1]", ctx2[4] === "webcam" && ctx2[10]);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(modifyupload.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(modifyupload.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(modifyupload, detaching);
      if (detaching)
        detach(t);
      if (detaching)
        detach(img);
    }
  };
}
function create_if_block_11(ctx) {
  let cropper;
  let t;
  let modifyupload;
  let current;
  cropper = new Cropper_1({ props: { image: ctx[0] } });
  cropper.$on("crop", ctx[25]);
  modifyupload = new ModifyUpload({});
  modifyupload.$on("clear", ctx[47]);
  return {
    c() {
      create_component(cropper.$$.fragment);
      t = space();
      create_component(modifyupload.$$.fragment);
    },
    m(target, anchor) {
      mount_component(cropper, target, anchor);
      insert(target, t, anchor);
      mount_component(modifyupload, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const cropper_changes = {};
      if (dirty[0] & 1)
        cropper_changes.image = ctx2[0];
      cropper.$set(cropper_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(cropper.$$.fragment, local);
      transition_in(modifyupload.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(cropper.$$.fragment, local);
      transition_out(modifyupload.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(cropper, detaching);
      if (detaching)
        detach(t);
      destroy_component(modifyupload, detaching);
    }
  };
}
function create_if_block_9(ctx) {
  let if_block_anchor;
  let current;
  let if_block = ctx[4] === "webcam" && !ctx[21] && create_if_block_10(ctx);
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
    p(ctx2, dirty) {
      if (ctx2[4] === "webcam" && !ctx2[21]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty[0] & 2097168) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block_10(ctx2);
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
function create_if_block_7(ctx) {
  let modifysketch;
  let t0;
  let t1;
  let sketch_1;
  let updating_brush_radius;
  let updating_brush_color;
  let current;
  modifysketch = new ModifySketch({});
  modifysketch.$on("undo", ctx[38]);
  modifysketch.$on("clear", ctx[39]);
  let if_block = ctx[1] === "color-sketch" && create_if_block_8(ctx);
  function sketch_1_brush_radius_binding_1(value) {
    ctx[42](value);
  }
  function sketch_1_brush_color_binding_1(value) {
    ctx[43](value);
  }
  let sketch_1_props = {
    value: ctx[0],
    mode: ctx[12],
    width: ctx[15] || ctx[20],
    height: ctx[14] || ctx[19],
    container_height: ctx[16] || ctx[19]
  };
  if (ctx[17] !== void 0) {
    sketch_1_props.brush_radius = ctx[17];
  }
  if (ctx[22] !== void 0) {
    sketch_1_props.brush_color = ctx[22];
  }
  sketch_1 = new Sketch({ props: sketch_1_props });
  binding_callbacks.push(() => bind(sketch_1, "brush_radius", sketch_1_brush_radius_binding_1));
  binding_callbacks.push(() => bind(sketch_1, "brush_color", sketch_1_brush_color_binding_1));
  ctx[44](sketch_1);
  sketch_1.$on("change", ctx[25]);
  return {
    c() {
      create_component(modifysketch.$$.fragment);
      t0 = space();
      if (if_block)
        if_block.c();
      t1 = space();
      create_component(sketch_1.$$.fragment);
    },
    m(target, anchor) {
      mount_component(modifysketch, target, anchor);
      insert(target, t0, anchor);
      if (if_block)
        if_block.m(target, anchor);
      insert(target, t1, anchor);
      mount_component(sketch_1, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[1] === "color-sketch") {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty[0] & 2) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block_8(ctx2);
          if_block.c();
          transition_in(if_block, 1);
          if_block.m(t1.parentNode, t1);
        }
      } else if (if_block) {
        group_outros();
        transition_out(if_block, 1, 1, () => {
          if_block = null;
        });
        check_outros();
      }
      const sketch_1_changes = {};
      if (dirty[0] & 1)
        sketch_1_changes.value = ctx2[0];
      if (dirty[0] & 4096)
        sketch_1_changes.mode = ctx2[12];
      if (dirty[0] & 1081344)
        sketch_1_changes.width = ctx2[15] || ctx2[20];
      if (dirty[0] & 540672)
        sketch_1_changes.height = ctx2[14] || ctx2[19];
      if (dirty[0] & 589824)
        sketch_1_changes.container_height = ctx2[16] || ctx2[19];
      if (!updating_brush_radius && dirty[0] & 131072) {
        updating_brush_radius = true;
        sketch_1_changes.brush_radius = ctx2[17];
        add_flush_callback(() => updating_brush_radius = false);
      }
      if (!updating_brush_color && dirty[0] & 4194304) {
        updating_brush_color = true;
        sketch_1_changes.brush_color = ctx2[22];
        add_flush_callback(() => updating_brush_color = false);
      }
      sketch_1.$set(sketch_1_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(modifysketch.$$.fragment, local);
      transition_in(if_block);
      transition_in(sketch_1.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(modifysketch.$$.fragment, local);
      transition_out(if_block);
      transition_out(sketch_1.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(modifysketch, detaching);
      if (detaching)
        detach(t0);
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(t1);
      ctx[44](null);
      destroy_component(sketch_1, detaching);
    }
  };
}
function create_if_block$2(ctx) {
  let upload;
  let updating_dragging;
  let current;
  function upload_dragging_binding(value) {
    ctx[37](value);
  }
  let upload_props = {
    filetype: "image/x-png,image/gif,image/jpeg",
    include_file_metadata: false,
    disable_click: !!ctx[0],
    $$slots: { default: [create_default_slot$1] },
    $$scope: { ctx }
  };
  if (ctx[11] !== void 0) {
    upload_props.dragging = ctx[11];
  }
  upload = new Upload({ props: upload_props });
  binding_callbacks.push(() => bind(upload, "dragging", upload_dragging_binding));
  upload.$on("load", ctx[23]);
  return {
    c() {
      create_component(upload.$$.fragment);
    },
    m(target, anchor) {
      mount_component(upload, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const upload_changes = {};
      if (dirty[0] & 1)
        upload_changes.disable_click = !!ctx2[0];
      if (dirty[0] & 8386035 | dirty[1] & 134217728) {
        upload_changes.$$scope = { dirty, ctx: ctx2 };
      }
      if (!updating_dragging && dirty[0] & 2048) {
        updating_dragging = true;
        upload_changes.dragging = ctx2[11];
        add_flush_callback(() => updating_dragging = false);
      }
      upload.$set(upload_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(upload.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(upload.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(upload, detaching);
    }
  };
}
function create_key_block_1(ctx) {
  let img;
  let img_src_value;
  let mounted;
  let dispose;
  return {
    c() {
      var _a;
      img = element("img");
      attr(img, "class", "absolute w-full h-full object-contain");
      if (!src_url_equal(img.src, img_src_value = ctx[21] || ((_a = ctx[0]) == null ? void 0 : _a.image) || ctx[0]))
        attr(img, "src", img_src_value);
      attr(img, "alt", "");
      toggle_class(img, "scale-x-[-1]", ctx[4] === "webcam" && ctx[10]);
    },
    m(target, anchor) {
      insert(target, img, anchor);
      ctx[49](img);
      if (!mounted) {
        dispose = listen(img, "load", ctx[26]);
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      var _a;
      if (dirty[0] & 2097153 && !src_url_equal(img.src, img_src_value = ctx2[21] || ((_a = ctx2[0]) == null ? void 0 : _a.image) || ctx2[0])) {
        attr(img, "src", img_src_value);
      }
      if (dirty[0] & 1040) {
        toggle_class(img, "scale-x-[-1]", ctx2[4] === "webcam" && ctx2[10]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(img);
      ctx[49](null);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block_14(ctx) {
  let sketch_1;
  let updating_brush_radius;
  let updating_brush_color;
  let t0;
  let modifysketch;
  let t1;
  let if_block_anchor;
  let current;
  function sketch_1_brush_radius_binding_2(value) {
    ctx[51](value);
  }
  function sketch_1_brush_color_binding_2(value) {
    ctx[52](value);
  }
  let sketch_1_props = {
    value: ctx[0],
    mode: ctx[12],
    width: ctx[15] || ctx[20],
    height: ctx[14] || ctx[19],
    container_height: ctx[16] || ctx[19],
    value_img: ctx[18],
    source: ctx[4]
  };
  if (ctx[17] !== void 0) {
    sketch_1_props.brush_radius = ctx[17];
  }
  if (ctx[22] !== void 0) {
    sketch_1_props.brush_color = ctx[22];
  }
  sketch_1 = new Sketch({ props: sketch_1_props });
  ctx[50](sketch_1);
  binding_callbacks.push(() => bind(sketch_1, "brush_radius", sketch_1_brush_radius_binding_2));
  binding_callbacks.push(() => bind(sketch_1, "brush_color", sketch_1_brush_color_binding_2));
  sketch_1.$on("change", ctx[25]);
  modifysketch = new ModifySketch({});
  modifysketch.$on("undo", ctx[53]);
  modifysketch.$on("clear", ctx[27]);
  let if_block = (ctx[1] === "color-sketch" || ctx[1] === "sketch") && create_if_block_15(ctx);
  return {
    c() {
      create_component(sketch_1.$$.fragment);
      t0 = space();
      create_component(modifysketch.$$.fragment);
      t1 = space();
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(sketch_1, target, anchor);
      insert(target, t0, anchor);
      mount_component(modifysketch, target, anchor);
      insert(target, t1, anchor);
      if (if_block)
        if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const sketch_1_changes = {};
      if (dirty[0] & 1)
        sketch_1_changes.value = ctx2[0];
      if (dirty[0] & 4096)
        sketch_1_changes.mode = ctx2[12];
      if (dirty[0] & 1081344)
        sketch_1_changes.width = ctx2[15] || ctx2[20];
      if (dirty[0] & 540672)
        sketch_1_changes.height = ctx2[14] || ctx2[19];
      if (dirty[0] & 589824)
        sketch_1_changes.container_height = ctx2[16] || ctx2[19];
      if (dirty[0] & 262144)
        sketch_1_changes.value_img = ctx2[18];
      if (dirty[0] & 16)
        sketch_1_changes.source = ctx2[4];
      if (!updating_brush_radius && dirty[0] & 131072) {
        updating_brush_radius = true;
        sketch_1_changes.brush_radius = ctx2[17];
        add_flush_callback(() => updating_brush_radius = false);
      }
      if (!updating_brush_color && dirty[0] & 4194304) {
        updating_brush_color = true;
        sketch_1_changes.brush_color = ctx2[22];
        add_flush_callback(() => updating_brush_color = false);
      }
      sketch_1.$set(sketch_1_changes);
      if (ctx2[1] === "color-sketch" || ctx2[1] === "sketch") {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty[0] & 2) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block_15(ctx2);
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
      transition_in(sketch_1.$$.fragment, local);
      transition_in(modifysketch.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(sketch_1.$$.fragment, local);
      transition_out(modifysketch.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      ctx[50](null);
      destroy_component(sketch_1, detaching);
      if (detaching)
        detach(t0);
      destroy_component(modifysketch, detaching);
      if (detaching)
        detach(t1);
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block_15(ctx) {
  let sketchsettings;
  let updating_brush_radius;
  let updating_brush_color;
  let current;
  function sketchsettings_brush_radius_binding_2(value) {
    ctx[54](value);
  }
  function sketchsettings_brush_color_binding_2(value) {
    ctx[55](value);
  }
  let sketchsettings_props = {
    container_height: ctx[16] || ctx[19],
    img_width: ctx[15] || ctx[20],
    img_height: ctx[14] || ctx[19],
    mode: ctx[12]
  };
  if (ctx[17] !== void 0) {
    sketchsettings_props.brush_radius = ctx[17];
  }
  if (ctx[22] !== void 0) {
    sketchsettings_props.brush_color = ctx[22];
  }
  sketchsettings = new SketchSettings({ props: sketchsettings_props });
  binding_callbacks.push(() => bind(sketchsettings, "brush_radius", sketchsettings_brush_radius_binding_2));
  binding_callbacks.push(() => bind(sketchsettings, "brush_color", sketchsettings_brush_color_binding_2));
  return {
    c() {
      create_component(sketchsettings.$$.fragment);
    },
    m(target, anchor) {
      mount_component(sketchsettings, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const sketchsettings_changes = {};
      if (dirty[0] & 589824)
        sketchsettings_changes.container_height = ctx2[16] || ctx2[19];
      if (dirty[0] & 1081344)
        sketchsettings_changes.img_width = ctx2[15] || ctx2[20];
      if (dirty[0] & 540672)
        sketchsettings_changes.img_height = ctx2[14] || ctx2[19];
      if (dirty[0] & 4096)
        sketchsettings_changes.mode = ctx2[12];
      if (!updating_brush_radius && dirty[0] & 131072) {
        updating_brush_radius = true;
        sketchsettings_changes.brush_radius = ctx2[17];
        add_flush_callback(() => updating_brush_radius = false);
      }
      if (!updating_brush_color && dirty[0] & 4194304) {
        updating_brush_color = true;
        sketchsettings_changes.brush_color = ctx2[22];
        add_flush_callback(() => updating_brush_color = false);
      }
      sketchsettings.$set(sketchsettings_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(sketchsettings.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(sketchsettings.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(sketchsettings, detaching);
    }
  };
}
function create_if_block_10(ctx) {
  let webcam;
  let current;
  webcam = new Webcam({
    props: {
      streaming: ctx[8],
      pending: ctx[9],
      mirror_webcam: ctx[10]
    }
  });
  webcam.$on("capture", ctx[45]);
  webcam.$on("stream", ctx[25]);
  webcam.$on("error", ctx[46]);
  return {
    c() {
      create_component(webcam.$$.fragment);
    },
    m(target, anchor) {
      mount_component(webcam, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const webcam_changes = {};
      if (dirty[0] & 256)
        webcam_changes.streaming = ctx2[8];
      if (dirty[0] & 512)
        webcam_changes.pending = ctx2[9];
      if (dirty[0] & 1024)
        webcam_changes.mirror_webcam = ctx2[10];
      webcam.$set(webcam_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(webcam.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(webcam.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(webcam, detaching);
    }
  };
}
function create_if_block_8(ctx) {
  let sketchsettings;
  let updating_brush_radius;
  let updating_brush_color;
  let current;
  function sketchsettings_brush_radius_binding_1(value) {
    ctx[40](value);
  }
  function sketchsettings_brush_color_binding_1(value) {
    ctx[41](value);
  }
  let sketchsettings_props = {
    container_height: ctx[16] || ctx[19],
    img_width: ctx[15] || ctx[20],
    img_height: ctx[14] || ctx[19]
  };
  if (ctx[17] !== void 0) {
    sketchsettings_props.brush_radius = ctx[17];
  }
  if (ctx[22] !== void 0) {
    sketchsettings_props.brush_color = ctx[22];
  }
  sketchsettings = new SketchSettings({ props: sketchsettings_props });
  binding_callbacks.push(() => bind(sketchsettings, "brush_radius", sketchsettings_brush_radius_binding_1));
  binding_callbacks.push(() => bind(sketchsettings, "brush_color", sketchsettings_brush_color_binding_1));
  return {
    c() {
      create_component(sketchsettings.$$.fragment);
    },
    m(target, anchor) {
      mount_component(sketchsettings, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const sketchsettings_changes = {};
      if (dirty[0] & 589824)
        sketchsettings_changes.container_height = ctx2[16] || ctx2[19];
      if (dirty[0] & 1081344)
        sketchsettings_changes.img_width = ctx2[15] || ctx2[20];
      if (dirty[0] & 540672)
        sketchsettings_changes.img_height = ctx2[14] || ctx2[19];
      if (!updating_brush_radius && dirty[0] & 131072) {
        updating_brush_radius = true;
        sketchsettings_changes.brush_radius = ctx2[17];
        add_flush_callback(() => updating_brush_radius = false);
      }
      if (!updating_brush_color && dirty[0] & 4194304) {
        updating_brush_color = true;
        sketchsettings_changes.brush_color = ctx2[22];
        add_flush_callback(() => updating_brush_color = false);
      }
      sketchsettings.$set(sketchsettings_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(sketchsettings.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(sketchsettings.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(sketchsettings, detaching);
    }
  };
}
function create_else_block$2(ctx) {
  let img;
  let img_src_value;
  return {
    c() {
      img = element("img");
      attr(img, "class", "w-full h-full object-contain");
      if (!src_url_equal(img.src, img_src_value = ctx[0].image || ctx[0]))
        attr(img, "src", img_src_value);
      attr(img, "alt", "");
      toggle_class(img, "scale-x-[-1]", ctx[4] === "webcam" && ctx[10]);
    },
    m(target, anchor) {
      insert(target, img, anchor);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 1 && !src_url_equal(img.src, img_src_value = ctx2[0].image || ctx2[0])) {
        attr(img, "src", img_src_value);
      }
      if (dirty[0] & 1040) {
        toggle_class(img, "scale-x-[-1]", ctx2[4] === "webcam" && ctx2[10]);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(img);
    }
  };
}
function create_if_block_4(ctx) {
  let previous_key = ctx[21];
  let t;
  let if_block_anchor;
  let current;
  let key_block = create_key_block(ctx);
  let if_block = ctx[15] > 0 && create_if_block_5(ctx);
  return {
    c() {
      key_block.c();
      t = space();
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      key_block.m(target, anchor);
      insert(target, t, anchor);
      if (if_block)
        if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (dirty[0] & 2097152 && safe_not_equal(previous_key, previous_key = ctx2[21])) {
        key_block.d(1);
        key_block = create_key_block(ctx2);
        key_block.c();
        key_block.m(t.parentNode, t);
      } else {
        key_block.p(ctx2, dirty);
      }
      if (ctx2[15] > 0) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty[0] & 32768) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block_5(ctx2);
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
      key_block.d(detaching);
      if (detaching)
        detach(t);
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block_3(ctx) {
  let modifyupload;
  let t;
  let img;
  let img_src_value;
  let current;
  modifyupload = new ModifyUpload({ props: { editable: true } });
  modifyupload.$on("edit", ctx[29]);
  modifyupload.$on("clear", ctx[24]);
  return {
    c() {
      create_component(modifyupload.$$.fragment);
      t = space();
      img = element("img");
      attr(img, "class", "w-full h-full object-contain");
      if (!src_url_equal(img.src, img_src_value = ctx[0]))
        attr(img, "src", img_src_value);
      attr(img, "alt", "");
      toggle_class(img, "scale-x-[-1]", ctx[4] === "webcam" && ctx[10]);
    },
    m(target, anchor) {
      mount_component(modifyupload, target, anchor);
      insert(target, t, anchor);
      insert(target, img, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (!current || dirty[0] & 1 && !src_url_equal(img.src, img_src_value = ctx2[0])) {
        attr(img, "src", img_src_value);
      }
      if (dirty[0] & 1040) {
        toggle_class(img, "scale-x-[-1]", ctx2[4] === "webcam" && ctx2[10]);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(modifyupload.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(modifyupload.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(modifyupload, detaching);
      if (detaching)
        detach(t);
      if (detaching)
        detach(img);
    }
  };
}
function create_if_block_2(ctx) {
  let cropper;
  let t;
  let modifyupload;
  let current;
  cropper = new Cropper_1({ props: { image: ctx[0] } });
  cropper.$on("crop", ctx[25]);
  modifyupload = new ModifyUpload({});
  modifyupload.$on("clear", ctx[28]);
  return {
    c() {
      create_component(cropper.$$.fragment);
      t = space();
      create_component(modifyupload.$$.fragment);
    },
    m(target, anchor) {
      mount_component(cropper, target, anchor);
      insert(target, t, anchor);
      mount_component(modifyupload, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const cropper_changes = {};
      if (dirty[0] & 1)
        cropper_changes.image = ctx2[0];
      cropper.$set(cropper_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(cropper.$$.fragment, local);
      transition_in(modifyupload.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(cropper.$$.fragment, local);
      transition_out(modifyupload.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(cropper, detaching);
      if (detaching)
        detach(t);
      destroy_component(modifyupload, detaching);
    }
  };
}
function create_if_block_1(ctx) {
  let div;
  let t0;
  let t1;
  let span;
  let t2;
  let t3;
  let t4;
  let t5;
  let t6;
  return {
    c() {
      div = element("div");
      t0 = text(ctx[5]);
      t1 = space();
      span = element("span");
      t2 = text("- ");
      t3 = text(ctx[6]);
      t4 = text(" -");
      t5 = space();
      t6 = text(ctx[7]);
      attr(span, "class", "text-gray-300");
      attr(div, "class", "flex flex-col");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t0);
      append(div, t1);
      append(div, span);
      append(span, t2);
      append(span, t3);
      append(span, t4);
      append(div, t5);
      append(div, t6);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 32)
        set_data(t0, ctx2[5]);
      if (dirty[0] & 64)
        set_data(t3, ctx2[6]);
      if (dirty[0] & 128)
        set_data(t6, ctx2[7]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_key_block(ctx) {
  let img;
  let img_src_value;
  let mounted;
  let dispose;
  return {
    c() {
      var _a;
      img = element("img");
      attr(img, "class", "absolute w-full h-full object-contain");
      if (!src_url_equal(img.src, img_src_value = ctx[21] || ((_a = ctx[0]) == null ? void 0 : _a.image) || ctx[0]))
        attr(img, "src", img_src_value);
      attr(img, "alt", "");
      toggle_class(img, "scale-x-[-1]", ctx[4] === "webcam" && ctx[10]);
    },
    m(target, anchor) {
      insert(target, img, anchor);
      ctx[30](img);
      if (!mounted) {
        dispose = listen(img, "load", ctx[26]);
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      var _a;
      if (dirty[0] & 2097153 && !src_url_equal(img.src, img_src_value = ctx2[21] || ((_a = ctx2[0]) == null ? void 0 : _a.image) || ctx2[0])) {
        attr(img, "src", img_src_value);
      }
      if (dirty[0] & 1040) {
        toggle_class(img, "scale-x-[-1]", ctx2[4] === "webcam" && ctx2[10]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(img);
      ctx[30](null);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block_5(ctx) {
  let sketch_1;
  let updating_brush_radius;
  let updating_brush_color;
  let t0;
  let modifysketch;
  let t1;
  let if_block_anchor;
  let current;
  function sketch_1_brush_radius_binding(value) {
    ctx[32](value);
  }
  function sketch_1_brush_color_binding(value) {
    ctx[33](value);
  }
  let sketch_1_props = {
    value: ctx[0],
    mode: ctx[12],
    width: ctx[15] || ctx[20],
    height: ctx[14] || ctx[19],
    container_height: ctx[16] || ctx[19],
    value_img: ctx[18],
    source: ctx[4]
  };
  if (ctx[17] !== void 0) {
    sketch_1_props.brush_radius = ctx[17];
  }
  if (ctx[22] !== void 0) {
    sketch_1_props.brush_color = ctx[22];
  }
  sketch_1 = new Sketch({ props: sketch_1_props });
  ctx[31](sketch_1);
  binding_callbacks.push(() => bind(sketch_1, "brush_radius", sketch_1_brush_radius_binding));
  binding_callbacks.push(() => bind(sketch_1, "brush_color", sketch_1_brush_color_binding));
  sketch_1.$on("change", ctx[25]);
  modifysketch = new ModifySketch({});
  modifysketch.$on("undo", ctx[34]);
  modifysketch.$on("clear", ctx[27]);
  let if_block = (ctx[1] === "color-sketch" || ctx[1] === "sketch") && create_if_block_6(ctx);
  return {
    c() {
      create_component(sketch_1.$$.fragment);
      t0 = space();
      create_component(modifysketch.$$.fragment);
      t1 = space();
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(sketch_1, target, anchor);
      insert(target, t0, anchor);
      mount_component(modifysketch, target, anchor);
      insert(target, t1, anchor);
      if (if_block)
        if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const sketch_1_changes = {};
      if (dirty[0] & 1)
        sketch_1_changes.value = ctx2[0];
      if (dirty[0] & 4096)
        sketch_1_changes.mode = ctx2[12];
      if (dirty[0] & 1081344)
        sketch_1_changes.width = ctx2[15] || ctx2[20];
      if (dirty[0] & 540672)
        sketch_1_changes.height = ctx2[14] || ctx2[19];
      if (dirty[0] & 589824)
        sketch_1_changes.container_height = ctx2[16] || ctx2[19];
      if (dirty[0] & 262144)
        sketch_1_changes.value_img = ctx2[18];
      if (dirty[0] & 16)
        sketch_1_changes.source = ctx2[4];
      if (!updating_brush_radius && dirty[0] & 131072) {
        updating_brush_radius = true;
        sketch_1_changes.brush_radius = ctx2[17];
        add_flush_callback(() => updating_brush_radius = false);
      }
      if (!updating_brush_color && dirty[0] & 4194304) {
        updating_brush_color = true;
        sketch_1_changes.brush_color = ctx2[22];
        add_flush_callback(() => updating_brush_color = false);
      }
      sketch_1.$set(sketch_1_changes);
      if (ctx2[1] === "color-sketch" || ctx2[1] === "sketch") {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty[0] & 2) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block_6(ctx2);
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
      transition_in(sketch_1.$$.fragment, local);
      transition_in(modifysketch.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(sketch_1.$$.fragment, local);
      transition_out(modifysketch.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      ctx[31](null);
      destroy_component(sketch_1, detaching);
      if (detaching)
        detach(t0);
      destroy_component(modifysketch, detaching);
      if (detaching)
        detach(t1);
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block_6(ctx) {
  let sketchsettings;
  let updating_brush_radius;
  let updating_brush_color;
  let current;
  function sketchsettings_brush_radius_binding(value) {
    ctx[35](value);
  }
  function sketchsettings_brush_color_binding(value) {
    ctx[36](value);
  }
  let sketchsettings_props = {
    container_height: ctx[16] || ctx[19],
    img_width: ctx[15] || ctx[20],
    img_height: ctx[14] || ctx[19],
    mode: ctx[12]
  };
  if (ctx[17] !== void 0) {
    sketchsettings_props.brush_radius = ctx[17];
  }
  if (ctx[22] !== void 0) {
    sketchsettings_props.brush_color = ctx[22];
  }
  sketchsettings = new SketchSettings({ props: sketchsettings_props });
  binding_callbacks.push(() => bind(sketchsettings, "brush_radius", sketchsettings_brush_radius_binding));
  binding_callbacks.push(() => bind(sketchsettings, "brush_color", sketchsettings_brush_color_binding));
  return {
    c() {
      create_component(sketchsettings.$$.fragment);
    },
    m(target, anchor) {
      mount_component(sketchsettings, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const sketchsettings_changes = {};
      if (dirty[0] & 589824)
        sketchsettings_changes.container_height = ctx2[16] || ctx2[19];
      if (dirty[0] & 1081344)
        sketchsettings_changes.img_width = ctx2[15] || ctx2[20];
      if (dirty[0] & 540672)
        sketchsettings_changes.img_height = ctx2[14] || ctx2[19];
      if (dirty[0] & 4096)
        sketchsettings_changes.mode = ctx2[12];
      if (!updating_brush_radius && dirty[0] & 131072) {
        updating_brush_radius = true;
        sketchsettings_changes.brush_radius = ctx2[17];
        add_flush_callback(() => updating_brush_radius = false);
      }
      if (!updating_brush_color && dirty[0] & 4194304) {
        updating_brush_color = true;
        sketchsettings_changes.brush_color = ctx2[22];
        add_flush_callback(() => updating_brush_color = false);
      }
      sketchsettings.$set(sketchsettings_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(sketchsettings.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(sketchsettings.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(sketchsettings, detaching);
    }
  };
}
function create_default_slot$1(ctx) {
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  const if_block_creators = [
    create_if_block_1,
    create_if_block_2,
    create_if_block_3,
    create_if_block_4,
    create_else_block$2
  ];
  const if_blocks = [];
  function select_block_type_1(ctx2, dirty) {
    if (ctx2[0] === null && !ctx2[21] || ctx2[8])
      return 0;
    if (ctx2[1] === "select")
      return 1;
    if (ctx2[1] === "editor")
      return 2;
    if ((ctx2[1] === "sketch" || ctx2[1] === "color-sketch") && (ctx2[0] !== null || ctx2[21]))
      return 3;
    return 4;
  }
  current_block_type_index = select_block_type_1(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type_1(ctx2);
      if (current_block_type_index === previous_block_index) {
        if_blocks[current_block_type_index].p(ctx2, dirty);
      } else {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        } else {
          if_block.p(ctx2, dirty);
        }
        transition_in(if_block, 1);
        if_block.m(if_block_anchor.parentNode, if_block_anchor);
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
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_fragment$2(ctx) {
  let blocklabel;
  let t;
  let div;
  let current_block_type_index;
  let if_block;
  let div_resize_listener;
  let current;
  blocklabel = new BlockLabel({
    props: {
      show_label: ctx[3],
      Icon: ctx[4] === "canvas" ? Sketch$1 : Image,
      label: ctx[2] || (ctx[4] === "canvas" ? "Sketch" : "Image")
    }
  });
  const if_block_creators = [
    create_if_block$2,
    create_if_block_7,
    create_if_block_9,
    create_if_block_11,
    create_if_block_12,
    create_if_block_13,
    create_else_block_1
  ];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[4] === "upload")
      return 0;
    if (ctx2[4] === "canvas")
      return 1;
    if (ctx2[0] === null && !ctx2[21] || ctx2[8])
      return 2;
    if (ctx2[1] === "select")
      return 3;
    if (ctx2[1] === "editor")
      return 4;
    if ((ctx2[1] === "sketch" || ctx2[1] === "color-sketch") && (ctx2[0] !== null || ctx2[21]))
      return 5;
    return 6;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(blocklabel.$$.fragment);
      t = space();
      div = element("div");
      if_block.c();
      attr(div, "data-testid", "image");
      add_render_callback(() => ctx[56].call(div));
      toggle_class(div, "bg-gray-200", ctx[0]);
      toggle_class(div, "h-60", ctx[4] !== "webcam" || ctx[1] === "sketch" || ctx[1] === "color-sketch");
    },
    m(target, anchor) {
      mount_component(blocklabel, target, anchor);
      insert(target, t, anchor);
      insert(target, div, anchor);
      if_blocks[current_block_type_index].m(div, null);
      div_resize_listener = add_resize_listener(div, ctx[56].bind(div));
      current = true;
    },
    p(ctx2, dirty) {
      const blocklabel_changes = {};
      if (dirty[0] & 8)
        blocklabel_changes.show_label = ctx2[3];
      if (dirty[0] & 16)
        blocklabel_changes.Icon = ctx2[4] === "canvas" ? Sketch$1 : Image;
      if (dirty[0] & 20)
        blocklabel_changes.label = ctx2[2] || (ctx2[4] === "canvas" ? "Sketch" : "Image");
      blocklabel.$set(blocklabel_changes);
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type(ctx2);
      if (current_block_type_index === previous_block_index) {
        if_blocks[current_block_type_index].p(ctx2, dirty);
      } else {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        } else {
          if_block.p(ctx2, dirty);
        }
        transition_in(if_block, 1);
        if_block.m(div, null);
      }
      if (dirty[0] & 1) {
        toggle_class(div, "bg-gray-200", ctx2[0]);
      }
      if (dirty[0] & 18) {
        toggle_class(div, "h-60", ctx2[4] !== "webcam" || ctx2[1] === "sketch" || ctx2[1] === "color-sketch");
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(blocklabel.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(blocklabel.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(blocklabel, detaching);
      if (detaching)
        detach(t);
      if (detaching)
        detach(div);
      if_blocks[current_block_type_index].d();
      div_resize_listener();
    }
  };
}
function instance$2($$self, $$props, $$invalidate) {
  let brush_color;
  let { value } = $$props;
  let { label = void 0 } = $$props;
  let { show_label } = $$props;
  let { source = "upload" } = $$props;
  let { tool = "editor" } = $$props;
  let { drop_text = "Drop an image file" } = $$props;
  let { or_text = "or" } = $$props;
  let { upload_text = "click to upload" } = $$props;
  let { streaming = false } = $$props;
  let { pending = false } = $$props;
  let { mirror_webcam } = $$props;
  let sketch;
  if (value && (source === "upload" || source === "webcam") && tool === "sketch") {
    value = { image: value, mask: null };
  }
  function handle_upload({ detail }) {
    if (tool === "color-sketch") {
      $$invalidate(21, static_image = detail);
    } else {
      $$invalidate(0, value = (source === "upload" || source === "webcam") && tool === "sketch" ? { image: detail, mask: null } : detail);
    }
    dispatch("upload", detail);
  }
  function handle_clear({ detail }) {
    $$invalidate(0, value = null);
    $$invalidate(21, static_image = void 0);
    dispatch("clear");
  }
  async function handle_save({ detail }, initial) {
    if (mode === "mask") {
      if (source === "webcam" && initial) {
        $$invalidate(0, value = { image: detail, mask: null });
      } else {
        $$invalidate(0, value = {
          image: typeof value === "string" ? value : (value == null ? void 0 : value.image) || null,
          mask: detail
        });
      }
    } else if ((source === "upload" || source === "webcam") && tool === "sketch") {
      $$invalidate(0, value = { image: detail, mask: null });
    } else {
      $$invalidate(0, value = detail);
    }
    await tick();
    dispatch(streaming ? "stream" : "edit");
  }
  const dispatch = createEventDispatcher();
  let dragging = false;
  function handle_image_load(event) {
    const element2 = event.composedPath()[0];
    $$invalidate(15, img_width = element2.naturalWidth);
    $$invalidate(14, img_height = element2.naturalHeight);
    $$invalidate(16, container_height = element2.getBoundingClientRect().height);
  }
  async function handle_mask_clear() {
    sketch.clear();
    await tick();
    $$invalidate(0, value = null);
    $$invalidate(21, static_image = void 0);
  }
  let img_height = 0;
  let img_width = 0;
  let container_height = 0;
  let brush_radius = 20;
  let mode;
  let value_img;
  let max_height;
  let max_width;
  let static_image = void 0;
  const clear_handler = (e) => (handle_clear(e), $$invalidate(1, tool = "editor"));
  const edit_handler = () => $$invalidate(1, tool = "select");
  function img_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      value_img = $$value;
      $$invalidate(18, value_img);
    });
  }
  function sketch_1_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      sketch = $$value;
      $$invalidate(13, sketch);
    });
  }
  function sketch_1_brush_radius_binding(value2) {
    brush_radius = value2;
    $$invalidate(17, brush_radius);
  }
  function sketch_1_brush_color_binding(value2) {
    brush_color = value2;
    $$invalidate(22, brush_color), $$invalidate(12, mode), $$invalidate(4, source), $$invalidate(1, tool);
  }
  const undo_handler = () => sketch.undo();
  function sketchsettings_brush_radius_binding(value2) {
    brush_radius = value2;
    $$invalidate(17, brush_radius);
  }
  function sketchsettings_brush_color_binding(value2) {
    brush_color = value2;
    $$invalidate(22, brush_color), $$invalidate(12, mode), $$invalidate(4, source), $$invalidate(1, tool);
  }
  function upload_dragging_binding(value2) {
    dragging = value2;
    $$invalidate(11, dragging);
  }
  const undo_handler_1 = () => sketch.undo();
  const clear_handler_1 = () => sketch.clear();
  function sketchsettings_brush_radius_binding_1(value2) {
    brush_radius = value2;
    $$invalidate(17, brush_radius);
  }
  function sketchsettings_brush_color_binding_1(value2) {
    brush_color = value2;
    $$invalidate(22, brush_color), $$invalidate(12, mode), $$invalidate(4, source), $$invalidate(1, tool);
  }
  function sketch_1_brush_radius_binding_1(value2) {
    brush_radius = value2;
    $$invalidate(17, brush_radius);
  }
  function sketch_1_brush_color_binding_1(value2) {
    brush_color = value2;
    $$invalidate(22, brush_color), $$invalidate(12, mode), $$invalidate(4, source), $$invalidate(1, tool);
  }
  function sketch_1_binding_1($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      sketch = $$value;
      $$invalidate(13, sketch);
    });
  }
  const capture_handler = (e) => tool === "color-sketch" ? handle_upload(e) : handle_save(e, true);
  function error_handler(event) {
    bubble.call(this, $$self, event);
  }
  const clear_handler_2 = (e) => (handle_clear(e), $$invalidate(1, tool = "editor"));
  const edit_handler_1 = () => $$invalidate(1, tool = "select");
  function img_binding_1($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      value_img = $$value;
      $$invalidate(18, value_img);
    });
  }
  function sketch_1_binding_2($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      sketch = $$value;
      $$invalidate(13, sketch);
    });
  }
  function sketch_1_brush_radius_binding_2(value2) {
    brush_radius = value2;
    $$invalidate(17, brush_radius);
  }
  function sketch_1_brush_color_binding_2(value2) {
    brush_color = value2;
    $$invalidate(22, brush_color), $$invalidate(12, mode), $$invalidate(4, source), $$invalidate(1, tool);
  }
  const undo_handler_2 = () => sketch.undo();
  function sketchsettings_brush_radius_binding_2(value2) {
    brush_radius = value2;
    $$invalidate(17, brush_radius);
  }
  function sketchsettings_brush_color_binding_2(value2) {
    brush_color = value2;
    $$invalidate(22, brush_color), $$invalidate(12, mode), $$invalidate(4, source), $$invalidate(1, tool);
  }
  function div_elementresize_handler() {
    max_height = this.offsetHeight;
    max_width = this.offsetWidth;
    $$invalidate(19, max_height);
    $$invalidate(20, max_width);
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("label" in $$props2)
      $$invalidate(2, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(3, show_label = $$props2.show_label);
    if ("source" in $$props2)
      $$invalidate(4, source = $$props2.source);
    if ("tool" in $$props2)
      $$invalidate(1, tool = $$props2.tool);
    if ("drop_text" in $$props2)
      $$invalidate(5, drop_text = $$props2.drop_text);
    if ("or_text" in $$props2)
      $$invalidate(6, or_text = $$props2.or_text);
    if ("upload_text" in $$props2)
      $$invalidate(7, upload_text = $$props2.upload_text);
    if ("streaming" in $$props2)
      $$invalidate(8, streaming = $$props2.streaming);
    if ("pending" in $$props2)
      $$invalidate(9, pending = $$props2.pending);
    if ("mirror_webcam" in $$props2)
      $$invalidate(10, mirror_webcam = $$props2.mirror_webcam);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty[0] & 1) {
      dispatch("change", value);
    }
    if ($$self.$$.dirty[0] & 2048) {
      dispatch("drag", dragging);
    }
    if ($$self.$$.dirty[0] & 18) {
      {
        if (source === "canvas" && tool === "sketch") {
          $$invalidate(12, mode = "bw-sketch");
        } else if (tool === "color-sketch") {
          $$invalidate(12, mode = "color-sketch");
        } else if ((source === "upload" || source === "webcam") && tool === "sketch") {
          $$invalidate(12, mode = "mask");
        } else {
          $$invalidate(12, mode = "editor");
        }
      }
    }
    if ($$self.$$.dirty[0] & 4096) {
      $$invalidate(22, brush_color = mode == "mask" ? "#000000" : "#000");
    }
    if ($$self.$$.dirty[0] & 1) {
      {
        if (value === null || value.image === null && value.mask === null) {
          $$invalidate(21, static_image = void 0);
        }
      }
    }
  };
  return [
    value,
    tool,
    label,
    show_label,
    source,
    drop_text,
    or_text,
    upload_text,
    streaming,
    pending,
    mirror_webcam,
    dragging,
    mode,
    sketch,
    img_height,
    img_width,
    container_height,
    brush_radius,
    value_img,
    max_height,
    max_width,
    static_image,
    brush_color,
    handle_upload,
    handle_clear,
    handle_save,
    handle_image_load,
    handle_mask_clear,
    clear_handler,
    edit_handler,
    img_binding,
    sketch_1_binding,
    sketch_1_brush_radius_binding,
    sketch_1_brush_color_binding,
    undo_handler,
    sketchsettings_brush_radius_binding,
    sketchsettings_brush_color_binding,
    upload_dragging_binding,
    undo_handler_1,
    clear_handler_1,
    sketchsettings_brush_radius_binding_1,
    sketchsettings_brush_color_binding_1,
    sketch_1_brush_radius_binding_1,
    sketch_1_brush_color_binding_1,
    sketch_1_binding_1,
    capture_handler,
    error_handler,
    clear_handler_2,
    edit_handler_1,
    img_binding_1,
    sketch_1_binding_2,
    sketch_1_brush_radius_binding_2,
    sketch_1_brush_color_binding_2,
    undo_handler_2,
    sketchsettings_brush_radius_binding_2,
    sketchsettings_brush_color_binding_2,
    div_elementresize_handler
  ];
}
class Image_1$2 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$2, create_fragment$2, safe_not_equal, {
      value: 0,
      label: 2,
      show_label: 3,
      source: 4,
      tool: 1,
      drop_text: 5,
      or_text: 6,
      upload_text: 7,
      streaming: 8,
      pending: 9,
      mirror_webcam: 10
    }, null, [-1, -1]);
  }
}
function create_else_block$1(ctx) {
  let img;
  let img_src_value;
  return {
    c() {
      img = element("img");
      attr(img, "class", "w-full h-full object-contain");
      if (!src_url_equal(img.src, img_src_value = ctx[0]))
        attr(img, "src", img_src_value);
      attr(img, "alt", "");
    },
    m(target, anchor) {
      insert(target, img, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && !src_url_equal(img.src, img_src_value = ctx2[0])) {
        attr(img, "src", img_src_value);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(img);
    }
  };
}
function create_if_block$1(ctx) {
  let div1;
  let div0;
  let image;
  let current;
  image = new Image({});
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      create_component(image.$$.fragment);
      attr(div0, "class", "h-5 dark:text-white opacity-50");
      attr(div1, "class", "h-full min-h-[15rem] flex justify-center items-center");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      mount_component(image, div0, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(image.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(image.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(image);
    }
  };
}
function create_fragment$1(ctx) {
  let blocklabel;
  let t;
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  blocklabel = new BlockLabel({
    props: {
      show_label: ctx[2],
      Icon: Image,
      label: ctx[1] || "Image"
    }
  });
  const if_block_creators = [create_if_block$1, create_else_block$1];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[0] === null)
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(blocklabel.$$.fragment);
      t = space();
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(blocklabel, target, anchor);
      insert(target, t, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocklabel_changes = {};
      if (dirty & 4)
        blocklabel_changes.show_label = ctx2[2];
      if (dirty & 2)
        blocklabel_changes.label = ctx2[1] || "Image";
      blocklabel.$set(blocklabel_changes);
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type(ctx2);
      if (current_block_type_index === previous_block_index) {
        if_blocks[current_block_type_index].p(ctx2, dirty);
      } else {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        } else {
          if_block.p(ctx2, dirty);
        }
        transition_in(if_block, 1);
        if_block.m(if_block_anchor.parentNode, if_block_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(blocklabel.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(blocklabel.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(blocklabel, detaching);
      if (detaching)
        detach(t);
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { value } = $$props;
  let { label = void 0 } = $$props;
  let { show_label } = $$props;
  const dispatch = createEventDispatcher();
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(2, show_label = $$props2.show_label);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      value && dispatch("change", value);
    }
  };
  return [value, label, show_label];
}
class StaticImage extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { value: 0, label: 1, show_label: 2 });
  }
}
function create_else_block(ctx) {
  let image;
  let updating_value;
  let current;
  function image_value_binding(value) {
    ctx[15](value);
  }
  let image_props = {
    source: ctx[4],
    tool: ctx[5],
    label: ctx[6],
    show_label: ctx[7],
    pending: ctx[9],
    streaming: ctx[8],
    drop_text: ctx[14]("interface.drop_image"),
    or_text: ctx[14]("or"),
    upload_text: ctx[14]("interface.click_to_upload"),
    mirror_webcam: ctx[11]
  };
  if (ctx[0] !== void 0) {
    image_props.value = ctx[0];
  }
  image = new Image_1$2({ props: image_props });
  binding_callbacks.push(() => bind(image, "value", image_value_binding));
  image.$on("edit", ctx[16]);
  image.$on("clear", ctx[17]);
  image.$on("change", ctx[18]);
  image.$on("stream", ctx[19]);
  image.$on("drag", ctx[20]);
  image.$on("upload", ctx[21]);
  image.$on("error", ctx[22]);
  return {
    c() {
      create_component(image.$$.fragment);
    },
    m(target, anchor) {
      mount_component(image, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const image_changes = {};
      if (dirty & 16)
        image_changes.source = ctx2[4];
      if (dirty & 32)
        image_changes.tool = ctx2[5];
      if (dirty & 64)
        image_changes.label = ctx2[6];
      if (dirty & 128)
        image_changes.show_label = ctx2[7];
      if (dirty & 512)
        image_changes.pending = ctx2[9];
      if (dirty & 256)
        image_changes.streaming = ctx2[8];
      if (dirty & 16384)
        image_changes.drop_text = ctx2[14]("interface.drop_image");
      if (dirty & 16384)
        image_changes.or_text = ctx2[14]("or");
      if (dirty & 16384)
        image_changes.upload_text = ctx2[14]("interface.click_to_upload");
      if (dirty & 2048)
        image_changes.mirror_webcam = ctx2[11];
      if (!updating_value && dirty & 1) {
        updating_value = true;
        image_changes.value = ctx2[0];
        add_flush_callback(() => updating_value = false);
      }
      image.$set(image_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(image.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(image.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(image, detaching);
    }
  };
}
function create_if_block(ctx) {
  let staticimage;
  let current;
  staticimage = new StaticImage({
    props: {
      value: ctx[0],
      label: ctx[6],
      show_label: ctx[7]
    }
  });
  return {
    c() {
      create_component(staticimage.$$.fragment);
    },
    m(target, anchor) {
      mount_component(staticimage, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const staticimage_changes = {};
      if (dirty & 1)
        staticimage_changes.value = ctx2[0];
      if (dirty & 64)
        staticimage_changes.label = ctx2[6];
      if (dirty & 128)
        staticimage_changes.show_label = ctx2[7];
      staticimage.$set(staticimage_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(staticimage.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(staticimage.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(staticimage, detaching);
    }
  };
}
function create_default_slot(ctx) {
  let statustracker;
  let t;
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  const statustracker_spread_levels = [ctx[1]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  const if_block_creators = [create_if_block, create_else_block];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[12] === "static")
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t = space();
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 2 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[1])]) : {};
      statustracker.$set(statustracker_changes);
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type(ctx2);
      if (current_block_type_index === previous_block_index) {
        if_blocks[current_block_type_index].p(ctx2, dirty);
      } else {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        } else {
          if_block.p(ctx2, dirty);
        }
        transition_in(if_block, 1);
        if_block.m(if_block_anchor.parentNode, if_block_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      visible: ctx[3],
      variant: ctx[12] === "dynamic" && ctx[0] === null && ctx[4] === "upload" ? "dashed" : "solid",
      color: ctx[13] ? "green" : "grey",
      padding: false,
      elem_id: ctx[2],
      style: {
        height: ctx[10].height,
        width: ctx[10].width
      },
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
      if (dirty & 4113)
        block_changes.variant = ctx2[12] === "dynamic" && ctx2[0] === null && ctx2[4] === "upload" ? "dashed" : "solid";
      if (dirty & 8192)
        block_changes.color = ctx2[13] ? "green" : "grey";
      if (dirty & 4)
        block_changes.elem_id = ctx2[2];
      if (dirty & 1024)
        block_changes.style = {
          height: ctx2[10].height,
          width: ctx2[10].width
        };
      if (dirty & 16808947) {
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
  let $_;
  component_subscribe($$self, X, ($$value) => $$invalidate(14, $_ = $$value));
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = null } = $$props;
  let { source = "upload" } = $$props;
  let { tool = "editor" } = $$props;
  let { label } = $$props;
  let { show_label } = $$props;
  let { streaming } = $$props;
  let { pending } = $$props;
  let { style = {} } = $$props;
  let { mirror_webcam } = $$props;
  let { loading_status } = $$props;
  let { mode } = $$props;
  const dispatch = createEventDispatcher();
  let dragging;
  function image_value_binding(value$1) {
    value = value$1;
    $$invalidate(0, value);
  }
  function edit_handler(event) {
    bubble.call(this, $$self, event);
  }
  function clear_handler(event) {
    bubble.call(this, $$self, event);
  }
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  function stream_handler(event) {
    bubble.call(this, $$self, event);
  }
  const drag_handler = ({ detail }) => $$invalidate(13, dragging = detail);
  function upload_handler(event) {
    bubble.call(this, $$self, event);
  }
  const error_handler = ({ detail }) => {
    $$invalidate(1, loading_status = loading_status || {});
    $$invalidate(1, loading_status.status = "error", loading_status);
    $$invalidate(1, loading_status.message = detail, loading_status);
  };
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(2, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(3, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("source" in $$props2)
      $$invalidate(4, source = $$props2.source);
    if ("tool" in $$props2)
      $$invalidate(5, tool = $$props2.tool);
    if ("label" in $$props2)
      $$invalidate(6, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(7, show_label = $$props2.show_label);
    if ("streaming" in $$props2)
      $$invalidate(8, streaming = $$props2.streaming);
    if ("pending" in $$props2)
      $$invalidate(9, pending = $$props2.pending);
    if ("style" in $$props2)
      $$invalidate(10, style = $$props2.style);
    if ("mirror_webcam" in $$props2)
      $$invalidate(11, mirror_webcam = $$props2.mirror_webcam);
    if ("loading_status" in $$props2)
      $$invalidate(1, loading_status = $$props2.loading_status);
    if ("mode" in $$props2)
      $$invalidate(12, mode = $$props2.mode);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      $$invalidate(0, value = !value ? null : value);
    }
    if ($$self.$$.dirty & 1) {
      dispatch("change");
    }
  };
  return [
    value,
    loading_status,
    elem_id,
    visible,
    source,
    tool,
    label,
    show_label,
    streaming,
    pending,
    style,
    mirror_webcam,
    mode,
    dragging,
    $_,
    image_value_binding,
    edit_handler,
    clear_handler,
    change_handler,
    stream_handler,
    drag_handler,
    upload_handler,
    error_handler
  ];
}
class Image_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 2,
      visible: 3,
      value: 0,
      source: 4,
      tool: 5,
      label: 6,
      show_label: 7,
      streaming: 8,
      pending: 9,
      style: 10,
      mirror_webcam: 11,
      loading_status: 1,
      mode: 12
    });
  }
}
var Image_1$1 = Image_1;
const modes = ["static", "dynamic"];
const document = (config) => ({
  type: "string",
  description: "image data as base64 string",
  example_data: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=="
});
export { Image_1$1 as Component, document, modes };
//# sourceMappingURL=index21.js.map
