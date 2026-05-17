package com.wutheringwaves.gacha.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pool_four_star")
public class PoolFourStar {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long poolId;
    private Long avatarId;
    private Integer sortOrder;
}
