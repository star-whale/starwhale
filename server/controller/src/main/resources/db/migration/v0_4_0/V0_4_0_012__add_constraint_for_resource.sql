
ALTER TABLE model_info ADD deleted_time BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE runtime_info ADD deleted_time BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE dataset_info ADD deleted_time BIGINT DEFAULT 0 NOT NULL;

UPDATE model_info set deleted_time = UNIX_TIMESTAMP(modified_time) where is_deleted != 0
UPDATE runtime_info set deleted_time = UNIX_TIMESTAMP(modified_time) where is_deleted != 0
UPDATE dataset_info set deleted_time = UNIX_TIMESTAMP(modified_time) where is_deleted != 0

ALTER TABLE model_info ADD CONSTRAINT model_info_PK UNIQUE KEY (model_name,project_id,deleted_time);
ALTER TABLE runtime_info ADD CONSTRAINT runtime_info_PK UNIQUE KEY (runtime_name,project_id,deleted_time);
ALTER TABLE dataset_info ADD CONSTRAINT dataset_info_PK UNIQUE KEY (dataset_name,project_id,deleted_time);
