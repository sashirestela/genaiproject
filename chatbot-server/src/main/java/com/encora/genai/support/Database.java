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

import com.encora.genai.data.Fragment;
import com.encora.genai.data.FragmentResult;
import com.pgvector.PGvector;

public class Database {

    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
    private static final String JDBC_URL;

    private static final int BATCH_SIZE = 500;

    static {
        JDBC_URL = System.getenv("JDBC_URL") + "&reWriteBatchedInserts=true";
    }

    private Database() {
    }

    public static void prepareDatabase() {
        String extensionSql = "CREATE EXTENSION IF NOT EXISTS vector";
        String dropTableSql = "DROP TABLE IF EXISTS fragment";
        String createTableSql = ""
                + "CREATE TABLE IF NOT EXISTS fragment (id bigserial PRIMARY KEY, "
                + "reference text, content text, embedding vector(1536))";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
                Statement statement = connection.createStatement();) {
            statement.executeUpdate(extensionSql);
            PGvector.addVectorType(connection);
            statement.executeUpdate(dropTableSql);
            statement.executeUpdate(createTableSql);
            LOGGER.debug("The table 'fragment' was created.");
        } catch (Exception e) {
            throw new RuntimeException("Cannot prepare the database.", e);
        }
    }

    public static void insertSegments(List<Fragment> fragments) {
        String insertSql = "INSERT INTO fragment (reference, content, embedding) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
                PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            int i = 0;
            int b = 0;
            connection.setAutoCommit(false);
            for (Fragment fragment : fragments) {
                insertStmt.setString(1, fragment.getReference());
                insertStmt.setString(2, fragment.getContent());
                insertStmt.setObject(3, new PGvector(fragment.getEmbedding()));
                insertStmt.addBatch();
                if (++i % BATCH_SIZE == 0) {
                    insertStmt.executeBatch();
                    connection.commit();
                    b++;
                    LOGGER.debug("{} fragments were inserted in database.", BATCH_SIZE);
                }
            }
            insertStmt.executeBatch();
            connection.commit();
            LOGGER.debug("{} fragments were inserted in database.", i - b * BATCH_SIZE);
        } catch (Exception e) {
            throw new RuntimeException("Cannot insert a fragment.", e);
        }
    }

    public static void indexSegments() {
        String createIndexSql = ""
                + "CREATE INDEX IF NOT EXISTS fragment_embedding ON fragment "
                + "USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
                Statement statement = connection.createStatement();) {
            statement.executeUpdate(createIndexSql);
            LOGGER.debug("The index 'fragment_embedding' was created.");
        } catch (Exception e) {
            throw new RuntimeException("Cannot create the index.", e);
        }
    }

    public static List<FragmentResult> selectFragments(List<Double> embedding, double matchThreshold, int matchCount) {
        String selectSql = ""
                + "SELECT reference, content, 1 - (embedding <=> ?) AS similarity, "
                + "row_number() OVER (ORDER BY embedding <=> ?) AS rowid "
                + "FROM fragment WHERE (embedding <=> ?) < (1 - ?) LIMIT ?";
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
                PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setObject(1, new PGvector(embedding));
            selectStmt.setObject(2, new PGvector(embedding));
            selectStmt.setObject(3, new PGvector(embedding));
            selectStmt.setDouble(4, matchThreshold);
            selectStmt.setInt(5, matchCount);
            ResultSet resultSet = selectStmt.executeQuery();
            List<FragmentResult> fragments = new ArrayList<>();
            while (resultSet.next()) {
                FragmentResult fragment = FragmentResult.builder()
                        .reference(resultSet.getString("reference"))
                        .content(resultSet.getString("content"))
                        .similarity(resultSet.getDouble("similarity"))
                        .rowid(resultSet.getInt("rowid"))
                        .build();
                fragments.add(fragment);
                LOGGER.debug("Fragment [rowId:{}, similarity:{}, reference: {}]",
                        fragment.getRowid(), fragment.getSimilarity(), fragment.getReference());
            }
            LOGGER.debug("{} fragments were selected.", fragments.size());
            return fragments;
        } catch (Exception e) {
            throw new RuntimeException("Cannot select fragments.", e);
        }
    }

}
