package com.cow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cow.entity.Checkin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.Date;
import java.sql.Time;
import java.util.List;

/**
 * 考勤签到Mapper
 */
public interface CheckinMapper extends BaseMapper<Checkin> {

    /**
     * 查询员工今日签到记录
     */
    @Select("SELECT * FROM att_checkin WHERE staff_id = #{staffId} AND checkin_date = #{date} AND is_deleted = 0 ORDER BY checkin_time")
    List<Checkin> findTodayCheckins(@Param("staffId") Integer staffId, @Param("date") Date date);

    /**
     * 查询员工是否已签到
     */
    @Select("SELECT COUNT(*) FROM att_checkin WHERE staff_id = #{staffId} AND checkin_date = #{date} AND checkin_type = #{type} AND is_deleted = 0")
    int countCheckin(@Param("staffId") Integer staffId, @Param("date") Date date, @Param("type") Integer type);

    /**
     * 插入签到记录
     */
    @Insert("INSERT INTO att_checkin(staff_id, checkin_date, checkin_time, checkin_type, location, create_time) " +
            "VALUES(#{staffId}, #{checkinDate}, #{checkinTime}, #{checkinType}, #{location}, NOW())")
    int insertCheckin(@Param("staffId") Integer staffId, 
                      @Param("checkinDate") Date checkinDate,
                      @Param("checkinTime") Time checkinTime,
                      @Param("checkinType") Integer checkinType,
                      @Param("location") String location);
}
