package com.ielts.courses.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** 开启异步支持（写作 AI 草稿生成等耗时任务）。 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
