package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.group1.cloudlibrary.entity.VerificationCode;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 验证码 Mapper
 */
@Mapper
public interface VerificationCodeMapper extends BaseMapper<VerificationCode> {

    /**
     * 删除某邮箱 + 类型的旧验证码
     */
    @Delete("DELETE FROM verification_code WHERE email = #{email} AND type = #{type}")
    int deleteByEmailAndType(@Param("email") String email, @Param("type") Integer type);

    /**
     * 清理已过期的验证码
     */
    @Update("DELETE FROM verification_code WHERE expired_at < NOW()")
    int cleanExpired();
}
