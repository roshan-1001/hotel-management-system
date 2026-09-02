package com.roshan.hotel.demos;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class JdbcDemo {

    public static void main(String[] args) throws SQLException{

        String url = "jdbc:postgresql://localhost:5432/hotel_management";
        String user = "roshanhingnekar";
        Connection connection = DriverManager.getConnection(url, user, "");
        System.out.println("Connected to PostgreSQL!");
        //connection.close();

        String sql = "SELECT * FROM guests WHERE id=?";
        PreparedStatement statement =  connection.prepareStatement(sql);
        statement.setLong(1,2);
        ResultSet resultSet = statement.executeQuery();
        while(resultSet.next()){
            long id = resultSet.getLong(1);
            String name = resultSet.getString(2);
            System.out.println(id);
            System.out.println(name);
        }
        resultSet.close();
        statement.close();

//        String insertSql = "INSERT INTO guests (id, name) VALUES (?,?)";
//        PreparedStatement insertStatement = connection.prepareStatement(insertSql);
//        insertStatement.setLong(1,2);
//        insertStatement.setString(2,"Srag");
//        int rowsAffected = insertStatement.executeUpdate();
//        System.out.println("Rows affected: " + rowsAffected);
//        insertStatement.close();
        connection.close();


    }
}
