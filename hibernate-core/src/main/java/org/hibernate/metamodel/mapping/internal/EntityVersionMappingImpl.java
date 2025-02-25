/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.UnsavedValueFactory;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.VersionValue;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.VersionJavaType;

/**
 * @author Steve Ebersole
 */
public class EntityVersionMappingImpl implements EntityVersionMapping, FetchOptions {
	private final String attributeName;
	private final EntityMappingType declaringType;

	private final String columnTableExpression;
	private final String columnExpression;
	private final String columnDefinition;
	private final Long length;
	private final Integer precision;
	private final Integer scale;

	private final BasicType versionBasicType;

	private final VersionValue unsavedValueStrategy;

	private BasicAttributeMapping attributeMapping;

	public EntityVersionMappingImpl(
			RootClass bootEntityDescriptor,
			Supplier<?> templateInstanceAccess,
			String attributeName,
			String columnTableExpression,
			String columnExpression,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			BasicType<?> versionBasicType,
			EntityMappingType declaringType,
			MappingModelCreationProcess creationProcess) {
		this.attributeName = attributeName;
		this.columnDefinition = columnDefinition;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
		this.declaringType = declaringType;

		this.columnTableExpression = columnTableExpression;
		this.columnExpression = columnExpression;

		this.versionBasicType = versionBasicType;

		unsavedValueStrategy = UnsavedValueFactory.getUnsavedVersionValue(
				(KeyValue) bootEntityDescriptor.getVersion().getValue(),
				(VersionJavaType<?>) versionBasicType.getJavaTypeDescriptor(),
				length,
				precision,
				scale,
				declaringType
						.getRepresentationStrategy()
						.resolvePropertyAccess( bootEntityDescriptor.getVersion() )
						.getGetter(),
				templateInstanceAccess
		);
	}

	@Override
	public BasicAttributeMapping getVersionAttribute() {
		return (BasicAttributeMapping) declaringType.findAttributeMapping( attributeName );
	}

	@Override
	public VersionValue getUnsavedStrategy() {
		return unsavedValueStrategy;
	}

	@Override
	public String getContainingTableExpression() {
		return columnTableExpression;
	}

	@Override
	public String getSelectionExpression() {
		return columnExpression;
	}

	@Override
	public boolean isFormula() {
		return false;
	}

	@Override
	public String getCustomReadExpression() {
		return null;
	}

	@Override
	public String getCustomWriteExpression() {
		return null;
	}

	@Override
	public String getColumnDefinition() {
		return columnDefinition;
	}

	@Override
	public Long getLength() {
		return length;
	}

	@Override
	public Integer getPrecision() {
		return precision;
	}

	@Override
	public Integer getScale() {
		return scale;
	}

	@Override
	public MappingType getPartMappingType() {
		return versionBasicType;
	}

	@Override
	public MappingType getMappedType() {
		return versionBasicType;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return versionBasicType.getJdbcMapping();
	}

	@Override
	public VersionJavaType<?> getJavaType() {
		return (VersionJavaType<?>) versionBasicType.getJavaTypeDescriptor();
	}

	@Override
	public String getPartName() {
		return attributeName;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return getVersionAttribute().getNavigableRole();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return declaringType;
	}

	@Override
	public String getFetchableName() {
		return attributeName;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().findTableGroup( fetchParent.getNavigablePath() );

		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference columnTableReference = tableGroup.resolveTableReference( fetchablePath, columnTableExpression );

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression( columnTableReference, this ),
				versionBasicType.getJdbcJavaType(),
				fetchParent,
				sqlAstCreationState.getCreationContext().getSessionFactory().getTypeConfiguration()
		);

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				fetchTiming,
				creationState
		);
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				versionBasicType,
				navigablePath
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		resolveSqlSelection( tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept( resolveSqlSelection( tableGroup, creationState ), getJdbcMapping() );
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( domainValue, this );
	}

	private SqlSelection resolveSqlSelection(TableGroup tableGroup, DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();

		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference columnTableReference = tableGroup.resolveTableReference(
				tableGroup.getNavigablePath()
						.append( getNavigableRole().getNavigableName() ),
				columnTableExpression
		);

		return sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression( columnTableReference, this ),
				versionBasicType.getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getSessionFactory().getTypeConfiguration()
		);
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return versionBasicType.disassemble( value, session );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return versionBasicType.forEachDisassembledJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
