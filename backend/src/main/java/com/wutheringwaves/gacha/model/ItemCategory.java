package com.wutheringwaves.gacha.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("item_category")
public class ItemCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer rarity;
    private String itemType;
    private String description;
    private Integer sortOrder;
}
