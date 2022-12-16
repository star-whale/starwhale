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

export interface UserUpdateStateRequest {
  isEnabled: boolean;
}

export interface ResponseMessageString {
  code?: string;
  message?: string;
  data?: string;
}

export interface UserUpdatePasswordRequest {
  currentUserPwd: string;
  newPwd: string;
}

export interface UserRoleUpdateRequest {
  currentUserPwd: string;
  roleId: string;
}

export interface UpdateProjectRequest {
  /** @pattern ^[a-zA-Z][a-zA-Z\d_-]{2,80}$ */
  projectName?: string;
  ownerId?: string;
  privacy?: string;
  description?: string;
}

export interface RuntimeTagRequest {
  tag: string;
  action?: string;
}

export interface ModelTagRequest {
  tag: string;
  action?: string;
}

export interface JobModifyRequest {
  comment: string;
}

export interface DatasetTagRequest {
  tag: string;
  action?: string;
}

export interface UserRequest {
  /** @pattern ^[a-zA-Z][a-zA-Z\d_-]{3,32}$ */
  userName: string;
  userPwd: string;
  salt?: string;
}

export interface UserCheckPasswordRequest {
  currentUserPwd: string;
}

export interface UserRoleAddRequest {
  currentUserPwd: string;
  userId: string;
  roleId: string;
}

export interface CreateProjectRequest {
  /** @pattern ^[a-zA-Z][a-zA-Z\d_-]{2,80}$ */
  projectName: string;
  ownerId: string;
  privacy: string;
  description: string;
}

export interface RuntimeRevertRequest {
  versionUrl: string;
}

export interface ClientRuntimeRequest {
  runtime?: string;
  project?: string;
  force?: string;
  manifest?: string;
}

export interface RevertModelVersionRequest {
  versionUrl: string;
}

export interface ModelUploadRequest {
  phase: "MANIFEST" | "BLOB" | "END" | "CANCEL";
  force?: string;
  project?: string;
  swmp?: string;
}

export interface ResponseMessageObject {
  code?: string;
  message?: string;
  data?: object;
}

export interface JobRequest {
  modelVersionUrl: string;
  datasetVersionUrls: string;
  runtimeVersionUrl: string;
  comment?: string;
  resourcePool?: string;
  stepSpecOverWrites?: string;
}

export interface ConfigRequest {
  name: string;
  content: string;
}

export interface DataConsumptionRequest {
  sessionId?: string;
  consumerId?: string;
  /** @format int32 */
  batchSize?: number;
  start?: string;
  startInclusive?: boolean;
  end?: string;
  endInclusive?: boolean;
  processedData?: DataIndexDesc[];
  serial?: boolean;
}

export interface DataIndexDesc {
  start?: string;
  end?: string;
}

export interface ResponseMessageDataIndexDesc {
  code?: string;
  message?: string;
  data?: DataIndexDesc;
}

export interface RevertDatasetRequest {
  versionUrl: string;
}

export interface DatasetUploadRequest {
  phase: "MANIFEST" | "BLOB" | "END" | "CANCEL";
  force?: string;
  project?: string;
  swds?: string;
}

export interface ResponseMessageUploadResult {
  code?: string;
  message?: string;
  data?: UploadResult;
}

export interface UploadResult {
  upload_id?: string;
}

export interface ResponseMessageMapObjectObject {
  code?: string;
  message?: string;
  data?: Record<string, object>;
}

export interface ColumnSchemaDesc {
  name?: string;
  type?: string;
  pythonType?: string;
  elementType?: ColumnSchemaDesc;
  keyType?: ColumnSchemaDesc;
  valueType?: ColumnSchemaDesc;
}

export interface RecordDesc {
  values: RecordValueDesc[];
}

export interface RecordValueDesc {
  key: string;
  value?: object;
}

export interface TableSchemaDesc {
  keyColumn?: string;
  columnSchemaList?: ColumnSchemaDesc[];
}

export interface UpdateTableRequest {
  tableName?: string;
  tableSchemaDesc?: TableSchemaDesc;
  records?: RecordDesc[];
}

export interface ColumnDesc {
  columnName?: string;
  alias?: string;
}

export interface ScanTableRequest {
  tables?: TableDesc[];
  start?: string;
  startInclusive?: boolean;
  end?: string;
  endInclusive?: boolean;
  /** @format int32 */
  limit?: number;
  keepNone?: boolean;
  rawResult?: boolean;
  ignoreNonExistingTable?: boolean;
}

export interface TableDesc {
  tableName?: string;
  columnPrefix?: string;
  columns?: ColumnDesc[];
  keepNone?: boolean;
}

export interface RecordListVo {
  columnTypes?: ColumnSchemaDesc[];
  records?: Record<string, object>[];
  lastKey?: string;
}

export interface ResponseMessageRecordListVo {
  code?: string;
  message?: string;
  data?: RecordListVo;
}

export interface OrderByDesc {
  columnName?: string;
  descending?: boolean;
}

export interface QueryTableRequest {
  tableName?: string;
  columns?: ColumnDesc[];
  orderBy?: OrderByDesc[];
  descending?: boolean;
  filter?: TableQueryFilterDesc;
  /** @format int32 */
  start?: number;
  /** @format int32 */
  limit?: number;
  keepNone?: boolean;
  rawResult?: boolean;
  ignoreNonExistingTable?: boolean;
}

export interface TableQueryFilterDesc {
  operator: string;
  operands?: TableQueryOperandDesc[];
}

export interface TableQueryOperandDesc {
  filter?: TableQueryFilterDesc;
  columnName?: string;
  boolValue?: boolean;
  /** @format int64 */
  intValue?: number;
  /** @format double */
  floatValue?: number;
  stringValue?: string;
  bytesValue?: string;
}

export interface ListTablesRequest {
  prefix?: string;
}

export interface ResponseMessageTableNameListVo {
  code?: string;
  message?: string;
  data?: TableNameListVo;
}

export interface TableNameListVo {
  tables?: string[];
}

export type FlushRequest = object;

export interface PageInfo {
  /** @format int64 */
  total?: number;
  list?: object[];
  /** @format int32 */
  pageNum?: number;
  /** @format int32 */
  pageSize?: number;
  /** @format int32 */
  size?: number;
  /** @format int64 */
  startRow?: number;
  /** @format int64 */
  endRow?: number;
  /** @format int32 */
  pages?: number;
  /** @format int32 */
  prePage?: number;
  /** @format int32 */
  nextPage?: number;
  isFirstPage?: boolean;
  isLastPage?: boolean;
  hasPreviousPage?: boolean;
  hasNextPage?: boolean;
  /** @format int32 */
  navigatePages?: number;
  navigatepageNums?: number[];
  /** @format int32 */
  navigateFirstPage?: number;
  /** @format int32 */
  navigateLastPage?: number;
}

/**
 * User
 * User object
 */
export interface UserVo {
  id?: string;
  name?: string;
  /** @format int64 */
  createdTime?: number;
  isEnabled?: boolean;
  systemRole?: string;
  projectRoles?: Record<string, string>;
}

/**
 * Role
 * Project Role object
 */
export interface ProjectRoleVo {
  id?: string;
  /** User object */
  user?: UserVo;
  /** Project object */
  project?: ProjectVo;
  /** User object */
  role?: RoleVo;
}

/**
 * Project
 * Project object
 */
export interface ProjectVo {
  id?: string;
  name?: string;
  description?: string;
  privacy?: string;
  /** @format int64 */
  createdTime?: number;
  /** User object */
  owner?: UserVo;
  statistics?: StatisticsVo;
}

export interface ResponseMessageListProjectRoleVo {
  code?: string;
  message?: string;
  data?: ProjectRoleVo[];
}

/**
 * User
 * User object
 */
export interface RoleVo {
  id?: string;
  name?: string;
  code?: string;
  description?: string;
}

export interface StatisticsVo {
  /** @format int32 */
  modelCounts?: number;
  /** @format int32 */
  datasetCounts?: number;
  /** @format int32 */
  runtimeCounts?: number;
  /** @format int32 */
  memberCounts?: number;
  /** @format int32 */
  evaluationCounts?: number;
}

/**
 * Version
 * System verion
 */
export interface SystemVersionVo {
  id?: string;
  version?: string;
}

/**
 * Progress
 * Upgrade progress object
 */
export interface UpgradeProgressVo {
  phase?: "downloading" | "upgrading";
  /** @format int32 */
  progress?: number;
}

export interface ResponseMessageListSystemRoleVo {
  code?: string;
  message?: string;
  data?: SystemRoleVo[];
}

/**
 * Role
 * System Role object
 */
export interface SystemRoleVo {
  id?: string;
  /** User object */
  user?: UserVo;
  /** User object */
  role?: RoleVo;
}

export interface ResponseMessageListRoleVo {
  code?: string;
  message?: string;
  data?: RoleVo[];
}

/**
 * StorageFile
 * Storage file object
 */
export interface FlattenFileVo {
  name?: string;
  size?: string;
}

export interface ResponseMessageRuntimeInfoVo {
  code?: string;
  message?: string;
  /** Runtime information object */
  data?: RuntimeInfoVo;
}

/**
 * RuntimeInfo
 * Runtime information object
 */
export interface RuntimeInfoVo {
  id?: string;
  name?: string;
  versionName?: string;
  versionAlias?: string;
  versionTag?: string;
  versionMeta?: string;
  manifest?: string;
  /** @format int64 */
  createdTime?: number;
  files?: FlattenFileVo[];
}

export interface PageInfoRuntimeVersionVo {
  /** @format int64 */
  total?: number;
  list?: RuntimeVersionVo[];
  /** @format int32 */
  pageNum?: number;
  /** @format int32 */
  pageSize?: number;
  /** @format int32 */
  size?: number;
  /** @format int64 */
  startRow?: number;
  /** @format int64 */
  endRow?: number;
  /** @format int32 */
  pages?: number;
  /** @format int32 */
  prePage?: number;
  /** @format int32 */
  nextPage?: number;
  isFirstPage?: boolean;
  isLastPage?: boolean;
  hasPreviousPage?: boolean;
  hasNextPage?: boolean;
  /** @format int32 */
  navigatePages?: number;
  navigatepageNums?: number[];
  /** @format int32 */
  navigateFirstPage?: number;
  /** @format int32 */
  navigateLastPage?: number;
}

export interface ResponseMessagePageInfoRuntimeVersionVo {
  code?: string;
  message?: string;
  data?: PageInfoRuntimeVersionVo;
}

/**
 * RuntimeVersion
 * Runtime version object
 */
export interface RuntimeVersionVo {
  id?: string;
  name?: string;
  tag?: string;
  alias?: string;
  meta?: object;
  image?: string;
  /** @format int64 */
  createdTime?: number;
  /** User object */
  owner?: UserVo;
}

export interface FileNode {
  name?: string;
  signature?: string;
  flag?: "added" | "updated" | "deleted" | "unchanged";
  mime?: string;
  type?: "directory" | "file";
  desc?: string;
  size?: string;
  files?: FileNode[];
}

/**
 * ModelInfo
 * Model information object
 */
export interface ModelInfoVo {
  id?: string;
  name?: string;
  versionAlias?: string;
  versionName?: string;
  versionTag?: string;
  versionMeta?: string;
  manifest?: string;
  /** @format int64 */
  createdTime?: number;
  files?: FileNode[];
}

/**
 * Job
 * Job object
 */
export interface JobVo {
  id?: string;
  uuid?: string;
  modelName?: string;
  modelVersion?: string;
  datasets?: string[];
  /** Runtime object */
  runtime?: RuntimeVo;
  device?: string;
  /** @format int32 */
  deviceAmount?: number;
  /** User object */
  owner?: UserVo;
  /** @format int64 */
  createdTime?: number;
  /** @format int64 */
  stopTime?: number;
  jobStatus?:
    | "CREATED"
    | "READY"
    | "PAUSED"
    | "RUNNING"
    | "TO_CANCEL"
    | "CANCELLING"
    | "CANCELED"
    | "SUCCESS"
    | "FAIL"
    | "UNKNOWN";
  comment?: string;
  resourcePool?: string;
  /** @format int64 */
  duration?: number;
}

/**
 * Runtime
 * Runtime object
 */
export interface RuntimeVo {
  id?: string;
  name?: string;
  /** @format int64 */
  createdTime?: number;
  /** User object */
  owner?: UserVo;
  /** Runtime version object */
  version?: RuntimeVersionVo;
}

export interface Graph {
  /** @format int64 */
  id?: number;
  groupingNodes?: Record<string, GraphNode[]>;
  edges?: GraphEdge[];
}

export interface GraphEdge {
  /** @format int64 */
  from?: number;
  /** @format int64 */
  to?: number;
  content?: string;
}

export interface GraphNode {
  /** @format int64 */
  id?: number;
  type?: string;
  content?: object;
  group?: string;
  /** @format int64 */
  entityId?: number;
}

export interface AttributeValueVo {
  name?: string;
  type?: string;
  value?: string;
}

export interface PageInfoSummaryVo {
  /** @format int64 */
  total?: number;
  list?: SummaryVo[];
  /** @format int32 */
  pageNum?: number;
  /** @format int32 */
  pageSize?: number;
  /** @format int32 */
  size?: number;
  /** @format int64 */
  startRow?: number;
  /** @format int64 */
  endRow?: number;
  /** @format int32 */
  pages?: number;
  /** @format int32 */
  prePage?: number;
  /** @format int32 */
  nextPage?: number;
  isFirstPage?: boolean;
  isLastPage?: boolean;
  hasPreviousPage?: boolean;
  hasNextPage?: boolean;
  /** @format int32 */
  navigatePages?: number;
  navigatepageNums?: number[];
  /** @format int32 */
  navigateFirstPage?: number;
  /** @format int32 */
  navigateLastPage?: number;
}

export interface ResponseMessagePageInfoSummaryVo {
  code?: string;
  message?: string;
  data?: PageInfoSummaryVo;
}

/**
 * Evaluation
 * Evaluation Summary object
 */
export interface SummaryVo {
  id?: string;
  uuid?: string;
  projectId?: string;
  projectName?: string;
  modelName?: string;
  modelVersion?: string;
  datasets?: string;
  runtime?: string;
  device?: string;
  /** @format int32 */
  deviceAmount?: number;
  /** @format int64 */
  createdTime?: number;
  /** @format int64 */
  stopTime?: number;
  owner?: string;
  /** @format int64 */
  duration?: number;
  jobStatus?:
    | "CREATED"
    | "READY"
    | "PAUSED"
    | "RUNNING"
    | "TO_CANCEL"
    | "CANCELLING"
    | "CANCELED"
    | "SUCCESS"
    | "FAIL"
    | "UNKNOWN";
  attributes?: AttributeValueVo[];
}

/**
 * Evaluation
 * Evaluation View Config object
 */
export interface ConfigVo {
  name?: string;
  content?: string;
  /** @format int64 */
  createTime?: number;
}

export interface ResponseMessageConfigVo {
  code?: string;
  message?: string;
  /** Evaluation View Config object */
  data?: ConfigVo;
}

/**
 * Evaluation
 * Evaluation Attribute object
 */
export interface AttributeVo {
  name?: string;
  type?: string;
}

export interface ResponseMessageListAttributeVo {
  code?: string;
  message?: string;
  data?: AttributeVo[];
}

/**
 * DatasetInfo
 * SWDataset information object
 */
export interface DatasetInfoVo {
  indexTable?: string;
  id?: string;
  name?: string;
  versionName?: string;
  versionAlias?: string;
  versionTag?: string;
  versionMeta?: string;
  /** @format int64 */
  createdTime?: number;
  files?: FlattenFileVo[];
}

export interface PageInfoPanelPluginVo {
  /** @format int64 */
  total?: number;
  list?: PanelPluginVo[];
  /** @format int32 */
  pageNum?: number;
  /** @format int32 */
  pageSize?: number;
  /** @format int32 */
  size?: number;
  /** @format int64 */
  startRow?: number;
  /** @format int64 */
  endRow?: number;
  /** @format int32 */
  pages?: number;
  /** @format int32 */
  prePage?: number;
  /** @format int32 */
  nextPage?: number;
  isFirstPage?: boolean;
  isLastPage?: boolean;
  hasPreviousPage?: boolean;
  hasNextPage?: boolean;
  /** @format int32 */
  navigatePages?: number;
  navigatepageNums?: number[];
  /** @format int32 */
  navigateFirstPage?: number;
  /** @format int32 */
  navigateLastPage?: number;
}

export interface PanelPluginVo {
  id?: string;
  name?: string;
  version?: string;
}

export interface ResponseMessagePageInfoPanelPluginVo {
  code?: string;
  message?: string;
  data?: PageInfoPanelPluginVo;
}

export interface UserRoleDeleteRequest {
  currentUserPwd: string;
}

import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, HeadersDefaults, ResponseType } from "axios";

export type QueryParamsType = Record<string | number, any>;

export interface FullRequestParams extends Omit<AxiosRequestConfig, "data" | "params" | "url" | "responseType"> {
  /** set parameter to `true` for call `securityWorker` for this request */
  secure?: boolean;
  /** request path */
  path: string;
  /** content type of request body */
  type?: ContentType;
  /** query params */
  query?: QueryParamsType;
  /** format of response (i.e. response.json() -> format: "json") */
  format?: ResponseType;
  /** request body */
  body?: unknown;
}

export type RequestParams = Omit<FullRequestParams, "body" | "method" | "query" | "path">;

export interface ApiConfig<SecurityDataType = unknown> extends Omit<AxiosRequestConfig, "data" | "cancelToken"> {
  securityWorker?: (
    securityData: SecurityDataType | null,
  ) => Promise<AxiosRequestConfig | void> | AxiosRequestConfig | void;
  secure?: boolean;
  format?: ResponseType;
}

export enum ContentType {
  Json = "application/json",
  FormData = "multipart/form-data",
  UrlEncoded = "application/x-www-form-urlencoded",
}

export class HttpClient<SecurityDataType = unknown> {
  public instance: AxiosInstance;
  private securityData: SecurityDataType | null = null;
  private securityWorker?: ApiConfig<SecurityDataType>["securityWorker"];
  private secure?: boolean;
  private format?: ResponseType;

  constructor({ securityWorker, secure, format, ...axiosConfig }: ApiConfig<SecurityDataType> = {}) {
    this.instance = axios.create({ ...axiosConfig, baseURL: axiosConfig.baseURL || "http://localhost:8082" });
    this.secure = secure;
    this.format = format;
    this.securityWorker = securityWorker;
  }

  public setSecurityData = (data: SecurityDataType | null) => {
    this.securityData = data;
  };

  protected mergeRequestParams(params1: AxiosRequestConfig, params2?: AxiosRequestConfig): AxiosRequestConfig {
    const method = params1.method || (params2 && params2.method);

    return {
      ...this.instance.defaults,
      ...params1,
      ...(params2 || {}),
      headers: {
        ...((method && this.instance.defaults.headers[method.toLowerCase() as keyof HeadersDefaults]) || {}),
        ...(params1.headers || {}),
        ...((params2 && params2.headers) || {}),
      },
    };
  }

  protected stringifyFormItem(formItem: unknown) {
    if (typeof formItem === "object" && formItem !== null) {
      return JSON.stringify(formItem);
    } else {
      return `${formItem}`;
    }
  }

  protected createFormData(input: Record<string, unknown>): FormData {
    return Object.keys(input || {}).reduce((formData, key) => {
      const property = input[key];
      const propertyContent: Iterable<any> = property instanceof Array ? property : [property];

      for (const formItem of propertyContent) {
        const isFileType = formItem instanceof Blob || formItem instanceof File;
        formData.append(key, isFileType ? formItem : this.stringifyFormItem(formItem));
      }

      return formData;
    }, new FormData());
  }

  public request = async <T = any, _E = any>({
    secure,
    path,
    type,
    query,
    format,
    body,
    ...params
  }: FullRequestParams): Promise<AxiosResponse<T>> => {
    const secureParams =
      ((typeof secure === "boolean" ? secure : this.secure) &&
        this.securityWorker &&
        (await this.securityWorker(this.securityData))) ||
      {};
    const requestParams = this.mergeRequestParams(params, secureParams);
    const responseFormat = format || this.format || undefined;

    if (type === ContentType.FormData && body && body !== null && typeof body === "object") {
      body = this.createFormData(body as Record<string, unknown>);
    }

    return this.instance.request({
      ...requestParams,
      headers: {
        ...(requestParams.headers || {}),
        ...(type && type !== ContentType.FormData ? { "Content-Type": type } : {}),
      },
      params: query,
      responseType: responseFormat,
      data: body,
      url: path,
    });
  };
}

/**
 * @title Starwhale Rest Api
 * @version {sw.version:1.0}
 * @baseUrl http://localhost:8082
 *
 * Rest Api for Starwhale controller
 */
export class Api<SecurityDataType extends unknown> {
  http: HttpClient<SecurityDataType>;

  constructor(http: HttpClient<SecurityDataType>) {
    this.http = http;
  }

  api = {
    /**
     * No description
     *
     * @tags User
     * @name UpdateUserState
     * @summary Enable or disable a user
     * @request PUT:/api/v1/user/{userId}/state
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    updateUserState: (userId: string, data: UserUpdateStateRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/user/${userId}/state`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name UpdateUserPwd
     * @summary Change user password
     * @request PUT:/api/v1/user/{userId}/pwd
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    updateUserPwd: (userId: string, data: UserUpdatePasswordRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/user/${userId}/pwd`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name UpdateCurrentUserPassword
     * @summary Update Current User password
     * @request PUT:/api/v1/user/current/pwd
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    updateCurrentUserPassword: (data: UserUpdatePasswordRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/user/current/pwd`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name CheckCurrentUserPassword
     * @summary Check Current User password
     * @request POST:/api/v1/user/current/pwd
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    checkCurrentUserPassword: (data: UserCheckPasswordRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/user/current/pwd`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name UpdateUserSystemRole
     * @summary Update user role of system
     * @request PUT:/api/v1/role/{systemRoleId}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    updateUserSystemRole: (systemRoleId: string, data: UserRoleUpdateRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/role/${systemRoleId}`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name DeleteUserSystemRole
     * @summary Delete user role of system
     * @request DELETE:/api/v1/role/{systemRoleId}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    deleteUserSystemRole: (systemRoleId: string, data: UserRoleDeleteRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/role/${systemRoleId}`,
        method: "DELETE",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * @description Returns a single project object.
     *
     * @tags Project
     * @name GetProjectByUrl
     * @summary Get a project by Url
     * @request GET:/api/v1/project/{projectUrl}
     * @secure
     * @response `200` `ProjectVo` ok.
     */
    getProjectByUrl: (projectUrl: string, params: RequestParams = {}) =>
      this.http.request<ProjectVo, any>({
        path: `/api/v1/project/${projectUrl}`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Project
     * @name UpdateProject
     * @summary Modify project information
     * @request PUT:/api/v1/project/{projectUrl}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    updateProject: (projectUrl: string, data: UpdateProjectRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Project
     * @name DeleteProjectByUrl
     * @summary Delete a project by Url
     * @request DELETE:/api/v1/project/{projectUrl}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    deleteProjectByUrl: (projectUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}`,
        method: "DELETE",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Trash
     * @name RecoverTrash
     * @summary Recover trash by id
     * @request PUT:/api/v1/project/{projectUrl}/trash/{trashId}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    recoverTrash: (projectUrl: string, trashId: number, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/trash/${trashId}`,
        method: "PUT",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Trash
     * @name DeleteTrash
     * @summary Delete trash
     * @request DELETE:/api/v1/project/{projectUrl}/trash/{trashId}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    deleteTrash: (projectUrl: string, trashId: number, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/trash/${trashId}`,
        method: "DELETE",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags SWRuntime
     * @name ManageRuntimeTag
     * @summary Manage tag of the runtime version
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    manageRuntimeTag: (
      projectUrl: string,
      runtimeUrl: string,
      versionUrl: string,
      data: RuntimeTagRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/tag`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags SWRuntime
     * @name ModifyRuntime
     * @summary Set tag of the model version
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    modifyRuntime: (
      projectUrl: string,
      runtimeUrl: string,
      runtimeVersionUrl: string,
      data: RuntimeTagRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${runtimeVersionUrl}`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags SWRuntime
     * @name RecoverRuntime
     * @summary Recover a runtime
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/recover
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    recoverRuntime: (projectUrl: string, runtimeUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/recover`,
        method: "PUT",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Project
     * @name ModifyProjectRole
     * @summary Modify a project role
     * @request PUT:/api/v1/project/{projectUrl}/role/{projectRoleId}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    modifyProjectRole: (
      projectUrl: string,
      projectRoleId: string,
      query: {
        roleId: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/role/${projectRoleId}`,
        method: "PUT",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Project
     * @name DeleteProjectRole
     * @summary Delete a project role
     * @request DELETE:/api/v1/project/{projectUrl}/role/{projectRoleId}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    deleteProjectRole: (projectUrl: string, projectRoleId: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/role/${projectRoleId}`,
        method: "DELETE",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name ModifyModel
     * @summary Set tag of the model version
     * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    modifyModel: (
      projectUrl: string,
      modelUrl: string,
      versionUrl: string,
      data: ModelTagRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * @description head for model info
     *
     * @tags Model
     * @name HeadModel
     * @summary head for model info
     * @request HEAD:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}
     * @secure
     * @response `200` `FlushRequest` ok
     */
    headModel: (projectUrl: string, modelUrl: string, versionUrl: string, params: RequestParams = {}) =>
      this.http.request<FlushRequest, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}`,
        method: "HEAD",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name ManageModelTag
     * @summary Manage tag of the model version
     * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    manageModelTag: (
      projectUrl: string,
      modelUrl: string,
      versionUrl: string,
      data: ModelTagRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/tag`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name RecoverModel
     * @summary Recover a model
     * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/recover
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    recoverModel: (projectUrl: string, modelUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/recover`,
        method: "PUT",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name FindJob
     * @summary Job information
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}
     * @secure
     * @response `200` `JobVo` OK
     */
    findJob: (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
      this.http.request<JobVo, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name ModifyJobComment
     * @summary Set Job Comment
     * @request PUT:/api/v1/project/{projectUrl}/job/{jobUrl}
     * @secure
     * @response `200` `string` OK
     */
    modifyJobComment: (projectUrl: string, jobUrl: string, data: JobModifyRequest, params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name RemoveJob
     * @summary Remove job
     * @request DELETE:/api/v1/project/{projectUrl}/job/{jobUrl}
     * @secure
     * @response `200` `string` OK
     */
    removeJob: (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description add|remove|set tags
     *
     * @tags Dataset
     * @name ManageDatasetTag
     * @summary Manage tag of the dataset version
     * @request PUT:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    manageDatasetTag: (
      projectUrl: string,
      datasetUrl: string,
      versionUrl: string,
      data: DatasetTagRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/tag`,
        method: "PUT",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Dataset
     * @name RecoverDataset
     * @summary Recover a dataset
     * @request PUT:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/recover
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    recoverDataset: (projectUrl: string, datasetUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/recover`,
        method: "PUT",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Project
     * @name RecoverProject
     * @summary Recover a project
     * @request PUT:/api/v1/project/{projectId}/recover
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    recoverProject: (projectId: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectId}/recover`,
        method: "PUT",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name ListUser
     * @summary Get the list of users
     * @request GET:/api/v1/user
     * @secure
     * @response `200` `PageInfo` ok
     */
    listUser: (
      query?: {
        /** User name prefix to search for */
        userName?: string;
        /**
         * Page number
         * @format int32
         */
        pageNum?: number;
        /**
         * Rows per page
         * @format int32
         */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<PageInfo, any>({
        path: `/api/v1/user`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name CreateUser
     * @summary Create a new user
     * @request POST:/api/v1/user
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    createUser: (data: UserRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/user`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags System
     * @name SystemVersionAction
     * @summary Upgrade system version or cancel upgrade
     * @request POST:/api/v1/system/version/{action}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    systemVersionAction: (action: "upgrade" | "cancel", params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/system/version/${action}`,
        method: "POST",
        secure: true,
        ...params,
      }),

    /**
     * @description Get system settings in yaml string
     *
     * @tags System
     * @name QuerySetting
     * @summary Get system settings
     * @request GET:/api/v1/system/setting
     * @secure
     * @response `200` `string` ok
     */
    querySetting: (params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/system/setting`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Update system settings
     *
     * @tags System
     * @name UpdateSetting
     * @summary Update system settings
     * @request POST:/api/v1/system/setting
     * @secure
     * @response `200` `string` ok
     */
    updateSetting: (data: string, params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/system/setting`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name ListSystemRoles
     * @summary List system role of users
     * @request GET:/api/v1/role
     * @secure
     * @response `200` `ResponseMessageListSystemRoleVo` ok
     */
    listSystemRoles: (params: RequestParams = {}) =>
      this.http.request<ResponseMessageListSystemRoleVo, any>({
        path: `/api/v1/role`,
        method: "GET",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name AddUserSystemRole
     * @summary Add user role of system
     * @request POST:/api/v1/role
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    addUserSystemRole: (data: UserRoleAddRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/role`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Project
     * @name ListProject
     * @summary Get the list of projects
     * @request GET:/api/v1/project
     * @secure
     * @response `200` `PageInfo` ok
     */
    listProject: (
      query?: {
        projectName?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
        sort?: string;
        /** @format int32 */
        order?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<PageInfo, any>({
        path: `/api/v1/project`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Project
     * @name CreateProject
     * @summary Create or Recover a new project
     * @request POST:/api/v1/project
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    createProject: (data: CreateProjectRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * @description Select a historical version of the runtime and revert the latest version of the current runtime to this version
     *
     * @tags SWRuntime
     * @name RevertRuntimeVersion
     * @summary Revert Runtime version
     * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/revert
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    revertRuntimeVersion: (
      projectUrl: string,
      runtimeUrl: string,
      data: RuntimeRevertRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/revert`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        format: "json",
        ...params,
      }),

    /**
     * @description Create a new version of the runtime. The data resources can be selected by uploading the file package or entering the server path.
     *
     * @tags SWRuntime
     * @name Upload
     * @summary Create a new runtime version
     * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeName}/version/{versionName}/file
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    upload: (
      projectUrl: string,
      runtimeName: string,
      versionName: string,
      query: {
        uploadRequest: ClientRuntimeRequest;
      },
      data: {
        /**
         * file detail
         * @format binary
         */
        file: File;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeName}/version/${versionName}/file`,
        method: "POST",
        query: query,
        body: data,
        secure: true,
        type: ContentType.FormData,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Project
     * @name ListProjectRole
     * @summary List project roles
     * @request GET:/api/v1/project/{projectUrl}/role
     * @secure
     * @response `200` `ResponseMessageListProjectRoleVo` ok
     */
    listProjectRole: (projectUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageListProjectRoleVo, any>({
        path: `/api/v1/project/${projectUrl}/role`,
        method: "GET",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Project
     * @name AddProjectRole
     * @summary Grant project role to a user
     * @request POST:/api/v1/project/{projectUrl}/role
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    addProjectRole: (
      projectUrl: string,
      query: {
        userId: string;
        roleId: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/role`,
        method: "POST",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * @description Select a historical version of the model and revert the latest version of the current model to this version
     *
     * @tags Model
     * @name RevertModelVersion
     * @summary Revert model version
     * @request POST:/api/v1/project/{projectUrl}/model/{modelUrl}/revert
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    revertModelVersion: (
      projectUrl: string,
      modelUrl: string,
      data: RevertModelVersionRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/revert`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * @description Create a new version of the model. The data resources can be selected by uploading the file package or entering the server path.
     *
     * @tags Model
     * @name Upload1
     * @summary Create a new model version
     * @request POST:/api/v1/project/{projectUrl}/model/{modelName}/version/{versionName}/file
     * @secure
     * @response `200` `ResponseMessageObject` ok
     */
    upload1: (
      projectUrl: string,
      modelName: string,
      versionName: string,
      query: {
        uploadRequest: ModelUploadRequest;
      },
      data: {
        /**
         * file detail
         * @format binary
         */
        file?: File;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageObject, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelName}/version/${versionName}/file`,
        method: "POST",
        query: query,
        body: data,
        secure: true,
        type: ContentType.FormData,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name ListJobs
     * @summary Get the list of jobs
     * @request GET:/api/v1/project/{projectUrl}/job
     * @secure
     * @response `200` `PageInfo` ok
     */
    listJobs: (
      projectUrl: string,
      query?: {
        swmpId?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<PageInfo, any>({
        path: `/api/v1/project/${projectUrl}/job`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name CreateJob
     * @summary Create a new job
     * @request POST:/api/v1/project/{projectUrl}/job
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    createJob: (projectUrl: string, data: JobRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/job`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name Action
     * @summary Job Action
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/{action}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    action: (projectUrl: string, jobUrl: string, action: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/${action}`,
        method: "POST",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name RecoverJob
     * @summary Recover job
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/recover
     * @secure
     * @response `200` `string` OK
     */
    recoverJob: (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/recover`,
        method: "POST",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Evaluation
     * @name GetViewConfig
     * @summary Get View Config
     * @request GET:/api/v1/project/{projectUrl}/evaluation/view/config
     * @secure
     * @response `200` `ResponseMessageConfigVo` ok
     */
    getViewConfig: (
      projectUrl: string,
      query: {
        name: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageConfigVo, any>({
        path: `/api/v1/project/${projectUrl}/evaluation/view/config`,
        method: "GET",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Evaluation
     * @name CreateViewConfig
     * @summary Create or Update View Config
     * @request POST:/api/v1/project/{projectUrl}/evaluation/view/config
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    createViewConfig: (projectUrl: string, data: ConfigRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/evaluation/view/config`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Dataset
     * @name ConsumeNextData
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/consume
     * @secure
     * @response `200` `ResponseMessageDataIndexDesc` OK
     */
    consumeNextData: (
      projectUrl: string,
      datasetUrl: string,
      versionUrl: string,
      data: DataConsumptionRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageDataIndexDesc, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/consume`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * @description Select a historical version of the dataset and revert the latest version of the current dataset to this version
     *
     * @tags Dataset
     * @name RevertDatasetVersion
     * @summary Revert Dataset version
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/revert
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    revertDatasetVersion: (
      projectUrl: string,
      datasetUrl: string,
      data: RevertDatasetRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/revert`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * @description Create a new version of the dataset. The data resources can be selected by uploading the file package or entering the server path.
     *
     * @tags Dataset
     * @name UploadDs
     * @summary Create a new dataset version
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetName}/version/{versionName}/file
     * @secure
     * @response `200` `ResponseMessageUploadResult` ok
     */
    uploadDs: (
      projectUrl: string,
      datasetName: string,
      versionName: string,
      query: {
        uploadRequest: DatasetUploadRequest;
      },
      data: {
        /**
         * file detail
         * @format binary
         */
        file?: File;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageUploadResult, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetName}/version/${versionName}/file`,
        method: "POST",
        query: query,
        body: data,
        secure: true,
        type: ContentType.FormData,
        format: "json",
        ...params,
      }),

    /**
     * @description Sign SWDS uris to get a batch of temporarily accessible links
     *
     * @tags Dataset
     * @name SignLinks
     * @summary Sign SWDS uris to get a batch of temporarily accessible links
     * @request POST:/api/v1/project/{projectName}/dataset/{datasetName}/version/{version}/sign-links
     * @secure
     * @response `200` `ResponseMessageMapObjectObject` ok
     */
    signLinks: (
      projectName: string,
      datasetName: string,
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
      this.http.request<ResponseMessageMapObjectObject, any>({
        path: `/api/v1/project/${projectName}/dataset/${datasetName}/version/${version}/sign-links`,
        method: "POST",
        query: query,
        body: data,
        secure: true,
        type: ContentType.Json,
        format: "json",
        ...params,
      }),

    /**
     * @description Get panel setting by project and key
     *
     * @tags Panel
     * @name GetPanelSetting
     * @summary Get panel setting
     * @request GET:/api/v1/panel/setting/{project_id}/{key}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    getPanelSetting: (projectId: string, key: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/panel/setting/${projectId}/${key}`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Save panel setting by project and key
     *
     * @tags Panel
     * @name SetPanelSetting
     * @summary Save panel setting
     * @request POST:/api/v1/panel/setting/{project_id}/{key}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    setPanelSetting: (projectId: string, key: string, data: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/panel/setting/${projectId}/${key}`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        format: "json",
        ...params,
      }),

    /**
     * @description List all plugins
     *
     * @tags Panel
     * @name PluginList
     * @summary List all plugins
     * @request GET:/api/v1/panel/plugin
     * @secure
     * @response `200` `ResponseMessagePageInfoPanelPluginVo` ok
     */
    pluginList: (params: RequestParams = {}) =>
      this.http.request<ResponseMessagePageInfoPanelPluginVo, any>({
        path: `/api/v1/panel/plugin`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Upload a tarball and install as panel plugin
     *
     * @tags Panel
     * @name InstallPlugin
     * @summary Install a plugin
     * @request POST:/api/v1/panel/plugin
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    installPlugin: (
      data: {
        /**
         * file detail
         * @format binary
         */
        file: File;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/panel/plugin`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.FormData,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags data-store-controller
     * @name UpdateTable
     * @request POST:/api/v1/datastore/updateTable
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    updateTable: (data: UpdateTableRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/datastore/updateTable`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags data-store-controller
     * @name ScanTable
     * @request POST:/api/v1/datastore/scanTable
     * @secure
     * @response `200` `ResponseMessageRecordListVo` OK
     */
    scanTable: (data: ScanTableRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageRecordListVo, any>({
        path: `/api/v1/datastore/scanTable`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags data-store-controller
     * @name QueryTable
     * @request POST:/api/v1/datastore/queryTable
     * @secure
     * @response `200` `ResponseMessageRecordListVo` OK
     */
    queryTable: (data: QueryTableRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageRecordListVo, any>({
        path: `/api/v1/datastore/queryTable`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags data-store-controller
     * @name QueryAndExport
     * @request POST:/api/v1/datastore/queryTable/export
     * @secure
     * @response `200` `void` OK
     */
    queryAndExport: (data: QueryTableRequest, params: RequestParams = {}) =>
      this.http.request<void, any>({
        path: `/api/v1/datastore/queryTable/export`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags data-store-controller
     * @name ListTables
     * @request POST:/api/v1/datastore/listTables
     * @secure
     * @response `200` `ResponseMessageTableNameListVo` OK
     */
    listTables: (data: ListTablesRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageTableNameListVo, any>({
        path: `/api/v1/datastore/listTables`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags data-store-controller
     * @name Flush
     * @request POST:/api/v1/datastore/flush
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    flush: (
      query: {
        request: FlushRequest;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/datastore/flush`,
        method: "POST",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * @description head for runtime info
     *
     * @tags SWRuntime
     * @name HeadRuntime
     * @summary head for runtime info
     * @request HEAD:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}
     * @secure
     * @response `200` `FlushRequest` OK
     */
    headRuntime: (projectUrl: string, runtimeUrl: string, versionUrl: string, params: RequestParams = {}) =>
      this.http.request<FlushRequest, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}`,
        method: "HEAD",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description head for dataset info
     *
     * @tags Dataset
     * @name HeadDataset
     * @summary head for dataset info
     * @request HEAD:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}
     * @secure
     * @response `200` `FlushRequest` ok
     */
    headDataset: (projectUrl: string, datasetUrl: string, versionUrl: string, params: RequestParams = {}) =>
      this.http.request<FlushRequest, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}`,
        method: "HEAD",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name GetUserById
     * @summary Get a user by user ID
     * @request GET:/api/v1/user/{userId}
     * @secure
     * @response `200` `UserVo` ok.
     */
    getUserById: (userId: string, params: RequestParams = {}) =>
      this.http.request<UserVo, any>({
        path: `/api/v1/user/${userId}`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Get token of any user for third party system integration, only super admin is allowed to do this
     *
     * @tags User
     * @name UserToken
     * @summary Get arbitrary user token
     * @request GET:/api/v1/user/token/{userId}
     * @secure
     * @response `200` `string` ok
     */
    userToken: (userId: number, params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/user/token/${userId}`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name GetCurrentUser
     * @summary Get the current logged in user.
     * @request GET:/api/v1/user/current
     * @secure
     * @response `200` `UserVo` ok.
     */
    getCurrentUser: (params: RequestParams = {}) =>
      this.http.request<UserVo, any>({
        path: `/api/v1/user/current`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name GetCurrentUserRoles
     * @summary Get the current user roles.
     * @request GET:/api/v1/user/current/role
     * @secure
     * @response `200` `ResponseMessageListProjectRoleVo` ok
     */
    getCurrentUserRoles: (
      query?: {
        projectUrl?: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageListProjectRoleVo, any>({
        path: `/api/v1/user/current/role`,
        method: "GET",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags System
     * @name GetCurrentVersion
     * @summary Get current version of the system
     * @request GET:/api/v1/system/version
     * @secure
     * @response `200` `SystemVersionVo` ok
     */
    getCurrentVersion: (params: RequestParams = {}) =>
      this.http.request<SystemVersionVo, any>({
        path: `/api/v1/system/version`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Get the current server upgrade process. If downloading, return the download progress
     *
     * @tags System
     * @name GetUpgradeProgress
     * @summary Get the current upgrade progress
     * @request GET:/api/v1/system/version/progress
     * @secure
     * @response `200` `UpgradeProgressVo` ok
     */
    getUpgradeProgress: (params: RequestParams = {}) =>
      this.http.request<UpgradeProgressVo, any>({
        path: `/api/v1/system/version/progress`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags System
     * @name GetLatestVersion
     * @summary Get latest version of the system
     * @request GET:/api/v1/system/version/latest
     * @secure
     * @response `200` `SystemVersionVo` ok
     */
    getLatestVersion: (params: RequestParams = {}) =>
      this.http.request<SystemVersionVo, any>({
        path: `/api/v1/system/version/latest`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags System
     * @name ListResourcePools
     * @summary Get the list of resource pool
     * @request GET:/api/v1/system/resourcePool
     * @secure
     * @response `200` `string` ok
     */
    listResourcePools: (params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/system/resourcePool`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Runtime
     * @name ListDevice
     * @summary Get the list of device types
     * @request GET:/api/v1/runtime/device
     * @secure
     * @response `200` `string` ok
     */
    listDevice: (params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/runtime/device`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags User
     * @name ListRoles
     * @summary List role enums
     * @request GET:/api/v1/role/enums
     * @secure
     * @response `200` `ResponseMessageListRoleVo` ok
     */
    listRoles: (params: RequestParams = {}) =>
      this.http.request<ResponseMessageListRoleVo, any>({
        path: `/api/v1/role/enums`,
        method: "GET",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Trash
     * @name ListTrash
     * @summary Get the list of trash
     * @request GET:/api/v1/project/{projectUrl}/trash
     * @secure
     * @response `200` `PageInfo` ok
     */
    listTrash: (
      projectUrl: string,
      query?: {
        name?: string;
        operator?: string;
        type?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<PageInfo, any>({
        path: `/api/v1/project/${projectUrl}/trash`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags SWRuntime
     * @name ListRuntime
     * @summary Get the list of runtimes
     * @request GET:/api/v1/project/{projectUrl}/runtime
     * @secure
     * @response `200` `PageInfo` ok
     */
    listRuntime: (
      projectUrl: string,
      query?: {
        /** Runtime name prefix to search for */
        runtimeName?: string;
        /**
         * Page number
         * @format int32
         */
        pageNum?: number;
        /**
         * Rows per page
         * @format int32
         */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<PageInfo, any>({
        path: `/api/v1/project/${projectUrl}/runtime`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Return the information of the latest version of the current runtime
     *
     * @tags SWRuntime
     * @name GetRuntimeInfo
     * @summary Get the information of a runtime
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}
     * @secure
     * @response `200` `ResponseMessageRuntimeInfoVo` OK
     */
    getRuntimeInfo: (
      projectUrl: string,
      runtimeUrl: string,
      query?: {
        runtimeVersionUrl?: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageRuntimeInfoVo, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags SWRuntime
     * @name DeleteRuntime
     * @summary Delete a runtime
     * @request DELETE:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    deleteRuntime: (projectUrl: string, runtimeUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags SWRuntime
     * @name ListRuntimeVersion
     * @summary Get the list of the runtime versions
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version
     * @secure
     * @response `200` `ResponseMessagePageInfoRuntimeVersionVo` OK
     */
    listRuntimeVersion: (
      projectUrl: string,
      runtimeUrl: string,
      query?: {
        /** Runtime version name prefix */
        name?: string;
        /** Runtime version tag */
        tag?: string;
        /**
         * The page number
         * @format int32
         */
        pageNum?: number;
        /**
         * Rows per page
         * @format int32
         */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoRuntimeVersionVo, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Pull file of a runtime version.
     *
     * @tags SWRuntime
     * @name Pull
     * @summary Pull file of a runtime version
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/file
     * @secure
     * @response `200` `void` ok
     */
    pull: (projectUrl: string, runtimeUrl: string, versionUrl: string, params: RequestParams = {}) =>
      this.http.request<void, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/file`,
        method: "GET",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name ListModel
     * @summary Get the list of models
     * @request GET:/api/v1/project/{projectUrl}/model
     * @secure
     * @response `200` `PageInfo` ok
     */
    listModel: (
      projectUrl: string,
      query?: {
        /** Model versionId */
        versionId?: string;
        /** Model name prefix to search for */
        modelName?: string;
        /**
         * Page number
         * @format int32
         */
        pageNum?: number;
        /**
         * Rows per page
         * @format int32
         */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<PageInfo, any>({
        path: `/api/v1/project/${projectUrl}/model`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Return the file information in the model package of the latest version of the current model
     *
     * @tags Model
     * @name GetModelInfo
     * @summary Model information
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}
     * @secure
     * @response `200` `ModelInfoVo` OK
     */
    getModelInfo: (
      projectUrl: string,
      modelUrl: string,
      query?: {
        /** Model versionUrl. (Return the current version as default when the versionUrl is not set.) */
        versionUrl?: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ModelInfoVo, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name DeleteModel
     * @summary Delete a model
     * @request DELETE:/api/v1/project/{projectUrl}/model/{modelUrl}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    deleteModel: (projectUrl: string, modelUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}`,
        method: "DELETE",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name ListModelVersion
     * @summary Get the list of model versions
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/version
     * @secure
     * @response `200` `PageInfo` ok
     */
    listModelVersion: (
      projectUrl: string,
      modelUrl: string,
      query?: {
        /** Model version name prefix to search for */
        name?: string;
        /** Model version tag */
        tag?: string;
        /**
         * Page number
         * @format int32
         */
        pageNum?: number;
        /**
         * Rows per page
         * @format int32
         */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<PageInfo, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Create a new version of the model.
     *
     * @tags Model
     * @name Pull1
     * @summary Pull file of a model version
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/file
     * @secure
     * @response `200` `void` ok
     */
    pull1: (projectUrl: string, modelUrl: string, versionUrl: string, params: RequestParams = {}) =>
      this.http.request<void, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/file`,
        method: "GET",
        secure: true,
        ...params,
      }),

    /**
     * @description Return the diff information between the base version and the compare version
     *
     * @tags Model
     * @name GetModelDiff
     * @summary Model Diff information
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/diff
     * @secure
     * @response `200` `ModelInfoVo` OK
     */
    getModelDiff: (
      projectUrl: string,
      modelUrl: string,
      query: {
        /** Model version of base.  */
        baseVersion: string;
        /** Model version of compare.  */
        compareVersion: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ModelInfoVo, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/diff`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name ListTasks
     * @summary Get the list of tasks
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/task
     * @secure
     * @response `200` `PageInfo` ok
     */
    listTasks: (
      projectUrl: string,
      jobUrl: string,
      query?: {
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<PageInfo, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name GetJobResult
     * @summary Job Evaluation Result
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/result
     * @secure
     * @response `200` `FlushRequest` OK
     */
    getJobResult: (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
      this.http.request<FlushRequest, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/result`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name GetJobDag
     * @summary DAG of Job
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/dag
     * @secure
     * @response `200` `Graph` OK
     */
    getJobDag: (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
      this.http.request<Graph, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/dag`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Evaluation
     * @name ListEvaluationSummary
     * @summary List Evaluation Summary
     * @request GET:/api/v1/project/{projectUrl}/evaluation
     * @secure
     * @response `200` `ResponseMessagePageInfoSummaryVo` ok
     */
    listEvaluationSummary: (
      projectUrl: string,
      query: {
        filter: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoSummaryVo, any>({
        path: `/api/v1/project/${projectUrl}/evaluation`,
        method: "GET",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Evaluation
     * @name ListAttributes
     * @summary List Evaluation Summary Attributes
     * @request GET:/api/v1/project/{projectUrl}/evaluation/view/attribute
     * @secure
     * @response `200` `ResponseMessageListAttributeVo` ok
     */
    listAttributes: (projectUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageListAttributeVo, any>({
        path: `/api/v1/project/${projectUrl}/evaluation/view/attribute`,
        method: "GET",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Dataset
     * @name ListDataset
     * @summary Get the list of the datasets
     * @request GET:/api/v1/project/{projectUrl}/dataset
     * @secure
     * @response `200` `PageInfo` ok
     */
    listDataset: (
      projectUrl: string,
      query?: {
        /** Dataset versionId */
        versionId?: string;
        /**
         * Page number
         * @format int32
         */
        pageNum?: number;
        /**
         * Rows per page
         * @format int32
         */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<PageInfo, any>({
        path: `/api/v1/project/${projectUrl}/dataset`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Return the information of the latest version of the current dataset
     *
     * @tags Dataset
     * @name GetDatasetInfo
     * @summary Get the information of a dataset
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}
     * @secure
     * @response `200` `DatasetInfoVo` OK
     */
    getDatasetInfo: (
      projectUrl: string,
      datasetUrl: string,
      query?: {
        /** Dataset versionUrl. (Return the current version as default when the versionUrl is not set.) */
        versionUrl?: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<DatasetInfoVo, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Dataset
     * @name DeleteDataset
     * @summary Delete a dataset
     * @request DELETE:/api/v1/project/{projectUrl}/dataset/{datasetUrl}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    deleteDataset: (projectUrl: string, datasetUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}`,
        method: "DELETE",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Dataset
     * @name ListDatasetVersion
     * @summary Get the list of the dataset versions
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version
     * @secure
     * @response `200` `PageInfo` ok
     */
    listDatasetVersion: (
      projectUrl: string,
      datasetUrl: string,
      query?: {
        /** Dataset version name prefix */
        name?: string;
        /** Dataset version tag */
        tag?: string;
        /**
         * The page number
         * @format int32
         */
        pageNum?: number;
        /**
         * Rows per page
         * @format int32
         */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<PageInfo, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Pull Dataset uri file contents
     *
     * @tags Dataset
     * @name PullLinkContent
     * @summary Pull Dataset uri file contents
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/link
     * @secure
     * @response `200` `void` ok
     */
    pullLinkContent: (
      projectUrl: string,
      datasetUrl: string,
      versionUrl: string,
      query: {
        /** uri of the link */
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
      this.http.request<void, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/link`,
        method: "GET",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * @description Pull Dataset files part by part.
     *
     * @tags Dataset
     * @name PullDs
     * @summary Pull Dataset files
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/file
     * @secure
     * @response `200` `void` ok
     */
    pullDs: (
      projectUrl: string,
      datasetUrl: string,
      versionUrl: string,
      query?: {
        /** optional, _manifest.yaml is used if not specified */
        part_name?: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<void, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/file`,
        method: "GET",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Log
     * @name OfflineLogs
     * @summary list the log files of a task
     * @request GET:/api/v1/log/offline/{taskId}
     * @secure
     * @response `200` `string` ok
     */
    offlineLogs: (taskId: number, params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/log/offline/${taskId}`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Log
     * @name LogContent
     * @summary Get the list of device types
     * @request GET:/api/v1/log/offline/{taskId}/{fileName}
     * @secure
     * @response `200` `string` ok
     */
    logContent: (taskId: number, fileName: string, params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/log/offline/${taskId}/${fileName}`,
        method: "GET",
        secure: true,
        ...params,
      }),

    /**
     * @description Uninstall plugin by id
     *
     * @tags Panel
     * @name UninstallPlugin
     * @summary Uninstall a plugin
     * @request DELETE:/api/v1/panel/plugin/{id}
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    uninstallPlugin: (id: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/panel/plugin/${id}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),
  };
}
