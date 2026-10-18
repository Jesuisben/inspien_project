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

    // 같은 형태의 설정에서 연결 대상을 만들되 실제 대상은 주입받은 order 또는 shipment로 구분
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

    // 연결 대상에 SQL 실행기를 붙이고 조회 제한 시간을 설정
    private JdbcTemplate createJdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(10);
        return jdbcTemplate;
    }

    // 반환하는 객체를 지정된 이름으로 등록하여 주입할 때 구분할 수 있게 함
    @Bean("orderTransaction")
    // 주문 INSERT 전체의 커밋·롤백을 묶으며 FTP 자체를 DB 트랜잭션에 포함시키지는 않음.
    TransactionTemplate orderTransaction(@Qualifier("orderDataSource") DataSource dataSource) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        transactionTemplate.setTimeout(60);
        return transactionTemplate;
    }
}
