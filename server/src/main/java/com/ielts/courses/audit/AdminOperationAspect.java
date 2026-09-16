package com.ielts.courses.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ielts.courses.auth.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员操作审计切面：自动采集 /api/admin/** 的写操作（POST/PUT/DELETE）。
 * 操作者身份只从服务端 JWT 安全上下文获取，不信任任何前端传参。
 */
@Aspect
@Component
public class AdminOperationAspect {

    private static final int PARAMS_MAX = 4000;

    private final EventLogService eventLogService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AdminOperationAspect(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    // 注意：不能写成 within(@RestController com.ielts.courses.admin..*) —— AspectJ 对
    // 「注解+包限定」组合的解析会使包限定失效，从而匹配所有 @RestController。
    // 这里用纯包 within + 单独列出包外的管理端 Controller，已用 AspectJ PointcutParser 实测精确匹配。
    @Around("within(com.ielts.courses.admin..*) || " +
            "execution(* com.ielts.courses.writing.WritingAdminController.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = WebUtils.currentRequest();
        // 只审计写操作；请求上下文缺失（内部调用）时直接放行
        if (request == null || isReadOnly(request.getMethod())) {
            return pjp.proceed();
        }
        // 双保险：非 /api/admin/ 路径一律不记（防止切点误匹配扩散）
        if (!request.getRequestURI().startsWith("/api/admin/")) {
            return pjp.proceed();
        }

        long start = System.currentTimeMillis();
        AdminOperationLog entry = new AdminOperationLog();
        entry.setMethod(request.getMethod());
        entry.setPath(WebUtils.truncate(request.getRequestURI(), 255));
        entry.setParams(serializeArgs(pjp.getArgs()));

        LoginUser admin = currentAdmin();
        if (admin != null) {
            entry.setAdminUserId(admin.userId());
            entry.setAdminName(WebUtils.truncate(eventLogService.resolveDisplayName(admin.userId()), 64));
        }
        entry.setIp(WebUtils.clientIp(request));
        entry.setUserAgent(WebUtils.userAgent(request));
        entry.setModule(resolveModule(request.getRequestURI()));
        entry.setAction(resolveAction(request.getMethod(), request.getRequestURI()));

        Object result;
        try {
            result = pjp.proceed();
            entry.setSuccess(1);
        } catch (Throwable ex) {
            entry.setSuccess(0);
            entry.setError(WebUtils.truncate(ex.getMessage(), 500));
            throw ex;
        } finally {
            entry.setDurationMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start));
            eventLogService.saveAdminOperation(entry);
        }
        return result;
    }

    private boolean isReadOnly(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method);
    }

    private LoginUser currentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof LoginUser u) {
            return u;
        }
        return null;
    }

    private String resolveModule(String uri) {
        if (uri.startsWith("/api/admin/courses")) return "课程管理";
        if (uri.startsWith("/api/admin/users")) return "用户管理";
        if (uri.startsWith("/api/admin/vod")) return "视频点播";
        if (uri.startsWith("/api/admin/security")) return "安全策略";
        if (uri.startsWith("/api/admin/upload")) return "文件上传";
        if (uri.startsWith("/api/admin/dashboard")) return "工作台";
        if (uri.startsWith("/api/admin/logs")) return "日志审计";
        if (uri.startsWith("/api/admin/writing")) return "写作批改";
        return "其他";
    }

    private String resolveAction(String method, String uri) {
        // 课程
        if (uri.matches("/api/admin/courses/\\d+/publish")) return "上架课程";
        if (uri.matches("/api/admin/courses/\\d+/offline")) return "下架课程";
        if (uri.matches("/api/admin/courses/\\d+/chapters/\\d+/lessons")) return "新建课时";
        if (uri.matches("/api/admin/courses/\\d+/lessons/\\d+")) {
            return "PUT".equalsIgnoreCase(method) ? "编辑课时" : "删除课时";
        }
        if (uri.matches("/api/admin/courses/\\d+/chapters/\\d+")) {
            return "PUT".equalsIgnoreCase(method) ? "编辑章节" : "删除章节";
        }
        if (uri.matches("/api/admin/courses/\\d+/chapters")) return "新建章节";
        if (uri.matches("/api/admin/courses/\\d+/materials/\\d+")) return "删除课件";
        if (uri.matches("/api/admin/courses/\\d+/materials")) return "新建课件";
        if (uri.matches("/api/admin/courses/\\d+")) {
            return switch (method) {
                case "PUT" -> "编辑课程";
                case "DELETE" -> "删除课程";
                default -> "课程操作";
            };
        }
        if (uri.equals("/api/admin/courses")) return "新建课程";
        // 用户
        if (uri.matches("/api/admin/users/\\d+/role")) return "修改用户角色";
        if (uri.matches("/api/admin/users/\\d+/entitlements")) return "开通/编辑学习权限";
        // 写作批改权限
        if (uri.matches("/api/admin/writing/users/\\d+/grant")) {
            return "DELETE".equalsIgnoreCase(method) ? "撤销写作权限" : "开通写作权限";
        }
        // 视频/上传/安全
        if (uri.matches("/api/admin/vod/files/[^/]+/transcode")) return "触发视频转码";
        if (uri.equals("/api/admin/upload/cover")) return "上传课程封面";
        if (uri.matches("/api/admin/courses/\\d+/materials/upload")) return "上传课件";
        if (uri.startsWith("/api/admin/security")) return "更新安全策略";
        // 兜底
        return switch (method) {
            case "POST" -> "新建操作";
            case "PUT" -> "更新操作";
            case "DELETE" -> "删除操作";
            default -> "其他操作";
        };
    }

    /**
     * 序列化方法参数为 JSON：剔除不可序列化对象（文件/Servlet/流），
     * 并对密码、密钥类字段脱敏，超长截断。
     */
    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) return null;
        try {
            List<Object> sanitized = new ArrayList<>();
            for (Object arg : args) {
                sanitized.add(sanitize(arg));
            }
            JsonNode node = mapper.valueToTree(sanitized);
            maskSecrets(node);
            String json = mapper.writeValueAsString(node);
            return WebUtils.truncate(json, PARAMS_MAX);
        } catch (Exception e) {
            return null;
        }
    }

    private Object sanitize(Object arg) {
        if (arg == null) return null;
        if (arg instanceof MultipartFile mf) {
            return "[文件: " + mf.getOriginalFilename() + "]";
        }
        String cn = arg.getClass().getName();
        if (arg instanceof byte[]
                || cn.startsWith("jakarta.servlet.")
                || cn.startsWith("org.springframework.web.multipart.")
                || cn.startsWith("java.io.")
                || arg instanceof LoginUser
                || cn.startsWith("org.springframework.validation.")) {
            return arg instanceof byte[] ? "[bytes]" : "[" + arg.getClass().getSimpleName() + "]";
        }
        return arg;
    }

    private void maskSecrets(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fields().forEachRemaining(entry -> {
                String key = entry.getKey().toLowerCase();
                if ((key.contains("password") || key.contains("secret") || key.contains("token"))
                        && entry.getValue().isValueNode()) {
                    obj.set(entry.getKey(), JsonNodeFactory.instance.textNode("***"));
                } else {
                    maskSecrets(entry.getValue());
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::maskSecrets);
        }
    }
}
