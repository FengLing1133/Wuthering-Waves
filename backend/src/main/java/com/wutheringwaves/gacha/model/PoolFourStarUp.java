package com.wutheringwaves.gacha.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("gacha_pool_fourstar_up")
public class PoolFourStarUp {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long poolId;
    private Long itemId;
}
