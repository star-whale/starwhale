import { IFileSchema } from '@/domain/base/schemas/file'

export interface IModelVersionDiffSchema {
    baseVersion: IFileSchema
    compareVersion: IFileSchema
}
