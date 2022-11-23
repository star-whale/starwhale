import { IResourceSchema } from '@/domain/base/schemas/resource';
import { IUserSchema } from '@user/schemas/user';
export interface IModelVersionSchema extends IResourceSchema {
    name: string;
    tag: string;
    alias: string;
    meta: Record<string, unknown>;
    owner?: IUserSchema;
    stepSpecs: StepSpec[];
}
export interface IModelVersionListSchema extends IResourceSchema {
    name: string;
    versionName: string;
    versionMeta: string;
    versionTag: string;
    versionAlias: string;
    manifest: string;
}
export interface IModelVersionDetailSchema extends IModelVersionSchema {
    modelName?: string;
}
export interface IUpdateModelVersionSchema {
    tag: string;
}
export interface ICreateModelVersionSchema {
    modelName: string;
    zipFile?: FileList;
    importPath?: string;
}
export interface RuntimeResource {
    type?: string;
    num?: number;
}
export interface StepSpec {
    concurrency?: number;
    needs?: string[];
    resources?: RuntimeResource[];
    job_name?: string;
    step_name?: string;
    task_num?: number;
}
