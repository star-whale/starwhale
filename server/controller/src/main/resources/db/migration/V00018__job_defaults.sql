ALTER TABLE job_info MODIFY COLUMN created_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE job_info MODIFY COLUMN duration_ms     bigint;


