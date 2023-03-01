"use strict";(self.webpackChunkstarwhale_docs=self.webpackChunkstarwhale_docs||[]).push([[1317],{3905:function(e,t,n){n.d(t,{Zo:function(){return u},kt:function(){return m}});var a=n(7294);function l(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function r(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);t&&(a=a.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,a)}return n}function i(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?r(Object(n),!0).forEach((function(t){l(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):r(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function o(e,t){if(null==e)return{};var n,a,l=function(e,t){if(null==e)return{};var n,a,l={},r=Object.keys(e);for(a=0;a<r.length;a++)n=r[a],t.indexOf(n)>=0||(l[n]=e[n]);return l}(e,t);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);for(a=0;a<r.length;a++)n=r[a],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(l[n]=e[n])}return l}var s=a.createContext({}),p=function(e){var t=a.useContext(s),n=t;return e&&(n="function"==typeof e?e(t):i(i({},t),e)),n},u=function(e){var t=p(e.components);return a.createElement(s.Provider,{value:t},e.children)},d={inlineCode:"code",wrapper:function(e){var t=e.children;return a.createElement(a.Fragment,{},t)}},c=a.forwardRef((function(e,t){var n=e.components,l=e.mdxType,r=e.originalType,s=e.parentName,u=o(e,["components","mdxType","originalType","parentName"]),c=p(n),m=l,h=c["".concat(s,".").concat(m)]||c[m]||d[m]||r;return n?a.createElement(h,i(i({ref:t},u),{},{components:n})):a.createElement(h,i({ref:t},u))}));function m(e,t){var n=arguments,l=t&&t.mdxType;if("string"==typeof e||l){var r=n.length,i=new Array(r);i[0]=c;var o={};for(var s in t)hasOwnProperty.call(t,s)&&(o[s]=t[s]);o.originalType=e,o.mdxType="string"==typeof e?e:l,i[1]=o;for(var p=2;p<r;p++)i[p]=n[p];return a.createElement.apply(null,i)}return a.createElement.apply(null,n)}c.displayName="MDXCreateElement"},9022:function(e,t,n){n.r(t),n.d(t,{assets:function(){return s},contentTitle:function(){return i},default:function(){return d},frontMatter:function(){return r},metadata:function(){return o},toc:function(){return p}});var a=n(3117),l=(n(7294),n(3905));const r={title:"Standalone Quickstart"},i=void 0,o={unversionedId:"quickstart/standalone",id:"version-0.4.0/quickstart/standalone",title:"Standalone Quickstart",description:"1. Installing Starwhale",source:"@site/versioned_docs/version-0.4.0/quickstart/standalone.md",sourceDirName:"quickstart",slug:"/quickstart/standalone",permalink:"/docs/quickstart/standalone",draft:!1,editUrl:"https://github.com/star-whale/starwhale/tree/main/docs/versioned_docs/version-0.4.0/quickstart/standalone.md",tags:[],version:"0.4.0",frontMatter:{title:"Standalone Quickstart"},sidebar:"mainSidebar",next:{title:"On-Premises Quickstart",permalink:"/docs/quickstart/on-premises"}},s={},p=[{value:"1. Installing Starwhale",id:"1-installing-starwhale",level:2},{value:"2. Downloading Examples",id:"2-downloading-examples",level:2},{value:"3. Building Runtime",id:"3-building-runtime",level:2},{value:"4. Building Model",id:"4-building-model",level:2},{value:"5. Building Dataset",id:"5-building-dataset",level:2},{value:"6. Running an Evaluation Job",id:"6-running-an-evaluation-job",level:2}],u={toc:p};function d(e){let{components:t,...r}=e;return(0,l.kt)("wrapper",(0,a.Z)({},u,r,{components:t,mdxType:"MDXLayout"}),(0,l.kt)("h2",{id:"1-installing-starwhale"},"1. Installing Starwhale"),(0,l.kt)("p",null,"Starwhale has three types of instances: Standalone, On-Premises, and Cloud Hosted. Starting with the standalone mode is ideal for quickly understanding and mastering Starwhale.\nYou install Starwhale Standalone by running:"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"python3 -m pip install starwhale\n")),(0,l.kt)("admonition",{type:"note"},(0,l.kt)("p",{parentName:"admonition"},"You can install the alpha version by the ",(0,l.kt)("inlineCode",{parentName:"p"},"--pre")," argument.")),(0,l.kt)("admonition",{type:"note"},(0,l.kt)("p",{parentName:"admonition"},"Starwhale standalone requires Python 3.7~3.10. Currently, Starwhale only supports Linux and macOS. Windows is coming soon.")),(0,l.kt)("p",null,"At the installation stage, we strongly recommend you follow the ",(0,l.kt)("a",{parentName:"p",href:"/docs/guides/install/standalone"},"doc"),"."),(0,l.kt)("h2",{id:"2-downloading-examples"},"2. Downloading Examples"),(0,l.kt)("p",null,"Download Starwhale examples by cloning Starwhale via:"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"git clone https://github.com/star-whale/starwhale.git\ncd starwhale\n")),(0,l.kt)("p",null,"If ",(0,l.kt)("a",{parentName:"p",href:"https://git-lfs.github.com/"},"git-lfs")," has not been previously installed in the local environment(the command is ",(0,l.kt)("inlineCode",{parentName:"p"},"git lfs install"),"), you need to download the trained model file."),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"wget https://media.githubusercontent.com/media/star-whale/starwhale/main/example/mnist/models/mnist_cnn.pt -O example/mnist/models/mnist_cnn.pt\n")),(0,l.kt)("p",null,"We will use ML/DL HelloWorld code ",(0,l.kt)("inlineCode",{parentName:"p"},"MNIST")," to start your Starwhale journey. The following steps are all performed in the ",(0,l.kt)("inlineCode",{parentName:"p"},"starwhale")," directory."),(0,l.kt)("p",null,(0,l.kt)("img",{alt:"Core Workflow",src:n(446).Z,width:"3036",height:"1741"})),(0,l.kt)("h2",{id:"3-building-runtime"},"3. Building Runtime"),(0,l.kt)("p",null,"Runtime example code are in the ",(0,l.kt)("inlineCode",{parentName:"p"},"example/runtime/pytorch")," directory."),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("p",{parentName:"li"},"Build the Starwhale Runtime bundle:"),(0,l.kt)("pre",{parentName:"li"},(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli runtime build .\n"))),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("p",{parentName:"li"},"Check your local Starwhale Runtimes:"),(0,l.kt)("pre",{parentName:"li"},(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli runtime list\nswcli runtime info pytorch/version/latest\n")))),(0,l.kt)("h2",{id:"4-building-model"},"4. Building Model"),(0,l.kt)("p",null,"Model example code are in the ",(0,l.kt)("inlineCode",{parentName:"p"},"example/mnist")," directory."),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("p",{parentName:"li"},"Build a Starwhale Model:"),(0,l.kt)("pre",{parentName:"li"},(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli model build .\n"))),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("p",{parentName:"li"},"Check your local Starwhale Models."),(0,l.kt)("pre",{parentName:"li"},(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli model list\nswcli model info mnist/version/latest\n")))),(0,l.kt)("h2",{id:"5-building-dataset"},"5. Building Dataset"),(0,l.kt)("p",null,"Dataset example code are in the ",(0,l.kt)("inlineCode",{parentName:"p"},"example/mnist")," directory."),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("p",{parentName:"li"},"Download the MNIST raw data:"),(0,l.kt)("pre",{parentName:"li"},(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"mkdir -p data && cd data\nwget http://yann.lecun.com/exdb/mnist/train-images-idx3-ubyte.gz\nwget http://yann.lecun.com/exdb/mnist/train-labels-idx1-ubyte.gz\nwget http://yann.lecun.com/exdb/mnist/t10k-images-idx3-ubyte.gz\nwget http://yann.lecun.com/exdb/mnist/t10k-labels-idx1-ubyte.gz\ngzip -d *.gz\ncd ..\nls -lah data/*\n"))),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("p",{parentName:"li"},"Build a Starwhale Dataset:"),(0,l.kt)("pre",{parentName:"li"},(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli dataset build .\n"))),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("p",{parentName:"li"},"Check your local Starwhale Dataset:"),(0,l.kt)("pre",{parentName:"li"},(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli dataset list\nswcli dataset info mnist/version/latest\n")))),(0,l.kt)("h2",{id:"6-running-an-evaluation-job"},"6. Running an Evaluation Job"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("p",{parentName:"li"},"Create an evaluation job"),(0,l.kt)("pre",{parentName:"li"},(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli -vvv eval run --model mnist/version/latest --dataset mnist/version/latest --runtime pytorch/version/latest\n"))),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("p",{parentName:"li"},"Check the evaluation result"),(0,l.kt)("pre",{parentName:"li"},(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli eval list\nswcli eval info ${version}\n")))),(0,l.kt)("admonition",{type:"tip"},(0,l.kt)("p",{parentName:"admonition"},"When you first use Runtime in the eval run command which maybe use a lot of time to create isolated python environment, download python dependencies. Use the befitting pypi mirror in the ",(0,l.kt)("inlineCode",{parentName:"p"},"~/.pip/pip.conf")," file is a recommend practice.")),(0,l.kt)("p",null,"  \ud83d\udc4f Now, you have completed the basic steps for Starwhale standalone."))}d.isMDXComponent=!0},446:function(e,t,n){t.Z=n.p+"assets/images/standalone-core-workflow-270ac0f0cb5b20dbe5ccd11727e2620b.gif"}}]);