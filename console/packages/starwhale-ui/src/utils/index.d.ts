export { useOverrides, mergeOverrides } from 'baseui/helpers/overrides';
export declare const isPromise: (obj: any) => boolean;
export declare function millisecondsToTimeStr(milliseconds: number): string;
export declare function getCookie(name: string): string;
export declare function simulationJump(href: string, download?: string): void;
export declare function formatCommitId(s: string): string;
export declare function popupWindow(url: string, windowName: string, width?: number, height?: number): Window | null;
export declare function processUrl(url: string): string;
export declare function sizeStrToByteNum(sizeStr?: string): number | undefined;
export declare function isArrayModified(orig: Array<any>, cur: Array<any>): boolean;
export declare function isModified(orig?: Record<string, any>, cur?: Record<string, any>): boolean;
export declare function getMilliCpuQuantity(value?: string): number;
export declare function getCpuCoresQuantityStr(value: number): string;
export declare function getReadableStorageQuantityStr(bytes?: number): string;
export declare function numberToPercentStr(v: number): string;
export declare function flattenObject(o: any, prefix?: string, result?: any, keepNull?: boolean): any;
export declare function longestCommonSubstring(string1: string, string2: string): string;
export declare function parseDecimal(v: number, decimal: number): string;
export declare function expandBorder(width?: string, weight?: string, color?: string): Record<string, string>;
export declare function expandBorderRadius(radius: string): {
    borderTopLeftRadius: string;
    borderTopRightRadius: string;
    borderBottomRightRadius: string;
    borderBottomLeftRadius: string;
};
export declare function expandPadding(top: string, right: string, bottom: string, left: string): {
    paddingTop: string;
    paddingBottom: string;
    paddingLeft: string;
    paddingRight: string;
};
export declare function expandMargin(top: string, right: string, bottom: string, left: string): {
    marginTop: string;
    marginBottom: string;
    marginLeft: string;
    marginRight: string;
};
