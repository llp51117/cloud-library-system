package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.group1.cloudlibrary.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员操作记录 Mapper
 */
@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {

    /**
     * 某管理员指定类型的操作条数（用于「处理举报 X 条 / 删除评论 X 条 / 编辑图书 X 本」）
     */
    @Select("SELECT COUNT(*) FROM admin_operation_log WHERE admin_id = #{adminId} AND type = #{type}")
    Long countByType(@Param("adminId") Long adminId, @Param("type") int type);
}
