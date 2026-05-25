package com.wutheringwaves.gacha.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("gacha_items")
public class GachaItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer rarity;
    private String itemType;
    private Long categoryId;
    private String imageUrl;
    private String videoUrl;
    private String loopVideoUrl;
    private String description;
}
