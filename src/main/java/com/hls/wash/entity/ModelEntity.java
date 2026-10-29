package com.hls.wash.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 蜂窝盘模型。
 *
 * <p>行数、每行管数、行首偏移等结构化数据以 JSON 文本保存，
 * 由服务层统一序列化/反序列化，业务代码不直接操作原始字符串。</p>
 */
@Entity
@Table(name = "model", uniqueConstraints = @UniqueConstraint(name = "uk_model_code", columnNames = "code"))
public class ModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "image_id", length = 64)
    private String imageId;

    @Column(name = "dx", nullable = false)
    private Integer dx;

    @Column(name = "dy", nullable = false)
    private Integer dy;

    @Column(name = "total_row", nullable = false)
    private Integer totalRow;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "row_len", columnDefinition = "TEXT")
    private String rowLen;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "row_offset", columnDefinition = "TEXT")
    private String rowOffset;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "hide_holes", columnDefinition = "TEXT")
    private String hideHoles;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "block_holes", columnDefinition = "TEXT")
    private String blockHoles;

    /** 保留原业务语义的竖向隔断配置，当前与 wideCenterGap 不同，不混用。 */
    @Column(name = "is_vertical_partition", nullable = false)
    private boolean verticalPartition = false;

    @Column(name = "is_horizon_partition", nullable = false)
    private boolean horizonPartitionPresent = false;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "horizon_partition", columnDefinition = "TEXT")
    private String horizonPartition;

    /** 盘面中间加宽（Android 的 b_is_wider）。 */
    @Column(name = "wide_center_gap", nullable = false)
    private boolean wideCenterGap = false;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (createTime == null) {
            createTime = now;
        }
        updateTime = now;
    }

    @PreUpdate
    void preUpdate() {
        updateTime = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public Integer getDx() {
        return dx;
    }

    public void setDx(Integer dx) {
        this.dx = dx;
    }

    public Integer getDy() {
        return dy;
    }

    public void setDy(Integer dy) {
        this.dy = dy;
    }

    public Integer getTotalRow() {
        return totalRow;
    }

    public void setTotalRow(Integer totalRow) {
        this.totalRow = totalRow;
    }

    public String getRowLen() {
        return rowLen;
    }

    public void setRowLen(String rowLen) {
        this.rowLen = rowLen;
    }

    public String getRowOffset() {
        return rowOffset;
    }

    public void setRowOffset(String rowOffset) {
        this.rowOffset = rowOffset;
    }

    public String getHideHoles() {
        return hideHoles;
    }

    public void setHideHoles(String hideHoles) {
        this.hideHoles = hideHoles;
    }

    public String getBlockHoles() {
        return blockHoles;
    }

    public void setBlockHoles(String blockHoles) {
        this.blockHoles = blockHoles;
    }

    public boolean isVerticalPartition() {
        return verticalPartition;
    }

    public void setVerticalPartition(boolean verticalPartition) {
        this.verticalPartition = verticalPartition;
    }

    public boolean isHorizonPartitionPresent() {
        return horizonPartitionPresent;
    }

    public void setHorizonPartitionPresent(boolean horizonPartitionPresent) {
        this.horizonPartitionPresent = horizonPartitionPresent;
    }

    public String getHorizonPartition() {
        return horizonPartition;
    }

    public void setHorizonPartition(String horizonPartition) {
        this.horizonPartition = horizonPartition;
    }

    public boolean isWideCenterGap() {
        return wideCenterGap;
    }

    public void setWideCenterGap(boolean wideCenterGap) {
        this.wideCenterGap = wideCenterGap;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
