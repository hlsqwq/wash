package com.hls.wash.repository;

import com.hls.wash.entity.ModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModelRepository extends JpaRepository<ModelEntity, Long> {

    Optional<ModelEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<ModelEntity> findAllByEnabledTrueOrderByIdAsc();

    Optional<ModelEntity> findByIdAndEnabledTrue(Long id);
}
