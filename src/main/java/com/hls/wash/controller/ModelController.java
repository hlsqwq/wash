package com.hls.wash.controller;

import com.hls.wash.dto.ApiResponse;
import com.hls.wash.dto.CreateModelRequest;
import com.hls.wash.dto.ModelDetailDto;
import com.hls.wash.dto.ModelSummaryDto;
import com.hls.wash.dto.UpdateModelRequest;
import com.hls.wash.service.ModelService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    public ApiResponse<List<ModelSummaryDto>> list() {
        return ApiResponse.ok(modelService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ModelDetailDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(modelService.get(id));
    }

    @PostMapping
    public ApiResponse<ModelDetailDto> create(@Valid @RequestBody CreateModelRequest request) {
        return ApiResponse.ok(modelService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ModelDetailDto> update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateModelRequest request) {
        return ApiResponse.ok(modelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        modelService.delete(id);
        return ApiResponse.ok();
    }
}
