-- ----------------------------
-- Table structure for job_info
-- ----------------------------
alter table job_info
    change swmp_version_id model_version_id bigint not null;

alter table job_info
    change swrt_version_id runtime_version_id bigint not null;

-- ----------------------------
-- Table structure for model_info
-- ----------------------------
alter table swmp_info
    change swmp_name model_name varchar(255) not null;

rename table swmp_info to model_info;

-- ----------------------------
-- Table structure for model_version
-- ----------------------------
alter table swmp_version
    change swmp_id model_id bigint not null;

rename table swmp_version to model_version;