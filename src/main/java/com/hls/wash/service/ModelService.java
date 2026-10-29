package com.hls.wash.service;

import com.hls.wash.dto.CreateModelRequest;
import com.hls.wash.dto.ModelDetailDto;
import com.hls.wash.dto.ModelSummaryDto;
import com.hls.wash.dto.UpdateModelRequest;
import com.hls.wash.entity.ModelEntity;
import com.hls.wash.exception.ApiException;
import com.hls.wash.repository.MediaRepository;
import com.hls.wash.repository.ModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ModelService {

    private static final String IMAGE_PATH = "/files/images/";

    private final ModelRepository modelRepository;
    private final MediaRepository mediaRepository;
    private final ModelAssembler assembler;

    public ModelService(ModelRepository modelRepository, MediaRepository mediaRepository,
                        ModelAssembler assembler) {
        this.modelRepository = modelRepository;
        this.mediaRepository = mediaRepository;
        this.assembler = assembler;
    }

    @Transactional(readOnly = true)
    public List<ModelSummaryDto> list() {
        return modelRepository.findAllByEnabledTrueOrderByIdAsc().stream()
                .map(assembler::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ModelDetailDto get(Long id) {
        return assembler.toDetail(requireEnabled(id));
    }

    @Transactional
    public ModelDetailDto create(CreateModelRequest request) {
        String code = request.code().trim();
        if (modelRepository.existsByCode(code)) {
            throw ApiException.duplicateCode("模型编码已存在: " + code);
        }
        String imageId = resolveImageId(request.imageUrl());
        ModelEntity entity = assembler.build(request, imageId);
        return assembler.toDetail(modelRepository.saveAndFlush(entity));
    }

    @Transactional
    public ModelDetailDto update(Long id, UpdateModelRequest request) {
        ModelEntity entity = requireEnabled(id);
        String code = request.code().trim();
        if (!code.equals(entity.getCode()) && modelRepository.existsByCode(code)) {
            throw ApiException.duplicateCode("模型编码已存在: " + code);
        }
        String imageId = resolveImageId(request.imageUrl());
        assembler.applyUpdate(entity, request, imageId);
        return assembler.toDetail(modelRepository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long id) {
        ModelEntity entity = requireEnabled(id);
        entity.setEnabled(false);
        modelRepository.saveAndFlush(entity);
    }

    private ModelEntity requireEnabled(Long id) {
        return modelRepository.findByIdAndEnabledTrue(id)
                .orElseThrow(() -> ApiException.notFound("模型不存在: " + id));
    }

    /**
     * 只接受本服务上传接口生成的图片地址（或 MD5 标识），并验证 media 记录确实存在。
     */
    private String resolveImageId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        String value = imageUrl.trim();
        int index = value.indexOf(IMAGE_PATH);
        if (index >= 0) {
            value = value.substring(index + IMAGE_PATH.length());
        }
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        int fragment = value.indexOf('#');
        if (fragment >= 0) {
            value = value.substring(0, fragment);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty() || value.length() > 64) {
            throw ApiException.notFound("图片不存在，请先通过上传接口获取图片地址");
        }
        if (!mediaRepository.existsById(value)) {
            throw ApiException.notFound("图片不存在，请先通过上传接口获取图片地址");
        }
        return value;
    }
}
