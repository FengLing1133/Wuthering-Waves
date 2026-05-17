package com.wutheringwaves.gacha.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gacha_pity")
public class GachaPity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String poolType;
    private Integer fiveStarCount;
    private Integer fourStarCount;
    private Boolean guaranteedFive;
    private LocalDateTime updatedAt;
}
