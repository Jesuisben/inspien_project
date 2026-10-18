package com.inspien_project.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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

    public void uploadFile(Path localFile, String remoteName) throws IOException {
        var settings = envSettings;
        FTPClient ftp = new FTPClient();
        try {
            ftp.setControlEncoding(settings.getFtpControlEncoding());
            ftp.setConnectTimeout(5_000);
            ftp.setDefaultTimeout(10_000);
            ftp.setDataTimeout(Duration.ofSeconds(10));
            ftp.connect(settings.getFtpHost(), settings.getFtpPort());
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) throw new IOException("FTP_CONNECT");
            ftp.setSoTimeout(10_000);
            if (!ftp.login(settings.getFtpUsername(), settings.getFtpPassword())) throw new IOException("FTP_LOGIN");
            ftp.enterLocalPassiveMode();
            if (!ftp.setFileType(FTP.BINARY_FILE_TYPE)) throw new IOException("FTP_BINARY");
            if (!ftp.changeWorkingDirectory(settings.getFtpDirectory())) throw new IOException("ftpDirectory");
            var existing = ftp.listFiles();
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) throw new IOException("FTP_LIST");
            for (var file : existing) {
                if (remoteName.equals(file.getName())) throw new IOException("FTP_FILE_ALREADY_EXISTS");
            }
            try (InputStream input = Files.newInputStream(localFile)) {
                if (!ftp.storeFile(remoteName, input)) throw new IOException("FTP_STORE");
            }
            var stored = ftp.listFiles(remoteName);
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()) || stored.length != 1
                    || stored[0].getSize() != Files.size(localFile)) {
                throw new IOException("FTP_VERIFY");
            }
        } catch (IOException error) {
            log.error("stage=FTP_TRANSFER result=FAILURE replyCode={} errorType={}",
                    ftp.getReplyCode(), error.getClass().getSimpleName());
            throw error;
        } finally {
            if (ftp.isConnected()) {
                try { ftp.disconnect(); }
                catch (IOException error) { log.warn("stage=FTP_DISCONNECT result=FAILURE"); }
            }
        }
    }
}
