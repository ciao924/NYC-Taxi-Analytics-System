package com.taxi.analytics.modules.ai.guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL 安全校验工具类
 * 负责校验智能体返回的 SQL 语句，防止恶意操作
 */
@Component
public class SqlSecurityGuard {

    private static final Logger log = LoggerFactory.getLogger(SqlSecurityGuard.class);

    /**
     * 危险的 SQL 关键字（DML/DDL）
     */
    private static final List<String> DANGEROUS_KEYWORDS = Arrays.asList(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE",
            "CREATE", "REPLACE", "RENAME", "GRANT", "REVOKE", "LOCK",
            "UNLOCK", "COMMIT", "ROLLBACK", "SAVEPOINT", "EXEC", "EXECUTE",
            "CALL", "MERGE", "UPSERT"
    );

    /**
     * 注释模式正则表达式
     */
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "(--.*?$)|(/\\*[\\s\\S]*?\\*/)", Pattern.MULTILINE
    );

    /**
     * SQL 语句长度限制（防止超长 SQL）
     */
    private static final int MAX_SQL_LENGTH = 10000;

    /**
     * 校验 SQL 安全性
     *
     * @param sql 待校验的 SQL 语句
     * @return 如果安全返回 null，否则返回错误信息
     */
    public String validate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            log.warn("SQL 语句为空");
            return "SQL 语句不能为空";
        }

        // 检查 SQL 长度
        if (sql.length() > MAX_SQL_LENGTH) {
            log.warn("SQL 语句过长: {} 字符", sql.length());
            return "SQL 语句过长，最大允许 " + MAX_SQL_LENGTH + " 字符";
        }

        // 移除注释后的 SQL（防止通过注释绕过检测）
        String sqlWithoutComments = removeComments(sql);

        // 转换为大写便于关键字检查
        String upperSql = sqlWithoutComments.toUpperCase();

        // 检查危险关键字
        for (String keyword : DANGEROUS_KEYWORDS) {
            // 使用单词边界匹配，避免误判（如 DELETE 在字段名中）
            String pattern = "\\b" + keyword + "\\b";
            if (Pattern.compile(pattern).matcher(upperSql).find()) {
                log.warn("检测到危险 SQL 关键字: {}", keyword);
                return "SQL 语句包含不允许的操作: " + keyword;
            }
        }

        // 检查是否包含多个语句（防止 SQL 注入）
        if (countStatements(sqlWithoutComments) > 1) {
            log.warn("检测到多条 SQL 语句");
            return "不允许执行多条 SQL 语句";
        }

        // 检查是否为有效的 SELECT 语句
        if (!upperSql.trim().startsWith("SELECT") && !upperSql.trim().startsWith("WITH")) {
            log.warn("SQL 语句不是有效的查询语句");
            return "只允许执行 SELECT 查询语句";
        }

        log.info("SQL 安全校验通过");
        return null;
    }

    /**
     * 移除 SQL 注释
     */
    private String removeComments(String sql) {
        return COMMENT_PATTERN.matcher(sql).replaceAll("");
    }

    /**
     * 统计 SQL 语句数量（检测分号分隔的多条语句）
     */
    private int countStatements(String sql) {
        // 移除字符串中的分号（字符串内的分号不应被计数）
        String sqlWithoutStrings = removeStrings(sql);
        int count = 1;
        boolean inComment = false;
        
        for (int i = 0; i < sqlWithoutStrings.length(); i++) {
            char c = sqlWithoutStrings.charAt(i);
            
            // 检查多行注释开始
            if (i < sqlWithoutStrings.length() - 1 && 
                c == '/' && sqlWithoutStrings.charAt(i + 1) == '*') {
                inComment = true;
                i++;
                continue;
            }
            
            // 检查多行注释结束
            if (i < sqlWithoutStrings.length() - 1 && 
                c == '*' && sqlWithoutStrings.charAt(i + 1) == '/') {
                inComment = false;
                i++;
                continue;
            }
            
            // 如果不在注释中，统计分号
            if (!inComment && c == ';') {
                // 关键修复：只有分号后面还有非空白字符时，才算作新语句的开始
                // 避免将末尾的分号误判为多条语句
                boolean hasMoreContent = false;
                for (int j = i + 1; j < sqlWithoutStrings.length(); j++) {
                    char nextChar = sqlWithoutStrings.charAt(j);
                    if (!Character.isWhitespace(nextChar)) {
                        hasMoreContent = true;
                        break;
                    }
                }
                if (hasMoreContent) {
                    count++;
                }
            }
        }
        
        return count;
    }

    /**
     * 移除非注释中的字符串
     */
    private String removeStrings(String sql) {
        StringBuilder result = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inComment = false;
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            
            // 检查转义字符
            if ((inSingleQuote || inDoubleQuote) && c == '\\' && i < sql.length() - 1) {
                i++; // 跳过转义字符
                continue;
            }
            
            // 检查单引号
            if (c == '\'' && !inDoubleQuote && !inComment) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            
            // 检查双引号
            if (c == '"' && !inSingleQuote && !inComment) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            
            // 检查单行注释
            if (c == '-' && i < sql.length() - 1 && sql.charAt(i + 1) == '-' && !inSingleQuote && !inDoubleQuote) {
                // 跳过整行
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            
            // 检查多行注释开始
            if (c == '/' && i < sql.length() - 1 && sql.charAt(i + 1) == '*' && !inSingleQuote && !inDoubleQuote) {
                inComment = true;
                i++;
                continue;
            }
            
            // 检查多行注释结束
            if (c == '*' && i < sql.length() - 1 && sql.charAt(i + 1) == '/' && !inSingleQuote && !inDoubleQuote) {
                inComment = false;
                i++;
                continue;
            }
            
            // 只有不在字符串和注释中的字符才保留
            if (!inSingleQuote && !inDoubleQuote && !inComment) {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}