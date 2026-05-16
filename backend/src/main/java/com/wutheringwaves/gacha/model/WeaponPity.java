package com.wutheringwaves.gacha.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("weapon_pity")
public class WeaponPity {
    @TableId
    private Long userId;
    private Integer fiveStarCount;
    private Integer fourStarCount;
    private Boolean guaranteedFive;
    private LocalDateTime updatedAt;
}
