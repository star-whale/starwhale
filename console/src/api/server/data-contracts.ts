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
  code: string;
  message: string;
  data: string;
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
  privacy?: string;
  description?: string;
  readme?: string;
}

export interface ResponseMessageObject {
  code: string;
  message: string;
  data: object;
}

export interface RuntimeTagRequest {
  force?: boolean;
  tag: string;
}

export interface UpdateReportRequest {
  /**
   * @minLength 0
   * @maxLength 255
   */
  title?: string;
  /**
   * @minLength 0
   * @maxLength 255
   */
  description?: string;
  content?: string;
  shared?: boolean;
}

export interface ModelUpdateRequest {
  tag?: string;
  built_in_runtime?: string;
}

export interface JobModifyRequest {
  comment: string;
}

export interface ApplySignedUrlRequest {
  flag?: string;
  pathPrefix: string;
  /** @uniqueItems true */
  files: string[];
}

export interface ResponseMessageSignedUrlResponse {
  code: string;
  message: string;
  data: SignedUrlResponse;
}

export interface SignedUrlResponse {
  pathPrefix?: string;
  signedUrls?: Record<string, string>;
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

export interface Resource {
  name?: string;
  /** @format float */
  max?: number;
  /** @format float */
  min?: number;
  /** @format float */
  defaults?: number;
}

export interface ResourcePool {
  name?: string;
  nodeSelector?: Record<string, string>;
  resources?: Resource[];
  tolerations?: Toleration[];
  metadata?: Record<string, string>;
  isPrivate?: boolean;
  visibleUserIds?: number[];
}

export interface Toleration {
  key?: string;
  operator?: string;
  value?: string;
  effect?: string;
  /** @format int64 */
  tolerationSeconds?: number;
}

export interface UserRoleAddRequest {
  currentUserPwd: string;
  userId: string;
  roleId: string;
}

export interface CreateProjectRequest {
  /** @pattern ^[a-zA-Z][a-zA-Z\d_-]{2,80}$ */
  projectName: string;
  privacy: string;
  description: string;
}

export interface CreateModelVersionRequest {
  metaBlobId: string;
  builtInRuntime?: string;
  force?: boolean;
}

export interface CreateJobTemplateRequest {
  name: string;
  jobUrl: string;
}

export interface ModelServingRequest {
  modelVersionUrl: string;
  runtimeVersionUrl: string;
  resourcePool?: string;
  /**
   * @deprecated
   * @format int64
   */
  ttlInSeconds?: number;
  spec?: string;
}

/**
 * Model Serving
 * Model Serving object
 */
export interface ModelServingVo {
  id: string;
  baseUri?: string;
}

export interface ResponseMessageModelServingVo {
  code: string;
  message: string;
  /** Model Serving object */
  data: ModelServingVo;
}

/** user defined running configurations such environment variables */
export interface RunEnvs {
  envVars?: Record<string, string>;
}

/**
 * Runtime
 * Build runtime image result
 */
export interface BuildImageResult {
  success?: boolean;
  message?: string;
}

export interface ResponseMessageBuildImageResult {
  code: string;
  message: string;
  /** Build runtime image result */
  data: BuildImageResult;
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

export interface CreateReportRequest {
  /**
   * @minLength 1
   * @maxLength 255
   */
  title: string;
  /**
   * @minLength 0
   * @maxLength 255
   */
  description?: string;
  /**
   * @minLength 1
   * @maxLength 2147483647
   */
  content: string;
}

export interface TransferReportRequest {
  targetProjectUrl: string;
}

export interface ModelTagRequest {
  force?: boolean;
  tag: string;
}

export interface RevertModelVersionRequest {
  versionUrl: string;
}

export interface JobRequest {
  /** @format int64 */
  modelVersionId?: number;
  datasetVersionIds?: number[];
  /** @format int64 */
  runtimeVersionId?: number;
  /** @format int64 */
  timeToLiveInSec?: number;
  /** @deprecated */
  modelVersionUrl?: string;
  /** @deprecated */
  datasetVersionUrls?: string;
  /** @deprecated */
  runtimeVersionUrl?: string;
  comment?: string;
  resourcePool: string;
  handler?: string;
  stepSpecOverWrites?: string;
  type?: "EVALUATION" | "TRAIN" | "FINE_TUNE" | "SERVING" | "BUILT_IN";
  devMode?: boolean;
  devPassword?: string;
  devWay?: "VS_CODE";
}

export interface ExecRequest {
  command: string[];
}

export interface ExecResponse {
  stdout?: string;
  stderr?: string;
}

export interface ResponseMessageExecResponse {
  code: string;
  message: string;
  data: ExecResponse;
}

export interface JobModifyPinRequest {
  pinned: boolean;
}

export interface EventRequest {
  eventType: "INFO" | "WARNING" | "ERROR";
  source: "CLIENT" | "SERVER" | "NODE";
  relatedResource: RelatedResource;
  message: string;
  data?: string;
  /** @format int64 */
  timestamp?: number;
}

export interface RelatedResource {
  eventResourceType: "JOB" | "TASK" | "RUN";
  /** @format int64 */
  id: number;
}

export interface ResponseMessageMapStringString {
  code: string;
  message: string;
  data: Record<string, string>;
}

export interface ConfigRequest {
  name: string;
  content: string;
}

export interface DatasetTagRequest {
  force?: boolean;
  tag: string;
}

export interface DataConsumptionRequest {
  sessionId?: string;
  consumerId?: string;
  /** @format int32 */
  batchSize?: number;
  start?: string;
  startType?: string;
  startInclusive?: boolean;
  end?: string;
  endType?: string;
  endInclusive?: boolean;
  processedData?: DataIndexDesc[];
  /** @deprecated */
  serial?: boolean;
}

export interface DataIndexDesc {
  start?: string;
  startType?: string;
  startInclusive?: boolean;
  end?: string;
  endType?: string;
  endInclusive?: boolean;
}

export interface NullableResponseMessageDataIndexDesc {
  code: string;
  message: string;
  data?: DataIndexDesc;
}

export interface RevertDatasetRequest {
  versionUrl: string;
}

export interface DatasetUploadRequest {
  /** @format int64 */
  uploadId: number;
  partName?: string;
  signature?: string;
  uri?: string;
  desc?: "MANIFEST" | "SRC_TAR" | "SRC" | "MODEL" | "DATA" | "UNKNOWN";
  phase: "MANIFEST" | "BLOB" | "END" | "CANCEL";
  force?: string;
  project: string;
  swds: string;
}

export interface ResponseMessageUploadResult {
  code: string;
  message: string;
  data: UploadResult;
}

export interface UploadResult {
  /** @format int64 */
  uploadId?: number;
}

export interface DatasetBuildRequest {
  type: "IMAGE" | "VIDEO" | "AUDIO";
  shared?: boolean;
  storagePath: string;
}

export interface ResponseMessageMapObjectObject {
  code: string;
  message: string;
  data: Record<string, object>;
}

export interface SftSpaceCreateRequest {
  name: string;
  desc: string;
}

export type DsInfo = object;

export type ModelInfo = object;

export interface PageInfoSftVo {
  /** @format int64 */
  total?: number;
  list?: SftVo[];
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

export interface ResponseMessagePageInfoSftVo {
  code: string;
  message: string;
  data: PageInfoSftVo;
}

export interface SftVo {
  /** @format int64 */
  id?: number;
  /** @format int64 */
  jobId?: number;
  status?: "CREATED" | "READY" | "PAUSED" | "RUNNING" | "CANCELLING" | "CANCELED" | "SUCCESS" | "FAIL" | "UNKNOWN";
  /** @format int64 */
  startTime?: number;
  /** @format int64 */
  endTime?: number;
  trainDatasets?: DsInfo[];
  evalDatasets?: DsInfo[];
  baseModel?: ModelInfo;
  targetModel?: ModelInfo;
}

export interface SftCreateRequest {
  /** @format int64 */
  modelVersionId?: number;
  datasetVersionIds?: number[];
  /** @format int64 */
  runtimeVersionId?: number;
  /** @format int64 */
  timeToLiveInSec?: number;
  /** @deprecated */
  modelVersionUrl?: string;
  /** @deprecated */
  datasetVersionUrls?: string;
  /** @deprecated */
  runtimeVersionUrl?: string;
  comment?: string;
  resourcePool: string;
  handler?: string;
  stepSpecOverWrites?: string;
  type?: "EVALUATION" | "TRAIN" | "FINE_TUNE" | "SERVING" | "BUILT_IN";
  devMode?: boolean;
  devPassword?: string;
  devWay?: "VS_CODE";
  evalDatasetVersionIds?: number[];
}

export interface ColumnSchemaDesc {
  name?: string;
  /** @format int32 */
  index?: number;
  type?: string;
  pythonType?: string;
  elementType?: ColumnSchemaDesc;
  keyType?: ColumnSchemaDesc;
  valueType?: ColumnSchemaDesc;
  attributes?: ColumnSchemaDesc[];
  sparseKeyValuePairSchema?: Record<string, KeyValuePairSchema>;
}

export interface KeyValuePairSchema {
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
  startType?: string;
  startInclusive?: boolean;
  end?: string;
  endType?: string;
  endInclusive?: boolean;
  /** @format int32 */
  limit?: number;
  keepNone?: boolean;
  rawResult?: boolean;
  encodeWithType?: boolean;
  ignoreNonExistingTable?: boolean;
}

export interface TableDesc {
  tableName?: string;
  columnPrefix?: string;
  columns?: ColumnDesc[];
  keepNone?: boolean;
  revision?: string;
}

export interface ColumnHintsDesc {
  typeHints?: string[];
  columnValueHints?: string[];
  elementHints?: ColumnHintsDesc;
  keyHints?: ColumnHintsDesc;
  valueHints?: ColumnHintsDesc;
}

export interface RecordListVo {
  columnTypes?: ColumnSchemaDesc[];
  columnHints?: Record<string, ColumnHintsDesc>;
  records?: Record<string, object>[];
  lastKey?: string;
}

export interface ResponseMessageRecordListVo {
  code: string;
  message: string;
  data: RecordListVo;
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
  encodeWithType?: boolean;
  ignoreNonExistingTable?: boolean;
  revision?: string;
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
  /** @uniqueItems true */
  prefixes?: string[];
}

export interface ResponseMessageTableNameListVo {
  code: string;
  message: string;
  data: TableNameListVo;
}

export interface TableNameListVo {
  tables?: string[];
}

export type FlushRequest = object;

export interface InitUploadBlobRequest {
  contentMd5: string;
  /** @format int64 */
  contentLength: number;
}

export interface InitUploadBlobResult {
  status?: "OK" | "EXISTED";
  blobId?: string;
  signedUrl?: string;
}

export interface ResponseMessageInitUploadBlobResult {
  code: string;
  message: string;
  data: InitUploadBlobResult;
}

export interface CompleteUploadBlobResult {
  blobId?: string;
}

export interface ResponseMessageCompleteUploadBlobResult {
  code: string;
  message: string;
  data: CompleteUploadBlobResult;
}

export interface PageInfoUserVo {
  /** @format int64 */
  total?: number;
  list?: UserVo[];
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

export interface ResponseMessagePageInfoUserVo {
  code: string;
  message: string;
  data: PageInfoUserVo;
}

/**
 * User
 * User object
 */
export interface UserVo {
  id: string;
  name: string;
  /** @format int64 */
  createdTime: number;
  isEnabled: boolean;
  systemRole?: string;
  projectRoles?: Record<string, string>;
}

export interface ResponseMessageUserVo {
  code: string;
  message: string;
  /** User object */
  data: UserVo;
}

/**
 * Role
 * Project Role object
 */
export interface ProjectMemberVo {
  id: string;
  /** User object */
  user: UserVo;
  /** Project object */
  project: ProjectVo;
  /** Role object */
  role: RoleVo;
}

/**
 * Project
 * Project object
 */
export interface ProjectVo {
  id: string;
  name: string;
  description?: string;
  privacy: string;
  /** @format int64 */
  createdTime: number;
  /** User object */
  owner: UserVo;
  statistics?: StatisticsVo;
}

export interface ResponseMessageListProjectMemberVo {
  code: string;
  message: string;
  data: ProjectMemberVo[];
}

/**
 * Role
 * Role object
 */
export interface RoleVo {
  id: string;
  name: string;
  code: string;
  description?: string;
}

export interface StatisticsVo {
  /** @format int32 */
  modelCounts: number;
  /** @format int32 */
  datasetCounts: number;
  /** @format int32 */
  runtimeCounts: number;
  /** @format int32 */
  memberCounts: number;
  /** @format int32 */
  evaluationCounts: number;
}

export interface ResponseMessageSystemVersionVo {
  code: string;
  message: string;
  /** System version */
  data: SystemVersionVo;
}

/**
 * Version
 * System version
 */
export interface SystemVersionVo {
  id?: string;
  version?: string;
}

export interface ResponseMessageListResourcePool {
  code: string;
  message: string;
  data: ResourcePool[];
}

/**
 * Features
 * System features
 */
export interface FeaturesVo {
  disabled: string[];
}

export interface ResponseMessageFeaturesVo {
  code: string;
  message: string;
  /** System features */
  data: FeaturesVo;
}

/**
 * Device
 * Device information
 */
export interface DeviceVo {
  name: string;
}

export interface ResponseMessageListDeviceVo {
  code: string;
  message: string;
  data: DeviceVo[];
}

export interface ResponseMessageListRoleVo {
  code: string;
  message: string;
  data: RoleVo[];
}

/**
 * Report
 * Report object
 */
export interface ReportVo {
  /** @format int64 */
  id: number;
  uuid: string;
  title: string;
  content?: string;
  description?: string;
  shared?: boolean;
  /** User object */
  owner: UserVo;
  /** @format int64 */
  createdTime: number;
  /** @format int64 */
  modifiedTime: number;
}

export interface ResponseMessageReportVo {
  code: string;
  message: string;
  /** Report object */
  data: ReportVo;
}

export interface PageInfoProjectVo {
  /** @format int64 */
  total?: number;
  list?: ProjectVo[];
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

export interface ResponseMessagePageInfoProjectVo {
  code: string;
  message: string;
  data: PageInfoProjectVo;
}

export interface FileNode {
  name?: string;
  signature?: string;
  flag?: "added" | "updated" | "deleted" | "unchanged";
  mime?: string;
  type?: "directory" | "file";
  desc?: string;
  size?: string;
}

export interface ListFilesResult {
  files?: FileNode[];
}

export interface ResponseMessageListFilesResult {
  code: string;
  message: string;
  data: ListFilesResult;
}

export interface ResponseMessageProjectVo {
  code: string;
  message: string;
  /** Project object */
  data: ProjectVo;
}

export interface PageInfoTrashVo {
  /** @format int64 */
  total?: number;
  list?: TrashVo[];
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

export interface ResponseMessagePageInfoTrashVo {
  code: string;
  message: string;
  data: PageInfoTrashVo;
}

export interface TrashVo {
  id?: string;
  name?: string;
  type?: string;
  /** @format int64 */
  trashedTime?: number;
  /** @format int64 */
  size?: number;
  trashedBy?: string;
  /** @format int64 */
  lastUpdatedTime?: number;
  /** @format int64 */
  retentionTime?: number;
}

export interface JobTemplateVo {
  /** @format int64 */
  id?: number;
  name?: string;
  /** @format int64 */
  jobId?: number;
}

export interface ResponseMessageListJobTemplateVo {
  code: string;
  message: string;
  data: JobTemplateVo[];
}

export interface ResponseMessageJobTemplateVo {
  code: string;
  message: string;
  data: JobTemplateVo;
}

export interface PageInfoRuntimeVo {
  /** @format int64 */
  total?: number;
  list?: RuntimeVo[];
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

export interface ResponseMessagePageInfoRuntimeVo {
  code: string;
  message: string;
  data: PageInfoRuntimeVo;
}

/**
 * RuntimeVersion
 * Runtime version object
 */
export interface RuntimeVersionVo {
  tags?: string[];
  latest: boolean;
  id: string;
  runtimeId: string;
  name: string;
  alias: string;
  meta?: string;
  image: string;
  builtImage?: string;
  /** @format int64 */
  createdTime: number;
  /** User object */
  owner?: UserVo;
  shared: boolean;
}

/**
 * Runtime
 * Runtime object
 */
export interface RuntimeVo {
  id: string;
  name: string;
  /** @format int64 */
  createdTime: number;
  /** User object */
  owner: UserVo;
  /** Runtime version object */
  version: RuntimeVersionVo;
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
  code: string;
  message: string;
  /** Runtime information object */
  data: RuntimeInfoVo;
}

/**
 * RuntimeInfo
 * Runtime information object
 */
export interface RuntimeInfoVo {
  /** Runtime version object */
  versionInfo: RuntimeVersionVo;
  id: string;
  name: string;
  versionId: string;
  versionName: string;
  versionAlias: string;
  versionTag?: string;
  versionMeta?: string;
  manifest: string;
  /** @format int32 */
  shared: number;
  /** @format int64 */
  createdTime: number;
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
  code: string;
  message: string;
  data: PageInfoRuntimeVersionVo;
}

export interface ResponseMessageListString {
  code: string;
  message: string;
  data: string[];
}

export interface ResponseMessageLong {
  code: string;
  message: string;
  /** @format int64 */
  data: number;
}

export interface ResponseMessageListRuntimeViewVo {
  code: string;
  message: string;
  data: RuntimeViewVo[];
}

/**
 * Runtime
 * Runtime Version View object
 */
export interface RuntimeVersionViewVo {
  id: string;
  versionName: string;
  alias: string;
  latest: boolean;
  /** @format int32 */
  shared: number;
  /** @format int64 */
  createdTime: number;
}

/**
 * Runtime
 * Runtime View object
 */
export interface RuntimeViewVo {
  ownerName: string;
  projectName: string;
  runtimeId: string;
  runtimeName: string;
  shared: boolean;
  versions: RuntimeVersionViewVo[];
}

export interface PageInfoReportVo {
  /** @format int64 */
  total?: number;
  list?: ReportVo[];
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

export interface ResponseMessagePageInfoReportVo {
  code: string;
  message: string;
  data: PageInfoReportVo;
}

export interface ContainerSpec {
  image?: string;
  cmds?: string[];
  entrypoint?: string[];
}

export interface Env {
  name?: string;
  value?: string;
}

/**
 * Model
 * Model Version View Object
 */
export interface ModelVersionViewVo {
  id: string;
  versionName: string;
  alias: string;
  latest: boolean;
  tags?: string[];
  /** @format int32 */
  shared: number;
  stepSpecs: StepSpec[];
  builtInRuntime?: string;
  /** @format int64 */
  createdTime: number;
}

/**
 * Model View
 * Model View Object
 */
export interface ModelViewVo {
  ownerName: string;
  projectName: string;
  modelId: string;
  modelName: string;
  shared: boolean;
  versions: ModelVersionViewVo[];
}

export interface ParameterSignature {
  name: string;
  required?: boolean;
  multiple?: boolean;
}

export interface ResponseMessageListModelViewVo {
  code: string;
  message: string;
  data: ModelViewVo[];
}

export interface RuntimeResource {
  type?: string;
  /** @format float */
  request?: number;
  /** @format float */
  limit?: number;
}

export interface StepSpec {
  name: string;
  /** @format int32 */
  concurrency?: number;
  /** @format int32 */
  replicas: number;
  /** @format int32 */
  backoffLimit?: number;
  needs?: string[];
  resources?: RuntimeResource[];
  env?: Env[];
  /** @format int32 */
  expose?: number;
  virtual?: boolean;
  job_name?: string;
  show_name: string;
  require_dataset?: boolean;
  container_spec?: ContainerSpec;
  ext_cmd_args?: string;
  parameters_sig?: ParameterSignature[];
}

/**
 * Dataset
 * Dataset Version View object
 */
export interface DatasetVersionViewVo {
  id: string;
  versionName: string;
  alias?: string;
  latest: boolean;
  /** @format int32 */
  shared: number;
  /** @format int64 */
  createdTime: number;
}

/**
 * Dataset
 * Dataset View object
 */
export interface DatasetViewVo {
  ownerName: string;
  projectName: string;
  datasetId: string;
  datasetName: string;
  shared: boolean;
  versions: DatasetVersionViewVo[];
}

export interface ResponseMessageListDatasetViewVo {
  code: string;
  message: string;
  data: DatasetViewVo[];
}

/**
 * ModelVersion
 * Model version object
 */
export interface ModelVersionVo {
  latest: boolean;
  tags?: string[];
  stepSpecs: StepSpec[];
  id: string;
  name: string;
  alias: string;
  /** @format int64 */
  size?: number;
  /** @format int64 */
  createdTime: number;
  /** User object */
  owner?: UserVo;
  shared: boolean;
  builtInRuntime?: string;
}

/**
 * Model
 * Model object
 */
export interface ModelVo {
  id: string;
  name: string;
  /** @format int64 */
  createdTime: number;
  /** User object */
  owner: UserVo;
  /** Model version object */
  version: ModelVersionVo;
}

export interface PageInfoModelVo {
  /** @format int64 */
  total?: number;
  list?: ModelVo[];
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

export interface ResponseMessagePageInfoModelVo {
  code: string;
  message: string;
  data: PageInfoModelVo;
}

/**
 * ModelInfo
 * Model information object
 */
export interface ModelInfoVo {
  /** Model version object */
  versionInfo: ModelVersionVo;
  id: string;
  name: string;
  versionAlias: string;
  versionId: string;
  versionName: string;
  versionTag?: string;
  /** @format int64 */
  createdTime: number;
  /** @format int32 */
  shared: number;
}

export interface ResponseMessageModelInfoVo {
  code: string;
  message: string;
  /** Model information object */
  data: ModelInfoVo;
}

export interface PageInfoModelVersionVo {
  /** @format int64 */
  total?: number;
  list?: ModelVersionVo[];
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

export interface ResponseMessagePageInfoModelVersionVo {
  code: string;
  message: string;
  data: PageInfoModelVersionVo;
}

export interface ResponseMessageMapStringListFileNode {
  code: string;
  message: string;
  data: Record<string, FileNode[]>;
}

/**
 * DatasetVersion
 * Dataset version object
 */
export interface DatasetVersionVo {
  tags?: string[];
  latest: boolean;
  indexTable?: string;
  id: string;
  name: string;
  alias?: string;
  meta?: string;
  /** @format int64 */
  createdTime: number;
  /** User object */
  owner?: UserVo;
  shared: boolean;
}

/**
 * Dataset
 * Dataset object
 */
export interface DatasetVo {
  id: string;
  name: string;
  /** @format int64 */
  createdTime: number;
  /** User object */
  owner?: UserVo;
  /** Dataset version object */
  version: DatasetVersionVo;
}

export interface ExposedLinkVo {
  type: "DEV_MODE" | "WEB_HANDLER";
  name: string;
  link: string;
}

/**
 * Job
 * Job object
 */
export interface JobVo {
  exposedLinks: ExposedLinkVo[];
  id: string;
  uuid: string;
  modelName: string;
  modelVersion: string;
  /** Model object */
  model: ModelVo;
  jobName?: string;
  datasets?: string[];
  datasetList?: DatasetVo[];
  /** Runtime object */
  runtime: RuntimeVo;
  isBuiltinRuntime?: boolean;
  device?: string;
  /** @format int32 */
  deviceAmount?: number;
  /** User object */
  owner: UserVo;
  /** @format int64 */
  createdTime: number;
  /** @format int64 */
  stopTime?: number;
  jobStatus: "CREATED" | "READY" | "PAUSED" | "RUNNING" | "CANCELLING" | "CANCELED" | "SUCCESS" | "FAIL" | "UNKNOWN";
  comment?: string;
  stepSpec?: string;
  resourcePool: string;
  /** @format int64 */
  duration?: number;
  /** @format int64 */
  pinnedTime?: number;
}

export interface PageInfoJobVo {
  /** @format int64 */
  total?: number;
  list?: JobVo[];
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

export interface ResponseMessagePageInfoJobVo {
  code: string;
  message: string;
  data: PageInfoJobVo;
}

export interface ResponseMessageJobVo {
  code: string;
  message: string;
  /** Job object */
  data: JobVo;
}

export interface PageInfoTaskVo {
  /** @format int64 */
  total?: number;
  list?: TaskVo[];
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

export interface ResponseMessagePageInfoTaskVo {
  code: string;
  message: string;
  data: PageInfoTaskVo;
}

export interface RunVo {
  /** @format int64 */
  id?: number;
  /** @format int64 */
  taskId?: number;
  status?: "PENDING" | "RUNNING" | "FINISHED" | "FAILED";
  ip?: string;
  /** @format int64 */
  startTime?: number;
  /** @format int64 */
  finishTime?: number;
  failedReason?: string;
}

/**
 * Task
 * Task object
 */
export interface TaskVo {
  id: string;
  uuid: string;
  /** @format int64 */
  startedTime?: number;
  /** @format int64 */
  finishedTime?: number;
  taskStatus:
    | "CREATED"
    | "READY"
    | "ASSIGNING"
    | "PAUSED"
    | "PREPARING"
    | "RUNNING"
    | "RETRYING"
    | "SUCCESS"
    | "CANCELLING"
    | "CANCELED"
    | "FAIL"
    | "UNKNOWN";
  /** @format int32 */
  retryNum?: number;
  resourcePool: string;
  stepName: string;
  exposedLinks?: ExposedLinkVo[];
  failedReason?: string;
  runs?: RunVo[];
}

export interface ResponseMessageTaskVo {
  code: string;
  message: string;
  /** Task object */
  data: TaskVo;
}

export interface ResponseMessageListRunVo {
  code: string;
  message: string;
  data: RunVo[];
}

export interface EventVo {
  eventType: "INFO" | "WARNING" | "ERROR";
  source: "CLIENT" | "SERVER" | "NODE";
  relatedResource: RelatedResource;
  message: string;
  data?: string;
  /** @format int64 */
  timestamp?: number;
  /** @format int64 */
  id?: number;
}

export interface ResponseMessageListEventVo {
  code: string;
  message: string;
  data: EventVo[];
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

export interface ResponseMessageGraph {
  code: string;
  message: string;
  data: Graph;
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
  code: string;
  message: string;
  data: PageInfoSummaryVo;
}

/**
 * Evaluation
 * Evaluation Summary object
 */
export interface SummaryVo {
  id: string;
  uuid: string;
  projectId: string;
  projectName: string;
  modelName: string;
  modelVersion: string;
  datasets?: string;
  runtime: string;
  device?: string;
  /** @format int32 */
  deviceAmount?: number;
  /** @format int64 */
  createdTime: number;
  /** @format int64 */
  stopTime?: number;
  owner: string;
  /** @format int64 */
  duration?: number;
  jobStatus: "CREATED" | "READY" | "PAUSED" | "RUNNING" | "CANCELLING" | "CANCELED" | "SUCCESS" | "FAIL" | "UNKNOWN";
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
  code: string;
  message: string;
  /** Evaluation View Config object */
  data: ConfigVo;
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
  code: string;
  message: string;
  data: AttributeVo[];
}

export interface PageInfoDatasetVo {
  /** @format int64 */
  total?: number;
  list?: DatasetVo[];
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

export interface ResponseMessagePageInfoDatasetVo {
  code: string;
  message: string;
  data: PageInfoDatasetVo;
}

/**
 * DatasetInfo
 * SWDataset information object
 */
export interface DatasetInfoVo {
  indexTable?: string;
  /** Dataset version object */
  versionInfo?: DatasetVersionVo;
  id: string;
  name: string;
  versionId: string;
  versionName: string;
  versionAlias?: string;
  versionTag?: string;
  /** @format int32 */
  shared: number;
  /** @format int64 */
  createdTime: number;
  files?: FlattenFileVo[];
  versionMeta: string;
}

export interface ResponseMessageDatasetInfoVo {
  code: string;
  message: string;
  /** SWDataset information object */
  data: DatasetInfoVo;
}

export interface PageInfoDatasetVersionVo {
  /** @format int64 */
  total?: number;
  list?: DatasetVersionVo[];
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

export interface ResponseMessagePageInfoDatasetVersionVo {
  code: string;
  message: string;
  data: PageInfoDatasetVersionVo;
}

export interface BuildRecordVo {
  id: string;
  projectId: string;
  taskId: string;
  datasetName: string;
  status:
    | "CREATED"
    | "READY"
    | "ASSIGNING"
    | "PAUSED"
    | "PREPARING"
    | "RUNNING"
    | "RETRYING"
    | "SUCCESS"
    | "CANCELLING"
    | "CANCELED"
    | "FAIL"
    | "UNKNOWN";
  type: "IMAGE" | "VIDEO" | "AUDIO";
  /** @format int64 */
  createTime: number;
}

export interface PageInfoBuildRecordVo {
  /** @format int64 */
  total?: number;
  list?: BuildRecordVo[];
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

export interface ResponseMessagePageInfoBuildRecordVo {
  code: string;
  message: string;
  data: PageInfoBuildRecordVo;
}

export interface PageInfoSftSpaceVo {
  /** @format int64 */
  total?: number;
  list?: SftSpaceVo[];
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

export interface ResponseMessagePageInfoSftSpaceVo {
  code: string;
  message: string;
  data: PageInfoSftSpaceVo;
}

export interface SftSpaceVo {
  /** @format int64 */
  id?: number;
  name?: string;
  description?: string;
  /** @format int64 */
  createdTime: number;
  /** User object */
  owner: UserVo;
}

/**
 * Model Serving Status
 * Model Serving Status object
 */
export interface ModelServingStatusVo {
  /** @format int32 */
  progress?: number;
  events?: string;
}

export interface ResponseMessageModelServingStatusVo {
  code: string;
  message: string;
  /** Model Serving Status object */
  data: ModelServingStatusVo;
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
  id: string;
  name: string;
  version: string;
}

export interface ResponseMessagePageInfoPanelPluginVo {
  code: string;
  message: string;
  data: PageInfoPanelPluginVo;
}

export interface ResponseMessageRuntimeSuggestionVo {
  code: string;
  message: string;
  /** Model Serving object */
  data: RuntimeSuggestionVo;
}

/**
 * Model Serving
 * Model Serving object
 */
export interface RuntimeSuggestionVo {
  runtimes?: RuntimeVersionVo[];
}

export interface UserRoleDeleteRequest {
  currentUserPwd: string;
}

export interface FileDeleteRequest {
  pathPrefix: string;
  /** @uniqueItems true */
  files: string[];
}

export type UpdateUserStateData = ResponseMessageString["data"];

export type UpdateUserPwdData = ResponseMessageString["data"];

export type UpdateCurrentUserPasswordData = ResponseMessageString["data"];

export type CheckCurrentUserPasswordData = ResponseMessageString["data"];

export type UpdateUserSystemRoleData = ResponseMessageString["data"];

export type DeleteUserSystemRoleData = ResponseMessageString["data"];

export type GetProjectByUrlData = ResponseMessageProjectVo["data"];

export type UpdateProjectData = ResponseMessageString["data"];

export type DeleteProjectByUrlData = ResponseMessageString["data"];

export type RecoverTrashData = ResponseMessageString["data"];

export type DeleteTrashData = ResponseMessageString["data"];

export type UpdateRuntimeData = ResponseMessageObject["data"];

export type ModifyRuntimeData = ResponseMessageString["data"];

export type ShareRuntimeVersionData = ResponseMessageString["data"];

export type RecoverRuntimeData = ResponseMessageString["data"];

export type ModifyProjectRoleData = ResponseMessageString["data"];

export type DeleteProjectRoleData = ResponseMessageString["data"];

export type GetReportData = ResponseMessageReportVo["data"];

export type ModifyReportData = ResponseMessageString["data"];

export type DeleteReportData = ResponseMessageString["data"];

export type SharedReportData = ResponseMessageString["data"];

export type ModifyModelData = ResponseMessageString["data"];

export type HeadModelData = object;

export type ShareModelVersionData = ResponseMessageString["data"];

export type RecoverModelData = ResponseMessageString["data"];

export type FindJobData = ResponseMessageJobVo["data"];

export type ModifyJobCommentData = ResponseMessageString["data"];

export type RemoveJobData = ResponseMessageString["data"];

export type ShareDatasetVersionData = ResponseMessageString["data"];

export type RecoverDatasetData = ResponseMessageString["data"];

export type RecoverProjectData = ResponseMessageString["data"];

export type ApplySignedGetUrlsData = ResponseMessageSignedUrlResponse["data"];

export type ApplySignedPutUrlsData = ResponseMessageSignedUrlResponse["data"];

export type ListUserData = ResponseMessagePageInfoUserVo["data"];

export type CreateUserData = ResponseMessageString["data"];

export type InstanceStatusData = ResponseMessageString["data"];

export type QuerySettingData = ResponseMessageString["data"];

export type UpdateSettingData = ResponseMessageString["data"];

export type ListResourcePoolsData = ResponseMessageListResourcePool["data"];

export type UpdateResourcePoolsData = ResponseMessageString["data"];

export type ListSystemRolesData = ResponseMessageListProjectMemberVo["data"];

export type AddUserSystemRoleData = ResponseMessageString["data"];

export type ListProjectData = ResponseMessagePageInfoProjectVo["data"];

export type CreateProjectData = ResponseMessageString["data"];

export type CreateModelVersionData = any;

export type SelectAllInProjectData = ResponseMessageListJobTemplateVo["data"];

export type AddTemplateData = ResponseMessageString["data"];

export type CreateModelServingData = ResponseMessageModelServingVo["data"];

export type ListRuntimeVersionTagsData = ResponseMessageListString["data"];

export type AddRuntimeVersionTagData = ResponseMessageString["data"];

export type BuildRuntimeImageData = ResponseMessageBuildImageResult["data"];

export type RevertRuntimeVersionData = ResponseMessageString["data"];

export type UploadData = ResponseMessageString["data"];

export type ListProjectRoleData = ResponseMessageListProjectMemberVo["data"];

export type AddProjectRoleData = ResponseMessageString["data"];

export type ListReportsData = ResponseMessagePageInfoReportVo["data"];

export type CreateReportData = ResponseMessageString["data"];

export type TransferData = ResponseMessageString["data"];

export type ListModelVersionTagsData = ResponseMessageListString["data"];

export type AddModelVersionTagData = ResponseMessageString["data"];

export type RevertModelVersionData = ResponseMessageString["data"];

export type ListJobsData = ResponseMessagePageInfoJobVo["data"];

export type CreateJobData = ResponseMessageString["data"];

export type ActionData = ResponseMessageString["data"];

export type ExecData = ResponseMessageExecResponse["data"];

export type RecoverJobData = ResponseMessageString["data"];

export type ModifyJobPinStatusData = ResponseMessageString["data"];

export type GetEventsData = ResponseMessageListEventVo["data"];

export type AddEventData = ResponseMessageString["data"];

export type SignLinksData = ResponseMessageMapStringString["data"];

export type GetHashedBlobData = any;

export type UploadHashedBlobData = ResponseMessageString["data"];

export type HeadHashedBlobData = object;

export type GetViewConfigData = ResponseMessageConfigVo["data"];

export type CreateViewConfigData = ResponseMessageString["data"];

export type ListDatasetVersionTagsData = ResponseMessageListString["data"];

export type AddDatasetVersionTagData = ResponseMessageString["data"];

export type ConsumeNextDataData = NullableResponseMessageDataIndexDesc;

export type RevertDatasetVersionData = ResponseMessageString["data"];

export type UploadDsData = ResponseMessageUploadResult["data"];

export type BuildDatasetData = ResponseMessageString["data"];

export type SignLinks1Data = ResponseMessageMapObjectObject["data"];

export type GetHashedBlob1Data = any;

export type UploadHashedBlob1Data = ResponseMessageString["data"];

export type HeadHashedBlob1Data = object;

export type ListSftSpaceData = ResponseMessagePageInfoSftSpaceVo["data"];

export type CreateSftSpaceData = ResponseMessageString["data"];

export type ListSftData = ResponseMessagePageInfoSftVo["data"];

export type CreateSftData = ResponseMessageString["data"];

export type GetPanelSettingData = ResponseMessageString["data"];

export type SetPanelSettingData = ResponseMessageString["data"];

export type PluginListData = ResponseMessagePageInfoPanelPluginVo["data"];

export type InstallPluginData = ResponseMessageString["data"];

export type SignLinks2Data = ResponseMessageMapStringString["data"];

export type UpdateTableData = ResponseMessageString["data"];

export type ScanTableData = ResponseMessageRecordListVo["data"];

export type ScanAndExportData = any;

export type QueryTableData = ResponseMessageRecordListVo["data"];

export type QueryAndExportData = any;

export type ListTablesData = ResponseMessageTableNameListVo["data"];

export type FlushData = ResponseMessageString["data"];

export type InitUploadBlobData = ResponseMessageInitUploadBlobResult["data"];

export type CompleteUploadBlobData = ResponseMessageCompleteUploadBlobResult["data"];

export type HeadRuntimeData = object;

export type HeadDatasetData = object;

export type GetUserByIdData = ResponseMessageUserVo["data"];

export type UserTokenData = ResponseMessageString["data"];

export type GetCurrentUserData = ResponseMessageUserVo["data"];

export type GetCurrentUserRolesData = ResponseMessageListProjectMemberVo["data"];

export type GetCurrentVersionData = ResponseMessageSystemVersionVo["data"];

export type QueryFeaturesData = ResponseMessageFeaturesVo["data"];

export type ListDeviceData = ResponseMessageListDeviceVo["data"];

export type ListRolesData = ResponseMessageListRoleVo["data"];

export type PreviewData = ResponseMessageReportVo["data"];

export type GetModelMetaBlobData = ResponseMessageString["data"];

export type ListFilesData = ResponseMessageListFilesResult["data"];

/** @format binary */
export type GetFileDataData = File;

export type ListTrashData = ResponseMessagePageInfoTrashVo["data"];

export type GetTemplateData = ResponseMessageJobTemplateVo["data"];

export type DeleteTemplateData = ResponseMessageString["data"];

export type ListRuntimeData = ResponseMessagePageInfoRuntimeVo["data"];

export type GetRuntimeInfoData = ResponseMessageRuntimeInfoVo["data"];

export type DeleteRuntimeData = ResponseMessageString["data"];

export type ListRuntimeVersionData = ResponseMessagePageInfoRuntimeVersionVo["data"];

export type PullData = any;

export type GetRuntimeVersionTagData = ResponseMessageLong["data"];

export type ListRuntimeTreeData = ResponseMessageListRuntimeViewVo["data"];

export type SelectRecentlyInProjectData = ResponseMessageListJobTemplateVo["data"];

export type RecentRuntimeTreeData = ResponseMessageListRuntimeViewVo["data"];

export type RecentModelTreeData = ResponseMessageListModelViewVo["data"];

export type RecentDatasetTreeData = ResponseMessageListDatasetViewVo["data"];

export type GetProjectReadmeByUrlData = ResponseMessageString["data"];

export type ListModelData = ResponseMessagePageInfoModelVo["data"];

export type GetModelInfoData = ResponseMessageModelInfoVo["data"];

export type DeleteModelData = ResponseMessageString["data"];

export type ListModelVersionData = ResponseMessagePageInfoModelVersionVo["data"];

export type GetModelVersionTagData = ResponseMessageLong["data"];

export type GetModelDiffData = ResponseMessageMapStringListFileNode["data"];

export type ListModelTreeData = ResponseMessageListModelViewVo["data"];

export type ListTasksData = ResponseMessagePageInfoTaskVo["data"];

export type GetTaskData = ResponseMessageTaskVo["data"];

export type GetRunsData = ResponseMessageListRunVo["data"];

export type GetJobDagData = ResponseMessageGraph["data"];

export type ListEvaluationSummaryData = ResponseMessagePageInfoSummaryVo["data"];

export type ListAttributesData = ResponseMessageListAttributeVo["data"];

export type ListDatasetData = ResponseMessagePageInfoDatasetVo["data"];

export type GetDatasetInfoData = ResponseMessageDatasetInfoVo["data"];

export type DeleteDatasetData = ResponseMessageString["data"];

export type ListDatasetVersionData = ResponseMessagePageInfoDatasetVersionVo["data"];

export type PullDsData = any;

export type GetDatasetVersionTagData = ResponseMessageLong["data"];

export type ListBuildRecordsData = ResponseMessagePageInfoBuildRecordVo["data"];

export type ListDatasetTreeData = ResponseMessageListDatasetViewVo["data"];

export type PullUriContentData = any;

export type GetModelServingStatusData = ResponseMessageModelServingStatusVo["data"];

export type OfflineLogsData = ResponseMessageListString["data"];

export type LogContentData = string;

export type GetRuntimeSuggestionData = ResponseMessageRuntimeSuggestionVo["data"];

export type ApplyPathPrefixData = ResponseMessageString["data"];

export type PullUriContent1Data = any;

export type DeletePathData = ResponseMessageString["data"];

export type DeleteRuntimeVersionTagData = ResponseMessageString["data"];

export type DeleteModelVersionTagData = ResponseMessageString["data"];

export type DeleteDatasetVersionTagData = ResponseMessageString["data"];

export type UninstallPluginData = ResponseMessageString["data"];
