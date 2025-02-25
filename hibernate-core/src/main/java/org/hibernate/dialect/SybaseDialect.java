/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.IntegralTimestampaddFunction;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.sql.StandardSqmTranslatorFactory;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsNullTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import jakarta.persistence.TemporalType;


/**
 * Superclass for all Sybase dialects.
 *
 * @author Brett Meyer
 */
public class SybaseDialect extends AbstractTransactSQLDialect {

	protected final boolean jtdsDriver;

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 16, 0 );

	//All Sybase dialects share an IN list size limit.
	private static final int PARAM_LIST_SIZE_LIMIT = 250000;

	public SybaseDialect() {
		this( MINIMUM_VERSION );
	}

	public SybaseDialect(DatabaseVersion version) {
		super(version);
		jtdsDriver = true;
	}

	public SybaseDialect(DialectResolutionInfo info) {
		super(info);
		jtdsDriver = info.getDriverName() != null
				&& info.getDriverName().contains( "jTDS" );
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		switch ( jdbcTypeCode ) {
			case Types.NUMERIC:
			case Types.DECIMAL:
				if ( precision == 19 && scale == 0 ) {
					return jdbcTypeRegistry.getDescriptor( Types.BIGINT );
				}
			case Types.TINYINT:
				if ( jtdsDriver ) {
					return jdbcTypeRegistry.getDescriptor( Types.SMALLINT );
				}
		}
		return super.resolveSqlTypeDescriptor(
				columnTypeName,
				jdbcTypeCode,
				precision,
				scale,
				jdbcTypeRegistry
		);
	}

	@Override
	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return new StandardSqmTranslatorFactory() {
			@Override
			public SqmTranslator<SelectStatement> createSelectTranslator(
					SqmSelectStatement<?> sqmSelectStatement,
					QueryOptions queryOptions,
					DomainParameterXref domainParameterXref,
					QueryParameterBindings domainParameterBindings,
					LoadQueryInfluencers loadQueryInfluencers,
					SqlAstCreationContext creationContext,
					boolean deduplicateSelectionItems) {
				return new SybaseSqmToSqlAstConverter<>(
						sqmSelectStatement,
						queryOptions,
						domainParameterXref,
						domainParameterBindings,
						loadQueryInfluencers,
						creationContext,
						deduplicateSelectionItems
				);
			}
		};
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SybaseSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		if ( jtdsDriver ) {
			jdbcTypeRegistry.addDescriptor( Types.TINYINT, SmallIntJdbcType.INSTANCE );

			// The jTDS driver doesn't support the JDBC4 signatures using 'long length' for stream bindings
			jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.CLOB_BINDING );

			// The jTDS driver doesn't support nationalized types
			jdbcTypeRegistry.addDescriptor( Types.NCLOB, ClobJdbcType.CLOB_BINDING );
			jdbcTypeRegistry.addDescriptor( Types.NVARCHAR, ClobJdbcType.CLOB_BINDING );
		}
		else {
			// Some Sybase drivers cannot support getClob.  See HHH-7889
			jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.STREAM_BINDING_EXTRACTING );
		}

		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.PRIMITIVE_ARRAY_BINDING );

		// Sybase requires a custom binder for binding untyped nulls with the NULL type
		typeContributions.contributeJdbcType( ObjectNullAsNullTypeJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullAsNullTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.getDescriptor( Object.class )
				)
		);
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		// At least the jTDS driver doesn't support this
		return jtdsDriver ? NationalizationSupport.IMPLICIT : super.getNationalizationSupport();
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(queryEngine);

		functionFactory.stddev();
		functionFactory.variance();
		functionFactory.stddevPopSamp_stdevp();
		functionFactory.varPopSamp_varp();
		functionFactory.stddevPopSamp();
		functionFactory.varPopSamp();

		// For SQL-Server we need to cast certain arguments to varchar(16384) to be able to concat them
		queryEngine.getSqmFunctionRegistry().register(
				"count",
				new CountFunction(
						this,
						queryEngine.getTypeConfiguration(),
						SqlAstNodeRenderingMode.DEFAULT,
						"count_big",
						"+",
						"varchar(16384)",
						false
				)
		);

		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.DEFAULT );

		//this doesn't work 100% on earlier versions of Sybase
		//which were missing the third parameter in charindex()
		//TODO: we could emulate it with substring() like in Postgres
		functionFactory.locate_charindex();

		functionFactory.replace_strReplace();
		functionFactory.everyAny_minMaxCase();
		functionFactory.octetLength_pattern( "datalength(?1)" );
		functionFactory.bitLength_pattern( "datalength(?1)*8" );

		queryEngine.getSqmFunctionRegistry().register( "timestampadd",
				new IntegralTimestampaddFunction( this, queryEngine.getTypeConfiguration() ) );
	}

	@Override
	public String getNullColumnString() {
		return " null";
	}

	@Override
	public boolean canCreateSchema() {
		// As far as I can tell, it does not
		return false;
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select db_name()";
	}

	@Override
	public int getMaxIdentifierLength() {
		return 128;
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		if ( to == CastType.STRING ) {
			switch ( from ) {
				case DATE:
					return "str_replace(convert(varchar,?1,102),'.','-')";
				case TIME:
					return "convert(varchar,?1,108)";
				case TIMESTAMP:
					return "str_replace(convert(varchar,?1,23),'T',' ')";
			}
		}
		return super.castPattern( from, to );
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case WEEK: return "calweekofyear"; //the ISO week number I think
			default: return super.translateExtractField(unit);
		}
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		//TODO!!
		return "datepart(?1,?2)";
	}

	@Override
	public boolean supportsFractionalTimestampArithmetic() {
		return false;
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		//TODO!!
		return "dateadd(?1,?2,?3)";
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		//TODO!!
		return "datediff(?1,?2,?3)";
	}

	@Override
	public String trimPattern(TrimSpec specification, char character) {
		return super.trimPattern(specification, character)
				.replace("replace", "str_replace");
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		throw new UnsupportedOperationException( "format() function not supported on Sybase");
	}

	@Override
	public boolean supportsStandardCurrentTimestampFunction() {
		return false;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {
		if ( dbMetaData == null ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( builder, dbMetaData );
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		if ( jtdsDriver ) {
			return NameQualifierSupport.CATALOG;
		}
		else {
			return NameQualifierSupport.BOTH;
		}
	}

}
