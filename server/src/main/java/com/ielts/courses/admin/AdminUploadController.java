package com.ielts.courses.admin;

import com.ielts.courses.catalog.Lesson;
import com.ielts.courses.catalog.LessonRepository;
import com.ielts.courses.catalog.Material;
import com.ielts.courses.catalog.MaterialRepository;
import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.ApiResult;
import com.ielts.courses.common.BizCode;
import com.ielts.courses.storage.CosStorageService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

/**
 * 管理端文件上传：封面图、课件文件。
 * 文件经后端中转上传到腾讯云 COS（/api/admin/** 已由 Spring Security 限制 ADMIN 角色）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUploadController {

    /** 课件支持在线预览的类型 */
    private static final Set<String> ALLOWED_MATERIAL_EXT =
            Set.of("pdf", "doc", "docx", "jpg", "jpeg", "png", "gif", "webp", "mp3", "aac", "m4a", "wav");
    private static final Set<String> ALLOWED_COVER_EXT =
            Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final CosStorageService cosStorage;
    private final MaterialRepository materialRepository;
    private final LessonRepository lessonRepository;

    public AdminUploadController(CosStorageService cosStorage, MaterialRepository materialRepository,
                                 LessonRepository lessonRepository) {
        this.cosStorage = cosStorage;
        this.materialRepository = materialRepository;
        this.lessonRepository = lessonRepository;
    }

    /** 上传课程封面图，返回可访问 URL（回填到 course.cover）。 */
    @PostMapping("/upload/cover")
    public ApiResult<Map<String, String>> uploadCover(@RequestParam("file") MultipartFile file) {
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_COVER_EXT.contains(ext)) {
            throw new ApiException(BizCode.BAD_REQUEST, "封面仅支持 JPG / PNG / WebP / GIF 图片");
        }
        CosStorageService.UploadedFile up = cosStorage.upload(file, "covers");
        return ApiResult.ok(Map.of("url", up.url(), "key", up.key()));
    }

    /** 上传课件文件到 COS 并登记为指定课时的课件（课件必须关联到具体课时）。 */
    @PostMapping("/courses/{courseId}/materials/upload")
    public ApiResult<Material> uploadMaterial(@PathVariable Long courseId,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "lessonId", required = false) Long lessonId) {
        if (lessonId == null) {
            throw new ApiException(BizCode.BAD_REQUEST, "请选择课件所属课时");
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException(BizCode.NOT_FOUND, "课时不存在"));
        if (!courseId.equals(lesson.getCourseId())) {
            throw new ApiException(BizCode.BAD_REQUEST, "课时不属于该课程");
        }

        String original = file.getOriginalFilename();
        String ext = extensionOf(original);
        if (!ALLOWED_MATERIAL_EXT.contains(ext)) {
            throw new ApiException(BizCode.BAD_REQUEST,
                    "课件仅支持 PDF、Word（doc/docx）、图片（JPG/PNG/WebP/GIF）或音频（MP3/AAC/M4A/WAV）");
        }

        CosStorageService.UploadedFile up = cosStorage.upload(file, "materials/" + courseId + "/" + lessonId);

        Material material = new Material();
        material.setCourseId(courseId);
        material.setLessonId(lessonId);
        material.setName(displayName(original));
        material.setType(inferType(ext));
        material.setCosKey(up.key());
        material.setPreviewOnly(1);
        return ApiResult.ok(materialRepository.save(material));
    }

    private String inferType(String ext) {
        return switch (ext) {
            case "pdf" -> "PDF";
            case "doc", "docx" -> "DOC";
            case "mp3", "aac", "m4a", "wav" -> "AUDIO";
            default -> "IMAGE";
        };
    }

    /** 保留原始文件名（含扩展名）作为课件名，去掉路径分隔符防止异常字符。 */
    private String displayName(String original) {
        if (original == null || original.isBlank()) return "未命名课件";
        String name = original.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.isBlank() ? "未命名课件" : name;
    }

    private String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase();
    }
}
