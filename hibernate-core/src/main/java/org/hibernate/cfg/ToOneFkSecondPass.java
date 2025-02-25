/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.TableBinder;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;

import static org.hibernate.cfg.BinderHelper.createSyntheticPropertyReference;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * Enable a proper set of the FK columns in respect with the id column order
 * Allow the correct implementation of the default EJB3 values which needs both
 * sides of the association to be resolved
 *
 * @author Emmanuel Bernard
 */
public class ToOneFkSecondPass extends FkSecondPass {
	private final PersistentClass persistentClass;
	private final MetadataBuildingContext buildingContext;
	private final boolean unique;
	private final String path;
	private final String entityClassName;

	public ToOneFkSecondPass(
			ToOne value,
			AnnotatedJoinColumns columns,
			boolean unique,
			PersistentClass persistentClass,
			String path,
			MetadataBuildingContext buildingContext) {
		super( value, columns );
		this.persistentClass = persistentClass;
		this.buildingContext = buildingContext;
		this.unique = unique;
		this.entityClassName = persistentClass.getClassName();
		this.path = entityClassName != null ? path.substring( entityClassName.length() + 1 ) : path;
	}

	@Override
	public String getReferencedEntityName() {
		return ( (ToOne) value ).getReferencedEntityName();
	}

	@Override
	public boolean isInPrimaryKey() {
		if ( entityClassName == null ) {
			return false;
		}
		final PersistentClass persistentClass = buildingContext.getMetadataCollector()
				.getEntityBinding( entityClassName );
		Property property = persistentClass.getIdentifierProperty();
		if ( path == null ) {
			return false;
		}
		else if ( property != null) {
			//try explicit identifier property
			return path.startsWith( property.getName() + "." );
		}
		//try the embedded property
		else {
			final KeyValue valueIdentifier = persistentClass.getIdentifier();
			if ( valueIdentifier instanceof Component ) {
				// Embedded property starts their path with 'id.'
				// See PropertyPreloadedData( ) use when idClass != null in AnnotationSourceProcessor
				String localPath = path;
				if ( path.startsWith( "id." ) ) {
					localPath = path.substring( 3 );
				}

				Component component = (Component) valueIdentifier;
				for ( Property idProperty : component.getProperties() ) {
					if ( localPath.equals( idProperty.getName() ) || localPath.startsWith( idProperty.getName() + "." ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void doSecondPass(java.util.Map<String, PersistentClass> persistentClasses) throws MappingException {
		if ( value instanceof ManyToOne ) {
			final ManyToOne manyToOne = (ManyToOne) value;
			final PersistentClass targetEntity = persistentClasses.get( manyToOne.getReferencedEntityName() );
			if ( targetEntity == null ) {
				throw new AnnotationException( "Association '" + qualify( entityClassName, path )
						+ "' targets an unknown entity named '" + manyToOne.getReferencedEntityName() + "'" );
			}
			manyToOne.setPropertyName( path );
			createSyntheticPropertyReference(
					columns,
					targetEntity,
					persistentClass,
					manyToOne,
					path,
					false,
					buildingContext
			);
			TableBinder.bindForeignKey( targetEntity, persistentClass, columns, manyToOne, unique, buildingContext );
			// HbmMetadataSourceProcessorImpl does this only when property-ref != null, but IMO, it makes sense event if it is null
			if ( !manyToOne.isIgnoreNotFound() ) {
				manyToOne.createPropertyRefConstraints( persistentClasses );
			}
		}
		else if ( value instanceof OneToOne ) {
			value.createForeignKey();
		}
		else {
			throw new AssertionFailure( "FkSecondPass for a wrong value type: " + value.getClass().getName() );
		}
	}
}
