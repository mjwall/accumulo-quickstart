package com.accumulobook.quickstart;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Map;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.user.GrepIterator;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.client.Scanner;
import org.apache.hadoop.io.Text;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

class AbstractCommand {

    protected Connector connection = null;

    public void setConnection(Connector connection) {
        this.connection = connection;
    }
}

class CreateTableCommand extends AbstractCommand {
    @Parameter(names = {"-t","--table"}, description = "Table name to create", required = true)
    private String table;

    public void run() throws AccumuloException, AccumuloSecurityException, TableExistsException {
        System.out.println("Creating table " + table);
        if (connection.tableOperations().exists(table)) {
            throw new RuntimeException("Table " + table + " already exists");
        } else {
            connection.tableOperations().create(table);
            System.out.println("Table created");
        }
    }
}

class InsertRowCommand extends AbstractCommand {
    @Parameter(names = {"-t","--table"}, description = "Table to scan", required = true)
    private String table;

    @Parameter(names = {"-r","--rowid"}, description = "Row Id to insert", required = true)
    private String rowId;

    @Parameter(names = {"-cf","--columnFamily"}, description = "Column Family to insert", required = true)
    private String cf;

    @Parameter(names = {"-cq","--columnQualifier"}, description = "Column Qualifier to insert", required = true)
    private String cq;

    @Parameter(names = {"-val","--value"}, description = "Value to insert", required = true)
    private String val;

    @Parameter(names = {"-ts","--timestamp"}, description = "Timestamp to use on row insert")
    private long timestamp;

    @Parameter(names = {"-a","--auths"}, description = "ColumnVisiblity expression to insert with data")
    private String auths;

    public void run() throws TableNotFoundException, MutationsRejectedException {
        System.out.println("Writing mutation for " + rowId);
        BatchWriter bw = connection.createBatchWriter(table, new BatchWriterConfig());

        Mutation m = new Mutation(new Text(rowId));
        m.put(new Text(cf), new Text(cq), new ColumnVisibility(auths), timestamp, new Value(val.getBytes()));
        bw.addMutation(m);
        bw.close();
    }
}

class ScanCommand extends AbstractCommand {
    @Parameter(names = {"-t","--table"}, description = "Table to scan", required = true)
    private String table;

    @Parameter(names = {"-r","--row"}, description = "Row to scan")
    private String row;

    @Parameter(names = {"-a","--auths"}, description = "Comma separated list of scan authorizations")
    private String auths;

    private String user;

    public void setUser(String user) {
        this.user = user;
    }

    public void run() throws TableNotFoundException, AccumuloException, AccumuloSecurityException {
        System.out.println("Scanning " + table);
        Authorizations authorizations = null;
        if ((null != auths) && (!auths.equals("SCAN_ALL"))) {
            System.out.println("Using scan auths " + auths);
            authorizations = new Authorizations(auths.split(","));
        } else {
            System.out.println("Scanning with all user auths");
            authorizations = connection.securityOperations().getUserAuthorizations(user);
        }
        Scanner scanner = connection.createScanner(table, authorizations);
        if ((null != row) && (!row.equals("SCAN_ALL"))) {
            System.out.println("Scanning for row " + row);
            scanner.setRange(new Range(row));
        } else {
            System.out.println("Scanning for all rows");
        }
        System.out.println("Results ->");
        for (Entry<Key,Value> entry : scanner) {
            System.out.println("  " + entry.getKey() + " " + entry.getValue());
        }
    }
}

class GrepCommand extends AbstractCommand {
    @Parameter(names = {"-t","--table"}, description = "Table to scan", required = true)
    private String table;

    @Parameter(names = "--term", description = "Term to grep for in table", required = true)
    private String term;

    private String user;

    public void setUser(String user) {
        this.user = user;
    }

    public void run() throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
        System.out.println("Grepping " + table);
        Authorizations authorizations = connection.securityOperations().getUserAuthorizations(user);
        Scanner scanner = connection.createScanner(table, authorizations);
        Map<String, String> grepProps = new HashMap<String, String>();
        grepProps.put("term", term);
        IteratorSetting is = new IteratorSetting(25, "sample-grep", GrepIterator.class.getName(), grepProps);
        scanner.addScanIterator(is);
        System.out.println("Results ->");
        for (Entry<Key,Value> entry : scanner) {
            System.out.println("  " + entry.getKey() + " " + entry.getValue());
        }
    }
}

public class JavaExample {

    @Parameter(names = {"-p", "--password"}, description = "Accumulo user password", required = true)
    private String password;

    @Parameter(names = {"-u","--user"}, description = "Accumulo user", required = true)
    private String user;

    @Parameter(names = {"-i","--instance"}, description = "Accumulo instance name", required = true)
    private String instance;

    @Parameter(names = {"-z","--zookeepers"}, description = "Comma-separated list of zookeepers", required = true)
    private String zookeepers;

    public Connector getConnection() throws AccumuloException, AccumuloSecurityException {
        Instance i = new ZooKeeperInstance(instance, zookeepers);
        Connector conn = i.getConnector(user, new PasswordToken(password));
        return conn;
    }

    public static void main(String[] args) {
        JavaExample javaExample = new JavaExample();
        JCommander jc = new JCommander(javaExample);

        CreateTableCommand createTableCommand = new CreateTableCommand();
        jc.addCommand("create", createTableCommand);
        InsertRowCommand insertCommand = new InsertRowCommand();
        jc.addCommand("insert", insertCommand);
        ScanCommand scanCommand = new ScanCommand();
        jc.addCommand("scan", scanCommand);
        GrepCommand grepCommand = new GrepCommand();
        jc.addCommand("grep", grepCommand);

        try {
            jc.parse(args);
            String command = jc.getParsedCommand();
            if (null == command) {
                throw new RuntimeException("You didn't choose a command");
            } else if (command.equals("create")) {
                System.out.println("Running create command");
                createTableCommand.setConnection(javaExample.getConnection());
                createTableCommand.run();
            } else if (command.equals("insert")) {
                System.out.println("Running insert command");
                insertCommand.setConnection(javaExample.getConnection());
                insertCommand.run();
            } else if (command.equals("scan")) {
                System.out.println("Running scan command");
                scanCommand.setConnection(javaExample.getConnection());
                scanCommand.setUser(javaExample.user);
                scanCommand.run();
            } else if (command.equals("grep")) {
                System.out.println("Running grep command");
                grepCommand.setConnection(javaExample.getConnection());
                grepCommand.setUser(javaExample.user);
                grepCommand.run();
            } else {
                throw new RuntimeException("Unrecognized command " + command);
            }
        } catch (Exception e) {
            System.err.println("Error: " +  e.getMessage());
            jc.usage();
        }
    }

    /*
    ==== Creating a table and inserting some data

            Now that you know how to get help on shell commands, let create a table and insert some data.
            Since Accumulo is a schema-less database, all you need is the table name.
            The schema will evolve as you insert data.
            So let's create a table a named 'table1'.
            Use the 'createtable' command and run:

                root@miniInstance> createtable table1
                root@miniInstance table1>

            Notice the prompt changed and now shows you the current table 'table1'.

            The table is currently empty, so we need to insert some data.
            We will use the 'insert' command.
            We introduced the <<data_model>> Data Model in chapter 1, so let's insert that data from that <<intro_table_kvtable>> table.

                insert "bob jones" "contact" "address" "123 any street" -l "billing" -ts 13234
                insert "bob jones" "contact" "city" "anytown" -l "billing" -ts 13234
                insert "bob jones" "contact" "phone" "555-1212" -l "billing" -ts 13234
                insert "bob jones" "purchases" "sneakers" "$60" -l "billing&inventory" -ts 13255
                insert "fred smith" "contact" "address" "444 main st." -l  "billing" -ts 13222
                insert "fred smith" "contact" "city" "othertown" -l "billing" -ts 13222
                insert "fred smith" "purchases" "glasses" "$30" -l  "billing&inventory" -ts 13201
                insert "fred smith" "purchases" "hat" "$20" -l "billing&inventory" -ts 13267

            ==== Scanning for data

            Once you get data into Accumulo, you need to view it.
            We will use the 'scan' command.
            But if you just type 'scan', no results will be returned.
            The data we entered included ColumnVisiblities with the '-l' switch, but the root user does not have those.
            A user in Accumulo can write data with any authorizations, but viewing records requires authorization.
            Assign the necessary auths with the following:

                setauths -u root -s billing,inventory

            You can see all the records with just 'scan'

                root@miniInstance table1> scan
                bob jones contact:city [billing]    anytown
                bob jones contact:phone [billing]    555-1212
                bob jones purchases:sneakers [billing&inventory]    $60
                fred smith contact:address [billing]    444 main st.
                fred smith contact:city [billing]    othertown
                fred smith purchases:glasses [billing&inventory]    $30
                fred smith purchases:hat [billing&inventory]    $20

            You may have noticed that the timestamps inserted in the records with the '-ts' switch don't show.
            Use scan's -st or --show-timestamps switch to see those

                root@miniInstance table1> scan -st
                bob jones contact:city [billing] 13234    anytown
                bob jones contact:phone [billing] 13234    555-1212
                bob jones purchases:sneakers [billing&inventory] 13255    $60
                fred smith contact:address [billing] 13222    444 main st.
                fred smith contact:city [billing] 13222    othertown
                fred smith purchases:glasses [billing&inventory] 13201    $30
                fred smith purchases:hat [billing&inventory] 13267    $20

            If you want to view just the records for one rowid, use the '-r' switch.
            This will limit the results to one rowId.

                root@miniInstance table1> scan -r "bob jones"
                bob jones contact:city [billing]    anytown
                bob jones contact:phone [billing]    555-1212
                bob jones purchases:sneakers [billing&inventory]    $60

            ==== Using auths

            By default, the scan command will use all the user's granted authorizations.
            Use the '-s' switch to limit the scan authorizations.

                root@miniInstance table1> scan -s billing
                bob jones contact:city [billing]    anytown
                bob jones contact:phone [billing]    555-1212
                fred smith contact:address [billing]    444 main st.
                fred smith contact:city [billing]    othertown

            We did not insert any records with only the 'inventory' visiblity, so you will not see any using just that authorization.

            ==== Using a simple iterator

            We have briefly discussed Accumulo's iterators.
            One built in iterator is the GrepIteratorfootnote:[http://accumulo.apache.org/1.5/apidocs/index.html?org/apache/accumulo/core/iterators/user/GrepIterator.html], which searches the Key and the Value for an exact string match.
            The shell's 'grep' command sets up this iterator and uses it during the scan.

                root@miniInstance table1> grep town
                bob jones contact:city [billing]    anytown
                fred smith contact:city [billing]    othertown
                */
}
