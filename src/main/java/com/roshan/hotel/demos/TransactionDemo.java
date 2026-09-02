package com.roshan.hotel.demos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionDemo {

    public static void main(String[] args) throws SQLException{

        String url = "jdbc:postgresql://localhost:5432/hotel_management";
        String user = "roshanhingnekar";

        try (Connection connection = DriverManager.getConnection(url, user, "")){
            connection.setAutoCommit(false);

            try {
                String insertSql = "INSERT INTO guests (id, name) VALUES (?,?)";

                try(PreparedStatement insertStatement = connection.prepareStatement(insertSql)){
                    insertStatement.setLong(1,2001);
                    insertStatement.setString(2,"Transaction Test Guest 1");

                    insertStatement.executeUpdate();
                }

                // Intentionally broken SQL
                String brokenSql = "INSERT INTO guests (id, name) VALUES(?,?)";

                try(PreparedStatement brokenStatement = connection.prepareStatement(brokenSql)){
                    brokenStatement.setLong(1,2002);
                    brokenStatement.setString(2,"Transaction Test Guest 2");

                    brokenStatement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException e){
                System.out.println("Something failed... Rolling back");
                connection.rollback();
            }

        }







    }

}
