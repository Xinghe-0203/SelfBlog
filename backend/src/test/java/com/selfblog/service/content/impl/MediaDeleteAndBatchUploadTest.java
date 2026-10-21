package com.selfblog.service.content.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.service.content.MediaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C3 + C4 回归测试：媒体删除的提交顺序、批量上传的失败补偿。
 *
 * <p><b>C3</b>：{@code deleteMedia} 原实现先删磁盘文件再删数据库行。行删除失败或事务回滚时，
 * 文件已经不可恢复，数据库里却还留着一条指向空文件的记录。现在的顺序是"先删行、提交后再删文件"，
 * 因此本类用"外层事务回滚"来证明：回滚后文件必须还在、行必须还在；提交后两者才一起消失。</p>
 *
 * <p><b>C4</b>：{@code uploadFiles} 原实现用一个事务包住整批上传，第 N 个文件失败时前 N-1 个的
 * 数据库行被回滚，但已经写盘的字节没有任何人清理。现在每个文件各自一个短事务，失败时对本批
 * 已成功项做补偿删除。本类断言"第二个文件失败后，第一个文件的行与字节都不残留"。</p>
 *
 * <p>这里刻意<b>不加</b> {@code @Transactional}：C3 要观察真实的提交/回滚边界，
 * 测试事务会把 {@code afterCommit} 全部吞掉。数据清理放在 {@link #cleanUp()}。</p>
 *
 * <p>上传目录用<b>相对</b>路径 {@code target/media-c3-c4-test}：{@code MediaServiceImpl} 会把它
 * 解析到 {@code user.dir}（Maven 下即 {@code backend/}），而 {@code WebMvcConfig} 用
 * {@code Paths.get(uploadRootPath, uploadBasePath)} 拼静态资源目录 —— 若这里塞绝对路径，
 * 后者会得到 {@code C:\...\backend\C:\...} 这种非法路径，整个 Spring 上下文都起不来。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("媒体删除顺序与批量上传补偿（C3/C4）")
class MediaDeleteAndBatchUploadTest {

    /** 相对 user.dir 的上传目录，落在构建产物目录里，不进源码树。 */
    private static final String UPLOAD_DIR_NAME = "target/media-c3-c4-test";

    private static final Path UPLOAD_ROOT =
            Paths.get(System.getProperty("user.dir")).resolve(UPLOAD_DIR_NAME);

    @DynamicPropertySource
    static void uploadProperties(DynamicPropertyRegistry registry) {
        registry.add("upload.base-path", () -> UPLOAD_DIR_NAME);
    }

    private static final long OWNER_ID = 771100L;
    private static final long MEDIA_ID = 771101L;

    @Autowired
    MediaService mediaService;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager transactionManager;

    private TransactionTemplate template;
    private Path mediaFile;

    @BeforeEach
    void seed() throws IOException {
        template = new TransactionTemplate(transactionManager);
        cleanUp();
        deleteTree();

        Path dir = Files.createDirectories(UPLOAD_ROOT.resolve("2026/10/21"));
        mediaFile = dir.resolve("seeded.png");
        Files.write(mediaFile, new byte[]{1, 2, 3, 4});
        jdbc.update("INSERT INTO blog_media(id,user_id,file_name,file_path,file_url,file_type,mime_type,file_size,status,is_deleted)"
                        + " VALUES(?,?,?,?,?,?,?,?,1,0)",
                MEDIA_ID, OWNER_ID, "seeded.png", mediaFile.toAbsolutePath().toString(),
                "/uploads/2026/10/21/seeded.png", "image", "image/png", 4L);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM blog_media WHERE user_id = ?", OWNER_ID);
    }

    @Test
    @DisplayName("C3：外层事务回滚时，文件与数据库行都必须保留")
    void rolledBackDeleteKeepsBothTheFileAndTheRow() {
        template.execute(status -> {
            mediaService.deleteMedia(MEDIA_ID, OWNER_ID);
            status.setRollbackOnly();
            return null;
        });

        assertTrue(Files.exists(mediaFile), "事务回滚后文件不得被删除——旧实现在删行之前就把文件删掉了");
        assertEquals(0, isDeletedOf(MEDIA_ID), "事务回滚后数据库行必须保持原样");
    }

    @Test
    @DisplayName("C3：提交后先删行、再删文件，两者都消失")
    void committedDeleteRemovesTheRowAndTheFileAfterCommit() {
        template.execute(status -> {
            mediaService.deleteMedia(MEDIA_ID, OWNER_ID);
            return null;
        });

        assertEquals(1, isDeletedOf(MEDIA_ID), "提交后数据库行必须逻辑删除");
        assertTrue(Files.notExists(mediaFile), "提交后磁盘文件必须被清理");
    }

    @Test
    @DisplayName("C4：批量上传中途失败时，已成功的文件不得留下行或字节")
    void failedBatchLeavesNoOrphanRowOrFile() throws IOException {
        MockMultipartFile good = new MockMultipartFile(
                "file", "ok.png", "image/png", pngBytes());
        MockMultipartFile bad = new MockMultipartFile(
                "file", "bad.png", "image/png", "this-is-not-a-png-at-all".getBytes());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> mediaService.uploadFiles(new MockMultipartFile[]{good, bad}, OWNER_ID, "avatar"));

        assertEquals(400, exception.getCode());
        assertEquals(0, activeMediaCount(), "第一个文件已经成功落库，补偿后不得残留活跃行");
        assertEquals(0, uploadedFileCount(), "补偿必须回收本批已经写入的字节，否则每批失败都留下孤儿文件");
    }

    @Test
    @DisplayName("C4：全部成功时逐个落库，不再共享一个长事务")
    void successfulBatchKeepsEveryFile() throws IOException {
        MockMultipartFile first = new MockMultipartFile(
                "file", "a.png", "image/png", pngBytes());
        MockMultipartFile second = new MockMultipartFile(
                "file", "b.png", "image/png", pngBytes());

        List<?> uploaded = mediaService.uploadFiles(new MockMultipartFile[]{first, second}, OWNER_ID, "avatar");

        assertEquals(2, uploaded.size());
        assertEquals(2, activeMediaCount());
        assertEquals(2, uploadedFileCount());
    }

    private byte[] pngBytes() throws IOException {
        BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        byte[] bytes = out.toByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 12);
        return bytes;
    }

    private int isDeletedOf(long id) {
        Integer value = jdbc.queryForObject("SELECT is_deleted FROM blog_media WHERE id = ?", Integer.class, id);
        return value == null ? -1 : value;
    }

    /** 只数本批上传落库的行；种子里那条 {@link #MEDIA_ID} 不算，否则计数永远多 1。 */
    private int activeMediaCount() {
        Integer value = jdbc.queryForObject(
                "SELECT COUNT(*) FROM blog_media WHERE user_id = ? AND is_deleted = 0 AND id <> ?",
                Integer.class, OWNER_ID, MEDIA_ID);
        return value == null ? -1 : value;
    }

    private long uploadedFileCount() {
        try (Stream<Path> walk = Files.walk(UPLOAD_ROOT)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> !path.equals(mediaFile))
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void deleteTree() throws IOException {
        if (!Files.exists(UPLOAD_ROOT)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(UPLOAD_ROOT)) {
            List<Path> paths = walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}
