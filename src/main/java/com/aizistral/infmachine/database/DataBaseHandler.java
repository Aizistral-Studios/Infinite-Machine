package com.aizistral.infmachine.database;

import com.aizistral.infmachine.utils.StandardLogger;

import java.sql.*;

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
        try {
            this.connection = DriverManager.getConnection(dbUrl);
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
        deleteTable(table.getTableName());
        try {
            Statement statement = connection.createStatement();
            String sqlString = "CREATE TABLE IF NOT EXISTS " + table.getTableName() + " (";
            int i = 0;
            for (Field field : table.getFields()) {
                if(i > 0)  sqlString += ",";
                sqlString += "\n"+field.getName() + " " + field.getType().toString();
                if(field.getPrimary()) sqlString += " PRIMARY KEY";
                if(field.getNotNull()) sqlString += " NOT NULL";
                i++;
            }
            sqlString += "\n);";
            statement.execute(sqlString);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    public void deleteTable(String tableName) {
        try {
            Statement statement = connection.createStatement();
            String sqlString = "DROP TABLE IF EXISTS " + tableName + ";";
            statement.execute(sqlString);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    public void executeSQL(String sqlString)
    {
        try {
            Statement statement = connection.createStatement();
            statement.execute(sqlString);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }




}
