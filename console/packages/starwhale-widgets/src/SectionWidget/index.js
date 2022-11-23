"use strict";
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
exports.__esModule = true;
exports.CONFIG = void 0;
var modal_1 = require("baseui/modal");
var react_1 = require("react");
var ui_1 = require("@starwhale/ui");
var Button_1 = require("@/components/Button");
var BusyPlaceholder_1 = require("@/components/BusyLoaderWrapper/BusyPlaceholder");
var const_1 = require("../../Widget/const");
var WidgetPlugin_1 = require("../../Widget/WidgetPlugin");
var GridBasicLayout_1 = require("./component/GridBasicLayout");
var SectionAccordionPanel_1 = require("./component/SectionAccordionPanel");
var SectionForm_1 = require("./component/SectionForm");
var app_1 = require("../../events/app");
console.log(ui_1["default"]);
exports.CONFIG = {
    type: 'ui:section',
    name: 'test',
    group: const_1.WidgetGroupType.LIST,
    description: 'ui layout for dynamic grid view',
    optionConfig: {
        title: 'Section',
        isExpaned: true,
        layoutConfig: {
            gutter: 10,
            columnsPerPage: 3,
            rowsPerPage: 2,
            boxWidth: 430,
            heightWidth: 274
        },
        gridLayoutConfig: {
            item: {
                w: 1,
                h: 2,
                minW: 1,
                maxW: 2,
                maxH: 3,
                minH: 1,
                isBounded: true
            },
            cols: 2
        },
        gridLayout: []
    }
};
function SectionWidget(props) {
    var optionConfig = props.optionConfig, children = props.children, eventBus = props.eventBus, type = props.type;
    var _a = optionConfig, _b = _a.title, title = _b === void 0 ? '' : _b, _c = _a.isExpaned, isExpaned = _c === void 0 ? false : _c, gridLayoutConfig = _a.gridLayoutConfig, gridLayout = _a.gridLayout;
    var len = react_1["default"].Children.count(children);
    var cols = gridLayoutConfig.cols;
    var layout = (0, react_1.useMemo)(function () {
        if (gridLayout.length !== 0)
            return gridLayout;
        return new Array(len).fill(0).map(function (_, i) { return (__assign({ i: String(i), x: i, y: 0 }, gridLayoutConfig.item)); });
    }, [gridLayout, gridLayoutConfig, len]);
    var _d = (0, react_1.useState)(false), isModelOpen = _d[0], setIsModelOpen = _d[1];
    var handleSectionForm = function (_a) {
        var _b;
        var name = _a.name;
        (_b = props.onOptionChange) === null || _b === void 0 ? void 0 : _b.call(props, {
            title: name
        });
        setIsModelOpen(false);
    };
    var handleEditPanel = function (id) {
        eventBus.publish(new app_1.PanelEditEvent({ id: id }));
    };
    var handleExpanded = function (expanded) {
        var _a;
        (_a = props.onOptionChange) === null || _a === void 0 ? void 0 : _a.call(props, {
            isExpaned: expanded
        });
    };
    var handleLayoutChange = function (args) {
        var _a;
        (_a = props.onOptionChange) === null || _a === void 0 ? void 0 : _a.call(props, {
            gridLayout: args
        });
    };
    return (<div>
            <SectionAccordionPanel_1["default"] childNums={len} title={title} expanded={isExpaned} onExpanded={handleExpanded} onPanelAdd={function () {
            // @FIXME abatract events
            return eventBus.publish(new app_1.PanelAddEvent({
                path: props.path
            }));
        }} onSectionRename={function () {
            setIsModelOpen(true);
        }} onSectionAddAbove={function () {
            var _a;
            (_a = props.onLayoutCurrentChange) === null || _a === void 0 ? void 0 : _a.call(props, { type: type }, { type: 'addAbove' });
        }} onSectionAddBelow={function () {
            var _a;
            (_a = props.onLayoutCurrentChange) === null || _a === void 0 ? void 0 : _a.call(props, { type: type }, { type: 'addBelow' });
        }} onSectionDelete={function () {
            var _a;
            (_a = props.onLayoutCurrentChange) === null || _a === void 0 ? void 0 : _a.call(props, { type: type }, { type: 'delete', id: props.id });
        }}>
                {len === 0 && <BusyPlaceholder_1["default"] type='empty' style={{ minHeight: '240px' }}/>}
                <GridBasicLayout_1.GridLayout rowHeight={300} className='layout' cols={cols} layout={layout} onLayoutChange={handleLayoutChange} containerPadding={[20, 0]} margin={[20, 20]}>
                    {react_1["default"].Children.map(children, function (child, i) { return (<div key={i} style={{
                width: '100%',
                height: '100%',
                overflow: 'auto',
                padding: '40px 20px 20px',
                backgroundColor: '#fff',
                border: '1px solid #CFD7E6',
                borderRadius: '4px',
                position: 'relative'
            }}>
                            {child}
                            <div style={{
                position: 'absolute',
                right: '20px',
                top: '16px'
            }}>
                                <Button_1["default"] 
        // @FIXME direct used child props here ?
        onClick={function () { return handleEditPanel(child.props.id); }} size='compact' kind='secondary' overrides={{
                BaseButton: {
                    style: {
                        'display': 'flex',
                        'fontSize': '12px',
                        'backgroundColor': '#F4F5F7',
                        'width': '20px',
                        'height': '20px',
                        'textDecoration': 'none',
                        'color': 'gray !important',
                        'paddingLeft': '10px',
                        'paddingRight': '10px',
                        ':hover span': {
                            color: ' #5181E0  !important'
                        },
                        ':hover': {
                            backgroundColor: '#F0F4FF'
                        }
                    }
                }
            }}>
                                    <IconFont type='edit' size={10}/>
                                </Button_1["default"]>
                            </div>
                        </div>); })}
                </GridBasicLayout_1.GridLayout>
            </SectionAccordionPanel_1["default"]>
            <modal_1.Modal isOpen={isModelOpen} onClose={function () { return setIsModelOpen(false); }} closeable animate autoFocus>
                <modal_1.ModalHeader>Panel</modal_1.ModalHeader>
                <modal_1.ModalBody>
                    <SectionForm_1["default"] onSubmit={handleSectionForm} formData={{ name: title }}/>
                </modal_1.ModalBody>
            </modal_1.Modal>
        </div>);
}
var widget = new WidgetPlugin_1["default"](SectionWidget, exports.CONFIG);
exports["default"] = widget;
