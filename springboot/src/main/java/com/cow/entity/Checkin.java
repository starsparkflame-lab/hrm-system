package com.cow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * 考勤签到实体类
 */
@Data
@Accessors(chain = true)
@TableName("att_checkin")
@ApiModel(value = "Checkin对象", description = "考勤签到")
public class Checkin implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("员工id")
    @TableField("staff_id")
    private Integer staffId;

    @ApiModelProperty("签到日期")
    @TableField("checkin_date")
    private Date checkinDate;

    @ApiModelProperty("签到时间")
    @TableField("checkin_time")
    private Time checkinTime;

    @ApiModelProperty("签到类型:1-上午上班,2-上午下班,3-下午上班,4-下午下班")
    @TableField("checkin_type")
    private Integer checkinType;

    @ApiModelProperty("签到位置")
    @TableField("location")
    private String location;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @ApiModelProperty("逻辑删除，0未删除，1删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;

}
