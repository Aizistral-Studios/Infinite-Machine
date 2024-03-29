package com.aizistral.infmachine.database;

import com.aizistral.infmachine.utils.StandardLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class DataBaseHandler {
    private static final StandardLogger LOGGER = new StandardLogger("Database Handler");
    private static final String dbUrl = "jdbc:sqlite:./database/database.db";

    public static final DataBaseHandler INSTANCE = new DataBaseHandler();

    private Connection connection;
    private DataBaseHandler()
    {
        LOGGER.log("Initializing database...");
        createNewDatabase();
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
                System.exit(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
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
        executeSQL(sqlString.toString());
    }

    public void deleteTable(String tableName) {
        String sqlString = "DROP TABLE IF EXISTS " + tableName + ";";
        executeSQL(sqlString);
    }

    public void executeSQL(String sqlString)
    {
        try(Connection connection = DriverManager.getConnection(dbUrl)) {
            Statement statement = connection.createStatement();
            statement.execute(sqlString);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    public List<Map<String, Object>> executeQuerySQL(String sqlString)
    {
        try(Connection connection = DriverManager.getConnection(dbUrl)) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sqlString);

            return resultSetToArrayList(resultSet);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
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




}
