package com.cow.util;

import com.cow.entity.OperLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志文件输出器：把每条操作日志追加写入"按天"的文本文件，
 * 文件落在项目根目录（见 application.yml 的 operlog.file-dir），
 * 便于直接在 IDEA 左侧项目树中按日期浏览历史操作记录。
 *
 * @Author : cow
 */
@Component
public class OperLogFileWriter {

    private static final Logger log = LoggerFactory.getLogger(OperLogFileWriter.class);

    /** 业务类型文案，与前端页面保持一致 */
    private static final Map<Integer, String> BUSINESS_TEXT = new HashMap<>();

    static {
        BUSINESS_TEXT.put(0, "其他");
        BUSINESS_TEXT.put(1, "新增");
        BUSINESS_TEXT.put(2, "修改");
        BUSINESS_TEXT.put(3, "删除");
        BUSINESS_TEXT.put(4, "登录");
        BUSINESS_TEXT.put(5, "登出");
        BUSINESS_TEXT.put(6, "导出");
        BUSINESS_TEXT.put(7, "导入");
    }

    @Value("${operlog.file-dir:}")
    private String fileDir;

    /**
     * 追加一条操作日志到当天文件，失败只告警不影响业务
     */
    public void write(OperLog operLog) {
        if (operLog == null || fileDir == null || fileDir.trim().isEmpty()) {
            return;
        }
        Writer writer = null;
        try {
            File dir = new File(fileDir.trim());
            if (!dir.exists() && !dir.mkdirs()) {
                log.warn("操作日志目录创建失败: {}", dir.getAbsolutePath());
                return;
            }
            // SimpleDateFormat 非线程安全，异步多线程下每次新建局部实例
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
            String day = dayFormat.format(operLog.getOperTime() == null ? new Date() : operLog.getOperTime());
            File file = new File(dir, day + ".log");
            writer = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
            writer.write(format(operLog));
            writer.write(System.lineSeparator());
        } catch (Exception e) {
            log.warn("操作日志写文件失败: {}", e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignore) {
                    // 关闭失败忽略
                }
            }
        }
    }

    /**
     * 单行文本格式，便于人工阅读
     */
    private String format(OperLog o) {
        // SimpleDateFormat 非线程安全，异步多线程下每次新建局部实例
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Timestamp time = o.getOperTime() == null ? new Timestamp(System.currentTimeMillis()) : o.getOperTime();
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(timeFormat.format(time)).append("] ");
        sb.append("操作人=").append(nullToDash(o.getOperName()));
        sb.append(" | 标题=").append(nullToDash(o.getTitle()));
        sb.append(" | 类型=").append(BUSINESS_TEXT.getOrDefault(o.getBusinessType(), "其他"));
        sb.append(" | 请求=").append(nullToDash(o.getRequestMethod())).append(' ').append(nullToDash(o.getOperUrl()));
        sb.append(" | IP=").append(nullToDash(o.getOperIp()));
        sb.append(" | 状态=").append(o.getStatus() != null && o.getStatus() == 1 ? "成功" : "失败");
        sb.append(" | 耗时=").append(o.getCostTime() == null ? 0 : o.getCostTime()).append("ms");
        if (o.getOperParam() != null && !o.getOperParam().isEmpty()) {
            sb.append(" | 参数=").append(o.getOperParam());
        }
        return sb.toString();
    }

    private String nullToDash(String s) {
        return s == null || s.isEmpty() ? "-" : s;
    }
}
