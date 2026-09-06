package com.ielts.courses.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unionid", unique = true, length = 64)
    private String unionid;

    @Column(name = "openid_h5", length = 64)
    private String openidH5;

    @Column(name = "openid_web", length = 64)
    private String openidWeb;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getOpenidH5() {
        return openidH5;
    }

    public void setOpenidH5(String openidH5) {
        this.openidH5 = openidH5;
    }

    public String getOpenidWeb() {
        return openidWeb;
    }

    public void setOpenidWeb(String openidWeb) {
        this.openidWeb = openidWeb;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
