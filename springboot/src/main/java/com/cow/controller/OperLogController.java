package com.cow.controller;

import com.cow.dto.ResponseDTO;
import com.cow.service.OperLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 操作日志接口
 *
 * @Author : cow
 */
@RestController
@RequestMapping("/operlog")
public class OperLogController {

    @Resource
    private OperLogService operLogService;

    @ApiOperation("分页条件查询")
    @GetMapping
    public ResponseDTO list(@RequestParam(defaultValue = "1") Integer current,
                            @RequestParam(defaultValue = "10") Integer size,
                            String title, String operName, Integer status) {
        return this.operLogService.list(current, size, title, operName, status);
    }

    @ApiOperation("删除单条")
    @DeleteMapping("/{id}")
    public ResponseDTO deleteById(@PathVariable Integer id) {
        return this.operLogService.deleteById(id);
    }

    @ApiOperation("批量删除")
    @DeleteMapping("/batch/{ids}")
    public ResponseDTO deleteBatch(@PathVariable List<Integer> ids) {
        return this.operLogService.deleteBatch(ids);
    }

    @ApiOperation("清空日志")
    @DeleteMapping("/clear")
    public ResponseDTO clear() {
        return this.operLogService.clear();
    }
}
