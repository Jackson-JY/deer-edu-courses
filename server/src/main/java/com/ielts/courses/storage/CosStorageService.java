package com.ielts.courses.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.ielts.courses.common.ApiException;
import com.ielts.courses.common.BizCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 腾讯云 COS 文件存储：后端中转上传（密钥不下发到浏览器，无需给存储桶配置 CORS）。
 * 封面、课件等小文件经后端接收后直传 COS，返回对象 Key 与可访问 URL。
 */
@Service
public class CosStorageService {

    @Value("${app.tencent.cos.bucket:}")
    private String bucket;

    @Value("${app.tencent.cos.region:ap-guangzhou}")
    private String region;

    @Value("${app.tencent.cos.preview-url:}")
    private String previewUrl;

    @Value("${app.tencent.cos.secret-id:}")
    private String secretId;

    @Value("${app.tencent.cos.secret-key:}")
    private String secretKey;

    private volatile COSClient cosClient;

    public boolean isConfigured() {
        return bucket != null && !bucket.isBlank()
                && secretId != null && !secretId.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }

    private COSClient client() {
        if (cosClient == null) {
            synchronized (this) {
                if (cosClient == null) {
                    COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
                    ClientConfig config = new ClientConfig(new Region(region));
                    config.setHttpProtocol(HttpProtocol.https);
                    cosClient = new COSClient(cred, config);
                }
            }
        }
        return cosClient;
    }

    public record UploadedFile(String key, String url) {
    }

    /**
     * 上传文件到 COS 指定目录。
     *
     * @param file 前端上传的文件
     * @param dir  对象 Key 前缀目录，如 "covers" / "materials/3"
     */
    public UploadedFile upload(MultipartFile file, String dir) {
        if (!isConfigured()) {
            throw new ApiException(BizCode.BAD_REQUEST, "文件存储未配置：请先在后台配置腾讯云 COS（COS_BUCKET / COS_REGION）");
        }
        if (file == null || file.isEmpty()) {
            throw new ApiException(BizCode.BAD_REQUEST, "上传文件为空");
        }

        String ext = extensionOf(file.getOriginalFilename());
        String key = dir + "/" + UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        // 按扩展名设置精确的 Content-Type（不依赖浏览器上传时可能给错的值）
        String contentType = contentTypeOf(ext);
        metadata.setContentType(contentType);
        // 强制内联显示，防止浏览器把 PDF/图片当附件下载
        metadata.setContentDisposition("inline");

        try {
            PutObjectRequest request = new PutObjectRequest(bucket, key, file.getInputStream(), metadata);
            client().putObject(request);
        } catch (IOException e) {
            throw new RuntimeException("读取上传文件失败", e);
        } catch (CosClientException e) {
            throw new RuntimeException("上传到腾讯云 COS 失败：" + e.getMessage(), e);
        }

        return new UploadedFile(key, publicUrl(key));
    }

    /** 拼接可访问 URL：优先使用配置的预览/CDN 域名，否则用 COS 默认域名。 */
    private String publicUrl(String key) {
        if (previewUrl != null && !previewUrl.isBlank()) {
            String base = previewUrl.endsWith("/") ? previewUrl.substring(0, previewUrl.length() - 1) : previewUrl;
            return base + "/" + key;
        }
        return "https://" + bucket + ".cos." + region + ".myqcloud.com/" + key;
    }

    private String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase();
    }

    /** 按扩展名映射 MIME 类型，未知扩展名回退为通用二进制。 */
    private String contentTypeOf(String ext) {
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp3" -> "audio/mpeg";
            case "aac" -> "audio/aac";
            case "m4a" -> "audio/mp4";
            case "wav" -> "audio/wav";
            default -> "application/octet-stream";
        };
    }
}
