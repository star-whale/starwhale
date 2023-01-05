import { c as createBroker } from "./module2.js";
const wrap = createBroker({
  characterize: ({ call }) => {
    return () => call("characterize");
  },
  encode: ({ call }) => {
    return (recordingId, timeslice) => {
      return call("encode", { recordingId, timeslice });
    };
  },
  record: ({ call }) => {
    return async (recordingId, sampleRate, typedArrays) => {
      await call("record", { recordingId, sampleRate, typedArrays }, typedArrays.map(({ buffer }) => buffer));
    };
  }
});
const load = (url2) => {
  const worker2 = new Worker(url2);
  return wrap(worker2);
};
const worker = `(()=>{var e={775:function(e,t,r){!function(e,t,r,n){"use strict";function o(e){return e&&"object"==typeof e&&"default"in e?e:{default:e}}var s=o(t),a=o(r),i=o(n),u=function(e,t){return void 0===t?e:t.reduce((function(e,t){if("capitalize"===t){var r=e.charAt(0).toUpperCase(),n=e.slice(1);return"".concat(r).concat(n)}return"dashify"===t?a.default(e):"prependIndefiniteArticle"===t?"".concat(i.default(e)," ").concat(e):e}),e)},c=function(e){var t=e.name+e.modifiers.map((function(e){return"\\\\.".concat(e,"\\\\(\\\\)")})).join("");return new RegExp("\\\\$\\\\{".concat(t,"}"),"g")},l=function(e,t){for(var r=/\\\${([^.}]+)((\\.[^(]+\\(\\))*)}/g,n=[],o=r.exec(e);null!==o;){var a={modifiers:[],name:o[1]};if(void 0!==o[3])for(var i=/\\.[^(]+\\(\\)/g,l=i.exec(o[2]);null!==l;)a.modifiers.push(l[0].slice(1,-2)),l=i.exec(o[2]);n.push(a),o=r.exec(e)}var d=n.reduce((function(e,r){return e.map((function(e){return"string"==typeof e?e.split(c(r)).reduce((function(e,n,o){return 0===o?[n]:r.name in t?[].concat(s.default(e),[u(t[r.name],r.modifiers),n]):[].concat(s.default(e),[function(e){return u(e[r.name],r.modifiers)},n])}),[]):[e]})).reduce((function(e,t){return[].concat(s.default(e),s.default(t))}),[])}),[e]);return function(e){return d.reduce((function(t,r){return[].concat(s.default(t),"string"==typeof r?[r]:[r(e)])}),[]).join("")}},d=function(e){var t=arguments.length>1&&void 0!==arguments[1]?arguments[1]:{},r=void 0===e.code?void 0:l(e.code,t),n=void 0===e.message?void 0:l(e.message,t);function o(){var t=arguments.length>0&&void 0!==arguments[0]?arguments[0]:{},o=arguments.length>1?arguments[1]:void 0,s=void 0===o&&(t instanceof Error||void 0!==t.code&&"Exception"===t.code.slice(-9))?{cause:t,missingParameters:{}}:{cause:o,missingParameters:t},a=s.cause,i=s.missingParameters,u=void 0===n?new Error:new Error(n(i));return null!==a&&(u.cause=a),void 0!==r&&(u.code=r(i)),void 0!==e.status&&(u.status=e.status),u}return o};e.compile=d,Object.defineProperty(e,"__esModule",{value:!0})}(t,r(106),r(881),r(507))},881:e=>{"use strict";e.exports=(e,t)=>{if("string"!=typeof e)throw new TypeError("expected a string");return e.trim().replace(/([a-z])([A-Z])/g,"$1-$2").replace(/\\W/g,(e=>/[\xC0-\u017E]/.test(e)?e:"-")).replace(/^-+|-+$/g,"").replace(/-{2,}/g,(e=>t&&t.condense?"-":e)).toLowerCase()}},107:function(e,t){!function(e){"use strict";var t=function(e){return function(t){var r=e(t);return t.add(r),r}},r=function(e){return function(t,r){return e.set(t,r),r}},n=void 0===Number.MAX_SAFE_INTEGER?9007199254740991:Number.MAX_SAFE_INTEGER,o=536870912,s=2*o,a=function(e,t){return function(r){var a=t.get(r),i=void 0===a?r.size:a<s?a+1:0;if(!r.has(i))return e(r,i);if(r.size<o){for(;r.has(i);)i=Math.floor(Math.random()*s);return e(r,i)}if(r.size>n)throw new Error("Congratulations, you created a collection of unique numbers which uses all available integers!");for(;r.has(i);)i=Math.floor(Math.random()*n);return e(r,i)}},i=new WeakMap,u=r(i),c=a(u,i),l=t(c);e.addUniqueNumber=l,e.generateUniqueNumber=c,Object.defineProperty(e,"__esModule",{value:!0})}(t)},507:e=>{var t=function(e){var t,r,n=/\\w+/.exec(e);if(!n)return"an";var o=(r=n[0]).toLowerCase(),s=["honest","hour","hono"];for(t in s)if(0==o.indexOf(s[t]))return"an";if(1==o.length)return"aedhilmnorsx".indexOf(o)>=0?"an":"a";if(r.match(/(?!FJO|[HLMNS]Y.|RY[EO]|SQU|(F[LR]?|[HL]|MN?|N|RH?|S[CHKLMNPTVW]?|X(YL)?)[AEIOU])[FHLMNRSX][A-Z]/))return"an";var a=[/^e[uw]/,/^onc?e\\b/,/^uni([^nmd]|mo)/,/^u[bcfhjkqrst][aeiou]/];for(t=0;t<a.length;t++)if(o.match(a[t]))return"a";return r.match(/^U[NK][AIEO]/)?"a":r==r.toUpperCase()?"aedhilmnorsx".indexOf(o[0])>=0?"an":"a":"aeiou".indexOf(o[0])>=0||o.match(/^y(b[lor]|cl[ea]|fere|gg|p[ios]|rou|tt)/)?"an":"a"};void 0!==e.exports?e.exports=t:window.indefiniteArticle=t},768:e=>{e.exports=function(e,t){(null==t||t>e.length)&&(t=e.length);for(var r=0,n=new Array(t);r<t;r++)n[r]=e[r];return n},e.exports.__esModule=!0,e.exports.default=e.exports},907:(e,t,r)=>{var n=r(768);e.exports=function(e){if(Array.isArray(e))return n(e)},e.exports.__esModule=!0,e.exports.default=e.exports},642:e=>{e.exports=function(e){if("undefined"!=typeof Symbol&&null!=e[Symbol.iterator]||null!=e["@@iterator"])return Array.from(e)},e.exports.__esModule=!0,e.exports.default=e.exports},344:e=>{e.exports=function(){throw new TypeError("Invalid attempt to spread non-iterable instance.\\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.")},e.exports.__esModule=!0,e.exports.default=e.exports},106:(e,t,r)=>{var n=r(907),o=r(642),s=r(906),a=r(344);e.exports=function(e){return n(e)||o(e)||s(e)||a()},e.exports.__esModule=!0,e.exports.default=e.exports},906:(e,t,r)=>{var n=r(768);e.exports=function(e,t){if(e){if("string"==typeof e)return n(e,t);var r=Object.prototype.toString.call(e).slice(8,-1);return"Object"===r&&e.constructor&&(r=e.constructor.name),"Map"===r||"Set"===r?Array.from(e):"Arguments"===r||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(r)?n(e,t):void 0}},e.exports.__esModule=!0,e.exports.default=e.exports}},t={};function r(n){var o=t[n];if(void 0!==o)return o.exports;var s=t[n]={exports:{}};return e[n].call(s.exports,s,s.exports,r),s.exports}(()=>{"use strict";var e=r(775);const t=-32603,n=-32602,o=-32601,s=(0,e.compile)({message:'The requested method called "\${method}" is not supported.',status:o}),a=(0,e.compile)({message:'The handler of the method called "\${method}" returned no required result.',status:t}),i=(0,e.compile)({message:'The handler of the method called "\${method}" returned an unexpected result.',status:t}),u=(0,e.compile)({message:'The specified parameter called "portId" with the given value "\${portId}" does not identify a port connected to this worker.',status:n}),c=(e,t)=>async r=>{let{data:{id:n,method:o,params:u}}=r;const c=t[o];try{if(void 0===c)throw s({method:o});const t=void 0===u?c():c(u);if(void 0===t)throw a({method:o});const r=t instanceof Promise?await t:t;if(null===n){if(void 0!==r.result)throw i({method:o})}else{if(void 0===r.result)throw i({method:o});const{result:t,transferables:s=[]}=r;e.postMessage({id:n,result:t},s)}}catch(t){const{message:r,status:o=-32603}=t;e.postMessage({error:{code:o,message:r},id:n})}};var l=r(107);const d=new Map,f=(e,t,r)=>({...t,connect:r=>{let{port:n}=r;n.start();const o=e(n,t),s=(0,l.generateUniqueNumber)(d);return d.set(s,(()=>{o(),n.close(),d.delete(s)})),{result:s}},disconnect:e=>{let{portId:t}=e;const r=d.get(t);if(void 0===r)throw u({portId:t.toString()});return r(),{result:null}},isSupported:async()=>{if(await new Promise((e=>{const t=new ArrayBuffer(0),{port1:r,port2:n}=new MessageChannel;r.onmessage=t=>{let{data:r}=t;return e(null!==r)},n.postMessage(t,[t])}))){const e=r();return{result:e instanceof Promise?await e:e}}return{result:!1}}}),p=function(e,t){let r=arguments.length>2&&void 0!==arguments[2]?arguments[2]:()=>!0;const n=f(p,t,r),o=c(e,n);return e.addEventListener("message",o),()=>e.removeEventListener("message",o)},m=e=>e.reduce(((e,t)=>e+t.length),0),h=(e,t)=>{const r=[];let n=0;e:for(;n<t;){const t=e.length;for(let o=0;o<t;o+=1){const t=e[o];void 0===r[o]&&(r[o]=[]);const s=t.shift();if(void 0===s)break e;r[o].push(s),0===o&&(n+=s.length)}}if(n>t){const o=n-t;r.forEach(((t,r)=>{const n=t.pop(),s=n.length-o;t.push(n.subarray(0,s)),e[r].unshift(n.subarray(s))}))}return r},v=new Map,g=(e=>(t,r,n)=>{const o=e.get(t);if(void 0===o){const o={channelDataArrays:n.map((e=>[e])),isComplete:!0,sampleRate:r};return e.set(t,o),o}return o.channelDataArrays.forEach(((e,t)=>e.push(n[t]))),o})(v),x=((e,t)=>(r,n,o,s)=>{const a=o>>3,i="subsequent"===n?0:44,u=r.length,c=e(r[0]),l=new ArrayBuffer(c*u*a+i),d=new DataView(l);return"subsequent"!==n&&t(d,o,u,"complete"===n?c:Number.POSITIVE_INFINITY,s),r.forEach(((e,t)=>{let r=i+t*a;e.forEach((e=>{const t=e.length;for(let n=0;n<t;n+=1){const t=e[n];d.setInt16(r,t<0?32768*Math.max(-1,t):32767*Math.min(1,t),!0),r+=u*a}}))})),[l]})(m,((e,t,r,n,o)=>{const s=t>>3,a=Math.min(n*r*s,4294967251);e.setUint32(0,1380533830),e.setUint32(4,a+36,!0),e.setUint32(8,1463899717),e.setUint32(12,1718449184),e.setUint32(16,16,!0),e.setUint16(20,1,!0),e.setUint16(22,r,!0),e.setUint32(24,o,!0),e.setUint32(28,o*r*s,!0),e.setUint16(32,r*s,!0),e.setUint16(34,t,!0),e.setUint32(36,1684108385),e.setUint32(40,a,!0)})),w=new Map;p(self,{characterize:()=>({result:/^audio\\/wav$/}),encode:e=>{let{recordingId:t,timeslice:r}=e;const n=w.get(t);void 0!==n&&(w.delete(t),n.reject(new Error("Another request was made to initiate an encoding.")));const o=v.get(t);if(null!==r){if(void 0===o||m(o.channelDataArrays[0])*(1e3/o.sampleRate)<r)return new Promise(((e,n)=>{w.set(t,{reject:n,resolve:e,timeslice:r})}));const e=h(o.channelDataArrays,Math.ceil(r*(o.sampleRate/1e3))),n=x(e,o.isComplete?"initial":"subsequent",16,o.sampleRate);return o.isComplete=!1,{result:n,transferables:n}}if(void 0!==o){const e=x(o.channelDataArrays,o.isComplete?"complete":"subsequent",16,o.sampleRate);return v.delete(t),{result:e,transferables:e}}return{result:[],transferables:[]}},record:e=>{let{recordingId:t,sampleRate:r,typedArrays:n}=e;const o=g(t,r,n),s=w.get(t);if(void 0!==s&&m(o.channelDataArrays[0])*(1e3/r)>=s.timeslice){const e=h(o.channelDataArrays,Math.ceil(s.timeslice*(r/1e3))),n=x(e,o.isComplete?"initial":"subsequent",16,r);o.isComplete=!1,w.delete(t),s.resolve({result:n,transferables:n})}return{result:null}}})})()})();`;
const blob = new Blob([worker], { type: "application/javascript; charset=utf-8" });
const url = URL.createObjectURL(blob);
const extendableMediaRecorderWavEncoder = load(url);
const characterize = extendableMediaRecorderWavEncoder.characterize;
const connect = extendableMediaRecorderWavEncoder.connect;
const disconnect = extendableMediaRecorderWavEncoder.disconnect;
const encode = extendableMediaRecorderWavEncoder.encode;
const isSupported = extendableMediaRecorderWavEncoder.isSupported;
const record = extendableMediaRecorderWavEncoder.record;
URL.revokeObjectURL(url);
export { characterize, connect, disconnect, encode, isSupported, record };
//# sourceMappingURL=module3.js.map
