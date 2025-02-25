/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.NullPrecedence;
import org.hibernate.query.sqm.SortOrder;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Hibernate extensions to the JPA CriteriaBuilder.
 *
 * @author Steve Ebersole
 */

public interface HibernateCriteriaBuilder extends CriteriaBuilder {

	<X, T> JpaExpression<X> cast(JpaExpression<T> expression, Class<X> castTargetJavaType);

	JpaPredicate wrap(Expression<Boolean> expression);

	@SuppressWarnings("unchecked")
	JpaPredicate wrap(Expression<Boolean>... expressions);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Criteria creation

	@Override
	JpaCriteriaQuery<Object> createQuery();

	@Override
	<T> JpaCriteriaQuery<T> createQuery(Class<T> resultClass);

	@Override
	JpaCriteriaQuery<Tuple> createTupleQuery();

	@Override
	<T> JpaCriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity);

	@Override
	<T> JpaCriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity);

	<T> JpaCriteriaInsertSelect<T> createCriteriaInsertSelect(Class<T> targetEntity);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Set operation

	default <T> JpaCriteriaQuery<T> unionAll(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return union( true, query1, queries );
	}

	default <T> JpaCriteriaQuery<T> union(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return union( false, query1, queries );
	}

	<T> JpaCriteriaQuery<T> union(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries);

	default <T> JpaCriteriaQuery<T> intersectAll(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return intersect( true, query1, queries );
	}

	default <T> JpaCriteriaQuery<T> intersect(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return intersect( false, query1, queries );
	}

	<T> JpaCriteriaQuery<T> intersect(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries);

	default <T> JpaCriteriaQuery<T> exceptAll(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return except( true, query1, queries );
	}

	default <T> JpaCriteriaQuery<T> except(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return except( false, query1, queries );
	}

	<T> JpaCriteriaQuery<T> except(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries);

	default <T> JpaSubQuery<T> unionAll(Subquery<? extends T> query1, Subquery<?>... queries) {
		return union( true, query1, queries );
	}

	default <T> JpaSubQuery<T> union(Subquery<? extends T> query1, Subquery<?>... queries) {
		return union( false, query1, queries );
	}

	<T> JpaSubQuery<T> union(boolean all, Subquery<? extends T> query1, Subquery<?>... queries);

	default <T> JpaSubQuery<T> intersectAll(Subquery<? extends T> query1, Subquery<?>... queries) {
		return intersect( true, query1, queries );
	}

	default <T> JpaSubQuery<T> intersect(Subquery<? extends T> query1, Subquery<?>... queries) {
		return intersect( false, query1, queries );
	}

	<T> JpaSubQuery<T> intersect(boolean all, Subquery<? extends T> query1, Subquery<?>... queries);

	default <T> JpaSubQuery<T> exceptAll(Subquery<? extends T> query1, Subquery<?>... queries) {
		return except( true, query1, queries );
	}

	default <T> JpaSubQuery<T> except(Subquery<? extends T> query1, Subquery<?>... queries) {
		return except( false, query1, queries );
	}

	<T> JpaSubQuery<T> except(boolean all, Subquery<? extends T> query1, Subquery<?>... queries);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA 3.1

	/**
	 * Create an expression that returns the sign of its
	 * argument, that is, {@code 1} if its argument is
	 * positive, {@code -1} if its argument is negative,
	 * or {@code 0} if its argument is exactly zero.
	 * @param x expression
	 * @return sign
	 */
	JpaExpression<Integer> sign(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the ceiling of its
	 * argument, that is, the smallest integer greater than
	 * or equal to its argument.
	 * @param x expression
	 * @return ceiling
	 */
	<N extends Number> JpaExpression<N> ceiling(Expression<N> x);

	/**
	 * Create an expression that returns the floor of its
	 * argument, that is, the largest integer smaller than
	 * or equal to its argument.
	 * @param x expression
	 * @return floor
	 */
	<N extends Number> JpaExpression<N> floor(Expression<N> x);

	/**
	 * Create an expression that returns the exponential
	 * of its argument, that is, Euler's number <i>e</i>
	 * raised to the power of its argument.
	 * @param x expression
	 * @return exponential
	 */
	JpaExpression<Double> exp(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the natural logarithm
	 * of its argument.
	 * @param x expression
	 * @return natural logarithm
	 */
	JpaExpression<Double> ln(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the first argument
	 * raised to the power of its second argument.
	 * @param x base
	 * @param y exponent
	 * @return the base raised to the power of the exponent
	 */
	JpaExpression<Double> power(Expression<? extends Number> x, Expression<? extends Number> y);

	/**
	 * Create an expression that returns the first argument
	 * raised to the power of its second argument.
	 * @param x base
	 * @param y exponent
	 * @return the base raised to the power of the exponent
	 */
	JpaExpression<Double> power(Expression<? extends Number> x, Number y);

	/**
	 * Create an expression that returns the first argument
	 * rounded to the number of decimal places given by the
	 * second argument.
	 * @param x base
	 * @param n number of decimal places
	 * @return the rounded value
	 */
	<T extends Number> JpaExpression<T> round(Expression<T> x, Integer n);

	/**
	 *  Create expression to return current local date.
	 *  @return expression for current date
	 */
	JpaExpression<java.time.LocalDate> localDate();

	/**
	 *  Create expression to return current local datetime.
	 *  @return expression for current timestamp
	 */
	JpaExpression<java.time.LocalDateTime> localDateTime();

	/**
	 *  Create expression to return current local time.
	 *  @return expression for current time
	 */
	JpaExpression<java.time.LocalTime> localTime();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Paths

	<P, F> JpaExpression<F> fk(Path<P> path);

	@Override
	<X, T extends X> JpaPath<T> treat(Path<X> path, Class<T> type);

	@Override
	<X, T extends X> JpaRoot<T> treat(Root<X> root, Class<T> type);

	@Override
	<X, T, V extends T> JpaJoin<X, V> treat(Join<X, T> join, Class<V> type);

	@Override
	<X, T, E extends T> JpaCollectionJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type);

	@Override
	<X, T, E extends T> JpaSetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type);

	@Override
	<X, T, E extends T> JpaListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type);

	@Override
	<X, K, T, V extends T> JpaMapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selections

	@Override
	<Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, Selection<?>[] selections);
	<Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, List<? extends JpaSelection<?>> arguments);

	@Override
	JpaCompoundSelection<Tuple> tuple(Selection<?>[] selections);
	JpaCompoundSelection<Tuple> tuple(List<? extends JpaSelection<?>> selections);

	@Override
	JpaCompoundSelection<Object[]> array(Selection<?>[] selections);
	JpaCompoundSelection<Object[]> array(List<? extends JpaSelection<?>> selections);

	<Y> JpaCompoundSelection<Y> array(Class<Y> resultClass, Selection<?>[] selections);
	<Y> JpaCompoundSelection<Y> array(Class<Y> resultClass, List<? extends JpaSelection<?>> selections);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	@Override
	<N extends Number> JpaExpression<Double> avg(Expression<N> argument);

	@Override
	<N extends Number> JpaExpression<N> sum(Expression<N> argument);

	@Override
	JpaExpression<Long> sumAsLong(Expression<Integer> argument);

	@Override
	JpaExpression<Double> sumAsDouble(Expression<Float> argument);

	@Override
	<N extends Number> JpaExpression<N> max(Expression<N> argument);

	@Override
	<N extends Number> JpaExpression<N> min(Expression<N> argument);

	@Override
	<X extends Comparable<? super X>> JpaExpression<X> greatest(Expression<X> argument);

	@Override
	<X extends Comparable<? super X>> JpaExpression<X> least(Expression<X> argument);

	@Override
	JpaExpression<Long> count(Expression<?> argument);

	@Override
	JpaExpression<Long> countDistinct(Expression<?> x);

	@Override
	<N extends Number> JpaExpression<N> neg(Expression<N> x);

	@Override
	<N extends Number> JpaExpression<N> abs(Expression<N> x);


	@Override
	<N extends Number> JpaExpression<N> sum(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpression<N> sum(Expression<? extends N> x, N y);

	@Override
	<N extends Number> JpaExpression<N> sum(N x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpression<N> prod(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpression<N> prod(Expression<? extends N> x, N y);

	@Override
	<N extends Number> JpaExpression<N> prod(N x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpression<N> diff(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpression<N> diff(Expression<? extends N> x, N y);

	@Override
	<N extends Number> JpaExpression<N> diff(N x, Expression<? extends N> y);

	@Override
	JpaExpression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaExpression<Number> quot(Expression<? extends Number> x, Number y);

	@Override
	JpaExpression<Number> quot(Number x, Expression<? extends Number> y);

	@Override
	JpaExpression<Integer> mod(Expression<Integer> x, Expression<Integer> y);

	@Override
	JpaExpression<Integer> mod(Expression<Integer> x, Integer y);

	@Override
	JpaExpression<Integer> mod(Integer x, Expression<Integer> y);

	@Override
	JpaExpression<Double> sqrt(Expression<? extends Number> x);

	@Override
	JpaExpression<Long> toLong(Expression<? extends Number> number);

	@Override
	JpaExpression<Integer> toInteger(Expression<? extends Number> number);

	@Override
	JpaExpression<Float> toFloat(Expression<? extends Number> number);

	@Override
	JpaExpression<Double> toDouble(Expression<? extends Number> number);

	@Override
	JpaExpression<BigDecimal> toBigDecimal(Expression<? extends Number> number);

	@Override
	JpaExpression<BigInteger> toBigInteger(Expression<? extends Number> number);

	@Override
	JpaExpression<String> toString(Expression<Character> character);

	@Override
	<T> JpaExpression<T> literal(T value);
	<T> SqmExpression<T> literal(T value, SqmExpression<? extends T> typeInferenceSource);

	<T> List<? extends JpaExpression<T>> literals(T[] values);

	<T> List<? extends JpaExpression<T>> literals(List<T> values);

	@Override
	<T> JpaExpression<T> nullLiteral(Class<T> resultClass);

	@Override
	<T> JpaParameterExpression<T> parameter(Class<T> paramClass);

	@Override
	<T> JpaParameterExpression<T> parameter(Class<T> paramClass, String name);

	@Override
	JpaExpression<String> concat(Expression<String> x, Expression<String> y);

	@Override
	JpaExpression<String> concat(Expression<String> x, String y);

	@Override
	JpaExpression<String> concat(String x, Expression<String> y);

	JpaExpression<String> concat(String x, String y);

	@Override
	JpaFunction<String> substring(Expression<String> x, Expression<Integer> from);

	@Override
	JpaFunction<String> substring(Expression<String> x, int from);

	@Override
	JpaFunction<String> substring(
			Expression<String> x,
			Expression<Integer> from,
			Expression<Integer> len);

	@Override
	JpaFunction<String> substring(Expression<String> x, int from, int len);

	@Override
	JpaFunction<String> trim(Expression<String> x);

	@Override
	JpaFunction<String> trim(Trimspec ts, Expression<String> x);

	@Override
	JpaFunction<String> trim(Expression<Character> t, Expression<String> x);

	@Override
	JpaFunction<String> trim(Trimspec ts, Expression<Character> t, Expression<String> x);

	@Override
	JpaFunction<String> trim(char t, Expression<String> x);

	@Override
	JpaFunction<String> trim(Trimspec ts, char t, Expression<String> x);

	@Override
	JpaFunction<String> lower(Expression<String> x);

	@Override
	JpaFunction<String> upper(Expression<String> x);

	@Override
	JpaFunction<Integer> length(Expression<String> x);

	@Override
	JpaFunction<Integer> locate(Expression<String> x, Expression<String> pattern);

	@Override
	JpaFunction<Integer> locate(Expression<String> x, String pattern);

	@Override
	JpaFunction<Integer> locate(
			Expression<String> x,
			Expression<String> pattern,
			Expression<Integer> from);

	@Override
	JpaFunction<Integer> locate(Expression<String> x, String pattern, int from);

	@Override
	JpaFunction<Date> currentDate();

	@Override
	JpaFunction<Time> currentTime();

	@Override
	JpaFunction<Timestamp> currentTimestamp();

	JpaFunction<Instant> currentInstant();

	@Override
	<T> JpaFunction<T> function(String name, Class<T> type, Expression<?>[] args);

	@Override
	<Y> JpaExpression<Y> all(Subquery<Y> subquery);

	@Override
	<Y> JpaExpression<Y> some(Subquery<Y> subquery);

	@Override
	<Y> JpaExpression<Y> any(Subquery<Y> subquery);

	@Override
	<K, M extends Map<K, ?>> JpaExpression<Set<K>> keys(M map);

	<K, L extends List<?>> JpaExpression<Set<K>> indexes(L list);

	<T> SqmExpression<T> value(T value);

	<T> SqmExpression<T> value(T value, SqmExpression<? extends T> typeInferenceSource);

	<V, C extends Collection<V>> JpaExpression<Collection<V>> values(C collection);

	@Override
	<V, M extends Map<?, V>> Expression<Collection<V>> values(M map);

	@Override
	<C extends Collection<?>> JpaExpression<Integer> size(Expression<C> collection);

	@Override
	<C extends Collection<?>> JpaExpression<Integer> size(C collection);

	@Override
	<T> JpaCoalesce<T> coalesce();

	@Override
	<Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Y y);

	@Override
	<Y> JpaExpression<Y> nullif(Expression<Y> x, Expression<?> y);

	@Override
	<Y> JpaExpression<Y> nullif(Expression<Y> x, Y y);

	@Override
	<C, R> JpaSimpleCase<C, R> selectCase(Expression<? extends C> expression);

	@Override
	<R> JpaSearchedCase<R> selectCase();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates


	@Override
	JpaPredicate and(Expression<Boolean> x, Expression<Boolean> y);

	@Override
	JpaPredicate and(Predicate... restrictions);

	@Override
	JpaPredicate or(Expression<Boolean> x, Expression<Boolean> y);

	@Override
	JpaPredicate or(Predicate... restrictions);

	@Override
	JpaPredicate not(Expression<Boolean> restriction);

	@Override
	JpaPredicate conjunction();

	@Override
	JpaPredicate disjunction();

	@Override
	JpaPredicate isTrue(Expression<Boolean> x);

	@Override
	JpaPredicate isFalse(Expression<Boolean> x);

	@Override
	JpaPredicate isNull(Expression<?> x);

	@Override
	JpaPredicate isNotNull(Expression<?> x);

	@Override
	JpaPredicate equal(Expression<?> x, Expression<?> y);

	@Override
	JpaPredicate equal(Expression<?> x, Object y);

	@Override
	JpaPredicate notEqual(Expression<?> x, Expression<?> y);

	@Override
	JpaPredicate notEqual(Expression<?> x, Object y);

	JpaPredicate distinctFrom(Expression<?> x, Expression<?> y);

	JpaPredicate distinctFrom(Expression<?> x, Object y);

	JpaPredicate notDistinctFrom(Expression<?> x, Expression<?> y);

	JpaPredicate notDistinctFrom(Expression<?> x, Object y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThan(
			Expression<? extends Y> x,
			Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThan(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThan(
			Expression<? extends Y> x,
			Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThan(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate between(
			Expression<? extends Y> value,
			Expression<? extends Y> lower,
			Expression<? extends Y> upper);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate between(Expression<? extends Y> value, Y lower, Y upper);

	@Override
	JpaPredicate gt(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicate gt(Expression<? extends Number> x, Number y);

	@Override
	JpaPredicate ge(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicate ge(Expression<? extends Number> x, Number y);

	@Override
	JpaPredicate lt(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicate lt(Expression<? extends Number> x, Number y);

	@Override
	JpaPredicate le(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicate le(Expression<? extends Number> x, Number y);

	@Override
	<C extends Collection<?>> JpaPredicate isEmpty(Expression<C> collection);

	@Override
	<C extends Collection<?>> JpaPredicate isNotEmpty(Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicate isMember(Expression<E> elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicate isMember(E elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicate isNotMember(Expression<E> elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicate isNotMember(E elem, Expression<C> collection);

	@Override
	JpaPredicate like(Expression<String> x, Expression<String> pattern);

	@Override
	JpaPredicate like(Expression<String> x, String pattern);

	@Override
	JpaPredicate like(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicate like(Expression<String> x, Expression<String> pattern, char escapeChar);

	@Override
	JpaPredicate like(Expression<String> x, String pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicate like(Expression<String> x, String pattern, char escapeChar);

	JpaPredicate ilike(Expression<String> x, Expression<String> pattern);

	JpaPredicate ilike(Expression<String> x, String pattern);

	JpaPredicate ilike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	JpaPredicate ilike(Expression<String> x, Expression<String> pattern, char escapeChar);

	JpaPredicate ilike(Expression<String> x, String pattern, Expression<Character> escapeChar);

	JpaPredicate ilike(Expression<String> x, String pattern, char escapeChar);

	@Override
	JpaPredicate notLike(Expression<String> x, Expression<String> pattern);

	@Override
	JpaPredicate notLike(Expression<String> x, String pattern);

	@Override
	JpaPredicate notLike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicate notLike(Expression<String> x, Expression<String> pattern, char escapeChar);

	@Override
	JpaPredicate notLike(Expression<String> x, String pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicate notLike(Expression<String> x, String pattern, char escapeChar);

	JpaPredicate notIlike(Expression<String> x, Expression<String> pattern);

	JpaPredicate notIlike(Expression<String> x, String pattern);

	JpaPredicate notIlike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	JpaPredicate notIlike(Expression<String> x, Expression<String> pattern, char escapeChar);

	JpaPredicate notIlike(Expression<String> x, String pattern, Expression<Character> escapeChar);

	JpaPredicate notIlike(Expression<String> x, String pattern, char escapeChar);

	@Override
	<T> JpaInPredicate<T> in(Expression<? extends T> expression);

	@SuppressWarnings("unchecked")
	<T> JpaInPredicate<T> in(Expression<? extends T> expression, Expression<? extends T>... values);

	@SuppressWarnings("unchecked")
	<T> JpaInPredicate<T> in(Expression<? extends T> expression, T... values);

	<T> JpaInPredicate<T> in(Expression<? extends T> expression, Collection<T> values);

	@Override
	JpaPredicate exists(Subquery<?> subquery);

	/**
	 * Create a predicate that tests whether a Map is empty.
	 * <p/>
	 * NOTE : Due to type-erasure we cannot name this the same as
	 * {@link CriteriaBuilder#isEmpty}
	 *
	 *
	 * @param mapExpression The expression resolving to a Map which we
	 * want to check for emptiness
	 *
	 * @return is-empty predicate
	 */
	<M extends Map<?,?>> JpaPredicate isMapEmpty(JpaExpression<M> mapExpression);

	/**
	 * Create a predicate that tests whether a Map is
	 * not empty.
	 * <p/>
	 * NOTE : Due to type-erasure we cannot name this the same as
	 * {@link CriteriaBuilder#isNotEmpty}
	 *
	 * @param mapExpression The expression resolving to a Map which we
	 * want to check for non-emptiness
	 *
	 * @return is-not-empty predicate
	 */
	<M extends Map<?,?>> JpaPredicate isMapNotEmpty(JpaExpression<M> mapExpression);

	/**
	 * Create an expression that tests the size of a map.
	 * <p/>
	 * NOTE : Due to type-erasure we cannot name this the same as
	 * {@link CriteriaBuilder#size}
	 *
	 * @param mapExpression The expression resolving to a Map for which we
	 * want to know the size
	 *
	 * @return size expression
	 */
	<M extends Map<?,?>> JpaExpression<Integer> mapSize(JpaExpression<M> mapExpression);

	/**
	 * Create an expression that tests the size of a map.
	 *
	 * @param map The Map for which we want to know the size
	 *
	 * @return size expression
	 */
	<M extends Map<?, ?>> JpaExpression<Integer> mapSize(M map);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordering

	JpaOrder sort(JpaExpression<?> sortExpression, SortOrder sortOrder, NullPrecedence nullPrecedence);
	JpaOrder sort(JpaExpression<?> sortExpression, SortOrder sortOrder);
	JpaOrder sort(JpaExpression<?> sortExpression);

	@Override
	JpaOrder asc(Expression<?> x);

	@Override
	JpaOrder desc(Expression<?> x);

	/**
	 * Create an ordering by the ascending value of the expression.
	 * @param x  expression used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return ascending ordering corresponding to the expression
	 */
	JpaOrder asc(Expression<?> x, boolean nullsFirst);

	/**
	 * Create an ordering by the descending value of the expression.
	 * @param x  expression used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return descending ordering corresponding to the expression
	 */
	JpaOrder desc(Expression<?> x, boolean nullsFirst);

	/**
	 * Create a search ordering based on the sort order and null precedence of the value of the CTE attribute.
	 * @param cteAttribute CTE attribute used to define the ordering
	 * @param sortOrder The sort order
	 * @param nullPrecedence The null precedence
	 * @return ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder search(JpaCteCriteriaAttribute cteAttribute, SortOrder sortOrder, NullPrecedence nullPrecedence);

	/**
	 * Create a search ordering based on the sort order of the value of the CTE attribute.
	 * @param cteAttribute CTE attribute used to define the ordering
	 * @param sortOrder The sort order
	 * @return ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder search(JpaCteCriteriaAttribute cteAttribute, SortOrder sortOrder);

	/**
	 * Create a search ordering based on the ascending value of the CTE attribute.
	 * @param cteAttribute CTE attribute used to define the ordering
	 * @return ascending ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder search(JpaCteCriteriaAttribute cteAttribute);

	/**
	 * Create a search ordering by the ascending value of the CTE attribute.
	 * @param x  CTE attribute used to define the ordering
	 * @return ascending ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder asc(JpaCteCriteriaAttribute x);

	/**
	 * Create a search ordering by the descending value of the CTE attribute.
	 * @param x CTE attribute used to define the ordering
	 * @return descending ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder desc(JpaCteCriteriaAttribute x);

	/**
	 * Create a search ordering by the ascending value of the CTE attribute.
	 * @param x  CTE attribute used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return ascending ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder asc(JpaCteCriteriaAttribute x, boolean nullsFirst);

	/**
	 * Create a search ordering by the descending value of the CTE attribute.
	 * @param x CTE attribute used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return descending ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder desc(JpaCteCriteriaAttribute x, boolean nullsFirst);
}
