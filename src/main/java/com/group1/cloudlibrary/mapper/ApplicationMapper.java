package com.group1.cloudlibrary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.group1.cloudlibrary.entity.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 操作申请 Mapper
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {

    /**
     * 操作申请分页查询（带申请人邮箱 / 昵称 / 头像）
     *
     * @param status      状态筛选（null 表示全部）
     * @param applicantId 只看某个申请人提交的申请（null 表示全部）
     */
    @Select("<script>" +
            "SELECT a.*, u.email AS applicant_email, u.nickname AS applicant_nickname, u.avatar AS applicant_avatar " +
            "FROM application a LEFT JOIN `user` u ON a.applicant_id = u.id " +
            "<where>" +
            "  <if test='status != null'> AND a.status = #{status} </if>" +
            "  <if test='applicantId != null'> AND a.applicant_id = #{applicantId} </if>" +
            "</where>" +
            " ORDER BY a.status ASC, a.created_at DESC" +
            "</script>")
    IPage<Application> selectApplicationPage(IPage<Application> page,
                                             @Param("status") Integer status,
                                             @Param("applicantId") Long applicantId);
}
