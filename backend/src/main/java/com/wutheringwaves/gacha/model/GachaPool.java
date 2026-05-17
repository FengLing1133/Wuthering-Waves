package com.wutheringwaves.gacha.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gacha_pool")
public class GachaPool {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String poolType;
    private String description;
    private String imageUrl;
    private BigDecimal fiveStarRate;
    private BigDecimal fourStarRate;
    private Integer maxPity;
    private Integer softPityStart;
    private BigDecimal softPityIncrement;
    private String upItems;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
