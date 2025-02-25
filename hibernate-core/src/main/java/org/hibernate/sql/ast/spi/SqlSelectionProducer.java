/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public interface SqlSelectionProducer {
	/**
	 * Create a SqlSelection for the given JDBC ResultSet position
	 *  @param jdbcPosition The index position used to read values from JDBC
	 * @param valuesArrayPosition The position in our {@linkplain RowProcessingState#getJdbcValue(SqlSelection) "current JDBC values array"}
	 * @param typeConfiguration The associated TypeConfiguration
	 */
	SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			TypeConfiguration typeConfiguration);
}
