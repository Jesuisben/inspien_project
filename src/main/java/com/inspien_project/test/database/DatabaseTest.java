package com.inspien_project.test.database;

import com.inspien_project.common.EnvSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

public class DatabaseTest {

    // IntelliJ의 실행 버튼에서 시작하는 진입점이며 이 메서드가 호출하는 작업만 실행
    public static void main(String[] args) {
        try {
            var envSettings = EnvSettings.load();
            StringBuilder report = new StringBuilder();
            // 조회 결과를 바꾸지 않는 SQL과 DB 메타데이터로 컬럼·제품·기본키를 확인
            checkTable(envSettings.getOrderDbUrl(), envSettings.getOrderDbUsername(), envSettings.getOrderDbPassword(), "ORDER_TB", report);
            Path path = Path.of("private/SchemaReport.txt");
            Files.createDirectories(path.getParent());
            Files.writeString(path, report, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            JOptionPane.showMessageDialog(null, "DB 읽기 점검 완료: " + path.toAbsolutePath()
                    + "\n테이블과 컬럼 형식 및 실제 기본키를 확인하세요.");
        } catch (Exception error) {
            JOptionPane.showMessageDialog(null, "DB 점검 실패: " + error.getClass().getSimpleName()
                    + "\nJDBC 드라이버, URL, 계정, 테이블 접근 권한을 확인하세요.");
        }
    }

    // 조회 결과를 바꾸지 않는 SQL과 DB 메타데이터로 컬럼·제품·기본키를 확인
    private static void checkTable(String url, String username, String password, String table,
            StringBuilder report) throws Exception {
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE 1 = 0")) {
            statement.setQueryTimeout(10);
            report.append(table).append(" / ").append(connection.getMetaData().getDatabaseProductName()).append('\n');
            try (var result = statement.executeQuery()) {
                var columns = result.getMetaData();
                for (int index = 1; index <= columns.getColumnCount(); index++) {
                    report.append(columns.getColumnName(index)).append(" : ")
                            .append(columns.getColumnTypeName(index)).append("(")
                            .append(columns.getPrecision(index)).append(")\n");
                }
            }
            try (var keys = connection.getMetaData().getPrimaryKeys(connection.getCatalog(), null, table)) {
                while (keys.next()) report.append("PK: ").append(keys.getString("COLUMN_NAME")).append('\n');
            }
            report.append("PK가 비어 있으면 DB 클라이언트에서 확인\n\n");
        }
    }
}
