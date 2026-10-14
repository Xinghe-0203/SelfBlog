package com.selfblog.config.db;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * MyBatis Plus 配置类
 * 【功能说明】
 *   1. 配置 Mapper 接口扫描路径
 *   2. 配置分页插件（根据 DB_TYPE 动态选择数据库类型）
 *   3. 配置 databaseIdProvider 以支持多数据库方言
 */
@Configuration
@MapperScan("com.selfblog.mapper")
public class MybatisPlusConfig {

    @Value("${DB_TYPE:mysql}")
    private String dbType;

    /**
     * 配置 MyBatis Plus 拦截器（分页插件）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        DbType type = "sqlite".equalsIgnoreCase(dbType) ? DbType.SQLITE : DbType.MYSQL;
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(type));
        return interceptor;
    }

    /**
     * 配置 DatabaseIdProvider，支持 XML 中的 databaseId 多数据库方言
     */
    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("MySQL", "mysql");
        properties.setProperty("SQLite", "sqlite");
        provider.setProperties(properties);
        return provider;
    }
}
