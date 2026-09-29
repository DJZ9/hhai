package tech.aiflowy.common.filestorage.impl;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地文件存储路径穿越修复的断言脚本。
 * <p>
 * 仓库当前没有测试框架，这里沿用 {@code src/test} 下 main() 的风格，
 * 通过反射注入 root / prefix（等价于 Spring 的 @Value 注入），无需启动容器。
 * <p>
 * 运行方式见项目修复说明：先 {@code mvn -DskipTests test-compile}，
 * 再用 java 直接运行本类的 main 方法。全部通过时退出码为 0，存在失败用例时退出码为 1。
 */
public class LocalFileStoragePathTest {

    private static final String PREFIX = "http://localhost:8080/attachment";
    private static final String SECRET_CONTENT = "TOP_SECRET_AIFLOWY";
    private static final String LEGAL_CONTENT = "hello-aiflowy";

    private static final List<String> FAILURES = new ArrayList<>();
    private static int passedCount = 0;

    public static void main(String[] args) throws Exception {
        Path tempRoot = Files.createTempDirectory("aiflowy-local-storage-root");
        String root = tempRoot.toAbsolutePath().toString();

        LocalFileStorageServiceImpl storage = new LocalFileStorageServiceImpl();
        inject(storage, "root", root);
        inject(storage, "prefix", PREFIX);

        // 合法文件：{root}/1/2026/09/20/<uuid>/a.pdf
        String legalRelative = "/1/2026/09/20/11111111-2222-3333-4444-555555555555/a.pdf";
        writeFile(new File(root + legalRelative), LEGAL_CONTENT);

        // 根目录内使用 .. 但仍落在根目录内的文件：{root}/1/2026/09/20/a.pdf
        writeFile(new File(root + "/1/2026/09/20/a.pdf"), LEGAL_CONTENT);

        // 根目录外的"敏感文件"，用于证明穿越读取/删除被拒绝
        Path outsideDir = Files.createTempDirectory("aiflowy-outside-secret");
        Path secretFile = outsideDir.resolve("secret.txt");
        Files.writeString(secretFile, SECRET_CONTENT);

        String relativeTraversal = tempRoot.relativize(secretFile).toString();

        // ===== 放行用例 =====
        expectAllowRead(storage, PREFIX + legalRelative, LEGAL_CONTENT,
                "合法路径（带 prefix）可正常读取");
        expectAllowRead(storage, legalRelative, LEGAL_CONTENT,
                "合法路径（无 prefix，历史数据）可正常读取");
        expectAllowRead(storage, PREFIX + "/1/2026/09/20/11111111-2222-3333-4444-555555555555/../a.pdf",
                LEGAL_CONTENT, "根目录内 .. 归一化后仍在根目录内的路径可正常读取");
        expectNotRejected(storage, PREFIX + "/temp/2026/9/20/22222222-3333-4444-5555-666666666666/t.xlsx",
                "临时文件路径不被沙箱拒绝（文件不存在时仅报文件不存在）");

        // ===== 拒绝用例（读取，严格断言必须抛 SecurityException） =====
        expectRejected(storage, relativeTraversal,
                "相对路径穿越读取根目录外的真实文件");
        expectRejected(storage, "../../../../../../etc/passwd",
                "CNVD 通报 payload（../../../../../../etc/passwd）");
        expectRejected(storage, PREFIX + "/../../../../../../etc/passwd",
                "带 prefix 的穿越 payload");
        expectRejected(storage, PREFIX, "仅 prefix（等价于存储根目录）");
        expectRejected(storage, "", "空路径");
        expectRejected(storage, PREFIX + "/1/2026/09/20/\0a.pdf", "包含 NUL 字符的路径");

        // ===== 拒绝用例（读取，平台差异用例：只要读不到敏感内容即视为拦截成功） =====
        expectNeverReadSecret(storage, "..\\..\\Windows\\win.ini",
                "Windows 反斜杠穿越 payload");
        expectNeverReadSecret(storage, secretFile.toAbsolutePath().toString(),
                "根目录外绝对路径 payload");
        expectNeverReadSecret(storage, PREFIX + "/%2e%2e%2f%2e%2e%2fetc/passwd",
                "URL 编码穿越（不参与解码，按普通文件名字面处理）");
        expectNeverReadSecret(storage, "https://another-host/attachment/../../../../../../etc/passwd",
                "伪造其他域名前缀 + 穿越");

        // ===== 拒绝用例（删除） =====
        expectRejectedDelete(storage, relativeTraversal, secretFile,
                "相对路径穿越删除根目录外的真实文件");
        expectRejectedDelete(storage, PREFIX, secretFile,
                "仅 prefix 不允许删除存储根目录本身");
        expectNeverDeleteOutside(storage, secretFile.toAbsolutePath().toString(), secretFile,
                "绝对路径穿越删除根目录外的真实文件");

        // ===== 静态解析方法（覆盖 save 上传新建文件的场景） =====
        File canonicalRoot = tempRoot.toFile().getCanonicalFile();
        File newFileTarget = LocalFileStorageServiceImpl.resolveLocalFile(
                root, "/commons/2026/09/20/33333333-4444-5555-6666-777777777777/brand-new.pdf", null);
        if (newFileTarget.toPath().startsWith(canonicalRoot.toPath()) && !newFileTarget.exists()) {
            writeFile(newFileTarget, LEGAL_CONTENT);
            try (InputStream in = Files.newInputStream(newFileTarget.toPath())) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (LEGAL_CONTENT.equals(content)) {
                    pass("新建文件（尚不存在）的路径可正常解析并写入");
                } else {
                    fail("新建文件（尚不存在）的路径可正常解析并写入", "写入后内容不符：" + content);
                }
            } catch (Exception e) {
                fail("新建文件（尚不存在）的路径可正常解析并写入",
                        "写入后读取失败：" + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } else {
            fail("新建文件（尚不存在）的路径可正常解析并写入",
                    "解析结果越出根目录或文件已存在：" + newFileTarget);
        }

        String stripped = LocalFileStorageServiceImpl.stripStoragePrefix(PREFIX + legalRelative, PREFIX);
        if (legalRelative.equals(stripped)) {
            pass("prefix 开头时仅截断一次");
        } else {
            fail("prefix 开头时仅截断一次", "截断结果=" + stripped);
        }
        String untouched = LocalFileStorageServiceImpl.stripStoragePrefix(legalRelative, PREFIX);
        if (legalRelative.equals(untouched)) {
            pass("未命中 prefix 时路径保持原样");
        } else {
            fail("未命中 prefix 时路径保持原样", "实际结果=" + untouched);
        }

        // ===== 汇总 =====
        System.out.println();
        System.out.println("通过用例：" + passedCount + "，失败用例：" + FAILURES.size());
        if (!FAILURES.isEmpty()) {
            System.out.println("失败明细：");
            FAILURES.forEach(item -> System.out.println("  - " + item));
            System.exit(1);
        }
        System.out.println("全部用例通过。");
    }

    /**
     * 断言该路径可以被正常读取，且内容符合预期（expectedContent 为 null 时不校验内容）。
     */
    private static void expectAllowRead(LocalFileStorageServiceImpl storage, String path,
                                        String expectedContent, String caseName) {
        try (InputStream in = storage.readStream(path)) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (expectedContent != null && !expectedContent.equals(content)) {
                fail(caseName, "内容不符合预期，实际为：" + content);
                return;
            }
            pass(caseName);
        } catch (Exception e) {
            fail(caseName, "不应被拒绝，但抛出 " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * 断言路径没有被沙箱拒绝（用于文件不存在的合法路径，允许抛 FileNotFound 等 IOException）。
     */
    private static void expectNotRejected(LocalFileStorageServiceImpl storage, String path, String caseName) {
        try (InputStream in = storage.readStream(path)) {
            in.readAllBytes();
            pass(caseName);
        } catch (SecurityException e) {
            fail(caseName, "合法路径被沙箱拒绝：" + e.getMessage());
        } catch (Exception e) {
            pass(caseName + "（以 " + e.getClass().getSimpleName() + " 失败告终，说明未被沙箱拒绝）");
        }
    }

    /**
     * 断言该路径必须被沙箱拒绝，即抛出 SecurityException。
     */
    private static void expectRejected(LocalFileStorageServiceImpl storage, String path, String caseName) {
        try (InputStream in = storage.readStream(path)) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            fail(caseName, "该路径未被拒绝，读到了内容：" + content);
        } catch (SecurityException e) {
            pass(caseName);
        } catch (Exception e) {
            fail(caseName, "期望 SecurityException，实际抛出 "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * 断言该路径无论如何都读不到根目录外的敏感内容。
     * <p>
     * 不同平台的路径解析规则不同（例如 Windows 下以 "/" 或 "//" 开头的路径会被解析回根目录内部，
     * Linux 下反斜杠只是普通文件名字符），因此这类用例只要最终没有泄露敏感内容即视为拦截成功。
     */
    private static void expectNeverReadSecret(LocalFileStorageServiceImpl storage, String path, String caseName) {
        try (InputStream in = storage.readStream(path)) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (content.contains(SECRET_CONTENT)) {
                fail(caseName, "越界读取成功，响应中出现了敏感文件内容");
            } else {
                fail(caseName, "该路径未被拒绝，读到了内容：" + content);
            }
        } catch (SecurityException e) {
            pass(caseName);
        } catch (Exception e) {
            pass(caseName + "（以 " + e.getClass().getSimpleName() + " 失败告终，未读到敏感内容）");
        }
    }

    /**
     * 断言该路径必须被沙箱拒绝，且根目录外的文件保持完好。
     */
    private static void expectRejectedDelete(LocalFileStorageServiceImpl storage, String path,
                                             Path protectedFile, String caseName) {
        try {
            storage.delete(path);
            if (Files.exists(protectedFile)) {
                fail(caseName, "该路径未被拒绝，删除操作被放行");
            } else {
                fail(caseName, "越界删除成功，根目录外的文件已被删除");
            }
        } catch (SecurityException e) {
            if (Files.exists(protectedFile)) {
                pass(caseName);
            } else {
                fail(caseName, "虽然抛出了 SecurityException，但根目录外的文件已丢失");
            }
        } catch (Exception e) {
            fail(caseName, "期望 SecurityException，实际抛出 "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * 断言该路径无法删除根目录外的文件（允许平台差异导致的其它异常）。
     */
    private static void expectNeverDeleteOutside(LocalFileStorageServiceImpl storage, String path,
                                                 Path protectedFile, String caseName) {
        try {
            storage.delete(path);
            if (Files.exists(protectedFile)) {
                fail(caseName, "该路径未被拒绝，删除操作被放行");
            } else {
                fail(caseName, "越界删除成功，根目录外的文件已被删除");
            }
        } catch (SecurityException e) {
            if (Files.exists(protectedFile)) {
                pass(caseName);
            } else {
                fail(caseName, "虽然抛出了 SecurityException，但根目录外的文件已丢失");
            }
        } catch (Exception e) {
            if (Files.exists(protectedFile)) {
                pass(caseName + "（以 " + e.getClass().getSimpleName() + " 失败告终，文件未被删除）");
            } else {
                fail(caseName, "文件已被删除，异常类型为：" + e.getClass().getSimpleName());
            }
        }
    }

    private static void writeFile(File file, String content) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("无法创建测试目录：" + parent);
        }
        Files.writeString(file.toPath(), content);
    }

    private static void inject(Object target, String fieldName, String value) throws Exception {
        Field field = LocalFileStorageServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void pass(String caseName) {
        passedCount++;
        System.out.println("[PASS] " + caseName);
    }

    private static void fail(String caseName, String reason) {
        FAILURES.add(caseName + " -> " + reason);
        System.out.println("[FAIL] " + caseName + " -> " + reason);
    }
}
