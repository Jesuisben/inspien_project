
package com.inspien_project.test.order;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import com.inspien_project.mapper.OrderXmlMapper;

public class OrderXmlTest {

    public static void main(String[] args) {
        var chooser = new JFileChooser(Path.of("private").toAbsolutePath().toFile());
        chooser.setDialogTitle("저장 없이 검증할 XML 선택");
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
        try {
            var file = chooser.getSelectedFile().toPath();
            if (Files.size(file) > 1_048_576) throw new IllegalArgumentException("XML은 1MiB 이하여야 함");
            var result = new OrderXmlMapper().parse(Files.readAllBytes(file));
            JOptionPane.showMessageDialog(null, "XML 검증 성공 / 수신 ITEM: " + result.getTotalCount()
                    + "\n생성할 주문 행 수: " + result.getOrderList().size()
                    + "\nHEADER 없는 ITEM 제외: " + result.getRejectedCount()
                    + "\nITEM 없는 HEADER 경고: " + result.getEmptyHeaderCount());
        } catch (Exception error) {
            JOptionPane.showMessageDialog(null, "XML 검증 실패: " + error.getClass().getSimpleName()
                    + "\n필수 필드, 관계, 인코딩 확인 필요");
        }
    }
}