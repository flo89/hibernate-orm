/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Enumerates the options for lazy loading of a
 * {@linkplain jakarta.persistence.ManyToOne many to one}
 * or {@linkplain jakarta.persistence.OneToOne one to one}
 * association.
 *
 * @author Emmanuel Bernard
 *
 * @see LazyToOne
 */
public enum LazyToOneOption {
	/**
	 * The association is always loaded eagerly. The identifier
	 * and concrete type of the associated entity instance,
	 * along with all the rest of its non-lazy fields, are always
	 * available immediately.
	 */
	FALSE,
	/**
	 * The association is proxied and a delegate entity instance
	 * is lazily fetched when any method of the proxy other than
	 * the getter method for the identifier property is first
	 * called.
	 * <ul>
	 * <li>The identifier property of the proxy object is set
	 *     when the proxy is instantiated.
	 *     The program may obtain the entity identifier value
	 *     of an unfetched proxy, without triggering lazy
	 *     fetching, by calling the corresponding getter method.
	 * <li>The proxy does not have the same concrete type as the
	 *     proxied delegate, and so
	 *     {@link org.hibernate.Hibernate#getClass(Object)}
	 *     must be used in place of {@link Object#getClass()}.
	 * <li>For a polymorphic association, the concrete type of
	 *     the proxied entity instance is not known until the
	 *     delegate is fetched from the database, and so
	 *     {@link org.hibernate.Hibernate#unproxy(Object, Class)}}
	 *     must be used to perform typecasts, and
	 *     {@link org.hibernate.Hibernate#getClass(Object)}
	 *     must be used instead of the Java {@code instanceof}
	 *     operator.
	 * </ul>
	 */
	PROXY,
	/**
	 * The associated entity instance is initially in an unloaded
	 * state, and is loaded lazily when any field other than the
	 * field containing the identifier is first accessed.
	 * <ul>
	 * <li>The identifier field of an unloaded entity instance is
	 *     set when the unloaded instance is instantiated.
	 *     The program may obtain the identifier of an unloaded
	 *     entity, without triggering lazy fetching, by accessing
	 *     the field containing the identifier.
	 * <li>Typecasts, the Java {@code instanceof} operator, and
	 *     {@link Object#getClass()} may be used as normal.
	 * <li>Bytecode enhancement is required. If the class is not
	 *     enhanced, this option is equivalent to {@link #PROXY}.
	 * </ul>
	 * <strong>Currently, Hibernate does not support this setting
	 * for polymorphic associations, and instead falls back to
	 * {@link #PROXY}!</strong>
	 */
	NO_PROXY
}
