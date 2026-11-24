package com.cow.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cow.dto.Response;
import com.cow.dto.ResponseDTO;
import com.cow.entity.OperLog;
import com.cow.mapper.OperLogMapper;
import com.cow.util.OperLogFileWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务
 *
 * @Author : cow
 */
@Service
public class OperLogService extends ServiceImpl<OperLogMapper, OperLog> {

    private static final Logger log = LoggerFactory.getLogger(OperLogService.class);

    @Resource
    private OperLogFileWriter operLogFileWriter;

    /**
     * 异步落库，避免拖慢业务请求；失败只告警不抛出。
     * 同时把日志追加写入按天的文本文件，便于在 IDEA 左侧项目树查看。
     */
    @Async
    public void recordAsync(OperLog operLog) {
        try {
            save(operLog);
        } catch (Exception e) {
            log.error("操作日志落库失败: {}", e.getMessage());
        }
        try {
            operLogFileWriter.write(operLog);
        } catch (Exception e) {
            log.warn("操作日志写文件失败: {}", e.getMessage());
        }
    }

    /**
     * 分页条件查询，按操作时间倒序
     */
    public ResponseDTO list(Integer current, Integer size, String title, String operName, Integer status) {
        Page<OperLog> page = new Page<>(current, size);
        QueryWrapper<OperLog> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(title)) {
            queryWrapper.like("title", title);
        }
        if (StrUtil.isNotBlank(operName)) {
            queryWrapper.like("oper_name", operName);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("oper_time");
        Page<OperLog> result = page(page, queryWrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        return Response.success(data);
    }

    public ResponseDTO deleteById(Integer id) {
        return removeById(id) ? Response.success("删除成功") : Response.error("删除失败");
    }

    public ResponseDTO deleteBatch(List<Integer> ids) {
        return removeByIds(ids) ? Response.success("批量删除成功") : Response.error("批量删除失败");
    }

    /**
     * 清空日志（物理删除，日志表不做逻辑删除）
     */
    public ResponseDTO clear() {
        int count = baseMapper.delete(new QueryWrapper<>());
        return Response.success("已清空 " + count + " 条日志");
    }
}
