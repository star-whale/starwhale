"use strict";(self.webpackChunkstarwhale_docs=self.webpackChunkstarwhale_docs||[]).push([[437],{3905:function(e,t,a){a.d(t,{Zo:function(){return u},kt:function(){return d}});var n=a(7294);function r(e,t,a){return t in e?Object.defineProperty(e,t,{value:a,enumerable:!0,configurable:!0,writable:!0}):e[t]=a,e}function l(e,t){var a=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),a.push.apply(a,n)}return a}function i(e){for(var t=1;t<arguments.length;t++){var a=null!=arguments[t]?arguments[t]:{};t%2?l(Object(a),!0).forEach((function(t){r(e,t,a[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(a)):l(Object(a)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(a,t))}))}return e}function s(e,t){if(null==e)return{};var a,n,r=function(e,t){if(null==e)return{};var a,n,r={},l=Object.keys(e);for(n=0;n<l.length;n++)a=l[n],t.indexOf(a)>=0||(r[a]=e[a]);return r}(e,t);if(Object.getOwnPropertySymbols){var l=Object.getOwnPropertySymbols(e);for(n=0;n<l.length;n++)a=l[n],t.indexOf(a)>=0||Object.prototype.propertyIsEnumerable.call(e,a)&&(r[a]=e[a])}return r}var o=n.createContext({}),c=function(e){var t=n.useContext(o),a=t;return e&&(a="function"==typeof e?e(t):i(i({},t),e)),a},u=function(e){var t=c(e.components);return n.createElement(o.Provider,{value:t},e.children)},p={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},m=n.forwardRef((function(e,t){var a=e.components,r=e.mdxType,l=e.originalType,o=e.parentName,u=s(e,["components","mdxType","originalType","parentName"]),m=c(a),d=r,h=m["".concat(o,".").concat(d)]||m[d]||p[d]||l;return a?n.createElement(h,i(i({ref:t},u),{},{components:a})):n.createElement(h,i({ref:t},u))}));function d(e,t){var a=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var l=a.length,i=new Array(l);i[0]=m;var s={};for(var o in t)hasOwnProperty.call(t,o)&&(s[o]=t[o]);s.originalType=e,s.mdxType="string"==typeof e?e:r,i[1]=s;for(var c=2;c<l;c++)i[c]=a[c];return n.createElement.apply(null,i)}return n.createElement.apply(null,a)}m.displayName="MDXCreateElement"},6708:function(e,t,a){a.r(t),a.d(t,{assets:function(){return u},contentTitle:function(){return o},default:function(){return d},frontMatter:function(){return s},metadata:function(){return c},toc:function(){return p}});var n=a(7462),r=a(3366),l=(a(7294),a(3905)),i=["components"],s={title:"On-Premises Quickstart"},o=void 0,c={unversionedId:"quickstart/on-premises",id:"quickstart/on-premises",title:"On-Premises Quickstart",description:"It is recommended to read standalone quickstart first.",source:"@site/docs/quickstart/on-premises.md",sourceDirName:"quickstart",slug:"/quickstart/on-premises",permalink:"/docs/quickstart/on-premises",draft:!1,editUrl:"https://github.com/star-whale/starwhale/tree/main/docs/docs/quickstart/on-premises.md",tags:[],version:"current",frontMatter:{title:"On-Premises Quickstart"},sidebar:"mainSidebar",previous:{title:"Standalone Quickstart",permalink:"/docs/quickstart/standalone"},next:{title:"Pytorch Starwhale Runtime Build",permalink:"/docs/tutorials/pytorch"}},u={},p=[{value:"1. Installing On-Premises",id:"1-installing-on-premises",level:2},{value:"1.1 Prerequisites",id:"11-prerequisites",level:3},{value:"1.2 Start Minikube",id:"12-start-minikube",level:3},{value:"1.3 Installing Starwhale",id:"13-installing-starwhale",level:3},{value:"2. Upload the artifacts to the cloud instance",id:"2-upload-the-artifacts-to-the-cloud-instance",level:2},{value:"2.1 Login Cloud Instance",id:"21-login-cloud-instance",level:3},{value:"2.2 Release artifacts",id:"22-release-artifacts",level:3},{value:"3. Use the web UI to run an evaluation",id:"3-use-the-web-ui-to-run-an-evaluation",level:2},{value:"3.1 Viewing Cloud Instance",id:"31-viewing-cloud-instance",level:3},{value:"3.2 Create an evaluation job",id:"32-create-an-evaluation-job",level:3}],m={toc:p};function d(e){var t=e.components,s=(0,r.Z)(e,i);return(0,l.kt)("wrapper",(0,n.Z)({},m,s,{components:t,mdxType:"MDXLayout"}),(0,l.kt)("div",{className:"admonition admonition-tip alert alert--success"},(0,l.kt)("div",{parentName:"div",className:"admonition-heading"},(0,l.kt)("h5",{parentName:"div"},(0,l.kt)("span",{parentName:"h5",className:"admonition-icon"},(0,l.kt)("svg",{parentName:"span",xmlns:"http://www.w3.org/2000/svg",width:"12",height:"16",viewBox:"0 0 12 16"},(0,l.kt)("path",{parentName:"svg",fillRule:"evenodd",d:"M6.5 0C3.48 0 1 2.19 1 5c0 .92.55 2.25 1 3 1.34 2.25 1.78 2.78 2 4v1h5v-1c.22-1.22.66-1.75 2-4 .45-.75 1-2.08 1-3 0-2.81-2.48-5-5.5-5zm3.64 7.48c-.25.44-.47.8-.67 1.11-.86 1.41-1.25 2.06-1.45 3.23-.02.05-.02.11-.02.17H5c0-.06 0-.13-.02-.17-.2-1.17-.59-1.83-1.45-3.23-.2-.31-.42-.67-.67-1.11C2.44 6.78 2 5.65 2 5c0-2.2 2.02-4 4.5-4 1.22 0 2.36.42 3.22 1.19C10.55 2.94 11 3.94 11 5c0 .66-.44 1.78-.86 2.48zM4 14h5c-.23 1.14-1.3 2-2.5 2s-2.27-.86-2.5-2z"}))),"tip")),(0,l.kt)("div",{parentName:"div",className:"admonition-content"},(0,l.kt)("p",{parentName:"div"},"It is recommended to read ",(0,l.kt)("a",{parentName:"p",href:"/docs/quickstart/standalone"},"standalone quickstart")," first."))),(0,l.kt)("h2",{id:"1-installing-on-premises"},"1. Installing On-Premises"),(0,l.kt)("p",null,"Starwhale provides two ways to install an On-Premises instance in your private cluster:"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("p",{parentName:"li"},"For Kubernetes:"),(0,l.kt)("ul",{parentName:"li"},(0,l.kt)("li",{parentName:"ul"},"Standard Kubernetes Cluster: A pre-deployed Kubernetes cluster is required."),(0,l.kt)("li",{parentName:"ul"},"Minikube: You should have minikube and docker installed on your machine."),(0,l.kt)("li",{parentName:"ul"},"For more deployment details of Kubernetes, you can refer to this ",(0,l.kt)("a",{parentName:"li",href:"/docs/guides/install/helm-charts"},"doc"),".")))),(0,l.kt)("div",{className:"admonition admonition-note alert alert--secondary"},(0,l.kt)("div",{parentName:"div",className:"admonition-heading"},(0,l.kt)("h5",{parentName:"div"},(0,l.kt)("span",{parentName:"h5",className:"admonition-icon"},(0,l.kt)("svg",{parentName:"span",xmlns:"http://www.w3.org/2000/svg",width:"14",height:"16",viewBox:"0 0 14 16"},(0,l.kt)("path",{parentName:"svg",fillRule:"evenodd",d:"M6.3 5.69a.942.942 0 0 1-.28-.7c0-.28.09-.52.28-.7.19-.18.42-.28.7-.28.28 0 .52.09.7.28.18.19.28.42.28.7 0 .28-.09.52-.28.7a1 1 0 0 1-.7.3c-.28 0-.52-.11-.7-.3zM8 7.99c-.02-.25-.11-.48-.31-.69-.2-.19-.42-.3-.69-.31H6c-.27.02-.48.13-.69.31-.2.2-.3.44-.31.69h1v3c.02.27.11.5.31.69.2.2.42.31.69.31h1c.27 0 .48-.11.69-.31.2-.19.3-.42.31-.69H8V7.98v.01zM7 2.3c-3.14 0-5.7 2.54-5.7 5.68 0 3.14 2.56 5.7 5.7 5.7s5.7-2.55 5.7-5.7c0-3.15-2.56-5.69-5.7-5.69v.01zM7 .98c3.86 0 7 3.14 7 7s-3.14 7-7 7-7-3.12-7-7 3.14-7 7-7z"}))),"s")),(0,l.kt)("div",{parentName:"div",className:"admonition-content"},(0,l.kt)("p",{parentName:"div"},"In this tutorial, minikube is used instead of the standard Kubernetes cluster"))),(0,l.kt)("h3",{id:"11-prerequisites"},"1.1 Prerequisites"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("a",{parentName:"li",href:"https://minikube.sigs.k8s.io/docs/start/"},"Minikube")," 1.25+"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("a",{parentName:"li",href:"https://helm.sh/docs/intro/install/"},"Helm")," 3.2.0+")),(0,l.kt)("h3",{id:"12-start-minikube"},"1.2 Start Minikube"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"minikube start\n")),(0,l.kt)("p",null,"For users in the mainland of China, please add these startup parameters\uff1a",(0,l.kt)("inlineCode",{parentName:"p"},"--image-mirror-country=cn --image-repository=registry.cn-hangzhou.aliyuncs.com/google_containers"),". If there is no kubectl bin in your machine, you may use ",(0,l.kt)("inlineCode",{parentName:"p"},"minikube kubectl")," or ",(0,l.kt)("inlineCode",{parentName:"p"},'alias kubectl="minikube kubectl --"')," alias command."),(0,l.kt)("h3",{id:"13-installing-starwhale"},"1.3 Installing Starwhale"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"helm repo add starwhale https://star-whale.github.io/charts\nhelm repo update\nhelm install --devel my-starwhale starwhale/starwhale -n starwhale --create-namespace --set minikube.enabled=true\n")),(0,l.kt)("p",null,"After the installation is successful, the following prompt message appears:"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"NAME: my-starwhale\nLAST DEPLOYED: Thu Jun 23 14:48:02 2022\nNAMESPACE: starwhale\nSTATUS: deployed\nREVISION: 1\nNOTES:\n******************************************\nChart Name: starwhale\nChart Version: 0.3.0\nApp Version: 0.3.0\nStarwhale Image:\n  - server: ghcr.io/star-whale/server:0.3.0\n\n******************************************\nWeb Visit:\n  - starwhale controller: http://console.minikube.local\n  - minio admin: http://minio.pre.intra.starwhale.ai\n\nPort Forward Visist:\n  - starwhale controller:\n    - run: kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082\n    - visit: http://localhost:8082\n  - minio admin:\n    - run: kubectl port-forward --namespace starwhale svc/my-starwhale-minio 9001:9001\n    - visit: http://localhost:9001\n  - mysql:\n    - run: kubectl port-forward --namespace starwhale svc/my-starwhale-mysql 3306:3306\n    - visit: mysql -h 127.0.0.1 -P 3306 -ustarwhale -pstarwhale\n\n******************************************\nLogin Info:\n- starwhale: u:starwhale, p:abcd1234\n- minio admin: u:minioadmin, p:minioadmin\n\n*_* Enjoy using Starwhale. *_*\n")),(0,l.kt)("p",null,"Then keep checking the minikube service status until all pods are running."),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"kubectl get pods -n starwhale\n")),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:"left"},"NAME"),(0,l.kt)("th",{parentName:"tr",align:null},"READY"),(0,l.kt)("th",{parentName:"tr",align:null},"STATUS"),(0,l.kt)("th",{parentName:"tr",align:null},"RESTARTS"),(0,l.kt)("th",{parentName:"tr",align:null},"AGE"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:"left"},"my-starwhale-controller-7d864558bc-vxvb8"),(0,l.kt)("td",{parentName:"tr",align:null},"1/1"),(0,l.kt)("td",{parentName:"tr",align:null},"Running"),(0,l.kt)("td",{parentName:"tr",align:null},"0"),(0,l.kt)("td",{parentName:"tr",align:null},"1m")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:"left"},"my-starwhale-minio-7d45db75f6-7wq9b"),(0,l.kt)("td",{parentName:"tr",align:null},"1/1"),(0,l.kt)("td",{parentName:"tr",align:null},"Running"),(0,l.kt)("td",{parentName:"tr",align:null},"0"),(0,l.kt)("td",{parentName:"tr",align:null},"2m")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:"left"},"my-starwhale-mysql-0"),(0,l.kt)("td",{parentName:"tr",align:null},"1/1"),(0,l.kt)("td",{parentName:"tr",align:null},"Running"),(0,l.kt)("td",{parentName:"tr",align:null},"0"),(0,l.kt)("td",{parentName:"tr",align:null},"2m")))),(0,l.kt)("p",null,"Make the Starwhale controller accessible locally with the following command:"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"kubectl port-forward --namespace starwhale svc/my-starwhale-controller 8082:8082\n")),(0,l.kt)("h2",{id:"2-upload-the-artifacts-to-the-cloud-instance"},"2. Upload the artifacts to the cloud instance"),(0,l.kt)("p",null,"Before starting this tutorial, the following three artifacts should already exist on your machine\uff1a"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},"a starwhale model named mnist"),(0,l.kt)("li",{parentName:"ul"},"a starwhale dataset named mnist"),(0,l.kt)("li",{parentName:"ul"},"a starwhale runtime named pytorch")),(0,l.kt)("p",null,"The above three artifacts are what we built in the ",(0,l.kt)("a",{parentName:"p",href:"/docs/quickstart/standalone"},"standalone tutorial"),"."),(0,l.kt)("h3",{id:"21-login-cloud-instance"},"2.1 Login Cloud Instance"),(0,l.kt)("p",null,"First, log in to the server:"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli instance login --username starwhale --password abcd1234 --alias dev http://localhost:8082\n")),(0,l.kt)("h3",{id:"22-release-artifacts"},"2.2 Release artifacts"),(0,l.kt)("p",null,"Start copying the model, dataset, and runtime that we constructed earlier:"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"swcli model copy mnist/version/latest dev/project/starwhale\nswcli dataset copy mnist/version/latest dev/project/starwhale\nswcli runtime copy pytorch/version/latest dev/project/starwhale\n")),(0,l.kt)("h2",{id:"3-use-the-web-ui-to-run-an-evaluation"},"3. Use the web UI to run an evaluation"),(0,l.kt)("h3",{id:"31-viewing-cloud-instance"},"3.1 Viewing Cloud Instance"),(0,l.kt)("p",null,"Ok, let's use the username(starwhale) and password(abcd1234) to open the server ",(0,l.kt)("a",{parentName:"p",href:"http://localhost:8082/"},"web UI"),"."),(0,l.kt)("p",null,(0,l.kt)("img",{alt:"console-artifacts.gif",src:a(8380).Z,width:"2060",height:"1112"})),(0,l.kt)("h3",{id:"32-create-an-evaluation-job"},"3.2 Create an evaluation job"),(0,l.kt)("p",null,(0,l.kt)("img",{alt:"console-create-job.gif",src:a(7226).Z,width:"2116",height:"1147"})),(0,l.kt)("p",null,(0,l.kt)("strong",{parentName:"p"},"Congratulations! You have completed the evaluation process for a model.")))}d.isMDXComponent=!0},8380:function(e,t,a){t.Z=a.p+"assets/images/console-artifacts-fd7bf6e54d06dc37d234019e769031e6.gif"},7226:function(e,t,a){t.Z=a.p+"assets/images/console-create-job-b3f6012e26da81d411aa7624990a7087.gif"}}]);