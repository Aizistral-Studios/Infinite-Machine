package com.aizistral.infmachine.database;

import com.aizistral.infmachine.data.ExitCode;
import com.aizistral.infmachine.utils.StandardLogger;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class DataBaseHandler {
    private static final StandardLogger LOGGER = new StandardLogger("Database Handler");
    private static final String path = "./database/database.db";
    private static final String dbUrl = "jdbc:sqlite:" + path;

    public static final DataBaseHandler INSTANCE = new DataBaseHandler();

    private ArrayList<String> tableNames = new ArrayList<>();

    private DataBaseHandler()
    {
        File file = new File(path);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        LOGGER.log("Initializing database...");
        createNewDatabase();
        Table.Builder tableBuilder = new Table.Builder("metaData");
        tableBuilder.addField("id", FieldType.LONG, true, true);
        tableBuilder.addField("infiniteVersion", FieldType.STRING, false, true);
        tableBuilder.addField("isIndexed", FieldType.BOOLEAN, false, false);
        Table table = tableBuilder.build();
        createNewTable(table);
        LOGGER.log("Database initialization complete.");
    }

    private void createNewDatabase() {
        try(Connection connection = DriverManager.getConnection(dbUrl)) {
            if (connection != null) {
                DatabaseMetaData meta = connection.getMetaData();
                LOGGER.log("The driver name is " + meta.getDriverName());
                LOGGER.log("A new database has been created.");
            } else {
                LOGGER.error("Database not available and failed to create.");
                System.exit(ExitCode.DATABASE_ERROR.getCode());
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            System.exit(ExitCode.DATABASE_ERROR.getCode());
        }
    }

    public void createNewTable(Table table) {
        StringBuilder sqlString = new StringBuilder("CREATE TABLE IF NOT EXISTS " + table.getTableName() + " (");
        int i = 0;
        for (Field field : table.getFields()) {
            if(i > 0)  sqlString.append(",");
            sqlString.append("\n").append(field.getName()).append(" ").append(field.getType().toString());
            if(field.getPrimary()) sqlString.append(" PRIMARY KEY");
            if(field.getNotNull()) sqlString.append(" NOT NULL");
            i++;
        }
        sqlString.append("\n);");
        if(executeSQL(sqlString.toString())) tableNames.add(table.getTableName());
    }

    public boolean executeSQL(String sqlString)
    {
        try(Connection connection = DriverManager.getConnection(dbUrl)) {
            Statement statement = connection.createStatement();
            statement.execute(sqlString);
            return true;
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            System.exit(ExitCode.DATABASE_ERROR.getCode());
        }
        return false;
    }

    public List<Map<String, Object>> executeQuerySQL(String sqlString)
    {
        try(Connection connection = DriverManager.getConnection(dbUrl)) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sqlString);

            return resultSetToArrayList(resultSet);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            System.exit(ExitCode.DATABASE_ERROR.getCode());
        }
        return null;
    }

    public List<Map<String, Object>> resultSetToArrayList(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<Map<String, Object>> list = new ArrayList<>();
        while (resultSet.next()) {
            Map<String, Object> row = new HashMap<>(columnCount);
            for (int i = 1; i <= columnCount; ++i) {
                row.put(metaData.getColumnName(i), resultSet.getObject(i));
            }
            list.add(row);
        }
        return list;
    }

    public void setInfiniteVersion(String version) {
        if(retrieveInfiniteVersion().equals(version)) return;
        String sql = String.format("INSERT INTO metaData (id, infiniteVersion) VALUES (1, \"%s\") ON CONFLICT(id) DO UPDATE SET infiniteVersion = \"%s\"", version, version);
        executeSQL(sql);
    }

    public String retrieveInfiniteVersion() {
        String sql = "SELECT * FROM metaData LIMIT 1;";
        List<Map<String, Object>> results = DataBaseHandler.INSTANCE.executeQuerySQL(sql);
        if(results.isEmpty()) return "";
        return results.get(0).get("infiniteVersion").toString();
    }

    public void setPrimalIndexation() {
        String sql = "UPDATE metaData SET isIndexed = \"true\" WHERE id = 1";
        executeSQL(sql);
    }

    public Boolean retrievePrimalIndexation() {
        String sql = "SELECT * FROM metaData LIMIT 1;";
        List<Map<String, Object>> results = DataBaseHandler.INSTANCE.executeQuerySQL(sql);
        if(results.isEmpty()) return false;
        return results.get(0).get("isIndexed") != null;
    }




}
