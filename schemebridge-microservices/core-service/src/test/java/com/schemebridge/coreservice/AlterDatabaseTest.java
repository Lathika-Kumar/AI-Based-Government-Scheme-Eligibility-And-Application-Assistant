package com.schemebridge.coreservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@SpringBootTest
public class AlterDatabaseTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void alterTables() {
        System.out.println("Executing Oracle DDL Alter statements...");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            try {
                stmt.execute("ALTER TABLE SCHEMES MODIFY (TITLE_ENGLISH VARCHAR2(1000))");
                System.out.println("Altered TITLE_ENGLISH to VARCHAR2(1000)");
            } catch (Exception e) {
                System.out.println("Alter TITLE_ENGLISH note: " + e.getMessage());
            }

            try {
                stmt.execute("ALTER TABLE SCHEMES MODIFY (TITLE_TAMIL VARCHAR2(1000))");
                System.out.println("Altered TITLE_TAMIL to VARCHAR2(1000)");
            } catch (Exception e) {
                System.out.println("Alter TITLE_TAMIL note: " + e.getMessage());
            }

            try {
                stmt.execute("ALTER TABLE SCHEMES MODIFY (SCHEME_CODE VARCHAR2(200))");
                System.out.println("Altered SCHEME_CODE to VARCHAR2(200)");
            } catch (Exception e) {
                System.out.println("Alter SCHEME_CODE note: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Database alter failed: " + e.getMessage());
        }
    }
}
