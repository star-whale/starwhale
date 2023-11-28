/* eslint-disable */
/* tslint:disable */
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

// extends
import qs from 'qs'
import { useQuery } from 'react-query'

import {
    IActionData,
    IAddDatasetVersionTagData,
    IAddEventData,
    IAddModelVersionTagData,
    IAddProjectRoleData,
    IAddRuntimeVersionTagData,
    IAddTemplateData,
    IAddUserSystemRoleData,
    IApplyPathPrefixData,
    IApplySignedGetUrlsData,
    IApplySignedPutUrlsData,
    IApplySignedUrlRequest,
    IBuildDatasetData,
    IBuildRuntimeImageData,
    ICheckCurrentUserPasswordData,
    IClientRuntimeRequest,
    ICompleteUploadBlobData,
    IConfigRequest,
    IConsumeNextDataData,
    ICreateJobData,
    ICreateJobTemplateRequest,
    ICreateModelServingData,
    ICreateModelVersionData,
    ICreateModelVersionRequest,
    ICreateProjectData,
    ICreateProjectRequest,
    ICreateReportData,
    ICreateReportRequest,
    ICreateSpaceData,
    ICreateUserData,
    ICreateViewConfigData,
    IDataConsumptionRequest,
    IDatasetBuildRequest,
    IDatasetTagRequest,
    IDatasetUploadRequest,
    IDeleteDatasetData,
    IDeleteDatasetVersionTagData,
    IDeleteModelData,
    IDeleteModelVersionTagData,
    IDeletePathData,
    IDeleteProjectByUrlData,
    IDeleteProjectRoleData,
    IDeleteReportData,
    IDeleteRuntimeData,
    IDeleteRuntimeVersionTagData,
    IDeleteTemplateData,
    IDeleteTrashData,
    IDeleteUserSystemRoleData,
    IEventRequest,
    IExecData,
    IExecRequest,
    IExportEvalData,
    IFileDeleteRequest,
    IFineTuneInfoData,
    IFineTuneMigrationRequest,
    IFineTuneSpaceCreateRequest,
    IFlushData,
    IFlushRequest,
    IGetCurrentUserData,
    IGetCurrentUserRolesData,
    IGetCurrentVersionData,
    IGetDatasetInfoData,
    IGetDatasetVersionTagData,
    IGetEventsData,
    IGetFileDataData,
    IGetHashedBlob1Data,
    IGetHashedBlobData,
    IGetJobDagData,
    IGetJobData,
    IGetModelDiffData,
    IGetModelInfoData,
    IGetModelMetaBlobData,
    IGetModelServingStatusData,
    IGetModelVersionTagData,
    IGetPanelSettingData,
    IGetProjectByUrlData,
    IGetProjectReadmeByUrlData,
    IGetReportData,
    IGetRunsData,
    IGetRuntimeInfoData,
    IGetRuntimeSuggestionData,
    IGetRuntimeVersionTagData,
    IGetTaskData,
    IGetTemplateData,
    IGetUserByIdData,
    IGetViewConfigData,
    IHeadDatasetData,
    IHeadHashedBlob1Data,
    IHeadHashedBlobData,
    IHeadModelData,
    IHeadRuntimeData,
    IImportEvalData,
    IInitUploadBlobData,
    IInitUploadBlobRequest,
    IInstallPluginData,
    IInstanceStatusData,
    IJobModifyPinRequest,
    IJobModifyRequest,
    IJobRequest,
    IListBuildRecordsData,
    IListDatasetData,
    IListDatasetTreeData,
    IListDatasetVersionData,
    IListDatasetVersionTagsData,
    IListDeviceData,
    IListFilesData,
    IListFineTuneData,
    IListJobsData,
    IListModelData,
    IListModelTree1Data,
    IListModelTreeData,
    IListModelVersionData,
    IListModelVersionTagsData,
    IListOnlineEvalData,
    IListProjectData,
    IListProjectRoleData,
    IListReportsData,
    IListResourcePoolsData,
    IListRolesData,
    IListRuntimeData,
    IListRuntimeTreeData,
    IListRuntimeVersionData,
    IListRuntimeVersionTagsData,
    IListSpaceData,
    IListSystemRolesData,
    IListTablesData,
    IListTablesRequest,
    IListTasksData,
    IListTrashData,
    IListUserData,
    ILogContentData,
    IModelServingRequest,
    IModelTagRequest,
    IModelUpdateRequest,
    IModifyJobCommentData,
    IModifyJobPinStatusData,
    IModifyModelData,
    IModifyProjectRoleData,
    IModifyReportData,
    IModifyRuntimeData,
    IOfflineLogsData,
    IPluginListData,
    IPreviewData,
    IPullData,
    IPullDsData,
    IPullUriContent1Data,
    IPullUriContentData,
    IQueryAndExportData,
    IQueryFeaturesData,
    IQuerySettingData,
    IQueryTableData,
    IQueryTableRequest,
    IRecentDatasetTreeData,
    IRecentModelTree1Data,
    IRecentModelTreeData,
    IRecentRuntimeTreeData,
    IRecoverDatasetData,
    IRecoverJobData,
    IRecoverModelData,
    IRecoverProjectData,
    IRecoverRuntimeData,
    IRecoverTrashData,
    IReleaseFtData,
    IRemoveJobData,
    IResourcePool,
    IRevertDatasetRequest,
    IRevertDatasetVersionData,
    IRevertModelVersionData,
    IRevertModelVersionRequest,
    IRevertRuntimeVersionData,
    IRunEnvs,
    IRuntimeRevertRequest,
    IRuntimeTagRequest,
    IScanAndExportData,
    IScanTableData,
    IScanTableRequest,
    ISelectAllInProjectData,
    ISelectRecentlyInProjectData,
    ISetPanelSettingData,
    IShareDatasetVersionData,
    IShareModelVersionData,
    IShareRuntimeVersionData,
    ISharedReportData,
    ISignLinks1Data,
    ISignLinks2Data,
    ISignLinksData,
    ISpaceInfoData,
    ITransferData,
    ITransferReportRequest,
    IUninstallPluginData,
    IUpdateCurrentUserPasswordData,
    IUpdateProjectData,
    IUpdateProjectRequest,
    IUpdateReportRequest,
    IUpdateResourcePoolsData,
    IUpdateRuntimeData,
    IUpdateSettingData,
    IUpdateSpaceData,
    IUpdateTableData,
    IUpdateTableRequest,
    IUpdateUserPwdData,
    IUpdateUserStateData,
    IUpdateUserSystemRoleData,
    IUploadData,
    IUploadDsData,
    IUploadHashedBlob1Data,
    IUploadHashedBlobData,
    IUserCheckPasswordRequest,
    IUserRequest,
    IUserRoleAddRequest,
    IUserRoleDeleteRequest,
    IUserRoleUpdateRequest,
    IUserTokenData,
    IUserUpdatePasswordRequest,
    IUserUpdateStateRequest,
} from './data-contracts'
import { ContentType, HttpClient, RequestParams } from './http-client'

export class Api<SecurityDataType = unknown> {
    http: HttpClient<SecurityDataType>

    constructor(http: HttpClient<SecurityDataType>) {
        this.http = http
    }

    /**
     * No description
     *
     * @tags User
     * @name UpdateUserState
     * @summary Enable or disable a user
     * @request PUT:/api/v1/user/{userId}/state
     * @secure
     * @response `200` `IUpdateUserStateData` OK
     */
    updateUserState = (userId: string, data: IUserUpdateStateRequest, params: RequestParams = {}) =>
        this.http.request<IUpdateUserStateData, any>({
            path: `/api/v1/user/${userId}/state`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags User
     * @name UpdateUserPwd
     * @summary Change user password
     * @request PUT:/api/v1/user/{userId}/pwd
     * @secure
     * @response `200` `IUpdateUserPwdData` OK
     */
    updateUserPwd = (userId: string, data: IUserUpdatePasswordRequest, params: RequestParams = {}) =>
        this.http.request<IUpdateUserPwdData, any>({
            path: `/api/v1/user/${userId}/pwd`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags User
     * @name UpdateCurrentUserPassword
     * @summary Update Current User password
     * @request PUT:/api/v1/user/current/pwd
     * @secure
     * @response `200` `IUpdateCurrentUserPasswordData` OK
     */
    updateCurrentUserPassword = (data: IUserUpdatePasswordRequest, params: RequestParams = {}) =>
        this.http.request<IUpdateCurrentUserPasswordData, any>({
            path: `/api/v1/user/current/pwd`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags User
     * @name CheckCurrentUserPassword
     * @summary Check Current User password
     * @request POST:/api/v1/user/current/pwd
     * @secure
     * @response `200` `ICheckCurrentUserPasswordData` OK
     */
    checkCurrentUserPassword = (data: IUserCheckPasswordRequest, params: RequestParams = {}) =>
        this.http.request<ICheckCurrentUserPasswordData, any>({
            path: `/api/v1/user/current/pwd`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags User
     * @name UpdateUserSystemRole
     * @summary Update user role of system
     * @request PUT:/api/v1/role/{systemRoleId}
     * @secure
     * @response `200` `IUpdateUserSystemRoleData` OK
     */
    updateUserSystemRole = (systemRoleId: string, data: IUserRoleUpdateRequest, params: RequestParams = {}) =>
        this.http.request<IUpdateUserSystemRoleData, any>({
            path: `/api/v1/role/${systemRoleId}`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags User
     * @name DeleteUserSystemRole
     * @summary Delete user role of system
     * @request DELETE:/api/v1/role/{systemRoleId}
     * @secure
     * @response `200` `IDeleteUserSystemRoleData` OK
     */
    deleteUserSystemRole = (systemRoleId: string, data: IUserRoleDeleteRequest, params: RequestParams = {}) =>
        this.http.request<IDeleteUserSystemRoleData, any>({
            path: `/api/v1/role/${systemRoleId}`,
            method: 'DELETE',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description Returns a single project object.
     *
     * @tags Project
     * @name GetProjectByUrl
     * @summary Get a project by Url
     * @request GET:/api/v1/project/{projectUrl}
     * @secure
     * @response `200` `IGetProjectByUrlData` OK
     */
    getProjectByUrl = (projectUrl: string, params: RequestParams = {}) =>
        this.http.request<IGetProjectByUrlData, any>({
            path: `/api/v1/project/${projectUrl}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetProjectByUrl = (projectUrl: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getProjectByUrl', projectUrl, params]),
            () => this.getProjectByUrl(projectUrl, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Project
     * @name UpdateProject
     * @summary Modify project information
     * @request PUT:/api/v1/project/{projectUrl}
     * @secure
     * @response `200` `IUpdateProjectData` OK
     */
    updateProject = (projectUrl: string, data: IUpdateProjectRequest, params: RequestParams = {}) =>
        this.http.request<IUpdateProjectData, any>({
            path: `/api/v1/project/${projectUrl}`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Project
     * @name DeleteProjectByUrl
     * @summary Delete a project by Url
     * @request DELETE:/api/v1/project/{projectUrl}
     * @secure
     * @response `200` `IDeleteProjectByUrlData` OK
     */
    deleteProjectByUrl = (projectUrl: string, params: RequestParams = {}) =>
        this.http.request<IDeleteProjectByUrlData, any>({
            path: `/api/v1/project/${projectUrl}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * @description Restore a trash to its original type and move it out of the recycle bin.
     *
     * @tags Trash
     * @name RecoverTrash
     * @summary Restore trash by id.
     * @request PUT:/api/v1/project/{projectUrl}/trash/{trashId}
     * @secure
     * @response `200` `IRecoverTrashData` OK
     */
    recoverTrash = (projectUrl: string, trashId: number, params: RequestParams = {}) =>
        this.http.request<IRecoverTrashData, any>({
            path: `/api/v1/project/${projectUrl}/trash/${trashId}`,
            method: 'PUT',
            secure: true,
            ...params,
        })

    /**
     * @description Move a trash out of the recycle bin. This operation cannot be resumed.
     *
     * @tags Trash
     * @name DeleteTrash
     * @summary Delete trash by id.
     * @request DELETE:/api/v1/project/{projectUrl}/trash/{trashId}
     * @secure
     * @response `200` `IDeleteTrashData` OK
     */
    deleteTrash = (projectUrl: string, trashId: number, params: RequestParams = {}) =>
        this.http.request<IDeleteTrashData, any>({
            path: `/api/v1/project/${projectUrl}/trash/${trashId}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * @description update image for runtime
     *
     * @tags Runtime
     * @name UpdateRuntime
     * @summary update image for runtime
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/image
     * @secure
     * @response `200` `IUpdateRuntimeData` OK
     */
    updateRuntime = (
        projectUrl: string,
        runtimeUrl: string,
        versionUrl: string,
        data: string,
        params: RequestParams = {}
    ) =>
        this.http.request<IUpdateRuntimeData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/image`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Runtime
     * @name ModifyRuntime
     * @summary Set tag of the runtime version
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}
     * @secure
     * @response `200` `IModifyRuntimeData` OK
     */
    modifyRuntime = (
        projectUrl: string,
        runtimeUrl: string,
        runtimeVersionUrl: string,
        data: IRuntimeTagRequest,
        params: RequestParams = {}
    ) =>
        this.http.request<IModifyRuntimeData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${runtimeVersionUrl}`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Runtime
     * @name ShareRuntimeVersion
     * @summary Share or unshare the runtime version
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}/shared
     * @secure
     * @response `200` `IShareRuntimeVersionData` OK
     */
    shareRuntimeVersion = (
        projectUrl: string,
        runtimeUrl: string,
        runtimeVersionUrl: string,
        query: {
            /** 1 or true - shared, 0 or false - unshared */
            shared: boolean
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IShareRuntimeVersionData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${runtimeVersionUrl}/shared`,
            method: 'PUT',
            query: query,
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Runtime
     * @name RecoverRuntime
     * @summary Recover a runtime
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/recover
     * @secure
     * @response `200` `IRecoverRuntimeData` OK
     */
    recoverRuntime = (projectUrl: string, runtimeUrl: string, params: RequestParams = {}) =>
        this.http.request<IRecoverRuntimeData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/recover`,
            method: 'PUT',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Project
     * @name ModifyProjectRole
     * @summary Modify a project role
     * @request PUT:/api/v1/project/{projectUrl}/role/{projectRoleId}
     * @secure
     * @response `200` `IModifyProjectRoleData` OK
     */
    modifyProjectRole = (
        projectUrl: string,
        projectRoleId: string,
        query: {
            roleId: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IModifyProjectRoleData, any>({
            path: `/api/v1/project/${projectUrl}/role/${projectRoleId}`,
            method: 'PUT',
            query: query,
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Project
     * @name DeleteProjectRole
     * @summary Delete a project role
     * @request DELETE:/api/v1/project/{projectUrl}/role/{projectRoleId}
     * @secure
     * @response `200` `IDeleteProjectRoleData` OK
     */
    deleteProjectRole = (projectUrl: string, projectRoleId: string, params: RequestParams = {}) =>
        this.http.request<IDeleteProjectRoleData, any>({
            path: `/api/v1/project/${projectUrl}/role/${projectRoleId}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Report
     * @name GetReport
     * @request GET:/api/v1/project/{projectUrl}/report/{reportId}
     * @secure
     * @response `200` `IGetReportData` OK
     */
    getReport = (projectUrl: string, reportId: number, params: RequestParams = {}) =>
        this.http.request<IGetReportData, any>({
            path: `/api/v1/project/${projectUrl}/report/${reportId}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetReport = (projectUrl: string, reportId: number, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getReport', projectUrl, reportId, params]),
            () => this.getReport(projectUrl, reportId, params),
            {
                enabled: [projectUrl, reportId].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Report
     * @name ModifyReport
     * @request PUT:/api/v1/project/{projectUrl}/report/{reportId}
     * @secure
     * @response `200` `IModifyReportData` OK
     */
    modifyReport = (projectUrl: string, reportId: number, data: IUpdateReportRequest, params: RequestParams = {}) =>
        this.http.request<IModifyReportData, any>({
            path: `/api/v1/project/${projectUrl}/report/${reportId}`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Report
     * @name DeleteReport
     * @request DELETE:/api/v1/project/{projectUrl}/report/{reportId}
     * @secure
     * @response `200` `IDeleteReportData` OK
     */
    deleteReport = (projectUrl: string, reportId: number, params: RequestParams = {}) =>
        this.http.request<IDeleteReportData, any>({
            path: `/api/v1/project/${projectUrl}/report/${reportId}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Report
     * @name SharedReport
     * @request PUT:/api/v1/project/{projectUrl}/report/{reportId}/shared
     * @secure
     * @response `200` `ISharedReportData` OK
     */
    sharedReport = (
        projectUrl: string,
        reportId: number,
        query: {
            shared: boolean
        },
        params: RequestParams = {}
    ) =>
        this.http.request<ISharedReportData, any>({
            path: `/api/v1/project/${projectUrl}/report/${reportId}/shared`,
            method: 'PUT',
            query: query,
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name ModifyModel
     * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}
     * @secure
     * @response `200` `IModifyModelData` OK
     */
    modifyModel = (
        projectUrl: string,
        modelUrl: string,
        versionUrl: string,
        data: IModelUpdateRequest,
        params: RequestParams = {}
    ) =>
        this.http.request<IModifyModelData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name HeadModel
     * @request HEAD:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}
     * @secure
     * @response `200` `IHeadModelData` OK
     */
    headModel = (projectUrl: string, modelUrl: string, versionUrl: string, params: RequestParams = {}) =>
        this.http.request<IHeadModelData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}`,
            method: 'HEAD',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name ShareModelVersion
     * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/shared
     * @secure
     * @response `200` `IShareModelVersionData` OK
     */
    shareModelVersion = (
        projectUrl: string,
        modelUrl: string,
        versionUrl: string,
        query: {
            shared: boolean
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IShareModelVersionData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/shared`,
            method: 'PUT',
            query: query,
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name RecoverModel
     * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/recover
     * @secure
     * @response `200` `IRecoverModelData` OK
     */
    recoverModel = (projectUrl: string, modelUrl: string, params: RequestParams = {}) =>
        this.http.request<IRecoverModelData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/recover`,
            method: 'PUT',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Job
     * @name GetJob
     * @summary Job information
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}
     * @secure
     * @response `200` `IGetJobData` OK
     */
    getJob = (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
        this.http.request<IGetJobData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetJob = (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
        useQuery(qs.stringify(['getJob', projectUrl, jobUrl, params]), () => this.getJob(projectUrl, jobUrl, params), {
            enabled: [projectUrl, jobUrl].every(Boolean),
        })
    /**
     * No description
     *
     * @tags Job
     * @name ModifyJobComment
     * @summary Set Job Comment
     * @request PUT:/api/v1/project/{projectUrl}/job/{jobUrl}
     * @secure
     * @response `200` `IModifyJobCommentData` OK
     */
    modifyJobComment = (projectUrl: string, jobUrl: string, data: IJobModifyRequest, params: RequestParams = {}) =>
        this.http.request<IModifyJobCommentData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Job
     * @name RemoveJob
     * @summary Remove job
     * @request DELETE:/api/v1/project/{projectUrl}/job/{jobUrl}
     * @secure
     * @response `200` `IRemoveJobData` OK
     */
    removeJob = (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
        this.http.request<IRemoveJobData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Dataset
     * @name ShareDatasetVersion
     * @summary Share or unshare the dataset version
     * @request PUT:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/shared
     * @secure
     * @response `200` `IShareDatasetVersionData` OK
     */
    shareDatasetVersion = (
        projectUrl: string,
        datasetUrl: string,
        versionUrl: string,
        query: {
            /** 1 or true - shared, 0 or false - unshared */
            shared: boolean
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IShareDatasetVersionData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/shared`,
            method: 'PUT',
            query: query,
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Dataset
     * @name RecoverDataset
     * @summary Recover a dataset
     * @request PUT:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/recover
     * @secure
     * @response `200` `IRecoverDatasetData` OK
     */
    recoverDataset = (projectUrl: string, datasetUrl: string, params: RequestParams = {}) =>
        this.http.request<IRecoverDatasetData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/recover`,
            method: 'PUT',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Project
     * @name RecoverProject
     * @summary Recover a project
     * @request PUT:/api/v1/project/{projectId}/recover
     * @secure
     * @response `200` `IRecoverProjectData` OK
     */
    recoverProject = (projectId: string, params: RequestParams = {}) =>
        this.http.request<IRecoverProjectData, any>({
            path: `/api/v1/project/${projectId}/recover`,
            method: 'PUT',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags FineTune
     * @name SpaceInfo
     * @summary Get the list of fine-tune spaces
     * @request GET:/api/v1/project/{projectId}/ftspace/{spaceId}
     * @secure
     * @response `200` `ISpaceInfoData` OK
     */
    spaceInfo = (projectId: number, spaceId: number, params: RequestParams = {}) =>
        this.http.request<ISpaceInfoData, any>({
            path: `/api/v1/project/${projectId}/ftspace/${spaceId}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useSpaceInfo = (projectId: number, spaceId: number, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['spaceInfo', projectId, spaceId, params]),
            () => this.spaceInfo(projectId, spaceId, params),
            {
                enabled: [projectId, spaceId].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags FineTune
     * @name UpdateSpace
     * @summary Update fine-tune space
     * @request PUT:/api/v1/project/{projectId}/ftspace/{spaceId}
     * @secure
     * @response `200` `IUpdateSpaceData` OK
     */
    updateSpace = (projectId: number, spaceId: number, data: IFineTuneSpaceCreateRequest, params: RequestParams = {}) =>
        this.http.request<IUpdateSpaceData, any>({
            path: `/api/v1/project/${projectId}/ftspace/${spaceId}`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags FineTune
     * @name ReleaseFt
     * @summary release fine-tune
     * @request PUT:/api/v1/project/{projectId}/ftspace/{spaceId}/ft/release
     * @secure
     * @response `200` `IReleaseFtData` OK
     */
    releaseFt = (
        projectId: number,
        spaceId: number,
        query: {
            /** @format int64 */
            ftId: number
            nonExistingModelName?: string
            /** @format int64 */
            existingModelId?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IReleaseFtData, any>({
            path: `/api/v1/project/${projectId}/ftspace/${spaceId}/ft/release`,
            method: 'PUT',
            query: query,
            secure: true,
            ...params,
        })

    /**
     * @description Apply signedUrls for get
     *
     * @tags File storage
     * @name ApplySignedGetUrls
     * @summary Apply signedUrls for get
     * @request GET:/api/v1/filestorage/signedurl
     * @secure
     * @response `200` `IApplySignedGetUrlsData` OK
     */
    applySignedGetUrls = (
        query: {
            pathPrefix: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IApplySignedGetUrlsData, any>({
            path: `/api/v1/filestorage/signedurl`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useApplySignedGetUrls = (
        query: {
            pathPrefix: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(qs.stringify(['applySignedGetUrls', query, params]), () => this.applySignedGetUrls(query, params), {
            enabled: [query].every(Boolean),
        })
    /**
     * @description Apply signedUrls for put
     *
     * @tags File storage
     * @name ApplySignedPutUrls
     * @summary Apply signedUrls for put
     * @request PUT:/api/v1/filestorage/signedurl
     * @secure
     * @response `200` `IApplySignedPutUrlsData` OK
     */
    applySignedPutUrls = (data: IApplySignedUrlRequest, params: RequestParams = {}) =>
        this.http.request<IApplySignedPutUrlsData, any>({
            path: `/api/v1/filestorage/signedurl`,
            method: 'PUT',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags User
     * @name ListUser
     * @summary Get the list of users
     * @request GET:/api/v1/user
     * @secure
     * @response `200` `IListUserData` OK
     */
    listUser = (
        query?: {
            /** User name prefix to search for */
            userName?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListUserData, any>({
            path: `/api/v1/user`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListUser = (
        query?: {
            /** User name prefix to search for */
            userName?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(qs.stringify(['listUser', query, params]), () => this.listUser(query, params), {
            enabled: [].every(Boolean),
        })
    /**
     * No description
     *
     * @tags User
     * @name CreateUser
     * @summary Create a new user
     * @request POST:/api/v1/user
     * @secure
     * @response `200` `ICreateUserData` OK
     */
    createUser = (data: IUserRequest, params: RequestParams = {}) =>
        this.http.request<ICreateUserData, any>({
            path: `/api/v1/user`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags rolling-update-controller
     * @name InstanceStatus
     * @summary instance status notify
     * @request POST:/api/v1/system/upgrade/instance/status
     * @secure
     * @response `200` `IInstanceStatusData` ok
     */
    instanceStatus = (
        query: {
            status: 'BORN' | 'READY_DOWN' | 'READY_UP' | 'DOWN'
            instanceType: 'NEW' | 'OLD'
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IInstanceStatusData, any>({
            path: `/api/v1/system/upgrade/instance/status`,
            method: 'POST',
            query: query,
            secure: true,
            ...params,
        })

    /**
     * @description Get system settings in yaml string
     *
     * @tags System
     * @name QuerySetting
     * @summary Get system settings
     * @request GET:/api/v1/system/setting
     * @secure
     * @response `200` `IQuerySettingData` OK
     */
    querySetting = (params: RequestParams = {}) =>
        this.http.request<IQuerySettingData, any>({
            path: `/api/v1/system/setting`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useQuerySetting = (params: RequestParams = {}) =>
        useQuery(qs.stringify(['querySetting', params]), () => this.querySetting(params), {
            enabled: [].every(Boolean),
        })
    /**
     * @description Update system settings
     *
     * @tags System
     * @name UpdateSetting
     * @summary Update system settings
     * @request POST:/api/v1/system/setting
     * @secure
     * @response `200` `IUpdateSettingData` OK
     */
    updateSetting = (data: string, params: RequestParams = {}) =>
        this.http.request<IUpdateSettingData, any>({
            path: `/api/v1/system/setting`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags System
     * @name ListResourcePools
     * @summary Get the list of resource pool
     * @request GET:/api/v1/system/resourcePool
     * @secure
     * @response `200` `IListResourcePoolsData` OK
     */
    listResourcePools = (params: RequestParams = {}) =>
        this.http.request<IListResourcePoolsData, any>({
            path: `/api/v1/system/resourcePool`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useListResourcePools = (params: RequestParams = {}) =>
        useQuery(qs.stringify(['listResourcePools', params]), () => this.listResourcePools(params), {
            enabled: [].every(Boolean),
        })
    /**
     * No description
     *
     * @tags System
     * @name UpdateResourcePools
     * @summary Update resource pool
     * @request POST:/api/v1/system/resourcePool
     * @secure
     * @response `200` `IUpdateResourcePoolsData` OK
     */
    updateResourcePools = (data: IResourcePool[], params: RequestParams = {}) =>
        this.http.request<IUpdateResourcePoolsData, any>({
            path: `/api/v1/system/resourcePool`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags User
     * @name ListSystemRoles
     * @summary List system role of users
     * @request GET:/api/v1/role
     * @secure
     * @response `200` `IListSystemRolesData` OK
     */
    listSystemRoles = (params: RequestParams = {}) =>
        this.http.request<IListSystemRolesData, any>({
            path: `/api/v1/role`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useListSystemRoles = (params: RequestParams = {}) =>
        useQuery(qs.stringify(['listSystemRoles', params]), () => this.listSystemRoles(params), {
            enabled: [].every(Boolean),
        })
    /**
     * No description
     *
     * @tags User
     * @name AddUserSystemRole
     * @summary Add user role of system
     * @request POST:/api/v1/role
     * @secure
     * @response `200` `IAddUserSystemRoleData` OK
     */
    addUserSystemRole = (data: IUserRoleAddRequest, params: RequestParams = {}) =>
        this.http.request<IAddUserSystemRoleData, any>({
            path: `/api/v1/role`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Project
     * @name ListProject
     * @summary Get the list of projects
     * @request GET:/api/v1/project
     * @secure
     * @response `200` `IListProjectData` OK
     */
    listProject = (
        sort: 'visited' | 'latest' | 'oldest',
        query?: {
            projectName?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListProjectData, any>({
            path: `/api/v1/project`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListProject = (
        sort: 'visited' | 'latest' | 'oldest',
        query?: {
            projectName?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(qs.stringify(['listProject', sort, query, params]), () => this.listProject(sort, query, params), {
            enabled: [sort].every(Boolean),
        })
    /**
     * No description
     *
     * @tags Project
     * @name CreateProject
     * @summary Create or Recover a new project
     * @request POST:/api/v1/project
     * @secure
     * @response `200` `ICreateProjectData` OK
     */
    createProject = (data: ICreateProjectRequest, params: RequestParams = {}) =>
        this.http.request<ICreateProjectData, any>({
            path: `/api/v1/project`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name CreateModelVersion
     * @request POST:/api/v1/project/{project}/model/{modelName}/version/{version}/completeUpload
     * @secure
     * @response `200` `ICreateModelVersionData` OK
     */
    createModelVersion = (
        project: string,
        modelName: string,
        version: string,
        data: ICreateModelVersionRequest,
        params: RequestParams = {}
    ) =>
        this.http.request<ICreateModelVersionData, any>({
            path: `/api/v1/project/${project}/model/${modelName}/version/${version}/completeUpload`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Template
     * @name SelectAllInProject
     * @summary Get Templates for project
     * @request GET:/api/v1/project/{projectUrl}/template
     * @secure
     * @response `200` `ISelectAllInProjectData` OK
     */
    selectAllInProject = (projectUrl: string, params: RequestParams = {}) =>
        this.http.request<ISelectAllInProjectData, any>({
            path: `/api/v1/project/${projectUrl}/template`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useSelectAllInProject = (projectUrl: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['selectAllInProject', projectUrl, params]),
            () => this.selectAllInProject(projectUrl, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Template
     * @name AddTemplate
     * @summary Add Template for job
     * @request POST:/api/v1/project/{projectUrl}/template
     * @secure
     * @response `200` `IAddTemplateData` OK
     */
    addTemplate = (projectUrl: string, data: ICreateJobTemplateRequest, params: RequestParams = {}) =>
        this.http.request<IAddTemplateData, any>({
            path: `/api/v1/project/${projectUrl}/template`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Job
     * @name CreateModelServing
     * @summary Create a new model serving job
     * @request POST:/api/v1/project/{projectUrl}/serving
     * @secure
     * @response `200` `ICreateModelServingData` OK
     */
    createModelServing = (projectUrl: string, data: IModelServingRequest, params: RequestParams = {}) =>
        this.http.request<ICreateModelServingData, any>({
            path: `/api/v1/project/${projectUrl}/serving`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Runtime
     * @name ListRuntimeVersionTags
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `IListRuntimeVersionTagsData` OK
     */
    listRuntimeVersionTags = (projectUrl: string, runtimeUrl: string, versionUrl: string, params: RequestParams = {}) =>
        this.http.request<IListRuntimeVersionTagsData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/tag`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useListRuntimeVersionTags = (
        projectUrl: string,
        runtimeUrl: string,
        versionUrl: string,
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listRuntimeVersionTags', projectUrl, runtimeUrl, versionUrl, params]),
            () => this.listRuntimeVersionTags(projectUrl, runtimeUrl, versionUrl, params),
            {
                enabled: [projectUrl, runtimeUrl, versionUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Runtime
     * @name AddRuntimeVersionTag
     * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `IAddRuntimeVersionTagData` OK
     */
    addRuntimeVersionTag = (
        projectUrl: string,
        runtimeUrl: string,
        versionUrl: string,
        data: IRuntimeTagRequest,
        params: RequestParams = {}
    ) =>
        this.http.request<IAddRuntimeVersionTagData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/tag`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description build image for runtime
     *
     * @tags Runtime
     * @name BuildRuntimeImage
     * @summary build image for runtime
     * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/image/build
     * @secure
     * @response `200` `IBuildRuntimeImageData` OK
     */
    buildRuntimeImage = (
        projectUrl: string,
        runtimeUrl: string,
        versionUrl: string,
        data: IRunEnvs,
        params: RequestParams = {}
    ) =>
        this.http.request<IBuildRuntimeImageData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/image/build`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description Select a historical version of the runtime and revert the latest version of the current runtime to this version
     *
     * @tags Runtime
     * @name RevertRuntimeVersion
     * @summary Revert Runtime version
     * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/revert
     * @secure
     * @response `200` `IRevertRuntimeVersionData` OK
     */
    revertRuntimeVersion = (
        projectUrl: string,
        runtimeUrl: string,
        data: IRuntimeRevertRequest,
        params: RequestParams = {}
    ) =>
        this.http.request<IRevertRuntimeVersionData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/revert`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description Create a new version of the runtime. The data resources can be selected by uploading the file package or entering the server path.
     *
     * @tags Runtime
     * @name Upload
     * @summary Create a new runtime version
     * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeName}/version/{versionName}/file
     * @secure
     * @response `200` `IUploadData` OK
     */
    upload = (
        projectUrl: string,
        runtimeName: string,
        versionName: string,
        query: {
            uploadRequest: IClientRuntimeRequest
        },
        data: {
            /** @format binary */
            file: File
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IUploadData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeName}/version/${versionName}/file`,
            method: 'POST',
            query: query,
            body: data,
            secure: true,
            type: ContentType.FormData,
            ...params,
        })

    /**
     * No description
     *
     * @tags Project
     * @name ListProjectRole
     * @summary List project roles
     * @request GET:/api/v1/project/{projectUrl}/role
     * @secure
     * @response `200` `IListProjectRoleData` OK
     */
    listProjectRole = (projectUrl: string, params: RequestParams = {}) =>
        this.http.request<IListProjectRoleData, any>({
            path: `/api/v1/project/${projectUrl}/role`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useListProjectRole = (projectUrl: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['listProjectRole', projectUrl, params]),
            () => this.listProjectRole(projectUrl, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Project
     * @name AddProjectRole
     * @summary Grant project role to a user
     * @request POST:/api/v1/project/{projectUrl}/role
     * @secure
     * @response `200` `IAddProjectRoleData` OK
     */
    addProjectRole = (
        projectUrl: string,
        query: {
            userId: string
            roleId: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IAddProjectRoleData, any>({
            path: `/api/v1/project/${projectUrl}/role`,
            method: 'POST',
            query: query,
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Report
     * @name ListReports
     * @summary Get the list of reports
     * @request GET:/api/v1/project/{projectUrl}/report
     * @secure
     * @response `200` `IListReportsData` OK
     */
    listReports = (
        projectUrl: string,
        query?: {
            title?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListReportsData, any>({
            path: `/api/v1/project/${projectUrl}/report`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListReports = (
        projectUrl: string,
        query?: {
            title?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listReports', projectUrl, query, params]),
            () => this.listReports(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Report
     * @name CreateReport
     * @request POST:/api/v1/project/{projectUrl}/report
     * @secure
     * @response `200` `ICreateReportData` OK
     */
    createReport = (projectUrl: string, data: ICreateReportRequest, params: RequestParams = {}) =>
        this.http.request<ICreateReportData, any>({
            path: `/api/v1/project/${projectUrl}/report`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Report
     * @name Transfer
     * @request POST:/api/v1/project/{projectUrl}/report/{reportId}/transfer
     * @secure
     * @response `200` `ITransferData` OK
     */
    transfer = (projectUrl: string, reportId: number, data: ITransferReportRequest, params: RequestParams = {}) =>
        this.http.request<ITransferData, any>({
            path: `/api/v1/project/${projectUrl}/report/${reportId}/transfer`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name ListModelVersionTags
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `IListModelVersionTagsData` OK
     */
    listModelVersionTags = (projectUrl: string, modelUrl: string, versionUrl: string, params: RequestParams = {}) =>
        this.http.request<IListModelVersionTagsData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/tag`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useListModelVersionTags = (projectUrl: string, modelUrl: string, versionUrl: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['listModelVersionTags', projectUrl, modelUrl, versionUrl, params]),
            () => this.listModelVersionTags(projectUrl, modelUrl, versionUrl, params),
            {
                enabled: [projectUrl, modelUrl, versionUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Model
     * @name AddModelVersionTag
     * @request POST:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `IAddModelVersionTagData` OK
     */
    addModelVersionTag = (
        projectUrl: string,
        modelUrl: string,
        versionUrl: string,
        data: IModelTagRequest,
        params: RequestParams = {}
    ) =>
        this.http.request<IAddModelVersionTagData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/tag`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name RevertModelVersion
     * @request POST:/api/v1/project/{projectUrl}/model/{modelUrl}/revert
     * @secure
     * @response `200` `IRevertModelVersionData` OK
     */
    revertModelVersion = (
        projectUrl: string,
        modelUrl: string,
        data: IRevertModelVersionRequest,
        params: RequestParams = {}
    ) =>
        this.http.request<IRevertModelVersionData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/revert`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Job
     * @name ListJobs
     * @summary Get the list of jobs
     * @request GET:/api/v1/project/{projectUrl}/job
     * @secure
     * @response `200` `IListJobsData` OK
     */
    listJobs = (
        projectUrl: string,
        query?: {
            swmpId?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListJobsData, any>({
            path: `/api/v1/project/${projectUrl}/job`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListJobs = (
        projectUrl: string,
        query?: {
            swmpId?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listJobs', projectUrl, query, params]),
            () => this.listJobs(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Job
     * @name CreateJob
     * @summary Create a new job
     * @request POST:/api/v1/project/{projectUrl}/job
     * @secure
     * @response `200` `ICreateJobData` OK
     */
    createJob = (projectUrl: string, data: IJobRequest, params: RequestParams = {}) =>
        this.http.request<ICreateJobData, any>({
            path: `/api/v1/project/${projectUrl}/job`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Job
     * @name Action
     * @summary Job Action
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/{action}
     * @secure
     * @response `200` `IActionData` OK
     */
    action = (projectUrl: string, jobUrl: string, action: string, params: RequestParams = {}) =>
        this.http.request<IActionData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}/${action}`,
            method: 'POST',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Job
     * @name Exec
     * @summary Execute command in running task
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/task/{taskId}/exec
     * @secure
     * @response `200` `IExecData` OK
     */
    exec = (projectUrl: string, jobUrl: string, taskId: string, data: IExecRequest, params: RequestParams = {}) =>
        this.http.request<IExecData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task/${taskId}/exec`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Job
     * @name RecoverJob
     * @summary Recover job
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/recover
     * @secure
     * @response `200` `IRecoverJobData` OK
     */
    recoverJob = (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
        this.http.request<IRecoverJobData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}/recover`,
            method: 'POST',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Job
     * @name ModifyJobPinStatus
     * @summary Pin Job
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/pin
     * @secure
     * @response `200` `IModifyJobPinStatusData` OK
     */
    modifyJobPinStatus = (projectUrl: string, jobUrl: string, data: IJobModifyPinRequest, params: RequestParams = {}) =>
        this.http.request<IModifyJobPinStatusData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}/pin`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Job
     * @name GetEvents
     * @summary Get events of job or task
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/event
     * @secure
     * @response `200` `IGetEventsData` OK
     */
    getEvents = (
        projectUrl: string,
        jobUrl: string,
        query?: {
            /** @format int64 */
            taskId?: number
            /** @format int64 */
            runId?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IGetEventsData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}/event`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useGetEvents = (
        projectUrl: string,
        jobUrl: string,
        query?: {
            /** @format int64 */
            taskId?: number
            /** @format int64 */
            runId?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['getEvents', projectUrl, jobUrl, query, params]),
            () => this.getEvents(projectUrl, jobUrl, query, params),
            {
                enabled: [projectUrl, jobUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Job
     * @name AddEvent
     * @summary Add event to job or task
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/event
     * @secure
     * @response `200` `IAddEventData` OK
     */
    addEvent = (projectUrl: string, jobUrl: string, data: IEventRequest, params: RequestParams = {}) =>
        this.http.request<IAddEventData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}/event`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description Sign uris to get a batch of temporarily accessible links
     *
     * @tags Evaluation
     * @name SignLinks
     * @summary Sign uris to get a batch of temporarily accessible links
     * @request POST:/api/v1/project/{projectUrl}/evaluation/{version}/uri/sign-links
     * @deprecated
     * @secure
     * @response `200` `ISignLinksData` ok
     */
    signLinks = (
        projectUrl: string,
        version: string,
        data: string[],
        query?: {
            /**
             * the link will be expired after expTimeMillis
             * @format int64
             */
            expTimeMillis?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<ISignLinksData, any>({
            path: `/api/v1/project/${projectUrl}/evaluation/${version}/uri/sign-links`,
            method: 'POST',
            query: query,
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description 404 if not exists; 200 if exists
     *
     * @tags Evaluation
     * @name GetHashedBlob
     * @summary Download the hashed blob in this evaluation
     * @request GET:/api/v1/project/{projectUrl}/evaluation/{version}/hashedBlob/{hash}
     * @secure
     * @response `200` `IGetHashedBlobData` ok
     */
    getHashedBlob = (projectUrl: string, version: string, hash: string, params: RequestParams = {}) =>
        this.http.request<IGetHashedBlobData, any>({
            path: `/api/v1/project/${projectUrl}/evaluation/${version}/hashedBlob/${hash}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetHashedBlob = (projectUrl: string, version: string, hash: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getHashedBlob', projectUrl, version, hash, params]),
            () => this.getHashedBlob(projectUrl, version, hash, params),
            {
                enabled: [projectUrl, version, hash].every(Boolean),
            }
        )
    /**
     * @description Upload a hashed BLOB to evaluation object store, returns a uri of the main storage
     *
     * @tags Evaluation
     * @name UploadHashedBlob
     * @summary Upload a hashed BLOB to evaluation object store
     * @request POST:/api/v1/project/{projectUrl}/evaluation/{version}/hashedBlob/{hash}
     * @secure
     * @response `200` `IUploadHashedBlobData` ok
     */
    uploadHashedBlob = (
        projectUrl: string,
        version: string,
        hash: string,
        data: {
            /**
             * file content
             * @format binary
             */
            file: File
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IUploadHashedBlobData, any>({
            path: `/api/v1/project/${projectUrl}/evaluation/${version}/hashedBlob/${hash}`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.FormData,
            ...params,
        })

    /**
     * @description 404 if not exists; 200 if exists
     *
     * @tags Evaluation
     * @name HeadHashedBlob
     * @summary Test if a hashed blob exists in this evaluation
     * @request HEAD:/api/v1/project/{projectUrl}/evaluation/{version}/hashedBlob/{hash}
     * @secure
     * @response `200` `IHeadHashedBlobData` ok
     */
    headHashedBlob = (projectUrl: string, version: string, hash: string, params: RequestParams = {}) =>
        this.http.request<IHeadHashedBlobData, any>({
            path: `/api/v1/project/${projectUrl}/evaluation/${version}/hashedBlob/${hash}`,
            method: 'HEAD',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Evaluation
     * @name GetViewConfig
     * @summary Get View Config
     * @request GET:/api/v1/project/{projectUrl}/evaluation/view/config
     * @secure
     * @response `200` `IGetViewConfigData` OK
     */
    getViewConfig = (
        projectUrl: string,
        query: {
            name: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IGetViewConfigData, any>({
            path: `/api/v1/project/${projectUrl}/evaluation/view/config`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useGetViewConfig = (
        projectUrl: string,
        query: {
            name: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['getViewConfig', projectUrl, query, params]),
            () => this.getViewConfig(projectUrl, query, params),
            {
                enabled: [projectUrl, query].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Evaluation
     * @name CreateViewConfig
     * @summary Create or Update View Config
     * @request POST:/api/v1/project/{projectUrl}/evaluation/view/config
     * @secure
     * @response `200` `ICreateViewConfigData` OK
     */
    createViewConfig = (projectUrl: string, data: IConfigRequest, params: RequestParams = {}) =>
        this.http.request<ICreateViewConfigData, any>({
            path: `/api/v1/project/${projectUrl}/evaluation/view/config`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Dataset
     * @name ListDatasetVersionTags
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `IListDatasetVersionTagsData` OK
     */
    listDatasetVersionTags = (projectUrl: string, datasetUrl: string, versionUrl: string, params: RequestParams = {}) =>
        this.http.request<IListDatasetVersionTagsData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/tag`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useListDatasetVersionTags = (
        projectUrl: string,
        datasetUrl: string,
        versionUrl: string,
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listDatasetVersionTags', projectUrl, datasetUrl, versionUrl, params]),
            () => this.listDatasetVersionTags(projectUrl, datasetUrl, versionUrl, params),
            {
                enabled: [projectUrl, datasetUrl, versionUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Dataset
     * @name AddDatasetVersionTag
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `IAddDatasetVersionTagData` OK
     */
    addDatasetVersionTag = (
        projectUrl: string,
        datasetUrl: string,
        versionUrl: string,
        data: IDatasetTagRequest,
        params: RequestParams = {}
    ) =>
        this.http.request<IAddDatasetVersionTagData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/tag`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Dataset
     * @name ConsumeNextData
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/consume
     * @secure
     * @response `200` `IConsumeNextDataData` OK
     */
    consumeNextData = (
        projectUrl: string,
        datasetUrl: string,
        versionUrl: string,
        data: IDataConsumptionRequest,
        params: RequestParams = {}
    ) =>
        this.http.request<IConsumeNextDataData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/consume`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description Select a historical version of the dataset and revert the latest version of the current dataset to this version
     *
     * @tags Dataset
     * @name RevertDatasetVersion
     * @summary Revert Dataset version
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/revert
     * @secure
     * @response `200` `IRevertDatasetVersionData` OK
     */
    revertDatasetVersion = (
        projectUrl: string,
        datasetUrl: string,
        data: IRevertDatasetRequest,
        params: RequestParams = {}
    ) =>
        this.http.request<IRevertDatasetVersionData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/revert`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description Create a new version of the dataset. The data resources can be selected by uploading the file package or entering the server path.
     *
     * @tags Dataset
     * @name UploadDs
     * @summary Create a new dataset version
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetName}/version/{versionName}/file
     * @deprecated
     * @secure
     * @response `200` `IUploadDsData` OK
     */
    uploadDs = (
        projectUrl: string,
        datasetName: string,
        versionName: string,
        query: {
            uploadRequest: IDatasetUploadRequest
        },
        data: {
            /** @format binary */
            file?: File
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IUploadDsData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetName}/version/${versionName}/file`,
            method: 'POST',
            query: query,
            body: data,
            secure: true,
            type: ContentType.FormData,
            ...params,
        })

    /**
     * @description Build Dataset
     *
     * @tags Dataset
     * @name BuildDataset
     * @summary Build Dataset
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetName}/build
     * @secure
     * @response `200` `IBuildDatasetData` OK
     */
    buildDataset = (projectUrl: string, datasetName: string, data: IDatasetBuildRequest, params: RequestParams = {}) =>
        this.http.request<IBuildDatasetData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetName}/build`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description Sign SWDS uris to get a batch of temporarily accessible links
     *
     * @tags Dataset
     * @name SignLinks1
     * @summary Sign SWDS uris to get a batch of temporarily accessible links
     * @request POST:/api/v1/project/{projectName}/dataset/{datasetName}/uri/sign-links
     * @deprecated
     * @secure
     * @response `200` `ISignLinks1Data` OK
     */
    signLinks1 = (
        projectName: string,
        datasetName: string,
        data: string[],
        query?: {
            /**
             * the link will be expired after expTimeMillis
             * @format int64
             */
            expTimeMillis?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<ISignLinks1Data, any>({
            path: `/api/v1/project/${projectName}/dataset/${datasetName}/uri/sign-links`,
            method: 'POST',
            query: query,
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description 404 if not exists; 200 if exists
     *
     * @tags Dataset
     * @name GetHashedBlob1
     * @summary Download the hashed blob in this dataset
     * @request GET:/api/v1/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}
     * @secure
     * @response `200` `IGetHashedBlob1Data` OK
     */
    getHashedBlob1 = (projectName: string, datasetName: string, hash: string, params: RequestParams = {}) =>
        this.http.request<IGetHashedBlob1Data, any>({
            path: `/api/v1/project/${projectName}/dataset/${datasetName}/hashedBlob/${hash}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetHashedBlob1 = (projectName: string, datasetName: string, hash: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getHashedBlob1', projectName, datasetName, hash, params]),
            () => this.getHashedBlob1(projectName, datasetName, hash, params),
            {
                enabled: [projectName, datasetName, hash].every(Boolean),
            }
        )
    /**
     * @description Upload a hashed BLOB to dataset object store, returns a uri of the main storage
     *
     * @tags Dataset
     * @name UploadHashedBlob1
     * @summary Upload a hashed BLOB to dataset object store
     * @request POST:/api/v1/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}
     * @secure
     * @response `200` `IUploadHashedBlob1Data` OK
     */
    uploadHashedBlob1 = (
        projectName: string,
        datasetName: string,
        hash: string,
        data: {
            /** @format binary */
            file: File
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IUploadHashedBlob1Data, any>({
            path: `/api/v1/project/${projectName}/dataset/${datasetName}/hashedBlob/${hash}`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.FormData,
            ...params,
        })

    /**
     * @description 404 if not exists; 200 if exists
     *
     * @tags Dataset
     * @name HeadHashedBlob1
     * @summary Test if a hashed blob exists in this dataset
     * @request HEAD:/api/v1/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}
     * @secure
     * @response `200` `IHeadHashedBlob1Data` OK
     */
    headHashedBlob1 = (projectName: string, datasetName: string, hash: string, params: RequestParams = {}) =>
        this.http.request<IHeadHashedBlob1Data, any>({
            path: `/api/v1/project/${projectName}/dataset/${datasetName}/hashedBlob/${hash}`,
            method: 'HEAD',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags FineTune
     * @name ListSpace
     * @summary Get the list of fine-tune spaces
     * @request GET:/api/v1/project/{projectId}/ftspace
     * @secure
     * @response `200` `IListSpaceData` OK
     */
    listSpace = (
        projectId: number,
        query?: {
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListSpaceData, any>({
            path: `/api/v1/project/${projectId}/ftspace`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListSpace = (
        projectId: number,
        query?: {
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listSpace', projectId, query, params]),
            () => this.listSpace(projectId, query, params),
            {
                enabled: [projectId].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags FineTune
     * @name CreateSpace
     * @summary Create fine-tune space
     * @request POST:/api/v1/project/{projectId}/ftspace
     * @secure
     * @response `200` `ICreateSpaceData` OK
     */
    createSpace = (projectId: number, data: IFineTuneSpaceCreateRequest, params: RequestParams = {}) =>
        this.http.request<ICreateSpaceData, any>({
            path: `/api/v1/project/${projectId}/ftspace`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags FineTune
     * @name ImportEval
     * @summary import from common eval summary
     * @request POST:/api/v1/project/{projectId}/ftspace/{spaceId}/eval/import
     * @secure
     * @response `200` `IImportEvalData` OK
     */
    importEval = (projectId: number, spaceId: number, data: IFineTuneMigrationRequest, params: RequestParams = {}) =>
        this.http.request<IImportEvalData, any>({
            path: `/api/v1/project/${projectId}/ftspace/${spaceId}/eval/import`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags FineTune
     * @name ExportEval
     * @summary export to common eval summary
     * @request POST:/api/v1/project/{projectId}/ftspace/{spaceId}/eval/export
     * @secure
     * @response `200` `IExportEvalData` OK
     */
    exportEval = (projectId: number, spaceId: number, data: IFineTuneMigrationRequest, params: RequestParams = {}) =>
        this.http.request<IExportEvalData, any>({
            path: `/api/v1/project/${projectId}/ftspace/${spaceId}/eval/export`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description Get panel setting by project and key
     *
     * @tags Panel
     * @name GetPanelSetting
     * @summary Get panel setting
     * @request GET:/api/v1/panel/setting/{projectUrl}/{key}
     * @secure
     * @response `200` `IGetPanelSettingData` OK
     */
    getPanelSetting = (projectUrl: string, key: string, params: RequestParams = {}) =>
        this.http.request<IGetPanelSettingData, any>({
            path: `/api/v1/panel/setting/${projectUrl}/${key}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetPanelSetting = (projectUrl: string, key: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getPanelSetting', projectUrl, key, params]),
            () => this.getPanelSetting(projectUrl, key, params),
            {
                enabled: [projectUrl, key].every(Boolean),
            }
        )
    /**
     * @description Save panel setting by project and key
     *
     * @tags Panel
     * @name SetPanelSetting
     * @summary Save panel setting
     * @request POST:/api/v1/panel/setting/{projectUrl}/{key}
     * @secure
     * @response `200` `ISetPanelSettingData` OK
     */
    setPanelSetting = (projectUrl: string, key: string, data: string, params: RequestParams = {}) =>
        this.http.request<ISetPanelSettingData, any>({
            path: `/api/v1/panel/setting/${projectUrl}/${key}`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * @description List all plugins
     *
     * @tags Panel
     * @name PluginList
     * @summary List all plugins
     * @request GET:/api/v1/panel/plugin
     * @secure
     * @response `200` `IPluginListData` OK
     */
    pluginList = (params: RequestParams = {}) =>
        this.http.request<IPluginListData, any>({
            path: `/api/v1/panel/plugin`,
            method: 'GET',
            secure: true,
            ...params,
        })

    usePluginList = (params: RequestParams = {}) =>
        useQuery(qs.stringify(['pluginList', params]), () => this.pluginList(params), {
            enabled: [].every(Boolean),
        })
    /**
     * @description Upload a tarball and install as panel plugin
     *
     * @tags Panel
     * @name InstallPlugin
     * @summary Install a plugin
     * @request POST:/api/v1/panel/plugin
     * @secure
     * @response `200` `IInstallPluginData` OK
     */
    installPlugin = (
        data: {
            /**
             * file detail
             * @format binary
             */
            file: File
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IInstallPluginData, any>({
            path: `/api/v1/panel/plugin`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.FormData,
            ...params,
        })

    /**
     * @description Sign uris to get a batch of temporarily accessible links
     *
     * @tags File storage
     * @name SignLinks2
     * @summary Sign uris to get a batch of temporarily accessible links
     * @request POST:/api/v1/filestorage/sign-links
     * @secure
     * @response `200` `ISignLinks2Data` ok
     */
    signLinks2 = (
        query: {
            /**
             * the link will be expired after expTimeMillis
             * @format int64
             */
            expTimeMillis: number
        },
        data: string[],
        params: RequestParams = {}
    ) =>
        this.http.request<ISignLinks2Data, any>({
            path: `/api/v1/filestorage/sign-links`,
            method: 'POST',
            query: query,
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags data-store-controller
     * @name UpdateTable
     * @request POST:/api/v1/datastore/updateTable
     * @secure
     * @response `200` `IUpdateTableData` OK
     */
    updateTable = (data: IUpdateTableRequest, params: RequestParams = {}) =>
        this.http.request<IUpdateTableData, any>({
            path: `/api/v1/datastore/updateTable`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags data-store-controller
     * @name ScanTable
     * @request POST:/api/v1/datastore/scanTable
     * @secure
     * @response `200` `IScanTableData` OK
     */
    scanTable = (data: IScanTableRequest, params: RequestParams = {}) =>
        this.http.request<IScanTableData, any>({
            path: `/api/v1/datastore/scanTable`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags data-store-controller
     * @name ScanAndExport
     * @request POST:/api/v1/datastore/scanTable/export
     * @secure
     * @response `200` `IScanAndExportData` OK
     */
    scanAndExport = (data: IScanTableRequest, params: RequestParams = {}) =>
        this.http.request<IScanAndExportData, any>({
            path: `/api/v1/datastore/scanTable/export`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags data-store-controller
     * @name QueryTable
     * @request POST:/api/v1/datastore/queryTable
     * @secure
     * @response `200` `IQueryTableData` OK
     */
    queryTable = (data: IQueryTableRequest, params: RequestParams = {}) =>
        this.http.request<IQueryTableData, any>({
            path: `/api/v1/datastore/queryTable`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags data-store-controller
     * @name QueryAndExport
     * @request POST:/api/v1/datastore/queryTable/export
     * @secure
     * @response `200` `IQueryAndExportData` OK
     */
    queryAndExport = (data: IQueryTableRequest, params: RequestParams = {}) =>
        this.http.request<IQueryAndExportData, any>({
            path: `/api/v1/datastore/queryTable/export`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags data-store-controller
     * @name ListTables
     * @request POST:/api/v1/datastore/listTables
     * @secure
     * @response `200` `IListTablesData` OK
     */
    listTables = (data: IListTablesRequest, params: RequestParams = {}) =>
        this.http.request<IListTablesData, any>({
            path: `/api/v1/datastore/listTables`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags data-store-controller
     * @name Flush
     * @request POST:/api/v1/datastore/flush
     * @secure
     * @response `200` `IFlushData` OK
     */
    flush = (
        query: {
            request: IFlushRequest
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IFlushData, any>({
            path: `/api/v1/datastore/flush`,
            method: 'POST',
            query: query,
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name InitUploadBlob
     * @request POST:/api/v1/blob
     * @secure
     * @response `200` `IInitUploadBlobData` OK
     */
    initUploadBlob = (data: IInitUploadBlobRequest, params: RequestParams = {}) =>
        this.http.request<IInitUploadBlobData, any>({
            path: `/api/v1/blob`,
            method: 'POST',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name CompleteUploadBlob
     * @request POST:/api/v1/blob/{blobId}
     * @secure
     * @response `200` `ICompleteUploadBlobData` OK
     */
    completeUploadBlob = (blobId: string, params: RequestParams = {}) =>
        this.http.request<ICompleteUploadBlobData, any>({
            path: `/api/v1/blob/${blobId}`,
            method: 'POST',
            secure: true,
            ...params,
        })

    /**
     * @description head for runtime info
     *
     * @tags Runtime
     * @name HeadRuntime
     * @summary head for runtime info
     * @request HEAD:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}
     * @secure
     * @response `200` `IHeadRuntimeData` OK
     */
    headRuntime = (projectUrl: string, runtimeUrl: string, versionUrl: string, params: RequestParams = {}) =>
        this.http.request<IHeadRuntimeData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}`,
            method: 'HEAD',
            secure: true,
            ...params,
        })

    /**
     * @description head for dataset info
     *
     * @tags Dataset
     * @name HeadDataset
     * @summary head for dataset info
     * @request HEAD:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}
     * @secure
     * @response `200` `IHeadDatasetData` OK
     */
    headDataset = (projectUrl: string, datasetUrl: string, versionUrl: string, params: RequestParams = {}) =>
        this.http.request<IHeadDatasetData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}`,
            method: 'HEAD',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags User
     * @name GetUserById
     * @summary Get a user by user ID
     * @request GET:/api/v1/user/{userId}
     * @secure
     * @response `200` `IGetUserByIdData` OK
     */
    getUserById = (userId: string, params: RequestParams = {}) =>
        this.http.request<IGetUserByIdData, any>({
            path: `/api/v1/user/${userId}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetUserById = (userId: string, params: RequestParams = {}) =>
        useQuery(qs.stringify(['getUserById', userId, params]), () => this.getUserById(userId, params), {
            enabled: [userId].every(Boolean),
        })
    /**
     * @description Get token of any user for third party system integration, only super admin is allowed to do this
     *
     * @tags User
     * @name UserToken
     * @summary Get arbitrary user token
     * @request GET:/api/v1/user/token/{userId}
     * @secure
     * @response `200` `IUserTokenData` OK
     */
    userToken = (userId: number, params: RequestParams = {}) =>
        this.http.request<IUserTokenData, any>({
            path: `/api/v1/user/token/${userId}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useUserToken = (userId: number, params: RequestParams = {}) =>
        useQuery(qs.stringify(['userToken', userId, params]), () => this.userToken(userId, params), {
            enabled: [userId].every(Boolean),
        })
    /**
     * No description
     *
     * @tags User
     * @name GetCurrentUser
     * @summary Get the current logged in user.
     * @request GET:/api/v1/user/current
     * @secure
     * @response `200` `IGetCurrentUserData` OK
     */
    getCurrentUser = (params: RequestParams = {}) =>
        this.http.request<IGetCurrentUserData, any>({
            path: `/api/v1/user/current`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetCurrentUser = (params: RequestParams = {}) =>
        useQuery(qs.stringify(['getCurrentUser', params]), () => this.getCurrentUser(params), {
            enabled: [].every(Boolean),
        })
    /**
     * No description
     *
     * @tags User
     * @name GetCurrentUserRoles
     * @summary Get the current user roles.
     * @request GET:/api/v1/user/current/role
     * @secure
     * @response `200` `IGetCurrentUserRolesData` OK
     */
    getCurrentUserRoles = (
        query: {
            projectUrl: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IGetCurrentUserRolesData, any>({
            path: `/api/v1/user/current/role`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useGetCurrentUserRoles = (
        query: {
            projectUrl: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(qs.stringify(['getCurrentUserRoles', query, params]), () => this.getCurrentUserRoles(query, params), {
            enabled: [query].every(Boolean),
        })
    /**
     * No description
     *
     * @tags System
     * @name GetCurrentVersion
     * @summary Get current version of the system
     * @request GET:/api/v1/system/version
     * @secure
     * @response `200` `IGetCurrentVersionData` OK
     */
    getCurrentVersion = (params: RequestParams = {}) =>
        this.http.request<IGetCurrentVersionData, any>({
            path: `/api/v1/system/version`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetCurrentVersion = (params: RequestParams = {}) =>
        useQuery(qs.stringify(['getCurrentVersion', params]), () => this.getCurrentVersion(params), {
            enabled: [].every(Boolean),
        })
    /**
     * @description Get system features list
     *
     * @tags System
     * @name QueryFeatures
     * @summary Get system features
     * @request GET:/api/v1/system/features
     * @secure
     * @response `200` `IQueryFeaturesData` OK
     */
    queryFeatures = (params: RequestParams = {}) =>
        this.http.request<IQueryFeaturesData, any>({
            path: `/api/v1/system/features`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useQueryFeatures = (params: RequestParams = {}) =>
        useQuery(qs.stringify(['queryFeatures', params]), () => this.queryFeatures(params), {
            enabled: [].every(Boolean),
        })
    /**
     * No description
     *
     * @tags env-controller
     * @name ListDevice
     * @summary Get the list of device types
     * @request GET:/api/v1/runtime/device
     * @secure
     * @response `200` `IListDeviceData` OK
     */
    listDevice = (params: RequestParams = {}) =>
        this.http.request<IListDeviceData, any>({
            path: `/api/v1/runtime/device`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useListDevice = (params: RequestParams = {}) =>
        useQuery(qs.stringify(['listDevice', params]), () => this.listDevice(params), {
            enabled: [].every(Boolean),
        })
    /**
     * No description
     *
     * @tags User
     * @name ListRoles
     * @summary List role enums
     * @request GET:/api/v1/role/enums
     * @secure
     * @response `200` `IListRolesData` OK
     */
    listRoles = (params: RequestParams = {}) =>
        this.http.request<IListRolesData, any>({
            path: `/api/v1/role/enums`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useListRoles = (params: RequestParams = {}) =>
        useQuery(qs.stringify(['listRoles', params]), () => this.listRoles(params), {
            enabled: [].every(Boolean),
        })
    /**
     * No description
     *
     * @tags Report
     * @name Preview
     * @request GET:/api/v1/report/{uuid}/preview
     * @secure
     * @response `200` `IPreviewData` OK
     */
    preview = (uuid: string, params: RequestParams = {}) =>
        this.http.request<IPreviewData, any>({
            path: `/api/v1/report/${uuid}/preview`,
            method: 'GET',
            secure: true,
            ...params,
        })

    usePreview = (uuid: string, params: RequestParams = {}) =>
        useQuery(qs.stringify(['preview', uuid, params]), () => this.preview(uuid, params), {
            enabled: [uuid].every(Boolean),
        })
    /**
     * No description
     *
     * @tags Model
     * @name GetModelMetaBlob
     * @request GET:/api/v1/project/{project}/model/{model}/version/{version}/meta
     * @secure
     * @response `200` `IGetModelMetaBlobData` OK
     */
    getModelMetaBlob = (
        project: string,
        model: string,
        version: string,
        query?: {
            /** @default "" */
            blobId?: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IGetModelMetaBlobData, any>({
            path: `/api/v1/project/${project}/model/${model}/version/${version}/meta`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useGetModelMetaBlob = (
        project: string,
        model: string,
        version: string,
        query?: {
            /** @default "" */
            blobId?: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['getModelMetaBlob', project, model, version, query, params]),
            () => this.getModelMetaBlob(project, model, version, query, params),
            {
                enabled: [project, model, version].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Model
     * @name ListFiles
     * @request GET:/api/v1/project/{project}/model/{model}/listFiles
     * @secure
     * @response `200` `IListFilesData` OK
     */
    listFiles = (
        project: string,
        model: string,
        query?: {
            /** @default "latest" */
            version?: string
            /** @default "" */
            path?: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListFilesData, any>({
            path: `/api/v1/project/${project}/model/${model}/listFiles`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListFiles = (
        project: string,
        model: string,
        query?: {
            /** @default "latest" */
            version?: string
            /** @default "" */
            path?: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listFiles', project, model, query, params]),
            () => this.listFiles(project, model, query, params),
            {
                enabled: [project, model].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Model
     * @name GetFileData
     * @request GET:/api/v1/project/{project}/model/{model}/getFileData
     * @secure
     * @response `200` `IGetFileDataData` OK
     */
    getFileData = (
        project: string,
        model: string,
        query: {
            /** @default "latest" */
            version?: string
            path: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IGetFileDataData, any>({
            path: `/api/v1/project/${project}/model/${model}/getFileData`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useGetFileData = (
        project: string,
        model: string,
        query: {
            /** @default "latest" */
            version?: string
            path: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['getFileData', project, model, query, params]),
            () => this.getFileData(project, model, query, params),
            {
                enabled: [project, model, query].every(Boolean),
            }
        )
    /**
     * @description List all types of trashes, such as models datasets runtimes and evaluations
     *
     * @tags Trash
     * @name ListTrash
     * @summary Get the list of trash
     * @request GET:/api/v1/project/{projectUrl}/trash
     * @secure
     * @response `200` `IListTrashData` OK
     */
    listTrash = (
        projectUrl: string,
        query?: {
            name?: string
            operator?: string
            type?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListTrashData, any>({
            path: `/api/v1/project/${projectUrl}/trash`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListTrash = (
        projectUrl: string,
        query?: {
            name?: string
            operator?: string
            type?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listTrash', projectUrl, query, params]),
            () => this.listTrash(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Template
     * @name GetTemplate
     * @summary Get Template
     * @request GET:/api/v1/project/{projectUrl}/template/{id}
     * @secure
     * @response `200` `IGetTemplateData` OK
     */
    getTemplate = (projectUrl: string, id: number, params: RequestParams = {}) =>
        this.http.request<IGetTemplateData, any>({
            path: `/api/v1/project/${projectUrl}/template/${id}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetTemplate = (projectUrl: string, id: number, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getTemplate', projectUrl, id, params]),
            () => this.getTemplate(projectUrl, id, params),
            {
                enabled: [projectUrl, id].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Template
     * @name DeleteTemplate
     * @summary Delete Template
     * @request DELETE:/api/v1/project/{projectUrl}/template/{id}
     * @secure
     * @response `200` `IDeleteTemplateData` OK
     */
    deleteTemplate = (projectUrl: string, id: number, params: RequestParams = {}) =>
        this.http.request<IDeleteTemplateData, any>({
            path: `/api/v1/project/${projectUrl}/template/${id}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Runtime
     * @name ListRuntime
     * @summary Get the list of runtimes
     * @request GET:/api/v1/project/{projectUrl}/runtime
     * @secure
     * @response `200` `IListRuntimeData` OK
     */
    listRuntime = (
        projectUrl: string,
        query?: {
            /** Runtime name prefix to search for */
            name?: string
            owner?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListRuntimeData, any>({
            path: `/api/v1/project/${projectUrl}/runtime`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListRuntime = (
        projectUrl: string,
        query?: {
            /** Runtime name prefix to search for */
            name?: string
            owner?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listRuntime', projectUrl, query, params]),
            () => this.listRuntime(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * @description Return the information of the latest version of the current runtime
     *
     * @tags Runtime
     * @name GetRuntimeInfo
     * @summary Get the information of a runtime
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}
     * @secure
     * @response `200` `IGetRuntimeInfoData` OK
     */
    getRuntimeInfo = (
        projectUrl: string,
        runtimeUrl: string,
        query?: {
            versionUrl?: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IGetRuntimeInfoData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useGetRuntimeInfo = (
        projectUrl: string,
        runtimeUrl: string,
        query?: {
            versionUrl?: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['getRuntimeInfo', projectUrl, runtimeUrl, query, params]),
            () => this.getRuntimeInfo(projectUrl, runtimeUrl, query, params),
            {
                enabled: [projectUrl, runtimeUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Runtime
     * @name DeleteRuntime
     * @summary Delete a runtime
     * @request DELETE:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}
     * @secure
     * @response `200` `IDeleteRuntimeData` OK
     */
    deleteRuntime = (projectUrl: string, runtimeUrl: string, params: RequestParams = {}) =>
        this.http.request<IDeleteRuntimeData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Runtime
     * @name ListRuntimeVersion
     * @summary Get the list of the runtime versions
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version
     * @secure
     * @response `200` `IListRuntimeVersionData` OK
     */
    listRuntimeVersion = (
        projectUrl: string,
        runtimeUrl: string,
        query?: {
            /** Runtime version name prefix */
            name?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListRuntimeVersionData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListRuntimeVersion = (
        projectUrl: string,
        runtimeUrl: string,
        query?: {
            /** Runtime version name prefix */
            name?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listRuntimeVersion', projectUrl, runtimeUrl, query, params]),
            () => this.listRuntimeVersion(projectUrl, runtimeUrl, query, params),
            {
                enabled: [projectUrl, runtimeUrl].every(Boolean),
            }
        )
    /**
     * @description Pull file of a runtime version.
     *
     * @tags Runtime
     * @name Pull
     * @summary Pull file of a runtime version
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/file
     * @secure
     * @response `200` `IPullData` OK
     */
    pull = (projectUrl: string, runtimeUrl: string, versionUrl: string, params: RequestParams = {}) =>
        this.http.request<IPullData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/file`,
            method: 'GET',
            secure: true,
            ...params,
        })

    usePull = (projectUrl: string, runtimeUrl: string, versionUrl: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['pull', projectUrl, runtimeUrl, versionUrl, params]),
            () => this.pull(projectUrl, runtimeUrl, versionUrl, params),
            {
                enabled: [projectUrl, runtimeUrl, versionUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Runtime
     * @name GetRuntimeVersionTag
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/tag/{tag}
     * @secure
     * @response `200` `IGetRuntimeVersionTagData` OK
     */
    getRuntimeVersionTag = (projectUrl: string, runtimeUrl: string, tag: string, params: RequestParams = {}) =>
        this.http.request<IGetRuntimeVersionTagData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/tag/${tag}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetRuntimeVersionTag = (projectUrl: string, runtimeUrl: string, tag: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getRuntimeVersionTag', projectUrl, runtimeUrl, tag, params]),
            () => this.getRuntimeVersionTag(projectUrl, runtimeUrl, tag, params),
            {
                enabled: [projectUrl, runtimeUrl, tag].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Runtime
     * @name ListRuntimeTree
     * @summary List runtime tree including global runtimes
     * @request GET:/api/v1/project/{projectUrl}/runtime-tree
     * @secure
     * @response `200` `IListRuntimeTreeData` OK
     */
    listRuntimeTree = (
        projectUrl: string,
        query?: {
            /**
             * Data range
             * @default "all"
             */
            scope?: 'all' | 'project' | 'shared'
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListRuntimeTreeData, any>({
            path: `/api/v1/project/${projectUrl}/runtime-tree`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListRuntimeTree = (
        projectUrl: string,
        query?: {
            /**
             * Data range
             * @default "all"
             */
            scope?: 'all' | 'project' | 'shared'
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listRuntimeTree', projectUrl, query, params]),
            () => this.listRuntimeTree(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Template
     * @name SelectRecentlyInProject
     * @summary Get Recently Templates for project
     * @request GET:/api/v1/project/{projectUrl}/recent-template
     * @secure
     * @response `200` `ISelectRecentlyInProjectData` OK
     */
    selectRecentlyInProject = (
        projectUrl: string,
        query?: {
            /**
             * @format int32
             * @min 1
             * @max 50
             * @default 5
             */
            limit?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<ISelectRecentlyInProjectData, any>({
            path: `/api/v1/project/${projectUrl}/recent-template`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useSelectRecentlyInProject = (
        projectUrl: string,
        query?: {
            /**
             * @format int32
             * @min 1
             * @max 50
             * @default 5
             */
            limit?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['selectRecentlyInProject', projectUrl, query, params]),
            () => this.selectRecentlyInProject(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Runtime
     * @name RecentRuntimeTree
     * @request GET:/api/v1/project/{projectUrl}/recent-runtime-tree
     * @secure
     * @response `200` `IRecentRuntimeTreeData` OK
     */
    recentRuntimeTree = (
        projectUrl: string,
        query?: {
            /**
             * Data limit
             * @format int32
             * @min 1
             * @max 50
             * @default 5
             */
            limit?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IRecentRuntimeTreeData, any>({
            path: `/api/v1/project/${projectUrl}/recent-runtime-tree`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useRecentRuntimeTree = (
        projectUrl: string,
        query?: {
            /**
             * Data limit
             * @format int32
             * @min 1
             * @max 50
             * @default 5
             */
            limit?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['recentRuntimeTree', projectUrl, query, params]),
            () => this.recentRuntimeTree(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Model
     * @name RecentModelTree
     * @request GET:/api/v1/project/{projectUrl}/recent-model-tree
     * @secure
     * @response `200` `IRecentModelTreeData` OK
     */
    recentModelTree = (
        projectUrl: string,
        query?: {
            /**
             * Data limit
             * @format int32
             * @min 1
             * @max 50
             * @default 5
             */
            limit?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IRecentModelTreeData, any>({
            path: `/api/v1/project/${projectUrl}/recent-model-tree`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useRecentModelTree = (
        projectUrl: string,
        query?: {
            /**
             * Data limit
             * @format int32
             * @min 1
             * @max 50
             * @default 5
             */
            limit?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['recentModelTree', projectUrl, query, params]),
            () => this.recentModelTree(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Dataset
     * @name RecentDatasetTree
     * @request GET:/api/v1/project/{projectUrl}/recent-dataset-tree
     * @secure
     * @response `200` `IRecentDatasetTreeData` OK
     */
    recentDatasetTree = (
        projectUrl: string,
        query?: {
            /**
             * Data limit
             * @format int32
             * @min 1
             * @max 50
             * @default 5
             */
            limit?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IRecentDatasetTreeData, any>({
            path: `/api/v1/project/${projectUrl}/recent-dataset-tree`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useRecentDatasetTree = (
        projectUrl: string,
        query?: {
            /**
             * Data limit
             * @format int32
             * @min 1
             * @max 50
             * @default 5
             */
            limit?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['recentDatasetTree', projectUrl, query, params]),
            () => this.recentDatasetTree(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * @description Returns readme content.
     *
     * @tags Project
     * @name GetProjectReadmeByUrl
     * @summary Get a project readme by Url
     * @request GET:/api/v1/project/{projectUrl}/readme
     * @secure
     * @response `200` `IGetProjectReadmeByUrlData` OK
     */
    getProjectReadmeByUrl = (projectUrl: string, params: RequestParams = {}) =>
        this.http.request<IGetProjectReadmeByUrlData, any>({
            path: `/api/v1/project/${projectUrl}/readme`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetProjectReadmeByUrl = (projectUrl: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getProjectReadmeByUrl', projectUrl, params]),
            () => this.getProjectReadmeByUrl(projectUrl, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Model
     * @name ListModel
     * @request GET:/api/v1/project/{projectUrl}/model
     * @secure
     * @response `200` `IListModelData` OK
     */
    listModel = (
        projectUrl: string,
        query?: {
            versionId?: string
            name?: string
            owner?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListModelData, any>({
            path: `/api/v1/project/${projectUrl}/model`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListModel = (
        projectUrl: string,
        query?: {
            versionId?: string
            name?: string
            owner?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listModel', projectUrl, query, params]),
            () => this.listModel(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Model
     * @name GetModelInfo
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}
     * @secure
     * @response `200` `IGetModelInfoData` OK
     */
    getModelInfo = (
        projectUrl: string,
        modelUrl: string,
        query?: {
            versionUrl?: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IGetModelInfoData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useGetModelInfo = (
        projectUrl: string,
        modelUrl: string,
        query?: {
            versionUrl?: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['getModelInfo', projectUrl, modelUrl, query, params]),
            () => this.getModelInfo(projectUrl, modelUrl, query, params),
            {
                enabled: [projectUrl, modelUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Model
     * @name DeleteModel
     * @request DELETE:/api/v1/project/{projectUrl}/model/{modelUrl}
     * @secure
     * @response `200` `IDeleteModelData` OK
     */
    deleteModel = (projectUrl: string, modelUrl: string, params: RequestParams = {}) =>
        this.http.request<IDeleteModelData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name ListModelVersion
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/version
     * @secure
     * @response `200` `IListModelVersionData` OK
     */
    listModelVersion = (
        projectUrl: string,
        modelUrl: string,
        query?: {
            name?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListModelVersionData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListModelVersion = (
        projectUrl: string,
        modelUrl: string,
        query?: {
            name?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listModelVersion', projectUrl, modelUrl, query, params]),
            () => this.listModelVersion(projectUrl, modelUrl, query, params),
            {
                enabled: [projectUrl, modelUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Model
     * @name GetModelVersionTag
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/tag/{tag}
     * @secure
     * @response `200` `IGetModelVersionTagData` OK
     */
    getModelVersionTag = (projectUrl: string, modelUrl: string, tag: string, params: RequestParams = {}) =>
        this.http.request<IGetModelVersionTagData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/tag/${tag}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetModelVersionTag = (projectUrl: string, modelUrl: string, tag: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getModelVersionTag', projectUrl, modelUrl, tag, params]),
            () => this.getModelVersionTag(projectUrl, modelUrl, tag, params),
            {
                enabled: [projectUrl, modelUrl, tag].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Model
     * @name GetModelDiff
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/diff
     * @secure
     * @response `200` `IGetModelDiffData` OK
     */
    getModelDiff = (
        projectUrl: string,
        modelUrl: string,
        query: {
            baseVersion: string
            compareVersion: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IGetModelDiffData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/diff`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useGetModelDiff = (
        projectUrl: string,
        modelUrl: string,
        query: {
            baseVersion: string
            compareVersion: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['getModelDiff', projectUrl, modelUrl, query, params]),
            () => this.getModelDiff(projectUrl, modelUrl, query, params),
            {
                enabled: [projectUrl, modelUrl, query].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Model
     * @name ListModelTree
     * @request GET:/api/v1/project/{projectUrl}/model-tree
     * @secure
     * @response `200` `IListModelTreeData` OK
     */
    listModelTree = (
        projectUrl: string,
        query?: {
            /**
             * Data range
             * @default "all"
             */
            scope?: 'all' | 'project' | 'shared'
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListModelTreeData, any>({
            path: `/api/v1/project/${projectUrl}/model-tree`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListModelTree = (
        projectUrl: string,
        query?: {
            /**
             * Data range
             * @default "all"
             */
            scope?: 'all' | 'project' | 'shared'
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listModelTree', projectUrl, query, params]),
            () => this.listModelTree(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Job
     * @name ListTasks
     * @summary Get the list of tasks
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/task
     * @secure
     * @response `200` `IListTasksData` OK
     */
    listTasks = (
        projectUrl: string,
        jobUrl: string,
        query?: {
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListTasksData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListTasks = (
        projectUrl: string,
        jobUrl: string,
        query?: {
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listTasks', projectUrl, jobUrl, query, params]),
            () => this.listTasks(projectUrl, jobUrl, query, params),
            {
                enabled: [projectUrl, jobUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Job
     * @name GetTask
     * @summary Get task info
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/task/{taskUrl}
     * @secure
     * @response `200` `IGetTaskData` OK
     */
    getTask = (projectUrl: string, jobUrl: string, taskUrl: string, params: RequestParams = {}) =>
        this.http.request<IGetTaskData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task/${taskUrl}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetTask = (projectUrl: string, jobUrl: string, taskUrl: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getTask', projectUrl, jobUrl, taskUrl, params]),
            () => this.getTask(projectUrl, jobUrl, taskUrl, params),
            {
                enabled: [projectUrl, jobUrl, taskUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Job
     * @name GetRuns
     * @summary Get runs info
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/task/{taskId}/run
     * @secure
     * @response `200` `IGetRunsData` OK
     */
    getRuns = (projectUrl: string, jobUrl: string, taskId: number, params: RequestParams = {}) =>
        this.http.request<IGetRunsData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task/${taskId}/run`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetRuns = (projectUrl: string, jobUrl: string, taskId: number, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getRuns', projectUrl, jobUrl, taskId, params]),
            () => this.getRuns(projectUrl, jobUrl, taskId, params),
            {
                enabled: [projectUrl, jobUrl, taskId].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Job
     * @name GetJobDag
     * @summary DAG of Job
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/dag
     * @secure
     * @response `200` `IGetJobDagData` OK
     */
    getJobDag = (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
        this.http.request<IGetJobDagData, any>({
            path: `/api/v1/project/${projectUrl}/job/${jobUrl}/dag`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetJobDag = (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getJobDag', projectUrl, jobUrl, params]),
            () => this.getJobDag(projectUrl, jobUrl, params),
            {
                enabled: [projectUrl, jobUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Dataset
     * @name ListDataset
     * @summary Get the list of the datasets
     * @request GET:/api/v1/project/{projectUrl}/dataset
     * @secure
     * @response `200` `IListDatasetData` OK
     */
    listDataset = (
        projectUrl: string,
        query?: {
            versionId?: string
            name?: string
            owner?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListDatasetData, any>({
            path: `/api/v1/project/${projectUrl}/dataset`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListDataset = (
        projectUrl: string,
        query?: {
            versionId?: string
            name?: string
            owner?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listDataset', projectUrl, query, params]),
            () => this.listDataset(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * @description Return the information of the latest version of the current dataset
     *
     * @tags Dataset
     * @name GetDatasetInfo
     * @summary Get the information of a dataset
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}
     * @secure
     * @response `200` `IGetDatasetInfoData` OK
     */
    getDatasetInfo = (
        projectUrl: string,
        datasetUrl: string,
        query?: {
            /** Dataset versionUrl. (Return the current version as default when the versionUrl is not set.) */
            versionUrl?: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IGetDatasetInfoData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useGetDatasetInfo = (
        projectUrl: string,
        datasetUrl: string,
        query?: {
            /** Dataset versionUrl. (Return the current version as default when the versionUrl is not set.) */
            versionUrl?: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['getDatasetInfo', projectUrl, datasetUrl, query, params]),
            () => this.getDatasetInfo(projectUrl, datasetUrl, query, params),
            {
                enabled: [projectUrl, datasetUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Dataset
     * @name DeleteDataset
     * @summary Delete a dataset
     * @request DELETE:/api/v1/project/{projectUrl}/dataset/{datasetUrl}
     * @secure
     * @response `200` `IDeleteDatasetData` OK
     */
    deleteDataset = (projectUrl: string, datasetUrl: string, params: RequestParams = {}) =>
        this.http.request<IDeleteDatasetData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Dataset
     * @name ListDatasetVersion
     * @summary Get the list of the dataset versions
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version
     * @secure
     * @response `200` `IListDatasetVersionData` OK
     */
    listDatasetVersion = (
        projectUrl: string,
        datasetUrl: string,
        query?: {
            name?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListDatasetVersionData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListDatasetVersion = (
        projectUrl: string,
        datasetUrl: string,
        query?: {
            name?: string
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listDatasetVersion', projectUrl, datasetUrl, query, params]),
            () => this.listDatasetVersion(projectUrl, datasetUrl, query, params),
            {
                enabled: [projectUrl, datasetUrl].every(Boolean),
            }
        )
    /**
     * @description Pull Dataset files part by part.
     *
     * @tags Dataset
     * @name PullDs
     * @summary Pull Dataset files
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/file
     * @deprecated
     * @secure
     * @response `200` `IPullDsData` OK
     */
    pullDs = (
        projectUrl: string,
        datasetUrl: string,
        versionUrl: string,
        query?: {
            /** optional, _manifest.yaml is used if not specified */
            partName?: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IPullDsData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/file`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    usePullDs = (
        projectUrl: string,
        datasetUrl: string,
        versionUrl: string,
        query?: {
            /** optional, _manifest.yaml is used if not specified */
            partName?: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['pullDs', projectUrl, datasetUrl, versionUrl, query, params]),
            () => this.pullDs(projectUrl, datasetUrl, versionUrl, query, params),
            {
                enabled: [projectUrl, datasetUrl, versionUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Dataset
     * @name GetDatasetVersionTag
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/tag/{tag}
     * @secure
     * @response `200` `IGetDatasetVersionTagData` OK
     */
    getDatasetVersionTag = (projectUrl: string, datasetUrl: string, tag: string, params: RequestParams = {}) =>
        this.http.request<IGetDatasetVersionTagData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/tag/${tag}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetDatasetVersionTag = (projectUrl: string, datasetUrl: string, tag: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getDatasetVersionTag', projectUrl, datasetUrl, tag, params]),
            () => this.getDatasetVersionTag(projectUrl, datasetUrl, tag, params),
            {
                enabled: [projectUrl, datasetUrl, tag].every(Boolean),
            }
        )
    /**
     * @description List Build Records
     *
     * @tags Dataset
     * @name ListBuildRecords
     * @summary List Build Records
     * @request GET:/api/v1/project/{projectUrl}/dataset/build/list
     * @secure
     * @response `200` `IListBuildRecordsData` OK
     */
    listBuildRecords = (
        projectUrl: string,
        query?: {
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListBuildRecordsData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/build/list`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListBuildRecords = (
        projectUrl: string,
        query?: {
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listBuildRecords', projectUrl, query, params]),
            () => this.listBuildRecords(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Dataset
     * @name ListDatasetTree
     * @summary List dataset tree including global datasets
     * @request GET:/api/v1/project/{projectUrl}/dataset-tree
     * @secure
     * @response `200` `IListDatasetTreeData` OK
     */
    listDatasetTree = (
        projectUrl: string,
        query?: {
            /** @default "all" */
            scope?: 'all' | 'project' | 'shared'
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListDatasetTreeData, any>({
            path: `/api/v1/project/${projectUrl}/dataset-tree`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListDatasetTree = (
        projectUrl: string,
        query?: {
            /** @default "all" */
            scope?: 'all' | 'project' | 'shared'
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listDatasetTree', projectUrl, query, params]),
            () => this.listDatasetTree(projectUrl, query, params),
            {
                enabled: [projectUrl].every(Boolean),
            }
        )
    /**
     * @description Pull Dataset uri file contents
     *
     * @tags Dataset
     * @name PullUriContent
     * @summary Pull Dataset uri file contents
     * @request GET:/api/v1/project/{projectName}/dataset/{datasetName}/uri
     * @secure
     * @response `200` `IPullUriContentData` OK
     */
    pullUriContent = (
        projectName: string,
        datasetName: string,
        query: {
            uri: string
            /** @format int64 */
            offset?: number
            /** @format int64 */
            size?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IPullUriContentData, any>({
            path: `/api/v1/project/${projectName}/dataset/${datasetName}/uri`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    usePullUriContent = (
        projectName: string,
        datasetName: string,
        query: {
            uri: string
            /** @format int64 */
            offset?: number
            /** @format int64 */
            size?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['pullUriContent', projectName, datasetName, query, params]),
            () => this.pullUriContent(projectName, datasetName, query, params),
            {
                enabled: [projectName, datasetName, query].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Job
     * @name GetModelServingStatus
     * @summary Get the events of the model serving job
     * @request GET:/api/v1/project/{projectId}/serving/{servingId}/status
     * @secure
     * @response `200` `IGetModelServingStatusData` OK
     */
    getModelServingStatus = (projectId: number, servingId: number, params: RequestParams = {}) =>
        this.http.request<IGetModelServingStatusData, any>({
            path: `/api/v1/project/${projectId}/serving/${servingId}/status`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useGetModelServingStatus = (projectId: number, servingId: number, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['getModelServingStatus', projectId, servingId, params]),
            () => this.getModelServingStatus(projectId, servingId, params),
            {
                enabled: [projectId, servingId].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags FineTune
     * @name RecentModelTree1
     * @request GET:/api/v1/project/{projectId}/ftspace/{spaceId}/recent-model-tree
     * @secure
     * @response `200` `IRecentModelTree1Data` OK
     */
    recentModelTree1 = (
        projectId: number,
        spaceId: number,
        query?: {
            /**
             * Data limit
             * @format int32
             * @min 1
             * @max 50
             * @default 5
             */
            limit?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IRecentModelTree1Data, any>({
            path: `/api/v1/project/${projectId}/ftspace/${spaceId}/recent-model-tree`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useRecentModelTree1 = (
        projectId: number,
        spaceId: number,
        query?: {
            /**
             * Data limit
             * @format int32
             * @min 1
             * @max 50
             * @default 5
             */
            limit?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['recentModelTree1', projectId, spaceId, query, params]),
            () => this.recentModelTree1(projectId, spaceId, query, params),
            {
                enabled: [projectId, spaceId].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags FineTune
     * @name ListOnlineEval
     * @summary List online eval
     * @request GET:/api/v1/project/{projectId}/ftspace/{spaceId}/online-eval
     * @secure
     * @response `200` `IListOnlineEvalData` OK
     */
    listOnlineEval = (projectId: number, spaceId: number, params: RequestParams = {}) =>
        this.http.request<IListOnlineEvalData, any>({
            path: `/api/v1/project/${projectId}/ftspace/${spaceId}/online-eval`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useListOnlineEval = (projectId: number, spaceId: number, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['listOnlineEval', projectId, spaceId, params]),
            () => this.listOnlineEval(projectId, spaceId, params),
            {
                enabled: [projectId, spaceId].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags FineTune
     * @name ListModelTree1
     * @request GET:/api/v1/project/{projectId}/ftspace/{spaceId}/model-tree
     * @secure
     * @response `200` `IListModelTree1Data` OK
     */
    listModelTree1 = (projectId: number, spaceId: number, params: RequestParams = {}) =>
        this.http.request<IListModelTree1Data, any>({
            path: `/api/v1/project/${projectId}/ftspace/${spaceId}/model-tree`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useListModelTree1 = (projectId: number, spaceId: number, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['listModelTree1', projectId, spaceId, params]),
            () => this.listModelTree1(projectId, spaceId, params),
            {
                enabled: [projectId, spaceId].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags FineTune
     * @name ListFineTune
     * @summary List fine-tune
     * @request GET:/api/v1/project/{projectId}/ftspace/{spaceId}/ft
     * @secure
     * @response `200` `IListFineTuneData` OK
     */
    listFineTune = (
        projectId: number,
        spaceId: number,
        query?: {
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IListFineTuneData, any>({
            path: `/api/v1/project/${projectId}/ftspace/${spaceId}/ft`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useListFineTune = (
        projectId: number,
        spaceId: number,
        query?: {
            /**
             * @format int32
             * @default 1
             */
            pageNum?: number
            /**
             * @format int32
             * @default 10
             */
            pageSize?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['listFineTune', projectId, spaceId, query, params]),
            () => this.listFineTune(projectId, spaceId, query, params),
            {
                enabled: [projectId, spaceId].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags FineTune
     * @name FineTuneInfo
     * @summary Get fine-tune info
     * @request GET:/api/v1/project/{projectId}/ftspace/{spaceId}/ft/{ftId}
     * @secure
     * @response `200` `IFineTuneInfoData` OK
     */
    fineTuneInfo = (projectId: number, spaceId: number, ftId: number, params: RequestParams = {}) =>
        this.http.request<IFineTuneInfoData, any>({
            path: `/api/v1/project/${projectId}/ftspace/${spaceId}/ft/${ftId}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useFineTuneInfo = (projectId: number, spaceId: number, ftId: number, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['fineTuneInfo', projectId, spaceId, ftId, params]),
            () => this.fineTuneInfo(projectId, spaceId, ftId, params),
            {
                enabled: [projectId, spaceId, ftId].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Log
     * @name OfflineLogs
     * @summary list the log files of a task
     * @request GET:/api/v1/log/offline/{taskId}
     * @secure
     * @response `200` `IOfflineLogsData` OK
     */
    offlineLogs = (taskId: number, params: RequestParams = {}) =>
        this.http.request<IOfflineLogsData, any>({
            path: `/api/v1/log/offline/${taskId}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useOfflineLogs = (taskId: number, params: RequestParams = {}) =>
        useQuery(qs.stringify(['offlineLogs', taskId, params]), () => this.offlineLogs(taskId, params), {
            enabled: [taskId].every(Boolean),
        })
    /**
     * No description
     *
     * @tags Log
     * @name LogContent
     * @summary Get the list of device types
     * @request GET:/api/v1/log/offline/{taskId}/{fileName}
     * @secure
     * @response `200` `ILogContentData` OK
     */
    logContent = (taskId: number, fileName: string, params: RequestParams = {}) =>
        this.http.request<ILogContentData, any>({
            path: `/api/v1/log/offline/${taskId}/${fileName}`,
            method: 'GET',
            secure: true,
            ...params,
        })

    useLogContent = (taskId: number, fileName: string, params: RequestParams = {}) =>
        useQuery(
            qs.stringify(['logContent', taskId, fileName, params]),
            () => this.logContent(taskId, fileName, params),
            {
                enabled: [taskId, fileName].every(Boolean),
            }
        )
    /**
     * No description
     *
     * @tags Job
     * @name GetRuntimeSuggestion
     * @summary Get suggest runtime for eval or online eval
     * @request GET:/api/v1/job/suggestion/runtime
     * @secure
     * @response `200` `IGetRuntimeSuggestionData` OK
     */
    getRuntimeSuggestion = (
        query: {
            /** @format int64 */
            projectId: number
            /** @format int64 */
            modelVersionId?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IGetRuntimeSuggestionData, any>({
            path: `/api/v1/job/suggestion/runtime`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useGetRuntimeSuggestion = (
        query: {
            /** @format int64 */
            projectId: number
            /** @format int64 */
            modelVersionId?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(
            qs.stringify(['getRuntimeSuggestion', query, params]),
            () => this.getRuntimeSuggestion(query, params),
            {
                enabled: [query].every(Boolean),
            }
        )
    /**
     * @description Apply pathPrefix
     *
     * @tags File storage
     * @name ApplyPathPrefix
     * @summary Apply pathPrefix
     * @request GET:/api/v1/filestorage/path/apply
     * @secure
     * @response `200` `IApplyPathPrefixData` OK
     */
    applyPathPrefix = (
        query: {
            flag: string
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IApplyPathPrefixData, any>({
            path: `/api/v1/filestorage/path/apply`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    useApplyPathPrefix = (
        query: {
            flag: string
        },
        params: RequestParams = {}
    ) =>
        useQuery(qs.stringify(['applyPathPrefix', query, params]), () => this.applyPathPrefix(query, params), {
            enabled: [query].every(Boolean),
        })
    /**
     * @description Pull file Content
     *
     * @tags File storage
     * @name PullUriContent1
     * @summary Pull file Content
     * @request GET:/api/v1/filestorage/file
     * @secure
     * @response `200` `IPullUriContent1Data` OK
     */
    pullUriContent1 = (
        query: {
            uri: string
            /**
             * offset in the content
             * @format int64
             */
            offset?: number
            /**
             * data size
             * @format int64
             */
            size?: number
        },
        params: RequestParams = {}
    ) =>
        this.http.request<IPullUriContent1Data, any>({
            path: `/api/v1/filestorage/file`,
            method: 'GET',
            query: query,
            secure: true,
            ...params,
        })

    usePullUriContent1 = (
        query: {
            uri: string
            /**
             * offset in the content
             * @format int64
             */
            offset?: number
            /**
             * data size
             * @format int64
             */
            size?: number
        },
        params: RequestParams = {}
    ) =>
        useQuery(qs.stringify(['pullUriContent1', query, params]), () => this.pullUriContent1(query, params), {
            enabled: [query].every(Boolean),
        })
    /**
     * @description Delete path
     *
     * @tags File storage
     * @name DeletePath
     * @summary Delete path
     * @request DELETE:/api/v1/filestorage/file
     * @secure
     * @response `200` `IDeletePathData` OK
     */
    deletePath = (data: IFileDeleteRequest, params: RequestParams = {}) =>
        this.http.request<IDeletePathData, any>({
            path: `/api/v1/filestorage/file`,
            method: 'DELETE',
            body: data,
            secure: true,
            type: ContentType.Json,
            ...params,
        })

    /**
     * No description
     *
     * @tags Runtime
     * @name DeleteRuntimeVersionTag
     * @request DELETE:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag/{tag}
     * @secure
     * @response `200` `IDeleteRuntimeVersionTagData` OK
     */
    deleteRuntimeVersionTag = (
        projectUrl: string,
        runtimeUrl: string,
        versionUrl: string,
        tag: string,
        params: RequestParams = {}
    ) =>
        this.http.request<IDeleteRuntimeVersionTagData, any>({
            path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/tag/${tag}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Model
     * @name DeleteModelVersionTag
     * @request DELETE:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag/{tag}
     * @secure
     * @response `200` `IDeleteModelVersionTagData` OK
     */
    deleteModelVersionTag = (
        projectUrl: string,
        modelUrl: string,
        versionUrl: string,
        tag: string,
        params: RequestParams = {}
    ) =>
        this.http.request<IDeleteModelVersionTagData, any>({
            path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/tag/${tag}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * No description
     *
     * @tags Dataset
     * @name DeleteDatasetVersionTag
     * @request DELETE:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag/{tag}
     * @secure
     * @response `200` `IDeleteDatasetVersionTagData` OK
     */
    deleteDatasetVersionTag = (
        projectUrl: string,
        datasetUrl: string,
        versionUrl: string,
        tag: string,
        params: RequestParams = {}
    ) =>
        this.http.request<IDeleteDatasetVersionTagData, any>({
            path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/tag/${tag}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })

    /**
     * @description Uninstall plugin by id
     *
     * @tags Panel
     * @name UninstallPlugin
     * @summary Uninstall a plugin
     * @request DELETE:/api/v1/panel/plugin/{id}
     * @secure
     * @response `200` `IUninstallPluginData` OK
     */
    uninstallPlugin = (id: string, params: RequestParams = {}) =>
        this.http.request<IUninstallPluginData, any>({
            path: `/api/v1/panel/plugin/${id}`,
            method: 'DELETE',
            secure: true,
            ...params,
        })
}
