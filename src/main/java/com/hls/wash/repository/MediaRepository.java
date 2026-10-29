package com.hls.wash.repository;

import com.hls.wash.entity.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaEntity, String> {
}
