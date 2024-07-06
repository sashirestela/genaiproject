package com.encora.genai.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgvector.PGvector;

public class Database {

    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
    private static final String JDBC_URL;

    static {
        JDBC_URL = System.getenv("JDBC_URL");
    }

    private Database() {
    }

    public static void prepareDatabase() {
        String extensionSql = "CREATE EXTENSION IF NOT EXISTS vector";
        String createTableSql = "CREATE TABLE IF NOT EXISTS fragment (id bigserial PRIMARY KEY, content text, embedding vector(1536))";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
                Statement statement = connection.createStatement();) {
            statement.executeUpdate(extensionSql);
            PGvector.addVectorType(connection);
            statement.executeUpdate(createTableSql);
        } catch (Exception e) {
            throw new RuntimeException("Cannot prepare the database.", e);
        }
    }

    public static void insertSegments(List<Fragment> fragments) {
        String insertSql = "INSERT INTO fragment (content, embedding) VALUES (?, ?)";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
                PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            int i = 0;
            for (Fragment fragment : fragments) {
                insertStmt.setString(1, fragment.getContent());
                insertStmt.setObject(2, new PGvector(fragment.getEmbedding()));
                insertStmt.executeUpdate();
                LOGGER.debug("Fragment {} was inserted in database.", ++i);
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot insert a fragment.", e);
        }
    }

    public static void indexSegments() {
        String createIndexSql = "CREATE INDEX IF NOT EXISTS fragment_embedding ON fragment "
                + "USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
                Statement statement = connection.createStatement();) {
            statement.executeUpdate(createIndexSql);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create the index.", e);
        }
    }

    public static List<Fragment> selectFragments(List<Double> embedding, int matchCount) {
        String selectSql = "SELECT id, content FROM fragment ORDER BY embedding <=> ? LIMIT ?";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
                PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setObject(1, new PGvector(embedding));
            selectStmt.setInt(2, matchCount);
            ResultSet resultSet = selectStmt.executeQuery();
            List<Fragment> fragments = new ArrayList<>();
            while (resultSet.next()) {
                Fragment fragment = Fragment.builder()
                .id(resultSet.getLong("id"))
                .content(resultSet.getString("content"))
                .build();
                fragments.add(fragment);
            }
            return fragments;
        } catch (Exception e) {
            throw new RuntimeException("Cannot insert a fragment.", e);
        }
    }

}
