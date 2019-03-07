/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.apache.edgent.function;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Common functions and functional utilities.
 *
 */
public class Functions {
    
    /**
     * Single instance of the identity function.
     */
    private static final UnaryOperator<Object> IDENTITY = t -> t;
    
    /**
     * Single instance of constant function that returns zero.
     */
    private static final Function<Object,Integer> ZERO = t -> 0;

    
    /**
     * Returns the identity function that returns its single argument.
     * @param <T> tuple type
     * @return Identity function that returns its single argument.
     */
    @SuppressWarnings("unchecked")
    public static <T> UnaryOperator<T> identity() {
        return (UnaryOperator<T>) IDENTITY;
    }
    
    /**
     * Returns a constant function that returns zero (0).
     * @param <T> tuple type
     * @return Constant function that returns zero (0).
     */
    @SuppressWarnings("unchecked")
    public static <T> Function<T,Integer> zero() {
        return (Function<T, Integer>) ZERO;
    }
    
    /**
     * Returns a constant function that returns zero (0).
     * This is identical to {@link #zero()} but is more
     * readable when applied as a key function.
     * @param <T> tuple type
     * @return Constant function that returns zero (0).
     */
    public static <T> Function<T,Integer> unpartitioned() {
        return zero();
    }
    
    /**
     * Close the function.
     * If {@code function} is an instance of {@code AutoCloseable}
     * then close is called, otherwise no action is taken.
     * @param function Function to be closed.
     * @throws Exception Error throwing function.
     */
    public static void closeFunction(Object function) throws Exception {
        
        AutoCloseable closeable = WrappedFunction.unwrap(AutoCloseable.class, function);
        if (closeable != null)
            closeable.close();
    }
    
    /**
     * Return a thread-safe version of a {@code Function} function.
     * If the function is guaranteed to be immutable (stateless)
     * then the function is returned, as it is thread safe,
     * otherwise a wrapper is returned that grabs synchronization
     * on {@code function} when calling {@link Function#apply(Object)}.
     * <BR>
     * If {@code function} implements {@code AutoCloseable} then
     * the function is assumed to be stateful and a thread-safe
     * version is returned.
     * @param <T> source tuple type
     * @param <R> result tuple type
     * @param function Function to return a thread-safe version of.
     * @return A thread-safe function
     */
    public static <T,R> Function<T,R> synchronizedFunction(final Function<T,R> function) {
        if (isImmutable(function) && !(function instanceof AutoCloseable))
            return function;
        
        // Return a function that is synchronized on the passed in function reference.
        return new ThreadSafeFunction<T,R>(function);
    }
    
    private static class ThreadSafeFunction<T,R> extends WrappedFunction<Function<T,R>> implements Function<T,R> {
        private static final long serialVersionUID = 1L;

        ThreadSafeFunction(Function<T,R> function) {
            super(function);
        }

        @Override
        public R apply(T value) {
            final Function<T,R> function = f();
            synchronized (function) {
                return function.apply(value);
            }
        }
    }
    
    /**
     * Return a thread-safe version of a {@code Supplier} function.
     * If the function is guaranteed to be immutable (stateless)
     * then the function is returned, as it is thread safe,
     * otherwise a wrapper is returned that grabs synchronization
     * on {@code function} when calling {@link Supplier#get()}.
     * <BR>
     * If {@code function} implements {@code AutoCloseable} then
     * the function is assumed to be stateful and a thread-safe
     * version is returned.
     * @param <T> tuple type
     * @param function Function to return a thread-safe version of.
     * @return A thread-safe function
     */
    public static <T> Supplier<T> synchronizedSupplier(final Supplier<T> function) {
        if (isImmutable(function) && !(function instanceof AutoCloseable))
            return function;
        
        // Return a function that is synchronized on the passed in function reference.
        return new ThreadSafeSupplier<T>(function);
    }

    private static class ThreadSafeSupplier<T> extends WrappedFunction<Supplier<T>> implements Supplier<T> {
        private static final long serialVersionUID = 1L;

        ThreadSafeSupplier(Supplier<T> function) {
            super(function);
        }

        @Override
        public T get() {
            final Supplier<T> function = f();
            synchronized (function) {
                return function.get();
            }
        }
    }
    
    /**
     * Return a thread-safe version of a {@code Consumer} function.
     * If the function is guaranteed to be immutable (stateless)
     * then the function is returned, as it is thread safe,
     * otherwise a wrapper is returned that grabs synchronization
     * on {@code function} when calling {@link Consumer#accept(Object)}.
     * <BR>
     * If {@code function} implements {@code AutoCloseable} then
     * the function is assumed to be stateful and a thread-safe
     * version is returned.
     * @param <T> tuple type
     * @param function Function to return a thread-safe version of.
     * @return A thread-safe function
     */
    public static <T> Consumer<T> synchronizedConsumer(final Consumer<T> function) {
        if (isImmutable(function) && !(function instanceof AutoCloseable))
            return function;
        
        // Return a function that is synchronized on the passed in function reference.
        return new ThreadSafeConsumer<T>(function);
    }

    private static class ThreadSafeConsumer<T>
              extends WrappedFunction<Consumer<T>> implements Consumer<T> {
        private static final long serialVersionUID = 1L;

        ThreadSafeConsumer(Consumer<T> function) {
            super(function);
        }

        @Override
        public void accept(T value) {
            final Consumer<T> function = f();
            synchronized (function) {
                function.accept(value);
            }
        }       
    }
    
    /**
     * Return a thread-safe version of a {@code BiFunction} function.
     * If the function is guaranteed to be immutable (stateless)
     * then the function is returned, as it is thread safe,
     * otherwise a wrapper is returned that grabs synchronization
     * on {@code function} when calling {@link BiFunction#apply(Object, Object)}.
     * <BR>
     * If {@code function} implements {@code AutoCloseable} then
     * the function is assumed to be stateful and a thread-safe
     * version is returned.
     * @param <T> Type of function's first argument
     * @param <U> Type of function's second argument
     * @param <R> Type of function's third argument
     * @param function Function to return a thread-safe version of.
     * @return A thread-safe function
     */
    public static <T,U,R> BiFunction<T,U,R> synchronizedBiFunction(final BiFunction<T,U,R> function) {
        if (isImmutable(function) && !(function instanceof AutoCloseable))
            return function;
        
        // Return a function that is synchronized on the passed in function reference.
        return new ThreadSafeBiFunction<T,U,R>(function);
    }

    private static class ThreadSafeBiFunction<T,U,R>
              extends WrappedFunction<BiFunction<T,U,R>> implements BiFunction<T,U,R> {
        private static final long serialVersionUID = 1L;

        ThreadSafeBiFunction(BiFunction<T,U,R> function) {
            super(function);
        }

        @Override
        public R apply(T t, U u) {
            final BiFunction<T,U,R> function = f();
            synchronized (function) {
                return function.apply(t, u);
            }
        }       
    }
    
    /**
     * See if the functional logic is immutable.
     * 
     * Logic is stateful if:
     *   Has a non-final instance field.
     *   Has a final instance field that is not a primitive
     *   or a known immutable object.
     *   
     * @param function Function to check
     * @return True if the function is immutable..
     */
    public static boolean isImmutable(Object function) {
        return isImmutableClass(function.getClass());
    }
    
    /**
     * See if a function class is immutable.
     * 
     * Logic is stateful if:
     *   Has a non-final instance field.
     *   Has a final instance field that is not a primitive
     *   or a known immutable object.
     *   
     * @param clazz Class to check
     * @return True if the function is immutable..
     */
    public static boolean isImmutableClass(Class<?> clazz) {
               
        do {
               Field[] fields = clazz.getDeclaredFields();
               for (Field field : fields) {
                   if (Modifier.isStatic(field.getModifiers()))
                       continue;
                   
                   if (Modifier.isTransient(field.getModifiers()))
                       continue;
                   
                   if (!Modifier.isFinal(field.getModifiers()))
                       return false;
                   
                   if (field.getType().isPrimitive())
                       continue; 
                   
                   if (immutableClasses.contains(field.getType()))
                       continue;
                   
                   if (field.getType().isEnum()) {
                       if (isImmutable(field.getType()))
                           continue;
                   }
                   
                   return false;
               }
               
               clazz = clazz.getSuperclass();
               
        } while (!Object.class.equals(clazz));
        
        return true;
    }
    
    private static final Set<Class<?>> immutableClasses = new HashSet<>();
    static {
        immutableClasses.add(String.class);
        
        immutableClasses.add(Boolean.class);
        
        immutableClasses.add(Byte.class);
        immutableClasses.add(Short.class);
        immutableClasses.add(Integer.class);
        immutableClasses.add(Long.class);
        
        immutableClasses.add(BigInteger.class);
        immutableClasses.add(BigDecimal.class);
        
        immutableClasses.add(Float.class);
        immutableClasses.add(Double.class);
        
        immutableClasses.add(File.class);
        
        immutableClasses.add(Character.class);
        
        immutableClasses.add(Locale.class);
        immutableClasses.add(UUID.class);
        
    }
    
    /**
     * Create a {@code Runnable} that calls
     * {@code consumer.accept(value)} when {@code run()} is called.
     * This can be used to delay the execution of the consumer
     * until some time in the future using an executor service.
     * 
     * @param <T> tuple type
     * @param consumer Function to be applied to {@code value}.
     * @param value Value to be consumed.
     * 
     * @return {@code Runnable} that invokes {@code consumer.accept(value)}.
     */
    public static <T> Runnable delayedConsume(Consumer<T> consumer, T value) {
        return () -> consumer.accept(value);
    }
    
    /**
     * Wrap a {@code Runnable} with a final action that
     * is always called when {@code action.run()} completes.
     * @param action Action to be invoked before {@code finalAction}.
     * @param finalAction Action to be invoked after {@code action.run()} is called.
     * @return {@code Runnable} that invokes {@code action.run()} and then {@code finalAction.run()}
     */
    public static Runnable runWithFinal(Runnable action, Runnable finalAction) {
        return () -> { try {action.run(); } finally {finalAction.run();}};
    }

    /**
     * A Consumer that discards all items passed to it.
     */
    private static final Consumer<Object> DISCARDER = t -> {};


    /**
     * A Consumer that discards all items passed to it.
     * @param <T> tuple type
     * @return A Consumer that discards all items passed to it.
     */
    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> discard() {
        return (Consumer<T>) DISCARDER;
    }
    
    /**
     * A Predicate that is always true.
     */
    private static final Predicate<Object> TRUE = t -> true;
    
    /**
     * A Predicate that is always false.
     */
    private static final Predicate<Object> FALSE = t -> false;
    
    /**
     * A Predicate that is always true
     * @param <T> tuple type
     * @return A Predicate that is always true.
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysTrue() {
        return (Predicate<T>) TRUE;
    }
    
    /**
     * A Predicate that is always false
     * @param <T> tuple type
     * @return A Predicate that is always false.
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysFalse() {
        return (Predicate<T>) FALSE;
    }
}
