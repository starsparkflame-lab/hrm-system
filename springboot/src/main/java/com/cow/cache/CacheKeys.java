package com.cow.cache;

/**
 * 缓存键与过期时间的统一定义，避免各处硬编码字符串导致键名不一致
 *
 * @Author : cow
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /* ------------------------- 过期时间（秒） ------------------------- */

    /** 图形验证码有效期：5分钟 */
    public static final long CAPTCHA_TTL = 5 * 60L;

    /** 找回密码短信验证码有效期：5分钟 */
    public static final long SMS_RESET_TTL = 5 * 60L;

    /** 短信验证码发送间隔：60秒，防止重复发送 */
    public static final long SMS_SEND_INTERVAL_TTL = 60L;

    /** 登录 token 白名单有效期：与 JWT 过期时间保持一致（1小时） */
    public static final long TOKEN_TTL = 60 * 60L;

    /** 员工信息缓存：10分钟，数据变更时会主动清理 */
    public static final long STAFF_TTL = 10 * 60L;

    /** 部门作息时间缓存：10分钟，部门或员工所属部门变更时会主动清理 */
    public static final long DEPT_TIME_TTL = 10 * 60L;

    /** 首页实时统计缓存：60秒，靠短 TTL 自愈，避免与业务写操作强耦合 */
    public static final long HOME_STAT_TTL = 60L;

    /** 城市社保数据缓存：1小时，属于近乎静态的基础数据 */
    public static final long CITY_TTL = 60 * 60L;

    /* ------------------------- 键名构造 ------------------------- */

    /** 首页员工入职统计（按当前年份聚合，全局唯一） */
    public static final String HOME_STAFF = "hrm:home:staff";

    /** 首页城市社保数据 */
    public static final String HOME_CITY = "hrm:home:city";

    /** 首页部门人数分布 */
    public static final String HOME_DEPARTMENT = "hrm:home:department";

    /**
     * 图形验证码
     *
     * @param captchaKey 下发给前端的一次性标识
     */
    public static String captcha(String captchaKey) {
        return "hrm:captcha:" + captchaKey;
    }

    /**
     * 找回密码短信验证码
     *
     * @param phone 手机号
     */
    public static String smsResetCode(String phone) {
        return "hrm:sms:reset:code:" + phone;
    }

    /**
     * 找回密码短信验证码的发送间隔标记
     *
     * @param phone 手机号
     */
    public static String smsResetLimit(String phone) {
        return "hrm:sms:reset:limit:" + phone;
    }

    /**
     * 员工信息
     *
     * @param staffId 员工id
     */
    public static String staff(Integer staffId) {
        return "hrm:staff:" + staffId;
    }

    /**
     * 登录 token 白名单，值为该员工当前有效的 token
     * <p>
     * 以此实现主动登出与踢人下线：删除该键即可让对应 token 立即失效
     *
     * @param staffId 员工id
     */
    public static String token(Integer staffId) {
        return "hrm:token:" + staffId;
    }

    /**
     * 员工所属部门的作息时间配置
     *
     * @param staffId 员工id
     */
    public static String deptTime(Integer staffId) {
        return "hrm:dept:time:" + staffId;
    }

    /**
     * 首页统计数据，按部门与登录类型区分可见范围
     *
     * @param deptId    部门id，可为 null
     * @param loginType 登录类型
     */
    public static String homeCount(Integer deptId, String loginType) {
        return "hrm:home:count:" + deptId + ":" + loginType;
    }
}
