import { createGlobalState } from 'react-hooks-global-state'
import { ITaskDetailSchema } from '@/domain/job/schemas/task'
import { Role } from '@/api/const'
import { ThemeType } from '@starwhale/ui/theme'
import { ISystemFeaturesSchema } from '@/domain/setting/schemas/system'
import { IDatasetInfoVo, IFineTuneVo, IJobVo, IModelInfoVo, IProjectVo, IRuntimeInfoVo, IUserVo } from '@/api'

const initialState = {
    token: undefined as string | undefined,
    themeType: 'deep' as ThemeType,
    currentUser: undefined as IUserVo | undefined,
    user: undefined as IUserVo | undefined,
    userLoading: false,
    project: undefined as IProjectVo | undefined,
    projectLoading: false,
    model: undefined as IModelInfoVo | undefined,
    modelLoading: false,
    modelVersion: undefined as IModelInfoVo | undefined,
    modelVersionLoading: false,
    dataset: undefined as IDatasetInfoVo | undefined,
    datasetLoading: false,
    datasetVersion: undefined as IDatasetInfoVo | undefined,
    datasetVersionLoading: false,
    runtime: undefined as IRuntimeInfoVo | undefined,
    runtimeLoading: false,
    runtimeVersion: undefined as IRuntimeInfoVo | undefined,
    runtimeVersionLoading: false,
    job: undefined as IJobVo | undefined,
    jobLoading: false,
    fineTune: undefined as IFineTuneVo | undefined,
    fineTuneLoading: false,
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
