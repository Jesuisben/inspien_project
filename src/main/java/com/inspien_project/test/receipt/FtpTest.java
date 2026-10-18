package com.inspien_project.test.receipt;

import com.inspien_project.common.EnvSettings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FtpTest {

    public static void main(String[] args) {
        JFileChooser chooser = new JFileChooser(Path.of("private/receipts").toAbsolutePath().toFile());
        chooser.setDialogTitle("대조할 로컬 영수증 선택");
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
        FTPClient ftp = new FTPClient();
        try {
            var settings = EnvSettings.load();
            ftp.setControlEncoding(settings.getFtpControlEncoding());
            ftp.setConnectTimeout(5_000);
            ftp.setDefaultTimeout(10_000);
            ftp.setDataTimeout(Duration.ofSeconds(10));
            ftp.connect(settings.getFtpHost(), settings.getFtpPort());
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) throw new IllegalStateException("FTP_CONNECT");
            ftp.setSoTimeout(10_000);
            if (!ftp.login(settings.getFtpUsername(), settings.getFtpPassword())) throw new IllegalStateException("FTP_LOGIN");
            ftp.enterLocalPassiveMode();
            if (!ftp.setFileType(FTP.BINARY_FILE_TYPE)
                    || !ftp.changeWorkingDirectory(settings.getFtpDirectory())) throw new IllegalStateException("FTP_CONFIG");
            Path local = chooser.getSelectedFile().toPath();
            Path folder = Path.of("private/downloads", UUID.randomUUID().toString());
            Files.createDirectories(folder);
            Path downloaded = folder.resolve(local.getFileName());
            try (var output = Files.newOutputStream(downloaded, java.nio.file.StandardOpenOption.CREATE_NEW)) {
                if (!ftp.retrieveFile(local.getFileName().toString(), output)) throw new IllegalStateException("FTP_DOWNLOAD");
            }
            if (Files.mismatch(local, downloaded) != -1) throw new IllegalStateException("FILE_BYTES_DIFFER");
            JOptionPane.showMessageDialog(null, "실제 FTP 다운로드와 로컬 영수증의 바이트가 일치합니다.\n"
                    + downloaded.toAbsolutePath());
        } catch (Exception error) {
            JOptionPane.showMessageDialog(null, "FTP 대조 실패: " + error.getClass().getSimpleName()
                    + "\n연결, 경로, 문자 인코딩, 업로드 결과를 확인하세요.");
        } finally {
            if (ftp.isConnected()) try { ftp.disconnect(); } catch (Exception ignored) { }
        }
    }
}
