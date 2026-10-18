package com.inspien_project.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.inspien_project.common.EnvSettings;

@Service
public class FtpService {

    private static final Logger log = LoggerFactory.getLogger(FtpService.class);
    private final EnvSettings envSettings;

    // 필수 FTP 값·포트·인코딩을 먼저 검증하며 생성자에서는 실제 서버에 연결하지 않음
    public FtpService(EnvSettings envSettings) {
        this.envSettings = envSettings;
        var settings = envSettings;
        if (settings == null) {
            throw new IllegalArgumentException("FTP 설정 확인");
        }
        for (String value : new String[]{settings.getFtpHost(), settings.getFtpUsername(),
                settings.getFtpPassword(), settings.getFtpDirectory()}) {
            if (value == null || value.isBlank() || value.contains("입력필요")) {
                throw new IllegalArgumentException("FTP 필수 설정 확인");
            }
        }
        if (settings.getFtpPort() < 1 || settings.getFtpPort() > 65535) {
            throw new IllegalArgumentException("FTP 포트 범위 확인");
        }
        java.nio.charset.Charset.forName(settings.getFtpControlEncoding());
    }

    // 호출한 쪽이 전송 완료 여부를 기다릴 수 있도록 전송 결과를 확인하고 실패를 예외로 전달
    public void uploadFile(Path localFile, String remoteName) throws IOException {
        var settings = envSettings;
        FTPClient ftp = new FTPClient();
        String temporaryName = remoteName + ".part-" + UUID.randomUUID();
        boolean uploaded = false;
        try {
            ftp.setControlEncoding(settings.getFtpControlEncoding());
            ftp.setConnectTimeout(5_000);
            ftp.setDefaultTimeout(10_000);
            ftp.setDataTimeout(Duration.ofSeconds(10));
            ftp.connect(settings.getFtpHost(), settings.getFtpPort());
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) throw new IOException("FTP_CONNECT");
            ftp.setSoTimeout(10_000);
            if (!ftp.login(settings.getFtpUsername(), settings.getFtpPassword())) throw new IOException("FTP_LOGIN");
            // 데이터 연결은 클라이언트가 시작하고, binary 모드로 LF·원본 바이트의 자동 변환을 막음
            ftp.enterLocalPassiveMode();
            if (!ftp.setFileType(FTP.BINARY_FILE_TYPE)) throw new IOException("FTP_BINARY");
            if (!ftp.changeWorkingDirectory(settings.getFtpDirectory())) throw new IOException("ftpDirectory");
            // 없는 파일명의 LIST가 550이 될 수 있어 현재 디렉터리에서 같은 이름을 찾음
            var existing = ftp.listFiles();
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) throw new IOException("FTP_LIST");
            for (var file : existing) {
                if (remoteName.equals(file.getName())) throw new IOException("FTP_FILE_ALREADY_EXISTS");
            }
            try (InputStream input = Files.newInputStream(localFile)) {
                if (!ftp.storeFile(temporaryName, input)) throw new IOException("FTP_STORE");
            }
            // 전송 중인 .part 파일과 완료 파일을 구분하고 이름 변경 응답·최종 크기까지 확인
            if (!ftp.rename(temporaryName, remoteName)) throw new IOException("FTP_RENAME");
            var stored = ftp.listFiles(remoteName);
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()) || stored.length != 1
                    || stored[0].getSize() != Files.size(localFile)) {
                throw new IOException("FTP_VERIFY");
            }
            uploaded = true;
        } finally {
            if (ftp.isConnected()) {
                if (!uploaded) {
                    // 이 요청이 만든 임시 파일만 정리하며 최종 파일은 임의로 지우지 않음
                    try { ftp.deleteFile(temporaryName); }
                    catch (IOException error) { log.warn("stage=FTP_TEMP_CLEANUP result=FAILURE"); }
                }
                try { ftp.disconnect(); }
                catch (IOException error) { log.warn("stage=FTP_DISCONNECT result=FAILURE"); }
            }
        }
    }
}
