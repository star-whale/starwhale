"use strict";(self.webpackChunkstarwhale_docs=self.webpackChunkstarwhale_docs||[]).push([[584],{3905:function(e,t,n){n.d(t,{Zo:function(){return u},kt:function(){return d}});var r=n(7294);function a(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function l(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function c(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?l(Object(n),!0).forEach((function(t){a(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):l(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function o(e,t){if(null==e)return{};var n,r,a=function(e,t){if(null==e)return{};var n,r,a={},l=Object.keys(e);for(r=0;r<l.length;r++)n=l[r],t.indexOf(n)>=0||(a[n]=e[n]);return a}(e,t);if(Object.getOwnPropertySymbols){var l=Object.getOwnPropertySymbols(e);for(r=0;r<l.length;r++)n=l[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(a[n]=e[n])}return a}var p=r.createContext({}),i=function(e){var t=r.useContext(p),n=t;return e&&(n="function"==typeof e?e(t):c(c({},t),e)),n},u=function(e){var t=i(e.components);return r.createElement(p.Provider,{value:t},e.children)},m={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},s=r.forwardRef((function(e,t){var n=e.components,a=e.mdxType,l=e.originalType,p=e.parentName,u=o(e,["components","mdxType","originalType","parentName"]),s=i(n),d=a,k=s["".concat(p,".").concat(d)]||s[d]||m[d]||l;return n?r.createElement(k,c(c({ref:t},u),{},{components:n})):r.createElement(k,c({ref:t},u))}));function d(e,t){var n=arguments,a=t&&t.mdxType;if("string"==typeof e||a){var l=n.length,c=new Array(l);c[0]=s;var o={};for(var p in t)hasOwnProperty.call(t,p)&&(o[p]=t[p]);o.originalType=e,o.mdxType="string"==typeof e?e:a,c[1]=o;for(var i=2;i<l;i++)c[i]=n[i];return r.createElement.apply(null,c)}return r.createElement.apply(null,n)}s.displayName="MDXCreateElement"},1333:function(e,t,n){n.r(t),n.d(t,{assets:function(){return u},contentTitle:function(){return p},default:function(){return d},frontMatter:function(){return o},metadata:function(){return i},toc:function(){return m}});var r=n(7462),a=n(3366),l=(n(7294),n(3905)),c=["components"],o={title:"Project\u547d\u4ee4"},p=void 0,i={unversionedId:"reference/cli/project",id:"reference/cli/project",title:"Project\u547d\u4ee4",description:"\u57fa\u672c\u4fe1\u606f",source:"@site/i18n/zh/docusaurus-plugin-content-docs/current/reference/cli/project.md",sourceDirName:"reference/cli",slug:"/reference/cli/project",permalink:"/zh/docs/reference/cli/project",draft:!1,editUrl:"https://github.com/star-whale/starwhale/tree/main/docs/docs/reference/cli/project.md",tags:[],version:"current",frontMatter:{title:"Project\u547d\u4ee4"},sidebar:"mainSidebar",previous:{title:"Instance\u547d\u4ee4",permalink:"/zh/docs/reference/cli/instance"},next:{title:"\u6570\u636e\u96c6\u547d\u4ee4",permalink:"/zh/docs/reference/cli/dataset"}},u={},m=[{value:"\u57fa\u672c\u4fe1\u606f",id:"\u57fa\u672c\u4fe1\u606f",level:2},{value:"\u521b\u5efaProject",id:"\u521b\u5efaproject",level:2},{value:"\u67e5\u770bProject\u8be6\u7ec6\u4fe1\u606f",id:"\u67e5\u770bproject\u8be6\u7ec6\u4fe1\u606f",level:2},{value:"\u5c55\u793aProject\u5217\u8868",id:"\u5c55\u793aproject\u5217\u8868",level:2},{value:"\u5220\u9664Project",id:"\u5220\u9664project",level:2},{value:"\u6062\u590d\u8f6f\u5220\u9664\u7684Project",id:"\u6062\u590d\u8f6f\u5220\u9664\u7684project",level:2},{value:"\u9009\u62e9\u5f53\u524dInstance\u4e0b\u9ed8\u8ba4\u7684Project",id:"\u9009\u62e9\u5f53\u524dinstance\u4e0b\u9ed8\u8ba4\u7684project",level:2}],s={toc:m};function d(e){var t=e.components,n=(0,a.Z)(e,c);return(0,l.kt)("wrapper",(0,r.Z)({},s,n,{components:t,mdxType:"MDXLayout"}),(0,l.kt)("h2",{id:"\u57fa\u672c\u4fe1\u606f"},"\u57fa\u672c\u4fe1\u606f"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli project [OPTIONS] COMMAND [ARGS]...\n")),(0,l.kt)("p",null,"project\u547d\u4ee4\u63d0\u4f9b\u9002\u7528\u4e8eStandalone Instance\u548cCloud Instance\u7684Starwhale Project\u5168\u751f\u547d\u5468\u671f\u7684\u7ba1\u7406\uff0c\u5305\u62ec\u521b\u5efa\u3001\u67e5\u770b\u3001\u9009\u62e9\u9ed8\u8ba4Project\u7b49\u529f\u80fd\u3002\u5728Standalone Instance\u4e2d\uff0cproject \u4ee3\u8868\u5728 ROOTDIR\u4e0b\u7684\u4e00\u4e2a\u76ee\u5f55\uff0c\u91cc\u9762\u5b58\u50a8Runtime\u3001Model\u3001Dataset\u3001Job\u7b49\u4fe1\u606f\uff0cROOTDIR\u9ed8\u8ba4\u8def\u5f84\u662f ",(0,l.kt)("inlineCode",{parentName:"p"},"~/.starwhale")," \u3002project\u547d\u4ee4\u901a\u8fc7HTTP API\u5bf9Cloud Instance\u5bf9\u8c61\u8fdb\u884c\u64cd\u4f5c\u3002"),(0,l.kt)("p",null,(0,l.kt)("strong",{parentName:"p"},"Project URI"),"\u683c\u5f0f: ",(0,l.kt)("inlineCode",{parentName:"p"},"[<Instance URI>/project]<project name>"),"\u3002"),(0,l.kt)("p",null,"project\u5305\u542b\u5982\u4e0b\u5b50\u547d\u4ee4\uff1a"),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"\u547d\u4ee4"),(0,l.kt)("th",{parentName:"tr",align:null},"Standalone"),(0,l.kt)("th",{parentName:"tr",align:null},"Cloud"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"create"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"info"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"list"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"remove"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"recover"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"select"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705"),(0,l.kt)("td",{parentName:"tr",align:null},"\u2705")))),(0,l.kt)("h2",{id:"\u521b\u5efaproject"},"\u521b\u5efaProject"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli project create PROJECT\n")),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"project create")," \u547d\u4ee4\u80fd\u591f\u521b\u5efa\u4e00\u4e2a\u65b0\u7684Project\uff0c",(0,l.kt)("inlineCode",{parentName:"p"},"PROJECT")," \u53c2\u6570\u4e3aProject URI\u3002"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"\u276f swcli project create myproject\n\ud83d\udc4f do successfully\n\u276f swcli project create myproject\n\ud83e\udd3f failed to run, reason:/home/liutianwei/.cache/starwhale/myproject was already existed\n")),(0,l.kt)("h2",{id:"\u67e5\u770bproject\u8be6\u7ec6\u4fe1\u606f"},"\u67e5\u770bProject\u8be6\u7ec6\u4fe1\u606f"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli project info PROJECT\n")),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"project info")," \u547d\u4ee4\u8f93\u51faProject\u8be6\u7ec6\u4fe1\u606f\u3002",(0,l.kt)("inlineCode",{parentName:"p"},"PROJECT")," \u53c2\u6570\u4e3aProject URI\u3002"),(0,l.kt)("h2",{id:"\u5c55\u793aproject\u5217\u8868"},"\u5c55\u793aProject\u5217\u8868"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli project list [OPTIONS]\n")),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"project list")," \u547d\u4ee4\u8f93\u51fa\u5f53\u524d\u9009\u5b9aInstance\u7684Project\u5217\u8868\uff0c\u547d\u4ee4\u53c2\u6570\u5982\u4e0b\uff1a"),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"\u53c2\u6570"),(0,l.kt)("th",{parentName:"tr",align:null},"\u53c2\u6570\u522b\u540d"),(0,l.kt)("th",{parentName:"tr",align:null},"\u5fc5\u8981\u6027"),(0,l.kt)("th",{parentName:"tr",align:null},"\u7c7b\u578b"),(0,l.kt)("th",{parentName:"tr",align:null},"\u9ed8\u8ba4\u503c"),(0,l.kt)("th",{parentName:"tr",align:null},"\u8bf4\u660e"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"--instance")),(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"-i")),(0,l.kt)("td",{parentName:"tr",align:null},"\u274c"),(0,l.kt)("td",{parentName:"tr",align:null},"String"),(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"swcli instance select")," \u9009\u5b9a\u7684Instance"),(0,l.kt)("td",{parentName:"tr",align:null},"Instance URI")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"--page")),(0,l.kt)("td",{parentName:"tr",align:null}),(0,l.kt)("td",{parentName:"tr",align:null},"\u274c"),(0,l.kt)("td",{parentName:"tr",align:null},"Integer"),(0,l.kt)("td",{parentName:"tr",align:null},"1"),(0,l.kt)("td",{parentName:"tr",align:null},"Cloud Instance\u4e2d\u5206\u9875\u663e\u793a\u4e2dpage\u5e8f\u53f7\u3002")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"--size")),(0,l.kt)("td",{parentName:"tr",align:null}),(0,l.kt)("td",{parentName:"tr",align:null},"\u274c"),(0,l.kt)("td",{parentName:"tr",align:null},"Integer"),(0,l.kt)("td",{parentName:"tr",align:null},"20"),(0,l.kt)("td",{parentName:"tr",align:null},"Cloud Instance\u4e2d\u5206\u9875\u663e\u793a\u4e2d\u6bcf\u9875\u6570\u91cf\u3002")))),(0,l.kt)("h2",{id:"\u5220\u9664project"},"\u5220\u9664Project"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli project remove PROJECT\n")),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"project remove")," \u547d\u4ee4\u8f6f\u5220\u9664Project\uff0c\u5728\u6ca1\u6709GC\u4e4b\u524d\uff0c\u90fd\u53ef\u4ee5\u901a\u8fc7 ",(0,l.kt)("inlineCode",{parentName:"p"},"swcli project recover")," \u547d\u4ee4\u8fdb\u884c\u6062\u590d\u3002",(0,l.kt)("inlineCode",{parentName:"p"},"PROJECT")," \u53c2\u6570\u4e3aProject URI\u3002"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"\u276f swcli project remove myproject\n\ud83d\udc36 remove project myproject. You can recover it, don't panic.\n")),(0,l.kt)("h2",{id:"\u6062\u590d\u8f6f\u5220\u9664\u7684project"},"\u6062\u590d\u8f6f\u5220\u9664\u7684Project"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli project recover PROJECT\n")),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"project recover")," \u547d\u4ee4\u6062\u590d\u8f6f\u5220\u9664\u7684Project\u3002",(0,l.kt)("inlineCode",{parentName:"p"},"PROJECT")," \u53c2\u6570\u4e3aProject URI\u3002"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"\u276f swcli project recover myproject\n\ud83d\udc4f recover project myproject\n")),(0,l.kt)("h2",{id:"\u9009\u62e9\u5f53\u524dinstance\u4e0b\u9ed8\u8ba4\u7684project"},"\u9009\u62e9\u5f53\u524dInstance\u4e0b\u9ed8\u8ba4\u7684Project"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli project select PROJECT\n")),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"project select")," \u547d\u4ee4\u9009\u62e9\u5f53\u524dInstance\u4e0b\u9ed8\u8ba4\u7684Project\uff0c\u8bbe\u7f6e\u540e\u5982\u679c\u7701\u7565Model URI\u3001Runtime URI\u3001Dataset URI\u4e2d\u7684project \u5b57\u6bb5\uff0c\u5c31\u4f1a\u6839\u636eInstance\u5bf9\u5e94\u7684\u9ed8\u8ba4Project\u8fdb\u884c\u586b\u5145\u3002",(0,l.kt)("inlineCode",{parentName:"p"},"PROJECT")," \u53c2\u6570\u4e3aProject URI\u3002\u5bf9\u4e8eCloud Instance\uff0c\u9700\u8981\u9996\u9009\u767b\u9646Instance\u624d\u80fd\u8fdb\u884cproject select\u3002"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"\u276f swcli project select local/project/self\n\ud83d\udc4f select instance:local, project:self successfully\n")))}d.isMDXComponent=!0}}]);