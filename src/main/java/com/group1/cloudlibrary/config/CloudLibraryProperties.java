package com.group1.cloudlibrary.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务自定义配置（对应 application.yml 中的 cloud-library.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloud-library")
public class CloudLibraryProperties {

    private Jwt jwt = new Jwt();
    private Borrow borrow = new Borrow();
    private Reservation reservation = new Reservation();
    private Upload upload = new Upload();
    private Vector vector = new Vector();
    private Ai ai = new Ai();
    private Email email = new Email();

    @Data
    public static class Jwt {
        private String secret = "cloud-library-system-jwt-secret-key-2026-group1";
        private Long expireHours = 24L;
        private String header = "Authorization";
        private String prefix = "Bearer ";
    }

    @Data
    public static class Borrow {
        /** 借阅期限（天） */
        private Integer days = 30;
        /** 续借延长天数 */
        private Integer renewDays = 15;
        /** 最多续借次数 */
        private Integer renewLimit = 1;
        /** 应还日前 N 天内可续借 */
        private Integer renewBeforeDays = 3;
    }

    @Data
    public static class Reservation {
        /** 可借阅后保留小时数 */
        private Integer keepHours = 24;
    }

    @Data
    public static class Upload {
        private String path = "./uploads/";
        private String urlPrefix = "/uploads/";
    }

    @Data
    public static class Vector {
        /** 是否启用 Milvus 向量检索 */
        private Boolean enabled = false;
        private String milvusHost = "localhost";
        private Integer milvusPort = 19530;
        private String collection = "book_vectors";
        private Integer dimension = 1536;
    }

    @Data
    public static class Ai {
        /** 是否启用真实大模型 API */
        private Boolean enabled = false;
        /**
         * 接口地址不再可配置：对话固定请求智谱 GLM（见 AiService.CHAT_URL），
         * 这里只保留 API Key / 模型名等需要按环境替换的项。
         */
        private String apiKey = "";
        /** 对话模型名 */
        private String model = "glm-4.7-flash";
        /** 采样温度：越低越稳定，问答场景 0.7 左右即可 */
        private Double temperature = 0.7;
        /**
         * 向量接口地址，实际请求为 embeddingBaseUrl + /embeddings。
         * 留空即跳过向量调用，语义搜索自动降级为关键词检索（千问 embedding 需单独开通，暂未启用）；
         * 如需真实向量检索，可填写对应的 embedding 服务地址与模型名。
         */
        private String embeddingBaseUrl = "";
        private String embeddingModel = "";
        private Integer timeoutSeconds = 30;
    }

    @Data
    public static class Email {
        /** 是否真实发送邮件，关闭时验证码输出到控制台 */
        private Boolean enabled = false;
        private String fromName = "云图书管理系统";
    }
}
