/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVersionVO;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Model")
@Validated
public interface SWModelPackageApi {

    @Operation(summary = "Get the list of models")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PageInfo.class)))
        })
    @GetMapping(value = "/model")
    ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVO>>> listModel(
        @Parameter(
            in = ParameterIn.QUERY,
            description = "Model name prefix to search for",
            schema = @Schema())
        @Valid
        @RequestParam(value = "modelName", required = false)
            String modelName,
        @Parameter(in = ParameterIn.QUERY, description = "Page number", schema = @Schema())
        @Valid
        @RequestParam(value = "pageNum", required = false)
            Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
        @Valid
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize);

    @Operation(
        summary = "Revert model version",
        description =
            "Select a historical version of the model and revert the latest version of the current model to this version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/model/{modelId}/revert")
    ResponseEntity<ResponseMessage<String>> revertModelVersion(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Model ID",
            required = true,
            schema = @Schema())
        @PathVariable("modelId")
            String modelId,
        @NotNull
        @Parameter(
            in = ParameterIn.QUERY,
            description = "The model version ID to revert",
            required = true,
            schema = @Schema())
        @Valid
        @RequestParam(value = "versionId")
            String versionId);

    @Operation(summary = "Delete a model")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(value = "/model/{modelId}")
    ResponseEntity<ResponseMessage<String>> deleteModelById(
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("modelId")
            Integer modelId);

    @Operation(summary = "Model information",
        description = "Return the file information in the model package of the latest version of the current model")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "OK",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SWModelPackageInfoVO.class)))
        })
    @GetMapping(value = "/model/{modelId}")
    ResponseEntity<ResponseMessage<SWModelPackageInfoVO>> getModelInfo(
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("modelId")
            Integer modelId);

    @Operation(summary = "Get the list of model versions")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PageInfo.class)))
        })
    @GetMapping(value = "/model/{modelId}/version")
    ResponseEntity<ResponseMessage<PageInfo<SWModelPackageVersionVO>>> listModelVersion(
        @Parameter(in = ParameterIn.PATH, description = "Model ID", required = true, schema = @Schema())
        @PathVariable("modelId")
            Integer modelId,
        @Parameter(in = ParameterIn.QUERY, description = "Model version name prefix to search for", schema = @Schema())
        @Valid
        @RequestParam(value = "modelVersionName", required = false)
            String modelVersionName,
        @Parameter(in = ParameterIn.QUERY, description = "Page number", schema = @Schema())
        @Valid
        @RequestParam(value = "pageNum", required = false)
            Integer pageNum,
        @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
        @Valid
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize);

    @Operation(summary = "Create a new dataset version",
        description = "Create a new model version. "
            + "The model file can be selected by uploading the file package or entering the server path")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
        value = "/model/{modelId}/version",
        produces = {"application/json"},
        consumes = {"multipart/form-data"})
    ResponseEntity<ResponseMessage<String>> createModelVersion(
        @Parameter(in = ParameterIn.PATH, description = "Model ID", required = true, schema = @Schema())
        @PathVariable("modelId")
            String modelId,
        @Parameter(description = "file detail") @Valid @RequestPart("file") MultipartFile zipFile,
        @Parameter(in = ParameterIn.DEFAULT, schema = @Schema())
        @RequestParam(value = "importPath", required = false)
            String importPath);

    @Operation(summary = "Set tag of the model version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/model/{modelId}/version/{versionId}")
    ResponseEntity<ResponseMessage<String>> modifyModel(
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("modelId")
            String modelId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("versionId")
            String versionId,
        @Parameter(in = ParameterIn.QUERY, schema = @Schema())
        @Valid
        @RequestParam(value = "tag", required = false)
            String tag);

    @Operation(summary = "Create a new model",
        description = "Create a new model and create an initial version. "
            + "The model file is selected by uploading a file package or entering a server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
        value = "/model",
        produces = {"application/json"},
        consumes = {"multipart/form-data"})
    ResponseEntity<ResponseMessage<String>> createModel(
        @Parameter(in = ParameterIn.DEFAULT, required = true, schema = @Schema())
        @RequestParam(value = "modelName")
            String modelName,
        @Parameter(description = "file detail") @Valid @RequestPart("file") MultipartFile zipFile,
        @Parameter(in = ParameterIn.DEFAULT, required = true, schema = @Schema())
        @RequestParam(value = "importPath")
            String importPath);
}
