"use strict";(self.webpackChunkstarwhale_docs=self.webpackChunkstarwhale_docs||[]).push([[733],{3905:function(e,t,n){n.d(t,{Zo:function(){return d},kt:function(){return m}});var a=n(7294);function r(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function l(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);t&&(a=a.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,a)}return n}function i(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?l(Object(n),!0).forEach((function(t){r(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):l(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function p(e,t){if(null==e)return{};var n,a,r=function(e,t){if(null==e)return{};var n,a,r={},l=Object.keys(e);for(a=0;a<l.length;a++)n=l[a],t.indexOf(n)>=0||(r[n]=e[n]);return r}(e,t);if(Object.getOwnPropertySymbols){var l=Object.getOwnPropertySymbols(e);for(a=0;a<l.length;a++)n=l[a],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(r[n]=e[n])}return r}var o=a.createContext({}),s=function(e){var t=a.useContext(o),n=t;return e&&(n="function"==typeof e?e(t):i(i({},t),e)),n},d=function(e){var t=s(e.components);return a.createElement(o.Provider,{value:t},e.children)},u={inlineCode:"code",wrapper:function(e){var t=e.children;return a.createElement(a.Fragment,{},t)}},c=a.forwardRef((function(e,t){var n=e.components,r=e.mdxType,l=e.originalType,o=e.parentName,d=p(e,["components","mdxType","originalType","parentName"]),c=s(n),m=r,k=c["".concat(o,".").concat(m)]||c[m]||u[m]||l;return n?a.createElement(k,i(i({ref:t},d),{},{components:n})):a.createElement(k,i({ref:t},d))}));function m(e,t){var n=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var l=n.length,i=new Array(l);i[0]=c;var p={};for(var o in t)hasOwnProperty.call(t,o)&&(p[o]=t[o]);p.originalType=e,p.mdxType="string"==typeof e?e:r,i[1]=p;for(var s=2;s<l;s++)i[s]=n[s];return a.createElement.apply(null,i)}return a.createElement.apply(null,n)}c.displayName="MDXCreateElement"},1367:function(e,t,n){n.r(t),n.d(t,{assets:function(){return d},contentTitle:function(){return o},default:function(){return m},frontMatter:function(){return p},metadata:function(){return s},toc:function(){return u}});var a=n(7462),r=n(3366),l=(n(7294),n(3905)),i=["components"],p={title:"\u6a21\u578b\u8bc4\u6d4b"},o=void 0,s={unversionedId:"reference/sdk/evaluation",id:"reference/sdk/evaluation",title:"\u6a21\u578b\u8bc4\u6d4b",description:"starwhale.PipelineHandler",source:"@site/i18n/zh/docusaurus-plugin-content-docs/current/reference/sdk/evaluation.md",sourceDirName:"reference/sdk",slug:"/reference/sdk/evaluation",permalink:"/zh/docs/reference/sdk/evaluation",draft:!1,editUrl:"https://github.com/star-whale/starwhale/tree/main/docs/docs/reference/sdk/evaluation.md",tags:[],version:"current",frontMatter:{title:"\u6a21\u578b\u8bc4\u6d4b"},sidebar:"mainSidebar",previous:{title:"\u6570\u636e\u96c6\u6784\u5efa\u548c\u52a0\u8f7d",permalink:"/zh/docs/reference/sdk/dataset"},next:{title:"\u5176\u4ed6SDK",permalink:"/zh/docs/reference/sdk/other"}},d={},u=[{value:"starwhale.PipelineHandler",id:"starwhalepipelinehandler",level:2},{value:"starwhale.Context",id:"starwhalecontext",level:2},{value:"starwhale.PPLResultIterator",id:"starwhalepplresultiterator",level:2},{value:"starwhale.multi_classification",id:"starwhalemulti_classification",level:2},{value:"starwhale.step",id:"starwhalestep",level:2}],c={toc:u};function m(e){var t=e.components,n=(0,r.Z)(e,i);return(0,l.kt)("wrapper",(0,a.Z)({},c,n,{components:t,mdxType:"MDXLayout"}),(0,l.kt)("h2",{id:"starwhalepipelinehandler"},"starwhale.PipelineHandler"),(0,l.kt)("p",null,"\u63d0\u4f9b\u9ed8\u8ba4\u7684\u6a21\u578b\u8bc4\u6d4b\u8fc7\u7a0b\u5b9a\u4e49\uff0c\u9700\u8981\u7528\u6237\u5b9e\u73b0 ",(0,l.kt)("inlineCode",{parentName:"p"},"ppl")," \u548c ",(0,l.kt)("inlineCode",{parentName:"p"},"cmp")," \u51fd\u6570\u3002Github\u4e0a\u7684",(0,l.kt)("a",{parentName:"p",href:"https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/api/_impl/model.py"},"\u4ee3\u7801\u94fe\u63a5"),"\u3002"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-python"},"from abc import ABCMeta, abstractmethod\n\nclass PipelineHandler(metaclass=ABCMeta):\n    def __init__(self,\n        ignore_annotations: bool = False,\n        ignore_error: bool = False,\n    ) -> None:\n        ...\n\n    @abstractmethod\n    def ppl(self, data: Any, **kw: Any) -> Any:\n        raise NotImplementedError\n\n    @abstractmethod\n    def cmp(self, ppl_result: PPLResultIterator) -> Any\n        raise NotImplementedError\n")),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"PipelineHandler")," \u7c7b\u5b9e\u4f8b\u5316\u65f6\u53ef\u4ee5\u5b9a\u4e49\u4e24\u4e2a\u53c2\u6570\uff1a\u5f53",(0,l.kt)("inlineCode",{parentName:"p"},"ignore_annotations"),"\u4e3aFalse\u65f6\uff0cPPLResultIterator\u4e2d\u4f1a\u643a\u5e26\u6570\u636e\u96c6\u6240\u5bf9\u5e94\u7684 annotations\u4fe1\u606f\uff0c\u4fdd\u8bc1index\u4e0a\u4e0e\u63a8\u7406\u7ed3\u679c\u662f\u4e00\u4e00\u5bf9\u5e94\u7684\uff1b\u5f53 ",(0,l.kt)("inlineCode",{parentName:"p"},"ignore_error"),"\u4e3aTrue\u662f\uff0c\u4f1a\u5ffd\u7565ppl\u8fc7\u7a0b\u4e2d\u7684\u9519\u8bef\uff0c\u53ef\u4ee5\u89e3\u51b3\u6bd4\u8f83\u5927\u7684\u6570\u636e\u96c6\u6837\u672c\u4e2d\uff0c\u6709\u4e2a\u522b\u6570\u636e\u9519\u8bef\u5bfc\u81f4ppl\u5931\u8d25\uff0c\u8fdb\u800c\u5bfc\u81f4\u65e0\u6cd5\u5b8c\u6210\u8bc4\u6d4b\u7684\u95ee\u9898\u3002"),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"ppl")," \u51fd\u6570\u7528\u6765\u8fdb\u884c\u63a8\u7406\uff0c\u8f93\u5165\u53c2\u6570\u4e3a data\u548ckw\u3002data\u8868\u793a\u6570\u636e\u96c6\u4e2d\u67d0\u4e2a\u6837\u672c\uff0ckw\u4e3a\u4e00\u4e2a\u5b57\u5178\uff0c\u76ee\u524d\u5305\u542b ",(0,l.kt)("inlineCode",{parentName:"p"},"annotations")," \u548c ",(0,l.kt)("inlineCode",{parentName:"p"},"index"),"\u3002\u6bcf\u6761\u6570\u636e\u96c6\u6837\u672c\u90fd\u4f1a\u8c03\u7528",(0,l.kt)("inlineCode",{parentName:"p"},"ppl"),"\u51fd\u6570\uff0c\u8f93\u51fa\u4e3a\u6a21\u578b\u63a8\u7406\u503c\uff0c\u4f1a\u81ea\u52a8\u88ab\u8bb0\u5f55\u548c\u5b58\u50a8\uff0c\u53ef\u4ee5\u5728cmp\u51fd\u6570\u4e2d\u901a\u8fc7 ",(0,l.kt)("inlineCode",{parentName:"p"},"ppl_result")," \u53c2\u6570\u83b7\u53d6\u3002"),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"cmp")," \u51fd\u6570\u4e00\u822c\u7528\u6765\u8fdb\u884c\u63a8\u7406\u7ed3\u679c\u7684\u6c47\u603b\uff0c\u5e76\u4ea7\u751f\u6700\u7ec8\u7684\u8bc4\u6d4b\u62a5\u544a\u6570\u636e\uff0c\u53ea\u4f1a\u8c03\u7528\u4e00\u6b21\u3002",(0,l.kt)("inlineCode",{parentName:"p"},"cmp")," \u51fd\u6570\u7684\u53c2\u6570\u4e3a ",(0,l.kt)("inlineCode",{parentName:"p"},"ppl_result")," \uff0c\u8be5\u503c\u662f ",(0,l.kt)("inlineCode",{parentName:"p"},"PPLResultIterator")," \u7c7b\u578b\uff0c\u53ef\u4ee5\u88ab\u8fed\u4ee3\u3002\u8fed\u4ee3\u51fa\u6765\u7684\u5bf9\u8c61\u4e3a\u4e00\u4e2a\u5b57\u5178\uff0c\u5305\u542b ",(0,l.kt)("inlineCode",{parentName:"p"},"result"),", ",(0,l.kt)("inlineCode",{parentName:"p"},"annotations")," \u548c ",(0,l.kt)("inlineCode",{parentName:"p"},"data_id")," \u4e09\u4e2a\u5143\u7d20\u3002",(0,l.kt)("inlineCode",{parentName:"p"},"result")," \u4e3a ",(0,l.kt)("inlineCode",{parentName:"p"},"ppl")," \u8fd4\u56de\u7684\u5143\u7d20\uff0c\u7531\u4e8e\u4f7f\u7528\u4e86 pickle\u505a\u5e8f\u5217\u5316-\u53cd\u5e8f\u5217\u5316\uff0cdata",'["result"]'," \u53d8\u91cf\u76f4\u63a5\u80fd\u83b7\u53d6ppl\u51fd\u6570return\u7684\u503c\uff1b",(0,l.kt)("inlineCode",{parentName:"p"},"annotations")," \u4e3a\u6784\u5efa\u6570\u636e\u96c6\u65f6\u5199\u5165\u7684\uff0c\u6b64\u9636\u6bb5\u7684result",'["annotations"]',"\u4e3a\u4e00\u4e2adict\u7c7b\u578b\u3002",(0,l.kt)("inlineCode",{parentName:"p"},"data_id")," \u8868\u793a\u6570\u636e\u96c6\u5bf9\u5e94\u7684index\u3002"),(0,l.kt)("p",null,"\u53e6\u5916\uff0c\u5728PipelineHandler\u53ca\u5176\u5b50\u7c7b\u4e2d\u53ef\u4ee5\u8bbf\u95ee ",(0,l.kt)("inlineCode",{parentName:"p"},"self.context")," \u83b7\u53d6 ",(0,l.kt)("inlineCode",{parentName:"p"},"starwhale.Context")," \u7c7b\u578b\u7684\u4e0a\u4e0b\u6587\u4fe1\u606f\u3002"),(0,l.kt)("p",null,"\u5e38\u89c1\u7684\u4f7f\u7528\u65b9\u6cd5\u793a\u4f8b\u5982\u4e0b\uff1a"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-python"},'\nclass Example(PipelineHandler):\n    def __init__(self) -> None:\n        super().__init__()\n        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")\n        self.model = self._load_model(self.device)\n\n    def ppl(self, img: Image, **kw):\n        data_tensor = self._pre(img)\n        output = self.model(data_tensor)\n        return self._post(output)\n\n    def cmp(self, ppl_result):\n        result, label, pr = [], [], []\n        for _data in ppl_result:\n            label.append(_data["annotations"]["label"])\n            result.extend(_data["result"][0])\n            pr.extend(_data["result"][1])\n        return label, result, pr\n\n    def _pre(self, input: Image) -> torch.Tensor:\n        ...\n\n    def _post(self, input):\n        ...\n\n    def _load_model(self, device):\n        ...\n')),(0,l.kt)("h2",{id:"starwhalecontext"},"starwhale.Context"),(0,l.kt)("p",null,"\u6267\u884c\u6a21\u578b\u8bc4\u6d4b\u8fc7\u7a0b\u4e2d\u4f20\u5165\u7684\u4e0a\u4e0b\u6587\u4fe1\u606f\uff0c\u5305\u62ecProject\u3001Task ID\u7b49\u3002Github\u4e0a\u7684",(0,l.kt)("a",{parentName:"p",href:"https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/api/_impl/job.py"},"\u4ee3\u7801\u94fe\u63a5"),"\u3002Context\u7684\u5185\u5bb9\u662f\u81ea\u52a8\u6ce8\u5165\u7684\uff0c\u7528\u6237\u901a\u8fc7 ",(0,l.kt)("inlineCode",{parentName:"p"},"@pass_context")," \u4f7f\u7528context\uff0c\u6216\u5728 \u7ee7\u627f ",(0,l.kt)("inlineCode",{parentName:"p"},"PipelineHandler")," \u7c7b\u5185\u4f7f\u7528\uff0c\u76ee\u524dContext\u53ef\u4ee5\u83b7\u5f97\u5982\u4e0b\u503c\uff1a"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-python"},'\n@pass_context\ndef func(ctx: Context):\n    ...\n    print(ctx.project)\n    print(ctx.version)\n    print(ctx.step)\n    ...\n\nContext(\n    workdir: Path,\n    step: str = "",\n    total: int = 1,\n    index: int = 0,\n    dataset_uris: t.List[str] = [],\n    version: str = "",\n    project: str = "",\n)\n')),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"\u53c2\u6570"),(0,l.kt)("th",{parentName:"tr",align:null},"\u8bf4\u660e"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"project"),(0,l.kt)("td",{parentName:"tr",align:null},"project\u540d\u5b57")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"version"),(0,l.kt)("td",{parentName:"tr",align:null},"Evaluation \u7248\u672c\u53f7")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"step"),(0,l.kt)("td",{parentName:"tr",align:null},"step\u540d\u5b57")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"total"),(0,l.kt)("td",{parentName:"tr",align:null},"step\u4e0b\u6240\u6709\u7684task\u6570\u91cf")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"index"),(0,l.kt)("td",{parentName:"tr",align:null},"\u5f53\u524dtask\u7684\u7d22\u5f15\u7f16\u53f7\uff0c\u4ece\u96f6\u5f00\u59cb")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"dataset_uris"),(0,l.kt)("td",{parentName:"tr",align:null},"dataset uri\u5b57\u7b26\u4e32\u7684\u5217\u8868")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"workdir"),(0,l.kt)("td",{parentName:"tr",align:null},"model.yaml\u6240\u5728\u76ee\u5f55")))),(0,l.kt)("h2",{id:"starwhalepplresultiterator"},"starwhale.PPLResultIterator"),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"cmp"),"\u51fd\u6570\u4e2d\u4f7f\u7528\uff0c\u662f\u4e00\u4e2a\u53ef\u8fed\u4ee3\u7684\u5bf9\u8c61\uff0c\u80fd\u591f\u8f93\u51fa ",(0,l.kt)("inlineCode",{parentName:"p"},"ppl")," \u7ed3\u679c\uff0c\u6570\u636e\u96c6index\u548c\u5bf9\u5e94\u7684\u6570\u636e\u96c6annotations\u3002Github\u4e0a\u7684",(0,l.kt)("a",{parentName:"p",href:"https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/api/_impl/model.py"},"\u4ee3\u7801\u94fe\u63a5"),"\u3002"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-python"},'from starwhale import PipelineHandler, PPLResultIterator\n\nclass Example(PipelineHandler):\n    def cmp(\n        self, ppl_result: PPLResultIterator\n    ) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:\n        result, label, pr = [], [], []\n        for _data in ppl_result:\n            label.append(_data["annotations"]["label"])\n            result.extend(_data["result"][0])\n            pr.extend(_data["result"][1])\n            print(_data["data_id"])\n        return label, result, pr\n\n')),(0,l.kt)("h2",{id:"starwhalemulti_classification"},"starwhale.multi_classification"),(0,l.kt)("p",null,"\u4fee\u9970\u5668\uff0c\u9002\u7528\u4e8e\u591a\u5206\u7c7b\u95ee\u9898\uff0c\u7528\u6765\u7b80\u5316cmp\u7ed3\u679c\u7684\u8fdb\u4e00\u6b65\u8ba1\u7b97\u548c\u7ed3\u679c\u5b58\u50a8\uff0c\u80fd\u66f4\u597d\u7684\u5448\u73b0\u8bc4\u6d4b\u7ed3\u679c\u3002Github\u4e0a\u7684",(0,l.kt)("a",{parentName:"p",href:"https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/api/_impl/metric.py"},"\u4ee3\u7801\u94fe\u63a5"),"\u3002"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-python"},'\n@multi_classification(\n    confusion_matrix_normalize="all",\n    show_hamming_loss=True,\n    show_cohen_kappa_score=True,\n    show_roc_auc=True,\n    all_labels=[i for i in range(0, 10)],\n)\ndef cmp(ppl_result: PPLResultIterator) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:\n    label, result, probability_matrix = [], [], []\n    return label, result, probability_matrix\n\n@multi_classification(\n    confusion_matrix_normalize="all",\n    show_hamming_loss=True,\n    show_cohen_kappa_score=True,\n    show_roc_auc=False,\n    all_labels=[i for i in range(0, 10)],\n)\ndef cmp(ppl_result: PPLResultIterator) -> t.Tuple[t.List[int], t.List[int], t.List[t.List[float]]]:\n    label, result = [], [], []\n    return label, result\n')),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"\u53c2\u6570"),(0,l.kt)("th",{parentName:"tr",align:null},"\u8bf4\u660e"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"confusion_matrix_normalize")),(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"true"),"(rows), ",(0,l.kt)("inlineCode",{parentName:"td"},"pred"),"(columns) \u6216 ",(0,l.kt)("inlineCode",{parentName:"td"},"all"),"(rows+columns)")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"show_hamming_loss")),(0,l.kt)("td",{parentName:"tr",align:null},"\u662f\u5426\u8ba1\u7b97hamming loss")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"show_cohen_kappa_score")),(0,l.kt)("td",{parentName:"tr",align:null},"\u662f\u5426\u8ba1\u7b97 cohen kappa score")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"show_roc_auc")),(0,l.kt)("td",{parentName:"tr",align:null},"\u662f\u5426\u8ba1\u7b97roc/auc, \u8ba1\u7b97\u7684\u65f6\u5019\uff0c\u9700\u8981\u51fd\u6570\u8fd4\u56de(label\uff0cresult, probability_matrix) \u4e09\u5143\u7ec4\uff0c\u5426\u5219\u53ea\u9700\u8fd4\u56de(label, result) \u4e24\u5143\u7ec4\u5373\u53ef")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},"all_labels"),(0,l.kt)("td",{parentName:"tr",align:null},"\u6240\u6709\u7684labels")))),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"multi_classification")," \u4fee\u9970\u5668\u4f7f\u7528sklearn lib\u5bf9\u591a\u5206\u7c7b\u95ee\u9898\u8fdb\u884c\u7ed3\u679c\u5206\u6790\uff0c\u8f93\u51faconfusion matrix, roc, auc\u7b49\u503c\uff0c\u5e76\u4e14\u4f1a\u5199\u5165\u5230 starwhale\u7684 DataStore \u4e2d\u3002\u4f7f\u7528\u7684\u65f6\u5019\u9700\u8981\u5bf9\u6240\u4fee\u9970\u7684\u51fd\u6570\u8fd4\u56de\u503c\u6709\u4e00\u5b9a\u8981\u6c42\uff0c\u8fd4\u56de(label, result, probability_matrix) \u6216 (label, result)\u3002"),(0,l.kt)("h2",{id:"starwhalestep"},"starwhale.step"),(0,l.kt)("p",null,"\u4fee\u9970\u5668\uff0c\u53ef\u4ee5\u6307\u5b9aDAG\u7684\u4f9d\u8d56\u5173\u7cfb\u548cTask\u6570\u91cf\u3001\u8d44\u6e90\u7b49\u914d\u7f6e\uff0c\u5b9e\u73b0\u7528\u6237\u81ea\u5b9a\u4e49\u8bc4\u6d4b\u8fc7\u7a0b\u3002Github\u4e0a\u7684",(0,l.kt)("a",{parentName:"p",href:"https://github.com/star-whale/starwhale/blob/dc6e6fdeae2f7c5bd0e72ccd8fb50768b1ce0826/client/starwhale/api/_impl/job.py"},"\u4ee3\u7801\u94fe\u63a5"),"\u3002\u4f7f\u7528 ",(0,l.kt)("inlineCode",{parentName:"p"},"step")," \u53ef\u4ee5\u5b8c\u5168\u4e0d\u4f9d\u8d56\u4e8e ",(0,l.kt)("inlineCode",{parentName:"p"},"PipelineHandler")," \u9884\u5b9a\u4e49\u7684\u57fa\u672c\u6a21\u578b\u8bc4\u6d4b\u8fc7\u7a0b\uff0c\u53ef\u4ee5\u81ea\u884c\u5b9a\u4e49\u591a\u9636\u6bb5\u548c\u6bcf\u4e2a\u9636\u6bb5\u7684\u4f9d\u8d56\u3001\u8d44\u6e90\u548c\u4efb\u52a1\u5e76\u53d1\u6570\u7b49\u3002"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-python"},"@step(\n    resources: Optional[List[str]] = None,\n    concurrency: int = 1,\n    task_num: int = 1,\n    needs: Optional[List[str]] = None,\n)\ndef func():\n    ...\n\n")),(0,l.kt)("table",null,(0,l.kt)("thead",{parentName:"table"},(0,l.kt)("tr",{parentName:"thead"},(0,l.kt)("th",{parentName:"tr",align:null},"\u53c2\u6570"),(0,l.kt)("th",{parentName:"tr",align:null},"\u8bf4\u660e"))),(0,l.kt)("tbody",{parentName:"table"},(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"resources")),(0,l.kt)("td",{parentName:"tr",align:null},"\u8be5step\u4e2d\u6bcf\u4e2atask\u6240\u4f9d\u8d56\u7684\u8d44\u6e90\u60c5\u51b5")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"concurrency")),(0,l.kt)("td",{parentName:"tr",align:null},"task\u6267\u884c\u7684\u5e76\u53d1\u5ea6")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"task_num")),(0,l.kt)("td",{parentName:"tr",align:null},"step\u4f1a\u88ab\u5206\u6210task\u7684\u6570\u91cf")),(0,l.kt)("tr",{parentName:"tbody"},(0,l.kt)("td",{parentName:"tr",align:null},(0,l.kt)("inlineCode",{parentName:"td"},"needs")),(0,l.kt)("td",{parentName:"tr",align:null},"\u4f9d\u8d56\u7684step\u5217\u8868")))),(0,l.kt)("p",null,(0,l.kt)("inlineCode",{parentName:"p"},"resources")," \u683c\u5f0f\u4e3a {\u540d\u79f0}:{\u6570\u91cf}\u3002\u540d\u79f0\u4e3a\u8d44\u6e90\u7684\u79cd\u7c7b\uff0c\u76ee\u524d\u652f\u6301 ",(0,l.kt)("inlineCode",{parentName:"p"},"cpu"),"\u3001",(0,l.kt)("inlineCode",{parentName:"p"},"gpu")," \u548c ",(0,l.kt)("inlineCode",{parentName:"p"},"memory"),"\u3002\u5f53\u79cd\u7c7b\u4e3a ",(0,l.kt)("inlineCode",{parentName:"p"},"cpu")," \u65f6\uff0c\u6570\u91cf\u7684\u7c7b\u578b\u4e3afloat, \u6ca1\u6709\u5355\u4f4d\uff0c1\u8868\u793a1\u4e2acpu core\uff0c\u5bf9\u5e94Kubernetes resource\u7684request\uff1b\u5f53\u79cd\u7c7b\u4e3a ",(0,l.kt)("inlineCode",{parentName:"p"},"gpu")," \u65f6\uff0c\u6570\u91cf\u7684\u7c7b\u578b\u4e3aint\uff0c\u6ca1\u6709\u5355\u4f4d\uff0c1\u8868\u793a1\u4e2agpu\uff0c\u5bf9\u5e94Kubernetes resource\u7684request\u548climit\uff1b\u5f53\u79cd\u7c7b\u4e3a ",(0,l.kt)("inlineCode",{parentName:"p"},"memory"),"\u65f6\uff0c\u6570\u91cf\u7684\u7c7b\u578b\u4e3afloat\uff0c\u6ca1\u6709\u5355\u4f4d\uff0c1\u8868\u793a1MB\u5185\u5b58\uff0c\u5bf9\u5e94Kubernetes resource\u7684request\u3002",(0,l.kt)("inlineCode",{parentName:"p"},"resources")," \u4f7f\u7528\u5217\u8868\u7684\u65b9\u5f0f\u652f\u6301\u6307\u5b9a\u591a\u4e2a\u8d44\u6e90\uff0c\u4e14\u8fd9\u4e9b\u8d44\u6e90\u90fd\u6ee1\u8db3\u65f6\u624d\u4f1a\u8fdb\u884c\u8c03\u5ea6\u3002\u5f53\u4e0d\u5199 ",(0,l.kt)("inlineCode",{parentName:"p"},"resources")," \u65f6\uff0c\u4f1a\u4f7f\u7528\u6240\u5728Kubernetes\u7684cpu\u3001memory\u9ed8\u8ba4\u503c\u3002 ",(0,l.kt)("inlineCode",{parentName:"p"},"resources")," \u8868\u793a\u7684\u662f\u4e00\u4e2atask\u6267\u884c\u7684\u65f6\u6240\u9700\u8981\u7684\u8d44\u6e90\u60c5\u51b5\uff0c\u5e76\u4e0d\u662fstep\u6240\u6709task\u7684\u8d44\u6e90\u603b\u548c\u9650\u5236\u3002",(0,l.kt)("strong",{parentName:"p"},"\u76ee\u524d ",(0,l.kt)("inlineCode",{parentName:"strong"},"resources")," \u53ea\u5728Cloud Instance\u4e2d\u751f\u6548"),"\u3002 ",(0,l.kt)("inlineCode",{parentName:"p"},"resources")," \u4f7f\u7528\u4f8b\u5b50\u5982\u4e0b\uff1a"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-python"},'@step()\n@step(resources=["cpu=1"])\n@step(resources=["gpu=1"])\n@step(resources=["memory=100"])\n@step(resources=["cpu=0.1", "gpu=1", "memory=100"])\n')))}m.isMDXComponent=!0}}]);