"use strict";(self.webpackChunkstarwhale_docs=self.webpackChunkstarwhale_docs||[]).push([[8317],{3905:function(e,t,n){n.d(t,{Zo:function(){return m},kt:function(){return s}});var r=n(7294);function a(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function l(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function i(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?l(Object(n),!0).forEach((function(t){a(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):l(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function o(e,t){if(null==e)return{};var n,r,a=function(e,t){if(null==e)return{};var n,r,a={},l=Object.keys(e);for(r=0;r<l.length;r++)n=l[r],t.indexOf(n)>=0||(a[n]=e[n]);return a}(e,t);if(Object.getOwnPropertySymbols){var l=Object.getOwnPropertySymbols(e);for(r=0;r<l.length;r++)n=l[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(a[n]=e[n])}return a}var c=r.createContext({}),p=function(e){var t=r.useContext(c),n=t;return e&&(n="function"==typeof e?e(t):i(i({},t),e)),n},m=function(e){var t=p(e.components);return r.createElement(c.Provider,{value:t},e.children)},d={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},u=r.forwardRef((function(e,t){var n=e.components,a=e.mdxType,l=e.originalType,c=e.parentName,m=o(e,["components","mdxType","originalType","parentName"]),u=p(n),s=a,f=u["".concat(c,".").concat(s)]||u[s]||d[s]||l;return n?r.createElement(f,i(i({ref:t},m),{},{components:n})):r.createElement(f,i({ref:t},m))}));function s(e,t){var n=arguments,a=t&&t.mdxType;if("string"==typeof e||a){var l=n.length,i=new Array(l);i[0]=u;var o={};for(var c in t)hasOwnProperty.call(t,c)&&(o[c]=t[c]);o.originalType=e,o.mdxType="string"==typeof e?e:a,i[1]=o;for(var p=2;p<l;p++)i[p]=n[p];return r.createElement.apply(null,i)}return r.createElement.apply(null,n)}u.displayName="MDXCreateElement"},667:function(e,t,n){n.r(t),n.d(t,{assets:function(){return c},contentTitle:function(){return i},default:function(){return d},frontMatter:function(){return l},metadata:function(){return o},toc:function(){return p}});var r=n(3117),a=(n(7294),n(3905));const l={title:"Overview"},i=void 0,o={unversionedId:"reference/cli/basic",id:"reference/cli/basic",title:"Overview",description:"Usage",source:"@site/docs/reference/cli/basic.md",sourceDirName:"reference/cli",slug:"/reference/cli/basic",permalink:"/docs/next/reference/cli/basic",draft:!1,editUrl:"https://github.com/star-whale/starwhale/tree/main/docs/docs/reference/cli/basic.md",tags:[],version:"current",frontMatter:{title:"Overview"},sidebar:"mainSidebar",previous:{title:"FAQ",permalink:"/docs/next/guides/faq"},next:{title:"Instance",permalink:"/docs/next/reference/cli/instance"}},c={},p=[{value:"Usage",id:"usage",level:2},{value:"Options",id:"options",level:2},{value:"Commands",id:"commands",level:2}],m={toc:p};function d(e){let{components:t,...n}=e;return(0,a.kt)("wrapper",(0,r.Z)({},m,n,{components:t,mdxType:"MDXLayout"}),(0,a.kt)("h2",{id:"usage"},"Usage"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-bash"},"swcli [OPTIONS] COMMAND [ARGS]...\n")),(0,a.kt)("admonition",{type:"note"},(0,a.kt)("p",{parentName:"admonition"},(0,a.kt)("inlineCode",{parentName:"p"},"sw"),", ",(0,a.kt)("inlineCode",{parentName:"p"},"starwhale")," are aliases for the ",(0,a.kt)("inlineCode",{parentName:"p"},"swcli")," commands.")),(0,a.kt)("h2",{id:"options"},"Options"),(0,a.kt)("table",null,(0,a.kt)("thead",{parentName:"table"},(0,a.kt)("tr",{parentName:"thead"},(0,a.kt)("th",{parentName:"tr",align:null},"Option"),(0,a.kt)("th",{parentName:"tr",align:null},"Description"))),(0,a.kt)("tbody",{parentName:"table"},(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},(0,a.kt)("inlineCode",{parentName:"td"},"--version")),(0,a.kt)("td",{parentName:"tr",align:null},"Show the Starwhale version")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},(0,a.kt)("inlineCode",{parentName:"td"},"-v")," or ",(0,a.kt)("inlineCode",{parentName:"td"},"--verbose")),(0,a.kt)("td",{parentName:"tr",align:null},"Show verbose log, support multi counts for ",(0,a.kt)("inlineCode",{parentName:"td"},"-v")," args. More ",(0,a.kt)("inlineCode",{parentName:"td"},"-v")," args, more logs.")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},(0,a.kt)("inlineCode",{parentName:"td"},"--help")),(0,a.kt)("td",{parentName:"tr",align:null},"Show help message.")))),(0,a.kt)("h2",{id:"commands"},"Commands"),(0,a.kt)("table",null,(0,a.kt)("thead",{parentName:"table"},(0,a.kt)("tr",{parentName:"thead"},(0,a.kt)("th",{parentName:"tr",align:null},"Command"),(0,a.kt)("th",{parentName:"tr",align:null},"Description"))),(0,a.kt)("tbody",{parentName:"table"},(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"dataset"),(0,a.kt)("td",{parentName:"tr",align:null},"Dataset management, build/info/list/copy/tag/render-fuse...")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"runtime"),(0,a.kt)("td",{parentName:"tr",align:null},"Runtime management, create/build/copy/activate/restore...")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"model"),(0,a.kt)("td",{parentName:"tr",align:null},"Model management, build/copy/ppl/cmp/eval...")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"project"),(0,a.kt)("td",{parentName:"tr",align:null},"Project management, for standalone and cloud instances")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"instance"),(0,a.kt)("td",{parentName:"tr",align:null},"Instance management, login and select standalone or cloud instance")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"job"),(0,a.kt)("td",{parentName:"tr",align:null},"Job management, create/list/info/compare evaluation job")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"ui"),(0,a.kt)("td",{parentName:"tr",align:null},"Open instance web ui")),(0,a.kt)("tr",{parentName:"tbody"},(0,a.kt)("td",{parentName:"tr",align:null},"gc"),(0,a.kt)("td",{parentName:"tr",align:null},"Standalone garbage collection")))))}d.isMDXComponent=!0}}]);