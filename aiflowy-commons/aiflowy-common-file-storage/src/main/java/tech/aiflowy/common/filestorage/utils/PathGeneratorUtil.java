package tech.aiflowy.common.filestorage.utils;

import cn.dev33.satoken.stp.StpUtil;
import tech.aiflowy.common.util.StringUtil;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 路径生成工具类：年/月/日分别作为独立目录，格式为 "/年/月/日/{uuid}/文件名"
 */
public class PathGeneratorUtil {

    public static String generateUserPath(String fileName) {
        return "/" + getAccountLoginIdOrCommons() + PathGeneratorUtil.generatePath(fileName);
    }

    private static String getAccountLoginIdOrCommons() {
        try {
            String loginIdAsString = StpUtil.getLoginIdAsString();
            return StringUtil.hasText(loginIdAsString) ? loginIdAsString : "commons";
        } catch (Exception e) {
            return "commons";
        }
    }


    /**
     * 使用当前日期 + 自动生成UUID + 自定义文件名，生成路径
     *
     * @param fileName 文件名（含后缀，如 "video.mp4"）
     * @return 示例："/2024/10/15/1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed/video.mp4"
     */
    public static String generatePath(String fileName) {
        LocalDate currentDate = LocalDate.now();
        String uuid = UUID.randomUUID().toString();
        return buildPath(currentDate, uuid, fileName);
    }

    /**
     * 使用当前日期 - 1天
     *
     * @return 示例："yyyy/MM/dd"（如 2024/10/19）
     */
    public static String generatePrePath() {
        LocalDate yesterday = LocalDate.now().minusDays(2);
        // 自定义格式为 "yyyy/MM/dd"（如 2024/10/19）
        return yesterday.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    /**
     * 内部拼接路径核心方法（年/月/日独立目录）
     */
    private static String buildPath(LocalDate date, String uuid, String fileName) {
        int year = date.getYear();          // 年（如 2024）
        int month = date.getMonthValue();   // 月（如 10，1-12）
        int day = date.getDayOfMonth();     // 日（如 15）

        // 处理文件名（剔除可能包含的路径，只保留纯文件名）
        String pureFileName = getPureFileName(fileName);

        // 拼接格式：/年/月/日/uuid/文件名
        return String.format("/%d/%d/%d/%s/%s", year, month, day, uuid, pureFileName);
    }

    /**
     * 提取纯文件名（避免文件名包含路径分隔符）
     */
    public static String getPureFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        // Unicode 规范化
        fileName = Normalizer.normalize(fileName, Normalizer.Form.NFKC);

        // 提取文件名
        int lastSeparator = Math.max(
                fileName.lastIndexOf('/'),
                fileName.lastIndexOf('\\')
        );
        if (lastSeparator >= 0) {
            fileName = fileName.substring(lastSeparator + 1);
        }

        // 仅保留：中文、英文、数字、点、下划线、短横线
        fileName = fileName.replaceAll("[^\\p{IsHan}\\p{Alnum}._-]", "");

        return fileName;
    }
}
