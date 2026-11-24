package com.cow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 操作日志实体，对应表 sys_oper_log
 *
 * @Author : cow
 */
@Data
@TableName("sys_oper_log")
@ApiModel(value = "OperLog对象", description = "操作日志")
public class OperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("模块/操作标题")
    @TableField("title")
    private String title;

    @ApiModelProperty("业务类型 0其他 1新增 2修改 3删除 4登录 5登出 6导出 7导入")
    @TableField("business_type")
    private Integer businessType;

    @ApiModelProperty("方法名")
    @TableField("method")
    private String method;

    @ApiModelProperty("请求方式")
    @TableField("request_method")
    private String requestMethod;

    @ApiModelProperty("请求URL")
    @TableField("oper_url")
    private String operUrl;

    @ApiModelProperty("操作IP")
    @TableField("oper_ip")
    private String operIp;

    @ApiModelProperty("操作人id")
    @TableField("oper_id")
    private Integer operId;

    @ApiModelProperty("操作人")
    @TableField("oper_name")
    private String operName;

    @ApiModelProperty("请求参数")
    @TableField("oper_param")
    private String operParam;

    @ApiModelProperty("返回结果")
    @TableField("json_result")
    private String jsonResult;

    @ApiModelProperty("1成功 0失败")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("耗时ms")
    @TableField("cost_time")
    private Long costTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("操作时间")
    @TableField("oper_time")
    private Timestamp operTime;
}
