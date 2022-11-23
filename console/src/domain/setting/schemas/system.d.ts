import { IResourceSchema } from '@/domain/base/schemas/resource';
export declare type IBaseImageSchema = IResourceSchema;
export declare type IDeviceSchema = IResourceSchema;
export interface IDeleteAgentSchema {
    serialNumber: string;
}
export interface ISystemVersionSchema {
    id: string;
    version: string;
}
export interface IAgentSchema {
    connectedTime: number;
    id: string;
    ip: string;
    serialNumber: string;
    status: string;
    version: string;
}
export declare type ISystemSettingSchema = string;
export declare type ISystemResource = {
    name: string;
    max: number;
    min: number;
    defaults: number;
};
export declare type ISystemResourcePool = {
    name: string;
    nodeSelector: Record<string, string>;
    resources: ISystemResource[];
};
