package com.wutheringwaves.gacha.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("four_star_avatars")
public class FourStarAvatar {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatarUrl;
    private LocalDateTime createdAt;
}
