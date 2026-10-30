package com.group1.cloudlibrary;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 云图书管理系统 启动类
 *
 * @author 一组（刘里萍、张语欣、朱文宣、许梓涵、王妍、孔令芬）
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.group1.cloudlibrary.mapper")
public class CloudLibrarySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudLibrarySystemApplication.class, args);
        // 用普通字符串拼接（不依赖 Java 15+ 的文本块），任何语言等级下都能编译
        System.out.println("============================================================");
        System.out.println("  云图书管理系统启动成功！");
        System.out.println("  后端接口:  http://localhost:8080/api/");
        System.out.println("  前端页面:  http://localhost:5173/");
        System.out.println("============================================================");
    }
}
