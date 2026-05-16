package com.wutheringwaves.gacha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wutheringwaves.gacha.model.GachaItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GachaItemMapper extends BaseMapper<GachaItem> {
}
