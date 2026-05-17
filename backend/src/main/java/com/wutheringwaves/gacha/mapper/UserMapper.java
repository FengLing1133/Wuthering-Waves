package com.wutheringwaves.gacha.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wutheringwaves.gacha.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE users SET starlight = starlight + #{delta} WHERE id = #{userId}")
    int updateStarlight(@Param("userId") Long userId, @Param("delta") int delta);

    @Update("UPDATE users SET selected_standard_weapon_up = #{weaponId} WHERE id = #{userId}")
    int updateSelectedWeaponUp(@Param("userId") Long userId, @Param("weaponId") Long weaponId);
}
