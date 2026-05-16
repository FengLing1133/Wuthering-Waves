package com.wutheringwaves.gacha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wutheringwaves.gacha.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
