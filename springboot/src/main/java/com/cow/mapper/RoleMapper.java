package com.cow.mapper;

import com.cow.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.HashMap;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * 
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色ID查询拥有该角色的员工信息
     */
    @Select("SELECT s.id, s.code, s.name, s.gender, s.phone, s.dept_id " +
            "FROM sys_staff s " +
            "INNER JOIN per_staff_role sr ON s.id = sr.staff_id " +
            "WHERE sr.role_id = #{roleId} AND sr.status = 1 AND sr.is_deleted = 0")
    List<HashMap<String, Object>> selectStaffNamesByRoleId(@Param("roleId") Integer roleId);

}
