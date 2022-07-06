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

package ai.starwhale.mlops.agent.task.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.util.OptionHelper;
import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.boot.logging.logback.ColorConverter;
import org.springframework.boot.logging.logback.ExtendedWhitespaceThrowableProxyConverter;
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

public class LogConfigurator {

    public static void defaultConfigure(LoggerContext context) {
        // org.springframework.boot.logging.logback.DefaultLogbackConfiguration.defaults
        conversionRule("clr", ColorConverter.class, context);
        conversionRule("wex", WhitespaceThrowableProxyConverter.class, context);
        conversionRule("wEx", ExtendedWhitespaceThrowableProxyConverter.class, context);

        context.putProperty("CONSOLE_LOG_PATTERN", resolve(context, "${CONSOLE_LOG_PATTERN:-"
                + "%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) "
                + "%clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} "
                + "%clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"));
        String defaultCharset = Charset.defaultCharset().name();
        context.putProperty("CONSOLE_LOG_CHARSET",
                resolve(context, "${CONSOLE_LOG_CHARSET:-" + defaultCharset + "}"));
        context.putProperty("FILE_LOG_PATTERN", resolve(context, "${FILE_LOG_PATTERN:-"
                + "%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] "
                + "%-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"));
        context.putProperty("FILE_LOG_CHARSET",
                resolve(context, "${FILE_LOG_CHARSET:-" + defaultCharset + "}"));
    }

    public static void main(String[] args) {
        LoggerContext context = getLoggerContext();
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(context);
        patternLayout.setPattern(resolve(context, "${CONSOLE_LOG_PATTERN}"));
        patternLayout.setOutputPatternAsHeader(false);
        patternLayout.start();

        LoggingEvent loggingEvent = new LoggingEvent("a.g.Vaollo", "sw.test.Logger", context, Level.DEBUG, "hi!{}", new RuntimeException(), new Object[]{"gxx"});

        System.out.println(patternLayout.doLayout(loggingEvent));
    }

    private static void conversionRule(String conversionWord, Class<? extends Converter> converterClass, LoggerContext context) {
        Assert.hasLength(conversionWord, "Conversion word must not be empty");
        Assert.notNull(converterClass, "Converter class must not be null");
        Map<String, String> registry = (Map<String, String>) context.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (registry == null) {
            registry = new HashMap<>();
            context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry);
        }
        registry.put(conversionWord, converterClass.getName());
    }

    public static String resolve(LoggerContext context, String val) {
        return OptionHelper.substVars(val, context);
    }

    public static LoggerContext getLoggerContext() {
        ILoggerFactory factory = StaticLoggerBinder.getSingleton().getLoggerFactory();
        Assert.isInstanceOf(LoggerContext.class, factory,
                () -> String.format(
                        "LoggerFactory is not a Logback LoggerContext but Logback is on "
                                + "the classpath. Either remove Logback or the competing "
                                + "implementation (%s loaded from %s). If you are using "
                                + "WebLogic you will need to add 'org.slf4j' to "
                                + "prefer-application-packages in WEB-INF/weblogic.xml",
                        factory.getClass(), getLocation(factory)));
        return (LoggerContext) factory;
    }

    private static Object getLocation(ILoggerFactory factory) {
        try {
            ProtectionDomain protectionDomain = factory.getClass().getProtectionDomain();
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource != null) {
                return codeSource.getLocation();
            }
        } catch (SecurityException ex) {
            // Unable to determine location
        }
        return "unknown location";
    }
}
