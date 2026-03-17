package com.rev.app.config;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.pagination.LegacyOracleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;

/**
 * Custom Oracle dialect for Oracle XE 10g/11g compatibility.
 * <p>
 * Oracle XE versions before 12c do not support the SQL:2008
 * {@code FETCH FIRST ? ROWS ONLY}
 * syntax. This dialect forces Hibernate to use the legacy {@code ROWNUM}-based
 * pagination
 * which is compatible with all Oracle versions.
 * </p>
 */
public class OracleLegacyDialect extends OracleDialect {

    private static final LimitHandler LIMIT_HANDLER = new LegacyOracleLimitHandler(DatabaseVersion.make(10, 2));

    @Override
    public LimitHandler getLimitHandler() {
        return LIMIT_HANDLER;
    }
}
