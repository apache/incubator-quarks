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
package org.apache.edgent.test.connectors.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.edgent.connectors.jdbc.JdbcStreams;
import org.apache.edgent.test.connectors.common.ConnectorTestBase;
import org.apache.edgent.topology.TSink;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.plumbing.PlumbingStreams;
import org.junit.Test;

/**
 * JdbcStreams connector tests.
 * <p>
 * The tests use Apache Embedded Derby as the backing dbms.
 * The Oracle JDK includes Derby in $JAVA_HOME/db.
 * Manually install Derby for other JDKs if required.
 * Arrange for the classpath to be configured by one of:
 * <ul>
 * <li>manually add derby.jar to the classpath</li>
 * <li>set the DERBY_HOME environment variable.  connectors/jdbc/build.gradle adds 
 *     $DERBY_HOME/lib/derby.jar to the classpath when running the tests.
 *     e.g., try
 *     <ul>
 *       <li> export DERBY_HOME=$JAVA_HOME/db</li>
 *       <li> OSX: export DERBY_HOME=`/usr/libexec/java_home`/db
 *     </ul>
 *     </li>
 * </ul>
 * The tests are "skipped" if the dbms's jdbc driver can't be found.
 */
public class JdbcStreamsTest  extends ConnectorTestBase {
    
    private static final int SEC_TIMEOUT = 10;
    private final static String DB_NAME = System.getProperty("derby.home") + "/JdbcStreamsTestDb";
    // TODO: My username contained a "." and that broke the build, so I set this to a constant.
    private final static String USERNAME = "test";//System.getProperty("user.name");
    private final static String PW = "none";
    private static final List<Person> personList = new ArrayList<>();
    static {
        personList.add(new Person(1, "John", "Doe", "male", 35));
        personList.add(new Person(2, "Jane", "Doe", "female", 29));
        personList.add(new Person(3, "Billy", "McDoe", "male", 3));
    }
    private static final List<PersonId> personIdList = new ArrayList<>();
    static {
        for(Person p : personList) {
            personIdList.add(new PersonId(p.id));
        }
    }
    
    static class Person {
        int id;
        String firstName;
        String lastName;
        String gender;
        int age;
        Person(int id, String first, String last, String gender, int age) {
            this.id = id;
            this.firstName = first;
            this.lastName = last;
            this.gender = gender;
            this.age = age;
        }
        public String toString() {
            return String.format("id=%d first=%s last=%s gender=%s age=%d",
                    id, firstName, lastName, gender, age);
        }
    }
    
    static class PersonId {
        int id;
        PersonId(int id) {
            this.id = id;
        }
        public String toString() {
            return String.format("id=%d", id);
        }
    }

    public List<Person> getPersonList() {
        return personList;
    }

    public List<PersonId> getPersonIdList() {
        return personIdList;
    }

    DataSource getDerbyEmbeddedDataSource(String database) throws Exception
    {
        // Avoid a compile-time dependency to the jdbc driver.
        // At runtime, require that the classpath can find it.
        // e.g., build.xml adds $DERBY_HOME/lib/derby.jar to the test classpath

        String DERBY_DATA_SOURCE = "org.apache.derby.jdbc.EmbeddedDataSource";
    
        Class<?> nsDataSource = null;
        try {
            nsDataSource = Class.forName(DERBY_DATA_SOURCE);
        }
        catch (ClassNotFoundException e) {
            String msg = "Fix the test classpath. ";
            if (System.getenv("DERBY_HOME") == null) {
                msg += "DERBY_HOME not set. ";
            }
            msg += "Class not found: "+e.getLocalizedMessage();
            System.err.println(msg);
            assumeTrue(false);
        }
        DataSource ds = (DataSource) nsDataSource.newInstance();

        @SuppressWarnings("rawtypes")
        Class[] methodParams = new Class[] {String.class};
        Method dbname = nsDataSource.getMethod("setDatabaseName", methodParams);
        Object[] args = new Object[] {database};
        dbname.invoke(ds, args);

        // create the db if necessary
        Method create = nsDataSource.getMethod("setCreateDatabase", methodParams);
        args = new Object[] {"create"};
        create.invoke(ds, args);
    
        return ds;
    }
    
    private DataSource getDataSource(String logicalDbName) throws Exception {
        return getDerbyEmbeddedDataSource(logicalDbName);
    }
    
    private Connection connect(DataSource ds) throws Exception {
        return ds.getConnection(USERNAME, PW);
    }
    
    private void createPersonsTable() throws Exception {
        DataSource ds = getDataSource(DB_NAME);
        try(Connection cn = connect(ds)) {
            Statement stmt = cn.createStatement();
            try {
                stmt.execute("CREATE TABLE persons "
                        + "("
                        + "id INTEGER NOT NULL,"
                        + "firstname VARCHAR(40) NOT NULL,"
                        + "lastname VARCHAR(40) NOT NULL,"
                        + "gender VARCHAR(6),"
                        + "age INTEGER,"
                        + "PRIMARY KEY (id)"
                        + ")"
                        );
            }
            catch (SQLException e) {
                if (e.getLocalizedMessage().contains("already exists"))
                    return;
                else
                    throw e;
            }
        }
    }
    
    private void truncatePersonsTable() throws Exception {
        createPersonsTable();
        DataSource ds = getDataSource(DB_NAME);
        try(Connection cn = connect(ds)) {
            Statement stmt = cn.createStatement();
            stmt.executeUpdate("DELETE FROM persons");
        }
    }
    
    private void populatePersonsTable(List<Person> personList) throws Exception {
        truncatePersonsTable();
        DataSource ds = getDataSource(DB_NAME);
        try(Connection cn = connect(ds)) {
            Statement stmt = cn.createStatement();
            for(Person p : personList) {
                stmt.execute(String.format(
                        "INSERT INTO persons VALUES(%d,'%s','%s','%s',%d)",
                            p.id, p.firstName, p.lastName, p.gender, p.age));
            }
        }
    }

    private TStream<Person> readPersonsTable(Topology t, JdbcStreams db, List<PersonId> personIdList, int delayMsec) {
        // Create a stream of Person from a stream of ids
        TStream<PersonId> personIds = t.collection(personIdList);
        if (delayMsec!=0) {
            personIds =  PlumbingStreams.blockingOneShotDelay(personIds,
                    delayMsec, TimeUnit.MILLISECONDS);
        }
        TStream<Person> rcvdPerson = db.executeStatement(personIds,
                () -> "SELECT id, firstname, lastname, gender, age"
                        + " FROM persons WHERE id = ?",
                (tuple,stmt) -> stmt.setInt(1, tuple.id),
                (tuple,resultSet,exc,stream) -> {
                    resultSet.next();
                    int id = resultSet.getInt("id");
                    String firstName = resultSet.getString("firstname");
                    String lastName = resultSet.getString("lastname");
                    String gender = resultSet.getString("gender");
                    int age = resultSet.getInt("age");
                    stream.accept(new Person(id, firstName, lastName, gender, age));
                    }
                );
        return rcvdPerson;
    }
    
    private static java.util.function.Predicate<Person> newOddIdPredicate() {
        return (person) -> person.id % 2 != 0;
    }
    
    private List<String> expectedPersons(java.util.function.Predicate<Person> predicate, List<Person> persons) {
        return persons.stream()
                .filter(predicate)
                .map(person -> person.toString())
                .collect(Collectors.toList());
    }

    @Test
    public void testBasicRead() throws Exception {
        Topology t = this.newTopology("testBasicRead");
        
        populatePersonsTable(getPersonList());
        List<String> expected = expectedPersons(person->true, getPersonList());

        JdbcStreams db = new JdbcStreams(t,
                () -> getDataSource(DB_NAME),
                dataSource -> connect(dataSource));

        // Create a stream of Person from a stream of ids
        TStream<Person> rcvdPerson = readPersonsTable(t, db, getPersonIdList(), 0/*msec*/);
        TStream<String> rcvd = rcvdPerson.map(person -> person.toString());
        
        rcvd.sink(tuple -> System.out.println(
                String.format("%s rcvd: %s", t.getName(), tuple)));
        completeAndValidate("", t, rcvd, SEC_TIMEOUT, expected.toArray(new String[0]));
    }

    @Test
    public void testBasicRead2() throws Exception {
        Topology t = newTopology("testBasicRead2");
        // same as testBasic but use the explicit PreparedStatement forms
        // of executeStatement().
        
        populatePersonsTable(getPersonList());
        List<String> expected = expectedPersons(person->true, getPersonList());

        JdbcStreams db = new JdbcStreams(t,
                () -> getDataSource(DB_NAME),
                dataSource -> connect(dataSource));

        // Create a stream of Person from a stream of ids
        // Delay so this runs after populating the db above
        TStream<PersonId> personIds =  PlumbingStreams.blockingOneShotDelay(
                t.collection(getPersonIdList()), 3, TimeUnit.SECONDS);
        TStream<Person> rcvdPerson = db.executeStatement(personIds,
                (cn) -> cn.prepareStatement("SELECT id, firstname, lastname, gender, age"
                        + " FROM persons WHERE id = ?"),
                (tuple,stmt) -> stmt.setInt(1, tuple.id),
                (tuple,resultSet,exc,stream) -> {
                    resultSet.next();
                    int id = resultSet.getInt("id");
                    String firstName = resultSet.getString("firstname");
                    String lastName = resultSet.getString("lastname");
                    String gender = resultSet.getString("gender");
                    int age = resultSet.getInt("age");
                    stream.accept(new Person(id, firstName, lastName, gender, age));
                    }
                );
        TStream<String> rcvd = rcvdPerson.map(person -> person.toString());
        
        rcvd.sink(tuple -> System.out.println(
                String.format("%s rcvd: %s", t.getName(), tuple)));
        completeAndValidate("", t, rcvd, SEC_TIMEOUT, expected.toArray(new String[0]));
    }
    
    @Test
    public void testBasicWrite() throws Exception {
        Topology t = newTopology("testBasicWrite");
        
        truncatePersonsTable();
        List<String> expected = expectedPersons(person->true, getPersonList());

        JdbcStreams db = new JdbcStreams(t,
                () -> getDataSource(DB_NAME),
                dataSource -> connect(dataSource));
        
        // Add stream of Person to the db
        TStream<Person> s = t.collection(getPersonList());
        TSink<Person> sink = db.executeStatement(s,
                () -> "INSERT INTO persons VALUES(?,?,?,?,?)",
                (tuple,stmt) -> {
                    stmt.setInt(1, tuple.id);
                    stmt.setString(2, tuple.firstName);
                    stmt.setString(3, tuple.lastName);
                    stmt.setString(4, tuple.gender);
                    stmt.setInt(5, tuple.age);
                    }
                );
        assertNotNull(sink);
        
        // Use the same code as testBasicRead to verify the write worked.
        TStream<Person> rcvdPerson = readPersonsTable(t, db, getPersonIdList(), 3000/*msec*/);
        TStream<String> rcvd = rcvdPerson.map(person -> person.toString());
        
        rcvd.sink(tuple -> System.out.println(
                String.format("%s rcvd: %s", t.getName(), tuple)));
        completeAndValidate("", t, rcvd, SEC_TIMEOUT, expected.toArray(new String[0]));
    }
    
    @Test
    public void testBasicWrite2() throws Exception {
        Topology t = newTopology("testBasicWrite2");
        // same as testBasic but use the explicit PreparedStatement forms
        // of executeStatement().
        
        truncatePersonsTable();
        List<String> expected = expectedPersons(person->true, getPersonList());

        JdbcStreams db = new JdbcStreams(t,
                () -> getDataSource(DB_NAME),
                dataSource -> connect(dataSource));
        
        // Add stream of Person to the db
        TStream<Person> s = t.collection(getPersonList());
        TSink<Person> sink = db.executeStatement(s,
                (cn) -> cn.prepareStatement("INSERT into PERSONS values(?,?,?,?,?)"),
                (tuple,stmt) -> {
                    stmt.setInt(1, tuple.id);
                    stmt.setString(2, tuple.firstName);
                    stmt.setString(3, tuple.lastName);
                    stmt.setString(4, tuple.gender);
                    stmt.setInt(5, tuple.age);
                    }
                );

        assertNotNull(sink);
        
        // Use the same code as testBasicRead to verify the write worked.
        TStream<Person> rcvdPerson = readPersonsTable(t, db, getPersonIdList(), 3000/*msec*/);
        TStream<String> rcvd = rcvdPerson.map(person -> person.toString());
        
        rcvd.sink(tuple -> System.out.println(
                String.format("%s rcvd: %s", t.getName(), tuple)));
        completeAndValidate("", t, rcvd, SEC_TIMEOUT, expected.toArray(new String[0]));
    }
    
    @Test
    public void testBadConnectFn() throws Exception {
        Topology t = newTopology("testBadConnectFn");
        // connFn is only called for initial connect or reconnect
        // following certain failures.
        // Hence, to exercise transient connFn failures, we need to start
        // off with a failure.
        
        // TODO for transient connection failure cases, simulate a failure
        // as part of preparedStatement.execute() failing (e.g., force cn-close
        // right before it?), so that we can verify the conn is closed and
        // then reconnected
        
        populatePersonsTable(getPersonList());
        List<String> expected = expectedPersons(p->true, getPersonList().subList(1, getPersonList().size()));
        int expectedExcCnt = getPersonList().size() - expected.size();

        AtomicInteger connFnCnt = new AtomicInteger();
        JdbcStreams db = new JdbcStreams(t,
                () -> getDataSource(DB_NAME),
                dataSource -> {
                    if (connFnCnt.incrementAndGet() == 1)
                        throw new SQLException("FAKE-CONNECT-FN-FAILURE");
                    else
                        return connect(dataSource);
                    });

        // Create a stream of Person from a stream of ids
        AtomicInteger executionExcCnt = new AtomicInteger();
        TStream<PersonId> personIds = t.collection(getPersonIdList());
        TStream<Person> rcvdPerson = db.executeStatement(personIds,
                () -> "SELECT id, firstname, lastname, gender, age"
                        + " FROM persons WHERE id = ?",
                (tuple,stmt) -> stmt.setInt(1, tuple.id),
                (tuple,resultSet,exc,stream) -> {
                    System.out.println(t.getName()+" resultHandler called tuple="+tuple+" exc="+exc);
                    if (exc!=null) {
                        executionExcCnt.incrementAndGet();
                        return;
                    }
                    resultSet.next();
                    int id = resultSet.getInt("id");
                    String firstName = resultSet.getString("firstname");
                    String lastName = resultSet.getString("lastname");
                    String gender = resultSet.getString("gender");
                    int age = resultSet.getInt("age");
                    stream.accept(new Person(id, firstName, lastName, gender, age));
                    }
                );
        TStream<String> rcvd = rcvdPerson.map(person -> person.toString());
        
        rcvd.sink(tuple -> System.out.println(
                String.format("%s rcvd: %s", t.getName(), tuple)));
        completeAndValidate("", t, rcvd, SEC_TIMEOUT, expected.toArray(new String[0]));
        assertEquals("executionExcCnt", expectedExcCnt, executionExcCnt.get());
    }
    
    @Test
    public void testBadSQL() throws Exception {
        Topology t = newTopology("testBadSQL");
        // the statement is nominally "retrieved" only once, not per-tuple.
        // hence, there's not much sense in trying to simulate it
        // getting called unsuccessfully, then successfully, etc.
        // however, verify the result handler gets called appropriately.
        
        populatePersonsTable(getPersonList());
        List<String> expected = Collections.emptyList();
        int expectedExcCnt = getPersonList().size() - expected.size();

        JdbcStreams db = new JdbcStreams(t,
                () -> getDataSource(DB_NAME),
                dataSource -> connect(dataSource));

        // Create a stream of Person from a stream of ids
        AtomicInteger executionExcCnt = new AtomicInteger();
        TStream<PersonId> personIds = t.collection(getPersonIdList());
        TStream<Person> rcvdPerson = db.executeStatement(personIds,
                () -> "SELECT id, firstname, lastname, gender, age"
                        + " FROM persons WHERE BOGUS_XYZZY id = ?",
                (tuple,stmt) -> stmt.setInt(1, tuple.id),
                (tuple,resultSet,exc,stream) -> {
                    System.out.println(t.getName()+" resultHandler called tuple="+tuple+" exc="+exc);
                    if (exc!=null) {
                        executionExcCnt.incrementAndGet();
                        return;
                    }
                    resultSet.next();
                    int id = resultSet.getInt("id");
                    String firstName = resultSet.getString("firstname");
                    String lastName = resultSet.getString("lastname");
                    String gender = resultSet.getString("gender");
                    int age = resultSet.getInt("age");
                    stream.accept(new Person(id, firstName, lastName, gender, age));
                    }
                );
        TStream<String> rcvd = rcvdPerson.map(person -> person.toString());
        
        rcvd.sink(tuple -> System.out.println(
                String.format("%s rcvd: %s", t.getName(), tuple)));
        completeAndValidate("", t, rcvd, SEC_TIMEOUT, expected.toArray(new String[0]));
        assertEquals("executionExcCnt", expectedExcCnt, executionExcCnt.get());
    }
    
    @Test
    public void testBadSetParams() throws Exception {
        Topology t = newTopology("testBadSetParams");
        // exercise and validate  behavior with transient parameter setter failures
        
        populatePersonsTable(getPersonList());
        List<String> expected = expectedPersons(newOddIdPredicate(), getPersonList());
        int expectedExcCnt = getPersonList().size() - expected.size();

        JdbcStreams db = new JdbcStreams(t,
                () -> getDataSource(DB_NAME),
                dataSource -> connect(dataSource));

        // Create a stream of Person from a stream of ids
        AtomicInteger executionExcCnt = new AtomicInteger();
        TStream<PersonId> personIds = t.collection(getPersonIdList());
        TStream<Person> rcvdPerson = db.executeStatement(personIds,
                () -> "SELECT id, firstname, lastname, gender, age"
                        + " FROM persons WHERE id = ?",
                (tuple,stmt) -> { if (tuple.id % 2 != 0)
                                    stmt.setInt(1, tuple.id);
                                  else
                                    stmt.setString(1, "THIS-IS-BOGUS"); },
                (tuple,resultSet,exc,stream) -> {
                    System.out.println(t.getName()+" resultHandler called tuple="+tuple+" exc="+exc);
                    if (exc!=null) {
                        executionExcCnt.incrementAndGet();
                        return;
                    }
                    resultSet.next();
                    int id = resultSet.getInt("id");
                    String firstName = resultSet.getString("firstname");
                    String lastName = resultSet.getString("lastname");
                    String gender = resultSet.getString("gender");
                    int age = resultSet.getInt("age");
                    stream.accept(new Person(id, firstName, lastName, gender, age));
                    }
                );
        TStream<String> rcvd = rcvdPerson.map(person -> person.toString());
        
        rcvd.sink(tuple -> System.out.println(
                String.format("%s rcvd: %s", t.getName(), tuple)));
        completeAndValidate("", t, rcvd, SEC_TIMEOUT, expected.toArray(new String[0]));
        assertEquals("executionExcCnt", expectedExcCnt, executionExcCnt.get());
    }
    
    @Test
    public void testBadResultHandler() throws Exception {
        Topology t = newTopology("testBadResultHandler");
        // exercise and validate behavior with transient result handler failures
        
        populatePersonsTable(getPersonList());
        List<String> expected = expectedPersons(newOddIdPredicate(), getPersonList());
        int expectedExcCnt = getPersonList().size() - expected.size();

        JdbcStreams db = new JdbcStreams(t,
                () -> getDataSource(DB_NAME),
                dataSource -> connect(dataSource));

        // Create a stream of Person from a stream of ids
        AtomicInteger executionExcCnt = new AtomicInteger();
        TStream<PersonId> personIds = t.collection(getPersonIdList());
        TStream<Person> rcvdPerson = db.executeStatement(personIds,
                () -> "SELECT id, firstname, lastname, gender, age"
                        + " FROM persons WHERE id = ?",
                (tuple,stmt) -> stmt.setInt(1, tuple.id),
                (tuple,resultSet,exc,stream) -> {
                    System.out.println(t.getName()+" resultHandler called tuple="+tuple+" exc="+exc);
                    if (exc!=null) {
                        executionExcCnt.incrementAndGet();
                        return;
                    }
                    resultSet.next();
                    int id = resultSet.getInt(tuple.id % 2 == 0
                                                ? "ID-THIS-IS-BOGUS" : "id");
                    String firstName = resultSet.getString("firstname");
                    String lastName = resultSet.getString("lastname");
                    String gender = resultSet.getString("gender");
                    int age = resultSet.getInt("age");
                    stream.accept(new Person(id, firstName, lastName, gender, age));
                    }
                );
        TStream<String> rcvd = rcvdPerson.map(person -> person.toString());
        
        rcvd.sink(tuple -> System.out.println(
                String.format("%s rcvd: %s", t.getName(), tuple)));
        completeAndValidate("", t, rcvd, SEC_TIMEOUT, expected.toArray(new String[0]));
        assertEquals("executionExcCnt", expectedExcCnt, executionExcCnt.get());
    }
    
    
    @Test
    public void testNonResultSetStmt() throws Exception {
        Topology t = newTopology("testNonResultSetStmt");
        // exercise and validate use of non-ResultSet SQL statement
        // wrt proper resultHandler behavior - e.g., receive exception,
        // can generate tuples
        
        List<String> expected = Arrays.asList("once");

        // throw if can't create DataSource - e.g., can't locate derby
        getDataSource(DB_NAME);

        JdbcStreams db = new JdbcStreams(t,
                () -> getDataSource(DB_NAME),
                dataSource -> connect(dataSource));
        
        // Add stream of Person to the db
        TStream<String> trigger = t.collection(expected);
        TStream<String> dropTableStep = db.executeStatement(trigger,
                () -> "DROP TABLE swill",
                (tuple,stmt) -> { /* no params */ },
                (tuple,rs,exc,consumer) -> {
                        // ok if fails
                        System.out.println(t.getName()+" resultHandler drop table exc="+exc);
                        if (rs!=null)
                            throw new IllegalStateException("rs!=null");
                        consumer.accept(tuple);
                    }
                );
        TStream<String> createTableStep = db.executeStatement(dropTableStep,
                () -> "CREATE TABLE swill (id INTEGER NOT NULL)",
                (tuple,stmt) -> { /* no params */ },
                (tuple,rs,exc,consumer) -> {
                        System.out.println(t.getName()+" resultHandler create table exc="+exc);
                        if (rs!=null)
                            throw new IllegalStateException("rs!=null");
                        consumer.accept(tuple);
                    }
                );
        TStream<String> failDropTable = db.executeStatement(createTableStep,
                () -> "DROP TABLE no_such_table",
                (tuple,stmt) -> { /* no params */ },
                (tuple,rs,exc,consumer) -> {
                        System.out.println(t.getName()+" resultHandler fail drop table exc="+exc);
                        if (exc==null)
                            throw new IllegalStateException("exc==null");
                        if (rs!=null)
                            throw new IllegalStateException("rs!=null");
                        consumer.accept(tuple);
                    }
                );
        TStream<String> selectStep = db.executeStatement(failDropTable,
                () -> "SELECT * FROM swill",
                (tuple,stmt) -> { /* no params */ },
                (tuple,rs,exc,consumer) -> {
                        System.out.println(t.getName()+" resultHandler select exc="+exc);
                        if (rs==null)
                            throw new IllegalStateException("rs==null");
                        consumer.accept(tuple);
                    }
                );
        TStream<String> rcvd = selectStep;
                
        completeAndValidate("", t, rcvd, SEC_TIMEOUT, expected.toArray(new String[0]));
    }


}
