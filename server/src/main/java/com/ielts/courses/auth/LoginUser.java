package com.ielts.courses.auth;

import java.util.List;

public record LoginUser(long userId, String sid, List<String> roles, boolean phoneBound) {

    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }

    public boolean isTeacher() {
        return roles != null && roles.contains("TEACHER");
    }
}
