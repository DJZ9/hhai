package tech.aiflowy.common.dict.loader;

import tech.aiflowy.common.util.SqlIdentifierUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据表字典（DatabaseDictLoader）二阶 SQL 注入修复验证脚本。
 * <p>
 * 对应 CNVD 通报：{@code POST /api/v1/sysDict/save} 写入的
 * {@code options.tableName/keyColumn/labelColumn/parentColumn/orderBy} 被直接拼进 SQL，
 * 任意已登录用户调用 {@code GET /api/v1/dict/items/{code}} 即可跨表读取数据。
 * <p>
 * 覆盖三类断言：
 * 1) 标识符校验器：合法表名/列名放行，注入载荷与拼接字符一律拒绝；
 * 2) 拼 SQL：合法配置生成反引号引用的语句（含修复 WHERE 前缺失空格），非法配置抛出异常且不产生 SQL；
 * 3) ORDER BY：只允许列名 + asc/desc 的逗号列表。
 * <p>
 * 运行方式（Windows PowerShell 示例）：
 * <pre>
 * mvn -pl aiflowy-commons/aiflowy-common-base -am -DskipTests test-compile
 * mvn -pl aiflowy-commons/aiflowy-common-base dependency:build-classpath "-Dmdep.outputFile=$env:TEMP\base-cp.txt" -DincludeScope=test
 * $cp = (Get-Content "$env:TEMP\base-cp.txt" -Raw).Trim()
 * java -Dfile.encoding=UTF-8 -cp "aiflowy-commons\aiflowy-common-base\target\classes;aiflowy-commons\aiflowy-common-base\target\test-classes;$cp" tech.aiflowy.common.dict.loader.DatabaseDictLoaderSecurityTest
 * </pre>
 * 全部用例通过时退出码为 0，存在失败用例时退出码为 1。
 *
 * @author aiflowy
 */
public class DatabaseDictLoaderSecurityTest {

    /**
     * CNVD 通报中的攻击载荷（写入 options.tableName）
     */
    private static final String ATTACK_PAYLOAD =
            "tb_sys_account WHERE 1=1 UNION SELECT login_name,password FROM tb_sys_account--";

    private static final String DICT_CODE = "poc_sqli";

    private static final List<String> FAILURES = new ArrayList<>();

    private static int passedCount = 0;

    public static void main(String[] args) {
        // ================= 1. 标识符校验器：放行 =================
        expectTrue("校验器：合法表名 tb_sys_account",
                SqlIdentifierUtil.isValidIdentifier("tb_sys_account"));
        expectTrue("校验器：合法列名 menu_title",
                SqlIdentifierUtil.isValidIdentifier("menu_title"));
        expectTrue("校验器：合法 schema.table",
                SqlIdentifierUtil.isValidIdentifier("db.tb_sys_account"));
        expectTrue("校验器：合法反引号写法",
                SqlIdentifierUtil.isValidIdentifier("`db`.`tb_sys_account`"));
        expectTrue("校验器：允许 $ 字符",
                SqlIdentifierUtil.isValidIdentifier("tb$dict"));

        // ================= 2. 标识符校验器：拒绝 =================
        expectFalse("校验器：拒绝通报 payload",
                SqlIdentifierUtil.isValidIdentifier(ATTACK_PAYLOAD));
        expectFalse("校验器：拒绝拼接 WHERE 条件",
                SqlIdentifierUtil.isValidIdentifier("tb_sys_account WHERE 1=1"));
        expectFalse("校验器：拒绝注释符截断",
                SqlIdentifierUtil.isValidIdentifier("tb_sys_account--"));
        expectFalse("校验器：拒绝分号追加语句",
                SqlIdentifierUtil.isValidIdentifier("tb_sys_account;DROP TABLE tb_sys_account"));
        expectFalse("校验器：拒绝逗号拼接多表",
                SqlIdentifierUtil.isValidIdentifier("tb_sys_account, tb_user"));
        expectFalse("校验器：拒绝括号子查询",
                SqlIdentifierUtil.isValidIdentifier("(SELECT uid FROM tb_order)"));
        expectFalse("校验器：拒绝空白",
                SqlIdentifierUtil.isValidIdentifier("tb sys_account"));
        expectFalse("校验器：拒绝换行拼接",
                SqlIdentifierUtil.isValidIdentifier("tb_sys_account\nUNION SELECT 1"));
        expectFalse("校验器：拒绝单侧反引号",
                SqlIdentifierUtil.isValidIdentifier("`tb_sys_account"));
        expectFalse("校验器：拒绝反引号内部再出现反引号",
                SqlIdentifierUtil.isValidIdentifier("`tb`sys``"));
        expectFalse("校验器：拒绝数字开头",
                SqlIdentifierUtil.isValidIdentifier("1tb"));
        expectFalse("校验器：拒绝超长标识符",
                SqlIdentifierUtil.isValidIdentifier("a".repeat(200)));
        expectFalse("校验器：拒绝空字符串",
                SqlIdentifierUtil.isValidIdentifier(""));
        expectFalse("校验器：拒绝 null",
                SqlIdentifierUtil.isValidIdentifier(null));

        // ================= 3. 反引号引用 =================
        expectEquals("引用：单段表名", "`tb_sys_account`",
                SqlIdentifierUtil.quoteIdentifier("tb_sys_account"));
        expectEquals("引用：点分表名逐段引用", "`db`.`tb_x`",
                SqlIdentifierUtil.quoteIdentifier("db.tb_x"));
        expectEquals("引用：已带反引号不会重复包裹", "`db`.`tb_x`",
                SqlIdentifierUtil.quoteIdentifier("`db`.`tb_x`"));
        expectReject("引用：非法取值直接拒绝",
                () -> SqlIdentifierUtil.quoteIdentifier(ATTACK_PAYLOAD));

        // ================= 4. ORDER BY =================
        expectTrue("orderBy：单列", SqlIdentifierUtil.isValidOrderBy("sort_no"));
        expectTrue("orderBy：列名 + 方向", SqlIdentifierUtil.isValidOrderBy("sort_no asc"));
        expectTrue("orderBy：多列", SqlIdentifierUtil.isValidOrderBy("sort_no asc, id desc"));
        expectFalse("orderBy：拒绝拼接语句", SqlIdentifierUtil.isValidOrderBy("sort_no asc; DROP TABLE t"));
        expectFalse("orderBy：拒绝子查询", SqlIdentifierUtil.isValidOrderBy("(SELECT 1)"));
        expectFalse("orderBy：拒绝空项", SqlIdentifierUtil.isValidOrderBy("sort_no asc,"));
        expectFalse("orderBy：拒绝未知方向", SqlIdentifierUtil.isValidOrderBy("sort_no desc nulls first"));
        expectEquals("orderBy：引用后拼装", "`sort_no` asc, `id` desc",
                SqlIdentifierUtil.quoteOrderBy("sort_no asc, id desc"));

        // ================= 5. 拼 SQL：合法配置 =================
        expectSql("拼 SQL：基本查询",
                DICT_CODE, "tb_sys_account", "id", "login_name", null, null, false,
                "SELECT `id`, `login_name` FROM `tb_sys_account`");
        expectSql("拼 SQL：树形字典带父级列",
                DICT_CODE, "tb_sys_menu", "id", "menu_title", "parent_id", null, false,
                "SELECT `id`, `menu_title`, `parent_id` FROM `tb_sys_menu`");
        expectSql("拼 SQL：keyword 仍是绑定参数（WHERE 前有空格）",
                DICT_CODE, "tb_sys_account", "id", "login_name", null, null, true,
                "SELECT `id`, `login_name` FROM `tb_sys_account` WHERE `login_name` = ?");
        expectSql("拼 SQL：ORDER BY 列名引用",
                DICT_CODE, "tb_sys_menu", "id", "menu_title", "parent_id", "sort_no asc", false,
                "SELECT `id`, `menu_title`, `parent_id` FROM `tb_sys_menu` ORDER BY `sort_no` asc");

        // ================= 6. 拼 SQL：非法配置被拒绝 =================
        expectSqlReject("拼 SQL：拒绝通报 payload（tableName）",
                DICT_CODE, ATTACK_PAYLOAD, "id", "login_name", null, null);
        expectSqlReject("拼 SQL：拒绝 payload 变体（去掉注释符）",
                DICT_CODE, "tb_sys_account WHERE 1=1 UNION SELECT login_name,password FROM tb_sys_account", "id", "login_name", null, null);
        expectSqlReject("拼 SQL：拒绝注释符截断（labelColumn）",
                DICT_CODE, "tb_sys_account", "id", "login_name--", null, null);
        expectSqlReject("拼 SQL：拒绝括号子查询（keyColumn）",
                DICT_CODE, "tb_sys_account", "(SELECT 1)", "login_name", null, null);
        expectSqlReject("拼 SQL：拒绝注入 parentColumn",
                DICT_CODE, "tb_sys_menu", "id", "menu_title", "parent_id UNION SELECT 1", null);
        expectSqlReject("拼 SQL：拒绝注入 orderBy",
                DICT_CODE, "tb_sys_menu", "id", "menu_title", null, "sort_no asc; DROP TABLE tb_sys_menu");
        expectSqlReject("拼 SQL：tableName 为空",
                DICT_CODE, null, "id", "login_name", null, null);
        expectSqlReject("拼 SQL：keyColumn 为空",
                DICT_CODE, "tb_sys_account", "  ", "login_name", null, null);
        expectSqlReject("拼 SQL：labelColumn 为空",
                DICT_CODE, "tb_sys_account", "id", null, null, null);

        // ================= 7. 兼容性：历史合法配置仍可用 =================
        // 典型历史配置：普通表名 + 下划线列名
        expectSql("兼容：菜单字典",
                "sysMenu", "tb_sys_menu", "id", "menu_title", "parent_id", "sort_no asc", false,
                "SELECT `id`, `menu_title`, `parent_id` FROM `tb_sys_menu` ORDER BY `sort_no` asc");
        // 历史配置里可能带反引号
        expectSql("兼容：表名带反引号",
                "sysMenu", "`tb_sys_menu`", "`id`", "`menu_title`", null, null, false,
                "SELECT `id`, `menu_title` FROM `tb_sys_menu`");
        // 历史配置里可能写成 db.table
        expectSql("兼容：带库名限定",
                "sysMenu", "aiflowy.tb_sys_menu", "id", "menu_title", null, null, false,
                "SELECT `id`, `menu_title` FROM `aiflowy`.`tb_sys_menu`");
        // 大写表名/列名
        expectSql("兼容：大写标识符",
                "sysMenu", "TB_SYS_MENU", "ID", "MENU_TITLE", null, null, false,
                "SELECT `ID`, `MENU_TITLE` FROM `TB_SYS_MENU`");
        // 前后空格（写入时可能手抖多敲了空格）
        expectSql("兼容：标识符前后空格",
                "sysPosition", " tb_sys_position ", " id ", " position_name ", null, null, false,
                "SELECT `id`, `position_name` FROM `tb_sys_position`");
        // 数字、$ 开头之外的下划线开头列名
        expectSql("兼容：下划线开头列名",
                "sysDept", "tb_sys_dept", "_id", "dept_name", "_parent_id", null, true,
                "SELECT `_id`, `dept_name`, `_parent_id` FROM `tb_sys_dept` WHERE `dept_name` = ?");

        // ================= 汇总 =================
        System.out.println();
        System.out.println("通过用例：" + passedCount + "，失败用例：" + FAILURES.size());
        if (!FAILURES.isEmpty()) {
            System.out.println("失败明细：");
            FAILURES.forEach(item -> System.out.println("  - " + item));
            System.exit(1);
        }
        System.out.println("全部用例通过。");
        System.exit(0);
    }

    private static void expectSql(String caseName, String code, String tableName, String keyColumn,
                                  String labelColumn, String parentColumn, String orderBy,
                                  boolean withKeyword, String expectedSql) {
        try {
            String sql = DatabaseDictLoader.buildSelectSql(code, tableName, keyColumn, labelColumn,
                    parentColumn, orderBy, withKeyword);
            if (!expectedSql.equals(sql)) {
                fail(caseName, "渲染结果不符\n      期望: " + expectedSql + "\n      实际: " + sql);
                return;
            }
            pass(caseName);
        } catch (Throwable e) {
            fail(caseName, "不应被拒绝，但抛出 " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void expectSqlReject(String caseName, String code, String tableName, String keyColumn,
                                        String labelColumn, String parentColumn, String orderBy) {
        try {
            String sql = DatabaseDictLoader.buildSelectSql(code, tableName, keyColumn, labelColumn,
                    parentColumn, orderBy, false);
            fail(caseName, "该配置未被拒绝，最终 SQL = " + sql);
        } catch (IllegalArgumentException e) {
            pass(caseName + "（" + e.getMessage() + "）");
        } catch (Throwable e) {
            fail(caseName, "期望 IllegalArgumentException，实际 " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void expectReject(String caseName, Runnable runnable) {
        try {
            runnable.run();
            fail(caseName, "该输入未被拒绝");
        } catch (IllegalArgumentException e) {
            pass(caseName + "（" + e.getMessage() + "）");
        } catch (Throwable e) {
            fail(caseName, "期望 IllegalArgumentException，实际 " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void expectTrue(String caseName, boolean value) {
        if (value) {
            pass(caseName);
        } else {
            fail(caseName, "期望 true，实际 false");
        }
    }

    private static void expectFalse(String caseName, boolean value) {
        if (!value) {
            pass(caseName);
        } else {
            fail(caseName, "期望 false，实际 true");
        }
    }

    private static void expectEquals(String caseName, String expected, String actual) {
        if (expected.equals(actual)) {
            pass(caseName);
        } else {
            fail(caseName, "期望 " + expected + "，实际 " + actual);
        }
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
