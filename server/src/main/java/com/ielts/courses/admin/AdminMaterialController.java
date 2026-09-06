package com.ielts.courses.admin;

import com.ielts.courses.catalog.Material;
import com.ielts.courses.catalog.MaterialRepository;
import com.ielts.courses.common.ApiResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/courses/{courseId}/materials")
public class AdminMaterialController {

    private final MaterialRepository materialRepository;

    public AdminMaterialController(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    @GetMapping
    public ApiResult<List<Material>> list(@PathVariable Long courseId) {
        return ApiResult.ok(materialRepository.findByCourseId(courseId));
    }

    @PostMapping
    public ApiResult<Material> create(@PathVariable Long courseId, @Valid @RequestBody MaterialReq req) {
        Material material = new Material();
        material.setCourseId(courseId);
        material.setLessonId(req.lessonId());
        material.setName(req.name());
        material.setType(req.type());
        material.setCosKey(req.cosKey());
        material.setPreviewOnly(req.previewOnly() == null || req.previewOnly() ? 1 : 0);
        return ApiResult.ok(materialRepository.save(material));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        materialRepository.deleteById(id);
        return ApiResult.ok();
    }

    public record MaterialReq(
            @NotBlank String name,
            @NotBlank String type,
            @NotBlank String cosKey,
            Long lessonId,
            Boolean previewOnly
    ) {
    }
}
