"use strict";(self.webpackChunkstarwhale_docs=self.webpackChunkstarwhale_docs||[]).push([[7550],{3905:function(t,e,a){a.d(e,{Zo:function(){return s},kt:function(){return k}});var n=a(7294);function l(t,e,a){return e in t?Object.defineProperty(t,e,{value:a,enumerable:!0,configurable:!0,writable:!0}):t[e]=a,t}function r(t,e){var a=Object.keys(t);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(t);e&&(n=n.filter((function(e){return Object.getOwnPropertyDescriptor(t,e).enumerable}))),a.push.apply(a,n)}return a}function i(t){for(var e=1;e<arguments.length;e++){var a=null!=arguments[e]?arguments[e]:{};e%2?r(Object(a),!0).forEach((function(e){l(t,e,a[e])})):Object.getOwnPropertyDescriptors?Object.defineProperties(t,Object.getOwnPropertyDescriptors(a)):r(Object(a)).forEach((function(e){Object.defineProperty(t,e,Object.getOwnPropertyDescriptor(a,e))}))}return t}function d(t,e){if(null==t)return{};var a,n,l=function(t,e){if(null==t)return{};var a,n,l={},r=Object.keys(t);for(n=0;n<r.length;n++)a=r[n],e.indexOf(a)>=0||(l[a]=t[a]);return l}(t,e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(t);for(n=0;n<r.length;n++)a=r[n],e.indexOf(a)>=0||Object.prototype.propertyIsEnumerable.call(t,a)&&(l[a]=t[a])}return l}var p=n.createContext({}),m=function(t){var e=n.useContext(p),a=e;return t&&(a="function"==typeof t?t(e):i(i({},e),t)),a},s=function(t){var e=m(t.components);return n.createElement(p.Provider,{value:e},t.children)},u={inlineCode:"code",wrapper:function(t){var e=t.children;return n.createElement(n.Fragment,{},e)}},o=n.forwardRef((function(t,e){var a=t.components,l=t.mdxType,r=t.originalType,p=t.parentName,s=d(t,["components","mdxType","originalType","parentName"]),o=m(a),k=l,N=o["".concat(p,".").concat(k)]||o[k]||u[k]||r;return a?n.createElement(N,i(i({ref:e},s),{},{components:a})):n.createElement(N,i({ref:e},s))}));function k(t,e){var a=arguments,l=e&&e.mdxType;if("string"==typeof t||l){var r=a.length,i=new Array(r);i[0]=o;var d={};for(var p in e)hasOwnProperty.call(e,p)&&(d[p]=e[p]);d.originalType=t,d.mdxType="string"==typeof t?t:l,i[1]=d;for(var m=2;m<r;m++)i[m]=a[m];return n.createElement.apply(null,i)}return n.createElement.apply(null,a)}o.displayName="MDXCreateElement"},2855:function(t,e,a){a.r(e),a.d(e,{assets:function(){return p},contentTitle:function(){return i},default:function(){return u},frontMatter:function(){return r},metadata:function(){return d},toc:function(){return m}});var n=a(3117),l=(a(7294),a(3905));const r={title:"Starwhale Dataset-\u6570\u636e\u96c6"},i=void 0,d={unversionedId:"guides/dataset",id:"version-0.4.0/guides/dataset",title:"Starwhale Dataset-\u6570\u636e\u96c6",description:"1. \u8bbe\u8ba1\u6982\u8ff0",source:"@site/i18n/zh/docusaurus-plugin-content-docs/version-0.4.0/guides/dataset.md",sourceDirName:"guides",slug:"/guides/dataset",permalink:"/zh/docs/guides/dataset",draft:!1,editUrl:"https://github.com/star-whale/starwhale/tree/main/docs/versioned_docs/version-0.4.0/guides/dataset.md",tags:[],version:"0.4.0",frontMatter:{title:"Starwhale Dataset-\u6570\u636e\u96c6"},sidebar:"mainSidebar",previous:{title:"Starwhale Resources URI\u5b9a\u4e49",permalink:"/zh/docs/guides/uri"},next:{title:"Starwhale Runtime-\u8fd0\u884c\u73af\u5883",permalink:"/zh/docs/guides/runtime"}},p={},m=[{value:"1. \u8bbe\u8ba1\u6982\u8ff0",id:"1-\u8bbe\u8ba1\u6982\u8ff0",level:2},{value:"1.1 Starwhale Dataset \u5b9a\u4f4d",id:"11-starwhale-dataset-\u5b9a\u4f4d",level:3},{value:"1.2 \u6838\u5fc3\u529f\u80fd",id:"12-\u6838\u5fc3\u529f\u80fd",level:3},{value:"1.3 \u5173\u952e\u5143\u7d20",id:"13-\u5173\u952e\u5143\u7d20",level:3},{value:"2. \u6700\u4f73\u5b9e\u8df5",id:"2-\u6700\u4f73\u5b9e\u8df5",level:2},{value:"2.1 \u547d\u4ee4\u884c\u5206\u7ec4",id:"21-\u547d\u4ee4\u884c\u5206\u7ec4",level:3},{value:"2.2 \u6838\u5fc3\u6d41\u7a0b",id:"22-\u6838\u5fc3\u6d41\u7a0b",level:3},{value:"3. dataset.yaml \u8bf4\u660e",id:"3-datasetyaml-\u8bf4\u660e",level:2},{value:"3.1 YAML \u5b57\u6bb5\u63cf\u8ff0",id:"31-yaml-\u5b57\u6bb5\u63cf\u8ff0",level:3},{value:"3.2 \u4f7f\u7528\u793a\u4f8b",id:"32-\u4f7f\u7528\u793a\u4f8b",level:3},{value:"3.3 \u6700\u7b80\u793a\u4f8b",id:"33-\u6700\u7b80\u793a\u4f8b",level:4},{value:"3.4 MNIST\u6570\u636e\u96c6\u6784\u5efa\u793a\u4f8b",id:"34-mnist\u6570\u636e\u96c6\u6784\u5efa\u793a\u4f8b",level:4},{value:"3.5 handler\u4e3agenerator function\u7684\u4f8b\u5b50",id:"35-handler\u4e3agenerator-function\u7684\u4f8b\u5b50",level:4},{value:"4. Starwhale Dataset Viewer",id:"4-starwhale-dataset-viewer",level:2},{value:"5. Starwhale Dataset \u6570\u636e\u683c\u5f0f",id:"5-starwhale-dataset-\u6570\u636e\u683c\u5f0f",level:2},{value:"5.1 \u6587\u4ef6\u7c7b\u6570\u636e\u7684\u5904\u7406\u65b9\u5f0f",id:"51-\u6587\u4ef6\u7c7b\u6570\u636e\u7684\u5904\u7406\u65b9\u5f0f",level:3}],s={toc:m};function u(t){let{components:e,...r}=t;return(0,l.kt)("wrapper",(0,n.Z)({},s,r,{components:e,mdxType:"MDXLayout"}),(0,l.kt)("h2",{id:"1-\u8bbe\u8ba1\u6982\u8ff0"},"1. \u8bbe\u8ba1\u6982\u8ff0"),(0,l.kt)("h3",{id:"11-starwhale-dataset-\u5b9a\u4f4d"},"1.1 Starwhale Dataset \u5b9a\u4f4d"),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Dataset")," \u5305\u542b\u6570\u636e\u6784\u5efa\u3001\u6570\u636e\u52a0\u8f7d\u548c\u6570\u636e\u53ef\u89c6\u5316\u4e09\u4e2a\u6838\u5fc3\u9636\u6bb5\uff0c\u662f\u4e00\u6b3e\u9762\u5411ML/DL\u9886\u57df\u7684\u6570\u636e\u7ba1\u7406\u5de5\u5177\u3002",(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Dataset")," \u80fd\u76f4\u63a5\u4f7f\u7528 ",(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Runtime")," \u6784\u5efa\u7684\u73af\u5883\uff0c\u80fd\u88ab ",(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Model")," \u548c ",(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Evaluation")," \u65e0\u7f1d\u96c6\u6210\uff0c\u662fStarwhale MLOps\u5de5\u5177\u94fe\u7684\u91cd\u8981\u7ec4\u6210\u90e8\u5206\u3002"),(0,l.kt)("p",null,"\u6839\u636e ",(0,l.kt)("a",{parentName:"p",href:"https://arxiv.org/abs/2205.02302"},"Machine Learning Operations (MLOps): Overview, Definition, and Architecture")," \u5bf9MLOps Roles\u7684\u5206\u7c7b\uff0cStarwhale Dataset\u7684\u4e09\u4e2a\u9636\u6bb5\u9488\u5bf9\u7528\u6237\u7fa4\u4f53\u5982\u4e0b\uff1a"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},"\u6570\u636e\u6784\u5efa\uff1aData Engineer\u3001Data Scientist"),(0,l.kt)("li",{parentName:"ul"},"\u6570\u636e\u52a0\u8f7d\uff1aData Scientist\u3001ML Developer"),(0,l.kt)("li",{parentName:"ul"},"\u6570\u636e\u53ef\u89c6\u5316\uff1aData Engineer\u3001Data Scientist\u3001ML Developer")),(0,l.kt)("p",null,(0,l.kt)("img",{alt:"mlops-users",src:a(3439).Z,width:"638",height:"537"})),(0,l.kt)("h3",{id:"12-\u6838\u5fc3\u529f\u80fd"},"1.2 \u6838\u5fc3\u529f\u80fd"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("strong",{parentName:"li"},"\u9ad8\u6548\u52a0\u8f7d"),"\uff1a\u6570\u636e\u96c6\u539f\u59cb\u6587\u4ef6\u5b58\u50a8\u5728OSS\u6216NAS\u7b49\u5916\u90e8\u5b58\u50a8\u4e0a\uff0c\u4f7f\u7528\u65f6\u6309\u9700\u52a0\u8f7d\uff0c\u4e0d\u9700\u8981\u6570\u636e\u843d\u76d8\u3002"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("strong",{parentName:"li"},"\u7b80\u5355\u6784\u5efa"),"\uff1a\u901a\u8fc7\u7f16\u5199\u7b80\u5355\u7684Python\u4ee3\u7801\uff08\u975e\u5fc5\u987b\uff09\uff0c\u5c11\u91cf\u7684dataset.yaml\uff08\u975e\u5fc5\u987b\uff09\u540e\uff0c\u6267\u884cswcli dataset build \u547d\u4ee4\u5c31\u80fd\u5b8c\u6210\u6570\u636e\u96c6\u7684\u6784\u5efa\u3002"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("strong",{parentName:"li"},"\u7248\u672c\u7ba1\u7406"),"\uff1a\u53ef\u4ee5\u8fdb\u884c\u7248\u672c\u8ffd\u8e2a\u3001\u6570\u636e\u8ffd\u52a0\u7b49\u64cd\u4f5c\uff0c\u5e76\u901a\u8fc7\u5185\u90e8\u62bd\u8c61\u7684ObjectStore\uff0c\u907f\u514d\u6570\u636e\u91cd\u590d\u5b58\u50a8\u3002"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("strong",{parentName:"li"},"\u6570\u636e\u96c6\u5206\u53d1"),"\uff1a\u901a\u8fc7copy\u547d\u4ee4\uff0c\u5b9e\u73b0standalone instance\u548ccloud instance\u53cc\u5411\u7684\u6570\u636e\u96c6\u5206\u4eab\u3002"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("strong",{parentName:"li"},"\u6570\u636e\u53ef\u89c6\u5316"),"\uff1aCloud Instance\u7684Web\u754c\u9762\u4e2d\u53ef\u4ee5\u5bf9\u6570\u636e\u96c6\u63d0\u4f9b\u591a\u7ef4\u5ea6\u3001\u591a\u7c7b\u578b\u7684\u6570\u636e\u5448\u73b0\u3002"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("strong",{parentName:"li"},"\u5236\u54c1\u5b58\u50a8"),"\uff1aStandalone Instance\u80fd\u5b58\u50a8\u672c\u5730\u6784\u5efa\u6216\u5206\u53d1\u7684swds\u7cfb\u5217\u6587\u4ef6\uff0cCloud Instance\u4f7f\u7528\u5bf9\u8c61\u5b58\u50a8\u63d0\u4f9b\u96c6\u4e2d\u5f0f\u7684swds\u5236\u54c1\u5b58\u50a8\u3002"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("strong",{parentName:"li"},"Starwhale\u65e0\u7f1d\u96c6\u6210"),"\uff1a",(0,l.kt)("inlineCode",{parentName:"li"},"Starwhale Dataset")," \u80fd\u4f7f\u7528 ",(0,l.kt)("inlineCode",{parentName:"li"},"Starwhale Runtime")," \u6784\u5efa\u7684\u8fd0\u884c\u73af\u5883\u6784\u5efa\u6570\u636e\u96c6\u3002",(0,l.kt)("inlineCode",{parentName:"li"},"Starwhale Evaluation")," \u548c ",(0,l.kt)("inlineCode",{parentName:"li"},"Starwhale Model")," \u76f4\u63a5\u901a\u8fc7 ",(0,l.kt)("inlineCode",{parentName:"li"},"--dataset")," \u53c2\u6570\u6307\u5b9a\u6570\u636e\u96c6\uff0c\u5c31\u80fd\u5b8c\u6210\u81ea\u52a8\u6570\u636e\u52a0\u8f7d\uff0c\u4fbf\u4e8e\u8fdb\u884c\u63a8\u7406\u3001\u6a21\u578b\u8bc4\u6d4b\u7b49\u73af\u5883\u3002")),(0,l.kt)("h3",{id:"13-\u5173\u952e\u5143\u7d20"},"1.3 \u5173\u952e\u5143\u7d20"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swds")," \u865a\u62df\u5305\u6587\u4ef6\uff1aswds \u4e0eswmp\u548cswrt\u4e0d\u4e00\u6837\uff0c\u4e0d\u662f\u4e00\u4e2a\u6253\u5305\u7684\u5355\u4e00\u6587\u4ef6\uff0c\u800c\u662f\u4e00\u4e2a\u865a\u62df\u7684\u6982\u5ff5\uff0c\u5177\u4f53\u6307\u7684\u662f\u4e00\u4e2a\u76ee\u5f55\uff0c\u662fStarwhale Dataset\u67d0\u4e2a\u7248\u672c\u5305\u542b\u7684\u6570\u636e\u96c6\u76f8\u5173\u7684\u6587\u4ef6\uff0c\u5305\u62ec _manifest.yaml, dataset.yaml, \u6570\u636e\u96c6\u6784\u5efa\u7684Python\u811a\u672c\u548c\u6570\u636e\u6587\u4ef6\u7684\u94fe\u63a5\u7b49\u3002\u53ef\u4ee5\u901a\u8fc7 ",(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset info")," \u547d\u4ee4\u67e5\u770bswds\u6240\u5728\u76ee\u5f55\u3002swds \u662fStarwhale Dataset\u7684\u7b80\u5199\u3002\n",(0,l.kt)("img",{alt:"swds-tree.png",src:a(5713).Z,width:"3249",height:"1135"})),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset")," \u547d\u4ee4\u884c\uff1a\u4e00\u7ec4dataset\u76f8\u5173\u7684\u547d\u4ee4\uff0c\u5305\u62ec\u6784\u5efa\u3001\u5206\u53d1\u548c\u7ba1\u7406\u7b49\u529f\u80fd\uff0c\u5177\u4f53\u8bf4\u660e\u53c2\u8003",(0,l.kt)("a",{parentName:"li",href:"/zh/docs/reference/cli/dataset"},"CLI Reference"),"\u3002"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"dataset.yaml")," \u914d\u7f6e\u6587\u4ef6\uff1a\u63cf\u8ff0\u6570\u636e\u96c6\u7684\u6784\u5efa\u8fc7\u7a0b\uff0c\u53ef\u4ee5\u5b8c\u5168\u7701\u7565\uff0c\u901a\u8fc7swcli dataset build\u53c2\u6570\u6307\u5b9a\uff0c\u53ef\u4ee5\u8ba4\u4e3adataset.yaml\u662fbuild\u547d\u4ee4\u884c\u53c2\u6570\u7684\u4e00\u79cd\u914d\u7f6e\u6587\u4ef6\u8868\u793a\u65b9\u5f0f\u3002swcli dataset build \u53c2\u6570\u4f18\u5148\u7ea7\u9ad8\u4e8e dataset.yaml\u3002"),(0,l.kt)("li",{parentName:"ul"},"Dataset Python SDK\uff1a\u5305\u62ec\u6570\u636e\u6784\u5efa\u3001\u6570\u636e\u52a0\u8f7d\u548c\u82e5\u5e72\u9884\u5b9a\u4e49\u7684\u6570\u636e\u7c7b\u578b\uff0c\u5177\u4f53\u8bf4\u660e\u53c2\u8003",(0,l.kt)("a",{parentName:"li",href:"/zh/docs/reference/sdk/dataset"},"Python SDK"),"\u3002"),(0,l.kt)("li",{parentName:"ul"},"\u6570\u636e\u96c6\u6784\u5efa\u7684Python\u811a\u672c\uff1a\u4f7f\u7528Starwhale Python SDK\u7f16\u5199\u7684\u7528\u6765\u6784\u5efa\u6570\u636e\u96c6\u7684\u4e00\u7cfb\u5217\u811a\u672c\u3002")),(0,l.kt)("h2",{id:"2-\u6700\u4f73\u5b9e\u8df5"},"2. \u6700\u4f73\u5b9e\u8df5"),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Dataset")," \u7684\u6784\u5efa\u662f\u72ec\u7acb\u8fdb\u884c\u7684\uff0c\u5982\u679c\u7f16\u5199\u6784\u5efa\u811a\u672c\u65f6\u9700\u8981\u5f15\u5165\u7b2c\u4e09\u65b9\u5e93\uff0c\u90a3\u4e48\u4f7f\u7528 ",(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Runtime")," \u53ef\u4ee5\u7b80\u5316Python\u7684\u4f9d\u8d56\u7ba1\u7406\uff0c\u80fd\u4fdd\u8bc1\u6570\u636e\u96c6\u7684\u6784\u5efa\u53ef\u590d\u73b0\u3002Starwhale\u5e73\u53f0\u4f1a\u5c3d\u53ef\u80fd\u591a\u7684\u5185\u5efa\u5f00\u6e90\u6570\u636e\u96c6\uff0c\u8ba9\u7528\u6237copy\u4e0b\u6765\u6570\u636e\u96c6\u540e\u80fd\u7acb\u5373\u4f7f\u7528\u3002"),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Dataset")," \u6784\u5efa\u7684\u65f6\u5019\u4f1a\u81ea\u52a8\u5c06Python\u6587\u4ef6\u8fdb\u884c\u6253\u5305\uff0c\u53ef\u4ee5\u8bbe\u7f6e ",(0,l.kt)("inlineCode",{parentName:"p"},".swignore")," ",(0,l.kt)("a",{parentName:"p",href:"/zh/docs/guides/config/swignore"},"\u6587\u4ef6")," \u6392\u9664\u67d0\u4e9b\u6587\u4ef6\u3002"),(0,l.kt)("h3",{id:"21-\u547d\u4ee4\u884c\u5206\u7ec4"},"2.1 \u547d\u4ee4\u884c\u5206\u7ec4"),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Dataset")," \u547d\u4ee4\u884c\u4ece\u4f7f\u7528\u9636\u6bb5\u7684\u89d2\u5ea6\u4e0a\uff0c\u53ef\u4ee5\u5212\u5206\u5982\u4e0b\uff1a"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},"\u6784\u5efa\u9636\u6bb5",(0,l.kt)("ul",{parentName:"li"},(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset build")))),(0,l.kt)("li",{parentName:"ul"},"\u53ef\u89c6\u5316\u9636\u6bb5",(0,l.kt)("ul",{parentName:"li"},(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset diff")))),(0,l.kt)("li",{parentName:"ul"},"\u5206\u53d1\u9636\u6bb5\uff1a",(0,l.kt)("ul",{parentName:"li"},(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset copy")))),(0,l.kt)("li",{parentName:"ul"},"\u57fa\u672c\u7ba1\u7406",(0,l.kt)("ul",{parentName:"li"},(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset tag")),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset info")),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset history")),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset list")),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset summary")),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset remove")),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"swcli dataset recover"))))),(0,l.kt)("h3",{id:"22-\u6838\u5fc3\u6d41\u7a0b"},"2.2 \u6838\u5fc3\u6d41\u7a0b"),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Dataset")," \u4f7f\u7528\u7684\u6838\u5fc3\u6d41\u7a0b\u5982\u4e0b\u56fe\uff1a"),(0,l.kt)("p",null,(0,l.kt)("img",{alt:"dataset-workflow.jpg",src:a(2217).Z,width:"2771",height:"662"})),(0,l.kt)("h2",{id:"3-datasetyaml-\u8bf4\u660e"},"3. dataset.yaml \u8bf4\u660e"),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale Dataset")," \u6784\u5efa\u7684\u65f6\u5019\u4f7f\u7528dataset.yaml\uff0c\u82e5\u7701\u7565dataset.yaml\uff0c\u5219\u53ef\u4ee5\u5728 ",(0,l.kt)("inlineCode",{parentName:"p"},"swcli dataset build")," \u547d\u4ee4\u884c\u53c2\u6570\u4e2d\u63cf\u8ff0\u76f8\u5173\u914d\u7f6e\u3002\u53ef\u4ee5\u8ba4\u4e3adataset.yaml\u662fbuild\u547d\u4ee4\u884c\u7684\u914d\u7f6e\u6587\u4ef6\u5316\u8868\u8ff0\u3002"),(0,l.kt)("h3",{id:"31-yaml-\u5b57\u6bb5\u63cf\u8ff0"},"3.1 YAML \u5b57\u6bb5\u63cf\u8ff0"),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"\u5b57\u6bb5"),(0,l.kt)("th",{parentName:"tr",align:null},"\u63cf\u8ff0"),(0,l.kt)("th",{parentName:"tr",align:null},"\u662f\u5426\u5fc5\u8981"),(0,l.kt)("th",{parentName:"tr",align:null},"\u7c7b\u578b"),(0,l.kt)("th",{parentName:"tr",align:null},"\u9ed8\u8ba4\u503c"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"name"),(0,l.kt)("td",{parentName:"tr",align:null},"Starwhale Dataset\u7684\u540d\u5b57"),(0,l.kt)("td",{parentName:"tr",align:null},"\u662f"),(0,l.kt)("td",{parentName:"tr",align:null},"String"),(0,l.kt)("td",{parentName:"tr",align:null})),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"handler"),(0,l.kt)("td",{parentName:"tr",align:null},"\u7ee7\u627f ",(0,l.kt)("inlineCode",{parentName:"td"},"starwhale.SWDSBinBuildExecutor"),", ",(0,l.kt)("inlineCode",{parentName:"td"},"starwhale.UserRawBuildExecutor")," \u6216 ",(0,l.kt)("inlineCode",{parentName:"td"},"starwhale.BuildExecutor")," \u7c7b\u7684\u53efimport\u5730\u5740\uff0c\u6216\u8005\u4e3a\u4e00\u4e2a\u51fd\u6570\uff0c\u8be5\u51fd\u6570\u8fd4\u56de\u4e00\u4e2aGenerator\u6216\u4e00\u4e2a\u53ef\u8fed\u4ee3\u7684\u5bf9\u8c61\uff0c\u683c\u5f0f\u4e3a {module \u8def\u5f84}:{\u7c7b\u540d"),(0,l.kt)("td",{parentName:"tr",align:null},"\u51fd\u6570\u540d}"),(0,l.kt)("td",{parentName:"tr",align:null},"\u662f"),(0,l.kt)("td",{parentName:"tr",align:null},"String")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"desc"),(0,l.kt)("td",{parentName:"tr",align:null},"\u6570\u636e\u96c6\u63cf\u8ff0\u4fe1\u606f"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"String"),(0,l.kt)("td",{parentName:"tr",align:null},'""')),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"version"),(0,l.kt)("td",{parentName:"tr",align:null},"dataset.yaml\u683c\u5f0f\u7248\u672c\uff0c\u76ee\u524d\u4ec5\u652f\u6301\u586b\u5199 1.0"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"String"),(0,l.kt)("td",{parentName:"tr",align:null},"1.0")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"pkg_data"),(0,l.kt)("td",{parentName:"tr",align:null},"swds\u4e2d\u5305\u542b\u7684\u6587\u4ef6\u6216\u76ee\u5f55\uff0c\u652f\u6301wildcard\u65b9\u5f0f\u63cf\u8ff0\u3002\u9ed8\u8ba4\u4f1a\u5305\u542b ",(0,l.kt)("inlineCode",{parentName:"td"},".py/.sh/.yaml")," \u6587\u4ef6"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"List","[String]"),(0,l.kt)("td",{parentName:"tr",align:null})),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"exclude_pkg_data"),(0,l.kt)("td",{parentName:"tr",align:null},"swds\u4e2d\u6392\u9664\u7684\u6587\u4ef6\u6216\u76ee\u5f55\uff0c\u652f\u6301wildcard\u65b9\u5f0f\u63cf\u8ff0\u3002\u4e0d\u5728pkg_data\u4e2d\u6307\u5b9a\u6216",(0,l.kt)("inlineCode",{parentName:"td"},".py/.sh/.yaml"),"\u540e\u7f00\u7684\u6587\u4ef6\uff0c\u90fd\u4e0d\u4f1a\u62f7\u8d1d\u5230swds\u4e2d"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"List","[String]"),(0,l.kt)("td",{parentName:"tr",align:null})),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"attr"),(0,l.kt)("td",{parentName:"tr",align:null},"\u6570\u636e\u96c6\u6784\u5efa\u53c2\u6570"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"Dict"),(0,l.kt)("td",{parentName:"tr",align:null})),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"attr.volume_size"),(0,l.kt)("td",{parentName:"tr",align:null},"swds-bin\u683c\u5f0f\u7684\u6570\u636e\u96c6\u6bcf\u4e2adata\u6587\u4ef6\u7684\u5927\u5c0f\u3002\u5f53\u5199\u6570\u5b57\u65f6\uff0c\u5355\u4f4dbytes\uff1b\u4e5f\u53ef\u4ee5\u662f\u6570\u5b57+\u5355\u4f4d\u683c\u5f0f\uff0c\u598264M, 1GB\u7b49"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"Int\u6216Str"),(0,l.kt)("td",{parentName:"tr",align:null},"64MB")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"attr.alignment_size"),(0,l.kt)("td",{parentName:"tr",align:null},"swds-bin\u683c\u5f0f\u7684\u6570\u636e\u96c6\u6bcf\u4e2a\u6570\u636e\u5757\u7684\u6570\u636ealignment\u5927\u5c0f\uff0c\u5982\u679c\u8bbe\u7f6ealignment_size\u4e3a4k\uff0c\u6570\u636e\u5757\u5927\u5c0f\u4e3a7.9K\uff0c\u5219\u4f1a\u8865\u9f500.1K\u7684\u7a7a\u6570\u636e\uff0c\u8ba9\u6570\u636e\u5757\u4e3aalignment_size\u7684\u6574\u6570\u500d\uff0c\u63d0\u5347page size\u7b49\u8bfb\u53d6\u6548\u7387"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"Integer\u6216String"),(0,l.kt)("td",{parentName:"tr",align:null},"4k")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"attr.data_mime_type"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5982\u679c\u4e0d\u5728\u4ee3\u7801\u4e2d\u4e3a\u6bcf\u6761\u6570\u636e\u6307\u5b9aMIMEType\uff0c\u5219\u4f1a\u4f7f\u7528\u8be5\u5b57\u6bb5\uff0c\u4fbf\u4e8eDataset Viewer\u5448\u73b0"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"String"),(0,l.kt)("td",{parentName:"tr",align:null},"undefined")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"append"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5f53append\u8bbe\u7f6e\u4e3aTrue\u65f6\uff0c\u8868\u793a\u6b64\u6b21\u6570\u636e\u96c6\u6784\u5efa\u4f1a\u7ee7\u627f ",(0,l.kt)("inlineCode",{parentName:"td"},"append_from"),"\u7248\u672c\u7684\u6570\u636e\u96c6\u5185\u5bb9\uff0c\u5b9e\u73b0\u8ffd\u52a0\u6570\u636e\u96c6\u7684\u76ee\u7684\u3002"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"Boolean"),(0,l.kt)("td",{parentName:"tr",align:null},"False")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"append_from"),(0,l.kt)("td",{parentName:"tr",align:null},"\u4e0e ",(0,l.kt)("inlineCode",{parentName:"td"},"append")," \u53c2\u6570\u7ec4\u5408\u4f7f\u7528\uff0c\u6307\u5b9a\u7ee7\u627f\u6570\u636e\u96c6\u7684\u7248\u672c\uff0c\u6ce8\u610f\u6b64\u5904\u5e76\u4e0d\u662fDataset URI\uff0c\u800c\u662f\u540c\u4e00\u4e2a\u6570\u636e\u96c6\u4e0b\u7684\u5176\u4ed6\u7248\u672c\u53f7\u6216tag\uff0c\u9ed8\u8ba4\u4e3alatest\uff0c\u5373\u6700\u8fd1\u4e00\u6b21\u6784\u5efa\u7684\u7248\u672c\u3002"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"String"),(0,l.kt)("td",{parentName:"tr",align:null},"latest")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"project_uri"),(0,l.kt)("td",{parentName:"tr",align:null},"Project URI"),(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"swcli project select"),"\u547d\u4ee4\u8bbe\u5b9a\u7684project"),(0,l.kt)("td",{parentName:"tr",align:null},"String"),(0,l.kt)("td",{parentName:"tr",align:null})),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"runtime_uri"),(0,l.kt)("td",{parentName:"tr",align:null},"Runtime URI\uff0c\u82e5\u8bbe\u7f6e\uff0c\u5219\u8868\u793a\u6570\u636e\u96c6\u6784\u5efa\u7684\u65f6\u5019\u4f1a\u4f7f\u7528\u8be5Runtime\u63d0\u4f9b\u7684\u8fd0\u884c\u65f6\u73af\u5883\uff1b\u82e5\u4e0d\u8bbe\u7f6e\uff0c\u5219\u4f7f\u7528\u5f53\u524dshell\u73af\u5883\u4f5c\u4e3a\u8fd0\u884c\u65f6"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5426"),(0,l.kt)("td",{parentName:"tr",align:null},"String"),(0,l.kt)("td",{parentName:"tr",align:null})))),(0,l.kt)("p",null,"\u5f53handler\u4e3a\u4e00\u4e2a\u51fd\u6570\u65f6\uff0c\u9700\u8981\u8be5\u51fd\u6570\u8fd4\u56de\u4e00\u4e2aGenerator\uff08\u63a8\u8350\u505a\u6cd5\uff09\u6216\u4e00\u4e2a\u53ef\u8fed\u4ee3\u7684\u5bf9\u8c61\uff08\u6bd4\u5982\u4e00\u4e2a\u5217\u8868\uff09\u3002Starwhale SDK\u4f1a\u6839\u636e\u51fd\u6570\u8fd4\u56de\u503c\u5224\u65ad\u9996\u4e2a\u5143\u7d20\u4e3a ",(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale.Link")," \u7c7b\u578b\u65f6\uff0c\u6784\u5efaremote-link\u6216user-raw\u683c\u5f0f\u7684\u6570\u636e\u96c6\uff0c\u5426\u5219\u6784\u5efauser-raw\u683c\u5f0f\u7684\u6570\u636e\u96c6\u3002\u4e0d\u652f\u6301\u6df7\u5408\u683c\u5f0f\u7684\u6570\u636e\u96c6\u3002"),(0,l.kt)("h3",{id:"32-\u4f7f\u7528\u793a\u4f8b"},"3.2 \u4f7f\u7528\u793a\u4f8b"),(0,l.kt)("h4",{id:"33-\u6700\u7b80\u793a\u4f8b"},"3.3 \u6700\u7b80\u793a\u4f8b"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-yaml"},"name: helloworld\nhandler: dataset:ExampleProcessExecutor\n")),(0,l.kt)("p",null,"helloworld\u7684\u6570\u636e\u96c6\uff0c\u4f7f\u7528dataset.yaml\u76ee\u5f55\u4e2ddataset.py\u6587\u4ef6\u4e2d\u7684 ",(0,l.kt)("inlineCode",{parentName:"p"},"ExampleProcessExecutor")," \u7c7b\u8fdb\u884c\u6570\u636e\u6784\u5efa\u3002"),(0,l.kt)("h4",{id:"34-mnist\u6570\u636e\u96c6\u6784\u5efa\u793a\u4f8b"},"3.4 MNIST\u6570\u636e\u96c6\u6784\u5efa\u793a\u4f8b"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-yaml"},'name: mnist\nhandler: mnist.dataset:DatasetProcessExecutor\n\ndesc: MNIST data and label test dataset\n\nattr:\n  alignment_size: 1k\n  volume_size: 4M\n  data_mime_type: "x/grayscale"\n')),(0,l.kt)("h4",{id:"35-handler\u4e3agenerator-function\u7684\u4f8b\u5b50"},"3.5 handler\u4e3agenerator function\u7684\u4f8b\u5b50"),(0,l.kt)("p",null,"dataset.yaml \u5185\u5bb9\uff1a"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-yaml"},"name: helloworld\nhandler: dataset:iter_item\n")),(0,l.kt)("p",null,"dataset.py \u5185\u5bb9\uff1a"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-python"},'def iter_item():\n    for i in range(10):\n        yield {"img": f"image-{i}".encode(), "label": i}\n')),(0,l.kt)("p",null,"\u672c\u4f8b\u4e2d\uff0chandler\u4e3a\u4e00\u4e2agenerator function\uff0cStarwhale SDK\u6839\u636e\u9996\u4e2ayield\u51fa\u6765\u7684\u5143\u7d20\u4e3a\u975e",(0,l.kt)("inlineCode",{parentName:"p"},"Starwhale.Link"),"\u7c7b\u578b\uff0c\u7b49\u540c\u4e8e\u7ee7\u627f ",(0,l.kt)("inlineCode",{parentName:"p"},"starwhale.SWDSBinBuildExecutor")," \u7c7b\u3002"),(0,l.kt)("h2",{id:"4-starwhale-dataset-viewer"},"4. Starwhale Dataset Viewer"),(0,l.kt)("p",null,"\u76ee\u524dCloud Instance\u4e2dWeb UI\u53ef\u4ee5\u5bf9\u6570\u636e\u96c6\u8fdb\u884c\u53ef\u89c6\u5316\u5c55\u793a\uff0c\u76ee\u524d\u53ea\u6709\u4f7f\u7528Python SDK\u7684",(0,l.kt)("a",{parentName:"p",href:"/zh/docs/reference/sdk/data_type"},"DataType")," \u624d\u80fd\u88ab\u524d\u7aef\u6b63\u786e\u7684\u89e3\u91ca\uff0c\u6620\u5c04\u5173\u7cfb\u5982\u4e0b\uff1a"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},"Image\uff1a\u5c55\u793a\u7f29\u7565\u56fe\u3001\u653e\u5927\u56fe\u3001MASK\u7c7b\u578b\u56fe\u7247\uff0c\u652f\u6301 ",(0,l.kt)("inlineCode",{parentName:"li"},"image/png"),"\u3001",(0,l.kt)("inlineCode",{parentName:"li"},"image/jpeg"),"\u3001",(0,l.kt)("inlineCode",{parentName:"li"},"image/webp"),"\u3001",(0,l.kt)("inlineCode",{parentName:"li"},"image/svg+xml"),"\u3001",(0,l.kt)("inlineCode",{parentName:"li"},"image/gif"),"\u3001",(0,l.kt)("inlineCode",{parentName:"li"},"image/apng"),"\u3001",(0,l.kt)("inlineCode",{parentName:"li"},"image/avif")," \u683c\u5f0f\u3002"),(0,l.kt)("li",{parentName:"ul"},"Audio\uff1a\u5c55\u793a\u4e3a\u97f3\u9891wave\u56fe\uff0c\u53ef\u64ad\u653e\uff0c\u652f\u6301 ",(0,l.kt)("inlineCode",{parentName:"li"},"audio/mp3")," \u548c ",(0,l.kt)("inlineCode",{parentName:"li"},"audio/wav")," \u683c\u5f0f\u3002"),(0,l.kt)("li",{parentName:"ul"},"GrayscaleImage\uff1a\u5c55\u793a\u7070\u5ea6\u56fe\uff0c\u652f\u6301 ",(0,l.kt)("inlineCode",{parentName:"li"},"x/grayscale")," \u683c\u5f0f\u3002"),(0,l.kt)("li",{parentName:"ul"},"Text\uff1a\u5c55\u793a\u6587\u672c\uff0c\u652f\u6301 ",(0,l.kt)("inlineCode",{parentName:"li"},"text/plain")," \u683c\u5f0f\uff0c\u8bbe\u7f6e\u8bbe\u7f6e\u7f16\u7801\u683c\u5f0f\uff0c\u9ed8\u8ba4\u4e3autf-8\u3002"),(0,l.kt)("li",{parentName:"ul"},"Binary\u548cBytes\uff1a\u6682\u4e0d\u652f\u6301\u5c55\u793a\u3002"),(0,l.kt)("li",{parentName:"ul"},"Link\uff1a\u4e0a\u8ff0\u51e0\u79cd\u591a\u5a92\u4f53\u7c7b\u578b\u90fd\u652f\u6301\u6307\u5b9alink\u4f5c\u4e3a\u5b58\u50a8\u8def\u5f84\u3002")),(0,l.kt)("h2",{id:"5-starwhale-dataset-\u6570\u636e\u683c\u5f0f"},"5. Starwhale Dataset \u6570\u636e\u683c\u5f0f"),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"SWDS"),"\u4ee5dict\u4f5c\u4e3a\u6bcf\u4e2a\u6570\u636e\u6761\u76ee\u7684\u683c\u5f0f\uff0c\u4f46\u662f\u5bf9key\u548cvalue\u6709\u4e00\u4e9b\u7b80\u5355\u7684\u9650\u5236","[L]","\uff1a"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},"dict\u7684key\u5fc5\u987b\u4e3astr\u7c7b\u578b"),(0,l.kt)("li",{parentName:"ul"},"dict\u7684value\u5fc5\u987b\u662fint/float/bool/str/bytes/dict/list/tuple\u7b49python\u7684\u57fa\u672c\u7c7b\u578b\uff0c\u6216\u8005",(0,l.kt)("a",{parentName:"li",href:"/zh/docs/reference/sdk/data_type"},"Starwhale\u5185\u7f6e\u7684\u6570\u636e\u7c7b\u578b")),(0,l.kt)("li",{parentName:"ul"},"\u4e0d\u540c\u6761\u76ee\u7684\u6570\u636e\u76f8\u540ckey\u7684value\u5fc5\u987b\u6709\u4e00\u81f4\u7684\u6570\u636e\u7c7b\u578b"),(0,l.kt)("li",{parentName:"ul"},"\u5982\u679cvalue\u662flist\u6216\u8005tuple\uff0c\u5176\u5143\u7d20\u7684\u6570\u636e\u7c7b\u578b\u5fc5\u987b\u4e00\u81f4"),(0,l.kt)("li",{parentName:"ul"},"value\u4e3adict\u65f6\uff0c\u5176\u9650\u5236\u7b49\u540c\u4e8e\u9650\u5236","[L]")),(0,l.kt)("p",null,"\u4f8b\u5b50\uff1a"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-python"},'{\n    "img": GrayscaleImage(\n        link=Link(\n            "123",\n            offset=32,\n            size=784,\n            _swds_bin_offset=0,\n            _swds_bin_size=8160,\n        )\n    ),\n    "label": 0,\n}\n')),(0,l.kt)("h3",{id:"51-\u6587\u4ef6\u7c7b\u6570\u636e\u7684\u5904\u7406\u65b9\u5f0f"},"5.1 \u6587\u4ef6\u7c7b\u6570\u636e\u7684\u5904\u7406\u65b9\u5f0f"),(0,l.kt)("p",null,"Starwhale Dataset\u5bf9\u6587\u4ef6\u7c7b\u578b\u7684\u6570\u636e\u8fdb\u884c\u4e86\u7279\u6b8a\u5904\u7406\uff0c\u7528\u6237\u5982\u679c\u4f60\u4e0d\u5173\u5fc3Starwhale\u7684\u5b9e\u73b0\u65b9\u5f0f\uff0c\u53ef\u4ee5\u5ffd\u7565\u672c\u5c0f\u8282\u3002"),(0,l.kt)("p",null,"\u6839\u636e\u5b9e\u9645\u4f7f\u7528\u573a\u666f\uff0cStarwhale Dataset \u5bf9\u57fa\u7c7b\u4e3a",(0,l.kt)("inlineCode",{parentName:"p"},"starwhale.BaseArtifact"),"\u7684\u6587\u4ef6\u7c7b\u6570\u636e\u6709\u4e09\u79cd\u5904\u7406\u65b9\u5f0f\uff1a"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},"bytes\uff1aStarwhale\u4ee5\u81ea\u5df1\u7684\u4e8c\u8fdb\u5236\u683c\u5f0f\u5c06bytes\u5408\u5e76\u6210\u82e5\u5e72\u4e2a\u5927\u6587\u4ef6\uff0c\u80fd\u9ad8\u6548\u7684\u8fdb\u884c\u7d22\u5f15\u3001\u5207\u7247\u548c\u52a0\u8f7d\u3002"),(0,l.kt)("li",{parentName:"ul"},"\u672c\u5730\u6587\u4ef6\uff1a\u4e0d\u6539\u53d8\u539f\u59cb\u7684\u6570\u636e\u683c\u5f0f\uff0c\u53ea\u662f\u5efa\u7acb\u7d22\u5f15\u5173\u7cfb\uff0c\u63d0\u4f9b\u6570\u636e\u7c7b\u578b\u62bd\u8c61\uff0c\u540c\u65f6\u6570\u636e\u96c6\u5206\u53d1\u7684\u65f6\u5019\u4f1a\u643a\u5e26\u539f\u59cb\u6570\u636e\uff0c\u4fbf\u4e8e\u8fdb\u884c\u5206\u4eab\u3002"),(0,l.kt)("li",{parentName:"ul"},"\u8fdc\u7aef\u6587\u4ef6\uff1a\u6ee1\u8db3\u7528\u6237\u7684\u539f\u59cb\u6570\u636e\u5b58\u653e\u5728\u67d0\u4e9b\u5916\u90e8\u5b58\u50a8\u4e0a\uff0c\u6bd4\u5982OSS\u6216NAS\u7b49\uff0c\u539f\u59cb\u6570\u636e\u8f83\u591a\uff0c\u4e0d\u65b9\u4fbf\u642c\u8fc1\u6216\u8005\u5df2\u7ecf\u7528\u4e00\u4e9b\u5185\u90e8\u7684\u6570\u636e\u96c6\u5b9e\u73b0\u8fdb\u884c\u5c01\u88c5\u8fc7\uff0c\u90a3\u4e48\u53ea\u9700\u8981\u5728\u6570\u636e\u4e2d\u4f7f\u7528link\uff0c\u5c31\u80fd\u5efa\u7acb\u7d22\u5f15\u3002")))}u.isMDXComponent=!0},2217:function(t,e,a){e.Z=a.p+"assets/images/dataset-workflow-4139e94939e7a1421926e7dea4401e57.jpg"},3439:function(t,e,a){e.Z=a.p+"assets/images/mlops-users-0df10d6967d83a75542b3f874d6f34f0.png"},5713:function(t,e,a){e.Z=a.p+"assets/images/swds-tree-f7145cd8ad8cfea99290ad923da88298.png"}}]);