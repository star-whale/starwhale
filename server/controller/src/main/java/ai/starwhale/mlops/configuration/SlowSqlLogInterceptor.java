/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.configuration;

import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;


@Slf4j
@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
})
@ConditionalOnProperty(prefix = "sw.sql.log-slow-sql", name = "enable", havingValue = "true", matchIfMissing = true)
public class SlowSqlLogInterceptor implements Interceptor {

    private Configuration configuration = null;
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

    private final int slowSqlMillis;

    private final int maxSqlLength;

    public SlowSqlLogInterceptor(
            @Value("sw.sql.slow-sql-millis:100") int slowSqlMillis,
            @Value("sw.sql.max-print-length:200") int maxSqlLength
    ) {
        this.slowSqlMillis = slowSqlMillis;
        this.maxSqlLength = maxSqlLength;
    }


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        long startTime = System.currentTimeMillis();
        StatementHandler statementHandler = (StatementHandler) target;
        try {
            return invocation.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            long cost = endTime - startTime;
            if (cost >= slowSqlMillis) {
                BoundSql boundSql = statementHandler.getBoundSql();

                if (configuration == null) {
                    var parameterHandler = (DefaultParameterHandler) statementHandler.getParameterHandler();
                    var configurationField = ReflectionUtils.findField(parameterHandler.getClass(), "configuration");
                    if (configurationField != null) {
                        ReflectionUtils.makeAccessible(configurationField);
                        this.configuration = (Configuration) configurationField.get(parameterHandler);
                    }
                }
                if (configuration != null) {
                    var sql = formatSql(boundSql, configuration);
                    log.info("Execute SQL：[ {} ] cost[ {} ms]", sql, cost);
                }
            }
        }
    }

    /**
     * Get param list
     *
     * @param configuration the configuration
     * @param boundSql      the bound sql
     *
     * @return the param list
     */
    private String formatSql(BoundSql boundSql, Configuration configuration) {
        String sql = boundSql.getSql();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        Object parameterObject = boundSql.getParameterObject();

        if (sql == null || sql.length() == 0) {
            return "";
        }
        if (configuration == null) {
            return "";
        }
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        sql = sql.replaceAll("[\\s\n ]+", " ");
        // DefaultParameterHandler
        if (parameterMappings != null) {
            for (ParameterMapping parameterMapping : parameterMappings) {
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (parameterObject == null) {
                        value = null;
                    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                        value = parameterObject;
                    } else {
                        MetaObject metaObject = configuration.newMetaObject(parameterObject);
                        value = metaObject.getValue(propertyName);
                    }
                    String paramValueStr = "";
                    if (value instanceof String) {
                        paramValueStr = "'" + value + "'";
                    } else if (value instanceof Date) {
                        paramValueStr = "'" + DATE_FORMAT_THREAD_LOCAL.get().format(value) + "'";
                    } else {
                        paramValueStr = String.valueOf(value);
                    }
                    sql = sql.replaceFirst("\\?", paramValueStr);
                }
            }
        }
        if (sql.length() > maxSqlLength) {
            return sql.substring(0, maxSqlLength);
        } else {
            return sql;
        }
    }
}