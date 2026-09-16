-- V5: 雅思写作批改（学生提交题目+作文，AI 生成草稿，教师审核发布）

-- 写作提交：题目由学生一并粘贴提交，不单独建题库
CREATE TABLE `writing_submission` (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  task_prompt TEXT         NOT NULL,               -- 学生粘贴的题目原文
  content     MEDIUMTEXT   NOT NULL,               -- 作文正文
  word_count  INT          NOT NULL DEFAULT 0,
  -- PENDING 待处理 / AI_DRAFTING AI生成中 / AWAITING_REVIEW 待教师审核 / CORRECTED 已批改 / FAILED 生成失败
  status      VARCHAR(24)  NOT NULL DEFAULT 'PENDING',
  ai_error    VARCHAR(500),
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ws_user (user_id, id),
  KEY idx_ws_status (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 批改结果：与提交一对一；DRAFT 为 AI/教师草稿，PUBLISHED 为已发布
CREATE TABLE `writing_correction` (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id  BIGINT       NOT NULL UNIQUE,
  corrector_id   BIGINT,                            -- 发布教师；AI 草稿阶段为空
  score_tr       DECIMAL(3,1),                      -- Task Response
  score_cc       DECIMAL(3,1),                      -- Coherence & Cohesion
  score_lr       DECIMAL(3,1),                      -- Lexical Resource
  score_ga       DECIMAL(3,1),                      -- Grammatical Range & Accuracy
  overall        DECIMAL(3,1),
  comment_tr     VARCHAR(1000),                     -- 分项评语（中文）
  comment_cc     VARCHAR(1000),
  comment_lr     VARCHAR(1000),
  comment_ga     VARCHAR(1000),
  closing_remark VARCHAR(500),                      -- 简短结语
  model_ideas    JSON,                              -- 范文思路提纲（分栏标题+要点）
  model_essay    MEDIUMTEXT,                        -- Model Essay 全文
  status         VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 行内批注：锚定作文字符偏移区间
CREATE TABLE `writing_annotation` (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  correction_id BIGINT        NOT NULL,
  seq           INT           NOT NULL,            -- 原文中的编号（按出现顺序）
  category      VARCHAR(2)    NOT NULL,            -- G 语法 / V 词汇 / C 衔接 / T 任务
  start_pos     INT           NOT NULL,
  end_pos       INT           NOT NULL,
  quote         VARCHAR(500),                      -- 引用原文片段（锚定校验）
  note          VARCHAR(2000),                     -- 批注说明（英文）
  suggestion    VARCHAR(500),                      -- 建议改写
  KEY idx_wa_corr (correction_id, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
