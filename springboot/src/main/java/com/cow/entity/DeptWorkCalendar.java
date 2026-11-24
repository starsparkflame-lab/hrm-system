package com.cow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 部门上班日历实体类
 */
@Data
@TableName("dept_work_calendar")
public class DeptWorkCalendar implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 部门ID
     */
    private Integer deptId;

    /**
     * 上班日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    /**
     * 是否工作日:1-工作日,0-休息日
     */
    private Integer isWorkday;

    /**
     * 预期状态:1-正常,2-迟到,3-早退,4-缺勤,5-休假
     */
    private Integer expectedStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;
}
