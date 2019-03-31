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
package org.apache.edgent.connectors.jdbc;

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.edgent.connectors.jdbc.runtime.JdbcConnector;
import org.apache.edgent.connectors.jdbc.runtime.JdbcStatement;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

/**
 * {@code JdbcStreams} is a streams connector to a database via the
 * JDBC API {@code java.sql} package.
 * <p>
 * The connector provides general SQL access to a database, enabling
 * writing of a stream's tuples to a database, creating a stream from
 * database query results, and other operations.  
 * Knowledge of the JDBC API is required.
 * <p>
 * Use of the connector involves:
 * <ul>
 * <li>constructing a streams connector to a database by providing it with:
 *     <ul>
 *     <li>a JDBC {@link javax.sql.DataSource}</li>
 *     <li>a function that creates a JDBC {@link java.sql.Connection} 
 *         from the {@code DataSource}</li>
 *     </ul>
 *     </li>
 * <li>defining SQL statement executions and results handling by calling one
 *     of the {@code executeStatement()} methods:
 *     <ul>
 *     <li>specify an SQL statement String or define a {@link StatementSupplier}.
 *         A {@code StatementSupplier} 
 *         creates a JDBC {@link java.sql.PreparedStatement} for an SQL statement
 *         (e.g., a query, insert, update, etc operation).</li>
 *     <li>define a {@link ParameterSetter}.  A {@code ParameterSetter}
 *         sets the parameter values in a generic {@code PreparedStatement}.</li>
 *     <li>define a {@link ResultsHandler} as required.
 *         A {@code ResultsHandler} processes a JDBC
 *         {@link java.sql.ResultSet} created by executing a SQL statement,
 *         optionally creating one or more tuples from the results
 *         and adding them to a stream.</li>
 *     </ul>
 *     </li>
 * </ul>
 * <p>
 * Sample use:
 * <pre>{@code
 *  // construct a connector to the database
 *  JdbcStreams mydb = new JdbcStreams(
 *                // fn to create the javax.sql.DataSource to the db
 *                () -> {
 *                       Context ctx = new javax.naming.InitialContext();
 *                       return (DataSource) ctx.lookup("jdbc/myDb");
 *                      },
 *                // fn to connect to the db (via the DataSource)
 *                (dataSource,cn) ->  dataSource.getConnection(username,pw)
 *              );
 *
 *  // ----------------------------------------------------
 *  //
 *  // Write a Person stream to a table
 *  //                       
 *  TStream<Person> persons = ...
 *  TSink sink = mydb.executeStatement(persons,
 *           () -> "INSERT INTO persons VALUES(?,?,?)",
 *           (person,stmt) -> {
 *               stmt.setInt(1, person.getId());
 *               stmt.setString(2, person.getFirstName());
 *               stmt.setString(3, person.getLastName());
 *               }, 
 *           );
 *           
 *  // ----------------------------------------------------
 *  //
 *  // Create a stream of Person from a PersonId tuple
 *  //
 *  TStream<PersonId> personIds = ...
 *  TStream<Person> persons = mydb.executeStatement(personIds,
 *            () -> "SELECT id, firstname, lastname FROM persons WHERE id = ?",
 *            (personId,stmt) -> stmt.setInt(1,personId.getId()),
 *            (personId,rs,exc,consumer) -> {
 *                    if (exc != null) {
 *                        // statement failed, do something
 *                        int ecode = exc.getErrorCode();
 *                        String state = exc.getSQLState();
 *                        ...  // consumer.accept(...) if desired.
 *                    }
 *                    else {
 *                        rs.next();
 *                        int id = resultSet.getInt("id");
 *                        String firstName = resultSet.getString("firstname");
 *                        String lastName = resultSet.getString("lastname");
 *                        consumer.accept(new Person(id, firstName, lastName));
 *                    }
 *                }
 *            ); 
 *  persons.print();
 *    
 *  // ----------------------------------------------------
 *  //
 *  // Delete all the rows from a table
 *  //
 *  TStream<String> beacon = org.apache.edgent.org.apache.edgent.topology.strings("once");
 *  mydb.executeStatement(beacon,
 *               () -> "DELETE FROM persons",
 *               (tuple,stmt) -> { }  // no params to set
 *              ); 
 * }</pre>
 */
public class JdbcStreams {
    @SuppressWarnings("unused")
    private final Topology top;
    private final JdbcConnector connector;
    
    /**
     * Create a connector that uses a JDBC {@link DataSource} object to get
     * a database connection.
     * <p>
     * In some environments it's common for JDBC DataSource objects to
     * have been registered in JNDI. In such cases the dataSourceFn can be:
     * <pre>{@code
     * () -> {  Context ctx = new javax.naming.InitialContext();
     *          return (DataSource) ctx.lookup("jdbc/" + logicalDbName);
     *       }
     * }</pre>
     * <p>
     * Alternatively, a DataSource can be created using a dbms implementation's
     * DataSource class.
     * For example:
     * <pre>{@code
     * () -> { EmbeddedDataSource ds = new org.apache.derby.jdbc.EmbeddedDataSource();
     *         ds.setDatabaseName(dbName);
     *         ds.setCreateDatabase("create");
     *         return ds;
     *       }
     * }</pre>
     * <p>
     * Once {@code dataSourceFn} returns a DataSource it will not be called again. 
     * <p>
     * {@code connFn} is called only if a new JDBC connection is needed.
     * It is not called per-processed-tuple.  JDBC failures in
     * {@code executeStatement()} can result in a JDBC connection getting
     * closed and {@code connFn} is subsequently called to reconnect.
     * 
     * @param topology org.apache.edgent.org.apache.edgent.topology that this connector is for
     * @param dataSourceFn function that yields the {@link DataSource}
     *              for the database.
     * @param connFn function that yields a {@link Connection} from a {@code DataSource}.
     */
    public JdbcStreams(Topology topology, CheckedSupplier<DataSource> dataSourceFn, CheckedFunction<DataSource,Connection> connFn) {
        this.top = topology;
        this.connector = new JdbcConnector(dataSourceFn, connFn);
    }

    /**
     * For each tuple on {@code stream} execute an SQL statement and
     * add 0 or more resulting tuples to a result stream.
     * <p>
     * Same as using {@link #executeStatement(TStream, StatementSupplier, ParameterSetter, ResultsHandler)}
     * specifying {@code dataSource -> dataSource.prepareStatement(stmtSupplier.get()}}
     * for the {@code StatementSupplier}.
     * 
     * @param <T> Tuple type for input stream
     * @param <R> Tuple type of result stream
     * @param stream tuples to execute a SQL statement on behalf of
     * @param stmtSupplier an SQL statement
     * @param paramSetter function to set SQL statement parameters
     * @param resultsHandler SQL ResultSet handler
     * @return result Stream
     */
    public <T,R> TStream<R> executeStatement(TStream<T> stream,
            Supplier<String> stmtSupplier,
            ParameterSetter<T> paramSetter,
            ResultsHandler<T,R> resultsHandler
            ) {
        return stream.flatMap(new JdbcStatement<T,R>(connector,
                cn -> cn.prepareStatement(stmtSupplier.get()),
                paramSetter, resultsHandler));
    }
    
    /**
     * For each tuple on {@code stream} execute an SQL statement and
     * add 0 or more resulting tuples to a result stream.
     * <p>
     * Use to transform T tuples to R tuples, or
     * enrich/update T tuples with additional information from a database.
     * It can also be used to load a table into stream, 
     * using a T to trigger that.
     * Or to execute non-ResultSet generating
     * SQL statements and receive failure info and/or generate tuple(s)
     * upon completion.
     * <p>
     * {@code stmtSupplier} is called only once per new JDBC connection/reconnect.
     * It is not called per-tuple.  Hence, with the exception of statement
     * parameters, the returned statement is expected to be unchanging.
     * Failures executing a statement can result in the connection getting
     * closed and subsequently reconnected, resulting in another
     * {@code stmtSupplier} call.
     * <p>
     * {@code resultsHandler} is called for every tuple.
     * If {@code resultsHandler} throws an Exception, it is called a
     * second time for the tuple with a non-null exception argument.
     * 
     * @param <T> Tuple type for input stream
     * @param <R> Tuple type of result stream
     * @param stream tuples to execute a SQL statement on behalf of
     * @param stmtSupplier an SQL statement
     * @param paramSetter function to set SQL statement parameters
     * @param resultsHandler SQL ResultSet handler
     * @return result Stream
     * @see #executeStatement(TStream, Supplier, ParameterSetter, ResultsHandler)
     */
    public <T,R> TStream<R> executeStatement(TStream<T> stream,
            StatementSupplier stmtSupplier,
            ParameterSetter<T> paramSetter,
            ResultsHandler<T,R> resultsHandler
            ) {
        return stream.flatMap(new JdbcStatement<T,R>(connector,
                stmtSupplier, paramSetter, resultsHandler));
    }
    
    /**
     * For each tuple on {@code stream} execute an SQL statement.
     * <p>
     * Same as using {@link #executeStatement(TStream, StatementSupplier, ParameterSetter)}
     * specifying {@code dataSource -> dataSource.prepareStatement(stmtSupplier.get()}}
     * for the {@code StatementSupplier}.
     *
     * @param <T> Tuple type
     * @param stream tuples to execute a SQL statement on behalf of
     * @param stmtSupplier an SQL statement
     * @param paramSetter function to set SQL statement parameters
     * @return TSink sink element representing termination of this stream.
     */
    public <T> TSink<T> executeStatement(TStream<T> stream,
            Supplier<String> stmtSupplier,
            ParameterSetter<T> paramSetter
            ) {
        return stream.sink(new JdbcStatement<T,Object>(connector,
                cn -> cn.prepareStatement(stmtSupplier.get()),
                paramSetter));
    }

    /**
     * For each tuple on {@code stream} execute an SQL statement.
     * <p>
     * Use to write a stream of T to a table.
     * More generally, use a T as a trigger to execute some SQL statement
     * that doesn't yield a ResultSet.
     * <p>
     * Use a non-sink form of {@code executeStatement()} (forms
     * that take a {@code ResultsHandler}), if you want to:
     * <ul>
     * <li>be notified of statement execution failures</li>
     * <li>generate tuple(s) after the statement has run.</li>
     * </ul>
     *
     * @param <T> Tuple type
     * @param stream tuples to execute a SQL statement on behalf of
     * @param stmtSupplier an SQL statement
     * @param paramSetter function to set SQL statement parameters
     * @return TSink sink element representing termination of this stream.
     * @see #executeStatement(TStream, Supplier, ParameterSetter)
     */
    public <T> TSink<T> executeStatement(TStream<T> stream,
            StatementSupplier stmtSupplier,
            ParameterSetter<T> paramSetter
            ) {
        return stream.sink(new JdbcStatement<T,Object>(connector,
                stmtSupplier, paramSetter));
    }
}
