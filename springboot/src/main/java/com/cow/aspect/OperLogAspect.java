package com.cow.aspect;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.auth0.jwt.JWT;
import com.cow.annotation.OperationLog;
import com.cow.dto.ResponseDTO;
import com.cow.entity.OperLog;
import com.cow.entity.Staff;
import com.cow.service.OperLogService;
import com.cow.service.StaffService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志切面：自动记录所有控制器的写操作（POST/PUT/DELETE）。
 * <p>
 * 方法上标注 {@link OperationLog} 时使用自定义标题与业务类型；
 * 未标注时标题由“模块名 + 动作”推导（如 员工管理新增）。
 * 查询类请求不记录，避免日志刷屏。
 *
 * @Author : cow
 */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    /**
     * 参数/结果落库的最大长度，防止超长内容撑爆 text 字段
     */
    private static final int MAX_LEN = 2000;

    /**
     * 敏感字段一律不写入日志
     */
    private static final SimplePropertyPreFilter PARAM_FILTER = new SimplePropertyPreFilter();

    static {
        PARAM_FILTER.getExcludes().add("password");
        PARAM_FILTER.getExcludes().add("captcha");
        PARAM_FILTER.getExcludes().add("captchaKey");
    }

    /**
     * 控制器 -> 模块中文名，用于推导默认日志标题
     */
    private static final Map<String, String> MODULE = new HashMap<>();

    static {
        MODULE.put("StaffController", "员工管理");
        MODULE.put("DeptController", "部门管理");
        MODULE.put("RoleController", "角色管理");
        MODULE.put("MenuController", "菜单管理");
        MODULE.put("DocsController", "文件管理");
        MODULE.put("SalaryController", "薪资管理");
        MODULE.put("SalaryDeductController", "薪资扣除");
        MODULE.put("CityController", "参保城市");
        MODULE.put("InsuranceController", "五险一金");
        MODULE.put("LeaveController", "请假审批");
        MODULE.put("StaffLeaveController", "员工请假");
        MODULE.put("AttendanceController", "考勤管理");
        MODULE.put("CheckinController", "打卡");
        MODULE.put("OvertimeController", "加班管理");
        MODULE.put("CalendarController", "工作日历");
        MODULE.put("DeptWorkCalendarController", "部门工作日历");
        MODULE.put("LoginController", "登录");
        MODULE.put("RegisterController", "注册");
        MODULE.put("OperLogController", "操作日志");
    }

    @Resource
    private OperLogService operLogService;

    @Resource
    private StaffService staffService;

    @Pointcut("execution(* com.cow.controller..*.*(..))")
    public void controllerPoint() {
    }

    @Around("controllerPoint()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return point.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        String httpMethod = request.getMethod();
        // 只记录写操作
        if (!"POST".equals(httpMethod) && !"PUT".equals(httpMethod) && !"DELETE".equals(httpMethod)) {
            return point.proceed();
        }

        long start = System.currentTimeMillis();
        OperLog operLog = build(point, request, httpMethod);
        try {
            Object result = point.proceed();
            fillResult(operLog, result);
            return result;
        } catch (Throwable e) {
            operLog.setStatus(0);
            operLog.setJsonResult(truncate(e.getClass().getSimpleName() + ":" + e.getMessage(), MAX_LEN));
            throw e;
        } finally {
            operLog.setCostTime(System.currentTimeMillis() - start);
            operLog.setOperTime(new Timestamp(System.currentTimeMillis()));
            try {
                this.operLogService.recordAsync(operLog);
            } catch (Exception e) {
                // 日志记录失败绝不影响业务
                log.warn("操作日志记录失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 组装日志基础信息
     */
    private OperLog build(ProceedingJoinPoint point, HttpServletRequest request, String httpMethod) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        String className = point.getTarget().getClass().getSimpleName();

        OperLog operLog = new OperLog();
        operLog.setTitle(annotation != null ? annotation.title() : defaultTitle(className, httpMethod));
        operLog.setBusinessType(annotation != null ? annotation.businessType() : defaultType(httpMethod));
        operLog.setMethod(className + "." + method.getName());
        operLog.setRequestMethod(httpMethod);
        operLog.setOperUrl(truncate(request.getRequestURI(), 255));
        operLog.setOperIp(getIp(request));
        fillOperator(operLog, request, point.getArgs());
        operLog.setOperParam(truncate(toParam(point.getArgs()), MAX_LEN));
        return operLog;
    }

    /**
     * 操作人：优先从 token 解析；无 token 的场景（登录/注册）退化为参数中的账号
     */
    private void fillOperator(OperLog operLog, HttpServletRequest request, Object[] args) {
        String token = request.getHeader("token");
        if (StrUtil.isNotBlank(token)) {
            try {
                Integer id = Integer.valueOf(JWT.decode(token).getAudience().get(0));
                Staff staff = this.staffService.getByIdWithCache(id);
                if (staff != null) {
                    operLog.setOperId(id);
                    operLog.setOperName(staff.getName());
                    return;
                }
            } catch (Exception e) {
                log.debug("操作日志解析 token 失败，忽略: {}", e.getMessage());
            }
        }
        for (Object arg : args) {
            if (arg instanceof Staff && StrUtil.isNotBlank(((Staff) arg).getCode())) {
                operLog.setOperName(((Staff) arg).getCode());
                return;
            }
        }
    }

    /**
     * 序列化请求参数，跳过无法/不应序列化的类型与敏感字段
     */
    private String toParam(Object[] args) {
        List<Object> list = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null || arg instanceof ServletRequest || arg instanceof ServletResponse || arg instanceof MultipartFile) {
                continue;
            }
            list.add(arg);
        }
        if (list.isEmpty()) {
            return "";
        }
        try {
            return JSON.toJSONString(list, PARAM_FILTER);
        } catch (Exception e) {
            return String.valueOf(list);
        }
    }

    /**
     * 记录返回结果；业务码非 200 视为失败
     */
    private void fillResult(OperLog operLog, Object result) {
        if (result instanceof ResponseDTO) {
            ResponseDTO dto = (ResponseDTO) result;
            operLog.setStatus(dto.getCode() == 200 ? 1 : 0);
            operLog.setJsonResult(truncate("code=" + dto.getCode() + ",message=" + dto.getMessage(), MAX_LEN));
        } else {
            operLog.setStatus(1);
            operLog.setJsonResult(truncate(String.valueOf(result), MAX_LEN));
        }
    }

    private String defaultTitle(String className, String httpMethod) {
        String module = MODULE.getOrDefault(className, className);
        return module + actionOf(httpMethod);
    }

    private String actionOf(String httpMethod) {
        switch (httpMethod) {
            case "POST":
                return "新增";
            case "PUT":
                return "修改";
            case "DELETE":
                return "删除";
            default:
                return "操作";
        }
    }

    private int defaultType(String httpMethod) {
        switch (httpMethod) {
            case "POST":
                return 1;
            case "PUT":
                return 2;
            case "DELETE":
                return 3;
            default:
                return 0;
        }
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
