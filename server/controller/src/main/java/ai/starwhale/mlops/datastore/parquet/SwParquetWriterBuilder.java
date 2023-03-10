/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.datastore.parquet;

import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.ParquetConfig;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.json.JSONUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

public class SwParquetWriterBuilder extends ParquetWriter.Builder<Map<String, Object>, SwParquetWriterBuilder> {

    private final Map<String, ColumnType> schema;
    private final Map<String, String> extraMeta = new HashMap<>();

    public SwParquetWriterBuilder(
            StorageAccessService storageAccessService,
            Map<String, ColumnType> schema,
            String tableSchema,
            String metadata,
            String path,
            ParquetConfig config) {
        super(new SwOutputFile(storageAccessService, path));
        this.schema = schema;

        this.extraMeta.put(SwReadSupport.PARQUET_SCHEMA_KEY,
                JSONUtil.toJsonStr(this.schema.entrySet().stream()
                    .map(entry -> entry.getValue().toColumnSchemaDesc(entry.getKey()))
                    .collect(Collectors.toList())));
        this.extraMeta.put(SwReadSupport.SCHEMA_KEY, tableSchema);
        this.extraMeta.put(SwReadSupport.META_DATA_KEY, metadata);
        this.extraMeta.put(SwReadSupport.ERROR_FLAG_KEY, String.valueOf(true));

        switch (config.getCompressionCodec()) {
            case SNAPPY:
                this.withCompressionCodec(CompressionCodecName.SNAPPY);
                break;
            case GZIP:
                this.withCompressionCodec(CompressionCodecName.GZIP);
                break;
            case LZO:
                this.withCompressionCodec(CompressionCodecName.LZO);
                break;
            case BROTLI:
                this.withCompressionCodec(CompressionCodecName.BROTLI);
                break;
            case LZ4:
                this.withCompressionCodec(CompressionCodecName.LZ4);
                break;
            case ZSTD:
                this.withCompressionCodec(CompressionCodecName.ZSTD);
                break;
            default:
                throw new SwProcessException(ErrorType.DATASTORE,
                        "invalid compression codec " + config.getCompressionCodec());
        }
        this.withRowGroupSize(config.getRowGroupSize());
        this.withPageSize(config.getPageSize());
        this.withPageRowCountLimit(config.getPageRowCountLimit());
    }

    @Override
    protected SwParquetWriterBuilder self() {
        return this;
    }

    public void success() {
        this.extraMeta.put(SwReadSupport.ERROR_FLAG_KEY, String.valueOf(false));
    }

    @Override
    protected WriteSupport<Map<String, Object>> getWriteSupport(Configuration configuration) {
        return new SwWriteSupport(this.schema, this.extraMeta);
    }
}
