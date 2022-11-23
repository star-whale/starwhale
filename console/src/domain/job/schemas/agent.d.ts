import { IResourceSchema } from '@/domain/base/schemas/resource';
export declare enum AgentStatus {
    ONLINE = "ONLINE",
    OFFLINE = "OFFLINE"
}
export interface IAgentSchema extends IResourceSchema {
    ip: string;
    connectedTime: number;
    status?: AgentStatus;
    version: string;
}
