package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.group1.cloudlibrary.entity.UserTheme;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户自定义主题 Mapper
 */
@Mapper
public interface UserThemeMapper extends BaseMapper<UserTheme> {
}
