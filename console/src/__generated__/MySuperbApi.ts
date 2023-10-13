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
  title?: string;
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

/**
 * user defined running configurations such environment variables
 */
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
  title: string;
  description?: string;
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
  timeToLiveInSec?: number;
  modelVersionUrl: string;
  datasetVersionUrls?: string;
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
  source: "CLIENT" | "SERVER";
  message: string;
  data?: string;
  /** @format int64 */
  timestamp?: number;
  relatedResource: RelatedResource;
}

export interface RelatedResource {
  resource: "JOB" | "TASK";
  /** @format int64 */
  id: number;
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
  startInclusive?: boolean;
  end?: string;
  endInclusive?: boolean;
  processedData?: DataIndexDesc[];
  /** @deprecated */
  serial?: boolean;
}

export interface DataIndexDesc {
  start?: string;
  startType?: string;
  end?: string;
  endType?: string;
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
}

export interface ResponseMessageTaskVo {
  code: string;
  message: string;
  /** Task object */
  data: TaskVo;
}

export interface EventVo {
  eventType: "INFO" | "WARNING" | "ERROR";
  source: "CLIENT" | "SERVER";
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
  files: string[];
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
    this.instance = axios.create({
      ...axiosConfig,
      baseURL: axiosConfig.baseURL || "http://e2e-20231007.pre.intra.starwhale.ai",
    });
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
 * @baseUrl http://e2e-20231007.pre.intra.starwhale.ai
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
     * @response `200` `ResponseMessageString` OK
     */
    updateUserState: (userId: string, data: UserUpdateStateRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/user/${userId}/state`,
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
     * @tags User
     * @name UpdateUserPwd
     * @summary Change user password
     * @request PUT:/api/v1/user/{userId}/pwd
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    updateUserPwd: (userId: string, data: UserUpdatePasswordRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/user/${userId}/pwd`,
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
     * @tags User
     * @name UpdateCurrentUserPassword
     * @summary Update Current User password
     * @request PUT:/api/v1/user/current/pwd
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    updateCurrentUserPassword: (data: UserUpdatePasswordRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/user/current/pwd`,
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
     * @tags User
     * @name CheckCurrentUserPassword
     * @summary Check Current User password
     * @request POST:/api/v1/user/current/pwd
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    checkCurrentUserPassword: (data: UserCheckPasswordRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/user/current/pwd`,
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
     * @name UpdateUserSystemRole
     * @summary Update user role of system
     * @request PUT:/api/v1/role/{systemRoleId}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    updateUserSystemRole: (systemRoleId: string, data: UserRoleUpdateRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/role/${systemRoleId}`,
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
     * @tags User
     * @name DeleteUserSystemRole
     * @summary Delete user role of system
     * @request DELETE:/api/v1/role/{systemRoleId}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    deleteUserSystemRole: (systemRoleId: string, data: UserRoleDeleteRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/role/${systemRoleId}`,
        method: "DELETE",
        body: data,
        secure: true,
        type: ContentType.Json,
        format: "json",
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
     * @response `200` `ResponseMessageProjectVo` OK
     */
    getProjectByUrl: (projectUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageProjectVo, any>({
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
     * @response `200` `ResponseMessageString` OK
     */
    updateProject: (projectUrl: string, data: UpdateProjectRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}`,
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
     * @tags Project
     * @name DeleteProjectByUrl
     * @summary Delete a project by Url
     * @request DELETE:/api/v1/project/{projectUrl}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    deleteProjectByUrl: (projectUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Restore a trash to its original type and move it out of the recycle bin.
     *
     * @tags Trash
     * @name RecoverTrash
     * @summary Restore trash by id.
     * @request PUT:/api/v1/project/{projectUrl}/trash/{trashId}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    recoverTrash: (projectUrl: string, trashId: number, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/trash/${trashId}`,
        method: "PUT",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Move a trash out of the recycle bin. This operation cannot be resumed.
     *
     * @tags Trash
     * @name DeleteTrash
     * @summary Delete trash by id.
     * @request DELETE:/api/v1/project/{projectUrl}/trash/{trashId}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    deleteTrash: (projectUrl: string, trashId: number, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/trash/${trashId}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description update image for runtime
     *
     * @tags Runtime
     * @name UpdateRuntime
     * @summary update image for runtime
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/image
     * @secure
     * @response `200` `ResponseMessageObject` OK
     */
    updateRuntime: (
      projectUrl: string,
      runtimeUrl: string,
      versionUrl: string,
      data: string,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageObject, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/image`,
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
     * @tags Runtime
     * @name ModifyRuntime
     * @summary Set tag of the runtime version
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}
     * @secure
     * @response `200` `ResponseMessageString` OK
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
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Runtime
     * @name ShareRuntimeVersion
     * @summary Share or unshare the runtime version
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}/shared
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    shareRuntimeVersion: (
      projectUrl: string,
      runtimeUrl: string,
      runtimeVersionUrl: string,
      query: {
        /** 1 or true - shared, 0 or false - unshared */
        shared: boolean;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${runtimeVersionUrl}/shared`,
        method: "PUT",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Runtime
     * @name RecoverRuntime
     * @summary Recover a runtime
     * @request PUT:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/recover
     * @secure
     * @response `200` `ResponseMessageString` OK
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
     * @response `200` `ResponseMessageString` OK
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
        format: "json",
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
     * @response `200` `ResponseMessageString` OK
     */
    deleteProjectRole: (projectUrl: string, projectRoleId: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/role/${projectRoleId}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Report
     * @name GetReport
     * @request GET:/api/v1/project/{projectUrl}/report/{reportId}
     * @secure
     * @response `200` `ResponseMessageReportVo` OK
     */
    getReport: (projectUrl: string, reportId: number, params: RequestParams = {}) =>
      this.http.request<ResponseMessageReportVo, any>({
        path: `/api/v1/project/${projectUrl}/report/${reportId}`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Report
     * @name ModifyReport
     * @request PUT:/api/v1/project/{projectUrl}/report/{reportId}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    modifyReport: (projectUrl: string, reportId: number, data: UpdateReportRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/report/${reportId}`,
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
     * @tags Report
     * @name DeleteReport
     * @request DELETE:/api/v1/project/{projectUrl}/report/{reportId}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    deleteReport: (projectUrl: string, reportId: number, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/report/${reportId}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Report
     * @name SharedReport
     * @request PUT:/api/v1/project/{projectUrl}/report/{reportId}/shared
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    sharedReport: (
      projectUrl: string,
      reportId: number,
      query: {
        shared: boolean;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/report/${reportId}/shared`,
        method: "PUT",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name ModifyModel
     * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    modifyModel: (
      projectUrl: string,
      modelUrl: string,
      versionUrl: string,
      data: ModelUpdateRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}`,
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
     * @tags Model
     * @name HeadModel
     * @request HEAD:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}
     * @secure
     * @response `200` `FlushRequest` OK
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
     * @name ShareModelVersion
     * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/shared
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    shareModelVersion: (
      projectUrl: string,
      modelUrl: string,
      versionUrl: string,
      query: {
        shared: boolean;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/shared`,
        method: "PUT",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name RecoverModel
     * @request PUT:/api/v1/project/{projectUrl}/model/{modelUrl}/recover
     * @secure
     * @response `200` `ResponseMessageString` OK
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
     * @response `200` `ResponseMessageJobVo` OK
     */
    findJob: (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageJobVo, any>({
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
     * @response `200` `ResponseMessageString` OK
     */
    modifyJobComment: (projectUrl: string, jobUrl: string, data: JobModifyRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
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
     * @response `200` `ResponseMessageString` OK
     */
    removeJob: (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Dataset
     * @name ShareDatasetVersion
     * @summary Share or unshare the dataset version
     * @request PUT:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/shared
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    shareDatasetVersion: (
      projectUrl: string,
      datasetUrl: string,
      versionUrl: string,
      query: {
        /** 1 or true - shared, 0 or false - unshared */
        shared: boolean;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/shared`,
        method: "PUT",
        query: query,
        secure: true,
        format: "json",
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
     * @response `200` `ResponseMessageString` OK
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
     * @response `200` `ResponseMessageString` OK
     */
    recoverProject: (projectId: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectId}/recover`,
        method: "PUT",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Apply signedUrls for get
     *
     * @tags File storage
     * @name ApplySignedGetUrls
     * @summary Apply signedUrls for get
     * @request GET:/api/v1/filestorage/signedurl
     * @secure
     * @response `200` `ResponseMessageSignedUrlResponse` OK
     */
    applySignedGetUrls: (
      query: {
        pathPrefix: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageSignedUrlResponse, any>({
        path: `/api/v1/filestorage/signedurl`,
        method: "GET",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * @description Apply signedUrls for put
     *
     * @tags File storage
     * @name ApplySignedPutUrls
     * @summary Apply signedUrls for put
     * @request PUT:/api/v1/filestorage/signedurl
     * @secure
     * @response `200` `ResponseMessageSignedUrlResponse` OK
     */
    applySignedPutUrls: (data: ApplySignedUrlRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageSignedUrlResponse, any>({
        path: `/api/v1/filestorage/signedurl`,
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
     * @name ListUser
     * @summary Get the list of users
     * @request GET:/api/v1/user
     * @secure
     * @response `200` `ResponseMessagePageInfoUserVo` OK
     */
    listUser: (
      query?: {
        /** User name prefix to search for */
        userName?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoUserVo, any>({
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
     * @response `200` `ResponseMessageString` OK
     */
    createUser: (data: UserRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/user`,
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
     * @tags rolling-update-controller
     * @name InstanceStatus
     * @summary instance status notify
     * @request POST:/api/v1/system/upgrade/instance/status
     * @secure
     * @response `200` `ResponseMessageString` ok
     */
    instanceStatus: (
      query: {
        status: "BORN" | "READY_DOWN" | "READY_UP" | "DOWN";
        instanceType: "NEW" | "OLD";
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/system/upgrade/instance/status`,
        method: "POST",
        query: query,
        secure: true,
        format: "json",
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
     * @response `200` `ResponseMessageString` OK
     */
    querySetting: (params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
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
     * @response `200` `ResponseMessageString` OK
     */
    updateSetting: (data: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
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
     * @tags System
     * @name ListResourcePools
     * @summary Get the list of resource pool
     * @request GET:/api/v1/system/resourcePool
     * @secure
     * @response `200` `ResponseMessageListResourcePool` OK
     */
    listResourcePools: (params: RequestParams = {}) =>
      this.http.request<ResponseMessageListResourcePool, any>({
        path: `/api/v1/system/resourcePool`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags System
     * @name UpdateResourcePools
     * @summary Update resource pool
     * @request POST:/api/v1/system/resourcePool
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    updateResourcePools: (data: ResourcePool[], params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/system/resourcePool`,
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
     * @response `200` `ResponseMessageListProjectMemberVo` OK
     */
    listSystemRoles: (params: RequestParams = {}) =>
      this.http.request<ResponseMessageListProjectMemberVo, any>({
        path: `/api/v1/role`,
        method: "GET",
        secure: true,
        format: "json",
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
     * @response `200` `ResponseMessageString` OK
     */
    addUserSystemRole: (data: UserRoleAddRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/role`,
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
     * @tags Project
     * @name ListProject
     * @summary Get the list of projects
     * @request GET:/api/v1/project
     * @secure
     * @response `200` `ResponseMessagePageInfoProjectVo` OK
     */
    listProject: (
      sort: "visited" | "latest" | "oldest",
      query?: {
        projectName?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoProjectVo, any>({
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
     * @response `200` `ResponseMessageString` OK
     */
    createProject: (data: CreateProjectRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project`,
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
     * @tags Model
     * @name CreateModelVersion
     * @request POST:/api/v1/project/{project}/model/{modelName}/version/{version}/completeUpload
     * @secure
     * @response `200` `void` OK
     */
    createModelVersion: (
      project: string,
      modelName: string,
      version: string,
      data: CreateModelVersionRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<void, any>({
        path: `/api/v1/project/${project}/model/${modelName}/version/${version}/completeUpload`,
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
     * @name CreateModelServing
     * @summary Create a new model serving job
     * @request POST:/api/v1/project/{projectUrl}/serving
     * @secure
     * @response `200` `ResponseMessageModelServingVo` OK
     */
    createModelServing: (projectUrl: string, data: ModelServingRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageModelServingVo, any>({
        path: `/api/v1/project/${projectUrl}/serving`,
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
     * @tags Runtime
     * @name ListRuntimeVersionTags
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `ResponseMessageListString` OK
     */
    listRuntimeVersionTags: (projectUrl: string, runtimeUrl: string, versionUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageListString, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/tag`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Runtime
     * @name AddRuntimeVersionTag
     * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    addRuntimeVersionTag: (
      projectUrl: string,
      runtimeUrl: string,
      versionUrl: string,
      data: RuntimeTagRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/tag`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        format: "json",
        ...params,
      }),

    /**
     * @description build image for runtime
     *
     * @tags Runtime
     * @name BuildRuntimeImage
     * @summary build image for runtime
     * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/image/build
     * @secure
     * @response `200` `ResponseMessageBuildImageResult` OK
     */
    buildRuntimeImage: (
      projectUrl: string,
      runtimeUrl: string,
      versionUrl: string,
      data: RunEnvs,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageBuildImageResult, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/image/build`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        format: "json",
        ...params,
      }),

    /**
     * @description Select a historical version of the runtime and revert the latest version of the current runtime to this version
     *
     * @tags Runtime
     * @name RevertRuntimeVersion
     * @summary Revert Runtime version
     * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/revert
     * @secure
     * @response `200` `ResponseMessageString` OK
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
     * @tags Runtime
     * @name Upload
     * @summary Create a new runtime version
     * @request POST:/api/v1/project/{projectUrl}/runtime/{runtimeName}/version/{versionName}/file
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    upload: (
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
     * @response `200` `ResponseMessageListProjectMemberVo` OK
     */
    listProjectRole: (projectUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageListProjectMemberVo, any>({
        path: `/api/v1/project/${projectUrl}/role`,
        method: "GET",
        secure: true,
        format: "json",
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
     * @response `200` `ResponseMessageString` OK
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
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Report
     * @name ListReports
     * @summary Get the list of reports
     * @request GET:/api/v1/project/{projectUrl}/report
     * @secure
     * @response `200` `ResponseMessagePageInfoReportVo` OK
     */
    listReports: (
      projectUrl: string,
      query?: {
        title?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoReportVo, any>({
        path: `/api/v1/project/${projectUrl}/report`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Report
     * @name CreateReport
     * @request POST:/api/v1/project/{projectUrl}/report
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    createReport: (projectUrl: string, data: CreateReportRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/report`,
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
     * @tags Report
     * @name Transfer
     * @request POST:/api/v1/project/{projectUrl}/report/{reportId}/transfer
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    transfer: (projectUrl: string, reportId: number, data: TransferReportRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/report/${reportId}/transfer`,
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
     * @tags Model
     * @name ListModelVersionTags
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `ResponseMessageListString` OK
     */
    listModelVersionTags: (projectUrl: string, modelUrl: string, versionUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageListString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/tag`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name AddModelVersionTag
     * @request POST:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    addModelVersionTag: (
      projectUrl: string,
      modelUrl: string,
      versionUrl: string,
      data: ModelTagRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/tag`,
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
     * @tags Model
     * @name RevertModelVersion
     * @request POST:/api/v1/project/{projectUrl}/model/{modelUrl}/revert
     * @secure
     * @response `200` `ResponseMessageString` OK
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
     * @response `200` `ResponseMessagePageInfoJobVo` OK
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
      this.http.request<ResponseMessagePageInfoJobVo, any>({
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
     * @response `200` `ResponseMessageString` OK
     */
    createJob: (projectUrl: string, data: JobRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/job`,
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
     * @tags Job
     * @name Action
     * @summary Job Action
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/{action}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    action: (projectUrl: string, jobUrl: string, action: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/${action}`,
        method: "POST",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name Exec
     * @summary Execute command in running task
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/task/{taskId}/exec
     * @secure
     * @response `200` `ResponseMessageExecResponse` OK
     */
    exec: (projectUrl: string, jobUrl: string, taskId: string, data: ExecRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageExecResponse, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task/${taskId}/exec`,
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
     * @tags Job
     * @name RecoverJob
     * @summary Recover job
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/recover
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    recoverJob: (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/recover`,
        method: "POST",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name ModifyJobPinStatus
     * @summary Pin Job
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/pin
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    modifyJobPinStatus: (projectUrl: string, jobUrl: string, data: JobModifyPinRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/pin`,
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
     * @tags Job
     * @name GetEvents
     * @summary Get events of job or task
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/event
     * @secure
     * @response `200` `ResponseMessageListEventVo` OK
     */
    getEvents: (
      projectUrl: string,
      jobUrl: string,
      query?: {
        /** @format int64 */
        taskId?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageListEventVo, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/event`,
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
     * @name AddEvent
     * @summary Add event to job or task
     * @request POST:/api/v1/project/{projectUrl}/job/{jobUrl}/event
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    addEvent: (projectUrl: string, jobUrl: string, data: EventRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/event`,
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
     * @tags Evaluation
     * @name GetViewConfig
     * @summary Get View Config
     * @request GET:/api/v1/project/{projectUrl}/evaluation/view/config
     * @secure
     * @response `200` `ResponseMessageConfigVo` OK
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
        format: "json",
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
     * @response `200` `ResponseMessageString` OK
     */
    createViewConfig: (projectUrl: string, data: ConfigRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/evaluation/view/config`,
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
     * @tags Dataset
     * @name ListDatasetVersionTags
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `ResponseMessageListString` OK
     */
    listDatasetVersionTags: (projectUrl: string, datasetUrl: string, versionUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageListString, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/tag`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Dataset
     * @name AddDatasetVersionTag
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    addDatasetVersionTag: (
      projectUrl: string,
      datasetUrl: string,
      versionUrl: string,
      data: DatasetTagRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/tag`,
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
     * @tags Dataset
     * @name ConsumeNextData
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/consume
     * @secure
     * @response `200` `NullableResponseMessageDataIndexDesc` OK
     */
    consumeNextData: (
      projectUrl: string,
      datasetUrl: string,
      versionUrl: string,
      data: DataConsumptionRequest,
      params: RequestParams = {},
    ) =>
      this.http.request<NullableResponseMessageDataIndexDesc, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/consume`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        format: "json",
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
     * @response `200` `ResponseMessageString` OK
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
        format: "json",
        ...params,
      }),

    /**
     * @description Create a new version of the dataset. The data resources can be selected by uploading the file package or entering the server path.
     *
     * @tags Dataset
     * @name UploadDs
     * @summary Create a new dataset version
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetName}/version/{versionName}/file
     * @deprecated
     * @secure
     * @response `200` `ResponseMessageUploadResult` OK
     */
    uploadDs: (
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
     * @description Build Dataset
     *
     * @tags Dataset
     * @name BuildDataset
     * @summary Build Dataset
     * @request POST:/api/v1/project/{projectUrl}/dataset/{datasetName}/build
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    buildDataset: (projectUrl: string, datasetName: string, data: DatasetBuildRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetName}/build`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * @description Sign SWDS uris to get a batch of temporarily accessible links
     *
     * @tags Dataset
     * @name SignLinks
     * @summary Sign SWDS uris to get a batch of temporarily accessible links
     * @request POST:/api/v1/project/{projectName}/dataset/{datasetName}/uri/sign-links
     * @secure
     * @response `200` `ResponseMessageMapObjectObject` OK
     */
    signLinks: (
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
      this.http.request<ResponseMessageMapObjectObject, any>({
        path: `/api/v1/project/${projectName}/dataset/${datasetName}/uri/sign-links`,
        method: "POST",
        query: query,
        body: data,
        secure: true,
        type: ContentType.Json,
        format: "json",
        ...params,
      }),

    /**
     * @description 404 if not exists; 200 if exists
     *
     * @tags Dataset
     * @name GetHashedBlob
     * @summary Download the hashed blob in this dataset
     * @request GET:/api/v1/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}
     * @secure
     * @response `200` `void` OK
     */
    getHashedBlob: (projectName: string, datasetName: string, hash: string, params: RequestParams = {}) =>
      this.http.request<void, any>({
        path: `/api/v1/project/${projectName}/dataset/${datasetName}/hashedBlob/${hash}`,
        method: "GET",
        secure: true,
        ...params,
      }),

    /**
     * @description Upload a hashed BLOB to dataset object store, returns a uri of the main storage
     *
     * @tags Dataset
     * @name UploadHashedBlob
     * @summary Upload a hashed BLOB to dataset object store
     * @request POST:/api/v1/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    uploadHashedBlob: (
      projectName: string,
      datasetName: string,
      hash: string,
      data: {
        /** @format binary */
        file: File;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectName}/dataset/${datasetName}/hashedBlob/${hash}`,
        method: "POST",
        body: data,
        secure: true,
        type: ContentType.FormData,
        format: "json",
        ...params,
      }),

    /**
     * @description 404 if not exists; 200 if exists
     *
     * @tags Dataset
     * @name HeadHashedBlob
     * @summary Test if a hashed blob exists in this dataset
     * @request HEAD:/api/v1/project/{projectName}/dataset/{datasetName}/hashedBlob/{hash}
     * @secure
     * @response `200` `FlushRequest` OK
     */
    headHashedBlob: (projectName: string, datasetName: string, hash: string, params: RequestParams = {}) =>
      this.http.request<FlushRequest, any>({
        path: `/api/v1/project/${projectName}/dataset/${datasetName}/hashedBlob/${hash}`,
        method: "HEAD",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Get panel setting by project and key
     *
     * @tags Panel
     * @name GetPanelSetting
     * @summary Get panel setting
     * @request GET:/api/v1/panel/setting/{projectUrl}/{key}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    getPanelSetting: (projectUrl: string, key: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/panel/setting/${projectUrl}/${key}`,
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
     * @request POST:/api/v1/panel/setting/{projectUrl}/{key}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    setPanelSetting: (projectUrl: string, key: string, data: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/panel/setting/${projectUrl}/${key}`,
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
     * @response `200` `ResponseMessagePageInfoPanelPluginVo` OK
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
     * @response `200` `ResponseMessageString` OK
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
     * @name ScanAndExport
     * @request POST:/api/v1/datastore/scanTable/export
     * @secure
     * @response `200` `void` OK
     */
    scanAndExport: (data: ScanTableRequest, params: RequestParams = {}) =>
      this.http.request<void, any>({
        path: `/api/v1/datastore/scanTable/export`,
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
     * No description
     *
     * @tags Model
     * @name InitUploadBlob
     * @request POST:/api/v1/blob
     * @secure
     * @response `200` `ResponseMessageInitUploadBlobResult` OK
     */
    initUploadBlob: (data: InitUploadBlobRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageInitUploadBlobResult, any>({
        path: `/api/v1/blob`,
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
     * @tags Model
     * @name CompleteUploadBlob
     * @request POST:/api/v1/blob/{blobId}
     * @secure
     * @response `200` `ResponseMessageCompleteUploadBlobResult` OK
     */
    completeUploadBlob: (blobId: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageCompleteUploadBlobResult, any>({
        path: `/api/v1/blob/${blobId}`,
        method: "POST",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description head for runtime info
     *
     * @tags Runtime
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
     * @response `200` `FlushRequest` OK
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
     * @response `200` `ResponseMessageUserVo` OK
     */
    getUserById: (userId: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageUserVo, any>({
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
     * @response `200` `ResponseMessageString` OK
     */
    userToken: (userId: number, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
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
     * @response `200` `ResponseMessageUserVo` OK
     */
    getCurrentUser: (params: RequestParams = {}) =>
      this.http.request<ResponseMessageUserVo, any>({
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
     * @response `200` `ResponseMessageListProjectMemberVo` OK
     */
    getCurrentUserRoles: (
      query: {
        projectUrl: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageListProjectMemberVo, any>({
        path: `/api/v1/user/current/role`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
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
     * @response `200` `ResponseMessageSystemVersionVo` OK
     */
    getCurrentVersion: (params: RequestParams = {}) =>
      this.http.request<ResponseMessageSystemVersionVo, any>({
        path: `/api/v1/system/version`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Get system features list
     *
     * @tags System
     * @name QueryFeatures
     * @summary Get system features
     * @request GET:/api/v1/system/features
     * @secure
     * @response `200` `ResponseMessageFeaturesVo` OK
     */
    queryFeatures: (params: RequestParams = {}) =>
      this.http.request<ResponseMessageFeaturesVo, any>({
        path: `/api/v1/system/features`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags env-controller
     * @name ListDevice
     * @summary Get the list of device types
     * @request GET:/api/v1/runtime/device
     * @secure
     * @response `200` `ResponseMessageListDeviceVo` OK
     */
    listDevice: (params: RequestParams = {}) =>
      this.http.request<ResponseMessageListDeviceVo, any>({
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
     * @response `200` `ResponseMessageListRoleVo` OK
     */
    listRoles: (params: RequestParams = {}) =>
      this.http.request<ResponseMessageListRoleVo, any>({
        path: `/api/v1/role/enums`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Report
     * @name Preview
     * @request GET:/api/v1/report/{uuid}/preview
     * @secure
     * @response `200` `ResponseMessageReportVo` OK
     */
    preview: (uuid: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageReportVo, any>({
        path: `/api/v1/report/${uuid}/preview`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name GetModelMetaBlob
     * @request GET:/api/v1/project/{project}/model/{model}/version/{version}/meta
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    getModelMetaBlob: (
      project: string,
      model: string,
      version: string,
      query?: {
        blobId?: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${project}/model/${model}/version/${version}/meta`,
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
     * @name ListFiles
     * @request GET:/api/v1/project/{project}/model/{model}/listFiles
     * @secure
     * @response `200` `ResponseMessageListFilesResult` OK
     */
    listFiles: (
      project: string,
      model: string,
      query?: {
        version?: string;
        path?: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageListFilesResult, any>({
        path: `/api/v1/project/${project}/model/${model}/listFiles`,
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
     * @name GetFileData
     * @request GET:/api/v1/project/{project}/model/{model}/getFileData
     * @secure
     * @response `200` `File` OK
     */
    getFileData: (
      project: string,
      model: string,
      query: {
        version?: string;
        path: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<File, any>({
        path: `/api/v1/project/${project}/model/${model}/getFileData`,
        method: "GET",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * @description List all types of trashes, such as models datasets runtimes and evaluations
     *
     * @tags Trash
     * @name ListTrash
     * @summary Get the list of trash
     * @request GET:/api/v1/project/{projectUrl}/trash
     * @secure
     * @response `200` `ResponseMessagePageInfoTrashVo` OK
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
      this.http.request<ResponseMessagePageInfoTrashVo, any>({
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
     * @tags Runtime
     * @name ListRuntime
     * @summary Get the list of runtimes
     * @request GET:/api/v1/project/{projectUrl}/runtime
     * @secure
     * @response `200` `ResponseMessagePageInfoRuntimeVo` OK
     */
    listRuntime: (
      projectUrl: string,
      query?: {
        /** Runtime name prefix to search for */
        name?: string;
        owner?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoRuntimeVo, any>({
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
     * @tags Runtime
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
        versionUrl?: string;
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
     * @tags Runtime
     * @name DeleteRuntime
     * @summary Delete a runtime
     * @request DELETE:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}
     * @secure
     * @response `200` `ResponseMessageString` OK
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
     * @tags Runtime
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
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
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
     * @tags Runtime
     * @name Pull
     * @summary Pull file of a runtime version
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/file
     * @secure
     * @response `200` `void` OK
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
     * @tags Runtime
     * @name GetRuntimeVersionTag
     * @request GET:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/tag/{tag}
     * @secure
     * @response `200` `ResponseMessageLong` OK
     */
    getRuntimeVersionTag: (projectUrl: string, runtimeUrl: string, tag: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageLong, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/tag/${tag}`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Runtime
     * @name ListRuntimeTree
     * @summary List runtime tree including global runtimes
     * @request GET:/api/v1/project/{projectUrl}/runtime-tree
     * @secure
     * @response `200` `ResponseMessageListRuntimeViewVo` OK
     */
    listRuntimeTree: (
      projectUrl: string,
      query?: {
        /** Data range */
        scope?: "all" | "project" | "shared";
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageListRuntimeViewVo, any>({
        path: `/api/v1/project/${projectUrl}/runtime-tree`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Runtime
     * @name RecentRuntimeTree
     * @request GET:/api/v1/project/{projectUrl}/recent-runtime-tree
     * @secure
     * @response `200` `ResponseMessageListRuntimeViewVo` OK
     */
    recentRuntimeTree: (
      projectUrl: string,
      query?: {
        /**
         * Data limit
         * @format int32
         * @min 1
         * @max 50
         */
        limit?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageListRuntimeViewVo, any>({
        path: `/api/v1/project/${projectUrl}/recent-runtime-tree`,
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
     * @name RecentModelTree
     * @request GET:/api/v1/project/{projectUrl}/recent-model-tree
     * @secure
     * @response `200` `ResponseMessageListModelViewVo` OK
     */
    recentModelTree: (
      projectUrl: string,
      query?: {
        /**
         * Data limit
         * @format int32
         * @min 1
         * @max 50
         */
        limit?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageListModelViewVo, any>({
        path: `/api/v1/project/${projectUrl}/recent-model-tree`,
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
     * @name RecentDatasetTree
     * @request GET:/api/v1/project/{projectUrl}/recent-dataset-tree
     * @secure
     * @response `200` `ResponseMessageListDatasetViewVo` OK
     */
    recentDatasetTree: (
      projectUrl: string,
      query?: {
        /**
         * Data limit
         * @format int32
         * @min 1
         * @max 50
         */
        limit?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageListDatasetViewVo, any>({
        path: `/api/v1/project/${projectUrl}/recent-dataset-tree`,
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
     * @name ListModel
     * @request GET:/api/v1/project/{projectUrl}/model
     * @secure
     * @response `200` `ResponseMessagePageInfoModelVo` OK
     */
    listModel: (
      projectUrl: string,
      query?: {
        versionId?: string;
        name?: string;
        owner?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoModelVo, any>({
        path: `/api/v1/project/${projectUrl}/model`,
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
     * @name GetModelInfo
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}
     * @secure
     * @response `200` `ResponseMessageModelInfoVo` OK
     */
    getModelInfo: (
      projectUrl: string,
      modelUrl: string,
      query?: {
        versionUrl?: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageModelInfoVo, any>({
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
     * @request DELETE:/api/v1/project/{projectUrl}/model/{modelUrl}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    deleteModel: (projectUrl: string, modelUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name ListModelVersion
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/version
     * @secure
     * @response `200` `ResponseMessagePageInfoModelVersionVo` OK
     */
    listModelVersion: (
      projectUrl: string,
      modelUrl: string,
      query?: {
        name?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoModelVersionVo, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version`,
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
     * @name GetModelVersionTag
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/tag/{tag}
     * @secure
     * @response `200` `ResponseMessageLong` OK
     */
    getModelVersionTag: (projectUrl: string, modelUrl: string, tag: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageLong, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/tag/${tag}`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name GetModelDiff
     * @request GET:/api/v1/project/{projectUrl}/model/{modelUrl}/diff
     * @secure
     * @response `200` `ResponseMessageMapStringListFileNode` OK
     */
    getModelDiff: (
      projectUrl: string,
      modelUrl: string,
      query: {
        baseVersion: string;
        compareVersion: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageMapStringListFileNode, any>({
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
     * @tags Model
     * @name ListModelTree
     * @request GET:/api/v1/project/{projectUrl}/model-tree
     * @secure
     * @response `200` `ResponseMessageListModelViewVo` OK
     */
    listModelTree: (
      projectUrl: string,
      query?: {
        /** Data range */
        scope?: "all" | "project" | "shared";
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageListModelViewVo, any>({
        path: `/api/v1/project/${projectUrl}/model-tree`,
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
     * @response `200` `ResponseMessagePageInfoTaskVo` OK
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
      this.http.request<ResponseMessagePageInfoTaskVo, any>({
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
     * @name GetTask
     * @summary Get task info
     * @request GET:/api/v1/project/{projectUrl}/job/{jobUrl}/task/{taskUrl}
     * @secure
     * @response `200` `ResponseMessageTaskVo` OK
     */
    getTask: (projectUrl: string, jobUrl: string, taskUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageTaskVo, any>({
        path: `/api/v1/project/${projectUrl}/job/${jobUrl}/task/${taskUrl}`,
        method: "GET",
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
     * @response `200` `ResponseMessageObject` OK
     */
    getJobResult: (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageObject, any>({
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
     * @response `200` `ResponseMessageGraph` OK
     */
    getJobDag: (projectUrl: string, jobUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageGraph, any>({
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
     * @response `200` `ResponseMessagePageInfoSummaryVo` OK
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
        format: "json",
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
     * @response `200` `ResponseMessageListAttributeVo` OK
     */
    listAttributes: (projectUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageListAttributeVo, any>({
        path: `/api/v1/project/${projectUrl}/evaluation/view/attribute`,
        method: "GET",
        secure: true,
        format: "json",
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
     * @response `200` `ResponseMessagePageInfoDatasetVo` OK
     */
    listDataset: (
      projectUrl: string,
      query?: {
        versionId?: string;
        name?: string;
        owner?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoDatasetVo, any>({
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
     * @response `200` `ResponseMessageDatasetInfoVo` OK
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
      this.http.request<ResponseMessageDatasetInfoVo, any>({
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
     * @response `200` `ResponseMessageString` OK
     */
    deleteDataset: (projectUrl: string, datasetUrl: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}`,
        method: "DELETE",
        secure: true,
        format: "json",
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
     * @response `200` `ResponseMessagePageInfoDatasetVersionVo` OK
     */
    listDatasetVersion: (
      projectUrl: string,
      datasetUrl: string,
      query?: {
        name?: string;
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoDatasetVersionVo, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Pull Dataset files part by part.
     *
     * @tags Dataset
     * @name PullDs
     * @summary Pull Dataset files
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/file
     * @deprecated
     * @secure
     * @response `200` `void` OK
     */
    pullDs: (
      projectUrl: string,
      datasetUrl: string,
      versionUrl: string,
      query?: {
        /** optional, _manifest.yaml is used if not specified */
        partName?: string;
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
     * @tags Dataset
     * @name GetDatasetVersionTag
     * @request GET:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/tag/{tag}
     * @secure
     * @response `200` `ResponseMessageLong` OK
     */
    getDatasetVersionTag: (projectUrl: string, datasetUrl: string, tag: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageLong, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/tag/${tag}`,
        method: "GET",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description List Build Records
     *
     * @tags Dataset
     * @name ListBuildRecords
     * @summary List Build Records
     * @request GET:/api/v1/project/{projectUrl}/dataset/build/list
     * @secure
     * @response `200` `ResponseMessagePageInfoBuildRecordVo` OK
     */
    listBuildRecords: (
      projectUrl: string,
      query?: {
        /** @format int32 */
        pageNum?: number;
        /** @format int32 */
        pageSize?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessagePageInfoBuildRecordVo, any>({
        path: `/api/v1/project/${projectUrl}/dataset/build/list`,
        method: "GET",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Dataset
     * @name ListDatasetTree
     * @summary List dataset tree including global datasets
     * @request GET:/api/v1/project/{projectUrl}/dataset-tree
     * @secure
     * @response `200` `ResponseMessageListDatasetViewVo` OK
     */
    listDatasetTree: (
      projectUrl: string,
      query?: {
        scope?: "all" | "project" | "shared";
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageListDatasetViewVo, any>({
        path: `/api/v1/project/${projectUrl}/dataset-tree`,
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
     * @name PullUriContent
     * @summary Pull Dataset uri file contents
     * @request GET:/api/v1/project/{projectName}/dataset/{datasetName}/uri
     * @secure
     * @response `200` `void` OK
     */
    pullUriContent: (
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
      this.http.request<void, any>({
        path: `/api/v1/project/${projectName}/dataset/${datasetName}/uri`,
        method: "GET",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name GetModelServingStatus
     * @summary Get the events of the model serving job
     * @request GET:/api/v1/project/{projectId}/serving/{servingId}/status
     * @secure
     * @response `200` `ResponseMessageModelServingStatusVo` OK
     */
    getModelServingStatus: (projectId: number, servingId: number, params: RequestParams = {}) =>
      this.http.request<ResponseMessageModelServingStatusVo, any>({
        path: `/api/v1/project/${projectId}/serving/${servingId}/status`,
        method: "GET",
        secure: true,
        format: "json",
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
     * @response `200` `ResponseMessageListString` OK
     */
    offlineLogs: (taskId: number, params: RequestParams = {}) =>
      this.http.request<ResponseMessageListString, any>({
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
     * @response `200` `string` OK
     */
    logContent: (taskId: number, fileName: string, params: RequestParams = {}) =>
      this.http.request<string, any>({
        path: `/api/v1/log/offline/${taskId}/${fileName}`,
        method: "GET",
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Job
     * @name GetRuntimeSuggestion
     * @summary Get suggest runtime for eval or online eval
     * @request GET:/api/v1/job/suggestion/runtime
     * @secure
     * @response `200` `ResponseMessageRuntimeSuggestionVo` OK
     */
    getRuntimeSuggestion: (
      query: {
        /** @format int64 */
        projectId: number;
        /** @format int64 */
        modelVersionId?: number;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageRuntimeSuggestionVo, any>({
        path: `/api/v1/job/suggestion/runtime`,
        method: "GET",
        query: query,
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Apply pathPrefix
     *
     * @tags File storage
     * @name ApplyPathPrefix
     * @summary Apply pathPrefix
     * @request GET:/api/v1/filestorage/path/apply
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    applyPathPrefix: (
      query: {
        flag: string;
      },
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/filestorage/path/apply`,
        method: "GET",
        query: query,
        secure: true,
        ...params,
      }),

    /**
     * No description
     *
     * @tags Runtime
     * @name DeleteRuntimeVersionTag
     * @request DELETE:/api/v1/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag/{tag}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    deleteRuntimeVersionTag: (
      projectUrl: string,
      runtimeUrl: string,
      versionUrl: string,
      tag: string,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/runtime/${runtimeUrl}/version/${versionUrl}/tag/${tag}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Model
     * @name DeleteModelVersionTag
     * @request DELETE:/api/v1/project/{projectUrl}/model/{modelUrl}/version/{versionUrl}/tag/{tag}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    deleteModelVersionTag: (
      projectUrl: string,
      modelUrl: string,
      versionUrl: string,
      tag: string,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/model/${modelUrl}/version/${versionUrl}/tag/${tag}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * No description
     *
     * @tags Dataset
     * @name DeleteDatasetVersionTag
     * @request DELETE:/api/v1/project/{projectUrl}/dataset/{datasetUrl}/version/{versionUrl}/tag/{tag}
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    deleteDatasetVersionTag: (
      projectUrl: string,
      datasetUrl: string,
      versionUrl: string,
      tag: string,
      params: RequestParams = {},
    ) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/project/${projectUrl}/dataset/${datasetUrl}/version/${versionUrl}/tag/${tag}`,
        method: "DELETE",
        secure: true,
        format: "json",
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
     * @response `200` `ResponseMessageString` OK
     */
    uninstallPlugin: (id: string, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/panel/plugin/${id}`,
        method: "DELETE",
        secure: true,
        format: "json",
        ...params,
      }),

    /**
     * @description Delete path
     *
     * @tags File storage
     * @name DeletePath
     * @summary Delete path
     * @request DELETE:/api/v1/filestorage/file
     * @secure
     * @response `200` `ResponseMessageString` OK
     */
    deletePath: (data: FileDeleteRequest, params: RequestParams = {}) =>
      this.http.request<ResponseMessageString, any>({
        path: `/api/v1/filestorage/file`,
        method: "DELETE",
        body: data,
        secure: true,
        type: ContentType.Json,
        ...params,
      }),
  };
}
