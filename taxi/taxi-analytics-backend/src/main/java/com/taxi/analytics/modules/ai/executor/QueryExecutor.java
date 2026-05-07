package com.taxi.analytics.modules.ai.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class QueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(QueryExecutor.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${ai.query.timeout:10000}")
    private int queryTimeout;

    @Value("${ai.query.max-rows:1000}")
    private int maxRows;

    public List<Map<String, Object>> execute(String sql) {
        log.info("开始执行 SQL 查询，超时时间：{}ms，最大返回行数：{}", queryTimeout, maxRows);
        log.info("执行 SQL：{}", sql);

        try {
            jdbcTemplate.setQueryTimeout(queryTimeout / 1000);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            if (results.size() > maxRows) {
                log.warn("查询结果超过最大返回行数，截取前 {} 行", maxRows);
                return results.subList(0, maxRows);
            }

            log.info("SQL 查询成功，返回 {} 行", results.size());
            return results;
        } catch (Exception e) {
            log.error("SQL 查询失败", e);
            throw new RuntimeException("SQL 查询失败：" + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> execute(String sql, Object... params) {
        log.info("开始执行参数化 SQL 查询，超时时间：{}ms，最大返回行数：{}", queryTimeout, maxRows);
        log.info("执行 SQL：{}", sql);
        log.info("执行参数：{}", params);

        try {
            jdbcTemplate.setQueryTimeout(queryTimeout / 1000);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);

            if (results.size() > maxRows) {
                log.warn("查询结果超过最大返回行数，截取前 {} 行", maxRows);
                return results.subList(0, maxRows);
            }

            log.info("SQL 查询成功，返回 {} 行", results.size());
            return results;
        } catch (Exception e) {
            log.error("SQL 查询失败", e);
            throw new RuntimeException("SQL 查询失败：" + e.getMessage(), e);
        }
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    public int getMaxRows() {
        return maxRows;
    }
}