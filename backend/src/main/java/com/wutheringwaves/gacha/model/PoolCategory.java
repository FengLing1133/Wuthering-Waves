package com.wutheringwaves.gacha.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pool_category")
public class PoolCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long poolId;
    private Long categoryId;
}
