/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementHelper;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * A mapping model object representing a property or field of an {@link PersistentClass entity}
 * or {@link Component embeddable class}.
 *
 * @author Gavin King
 */
public class Property implements Serializable, MetaAttributable {
	private String name;
	private Value value;
	private String cascade;
	private boolean updateable = true;
	private boolean insertable = true;
	private boolean selectable = true;
	private boolean optimisticLocked = true;
	private ValueGeneration valueGenerationStrategy;
	private String propertyAccessorName;
	private PropertyAccessStrategy propertyAccessStrategy;
	private boolean lazy;
	private String lazyGroup;
	private boolean optional;
	private java.util.Map metaAttributes;
	private PersistentClass persistentClass;
	private boolean naturalIdentifier;
	private boolean lob;
	private java.util.List<CallbackDefinition> callbackDefinitions;
	private String returnedClassName;

	public boolean isBackRef() {
		return false;
	}

	/**
	 * Does this property represent a synthetic property?  A synthetic property is one we create during
	 * metamodel binding to represent a collection of columns but which does not represent a property
	 * physically available on the entity.
	 *
	 * @return True if synthetic; false otherwise.
	 */
	public boolean isSynthetic() {
		return false;
	}

	public Type getType() throws MappingException {
		return value.getType();
	}
	
	public int getColumnSpan() {
		return value.getColumnSpan();
	}

	/**
	 * @deprecated moving away from the use of {@link Iterator} as a return type
	 */
	@Deprecated(since = "6.0")
	public Iterator<Selectable> getColumnIterator() {
		return value.getColumnIterator();
	}

	/**
	 * Delegates to {@link Value#getSelectables()}.
	 */
	public java.util.List<Selectable> getSelectables() {
		return value.getSelectables();
	}

	/**
	 * Delegates to {@link Value#getColumns()}.
	 *
	 * @throws org.hibernate.AssertionFailure if the mapping involves formulas
	 */
	public java.util.List<Column> getColumns() {
		return value.getColumns();
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isComposite() {
		return value instanceof Component;
	}

	public Value getValue() {
		return value;
	}

	public void resetUpdateable(boolean updateable) {
		setUpdateable(updateable);
		boolean[] columnUpdateability = getValue().getColumnUpdateability();
		for (int i=0; i<getColumnSpan(); i++ ) {
			columnUpdateability[i] = updateable;
		}
	}

	public void resetOptional(boolean optional) {
		setOptional(optional);
		for ( Selectable column: getValue().getSelectables() ) {
			if (column instanceof Column) {
				( (Column) column ).setNullable(optional);
			}
		}
	}

	public boolean isPrimitive(Class clazz) {
		return getGetter(clazz).getReturnTypeClass().isPrimitive();
	}

	public CascadeStyle getCascadeStyle() throws MappingException {
		Type type = value.getType();
		if ( type.isComponentType() ) {
			return getCompositeCascadeStyle( (CompositeType) type, cascade );
		}
		else if ( type.isCollectionType() ) {
			return getCollectionCascadeStyle( ( (Collection) value ).getElement().getType(), cascade );
		}
		else {
			return getCascadeStyle( cascade );			
		}
	}

	private static CascadeStyle getCompositeCascadeStyle(CompositeType compositeType, String cascade) {
		if ( compositeType.isAnyType() ) {
			return getCascadeStyle( cascade );
		}
		int length = compositeType.getSubtypes().length;
		for ( int i=0; i<length; i++ ) {
			if ( compositeType.getCascadeStyle(i) != CascadeStyles.NONE ) {
				return CascadeStyles.ALL;
			}
		}
		return getCascadeStyle( cascade );
	}

	private static CascadeStyle getCollectionCascadeStyle(Type elementType, String cascade) {
		if ( elementType.isComponentType() ) {
			return getCompositeCascadeStyle( (CompositeType) elementType, cascade );
		}
		else {
			return getCascadeStyle( cascade );
		}
	}
	
	private static CascadeStyle getCascadeStyle(String cascade) {
		if ( cascade==null || cascade.equals("none") ) {
			return CascadeStyles.NONE;
		}
		else {
			StringTokenizer tokens = new StringTokenizer(cascade, ", ");
			CascadeStyle[] styles = new CascadeStyle[ tokens.countTokens() ] ;
			int i=0;
			while ( tokens.hasMoreTokens() ) {
				styles[i++] = CascadeStyles.getCascadeStyle( tokens.nextToken() );
			}
			return new CascadeStyles.MultipleCascadeStyle(styles);
		}		
	}
	
	public String getCascade() {
		return cascade;
	}

	public void setCascade(String cascade) {
		this.cascade = cascade;
	}

	public void setName(String name) {
		this.name = name==null ? null : name.intern();
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public boolean isUpdateable() {
		// if the property mapping consists of all formulas,
		// make it non-updatable
		return updateable && value.hasAnyUpdatableColumns();
	}

	public boolean isInsertable() {
		// if the property mapping consists of all formulas, 
		// make it non-insertable
		return insertable && value.hasAnyInsertableColumns();
	}

	public ValueGeneration getValueGenerationStrategy() {
		return valueGenerationStrategy;
	}

	public void setValueGenerationStrategy(ValueGeneration valueGenerationStrategy) {
		this.valueGenerationStrategy = valueGenerationStrategy;
	}

	public void setUpdateable(boolean mutable) {
		this.updateable = mutable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	public void setPropertyAccessorName(String string) {
		propertyAccessorName = string;
	}

	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return propertyAccessStrategy;
	}

	public void setPropertyAccessStrategy(PropertyAccessStrategy propertyAccessStrategy) {
		this.propertyAccessStrategy = propertyAccessStrategy;
	}

	/**
	 * Approximate!
	 */
	boolean isNullable() {
		return value==null || value.isNullable();
	}

	public boolean isBasicPropertyAccessor() {
		return propertyAccessorName==null || "property".equals( propertyAccessorName );
	}

	public Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
	}

	public MetaAttribute getMetaAttribute(String attributeName) {
		return metaAttributes==null?null:(MetaAttribute) metaAttributes.get(attributeName);
	}

	public void setMetaAttributes(Map<String, MetaAttribute> metas) {
		this.metaAttributes = metas;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		return getValue().isValid( mapping );
	}

	public String toString() {
		return getClass().getSimpleName() + '(' + name + ')';
	}
	
	public void setLazy(boolean lazy) {
		this.lazy=lazy;
	}

	/**
	 * Is this property lazy in the "bytecode" sense?
	 *
	 * Lazy here means whether we should push *something* to the entity
	 * instance for this field in its "base fetch group".  Mainly it affects
	 * whether we should list this property's columns in the SQL select
	 * for the owning entity when we load its "base fetch group".
	 *
	 * The "something" we push varies based on the nature (basic, etc) of
	 * the property.
	 *
	 * @apiNote This form reports whether the property is considered part of the
	 * base fetch group based solely on the mapping information.  However,
	 * {@link EnhancementHelper#includeInBaseFetchGroup} is used internally to make that
	 * decision to account for other details
	 */
	public boolean isLazy() {
		if ( value instanceof ToOne ) {
			// For a many-to-one, this is always false.  Whether the
			// association is EAGER, PROXY or NO-PROXY we want the fk
			// selected
			return false;
		}

		return lazy;
	}

	public String getLazyGroup() {
		return lazyGroup;
	}

	public void setLazyGroup(String lazyGroup) {
		this.lazyGroup = lazyGroup;
	}

	public boolean isOptimisticLocked() {
		return optimisticLocked;
	}

	public void setOptimisticLocked(boolean optimisticLocked) {
		this.optimisticLocked = optimisticLocked;
	}
	
	public boolean isOptional() {
		return optional || isNullable();
	}
	
	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	public boolean isSelectable() {
		return selectable;
	}
	
	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	public String getAccessorPropertyName(RepresentationMode mode) {
		return getName();
	}

	// todo : remove
	public Getter getGetter(Class clazz) throws PropertyNotFoundException, MappingException {
		return getPropertyAccessStrategy( clazz ).buildPropertyAccess( clazz, name, true ).getGetter();
	}

	// todo : remove
	public Setter getSetter(Class clazz) throws PropertyNotFoundException, MappingException {
		return getPropertyAccessStrategy( clazz ).buildPropertyAccess( clazz, name, true ).getSetter();
	}

	// todo : remove
	public PropertyAccessStrategy getPropertyAccessStrategy(Class clazz) throws MappingException {
		final PropertyAccessStrategy propertyAccessStrategy = getPropertyAccessStrategy();
		if ( propertyAccessStrategy != null ) {
			return propertyAccessStrategy;
		}
		String accessName = getPropertyAccessorName();
		if ( accessName == null ) {
			if ( clazz == null || java.util.Map.class.equals( clazz ) ) {
				accessName = "map";
			}
			else {
				accessName = "property";
			}
		}

		final RepresentationMode representationMode = clazz == null || java.util.Map.class.equals( clazz )
				? RepresentationMode.MAP
				: RepresentationMode.POJO;

		return resolveServiceRegistry().getService( PropertyAccessStrategyResolver.class ).resolvePropertyAccessStrategy(
				clazz,
				accessName,
				representationMode
		);
	}

	protected ServiceRegistry resolveServiceRegistry() {
		if ( getPersistentClass() != null ) {
			return getPersistentClass().getServiceRegistry();
		}
		if ( getValue() != null ) {
			return getValue().getServiceRegistry();
		}
		throw new HibernateException( "Could not resolve ServiceRegistry" );
	}

	public boolean isNaturalIdentifier() {
		return naturalIdentifier;
	}

	public void setNaturalIdentifier(boolean naturalIdentifier) {
		this.naturalIdentifier = naturalIdentifier;
	}

	public boolean isLob() {
		return lob;
	}

	public void setLob(boolean lob) {
		this.lob = lob;
	}

	public void addCallbackDefinitions(java.util.List<CallbackDefinition> callbackDefinitions) {
		if ( callbackDefinitions == null || callbackDefinitions.isEmpty() ) {
			return;
		}
		if ( this.callbackDefinitions == null ) {
			this.callbackDefinitions = new ArrayList<>();
		}
		this.callbackDefinitions.addAll( callbackDefinitions );
	}

	public java.util.List<CallbackDefinition> getCallbackDefinitions() {
		if ( callbackDefinitions == null ) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList( callbackDefinitions );
	}

	public String getReturnedClassName() {
		return returnedClassName;
	}

	public void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;
	}
	
	public Property copy() {
		final Property prop = new Property();
		prop.setName( getName() );
		prop.setValue( getValue() );
		prop.setCascade( getCascade() );
		prop.setUpdateable( isUpdateable() );
		prop.setInsertable( isInsertable() );
		prop.setSelectable( isSelectable() );
		prop.setOptimisticLocked( isOptimisticLocked() );
		prop.setValueGenerationStrategy( getValueGenerationStrategy() );
		prop.setPropertyAccessorName( getPropertyAccessorName() );
		prop.setPropertyAccessStrategy( getPropertyAccessStrategy() );
		prop.setLazy( isLazy() );
		prop.setLazyGroup( getLazyGroup() );
		prop.setOptional( isOptional() );
		prop.setMetaAttributes( getMetaAttributes() );
		prop.setPersistentClass( getPersistentClass() );
		prop.setNaturalIdentifier( isNaturalIdentifier() );
		prop.setLob( isLob() );
		prop.addCallbackDefinitions( getCallbackDefinitions() );
		prop.setReturnedClassName( getReturnedClassName() );
		return prop;
	}
}
