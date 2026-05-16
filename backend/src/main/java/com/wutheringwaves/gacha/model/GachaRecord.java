package com.wutheringwaves.gacha.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gacha_records")
public class GachaRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String poolType;
    private String itemName;
    private Integer itemRarity;
    private String itemType;
    private Boolean isLimited;
    private Integer pityCount;
    private LocalDateTime createdAt;
}
