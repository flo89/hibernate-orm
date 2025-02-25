/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.Remove;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.ContributableDatabaseObject;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;

import org.jboss.logging.Logger;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * A mapping model object representing a relational database {@linkplain jakarta.persistence.Table table}.
 *
 * @author Gavin King
 */
public class Table implements Serializable, ContributableDatabaseObject {
	private static final Logger log = Logger.getLogger( Table.class );
	private static final Column[] EMPTY_COLUMN_ARRAY = new Column[0];

	private final String contributor;

	private Identifier catalog;
	private Identifier schema;
	private Identifier name;

	/**
	 * contains all columns, including the primary key
	 */
	private final Map<String, Column> columns = new LinkedHashMap<>();
	private KeyValue idValue;
	private PrimaryKey primaryKey;
	private final Map<ForeignKeyKey, ForeignKey> foreignKeys = new LinkedHashMap<>();
	private final Map<String, Index> indexes = new LinkedHashMap<>();
	private final Map<String,UniqueKey> uniqueKeys = new LinkedHashMap<>();
	private int uniqueInteger;
	private final List<String> checkConstraints = new ArrayList<>();
	private String rowId;
	private String subselect;
	private boolean isAbstract;
	private boolean hasDenormalizedTables;
	private String comment;

	private List<Function<SqlStringGenerationContext, InitCommand>> initCommandProducers;

	@Deprecated(since="6.2") @Remove
	public Table() {
		this( "orm" );
	}

	public Table(String contributor) {
		this( contributor, null );
	}

	public Table(String contributor, String name) {
		this.contributor = contributor;
		setName( name );
	}

	public Table(
			String contributor,
			Namespace namespace,
			Identifier physicalTableName,
			boolean isAbstract) {
		this.contributor = contributor;
		this.catalog = namespace.getPhysicalName().getCatalog();
		this.schema = namespace.getPhysicalName().getSchema();
		this.name = physicalTableName;
		this.isAbstract = isAbstract;
	}

	public Table(
			String contributor,
			Namespace namespace,
			Identifier physicalTableName,
			String subselect,
			boolean isAbstract) {
		this.contributor = contributor;
		this.catalog = namespace.getPhysicalName().getCatalog();
		this.schema = namespace.getPhysicalName().getSchema();
		this.name = physicalTableName;
		this.subselect = subselect;
		this.isAbstract = isAbstract;
	}

	public Table(String contributor, Namespace namespace, String subselect, boolean isAbstract) {
		this.contributor = contributor;
		this.catalog = namespace.getPhysicalName().getCatalog();
		this.schema = namespace.getPhysicalName().getSchema();
		this.subselect = subselect;
		this.isAbstract = isAbstract;
	}

	@Override
	public String getContributor() {
		return contributor;
	}

	public String getQualifiedName(SqlStringGenerationContext context) {
		return subselect != null
				? "( " + subselect + " )"
				: context.format( new QualifiedTableName( catalog, schema, name ) );
	}

	/**
	 * @deprecated Should build a {@link QualifiedTableName}
	 * then use {@link SqlStringGenerationContext#format(QualifiedTableName)}.
	 */
	@Deprecated
	public static String qualify(String catalog, String schema, String table) {
		final StringBuilder qualifiedName = new StringBuilder();
		if ( catalog != null ) {
			qualifiedName.append( catalog ).append( '.' );
		}
		if ( schema != null ) {
			qualifiedName.append( schema ).append( '.' );
		}
		return qualifiedName.append( table ).toString();
	}

	public void setName(String name) {
		this.name = Identifier.toIdentifier( name );
	}

	public String getName() {
		return name == null ? null : name.getText();
	}

	public Identifier getNameIdentifier() {
		return name;
	}

	public String getQuotedName() {
		return name == null ? null : name.toString();
	}

	public String getQuotedName(Dialect dialect) {
		return name == null ? null : name.render( dialect );
	}

	public QualifiedTableName getQualifiedTableName() {
		return name == null ? null : new QualifiedTableName( catalog, schema, name );
	}

	public boolean isQuoted() {
		return name.isQuoted();
	}

	public void setQuoted(boolean quoted) {
		if ( quoted == name.isQuoted() ) {
			return;
		}
		this.name = new Identifier( name.getText(), quoted );
	}

	public void setSchema(String schema) {
		this.schema = Identifier.toIdentifier( schema );
	}

	public String getSchema() {
		return schema == null ? null : schema.getText();
	}

	public String getQuotedSchema() {
		return schema == null ? null : schema.toString();
	}

	public String getQuotedSchema(Dialect dialect) {
		return schema == null ? null : schema.render( dialect );
	}

	public boolean isSchemaQuoted() {
		return schema != null && schema.isQuoted();
	}

	public void setCatalog(String catalog) {
		this.catalog = Identifier.toIdentifier( catalog );
	}

	public String getCatalog() {
		return catalog == null ? null : catalog.getText();
	}

	public String getQuotedCatalog() {
		return catalog == null ? null : catalog.render();
	}

	public String getQuotedCatalog(Dialect dialect) {
		return catalog == null ? null : catalog.render( dialect );
	}

	public boolean isCatalogQuoted() {
		return catalog != null && catalog.isQuoted();
	}

	/**
	 * Return the column which is identified by column provided as argument.
	 *
	 * @param column column with at least a name.
	 * @return the underlying column or null if not inside this table.
	 *         Note: the instance *can* be different than the input parameter,
	 *         but the name will be the same.
	 */
	public Column getColumn(Column column) {
		if ( column == null ) {
			return null;
		}
		else {
			final Column existing = columns.get( column.getCanonicalName() );
			return column.equals( existing ) ? existing : null;
		}
	}

	public Column getColumn(Identifier name) {
		if ( name == null ) {
			return null;
		}
		return columns.get( name.getCanonicalName() );
	}

	public Column getColumn(int n) {
		final Iterator<Column> iter = columns.values().iterator();
		for ( int i = 0; i < n - 1; i++ ) {
			iter.next();
		}
		return iter.next();
	}

	public void addColumn(Column column) {
		final Column old = getColumn( column );
		if ( old == null ) {
			if ( primaryKey != null ) {
				for ( Column pkColumn : primaryKey.getColumns() ) {
					if ( pkColumn.getCanonicalName().equals( column.getCanonicalName() ) ) {
						column.setNullable( false );
						if ( log.isDebugEnabled() ) {
							log.debugf(
									"Forcing column [%s] to be non-null as it is part of the primary key for table [%s]",
									column.getCanonicalName(),
									getNameIdentifier().getCanonicalName()
							);
						}
					}
				}
			}
			columns.put( column.getCanonicalName(), column );
			column.uniqueInteger = columns.size();
		}
		else {
			column.uniqueInteger = old.uniqueInteger;
		}
	}

	public int getColumnSpan() {
		return columns.size();
	}

	@Deprecated(since = "6.0")
	public Iterator<Column> getColumnIterator() {
		return getColumns().iterator();
	}

	public Collection<Column> getColumns() {
		return columns.values();
	}

	@Deprecated(since = "6.2")
	public Iterator<Index> getIndexIterator() {
		return getIndexes().values().iterator();
	}

	public Map<String, Index> getIndexes() {
		return unmodifiableMap( indexes );
	}

	@Deprecated(since = "6.0")
	public Iterator<ForeignKey> getForeignKeyIterator() {
		return getForeignKeys().values().iterator();
	}

	public Map<ForeignKeyKey, ForeignKey> getForeignKeys() {
		return unmodifiableMap( foreignKeys );
	}

	@Deprecated(since = "6.0")
	public Iterator<UniqueKey> getUniqueKeyIterator() {
		return getUniqueKeys().values().iterator();
	}

	public Map<String, UniqueKey> getUniqueKeys() {
		cleanseUniqueKeyMapIfNeeded();
		return unmodifiableMap( uniqueKeys );
	}

	private int sizeOfUniqueKeyMapOnLastCleanse;

	private void cleanseUniqueKeyMapIfNeeded() {
		if ( uniqueKeys.size() == sizeOfUniqueKeyMapOnLastCleanse ) {
			// nothing to do
			return;
		}
		cleanseUniqueKeyMap();
		sizeOfUniqueKeyMapOnLastCleanse = uniqueKeys.size();
	}

	private void cleanseUniqueKeyMap() {
		// We need to account for a few conditions here...
		// 	1) If there are multiple unique keys contained in the uniqueKeys Map, we need to deduplicate
		// 		any sharing the same columns as other defined unique keys; this is needed for the annotation
		// 		processor since it creates unique constraints automagically for the user
		//	2) Remove any unique keys that share the same columns as the primary key; again, this is
		//		needed for the annotation processor to handle @Id @OneToOne cases.  In such cases the
		//		unique key is unnecessary because a primary key is already unique by definition.  We handle
		//		this case specifically because some databases fail if you try to apply a unique key to
		//		the primary key columns which causes schema export to fail in these cases.
		if ( !uniqueKeys.isEmpty() ) {
			if ( uniqueKeys.size() == 1 ) {
				// we have to worry about condition 2 above, but not condition 1
				final Map.Entry<String,UniqueKey> uniqueKeyEntry = uniqueKeys.entrySet().iterator().next();
				if ( isSameAsPrimaryKeyColumns( uniqueKeyEntry.getValue() ) ) {
					uniqueKeys.remove( uniqueKeyEntry.getKey() );
				}
			}
			else {
				// we have to check both conditions 1 and 2
				final Iterator<Map.Entry<String,UniqueKey>> uniqueKeyEntries = uniqueKeys.entrySet().iterator();
				while ( uniqueKeyEntries.hasNext() ) {
					final Map.Entry<String,UniqueKey> uniqueKeyEntry = uniqueKeyEntries.next();
					final UniqueKey uniqueKey = uniqueKeyEntry.getValue();
					boolean removeIt = false;

					// condition 1 : check against other unique keys
					for ( UniqueKey otherUniqueKey : uniqueKeys.values() ) {
						// make sure it's not the same unique key
						if ( uniqueKeyEntry.getValue() == otherUniqueKey ) {
							continue;
						}
						if ( otherUniqueKey.getColumns().containsAll( uniqueKey.getColumns() )
								&& uniqueKey.getColumns().containsAll( otherUniqueKey.getColumns() ) ) {
							removeIt = true;
							break;
						}
					}

					// condition 2 : check against pk
					if ( isSameAsPrimaryKeyColumns( uniqueKeyEntry.getValue() ) ) {
						removeIt = true;
					}

					if ( removeIt ) {
						//uniqueKeys.remove( uniqueKeyEntry.getKey() );
						uniqueKeyEntries.remove();
					}
				}
			}
		}
	}

	private boolean isSameAsPrimaryKeyColumns(UniqueKey uniqueKey) {
		if ( primaryKey == null || primaryKey.getColumns().isEmpty() ) {
			// happens for many-to-many tables
			return false;
		}
		return primaryKey.getColumns().containsAll( uniqueKey.getColumns() )
			&& uniqueKey.getColumns().containsAll( primaryKey.getColumns() );
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (catalog == null ? 0 : catalog.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (schema == null ? 0 : schema.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof Table && equals((Table) object);
	}

	public boolean equals(Table table) {
		if ( null == table ) {
			return false;
		}
		else if ( this == table ) {
			return true;
		}
		else {
			return Identifier.areEqual( name, table.name )
				&& Identifier.areEqual( schema, table.schema )
				&& Identifier.areEqual( catalog, table.catalog );
		}
	}

	public Iterator<String> sqlAlterStrings(
			Dialect dialect,
			Metadata metadata,
			TableInformation tableInfo,
			SqlStringGenerationContext sqlStringGenerationContext) throws HibernateException {
		final String tableName = sqlStringGenerationContext.format( new QualifiedTableName( catalog, schema, name ) );

		final StringBuilder root = new StringBuilder( dialect.getAlterTableString( tableName ) )
				.append( ' ' )
				.append( dialect.getAddColumnString() );

		final List<String> results = new ArrayList<>();

		for ( Column column : getColumns() ) {
			final ColumnInformation columnInfo = tableInfo.getColumn(
					Identifier.toIdentifier( column.getName(), column.isQuoted() )
			);

			if ( columnInfo == null ) {
				// the column doesn't exist at all.
				final StringBuilder alter = new StringBuilder( root.toString() )
						.append( ' ' )
						.append( column.getQuotedName( dialect ) );

				final String columnType = column.getSqlType(
						metadata.getDatabase().getTypeConfiguration(),
						dialect,
						metadata
				);
				if ( column.getGeneratedAs()==null || dialect.hasDataTypeBeforeGeneratedAs() ) {
					alter.append( ' ' ).append( columnType );
				}

				final String defaultValue = column.getDefaultValue();
				if ( defaultValue != null ) {
					alter.append( " default " ).append( defaultValue );
				}

				final String generatedAs = column.getGeneratedAs();
				if ( generatedAs != null) {
					alter.append( dialect.generatedAs( generatedAs ) );
				}

				if ( column.isNullable() ) {
					alter.append( dialect.getNullColumnString( columnType ) );
				}
				else {
					alter.append( " not null" );
				}

				if ( column.isUnique() ) {
					String keyName = Constraint.generateName( "UK_", this, column );
					UniqueKey uk = getOrCreateUniqueKey( keyName );
					uk.addColumn( column );
					alter.append( dialect.getUniqueDelegate()
							.getColumnDefinitionUniquenessFragment( column, sqlStringGenerationContext ) );
				}

				final String checkConstraint = column.checkConstraint();
				if ( checkConstraint !=null && dialect.supportsColumnCheck() ) {
					alter.append( checkConstraint );
				}

				final String columnComment = column.getComment();
				if ( columnComment != null ) {
					alter.append( dialect.getColumnComment( columnComment ) );
				}

				alter.append( dialect.getAddColumnSuffixString() );

				results.add( alter.toString() );
			}

		}

		if ( results.isEmpty() ) {
			log.debugf( "No alter strings for table : %s", getQuotedName() );
		}

		return results.iterator();
	}

	public boolean hasPrimaryKey() {
		return getPrimaryKey() != null;
	}

	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(PrimaryKey primaryKey) {
		this.primaryKey = primaryKey;
	}

	public Index getOrCreateIndex(String indexName) {
		Index index =  indexes.get( indexName );
		if ( index == null ) {
			index = new Index();
			index.setName( indexName );
			index.setTable( this );
			indexes.put( indexName, index );
		}
		return index;
	}

	public Index getIndex(String indexName) {
		return  indexes.get( indexName );
	}

	public Index addIndex(Index index) {
		Index current =  indexes.get( index.getName() );
		if ( current != null ) {
			throw new MappingException( "Index " + index.getName() + " already exists" );
		}
		indexes.put( index.getName(), index );
		return index;
	}

	public UniqueKey addUniqueKey(UniqueKey uniqueKey) {
		UniqueKey current = uniqueKeys.get( uniqueKey.getName() );
		if ( current != null ) {
			throw new MappingException( "UniqueKey " + uniqueKey.getName() + " already exists" );
		}
		uniqueKeys.put( uniqueKey.getName(), uniqueKey );
		return uniqueKey;
	}

	public UniqueKey createUniqueKey(List<Column> keyColumns) {
		String keyName = Constraint.generateName( "UK_", this, keyColumns );
		UniqueKey uniqueKey = getOrCreateUniqueKey( keyName );
		for (Column keyColumn : keyColumns) {
			uniqueKey.addColumn( keyColumn );
		}
		return uniqueKey;
	}

	public UniqueKey getUniqueKey(String keyName) {
		return uniqueKeys.get( keyName );
	}

	public UniqueKey getOrCreateUniqueKey(String keyName) {
		UniqueKey uniqueKey = uniqueKeys.get( keyName );
		if ( uniqueKey == null ) {
			uniqueKey = new UniqueKey();
			uniqueKey.setName( keyName );
			uniqueKey.setTable( this );
			uniqueKeys.put( keyName, uniqueKey );
		}
		return uniqueKey;
	}

	public void createForeignKeys() {
	}

	public ForeignKey createForeignKey(String keyName, List<Column> keyColumns, String referencedEntityName, String keyDefinition) {
		return createForeignKey( keyName, keyColumns, referencedEntityName, keyDefinition, null );
	}

	public ForeignKey createForeignKey(
			String keyName,
			List<Column> keyColumns,
			String referencedEntityName,
			String keyDefinition,
			List<Column> referencedColumns) {
		final ForeignKeyKey key = new ForeignKeyKey( keyColumns, referencedEntityName, referencedColumns );

		ForeignKey foreignKey = foreignKeys.get( key );
		if ( foreignKey == null ) {
			foreignKey = new ForeignKey();
			foreignKey.setTable( this );
			foreignKey.setReferencedEntityName( referencedEntityName );
			foreignKey.setKeyDefinition( keyDefinition );
			for (Column keyColumn : keyColumns) {
				foreignKey.addColumn( keyColumn );
			}
			if ( referencedColumns != null ) {
				foreignKey.addReferencedColumns( referencedColumns );
			}

			// NOTE : if the name is null, we will generate an implicit name during second pass processing
			// after we know the referenced table name (which might not be resolved yet).
			foreignKey.setName( keyName );

			foreignKeys.put( key, foreignKey );
		}

		if ( keyName != null ) {
			foreignKey.setName( keyName );
		}

		return foreignKey;
	}


	// This must be done outside of Table, rather than statically, to ensure
	// deterministic alias names.  See HHH-2448.
	public void setUniqueInteger( int uniqueInteger ) {
		this.uniqueInteger = uniqueInteger;
	}

	public int getUniqueInteger() {
		return uniqueInteger;
	}

	public void setIdentifierValue(KeyValue idValue) {
		this.idValue = idValue;
	}

	public KeyValue getIdentifierValue() {
		return idValue;
	}

	public void addCheckConstraint(String constraint) {
		checkConstraints.add( constraint );
	}

	public boolean containsColumn(Column column) {
		return columns.containsValue( column );
	}

	public String getRowId() {
		return rowId;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	public String toString() {
		final StringBuilder buf = new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( '(' );
		if ( getCatalog() != null ) {
			buf.append( getCatalog() ).append( "." );
		}
		if ( getSchema() != null ) {
			buf.append( getSchema() ).append( "." );
		}
		buf.append( getName() ).append( ')' );
		return buf.toString();
	}

	public String getSubselect() {
		return subselect;
	}

	public void setSubselect(String subselect) {
		this.subselect = subselect;
	}

	public boolean isSubselect() {
		return subselect != null;
	}

	public boolean isAbstractUnionTable() {
		return hasDenormalizedTables() && isAbstract;
	}

	public boolean hasDenormalizedTables() {
		return hasDenormalizedTables;
	}

	void setHasDenormalizedTables() {
		hasDenormalizedTables = true;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public boolean isPhysicalTable() {
		return !isSubselect() && !isAbstractUnionTable();
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Deprecated(since = "6.0")
	public Iterator<String> getCheckConstraintsIterator() {
		return getCheckConstraints().iterator();
	}

	public List<String> getCheckConstraints() {
		return unmodifiableList( checkConstraints );
	}

	@Override
	public String getExportIdentifier() {
		return Table.qualify( render( catalog ), render( schema ), name.render() );
	}

	private String render(Identifier identifier) {
		return identifier == null ? null : identifier.render();
	}

	public static class ForeignKeyKey implements Serializable {
		private final String referencedClassName;
		private final Column[] columns;
		private final Column[] referencedColumns;

		ForeignKeyKey(List<Column> columns, String referencedClassName, List<Column> referencedColumns) {
			Objects.requireNonNull( columns );
			Objects.requireNonNull( referencedClassName );
			this.referencedClassName = referencedClassName;
			this.columns = columns.toArray( EMPTY_COLUMN_ARRAY );
			this.referencedColumns = referencedColumns != null
					? referencedColumns.toArray(EMPTY_COLUMN_ARRAY)
					: EMPTY_COLUMN_ARRAY;
		}

		public int hashCode() {
			return Arrays.hashCode( columns ) + Arrays.hashCode( referencedColumns );
		}

		public boolean equals(Object other) {
			ForeignKeyKey fkk = (ForeignKeyKey) other;
			return fkk != null
				&& Arrays.equals( fkk.columns, columns )
				&& Arrays.equals( fkk.referencedColumns, referencedColumns );
		}

		@Override
		public String toString() {
			return "ForeignKeyKey{columns=" + Arrays.toString( columns ) +
					", referencedClassName='" + referencedClassName +
					"', referencedColumns=" + Arrays.toString( referencedColumns ) +
					'}';
		}
	}

	/**
	 * @deprecated Use {@link #addInitCommand(Function)} instead.
	 */
	@Deprecated
	public void addInitCommand(InitCommand command) {
		addInitCommand( ignored -> command );
	}

	public void addInitCommand(Function<SqlStringGenerationContext, InitCommand> commandProducer) {
		if ( initCommandProducers == null ) {
			initCommandProducers = new ArrayList<>();
		}
		initCommandProducers.add( commandProducer );
	}

	public List<InitCommand> getInitCommands(SqlStringGenerationContext context) {
		if ( initCommandProducers == null ) {
			return Collections.emptyList();
		}
		else {
			final List<InitCommand> initCommands = new ArrayList<>();
			for ( Function<SqlStringGenerationContext, InitCommand> producer : initCommandProducers ) {
				initCommands.add( producer.apply( context ) );
			}
			return unmodifiableList( initCommands );
		}
	}
}
