import { createGlobalState } from 'react-hooks-global-state'
import { IUserSchema } from '@user/schemas/user'
import { IProjectSchema } from '@project/schemas/project'
import { IModelDetailSchema } from '@model/schemas/model'
import { IModelVersionDetailSchema } from '@model/schemas/modelVersion'
import { IDatasetDetailSchema } from '@/domain/dataset/schemas/dataset'
import { IDatasetVersionDetailSchema } from '@/domain/dataset/schemas/datasetVersion'
import { IJobSchema } from '@/domain/job/schemas/job'
import { ITaskDetailSchema } from '@/domain/job/schemas/task'
import { Role } from '@/api/const'
import { IRuntimeDetailSchema } from '@/domain/runtime/schemas/runtime'
import { ThemeType } from '@starwhale/ui/theme'
import { ISystemFeaturesSchema } from '@/domain/setting/schemas/system'

const initialState = {
    token: undefined as string | undefined,
    themeType: 'deep' as ThemeType,
    currentUser: undefined as IUserSchema | undefined,
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
    runtimeVersion: undefined as IRuntimeDetailSchema | undefined,
    runtimeVersionLoading: false,
    job: undefined as IJobSchema | undefined,
    jobLoading: false,
    task: undefined as ITaskDetailSchema | undefined,
    taskLoading: false,
    drawerExpanded: false,
    role: Role.NONE as Role,
    vscodeTheme: 'light' as string,
    systemFeatures: { disabled: [] } as ISystemFeaturesSchema,
    locale: 'zh' as string,
}

const { useGlobalState } = createGlobalState(initialState)
export default useGlobalState
