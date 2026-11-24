package com.cow.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cow.cache.CacheKeys;
import com.cow.cache.CacheService;
import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import com.cow.entity.Dept;
import com.cow.entity.Staff;
import com.cow.enums.BusinessStatusEnum;
import com.cow.exception.ServiceException;
import com.cow.mapper.StaffMapper;
import com.cow.util.HutoolExcelUtil;
import com.cow.util.MD5Util;
import com.cow.vo.StaffDeptVO;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

/**
 * <p>
 * 服务类
 * </p>
 *
 * 
 */

@Service
public class StaffService extends ServiceImpl<StaffMapper, Staff> {


    @Resource
    private DeptService deptService;

    @Resource
    private StaffMapper staffMapper;

    @Resource
    private CacheService cacheService;


    /**
     * 带缓存的员工查询
     * <p>
     * JWTInterceptor 对每个受保护请求都会校验员工，属于全系统最高频的读操作，
     * 缓存后可以把这部分数据库压力降下来。
     *
     * @param id 员工id
     * @return 员工信息，不存在返回 null
     */
    public Staff getByIdWithCache(Integer id) {
        if (id == null) {
            return null;
        }
        String key = CacheKeys.staff(id);
        Object cached = this.cacheService.get(key);
        if (cached instanceof Staff) {
            return (Staff) cached;
        }
        Staff staff = getById(id);
        if (staff != null) {
            this.cacheService.set(key, staff, CacheKeys.STAFF_TTL);
        }
        return staff;
    }

    /**
     * 清理员工缓存
     * <p>
     * 员工的状态、密码、部门发生变更后必须调用，否则拦截器会继续使用旧数据放行请求。
     *
     * @param id             员工id
     * @param revokeLogin    是否同时撤销登录态（删除 token 白名单）
     */
    public void evictCache(Integer id, boolean revokeLogin) {
        if (id == null) {
            return;
        }
        this.cacheService.delete(CacheKeys.staff(id));
        // 员工所属部门可能变了，作息时间缓存一并清理
        this.deptService.evictDeptTimeByStaffId(id);
        if (revokeLogin) {
            this.cacheService.delete(CacheKeys.token(id));
        }
    }

    /**
     * 新增
     *
     * @param staff
     * @return ResponseDTO
     */
    public ResponseDTO add(Staff staff) {
        if (save(staff)) {
            // 设置默认密码、工号
            staff.setPassword(MD5Util.MD55("123")).setCode("staff_" + staff.getId());
            updateById(staff);
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 逻辑删除
     *
     * @param id
     * @return
     */
    public ResponseDTO deleteById(Integer id) {
        if (removeById(id)) {
            // 员工已删除，清理缓存并立即撤销其登录态
            evictCache(id, true);
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 编辑
     *
     * @param staff
     * @return
     */
    public ResponseDTO edit(Staff staff) {
        if (updateById(staff)) {
            // 状态被改为非在职时直接踢下线，否则仅清理缓存让拦截器读到最新数据
            boolean revokeLogin = staff.getStatus() != null && staff.getStatus() != 1;
            evictCache(staff.getId(), revokeLogin);
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 查找
     *
     * @param id
     * @return
     */
    public ResponseDTO findById(Integer id) {
        Staff staff = getById(id);
        if (staff != null) {
            return Response.success(staff);
        }
        return Response.error();
    }


    /**
     * 多条件分页查询
     *
     * @param current 当前页
     * @param size    页面显示的数据条数
     * @param staff
     * @return
     */
    public ResponseDTO list(Integer current, Integer size, Staff staff) {
        // 分页构造
        IPage<Staff> pageConfig = new Page<>(current, size);
        QueryWrapper<Staff> wrapper = new QueryWrapper<>();
        if (staff.getName() != "" && staff.getName() != null) {
            wrapper.like("name", staff.getName());
        }
        if (staff.getBirthday() != null) {
            wrapper.ge("birthday", staff.getBirthday());
        }
        if (staff.getDeptId() != null) {
            wrapper.eq("dept_id", staff.getDeptId());
        }
        if (staff.getStatus() != null) {
            wrapper.eq("status", staff.getStatus());
        }
        IPage<Staff> page = page(pageConfig, wrapper);
        List<Staff> records = page.getRecords();
        List<StaffDeptVO> staffDeptVOList = new ArrayList<>();
        for (Staff record : records) {
            QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", record.getDeptId());
            Dept dept = this.deptService.getOne(queryWrapper);
            // 设置部门和年龄
            StaffDeptVO staffDeptVO = new StaffDeptVO();
            staffDeptVO.setDeptName(dept.getName());
            if (record.getBirthday() != null) {
                staffDeptVO.setAge(DateUtil.ageOfNow(record.getBirthday()));
            }
            BeanUtil.copyProperties(record, staffDeptVO);
            staffDeptVOList.add(staffDeptVO);
        }
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", staffDeptVOList);
        return Response.success(map);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            for (Integer id : ids) {
                evictCache(id, true);
            }
            return Response.success();
        }
        return Response.error();
    }


    /**
     * 数据导出
     *
     * @param response
     * @return
     */
    public ResponseDTO export(HttpServletResponse response) throws IOException {
        List<StaffDeptVO> list = this.staffMapper.findStaffDeptVO();
        // 设置员工年龄
        for (StaffDeptVO staffDeptVO : list) {
            if (staffDeptVO.getBirthday() != null) {
                staffDeptVO.setAge(DateUtil.ageOfNow(staffDeptVO.getBirthday()));
            }
        }
        HutoolExcelUtil.writeExcel(response, list, "员工信息表", StaffDeptVO.class);
        return Response.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO imp(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        List<Staff> list = HutoolExcelUtil.readExcel(inputStream, 1, Staff.class);
        for (Staff staff : list) {
            if (save(staff)) {
                // 设置默认密码、工号、部门
                staff.setPassword(MD5Util.MD55("123")).setCode("staff_" + staff.getId()).setDeptId(13);
                updateById(staff);
            } else {
                return Response.error();
            }
        }
        return Response.success();
    }

    // 检查员工的密码
    public ResponseDTO checkPassword(String pwd, Integer id) {
        Staff staff = getById(id);
        if (staff != null) {
            if (StrUtil.isNotBlank(pwd)) {
                if (MD5Util.MD55(pwd).equals(staff.getPassword())) {
                    return Response.success();
                }
                throw new ServiceException(BusinessStatusEnum.ERROR.getCode(), "密码错误！");
            }
            throw new ServiceException(500, "密码不能为空！");
        }
        throw new ServiceException(500, "此员工不存在！");
    }

    public ResponseDTO updatePassword(Staff staff) {
        // MD5加密
        staff.setPassword(MD5Util.MD55(staff.getPassword()));
        if (updateById(staff)) {
            // 密码是 JWT 的签名密钥，改密后旧 token 已不可信，必须清缓存并撤销登录态
            evictCache(staff.getId(), true);
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 获取员工信息
     *
     * @param id
     * @return
     */
    public ResponseDTO findInfoById(Integer id) {
        StaffDeptVO staffInfo = this.staffMapper.findInfo(id);
        if (staffInfo != null) {
            return Response.success(staffInfo);
        }
        return Response.error();
    }
}
