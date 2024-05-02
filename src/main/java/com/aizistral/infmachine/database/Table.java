package com.aizistral.infmachine.database;

import java.sql.SQLException;
import java.util.ArrayList;

public class Table {
    private String tableName;
    private ArrayList<Field> fields;
    private Table(Builder tableBuilder) {
        this.tableName = tableBuilder.tableName;
        this.fields = tableBuilder.fields;
    }

    public String getTableName() {
        return tableName;
    }

    public ArrayList<Field> getFields() {
        return fields;
    }



    // ---------------------------------------------------------------------------------------------------------- //
    public static class Builder {
        private String tableName;
        private ArrayList<Field> fields = new ArrayList<>();

        public Builder(String tableName){
            this.tableName = tableName;
        }

        public Builder addField(String fieldName, FieldType fieldType, Boolean isPrimary, Boolean isNotNull) {
            if(isPrimary && hasPrimary()) throw new RuntimeException("Tried to add second primary-key in addField during table-building.");
            this.fields.add(new Field(fieldName, fieldType, isPrimary, isNotNull));
            return this;
        }

        public Table build() {
            return new Table(this);
        }

        private boolean hasPrimary() {
            for(Field field : this.fields)
            {
                if(field.getPrimary()) return true;
            }
            return false;
        }
    }
}
