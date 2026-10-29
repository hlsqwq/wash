package com.hls.wash.seed;

import com.hls.wash.config.SeedProperties;
import com.hls.wash.dto.CreateModelRequest;
import com.hls.wash.dto.RowDto;
import com.hls.wash.entity.MediaEntity;
import com.hls.wash.entity.ModelEntity;
import com.hls.wash.exception.ApiException;
import com.hls.wash.exception.StorageException;
import com.hls.wash.repository.ModelRepository;
import com.hls.wash.service.JsonCodec;
import com.hls.wash.service.MediaService;
import com.hls.wash.service.ModelAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 幂等导入 Android 中现有的四个真实模型。
 *
 * <ul>
 *   <li>只插入 code 不存在的模型，不覆盖管理页面维护过的数据；</li>
 *   <li>已存在但 image_id 为空的种子模型，只补图片关联；</li>
 *   <li>图片上传失败时如实跳过并记录日志，不写入伪造的图片地址；</li>
 *   <li>可重复执行。</li>
 * </ul>
 */
@Component
public class ModelSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ModelSeeder.class);
    private static final String SEED_FILE = "seed/models.json";

    private final SeedProperties properties;
    private final ModelRepository modelRepository;
    private final MediaService mediaService;
    private final ModelAssembler assembler;
    private final JsonCodec jsonCodec;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    public ModelSeeder(SeedProperties properties, ModelRepository modelRepository,
                       MediaService mediaService, ModelAssembler assembler, JsonCodec jsonCodec) {
        this.properties = properties;
        this.modelRepository = modelRepository;
        this.mediaService = mediaService;
        this.assembler = assembler;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        List<SeedModel> seeds = loadSeeds();
        int created = 0;
        int skipped = 0;
        int imagesLinked = 0;

        for (SeedModel seed : seeds) {
            try {
                Optional<ModelEntity> existing = modelRepository.findByCode(seed.code());
                if (existing.isPresent()) {
                    skipped++;
                    ModelEntity entity = existing.get();
                    if (entity.getImageId() == null) {
                        String imageId = tryStoreImage(seed);
                        if (imageId != null) {
                            entity.setImageId(imageId);
                            modelRepository.save(entity);
                            imagesLinked++;
                        }
                    }
                    verify(seed, entity);
                    continue;
                }

                String imageId = tryStoreImage(seed);
                ModelEntity entity = assembler.build(toRequest(seed), imageId);
                ModelEntity saved = modelRepository.saveAndFlush(entity);
                created++;
                log.info("导入模型 {}：{} 行 / 总管数 {}", saved.getCode(), saved.getTotalRow(),
                        jsonCodec.readIntList(saved.getRowLen()).stream().mapToInt(Integer::intValue).sum());
                verify(seed, saved);
            } catch (RuntimeException e) {
                log.error("导入模型 {} 失败: {}", seed.code(), e.getMessage(), e);
            }
        }

        log.info("模型种子数据完成：新建 {} 个，已存在跳过 {} 个，补齐图片 {} 个", created, skipped, imagesLinked);
    }

    private List<SeedModel> loadSeeds() {
        try (InputStream in = new ClassPathResource(SEED_FILE).getInputStream()) {
            List<SeedModel> seeds = mapper.readValue(in, new TypeReference<List<SeedModel>>() {
            });
            if (seeds == null || seeds.isEmpty()) {
                throw new IllegalStateException("种子数据为空: " + SEED_FILE);
            }
            return seeds;
        } catch (IOException e) {
            throw new IllegalStateException("无法读取种子数据 " + SEED_FILE, e);
        }
    }

    private String tryStoreImage(SeedModel seed) {
        if (seed.imageFile() == null || seed.imageFile().isBlank()) {
            return null;
        }
        ClassPathResource resource = new ClassPathResource(seed.imageFile());
        if (!resource.exists()) {
            log.warn("模型 {} 的图片资源不存在: {}", seed.code(), seed.imageFile());
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            MediaEntity media = mediaService.storeImage(fileName(seed.imageFile()), bytes);
            return media.getId();
        } catch (StorageException e) {
            log.warn("模型 {} 的图片上传 RustFS 失败，模型将先以无图方式导入: {}", seed.code(), e.getMessage());
            return null;
        } catch (ApiException e) {
            log.warn("模型 {} 的图片校验失败: {}", seed.code(), e.getMessage());
            return null;
        } catch (IOException e) {
            log.warn("读取模型 {} 的图片失败: {}", seed.code(), e.getMessage());
            return null;
        }
    }

    private String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private CreateModelRequest toRequest(SeedModel seed) {
        List<RowDto> rows = new ArrayList<>(seed.rows().size());
        for (int i = 0; i < seed.rows().size(); i++) {
            SeedModel.SeedRow row = seed.rows().get(i);
            rows.add(new RowDto(i, row.tubeCount(), row.offsetX(), row.offsetY()));
        }
        return new CreateModelRequest(
                seed.code(),
                seed.name(),
                null,
                null,
                seed.dx(),
                seed.dy(),
                seed.wideCenterGap(),
                rows,
                seed.hideCells(),
                seed.blockCells(),
                seed.separators()
        );
    }

    private void verify(SeedModel seed, ModelEntity entity) {
        List<Integer> lens = jsonCodec.readIntList(entity.getRowLen());
        int tubes = lens.stream().mapToInt(Integer::intValue).sum();
        int expectedTubes = seed.rows().stream().mapToInt(SeedModel.SeedRow::tubeCount).sum();
        boolean ok = lens.size() == seed.rows().size() && tubes == expectedTubes;
        if (ok) {
            log.info("校验通过 {}：{} 行，{} 根管", seed.code(), lens.size(), tubes);
        } else {
            log.error("校验失败 {}：数据库 {} 行/{} 根，种子数据 {} 行/{} 根",
                    seed.code(), lens.size(), tubes, seed.rows().size(), expectedTubes);
        }
    }
}
