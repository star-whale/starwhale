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

import {
  ActionData,
  AddDatasetVersionTagData,
  AddEventData,
  AddModelVersionTagData,
  AddProjectRoleData,
  AddRuntimeVersionTagData,
  AddTemplateData,
  AddUserSystemRoleData,
  ApplyPathPrefixData,
  ApplySignedGetUrlsData,
  ApplySignedPutUrlsData,
  ApplySignedUrlRequest,
  BuildDatasetData,
  BuildRuntimeImageData,
  CheckCurrentUserPasswordData,
  ClientRuntimeRequest,
  CompleteUploadBlobData,
  ConfigRequest,
  ConsumeNextDataData,
  CreateJobData,
  CreateJobTemplateRequest,
  CreateModelServingData,
  CreateModelVersionData,
  CreateModelVersionRequest,
  CreateProjectData,
  CreateProjectRequest,
  CreateReportData,
  CreateReportRequest,
  CreateSftData,
  CreateSftSpaceData,
  CreateUserData,
  CreateViewConfigData,
  DataConsumptionRequest,
  DatasetBuildRequest,
  DatasetTagRequest,
  DatasetUploadRequest,
  DeleteDatasetData,
  DeleteDatasetVersionTagData,
  DeleteModelData,
  DeleteModelVersionTagData,
  DeletePathData,
  DeleteProjectByUrlData,
  DeleteProjectRoleData,
  DeleteReportData,
  DeleteRuntimeData,
  DeleteRuntimeVersionTagData,
  DeleteTemplateData,
  DeleteTrashData,
  DeleteUserSystemRoleData,
  EventRequest,
  ExecData,
  ExecRequest,
  FileDeleteRequest,
  FindJobData,
  FlushData,
  FlushRequest,
  GetCurrentUserData,
  GetCurrentUserRolesData,
  GetCurrentVersionData,
  GetDatasetInfoData,
  GetDatasetVersionTagData,
  GetEventsData,
  GetFileDataData,
  GetHashedBlob1Data,
  GetHashedBlobData,
  GetJobDagData,
  GetModelDiffData,
  GetModelInfoData,
  GetModelMetaBlobData,
  GetModelServingStatusData,
  GetModelVersionTagData,
  GetPanelSettingData,
  GetProjectByUrlData,
  GetProjectReadmeByUrlData,
  GetReportData,
  GetRunsData,
  GetRuntimeInfoData,
  GetRuntimeSuggestionData,
  GetRuntimeVersionTagData,
  GetTaskData,
  GetTemplateData,
  GetUserByIdData,
  GetViewConfigData,
  HeadDatasetData,
  HeadHashedBlob1Data,
  HeadHashedBlobData,
  HeadModelData,
  HeadRuntimeData,
  InitUploadBlobData,
  InitUploadBlobRequest,
  InstallPluginData,
  InstanceStatusData,
  JobModifyPinRequest,
  JobModifyRequest,
  JobRequest,
  ListAttributesData,
  ListBuildRecordsData,
  ListDatasetData,
  ListDatasetTreeData,
  ListDatasetVersionData,
  ListDatasetVersionTagsData,
  ListDeviceData,
  ListEvaluationSummaryData,
  ListFilesData,
  ListJobsData,
  ListModelData,
  ListModelTreeData,
  ListModelVersionData,
  ListModelVersionTagsData,
  ListProjectData,
  ListProjectRoleData,
  ListReportsData,
  ListResourcePoolsData,
  ListRolesData,
  ListRuntimeData,
  ListRuntimeTreeData,
  ListRuntimeVersionData,
  ListRuntimeVersionTagsData,
  ListSftData,
  ListSftSpaceData,
  ListSystemRolesData,
  ListTablesData,
  ListTablesRequest,
  ListTasksData,
  ListTrashData,
  ListUserData,
  LogContentData,
  ModelServingRequest,
  ModelTagRequest,
  ModelUpdateRequest,
  ModifyJobCommentData,
  ModifyJobPinStatusData,
  ModifyModelData,
  ModifyProjectRoleData,
  ModifyReportData,
  ModifyRuntimeData,
  OfflineLogsData,
  PluginListData,
  PreviewData,
  PullData,
  PullDsData,
  PullUriContent1Data,
  PullUriContentData,
  QueryAndExportData,
  QueryFeaturesData,
  QuerySettingData,
  QueryTableData,
  QueryTableRequest,
  RecentDatasetTreeData,
  RecentModelTreeData,
  RecentRuntimeTreeData,
  RecoverDatasetData,
  RecoverJobData,
  RecoverModelData,
  RecoverProjectData,
  RecoverRuntimeData,
  RecoverTrashData,
  RemoveJobData,
  ResourcePool,
  RevertDatasetRequest,
  RevertDatasetVersionData,
  RevertModelVersionData,
  RevertModelVersionRequest,
  RevertRuntimeVersionData,
  RunEnvs,
  RuntimeRevertRequest,
  RuntimeTagRequest,
  ScanAndExportData,
  ScanTableData,
  ScanTableRequest,
  SelectAllInProjectData,
  SelectRecentlyInProjectData,
  SetPanelSettingData,
  SftCreateRequest,
  SftSpaceCreateRequest,
  ShareDatasetVersionData,
  ShareModelVersionData,
  ShareRuntimeVersionData,
  SharedReportData,
  SignLinks1Data,
  SignLinks2Data,
  SignLinksData,
  TransferData,
  TransferReportRequest,
  UninstallPluginData,
  UpdateCurrentUserPasswordData,
  UpdateProjectData,
  UpdateProjectRequest,
  UpdateReportRequest,
  UpdateResourcePoolsData,
  UpdateRuntimeData,
  UpdateSettingData,
  UpdateTableData,
  UpdateTableRequest,
  UpdateUserPwdData,
  UpdateUserStateData,
  UpdateUserSystemRoleData,
  UploadData,
  UploadDsData,
  UploadHashedBlob1Data,
  UploadHashedBlobData,
  UserCheckPasswordRequest,
  UserRequest,
  UserRoleAddRequest,
  UserRoleDeleteRequest,
  UserRoleUpdateRequest,
  UserTokenData,
  UserUpdatePasswordRequest,
  UserUpdateStateRequest,
} from "./data-contracts";
import { ContentType, HttpClient, RequestParams } from "./http-client";

export class Api<SecurityDataType = unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  /**
   * No description
   *
   * @tags User
   * @name UpdateUserState
   * @summary Enable or disable a user
   * @request PUT:/api/v1/user/{userId}/state
   * @secure
   * @response `200` `UpdateUserStateData` OK
   */
  updateUserState = (userId: string, data: UserUpdateStateRequest, params: RequestParams = {}) =>
    this.http.request<UpdateUserStateData, any>({
      path: `/api/v1/user/${userId}/state`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name UpdateUserPwd
   * @summary Change user password
   * @request PUT:/api/v1/user/{userId}/pwd
   * @secure
   * @response `200` `UpdateUserPwdData` OK
   */
  updateUserPwd = (userId: string, data: UserUpdatePasswordRequest, params: RequestParams = {}) =>
    this.http.request<UpdateUserPwdData, any>({
      path: `/api/v1/user/${userId}/pwd`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name UpdateCurrentUserPassword
   * @summary Update Current User password
   * @request PUT:/api/v1/user/current/pwd
   * @secure
   * @response `200` `UpdateCurrentUserPasswordData` OK
   */
  updateCurrentUserPassword = (data: UserUpdatePasswordRequest, params: RequestParams = {}) =>
    this.http.request<UpdateCurrentUserPasswordData, any>({
      path: `/api/v1/user/current/pwd`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name CheckCurrentUserPassword
   * @summary Check Current User password
   * @request POST:/api/v1/user/current/pwd
   * @secure
   * @response `200` `CheckCurrentUserPasswordData` OK
   */
  checkCurrentUserPassword = (data: UserCheckPasswordRequest, params: RequestParams = {}) =>
    this.http.request<CheckCurrentUserPasswordData, any>({
      path: `/api/v1/user/current/pwd`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name UpdateUserSystemRole
   * @summary Update user role of system
   * @request PUT:/api/v1/role/{systemRoleId}
   * @secure
   * @response `200` `UpdateUserSystemRoleData` OK
   */
  updateUserSystemRole = (systemRoleId: string, data: UserRoleUpdateRequest, params: RequestParams = {}) =>
    this.http.request<UpdateUserSystemRoleData, any>({
      path: `/api/v1/role/${systemRoleId}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name DeleteUserSystemRole
   * @summary Delete user role of system
   * @request DELETE:/api/v1/role/{systemRoleId}
   * @secure
   * @response `200` `DeleteUserSystemRoleData` OK
   */
  deleteUserSystemRole = (systemRoleId: string, data: UserRoleDeleteRequest, params: RequestParams = {}) =>
    this.http.request<DeleteUserSystemRoleData, any>({
      path: `/api/v1/role/${systemRoleId}`,
      method: "DELETE",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description Returns a single project object.
   *
   * @tags Project
   * @name GetProjectByUrl
   * @summary Get a project by Url
   * @request GET:/api/v1/project/{projectUrl}
   * @secure
   * @response `200` `GetProjectByUrlData` OK
   */
  getProjectByUrl = (projectUrl: string, params: RequestParams = {}) =>
    this.http.request<GetProjectByUrlData, any>({
      path: `/api/v1/project/${projectUrl}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Project
   * @name UpdateProject
   * @summary Modify project information
   * @request PUT:/api/v1/project/{projectUrl}
   * @secure
   * @response `200` `UpdateProjectData` OK
   */
  updateProject = (projectUrl: string, data: UpdateProjectRequest, params: RequestParams = {}) =>
    this.http.request<UpdateProjectData, any>({
      path: `/api/v1/project/${projectUrl}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Project
   * @name DeleteProjectByUrl
   * @summary Delete a project by Url
   * @request DELETE:/api/v1/project/{projectUrl}
   * @secure
   * @response `200` `DeleteProjectByUrlData` OK
   */
  deleteProjectByUrl = (projectUrl: string, params: RequestParams = {}) =>
    this.http.request<DeleteProjectByUrlData, any>({
      path: `/api/v1/project/${projectUrl}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * @description Restore a trash to its original type and move it out of the recycle bin.
   *
   * @tags Trash
   * @name RecoverTrash
   * @summary Restore trash by id.
   * @request PUT:/api/v1/project/{projectUrl}/trash/{trashId}
   * @secure
   * @response `200` `RecoverTrashData` OK
   */
  recoverTrash = (projectUrl: string, trashId: number, params: RequestParams = {}) =>
    this.http.request<RecoverTrashData, any>({
      path: `/api/v1/project/${projectUrl}/trash/${trashId}`,
      method: "PUT",
      secure: true,
      ...params,
    });
  /**
   * @description Move a trash out of the recycle bin. This operation cannot be resumed.
   *
   * @tags Trash
   * @name DeleteTrash
   * @summary Delete trash by id.
   * @request DELETE:/api/v1/project/{projectUrl}/trash/{trashId}
   * @secure
   * @response `200` `DeleteTrashData` OK
   */
  deleteTrash = (projectUrl: string, trashId: number, params: RequestParams = {}) =>
    this.http.request<DeleteTrashData, any>({
      path: `/api/v1/project/${projectUrl}/trash/${trashId}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * @description update image for runtime
   *
   * @tags Runtime
   * @name UpdateRuntime
   * @summary update image for runtime
   * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/image
   * @secure
   * @response `200` `UpdateRuntimeData` OK
   */
  updateRuntime = (
    projectUrl: string,
    runtimeUrl: string,
    versionUrl: string,
    data: string,
    params: RequestParams = {},
  ) =>
    this.http.request<UpdateRuntimeData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/image`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name ModifyRuntime
   * @summary Set tag of the runtime version
   * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}
   * @secure
   * @response `200` `ModifyRuntimeData` OK
   */
  modifyRuntime = (
    projectUrl: string,
    runtimeUrl: string,
    runtimeVersionUrl: string,
    data: RuntimeTagRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<ModifyRuntimeData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${runtimeVersionUrl}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name ShareRuntimeVersion
   * @summary Share or unshare the runtime version
   * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}/shared
   * @secure
   * @response `200` `ShareRuntimeVersionData` OK
   */
  shareRuntimeVersion = (
    projectUrl: string,
    runtimeUrl: string,
    runtimeVersionUrl: string,
    query: {
      /** 1 or true - shared, 0 or false - unshared */
      shared: boolean;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ShareRuntimeVersionData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${runtimeVersionUrl}/shared`,
      method: "PUT",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name RecoverRuntime
   * @summary Recover a runtime
   * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/recover
   * @secure
   * @response `200` `RecoverRuntimeData` OK
   */
  recoverRuntime = (projectUrl: string, runtimeUrl: string, params: RequestParams = {}) =>
    this.http.request<RecoverRuntimeData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/recover`,
      method: "PUT",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Project
   * @name ModifyProjectRole
   * @summary Modify a project role
   * @request PUT:/api/v1/project/{projectUrl}/role/{projectRoleId}
   * @secure
   * @response `200` `ModifyProjectRoleData` OK
   */
  modifyProjectRole = (
    projectUrl: string,
    projectRoleId: string,
    query: {
      roleId: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ModifyProjectRoleData, any>({
      path: `/api/v1/project/${projectUrl}/role/${projectRoleId}`,
      method: "PUT",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Project
   * @name DeleteProjectRole
   * @summary Delete a project role
   * @request DELETE:/api/v1/project/{projectUrl}/role/{projectRoleId}
   * @secure
   * @response `200` `DeleteProjectRoleData` OK
   */
  deleteProjectRole = (projectUrl: string, projectRoleId: string, params: RequestParams = {}) =>
    this.http.request<DeleteProjectRoleData, any>({
      path: `/api/v1/project/${projectUrl}/role/${projectRoleId}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Report
   * @name GetReport
   * @request GET:/api/v1/project/{projectUrl}/report/{reportId}
   * @secure
   * @response `200` `GetReportData` OK
   */
  getReport = (projectUrl: string, reportId: number, params: RequestParams = {}) =>
    this.http.request<GetReportData, any>({
      path: `/api/v1/project/${projectUrl}/report/${reportId}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Report
   * @name ModifyReport
   * @request PUT:/api/v1/project/{projectUrl}/report/{reportId}
   * @secure
   * @response `200` `ModifyReportData` OK
   */
  modifyReport = (projectUrl: string, reportId: number, data: UpdateReportRequest, params: RequestParams = {}) =>
    this.http.request<ModifyReportData, any>({
      path: `/api/v1/project/${projectUrl}/report/${reportId}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Report
   * @name DeleteReport
   * @request DELETE:/api/v1/project/{projectUrl}/report/{reportId}
   * @secure
   * @response `200` `DeleteReportData` OK
   */
  deleteReport = (projectUrl: string, reportId: number, params: RequestParams = {}) =>
    this.http.request<DeleteReportData, any>({
      path: `/api/v1/project/${projectUrl}/report/${reportId}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Report
   * @name SharedReport
   * @request PUT:/api/v1/project/{projectUrl}/report/{reportId}/shared
   * @secure
   * @response `200` `SharedReportData` OK
   */
  sharedReport = (
    projectUrl: string,
    reportId: number,
    query: {
      shared: boolean;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<SharedReportData, any>({
      path: `/api/v1/project/${projectUrl}/report/${reportId}/shared`,
      method: "PUT",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name ModifyModel
   * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}
   * @secure
   * @response `200` `ModifyModelData` OK
   */
  modifyModel = (
    projectUrl: string,
    modelUrl: string,
    versionUrl: string,
    data: ModelUpdateRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<ModifyModelData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name HeadModel
   * @request HEAD:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}
   * @secure
   * @response `200` `HeadModelData` OK
   */
  headModel = (projectUrl: string, modelUrl: string, versionUrl: string, params: RequestParams = {}) =>
    this.http.request<HeadModelData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}`,
      method: "HEAD",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name ShareModelVersion
   * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/shared
   * @secure
   * @response `200` `ShareModelVersionData` OK
   */
  shareModelVersion = (
    projectUrl: string,
    modelUrl: string,
    versionUrl: string,
    query: {
      shared: boolean;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ShareModelVersionData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/shared`,
      method: "PUT",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name RecoverModel
   * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/recover
   * @secure
   * @response `200` `RecoverModelData` OK
   */
  recoverModel = (projectUrl: string, modelUrl: string, params: RequestParams = {}) =>
    this.http.request<RecoverModelData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/recover`,
      method: "PUT",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name FindJob
   * @summary Job information
   * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}
   * @secure
   * @response `200` `FindJobData` OK
   */
  findJob = (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
    this.http.request<FindJobData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name ModifyJobComment
   * @summary Set Job Comment
   * @request PUT:/api/v1/project/{projectUrl}/job/{jobUrl}
   * @secure
   * @response `200` `ModifyJobCommentData` OK
   */
  modifyJobComment = (projectUrl: string, jobUrl: string, data: JobModifyRequest, params: RequestParams = {}) =>
    this.http.request<ModifyJobCommentData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name RemoveJob
   * @summary Remove job
   * @request DELETE:/api/v1/project/{projectUrl}/job/{jobUrl}
   * @secure
   * @response `200` `RemoveJobData` OK
   */
  removeJob = (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
    this.http.request<RemoveJobData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name ShareDatasetVersion
   * @summary Share or unshare the dataset version
   * @request PUT:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/shared
   * @secure
   * @response `200` `ShareDatasetVersionData` OK
   */
  shareDatasetVersion = (
    projectUrl: string,
    datasetUrl: string,
    versionUrl: string,
    query: {
      /** 1 or true - shared, 0 or false - unshared */
      shared: boolean;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ShareDatasetVersionData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/shared`,
      method: "PUT",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name RecoverDataset
   * @summary Recover a dataset
   * @request PUT:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/recover
   * @secure
   * @response `200` `RecoverDatasetData` OK
   */
  recoverDataset = (projectUrl: string, datasetUrl: string, params: RequestParams = {}) =>
    this.http.request<RecoverDatasetData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/recover`,
      method: "PUT",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Project
   * @name RecoverProject
   * @summary Recover a project
   * @request PUT:/api/v1/project/{projectId}/recover
   * @secure
   * @response `200` `RecoverProjectData` OK
   */
  recoverProject = (projectId: string, params: RequestParams = {}) =>
    this.http.request<RecoverProjectData, any>({
      path: `/api/v1/project/${projectId}/recover`,
      method: "PUT",
      secure: true,
      ...params,
    });
  /**
   * @description Apply signedUrls for get
   *
   * @tags File storage
   * @name ApplySignedGetUrls
   * @summary Apply signedUrls for get
   * @request GET:/api/v1/filestorage/signedurl
   * @secure
   * @response `200` `ApplySignedGetUrlsData` OK
   */
  applySignedGetUrls = (
    query: {
      pathPrefix: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ApplySignedGetUrlsData, any>({
      path: `/api/v1/filestorage/signedurl`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Apply signedUrls for put
   *
   * @tags File storage
   * @name ApplySignedPutUrls
   * @summary Apply signedUrls for put
   * @request PUT:/api/v1/filestorage/signedurl
   * @secure
   * @response `200` `ApplySignedPutUrlsData` OK
   */
  applySignedPutUrls = (data: ApplySignedUrlRequest, params: RequestParams = {}) =>
    this.http.request<ApplySignedPutUrlsData, any>({
      path: `/api/v1/filestorage/signedurl`,
      method: "PUT",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name ListUser
   * @summary Get the list of users
   * @request GET:/api/v1/user
   * @secure
   * @response `200` `ListUserData` OK
   */
  listUser = (
    query?: {
      /** User name prefix to search for */
      userName?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListUserData, any>({
      path: `/api/v1/user`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name CreateUser
   * @summary Create a new user
   * @request POST:/api/v1/user
   * @secure
   * @response `200` `CreateUserData` OK
   */
  createUser = (data: UserRequest, params: RequestParams = {}) =>
    this.http.request<CreateUserData, any>({
      path: `/api/v1/user`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags rolling-update-controller
   * @name InstanceStatus
   * @summary instance status notify
   * @request POST:/api/v1/system/upgrade/instance/status
   * @secure
   * @response `200` `InstanceStatusData` ok
   */
  instanceStatus = (
    query: {
      status: "BORN" | "READY_DOWN" | "READY_UP" | "DOWN";
      instanceType: "NEW" | "OLD";
    },
    params: RequestParams = {},
  ) =>
    this.http.request<InstanceStatusData, any>({
      path: `/api/v1/system/upgrade/instance/status`,
      method: "POST",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Get system settings in yaml string
   *
   * @tags System
   * @name QuerySetting
   * @summary Get system settings
   * @request GET:/api/v1/system/setting
   * @secure
   * @response `200` `QuerySettingData` OK
   */
  querySetting = (params: RequestParams = {}) =>
    this.http.request<QuerySettingData, any>({
      path: `/api/v1/system/setting`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * @description Update system settings
   *
   * @tags System
   * @name UpdateSetting
   * @summary Update system settings
   * @request POST:/api/v1/system/setting
   * @secure
   * @response `200` `UpdateSettingData` OK
   */
  updateSetting = (data: string, params: RequestParams = {}) =>
    this.http.request<UpdateSettingData, any>({
      path: `/api/v1/system/setting`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags System
   * @name ListResourcePools
   * @summary Get the list of resource pool
   * @request GET:/api/v1/system/resourcePool
   * @secure
   * @response `200` `ListResourcePoolsData` OK
   */
  listResourcePools = (params: RequestParams = {}) =>
    this.http.request<ListResourcePoolsData, any>({
      path: `/api/v1/system/resourcePool`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags System
   * @name UpdateResourcePools
   * @summary Update resource pool
   * @request POST:/api/v1/system/resourcePool
   * @secure
   * @response `200` `UpdateResourcePoolsData` OK
   */
  updateResourcePools = (data: ResourcePool[], params: RequestParams = {}) =>
    this.http.request<UpdateResourcePoolsData, any>({
      path: `/api/v1/system/resourcePool`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name ListSystemRoles
   * @summary List system role of users
   * @request GET:/api/v1/role
   * @secure
   * @response `200` `ListSystemRolesData` OK
   */
  listSystemRoles = (params: RequestParams = {}) =>
    this.http.request<ListSystemRolesData, any>({
      path: `/api/v1/role`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name AddUserSystemRole
   * @summary Add user role of system
   * @request POST:/api/v1/role
   * @secure
   * @response `200` `AddUserSystemRoleData` OK
   */
  addUserSystemRole = (data: UserRoleAddRequest, params: RequestParams = {}) =>
    this.http.request<AddUserSystemRoleData, any>({
      path: `/api/v1/role`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Project
   * @name ListProject
   * @summary Get the list of projects
   * @request GET:/api/v1/project
   * @secure
   * @response `200` `ListProjectData` OK
   */
  listProject = (
    sort: "visited" | "latest" | "oldest",
    query?: {
      projectName?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListProjectData, any>({
      path: `/api/v1/project`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Project
   * @name CreateProject
   * @summary Create or Recover a new project
   * @request POST:/api/v1/project
   * @secure
   * @response `200` `CreateProjectData` OK
   */
  createProject = (data: CreateProjectRequest, params: RequestParams = {}) =>
    this.http.request<CreateProjectData, any>({
      path: `/api/v1/project`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name CreateModelVersion
   * @request POST:/api/v1/project/{project}/model/{modelName}/version/{version}/completeUpload
   * @secure
   * @response `200` `CreateModelVersionData` OK
   */
  createModelVersion = (
    project: string,
    modelName: string,
    version: string,
    data: CreateModelVersionRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<CreateModelVersionData, any>({
      path: `/api/v1/project/${project}/model/${modelName}/version/${version}/completeUpload`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Template
   * @name SelectAllInProject
   * @summary Get Templates for project
   * @request GET:/api/v1/project/{projectUrl}/template
   * @secure
   * @response `200` `SelectAllInProjectData` OK
   */
  selectAllInProject = (projectUrl: string, params: RequestParams = {}) =>
    this.http.request<SelectAllInProjectData, any>({
      path: `/api/v1/project/${projectUrl}/template`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Template
   * @name AddTemplate
   * @summary Add Template for job
   * @request POST:/api/v1/project/{projectUrl}/template
   * @secure
   * @response `200` `AddTemplateData` OK
   */
  addTemplate = (projectUrl: string, data: CreateJobTemplateRequest, params: RequestParams = {}) =>
    this.http.request<AddTemplateData, any>({
      path: `/api/v1/project/${projectUrl}/template`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name CreateModelServing
   * @summary Create a new model serving job
   * @request POST:/api/v1/project/{projectUrl}/serving
   * @secure
   * @response `200` `CreateModelServingData` OK
   */
  createModelServing = (projectUrl: string, data: ModelServingRequest, params: RequestParams = {}) =>
    this.http.request<CreateModelServingData, any>({
      path: `/api/v1/project/${projectUrl}/serving`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name ListRuntimeVersionTags
   * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag
   * @secure
   * @response `200` `ListRuntimeVersionTagsData` OK
   */
  listRuntimeVersionTags = (projectUrl: string, runtimeUrl: string, versionUrl: string, params: RequestParams = {}) =>
    this.http.request<ListRuntimeVersionTagsData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/tag`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name AddRuntimeVersionTag
   * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag
   * @secure
   * @response `200` `AddRuntimeVersionTagData` OK
   */
  addRuntimeVersionTag = (
    projectUrl: string,
    runtimeUrl: string,
    versionUrl: string,
    data: RuntimeTagRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<AddRuntimeVersionTagData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/tag`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description build image for runtime
   *
   * @tags Runtime
   * @name BuildRuntimeImage
   * @summary build image for runtime
   * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/image/build
   * @secure
   * @response `200` `BuildRuntimeImageData` OK
   */
  buildRuntimeImage = (
    projectUrl: string,
    runtimeUrl: string,
    versionUrl: string,
    data: RunEnvs,
    params: RequestParams = {},
  ) =>
    this.http.request<BuildRuntimeImageData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/image/build`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description Select a historical version of the runtime and revert the latest version of the current runtime to this version
   *
   * @tags Runtime
   * @name RevertRuntimeVersion
   * @summary Revert Runtime version
   * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/revert
   * @secure
   * @response `200` `RevertRuntimeVersionData` OK
   */
  revertRuntimeVersion = (
    projectUrl: string,
    runtimeUrl: string,
    data: RuntimeRevertRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<RevertRuntimeVersionData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/revert`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description Create a new version of the runtime. The data resources can be selected by uploading the file package or entering the server path.
   *
   * @tags Runtime
   * @name Upload
   * @summary Create a new runtime version
   * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeName}/version/{versionName}/file
   * @secure
   * @response `200` `UploadData` OK
   */
  upload = (
    projectUrl: string,
    runtimeName: string,
    versionName: string,
    query: {
      uploadRequest: ClientRuntimeRequest;
    },
    data: {
      /** @format binary */
      file: File;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<UploadData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeName}/version/${versionName}/file`,
      method: "POST",
      query: query,
      body: data,
      secure: true,
      type: ContentType.FormData,
      ...params,
    });
  /**
   * No description
   *
   * @tags Project
   * @name ListProjectRole
   * @summary List project roles
   * @request GET:/api/v1/project/{projectUrl}/role
   * @secure
   * @response `200` `ListProjectRoleData` OK
   */
  listProjectRole = (projectUrl: string, params: RequestParams = {}) =>
    this.http.request<ListProjectRoleData, any>({
      path: `/api/v1/project/${projectUrl}/role`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Project
   * @name AddProjectRole
   * @summary Grant project role to a user
   * @request POST:/api/v1/project/{projectUrl}/role
   * @secure
   * @response `200` `AddProjectRoleData` OK
   */
  addProjectRole = (
    projectUrl: string,
    query: {
      userId: string;
      roleId: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<AddProjectRoleData, any>({
      path: `/api/v1/project/${projectUrl}/role`,
      method: "POST",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Report
   * @name ListReports
   * @summary Get the list of reports
   * @request GET:/api/v1/project/{projectUrl}/report
   * @secure
   * @response `200` `ListReportsData` OK
   */
  listReports = (
    projectUrl: string,
    query?: {
      title?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListReportsData, any>({
      path: `/api/v1/project/${projectUrl}/report`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Report
   * @name CreateReport
   * @request POST:/api/v1/project/{projectUrl}/report
   * @secure
   * @response `200` `CreateReportData` OK
   */
  createReport = (projectUrl: string, data: CreateReportRequest, params: RequestParams = {}) =>
    this.http.request<CreateReportData, any>({
      path: `/api/v1/project/${projectUrl}/report`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Report
   * @name Transfer
   * @request POST:/api/v1/project/{projectUrl}/report/{reportId}/transfer
   * @secure
   * @response `200` `TransferData` OK
   */
  transfer = (projectUrl: string, reportId: number, data: TransferReportRequest, params: RequestParams = {}) =>
    this.http.request<TransferData, any>({
      path: `/api/v1/project/${projectUrl}/report/${reportId}/transfer`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name ListModelVersionTags
   * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag
   * @secure
   * @response `200` `ListModelVersionTagsData` OK
   */
  listModelVersionTags = (projectUrl: string, modelUrl: string, versionUrl: string, params: RequestParams = {}) =>
    this.http.request<ListModelVersionTagsData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/tag`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name AddModelVersionTag
   * @request POST:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag
   * @secure
   * @response `200` `AddModelVersionTagData` OK
   */
  addModelVersionTag = (
    projectUrl: string,
    modelUrl: string,
    versionUrl: string,
    data: ModelTagRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<AddModelVersionTagData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/tag`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name RevertModelVersion
   * @request POST:/api/v1/project/{projectUrl}/model/{modelUrl}/revert
   * @secure
   * @response `200` `RevertModelVersionData` OK
   */
  revertModelVersion = (
    projectUrl: string,
    modelUrl: string,
    data: RevertModelVersionRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<RevertModelVersionData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/revert`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name ListJobs
   * @summary Get the list of jobs
   * @request GET:/api/v1/project/{projectUrl}/job
   * @secure
   * @response `200` `ListJobsData` OK
   */
  listJobs = (
    projectUrl: string,
    query?: {
      swmpId?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListJobsData, any>({
      path: `/api/v1/project/${projectUrl}/job`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name CreateJob
   * @summary Create a new job
   * @request POST:/api/v1/project/{projectUrl}/job
   * @secure
   * @response `200` `CreateJobData` OK
   */
  createJob = (projectUrl: string, data: JobRequest, params: RequestParams = {}) =>
    this.http.request<CreateJobData, any>({
      path: `/api/v1/project/${projectUrl}/job`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name Action
   * @summary Job Action
   * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/{action}
   * @secure
   * @response `200` `ActionData` OK
   */
  action = (projectUrl: string, jobUrl: string, action: string, params: RequestParams = {}) =>
    this.http.request<ActionData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}/${action}`,
      method: "POST",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name Exec
   * @summary Execute command in running task
   * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/task/{taskId}/exec
   * @secure
   * @response `200` `ExecData` OK
   */
  exec = (projectUrl: string, jobUrl: string, taskId: string, data: ExecRequest, params: RequestParams = {}) =>
    this.http.request<ExecData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task/${taskId}/exec`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name RecoverJob
   * @summary Recover job
   * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/recover
   * @secure
   * @response `200` `RecoverJobData` OK
   */
  recoverJob = (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
    this.http.request<RecoverJobData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}/recover`,
      method: "POST",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name ModifyJobPinStatus
   * @summary Pin Job
   * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/pin
   * @secure
   * @response `200` `ModifyJobPinStatusData` OK
   */
  modifyJobPinStatus = (projectUrl: string, jobUrl: string, data: JobModifyPinRequest, params: RequestParams = {}) =>
    this.http.request<ModifyJobPinStatusData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}/pin`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name GetEvents
   * @summary Get events of job or task
   * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/event
   * @secure
   * @response `200` `GetEventsData` OK
   */
  getEvents = (
    projectUrl: string,
    jobUrl: string,
    query?: {
      /** @format int64 */
      taskId?: number;
      /** @format int64 */
      runId?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<GetEventsData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}/event`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name AddEvent
   * @summary Add event to job or task
   * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/event
   * @secure
   * @response `200` `AddEventData` OK
   */
  addEvent = (projectUrl: string, jobUrl: string, data: EventRequest, params: RequestParams = {}) =>
    this.http.request<AddEventData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}/event`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description Sign uris to get a batch of temporarily accessible links
   *
   * @tags Evaluation
   * @name SignLinks
   * @summary Sign uris to get a batch of temporarily accessible links
   * @request POST:/api/v1/project/{projectUrl}/evaluation/{version}/uri/sign-links
   * @deprecated
   * @secure
   * @response `200` `SignLinksData` ok
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
      expTimeMillis?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<SignLinksData, any>({
      path: `/api/v1/project/${projectUrl}/evaluation/${version}/uri/sign-links`,
      method: "POST",
      query: query,
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description 404 if not exists; 200 if exists
   *
   * @tags Evaluation
   * @name GetHashedBlob
   * @summary Download the hashed blob in this evaluation
   * @request GET:/api/v1/project/{projectUrl}/evaluation/{version}/hashedBlob/{hash}
   * @secure
   * @response `200` `GetHashedBlobData` ok
   */
  getHashedBlob = (projectUrl: string, version: string, hash: string, params: RequestParams = {}) =>
    this.http.request<GetHashedBlobData, any>({
      path: `/api/v1/project/${projectUrl}/evaluation/${version}/hashedBlob/${hash}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * @description Upload a hashed BLOB to evaluation object store, returns a uri of the main storage
   *
   * @tags Evaluation
   * @name UploadHashedBlob
   * @summary Upload a hashed BLOB to evaluation object store
   * @request POST:/api/v1/project/{projectUrl}/evaluation/{version}/hashedBlob/{hash}
   * @secure
   * @response `200` `UploadHashedBlobData` ok
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
      file: File;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<UploadHashedBlobData, any>({
      path: `/api/v1/project/${projectUrl}/evaluation/${version}/hashedBlob/${hash}`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.FormData,
      ...params,
    });
  /**
   * @description 404 if not exists; 200 if exists
   *
   * @tags Evaluation
   * @name HeadHashedBlob
   * @summary Test if a hashed blob exists in this evaluation
   * @request HEAD:/api/v1/project/{projectUrl}/evaluation/{version}/hashedBlob/{hash}
   * @secure
   * @response `200` `HeadHashedBlobData` ok
   */
  headHashedBlob = (projectUrl: string, version: string, hash: string, params: RequestParams = {}) =>
    this.http.request<HeadHashedBlobData, any>({
      path: `/api/v1/project/${projectUrl}/evaluation/${version}/hashedBlob/${hash}`,
      method: "HEAD",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Evaluation
   * @name GetViewConfig
   * @summary Get View Config
   * @request GET:/api/v1/project/{projectUrl}/evaluation/view/config
   * @secure
   * @response `200` `GetViewConfigData` OK
   */
  getViewConfig = (
    projectUrl: string,
    query: {
      name: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<GetViewConfigData, any>({
      path: `/api/v1/project/${projectUrl}/evaluation/view/config`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Evaluation
   * @name CreateViewConfig
   * @summary Create or Update View Config
   * @request POST:/api/v1/project/{projectUrl}/evaluation/view/config
   * @secure
   * @response `200` `CreateViewConfigData` OK
   */
  createViewConfig = (projectUrl: string, data: ConfigRequest, params: RequestParams = {}) =>
    this.http.request<CreateViewConfigData, any>({
      path: `/api/v1/project/${projectUrl}/evaluation/view/config`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name ListDatasetVersionTags
   * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag
   * @secure
   * @response `200` `ListDatasetVersionTagsData` OK
   */
  listDatasetVersionTags = (projectUrl: string, datasetUrl: string, versionUrl: string, params: RequestParams = {}) =>
    this.http.request<ListDatasetVersionTagsData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/tag`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name AddDatasetVersionTag
   * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag
   * @secure
   * @response `200` `AddDatasetVersionTagData` OK
   */
  addDatasetVersionTag = (
    projectUrl: string,
    datasetUrl: string,
    versionUrl: string,
    data: DatasetTagRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<AddDatasetVersionTagData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/tag`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name ConsumeNextData
   * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/consume
   * @secure
   * @response `200` `ConsumeNextDataData` OK
   */
  consumeNextData = (
    projectUrl: string,
    datasetUrl: string,
    versionUrl: string,
    data: DataConsumptionRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<ConsumeNextDataData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/consume`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description Select a historical version of the dataset and revert the latest version of the current dataset to this version
   *
   * @tags Dataset
   * @name RevertDatasetVersion
   * @summary Revert Dataset version
   * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/revert
   * @secure
   * @response `200` `RevertDatasetVersionData` OK
   */
  revertDatasetVersion = (
    projectUrl: string,
    datasetUrl: string,
    data: RevertDatasetRequest,
    params: RequestParams = {},
  ) =>
    this.http.request<RevertDatasetVersionData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/revert`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description Create a new version of the dataset. The data resources can be selected by uploading the file package or entering the server path.
   *
   * @tags Dataset
   * @name UploadDs
   * @summary Create a new dataset version
   * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetName}/version/{versionName}/file
   * @deprecated
   * @secure
   * @response `200` `UploadDsData` OK
   */
  uploadDs = (
    projectUrl: string,
    datasetName: string,
    versionName: string,
    query: {
      uploadRequest: DatasetUploadRequest;
    },
    data: {
      /** @format binary */
      file?: File;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<UploadDsData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetName}/version/${versionName}/file`,
      method: "POST",
      query: query,
      body: data,
      secure: true,
      type: ContentType.FormData,
      ...params,
    });
  /**
   * @description Build Dataset
   *
   * @tags Dataset
   * @name BuildDataset
   * @summary Build Dataset
   * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetName}/build
   * @secure
   * @response `200` `BuildDatasetData` OK
   */
  buildDataset = (projectUrl: string, datasetName: string, data: DatasetBuildRequest, params: RequestParams = {}) =>
    this.http.request<BuildDatasetData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetName}/build`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description Sign SWDS uris to get a batch of temporarily accessible links
   *
   * @tags Dataset
   * @name SignLinks1
   * @summary Sign SWDS uris to get a batch of temporarily accessible links
   * @request POST:/api/v1/project/{projectName}/dataset/{datasetName}/uri/sign-links
   * @deprecated
   * @secure
   * @response `200` `SignLinks1Data` OK
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
      expTimeMillis?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<SignLinks1Data, any>({
      path: `/api/v1/project/${projectName}/dataset/${datasetName}/uri/sign-links`,
      method: "POST",
      query: query,
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description 404 if not exists; 200 if exists
   *
   * @tags Dataset
   * @name GetHashedBlob1
   * @summary Download the hashed blob in this dataset
   * @request GET:/api/v1/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}
   * @secure
   * @response `200` `GetHashedBlob1Data` OK
   */
  getHashedBlob1 = (projectName: string, datasetName: string, hash: string, params: RequestParams = {}) =>
    this.http.request<GetHashedBlob1Data, any>({
      path: `/api/v1/project/${projectName}/dataset/${datasetName}/hashedBlob/${hash}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * @description Upload a hashed BLOB to dataset object store, returns a uri of the main storage
   *
   * @tags Dataset
   * @name UploadHashedBlob1
   * @summary Upload a hashed BLOB to dataset object store
   * @request POST:/api/v1/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}
   * @secure
   * @response `200` `UploadHashedBlob1Data` OK
   */
  uploadHashedBlob1 = (
    projectName: string,
    datasetName: string,
    hash: string,
    data: {
      /** @format binary */
      file: File;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<UploadHashedBlob1Data, any>({
      path: `/api/v1/project/${projectName}/dataset/${datasetName}/hashedBlob/${hash}`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.FormData,
      ...params,
    });
  /**
   * @description 404 if not exists; 200 if exists
   *
   * @tags Dataset
   * @name HeadHashedBlob1
   * @summary Test if a hashed blob exists in this dataset
   * @request HEAD:/api/v1/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}
   * @secure
   * @response `200` `HeadHashedBlob1Data` OK
   */
  headHashedBlob1 = (projectName: string, datasetName: string, hash: string, params: RequestParams = {}) =>
    this.http.request<HeadHashedBlob1Data, any>({
      path: `/api/v1/project/${projectName}/dataset/${datasetName}/hashedBlob/${hash}`,
      method: "HEAD",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Sft
   * @name ListSftSpace
   * @summary Get the list of SFT spaces
   * @request GET:/api/v1/project/{projectId}/sft/space
   * @secure
   * @response `200` `ListSftSpaceData` OK
   */
  listSftSpace = (
    projectId: number,
    query?: {
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListSftSpaceData, any>({
      path: `/api/v1/project/${projectId}/sft/space`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Sft
   * @name CreateSftSpace
   * @summary Create SFT space
   * @request POST:/api/v1/project/{projectId}/sft/space
   * @secure
   * @response `200` `CreateSftSpaceData` OK
   */
  createSftSpace = (projectId: number, data: SftSpaceCreateRequest, params: RequestParams = {}) =>
    this.http.request<CreateSftSpaceData, any>({
      path: `/api/v1/project/${projectId}/sft/space`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Sft
   * @name ListSft
   * @summary List SFT
   * @request POST:/api/v1/project/{projectId}/sft/space/{spaceId}/list
   * @secure
   * @response `200` `ListSftData` OK
   */
  listSft = (
    projectId: number,
    spaceId: number,
    query?: {
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListSftData, any>({
      path: `/api/v1/project/${projectId}/sft/space/${spaceId}/list`,
      method: "POST",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Sft
   * @name CreateSft
   * @summary Create SFT
   * @request POST:/api/v1/project/{projectId}/sft/space/{spaceId}/create
   * @secure
   * @response `200` `CreateSftData` OK
   */
  createSft = (projectId: number, spaceId: number, data: SftCreateRequest, params: RequestParams = {}) =>
    this.http.request<CreateSftData, any>({
      path: `/api/v1/project/${projectId}/sft/space/${spaceId}/create`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description Get panel setting by project and key
   *
   * @tags Panel
   * @name GetPanelSetting
   * @summary Get panel setting
   * @request GET:/api/v1/panel/setting/{projectUrl}/{key}
   * @secure
   * @response `200` `GetPanelSettingData` OK
   */
  getPanelSetting = (projectUrl: string, key: string, params: RequestParams = {}) =>
    this.http.request<GetPanelSettingData, any>({
      path: `/api/v1/panel/setting/${projectUrl}/${key}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * @description Save panel setting by project and key
   *
   * @tags Panel
   * @name SetPanelSetting
   * @summary Save panel setting
   * @request POST:/api/v1/panel/setting/{projectUrl}/{key}
   * @secure
   * @response `200` `SetPanelSettingData` OK
   */
  setPanelSetting = (projectUrl: string, key: string, data: string, params: RequestParams = {}) =>
    this.http.request<SetPanelSettingData, any>({
      path: `/api/v1/panel/setting/${projectUrl}/${key}`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * @description List all plugins
   *
   * @tags Panel
   * @name PluginList
   * @summary List all plugins
   * @request GET:/api/v1/panel/plugin
   * @secure
   * @response `200` `PluginListData` OK
   */
  pluginList = (params: RequestParams = {}) =>
    this.http.request<PluginListData, any>({
      path: `/api/v1/panel/plugin`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * @description Upload a tarball and install as panel plugin
   *
   * @tags Panel
   * @name InstallPlugin
   * @summary Install a plugin
   * @request POST:/api/v1/panel/plugin
   * @secure
   * @response `200` `InstallPluginData` OK
   */
  installPlugin = (
    data: {
      /**
       * file detail
       * @format binary
       */
      file: File;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<InstallPluginData, any>({
      path: `/api/v1/panel/plugin`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.FormData,
      ...params,
    });
  /**
   * @description Sign uris to get a batch of temporarily accessible links
   *
   * @tags File storage
   * @name SignLinks2
   * @summary Sign uris to get a batch of temporarily accessible links
   * @request POST:/api/v1/filestorage/sign-links
   * @secure
   * @response `200` `SignLinks2Data` ok
   */
  signLinks2 = (
    query: {
      /**
       * the link will be expired after expTimeMillis
       * @format int64
       */
      expTimeMillis: number;
    },
    data: string[],
    params: RequestParams = {},
  ) =>
    this.http.request<SignLinks2Data, any>({
      path: `/api/v1/filestorage/sign-links`,
      method: "POST",
      query: query,
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags data-store-controller
   * @name UpdateTable
   * @request POST:/api/v1/datastore/updateTable
   * @secure
   * @response `200` `UpdateTableData` OK
   */
  updateTable = (data: UpdateTableRequest, params: RequestParams = {}) =>
    this.http.request<UpdateTableData, any>({
      path: `/api/v1/datastore/updateTable`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags data-store-controller
   * @name ScanTable
   * @request POST:/api/v1/datastore/scanTable
   * @secure
   * @response `200` `ScanTableData` OK
   */
  scanTable = (data: ScanTableRequest, params: RequestParams = {}) =>
    this.http.request<ScanTableData, any>({
      path: `/api/v1/datastore/scanTable`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags data-store-controller
   * @name ScanAndExport
   * @request POST:/api/v1/datastore/scanTable/export
   * @secure
   * @response `200` `ScanAndExportData` OK
   */
  scanAndExport = (data: ScanTableRequest, params: RequestParams = {}) =>
    this.http.request<ScanAndExportData, any>({
      path: `/api/v1/datastore/scanTable/export`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags data-store-controller
   * @name QueryTable
   * @request POST:/api/v1/datastore/queryTable
   * @secure
   * @response `200` `QueryTableData` OK
   */
  queryTable = (data: QueryTableRequest, params: RequestParams = {}) =>
    this.http.request<QueryTableData, any>({
      path: `/api/v1/datastore/queryTable`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags data-store-controller
   * @name QueryAndExport
   * @request POST:/api/v1/datastore/queryTable/export
   * @secure
   * @response `200` `QueryAndExportData` OK
   */
  queryAndExport = (data: QueryTableRequest, params: RequestParams = {}) =>
    this.http.request<QueryAndExportData, any>({
      path: `/api/v1/datastore/queryTable/export`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags data-store-controller
   * @name ListTables
   * @request POST:/api/v1/datastore/listTables
   * @secure
   * @response `200` `ListTablesData` OK
   */
  listTables = (data: ListTablesRequest, params: RequestParams = {}) =>
    this.http.request<ListTablesData, any>({
      path: `/api/v1/datastore/listTables`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags data-store-controller
   * @name Flush
   * @request POST:/api/v1/datastore/flush
   * @secure
   * @response `200` `FlushData` OK
   */
  flush = (
    query: {
      request: FlushRequest;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<FlushData, any>({
      path: `/api/v1/datastore/flush`,
      method: "POST",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name InitUploadBlob
   * @request POST:/api/v1/blob
   * @secure
   * @response `200` `InitUploadBlobData` OK
   */
  initUploadBlob = (data: InitUploadBlobRequest, params: RequestParams = {}) =>
    this.http.request<InitUploadBlobData, any>({
      path: `/api/v1/blob`,
      method: "POST",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name CompleteUploadBlob
   * @request POST:/api/v1/blob/{blobId}
   * @secure
   * @response `200` `CompleteUploadBlobData` OK
   */
  completeUploadBlob = (blobId: string, params: RequestParams = {}) =>
    this.http.request<CompleteUploadBlobData, any>({
      path: `/api/v1/blob/${blobId}`,
      method: "POST",
      secure: true,
      ...params,
    });
  /**
   * @description head for runtime info
   *
   * @tags Runtime
   * @name HeadRuntime
   * @summary head for runtime info
   * @request HEAD:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}
   * @secure
   * @response `200` `HeadRuntimeData` OK
   */
  headRuntime = (projectUrl: string, runtimeUrl: string, versionUrl: string, params: RequestParams = {}) =>
    this.http.request<HeadRuntimeData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}`,
      method: "HEAD",
      secure: true,
      ...params,
    });
  /**
   * @description head for dataset info
   *
   * @tags Dataset
   * @name HeadDataset
   * @summary head for dataset info
   * @request HEAD:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}
   * @secure
   * @response `200` `HeadDatasetData` OK
   */
  headDataset = (projectUrl: string, datasetUrl: string, versionUrl: string, params: RequestParams = {}) =>
    this.http.request<HeadDatasetData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}`,
      method: "HEAD",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name GetUserById
   * @summary Get a user by user ID
   * @request GET:/api/v1/user/{userId}
   * @secure
   * @response `200` `GetUserByIdData` OK
   */
  getUserById = (userId: string, params: RequestParams = {}) =>
    this.http.request<GetUserByIdData, any>({
      path: `/api/v1/user/${userId}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * @description Get token of any user for third party system integration, only super admin is allowed to do this
   *
   * @tags User
   * @name UserToken
   * @summary Get arbitrary user token
   * @request GET:/api/v1/user/token/{userId}
   * @secure
   * @response `200` `UserTokenData` OK
   */
  userToken = (userId: number, params: RequestParams = {}) =>
    this.http.request<UserTokenData, any>({
      path: `/api/v1/user/token/${userId}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name GetCurrentUser
   * @summary Get the current logged in user.
   * @request GET:/api/v1/user/current
   * @secure
   * @response `200` `GetCurrentUserData` OK
   */
  getCurrentUser = (params: RequestParams = {}) =>
    this.http.request<GetCurrentUserData, any>({
      path: `/api/v1/user/current`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name GetCurrentUserRoles
   * @summary Get the current user roles.
   * @request GET:/api/v1/user/current/role
   * @secure
   * @response `200` `GetCurrentUserRolesData` OK
   */
  getCurrentUserRoles = (
    query: {
      projectUrl: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<GetCurrentUserRolesData, any>({
      path: `/api/v1/user/current/role`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags System
   * @name GetCurrentVersion
   * @summary Get current version of the system
   * @request GET:/api/v1/system/version
   * @secure
   * @response `200` `GetCurrentVersionData` OK
   */
  getCurrentVersion = (params: RequestParams = {}) =>
    this.http.request<GetCurrentVersionData, any>({
      path: `/api/v1/system/version`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * @description Get system features list
   *
   * @tags System
   * @name QueryFeatures
   * @summary Get system features
   * @request GET:/api/v1/system/features
   * @secure
   * @response `200` `QueryFeaturesData` OK
   */
  queryFeatures = (params: RequestParams = {}) =>
    this.http.request<QueryFeaturesData, any>({
      path: `/api/v1/system/features`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags env-controller
   * @name ListDevice
   * @summary Get the list of device types
   * @request GET:/api/v1/runtime/device
   * @secure
   * @response `200` `ListDeviceData` OK
   */
  listDevice = (params: RequestParams = {}) =>
    this.http.request<ListDeviceData, any>({
      path: `/api/v1/runtime/device`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags User
   * @name ListRoles
   * @summary List role enums
   * @request GET:/api/v1/role/enums
   * @secure
   * @response `200` `ListRolesData` OK
   */
  listRoles = (params: RequestParams = {}) =>
    this.http.request<ListRolesData, any>({
      path: `/api/v1/role/enums`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Report
   * @name Preview
   * @request GET:/api/v1/report/{uuid}/preview
   * @secure
   * @response `200` `PreviewData` OK
   */
  preview = (uuid: string, params: RequestParams = {}) =>
    this.http.request<PreviewData, any>({
      path: `/api/v1/report/${uuid}/preview`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name GetModelMetaBlob
   * @request GET:/api/v1/project/{project}/model/{model}/version/{version}/meta
   * @secure
   * @response `200` `GetModelMetaBlobData` OK
   */
  getModelMetaBlob = (
    project: string,
    model: string,
    version: string,
    query?: {
      /** @default "" */
      blobId?: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<GetModelMetaBlobData, any>({
      path: `/api/v1/project/${project}/model/${model}/version/${version}/meta`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name ListFiles
   * @request GET:/api/v1/project/{project}/model/{model}/listFiles
   * @secure
   * @response `200` `ListFilesData` OK
   */
  listFiles = (
    project: string,
    model: string,
    query?: {
      /** @default "latest" */
      version?: string;
      /** @default "" */
      path?: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListFilesData, any>({
      path: `/api/v1/project/${project}/model/${model}/listFiles`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name GetFileData
   * @request GET:/api/v1/project/{project}/model/{model}/getFileData
   * @secure
   * @response `200` `GetFileDataData` OK
   */
  getFileData = (
    project: string,
    model: string,
    query: {
      /** @default "latest" */
      version?: string;
      path: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<GetFileDataData, any>({
      path: `/api/v1/project/${project}/model/${model}/getFileData`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description List all types of trashes, such as models datasets runtimes and evaluations
   *
   * @tags Trash
   * @name ListTrash
   * @summary Get the list of trash
   * @request GET:/api/v1/project/{projectUrl}/trash
   * @secure
   * @response `200` `ListTrashData` OK
   */
  listTrash = (
    projectUrl: string,
    query?: {
      name?: string;
      operator?: string;
      type?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListTrashData, any>({
      path: `/api/v1/project/${projectUrl}/trash`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Template
   * @name GetTemplate
   * @summary Get Template
   * @request GET:/api/v1/project/{projectUrl}/template/{id}
   * @secure
   * @response `200` `GetTemplateData` OK
   */
  getTemplate = (projectUrl: string, id: number, params: RequestParams = {}) =>
    this.http.request<GetTemplateData, any>({
      path: `/api/v1/project/${projectUrl}/template/${id}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Template
   * @name DeleteTemplate
   * @summary Delete Template
   * @request DELETE:/api/v1/project/{projectUrl}/template/{id}
   * @secure
   * @response `200` `DeleteTemplateData` OK
   */
  deleteTemplate = (projectUrl: string, id: number, params: RequestParams = {}) =>
    this.http.request<DeleteTemplateData, any>({
      path: `/api/v1/project/${projectUrl}/template/${id}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name ListRuntime
   * @summary Get the list of runtimes
   * @request GET:/api/v1/project/{projectUrl}/runtime
   * @secure
   * @response `200` `ListRuntimeData` OK
   */
  listRuntime = (
    projectUrl: string,
    query?: {
      /** Runtime name prefix to search for */
      name?: string;
      owner?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListRuntimeData, any>({
      path: `/api/v1/project/${projectUrl}/runtime`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Return the information of the latest version of the current runtime
   *
   * @tags Runtime
   * @name GetRuntimeInfo
   * @summary Get the information of a runtime
   * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}
   * @secure
   * @response `200` `GetRuntimeInfoData` OK
   */
  getRuntimeInfo = (
    projectUrl: string,
    runtimeUrl: string,
    query?: {
      versionUrl?: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<GetRuntimeInfoData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name DeleteRuntime
   * @summary Delete a runtime
   * @request DELETE:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}
   * @secure
   * @response `200` `DeleteRuntimeData` OK
   */
  deleteRuntime = (projectUrl: string, runtimeUrl: string, params: RequestParams = {}) =>
    this.http.request<DeleteRuntimeData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name ListRuntimeVersion
   * @summary Get the list of the runtime versions
   * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version
   * @secure
   * @response `200` `ListRuntimeVersionData` OK
   */
  listRuntimeVersion = (
    projectUrl: string,
    runtimeUrl: string,
    query?: {
      /** Runtime version name prefix */
      name?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListRuntimeVersionData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Pull file of a runtime version.
   *
   * @tags Runtime
   * @name Pull
   * @summary Pull file of a runtime version
   * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/file
   * @secure
   * @response `200` `PullData` OK
   */
  pull = (projectUrl: string, runtimeUrl: string, versionUrl: string, params: RequestParams = {}) =>
    this.http.request<PullData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/file`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name GetRuntimeVersionTag
   * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/tag/{tag}
   * @secure
   * @response `200` `GetRuntimeVersionTagData` OK
   */
  getRuntimeVersionTag = (projectUrl: string, runtimeUrl: string, tag: string, params: RequestParams = {}) =>
    this.http.request<GetRuntimeVersionTagData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/tag/${tag}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name ListRuntimeTree
   * @summary List runtime tree including global runtimes
   * @request GET:/api/v1/project/{projectUrl}/runtime-tree
   * @secure
   * @response `200` `ListRuntimeTreeData` OK
   */
  listRuntimeTree = (
    projectUrl: string,
    query?: {
      /**
       * Data range
       * @default "all"
       */
      scope?: "all" | "project" | "shared";
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListRuntimeTreeData, any>({
      path: `/api/v1/project/${projectUrl}/runtime-tree`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Template
   * @name SelectRecentlyInProject
   * @summary Get Recently Templates for project
   * @request GET:/api/v1/project/{projectUrl}/recent-template
   * @secure
   * @response `200` `SelectRecentlyInProjectData` OK
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
      limit?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<SelectRecentlyInProjectData, any>({
      path: `/api/v1/project/${projectUrl}/recent-template`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name RecentRuntimeTree
   * @request GET:/api/v1/project/{projectUrl}/recent-runtime-tree
   * @secure
   * @response `200` `RecentRuntimeTreeData` OK
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
      limit?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<RecentRuntimeTreeData, any>({
      path: `/api/v1/project/${projectUrl}/recent-runtime-tree`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name RecentModelTree
   * @request GET:/api/v1/project/{projectUrl}/recent-model-tree
   * @secure
   * @response `200` `RecentModelTreeData` OK
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
      limit?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<RecentModelTreeData, any>({
      path: `/api/v1/project/${projectUrl}/recent-model-tree`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name RecentDatasetTree
   * @request GET:/api/v1/project/{projectUrl}/recent-dataset-tree
   * @secure
   * @response `200` `RecentDatasetTreeData` OK
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
      limit?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<RecentDatasetTreeData, any>({
      path: `/api/v1/project/${projectUrl}/recent-dataset-tree`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Returns readme content.
   *
   * @tags Project
   * @name GetProjectReadmeByUrl
   * @summary Get a project readme by Url
   * @request GET:/api/v1/project/{projectUrl}/readme
   * @secure
   * @response `200` `GetProjectReadmeByUrlData` OK
   */
  getProjectReadmeByUrl = (projectUrl: string, params: RequestParams = {}) =>
    this.http.request<GetProjectReadmeByUrlData, any>({
      path: `/api/v1/project/${projectUrl}/readme`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name ListModel
   * @request GET:/api/v1/project/{projectUrl}/model
   * @secure
   * @response `200` `ListModelData` OK
   */
  listModel = (
    projectUrl: string,
    query?: {
      versionId?: string;
      name?: string;
      owner?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListModelData, any>({
      path: `/api/v1/project/${projectUrl}/model`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name GetModelInfo
   * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}
   * @secure
   * @response `200` `GetModelInfoData` OK
   */
  getModelInfo = (
    projectUrl: string,
    modelUrl: string,
    query?: {
      versionUrl?: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<GetModelInfoData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name DeleteModel
   * @request DELETE:/api/v1/project/{projectUrl}/model/{modelUrl}
   * @secure
   * @response `200` `DeleteModelData` OK
   */
  deleteModel = (projectUrl: string, modelUrl: string, params: RequestParams = {}) =>
    this.http.request<DeleteModelData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name ListModelVersion
   * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/version
   * @secure
   * @response `200` `ListModelVersionData` OK
   */
  listModelVersion = (
    projectUrl: string,
    modelUrl: string,
    query?: {
      name?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListModelVersionData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name GetModelVersionTag
   * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/tag/{tag}
   * @secure
   * @response `200` `GetModelVersionTagData` OK
   */
  getModelVersionTag = (projectUrl: string, modelUrl: string, tag: string, params: RequestParams = {}) =>
    this.http.request<GetModelVersionTagData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/tag/${tag}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name GetModelDiff
   * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/diff
   * @secure
   * @response `200` `GetModelDiffData` OK
   */
  getModelDiff = (
    projectUrl: string,
    modelUrl: string,
    query: {
      baseVersion: string;
      compareVersion: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<GetModelDiffData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/diff`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name ListModelTree
   * @request GET:/api/v1/project/{projectUrl}/model-tree
   * @secure
   * @response `200` `ListModelTreeData` OK
   */
  listModelTree = (
    projectUrl: string,
    query?: {
      /**
       * Data range
       * @default "all"
       */
      scope?: "all" | "project" | "shared";
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListModelTreeData, any>({
      path: `/api/v1/project/${projectUrl}/model-tree`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name ListTasks
   * @summary Get the list of tasks
   * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/task
   * @secure
   * @response `200` `ListTasksData` OK
   */
  listTasks = (
    projectUrl: string,
    jobUrl: string,
    query?: {
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListTasksData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name GetTask
   * @summary Get task info
   * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/task/{taskUrl}
   * @secure
   * @response `200` `GetTaskData` OK
   */
  getTask = (projectUrl: string, jobUrl: string, taskUrl: string, params: RequestParams = {}) =>
    this.http.request<GetTaskData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task/${taskUrl}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name GetRuns
   * @summary Get runs info
   * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/task/{taskId}/run
   * @secure
   * @response `200` `GetRunsData` OK
   */
  getRuns = (projectUrl: string, jobUrl: string, taskId: number, params: RequestParams = {}) =>
    this.http.request<GetRunsData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task/${taskId}/run`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name GetJobDag
   * @summary DAG of Job
   * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/dag
   * @secure
   * @response `200` `GetJobDagData` OK
   */
  getJobDag = (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
    this.http.request<GetJobDagData, any>({
      path: `/api/v1/project/${projectUrl}/job/${jobUrl}/dag`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Evaluation
   * @name ListEvaluationSummary
   * @summary List Evaluation Summary
   * @request GET:/api/v1/project/{projectUrl}/evaluation
   * @secure
   * @response `200` `ListEvaluationSummaryData` OK
   */
  listEvaluationSummary = (
    projectUrl: string,
    query: {
      filter: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListEvaluationSummaryData, any>({
      path: `/api/v1/project/${projectUrl}/evaluation`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Evaluation
   * @name ListAttributes
   * @summary List Evaluation Summary Attributes
   * @request GET:/api/v1/project/{projectUrl}/evaluation/view/attribute
   * @secure
   * @response `200` `ListAttributesData` OK
   */
  listAttributes = (projectUrl: string, params: RequestParams = {}) =>
    this.http.request<ListAttributesData, any>({
      path: `/api/v1/project/${projectUrl}/evaluation/view/attribute`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name ListDataset
   * @summary Get the list of the datasets
   * @request GET:/api/v1/project/{projectUrl}/dataset
   * @secure
   * @response `200` `ListDatasetData` OK
   */
  listDataset = (
    projectUrl: string,
    query?: {
      versionId?: string;
      name?: string;
      owner?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListDatasetData, any>({
      path: `/api/v1/project/${projectUrl}/dataset`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Return the information of the latest version of the current dataset
   *
   * @tags Dataset
   * @name GetDatasetInfo
   * @summary Get the information of a dataset
   * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}
   * @secure
   * @response `200` `GetDatasetInfoData` OK
   */
  getDatasetInfo = (
    projectUrl: string,
    datasetUrl: string,
    query?: {
      /** Dataset versionUrl. (Return the current version as default when the versionUrl is not set.) */
      versionUrl?: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<GetDatasetInfoData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name DeleteDataset
   * @summary Delete a dataset
   * @request DELETE:/api/v1/project/{projectUrl}/dataset/{datasetUrl}
   * @secure
   * @response `200` `DeleteDatasetData` OK
   */
  deleteDataset = (projectUrl: string, datasetUrl: string, params: RequestParams = {}) =>
    this.http.request<DeleteDatasetData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name ListDatasetVersion
   * @summary Get the list of the dataset versions
   * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version
   * @secure
   * @response `200` `ListDatasetVersionData` OK
   */
  listDatasetVersion = (
    projectUrl: string,
    datasetUrl: string,
    query?: {
      name?: string;
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListDatasetVersionData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Pull Dataset files part by part.
   *
   * @tags Dataset
   * @name PullDs
   * @summary Pull Dataset files
   * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/file
   * @deprecated
   * @secure
   * @response `200` `PullDsData` OK
   */
  pullDs = (
    projectUrl: string,
    datasetUrl: string,
    versionUrl: string,
    query?: {
      /** optional, _manifest.yaml is used if not specified */
      partName?: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<PullDsData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/file`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name GetDatasetVersionTag
   * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/tag/{tag}
   * @secure
   * @response `200` `GetDatasetVersionTagData` OK
   */
  getDatasetVersionTag = (projectUrl: string, datasetUrl: string, tag: string, params: RequestParams = {}) =>
    this.http.request<GetDatasetVersionTagData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/tag/${tag}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * @description List Build Records
   *
   * @tags Dataset
   * @name ListBuildRecords
   * @summary List Build Records
   * @request GET:/api/v1/project/{projectUrl}/dataset/build/list
   * @secure
   * @response `200` `ListBuildRecordsData` OK
   */
  listBuildRecords = (
    projectUrl: string,
    query?: {
      /**
       * @format int32
       * @default 1
       */
      pageNum?: number;
      /**
       * @format int32
       * @default 10
       */
      pageSize?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListBuildRecordsData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/build/list`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name ListDatasetTree
   * @summary List dataset tree including global datasets
   * @request GET:/api/v1/project/{projectUrl}/dataset-tree
   * @secure
   * @response `200` `ListDatasetTreeData` OK
   */
  listDatasetTree = (
    projectUrl: string,
    query?: {
      /** @default "all" */
      scope?: "all" | "project" | "shared";
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ListDatasetTreeData, any>({
      path: `/api/v1/project/${projectUrl}/dataset-tree`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Pull Dataset uri file contents
   *
   * @tags Dataset
   * @name PullUriContent
   * @summary Pull Dataset uri file contents
   * @request GET:/api/v1/project/{projectName}/dataset/{datasetName}/uri
   * @secure
   * @response `200` `PullUriContentData` OK
   */
  pullUriContent = (
    projectName: string,
    datasetName: string,
    query: {
      uri: string;
      /** @format int64 */
      offset?: number;
      /** @format int64 */
      size?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<PullUriContentData, any>({
      path: `/api/v1/project/${projectName}/dataset/${datasetName}/uri`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name GetModelServingStatus
   * @summary Get the events of the model serving job
   * @request GET:/api/v1/project/{projectId}/serving/{servingId}/status
   * @secure
   * @response `200` `GetModelServingStatusData` OK
   */
  getModelServingStatus = (projectId: number, servingId: number, params: RequestParams = {}) =>
    this.http.request<GetModelServingStatusData, any>({
      path: `/api/v1/project/${projectId}/serving/${servingId}/status`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Log
   * @name OfflineLogs
   * @summary list the log files of a task
   * @request GET:/api/v1/log/offline/{taskId}
   * @secure
   * @response `200` `OfflineLogsData` OK
   */
  offlineLogs = (taskId: number, params: RequestParams = {}) =>
    this.http.request<OfflineLogsData, any>({
      path: `/api/v1/log/offline/${taskId}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Log
   * @name LogContent
   * @summary Get the list of device types
   * @request GET:/api/v1/log/offline/{taskId}/{fileName}
   * @secure
   * @response `200` `LogContentData` OK
   */
  logContent = (taskId: number, fileName: string, params: RequestParams = {}) =>
    this.http.request<LogContentData, any>({
      path: `/api/v1/log/offline/${taskId}/${fileName}`,
      method: "GET",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Job
   * @name GetRuntimeSuggestion
   * @summary Get suggest runtime for eval or online eval
   * @request GET:/api/v1/job/suggestion/runtime
   * @secure
   * @response `200` `GetRuntimeSuggestionData` OK
   */
  getRuntimeSuggestion = (
    query: {
      /** @format int64 */
      projectId: number;
      /** @format int64 */
      modelVersionId?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<GetRuntimeSuggestionData, any>({
      path: `/api/v1/job/suggestion/runtime`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Apply pathPrefix
   *
   * @tags File storage
   * @name ApplyPathPrefix
   * @summary Apply pathPrefix
   * @request GET:/api/v1/filestorage/path/apply
   * @secure
   * @response `200` `ApplyPathPrefixData` OK
   */
  applyPathPrefix = (
    query: {
      flag: string;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<ApplyPathPrefixData, any>({
      path: `/api/v1/filestorage/path/apply`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Pull file Content
   *
   * @tags File storage
   * @name PullUriContent1
   * @summary Pull file Content
   * @request GET:/api/v1/filestorage/file
   * @secure
   * @response `200` `PullUriContent1Data` OK
   */
  pullUriContent1 = (
    query: {
      uri: string;
      /**
       * offset in the content
       * @format int64
       */
      offset?: number;
      /**
       * data size
       * @format int64
       */
      size?: number;
    },
    params: RequestParams = {},
  ) =>
    this.http.request<PullUriContent1Data, any>({
      path: `/api/v1/filestorage/file`,
      method: "GET",
      query: query,
      secure: true,
      ...params,
    });
  /**
   * @description Delete path
   *
   * @tags File storage
   * @name DeletePath
   * @summary Delete path
   * @request DELETE:/api/v1/filestorage/file
   * @secure
   * @response `200` `DeletePathData` OK
   */
  deletePath = (data: FileDeleteRequest, params: RequestParams = {}) =>
    this.http.request<DeletePathData, any>({
      path: `/api/v1/filestorage/file`,
      method: "DELETE",
      body: data,
      secure: true,
      type: ContentType.Json,
      ...params,
    });
  /**
   * No description
   *
   * @tags Runtime
   * @name DeleteRuntimeVersionTag
   * @request DELETE:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag/{tag}
   * @secure
   * @response `200` `DeleteRuntimeVersionTagData` OK
   */
  deleteRuntimeVersionTag = (
    projectUrl: string,
    runtimeUrl: string,
    versionUrl: string,
    tag: string,
    params: RequestParams = {},
  ) =>
    this.http.request<DeleteRuntimeVersionTagData, any>({
      path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/tag/${tag}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Model
   * @name DeleteModelVersionTag
   * @request DELETE:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag/{tag}
   * @secure
   * @response `200` `DeleteModelVersionTagData` OK
   */
  deleteModelVersionTag = (
    projectUrl: string,
    modelUrl: string,
    versionUrl: string,
    tag: string,
    params: RequestParams = {},
  ) =>
    this.http.request<DeleteModelVersionTagData, any>({
      path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/tag/${tag}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * No description
   *
   * @tags Dataset
   * @name DeleteDatasetVersionTag
   * @request DELETE:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag/{tag}
   * @secure
   * @response `200` `DeleteDatasetVersionTagData` OK
   */
  deleteDatasetVersionTag = (
    projectUrl: string,
    datasetUrl: string,
    versionUrl: string,
    tag: string,
    params: RequestParams = {},
  ) =>
    this.http.request<DeleteDatasetVersionTagData, any>({
      path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/tag/${tag}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
  /**
   * @description Uninstall plugin by id
   *
   * @tags Panel
   * @name UninstallPlugin
   * @summary Uninstall a plugin
   * @request DELETE:/api/v1/panel/plugin/{id}
   * @secure
   * @response `200` `UninstallPluginData` OK
   */
  uninstallPlugin = (id: string, params: RequestParams = {}) =>
    this.http.request<UninstallPluginData, any>({
      path: `/api/v1/panel/plugin/${id}`,
      method: "DELETE",
      secure: true,
      ...params,
    });
}
