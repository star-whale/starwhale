import { createGlobalState } from 'react-hooks-global-state'
import { IUserSchema } from '@user/schemas/user'
import { IProjectSchema, IProjectRoleSchema } from '@project/schemas/project'
import { IModelDetailSchema } from '@model/schemas/model'
import { IModelVersionDetailSchema } from '@model/schemas/modelVersion'
import { IDatasetDetailSchema } from '@/domain/dataset/schemas/dataset'
import { IDatasetVersionDetailSchema } from '@/domain/dataset/schemas/datasetVersion'
import { IJobSchema } from '@/domain/job/schemas/job'
import { ITaskDetailSchema } from '@/domain/job/schemas/task'
import { ThemeType } from '@/theme'
import { Role } from '@/api/WithAuth'
import { IRuntimeDetailSchema } from '../domain/runtime/schemas/runtime'
import { IRuntimeVersionDetailSchema } from '../domain/runtime/schemas/runtimeVersion'

const initialState = {
    token: undefined as string | undefined,
    themeType: 'deep' as ThemeType,
    currentUser: undefined as IUserSchema | undefined,
    currentUserRoles: undefined as IProjectRoleSchema[] | undefined,
    user: undefined as IUserSchema | undefined,
    userLoading: false,
    project: undefined as IProjectSchema | undefined,
    projectLoading: false,
    model: undefined as IModelDetailSchema | undefined,
    modelLoading: false,
    modelVersion: undefined as IModelVersionDetailSchema | undefined,
    modelVersionLoading: false,
    dataset: undefined as IDatasetDetailSchema | undefined,
    datasetLoading: false,
    datasetVersion: undefined as IDatasetVersionDetailSchema | undefined,
    datasetVersionLoading: false,
    runtime: undefined as IRuntimeDetailSchema | undefined,
    runtimeLoading: false,
    runtimeVersion: undefined as IRuntimeVersionDetailSchema | undefined,
    runtimeVersionLoading: false,
    job: undefined as IJobSchema | undefined,
    jobLoading: false,
    task: undefined as ITaskDetailSchema | undefined,
    taskLoading: false,
    drawerExpanded: false,
    role: Role.NONE as Role,
}

const { useGlobalState } = createGlobalState(initialState)
export default useGlobalState
