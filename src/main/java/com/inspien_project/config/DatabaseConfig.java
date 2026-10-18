package com.inspien_project.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import com.inspien_project.common.EnvSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration

public class DatabaseConfig {

    @Bean("orderDataSource")
    DataSource orderDataSource(EnvSettings envSettings) {
        checkSetting(envSettings.getApplicantKey(), "applicantKey");
        checkSetting(envSettings.getName(), "NAME");
        if (!envSettings.getName().matches("[가-힣]+")) {
            throw new IllegalArgumentException("참여자명은 한글로 설정하세요.");
        }
        return createDataSource(new EnvSettings.Target(envSettings.getOrderDbUrl(), envSettings.getOrderDbUsername(), envSettings.getOrderDbPassword()));
    }

    @Bean("shipmentDataSource")
    DataSource shipmentDataSource(EnvSettings envSettings) {
        return createDataSource(new EnvSettings.Target(envSettings.getShipmentDbUrl(), envSettings.getShipmentDbUsername(), envSettings.getShipmentDbPassword()));
    }

    private DataSource createDataSource(EnvSettings.Target target) {
        if (target == null) throw new IllegalArgumentException("DB 설정이 없습니다.");
        checkSetting(target.getUrl(), "JDBC URL");
        checkSetting(target.getUsername(), "DB username");
        checkSetting(target.getPassword(), "DB password");
        if (!target.getUrl().startsWith("jdbc:")) {
            throw new IllegalArgumentException("DB URL은 JDBC URL이어야 합니다.");
        }
        return new DriverManagerDataSource(target.getUrl(), target.getUsername(), target.getPassword());
    }

    private void checkSetting(String value, String label) {
        if (value == null || value.isBlank() || value.contains("입력필요")) {
            throw new IllegalArgumentException(label + " 설정을 확인하세요.");
        }
    }

    @Bean("orderJdbc")
    JdbcTemplate orderJdbc(@Qualifier("orderDataSource") DataSource dataSource) {
        return createJdbcTemplate(dataSource);
    }

    @Bean("shipmentJdbc")
    JdbcTemplate shipmentJdbc(@Qualifier("shipmentDataSource") DataSource dataSource) {
        return createJdbcTemplate(dataSource);
    }

    private JdbcTemplate createJdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(10);
        return jdbcTemplate;
    }

    @Bean("orderTransaction")
    TransactionTemplate orderTransaction(@Qualifier("orderDataSource") DataSource dataSource) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        transactionTemplate.setTimeout(60);
        return transactionTemplate;
    }
}
