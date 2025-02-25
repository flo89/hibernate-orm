/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public abstract class DelegatingTableGroup implements TableGroup {

	protected abstract TableGroup getTableGroup();

	@Override
	public ModelPart getExpressionType() {
		return getTableGroup().getExpressionType();
	}

	@Override
	public Expression getSqlExpression() {
		return getTableGroup().getSqlExpression();
	}

	@Override
	public ColumnReference getColumnReference() {
		return getTableGroup().getColumnReference();
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			TypeConfiguration typeConfiguration) {
		return getTableGroup().createSqlSelection( jdbcPosition, valuesArrayPosition, typeConfiguration );
	}

	@Override
	public TableReference resolveTableReference(NavigablePath navigablePath, String tableExpression) {
		return resolveTableReference( navigablePath, tableExpression, true );
	}

	@Override
	public TableReference resolveTableReference(String tableExpression) {
		return resolveTableReference( null, tableExpression, true );
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
		return getTableGroup().resolveTableReference( navigablePath, tableExpression, allowFkOptimization );
	}

	@Override
	public TableReference getTableReference(NavigablePath navigablePath, String tableExpression) {
		return getTableReference( navigablePath, tableExpression, true, false );
	}

	@Override
	public TableReference getTableReference(String tableExpression) {
		return getTableReference( null, tableExpression, true, false );
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve) {
		return getTableGroup().getTableReference( navigablePath, tableExpression, allowFkOptimization, resolve );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return getTableGroup().getNavigablePath();
	}

	@Override
	public String getGroupAlias() {
		return getTableGroup().getGroupAlias();
	}

	@Override
	public ModelPartContainer getModelPart() {
		return getTableGroup().getModelPart();
	}

	@Override
	public String getSourceAlias() {
		return getTableGroup().getSourceAlias();
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return getTableGroup().getTableGroupJoins();
	}

	@Override
	public List<TableGroupJoin> getNestedTableGroupJoins() {
		return getTableGroup().getNestedTableGroupJoins();
	}

	@Override
	public boolean canUseInnerJoins() {
		return getTableGroup().canUseInnerJoins();
	}

	@Override
	public boolean isLateral() {
		return getTableGroup().isLateral();
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		getTableGroup().addTableGroupJoin( join );
	}

	@Override
	public void prependTableGroupJoin(NavigablePath navigablePath, TableGroupJoin join) {
		getTableGroup().prependTableGroupJoin( navigablePath, join );
	}

	@Override
	public void addNestedTableGroupJoin(TableGroupJoin join) {
		getTableGroup().addNestedTableGroupJoin( join );
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		getTableGroup().visitTableGroupJoins( consumer );
	}

	@Override
	public void visitNestedTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		getTableGroup().visitNestedTableGroupJoins( consumer );
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		getTableGroup().applyAffectedTableNames( nameCollector );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return getTableGroup().getPrimaryTableReference();
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return getTableGroup().getTableReferenceJoins();
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return getTableGroup().createDomainResult( resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		getTableGroup().applySqlSelections( creationState );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		getTableGroup().accept( sqlTreeWalker );
	}

	@Override
	public boolean isRealTableGroup() {
		return getTableGroup().isRealTableGroup();
	}

	@Override
	public boolean isFetched() {
		return getTableGroup().isFetched();
	}

	@Override
	public boolean isInitialized() {
		return getTableGroup().isInitialized();
	}
}
