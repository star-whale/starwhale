"use strict";(self.webpackChunkstarwhale_docs=self.webpackChunkstarwhale_docs||[]).push([[673],{3905:function(e,n,t){t.d(n,{Zo:function(){return c},kt:function(){return h}});var a=t(7294);function r(e,n,t){return n in e?Object.defineProperty(e,n,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[n]=t,e}function l(e,n){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);n&&(a=a.filter((function(n){return Object.getOwnPropertyDescriptor(e,n).enumerable}))),t.push.apply(t,a)}return t}function o(e){for(var n=1;n<arguments.length;n++){var t=null!=arguments[n]?arguments[n]:{};n%2?l(Object(t),!0).forEach((function(n){r(e,n,t[n])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):l(Object(t)).forEach((function(n){Object.defineProperty(e,n,Object.getOwnPropertyDescriptor(t,n))}))}return e}function i(e,n){if(null==e)return{};var t,a,r=function(e,n){if(null==e)return{};var t,a,r={},l=Object.keys(e);for(a=0;a<l.length;a++)t=l[a],n.indexOf(t)>=0||(r[t]=e[t]);return r}(e,n);if(Object.getOwnPropertySymbols){var l=Object.getOwnPropertySymbols(e);for(a=0;a<l.length;a++)t=l[a],n.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(r[t]=e[t])}return r}var s=a.createContext({}),p=function(e){var n=a.useContext(s),t=n;return e&&(t="function"==typeof e?e(n):o(o({},n),e)),t},c=function(e){var n=p(e.components);return a.createElement(s.Provider,{value:n},e.children)},u={inlineCode:"code",wrapper:function(e){var n=e.children;return a.createElement(a.Fragment,{},n)}},d=a.forwardRef((function(e,n){var t=e.components,r=e.mdxType,l=e.originalType,s=e.parentName,c=i(e,["components","mdxType","originalType","parentName"]),d=p(t),h=r,m=d["".concat(s,".").concat(h)]||d[h]||u[h]||l;return t?a.createElement(m,o(o({ref:n},c),{},{components:t})):a.createElement(m,o({ref:n},c))}));function h(e,n){var t=arguments,r=n&&n.mdxType;if("string"==typeof e||r){var l=t.length,o=new Array(l);o[0]=d;var i={};for(var s in n)hasOwnProperty.call(n,s)&&(i[s]=n[s]);i.originalType=e,i.mdxType="string"==typeof e?e:r,o[1]=i;for(var p=2;p<l;p++)o[p]=t[p];return a.createElement.apply(null,o)}return a.createElement.apply(null,t)}d.displayName="MDXCreateElement"},2969:function(e,n,t){t.r(n),t.d(n,{assets:function(){return c},contentTitle:function(){return s},default:function(){return h},frontMatter:function(){return i},metadata:function(){return p},toc:function(){return u}});var a=t(7462),r=t(3366),l=(t(7294),t(3905)),o=["components"],i={title:"Standalone Installing"},s=void 0,p={unversionedId:"guides/install/standalone",id:"guides/install/standalone",title:"Standalone Installing",description:"We can use swcli to complete all tasks for Starwhale Standalone mode. swcli is written by pure python3, which can be installed easily by the pip command.",source:"@site/docs/guides/install/standalone.md",sourceDirName:"guides/install",slug:"/guides/install/standalone",permalink:"/docs/guides/install/standalone",draft:!1,editUrl:"https://github.com/star-whale/starwhale/tree/main/docs/docs/guides/install/standalone.md",tags:[],version:"current",frontMatter:{title:"Standalone Installing"},sidebar:"mainSidebar",previous:{title:"Evaluation",permalink:"/docs/guides/evaluation"},next:{title:"Helm Charts Installation",permalink:"/docs/guides/install/helm-charts"}},c={},u=[{value:"Prerequisites",id:"prerequisites",level:2},{value:"Install Starwhale with venv",id:"install-starwhale-with-venv",level:2},{value:"Install Starwhale with conda",id:"install-starwhale-with-conda",level:2},{value:"Upgrade Starwhale",id:"upgrade-starwhale",level:2},{value:"Remove Starwhale",id:"remove-starwhale",level:2}],d={toc:u};function h(e){var n=e.components,t=(0,r.Z)(e,o);return(0,l.kt)("wrapper",(0,a.Z)({},d,t,{components:n,mdxType:"MDXLayout"}),(0,l.kt)("p",null,"We can use ",(0,l.kt)("inlineCode",{parentName:"p"},"swcli")," to complete all tasks for Starwhale Standalone mode. ",(0,l.kt)("inlineCode",{parentName:"p"},"swcli")," is written by pure python3, which can be installed easily by the ",(0,l.kt)("inlineCode",{parentName:"p"},"pip")," command.\nHere are some installation tips that can help you get a cleaner, unambiguous, no dependency conflicts ",(0,l.kt)("inlineCode",{parentName:"p"},"swcli")," python environment."),(0,l.kt)("div",{className:"admonition admonition-caution alert alert--warning"},(0,l.kt)("div",{parentName:"div",className:"admonition-heading"},(0,l.kt)("h5",{parentName:"div"},(0,l.kt)("span",{parentName:"h5",className:"admonition-icon"},(0,l.kt)("svg",{parentName:"span",xmlns:"http://www.w3.org/2000/svg",width:"16",height:"16",viewBox:"0 0 16 16"},(0,l.kt)("path",{parentName:"svg",fillRule:"evenodd",d:"M8.893 1.5c-.183-.31-.52-.5-.887-.5s-.703.19-.886.5L.138 13.499a.98.98 0 0 0 0 1.001c.193.31.53.501.886.501h13.964c.367 0 .704-.19.877-.5a1.03 1.03 0 0 0 .01-1.002L8.893 1.5zm.133 11.497H6.987v-2.003h2.039v2.003zm0-3.004H6.987V5.987h2.039v4.006z"}))),"Installing Advice")),(0,l.kt)("div",{parentName:"div",className:"admonition-content"},(0,l.kt)("p",{parentName:"div"},"DO NOT install Starwhale in your system's global Python environment. It will cause a python dependency conflict problem."))),(0,l.kt)("p",null,"We recommend you build an independent virutalenv or conda environment to install Starwhale."),(0,l.kt)("h2",{id:"prerequisites"},"Prerequisites"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},"Python 3.7+"),(0,l.kt)("li",{parentName:"ul"},"Linux or macOS X"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("a",{parentName:"li",href:"https://conda.io/"},"Conda")," (optional)")),(0,l.kt)("p",null,"In the Ubuntu system, you can run the following commands:"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"sudo apt-get install python3 python3-venv python3-pip\n\n#If you want to install multi python versions\nsudo add-apt-repository -y ppa:deadsnakes/ppa\nsudo apt-get update\nsudo apt-get install -y python3.7 python3.8 python3.9 python3-pip python3-venv python3.8-venv python3.7-venv python3.9-venv\n")),(0,l.kt)("p",null,"Starwhale works on macOS X. If you run into issues with the default system Python3 on macOS, try installing Python3 through the homebrew:"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"brew install python3\n")),(0,l.kt)("h2",{id:"install-starwhale-with-venv"},"Install Starwhale with venv"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"python3 -m venv ~/.cache/venv/starwhale\nsource ~/.cache/venv/starwhale/bin/activate\npython3 -m pip install --pre starwhale\n\nswcli --version\n\nsudo rm -rf /usr/local/bin/swcli\nsudo ln -s `which swcli` /usr/local/bin/\n")),(0,l.kt)("h2",{id:"install-starwhale-with-conda"},"Install Starwhale with conda"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"conda create --name starwhale --yes  python=3.9\nconda activate starwhale\npython3 -m pip install --pre starwhale\n\nswcli --version\n\nsudo rm -rf /usr/local/bin/swcli\nsudo ln -s `which swcli` /usr/local/bin/\n")),(0,l.kt)("p",null,"\ud83d\udc4f Now, you can use ",(0,l.kt)("inlineCode",{parentName:"p"},"swcli")," in the global environment."),(0,l.kt)("h2",{id:"upgrade-starwhale"},"Upgrade Starwhale"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"#for venv\n~/.cache/venv/starwhale/bin/python3 -m pip install --pre --upgrade starwhale\n\n#for conda\nconda run -n starwhale python3 -m pip install --pre --upgrade starwhale\n")),(0,l.kt)("h2",{id:"remove-starwhale"},"Remove Starwhale"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"python3 -m pip remove starwhale\n\nrm -rf ~/.config/starwhale\nrm -rf ~/.cache/starwhale\n")))}h.isMDXComponent=!0}}]);