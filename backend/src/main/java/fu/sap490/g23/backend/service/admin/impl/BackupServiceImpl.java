package fu.sap490.g23.backend.service.admin.impl;

import fu.sap490.g23.backend.dto.response.admin.BackupCapabilityResponse;
import fu.sap490.g23.backend.dto.response.admin.BackupRecordResponse;
import fu.sap490.g23.backend.entity.admin.BackupRecord;
import fu.sap490.g23.backend.entity.admin.enums.BackupStatus;
import fu.sap490.g23.backend.repository.admin.BackupRecordRepository;
import fu.sap490.g23.backend.service.admin.AuditLogService;
import fu.sap490.g23.backend.service.admin.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {
    private static final String CONFIRMATION = "PHUC HOI DU LIEU";
    private static final byte[] POSTGRES_CUSTOM_MAGIC = {'P', 'G', 'D', 'M', 'P'};
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BackupRecordRepository repository;
    private final AuditLogService auditLogService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    @Value("${spring.datasource.username}")
    private String datasourceUsername;
    @Value("${spring.datasource.password:}")
    private String datasourcePassword;
    @Value("${englishlab.backup.directory:${user.dir}/data/backups}")
    private String configuredDirectory;
    @Value("${englishlab.backup.pg-dump-command:pg_dump}")
    private String pgDumpCommand;
    @Value("${englishlab.backup.pg-restore-command:pg_restore}")
    private String pgRestoreCommand;
    @Value("${englishlab.backup.maximum-upload-bytes:262144000}")
    private long maximumUploadBytes;
    @Value("${englishlab.backup.process-timeout-seconds:900}")
    private long processTimeoutSeconds;

    @Override
    public BackupCapabilityResponse capabilities() {
        String resolvedDump = resolveCommand(pgDumpCommand, "pg_dump");
        String resolvedRestore = resolveCommand(pgRestoreCommand, "pg_restore");
        String dumpVersion = commandVersion(resolvedDump);
        String restoreVersion = commandVersion(resolvedRestore);
        return BackupCapabilityResponse.builder()
                .backupAvailable(dumpVersion != null)
                .restoreAvailable(restoreVersion != null && dumpVersion != null)
                .pgDumpVersion(dumpVersion)
                .pgRestoreVersion(restoreVersion)
                .maximumUploadBytes(maximumUploadBytes)
                .restoreConfirmationPhrase(CONFIRMATION)
                .storageDirectory(backupDirectory().toString())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BackupRecordResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public BackupRecordResponse create(String actorEmail) {
        String dumpCommand = requireCommand(pgDumpCommand, "pg_dump");
        Path directory = backupDirectory();
        createDirectories(directory);
        String fileName = "englishlab-" + LocalDateTime.now().format(FILE_TIME) + "-"
                + Long.toUnsignedString(System.nanoTime(), 36) + ".backup";
        Path output = safeResolve(fileName);
        BackupRecord record = repository.save(BackupRecord.builder()
                .fileName(fileName)
                .status(BackupStatus.CREATING)
                .createdBy(actorEmail)
                .build());
        try {
            DatabaseTarget target = databaseTarget();
            runCommand(List.of(
                    dumpCommand,
                    "--format=custom",
                    "--no-owner",
                    "--no-privileges",
                    "--host=" + target.host(),
                    "--port=" + target.port(),
                    "--username=" + datasourceUsername,
                    "--file=" + output,
                    target.database()
            ));
            record.setFileSizeBytes(Files.size(output));
            record.setSha256(sha256(output));
            record.setStatus(BackupStatus.READY);
            record.setFailureReason(null);
            auditLogService.record(actorEmail, "BACKUP_CREATE", "SYSTEM_BACKUP", record.getId().toString(),
                    "Tạo bản sao lưu " + fileName + " (" + record.getFileSizeBytes() + " bytes).");
        } catch (Exception exception) {
            deleteQuietly(output);
            record.setStatus(BackupStatus.FAILED);
            record.setFailureReason(safeError(exception));
        }
        return toResponse(repository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource download(Long id) {
        BackupRecord record = requireRecord(id);
        if (record.getStatus() != BackupStatus.READY && record.getStatus() != BackupStatus.RESTORED) {
            throw new IllegalArgumentException("Bản sao lưu này chưa sẵn sàng để tải xuống.");
        }
        Path file = safeResolve(record.getFileName());
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("Tệp sao lưu không còn tồn tại trên máy chủ.");
        return new FileSystemResource(file);
    }

    @Override
    public BackupRecordResponse restore(
            String actorEmail,
            InputStream input,
            String originalFileName,
            long uploadSize,
            String confirmation
    ) {
        if (!CONFIRMATION.equals(confirmation)) {
            throw new IllegalArgumentException("Câu xác nhận phục hồi không chính xác.");
        }
        if (uploadSize <= 0 || uploadSize > maximumUploadBytes) {
            throw new IllegalArgumentException("Tệp phục hồi trống hoặc vượt quá dung lượng cho phép.");
        }
        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".backup")) {
            throw new IllegalArgumentException("Chỉ chấp nhận tệp .backup ở định dạng PostgreSQL custom.");
        }
        requireCommand(pgDumpCommand, "pg_dump");
        String restoreCommand = requireCommand(pgRestoreCommand, "pg_restore");

        BackupRecordResponse safetyBackup = create(actorEmail);
        if (safetyBackup.getStatus() != BackupStatus.READY) {
            throw new IllegalStateException("Không thể tạo bản sao lưu an toàn trước phục hồi; thao tác đã dừng.");
        }

        Path upload = null;
        BackupRecord restoreRecord = repository.save(BackupRecord.builder()
                .fileName("restore-upload-" + LocalDateTime.now().format(FILE_TIME) + "-"
                        + Long.toUnsignedString(System.nanoTime(), 36) + ".backup")
                .status(BackupStatus.RESTORING)
                .createdBy(actorEmail)
                .build());
        try {
            upload = safeResolve(restoreRecord.getFileName());
            try (InputStream source = input; OutputStream destination = Files.newOutputStream(
                    upload, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                source.transferTo(destination);
            }
            if (Files.size(upload) != uploadSize) {
                throw new IllegalArgumentException("Dung lượng tệp nhận được không khớp với yêu cầu.");
            }
            verifyPostgresCustomFormat(upload);
            DatabaseTarget target = databaseTarget();
            runCommand(List.of(
                    restoreCommand,
                    "--clean",
                    "--if-exists",
                    "--no-owner",
                    "--no-privileges",
                    "--exit-on-error",
                    "--host=" + target.host(),
                    "--port=" + target.port(),
                    "--username=" + datasourceUsername,
                    "--dbname=" + target.database(),
                    upload.toString()
            ));
            restoreRecord.setStatus(BackupStatus.RESTORED);
            restoreRecord.setRestoredBy(actorEmail);
            restoreRecord.setRestoredAt(LocalDateTime.now());
            restoreRecord.setFileSizeBytes(Files.size(upload));
            restoreRecord.setSha256(sha256(upload));
            auditLogService.record(actorEmail, "BACKUP_RESTORE", "SYSTEM_BACKUP",
                    restoreRecord.getId().toString(),
                    "Phục hồi cơ sở dữ liệu; bản an toàn trước phục hồi #" + safetyBackup.getId() + ".");
        } catch (Exception exception) {
            restoreRecord.setStatus(BackupStatus.FAILED);
            restoreRecord.setFailureReason(safeError(exception));
        }
        return toResponse(repository.save(restoreRecord));
    }

    @Override
    @Transactional
    public void delete(String actorEmail, Long id) {
        BackupRecord record = requireRecord(id);
        if (record.getStatus() == BackupStatus.CREATING || record.getStatus() == BackupStatus.RESTORING) {
            throw new IllegalArgumentException("Không thể xóa tệp khi tiến trình đang chạy.");
        }
        Path file = safeResolve(record.getFileName());
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xóa tệp sao lưu khỏi máy chủ.");
        }
        record.setStatus(BackupStatus.DELETED);
        record.setFailureReason(null);
        repository.save(record);
        auditLogService.record(actorEmail, "BACKUP_DELETE", "SYSTEM_BACKUP", id.toString(),
                "Xóa tệp sao lưu " + record.getFileName() + ".");
    }

    private void runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        if (datasourcePassword != null && !datasourcePassword.isBlank()) {
            builder.environment().put("PGPASSWORD", datasourcePassword);
        }
        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread reader = Thread.ofVirtual().start(() -> {
            try (InputStream stream = process.getInputStream()) {
                stream.transferTo(output);
            } catch (IOException ignored) {
                // Process exit status remains the authoritative result.
            }
        });
        boolean finished = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Tiến trình PostgreSQL vượt quá thời gian cho phép.");
        }
        reader.join(TimeUnit.SECONDS.toMillis(5));
        if (process.exitValue() != 0) {
            String message = output.toString(java.nio.charset.StandardCharsets.UTF_8);
            throw new IllegalStateException("Công cụ PostgreSQL trả về lỗi: " + trim(message, 700));
        }
    }

    private DatabaseTarget databaseTarget() {
        String raw = datasourceUrl.startsWith("jdbc:") ? datasourceUrl.substring(5) : datasourceUrl;
        try {
            URI uri = URI.create(raw);
            String path = uri.getPath();
            if (uri.getHost() == null || path == null || path.length() < 2) {
                throw new IllegalArgumentException("URL cơ sở dữ liệu không hợp lệ.");
            }
            return new DatabaseTarget(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 5432, path.substring(1));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể đọc cấu hình PostgreSQL để sao lưu.");
        }
    }

    private void verifyPostgresCustomFormat(Path path) throws IOException {
        byte[] header = new byte[POSTGRES_CUSTOM_MAGIC.length];
        try (InputStream stream = Files.newInputStream(path)) {
            if (stream.read(header) != header.length || !java.util.Arrays.equals(header, POSTGRES_CUSTOM_MAGIC)) {
                throw new IllegalArgumentException("Tệp không phải bản sao lưu PostgreSQL custom hợp lệ.");
            }
        }
    }

    private String commandVersion(String command) {
        if (command == null || command.isBlank()) return null;
        try {
            Process process = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
            String output;
            try (InputStream stream = process.getInputStream()) {
                output = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            }
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0 ? trim(output, 180) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String requireCommand(String configuredCommand, String executableName) {
        String resolved = resolveCommand(configuredCommand, executableName);
        if (commandVersion(resolved) == null) {
            throw new IllegalStateException("Máy chủ chưa cài hoặc chưa cấu hình đường dẫn cho " + executableName + ".");
        }
        return resolved;
    }

    private String resolveCommand(String configuredCommand, String executableName) {
        if (commandVersion(configuredCommand) != null) return configuredCommand;
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) return configuredCommand;
        for (String environmentKey : List.of("ProgramFiles", "ProgramFiles(x86)")) {
            String baseDirectory = System.getenv(environmentKey);
            if (baseDirectory == null || baseDirectory.isBlank()) continue;
            Path postgresqlRoot = Path.of(baseDirectory, "PostgreSQL");
            if (!Files.isDirectory(postgresqlRoot)) continue;
            try (var versions = Files.list(postgresqlRoot)) {
                Path match = versions
                        .filter(Files::isDirectory)
                        .sorted(Comparator.comparingInt(this::postgresVersion).reversed())
                        .map(path -> path.resolve("bin").resolve(executableName + ".exe"))
                        .filter(Files::isRegularFile)
                        .findFirst()
                        .orElse(null);
                if (match != null) return match.toString();
            } catch (IOException ignored) {
                // Explicit configuration remains available when auto-discovery is not permitted.
            }
        }
        return configuredCommand;
    }

    private int postgresVersion(Path directory) {
        String value = directory.getFileName().toString().replaceFirst("[^0-9].*$", "");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private Path backupDirectory() {
        return Path.of(configuredDirectory).toAbsolutePath().normalize();
    }

    private Path safeResolve(String fileName) {
        if (fileName == null || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("Tên tệp sao lưu không hợp lệ.");
        }
        Path directory = backupDirectory();
        Path result = directory.resolve(fileName).normalize();
        if (!result.startsWith(directory)) throw new IllegalArgumentException("Đường dẫn sao lưu không hợp lệ.");
        createDirectories(directory);
        return result;
    }

    private void createDirectories(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo thư mục sao lưu.");
        }
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Failure state remains visible to the administrator.
        }
    }

    private BackupRecord requireRecord(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản sao lưu."));
    }

    private BackupRecordResponse toResponse(BackupRecord item) {
        Path file = safeResolve(item.getFileName());
        return BackupRecordResponse.builder()
                .id(item.getId()).fileName(item.getFileName()).status(item.getStatus())
                .fileSizeBytes(item.getFileSizeBytes()).sha256(item.getSha256())
                .createdBy(item.getCreatedBy()).restoredBy(item.getRestoredBy()).restoredAt(item.getRestoredAt())
                .failureReason(item.getFailureReason()).createdAt(item.getCreatedAt()).updatedAt(item.getUpdatedAt())
                .downloadable((item.getStatus() == BackupStatus.READY || item.getStatus() == BackupStatus.RESTORED)
                        && Files.isRegularFile(file))
                .build();
    }

    private String safeError(Exception exception) {
        return trim(exception.getMessage() == null ? "Lỗi không xác định." : exception.getMessage(), 1000);
    }

    private String trim(String value, int limit) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.substring(0, Math.min(limit, normalized.length()));
    }

    private record DatabaseTarget(String host, int port, String database) {
    }
}
