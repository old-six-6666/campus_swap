package com.itcodai.campus_swap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 原生JDBC测试数据库连接
 */
public class JdbcConnectionTest {
    public static void main(String[] args) {
        // 替换为你配置文件中的实际信息
        String url = "jdbc:mysql://localhost:3306/campus_swap?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "He2004080922"; // 务必替换

        // 声明连接对象
        Connection connection = null;

        try {
            // 加载驱动（MySQL 8.0+ 可省略，自动加载）
            Class.forName("com.mysql.cj.jdbc.Driver");
            // 获取连接
            connection = DriverManager.getConnection(url, username, password);

            if (connection != null && !connection.isClosed()) {
                System.out.println("✅ 数据库连接成功！");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ 驱动加载失败：" + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ 数据库连接失败：" + e.getMessage());
            // 常见错误原因：密码错误、数据库未创建、端口错误、MySQL服务未启动
        } finally {
            // 关闭连接
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}