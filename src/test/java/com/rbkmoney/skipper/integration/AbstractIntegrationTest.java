package com.rbkmoney.skipper.integration;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import com.rbkmoney.skipper.SkipperApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.boot.test.util.TestPropertyValues.Type.MAP;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = {SkipperApplication.class},
        initializers = AbstractIntegrationTest.Initializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    private static final int PORT = 15432;

    private static final String DB_NAME = "skipper";

    private static final String DB_USER = "postgres";

    private static final String DB_PASSWORD = "postgres";

    private static final String JDBC_URL = "jdbc:postgresql://localhost:" + PORT + "/" + DB_NAME;

    private static EmbeddedPostgres postgres;

    @After
    public void destroy() throws IOException {
        if (postgres != null) {
            postgres.close();
            postgres = null;
        }
    }

    private static void startPgServer() {
        try {
            log.info("The PG server is starting...");
            EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder();
            String dbDir = prepareDbDir();
            log.info("Dir for PG files: " + dbDir);
            builder.setDataDirectory(dbDir);
            builder.setPort(PORT);
            postgres = builder.start();
            log.info("The PG server was started!");
        } catch (IOException e) {
            log.error("An error occurred while starting server ", e);
            e.printStackTrace();
        }
    }

    private static void createDatabase() {
        try (Connection conn = postgres.getPostgresDatabase().getConnection()) {
            Statement statement = conn.createStatement();
            statement.execute("CREATE DATABASE " + DB_NAME);
            statement.close();
        } catch (SQLException e) {
            log.error("An error occurred while creating the database "+ DB_NAME, e);
            e.printStackTrace();
        }
    }

    private static String prepareDbDir() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String currentDate = dateFormat.format(new Date());
        String dir = "target" + File.separator + "pgdata_" + currentDate;
        log.info("Postgres source files in {}", dir);
        return dir;
    }

    private DataSource getDataSource() {
        return postgres.getDatabase(DB_USER, DB_NAME);
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of("spring.datasource.url=" + JDBC_URL,
                    "spring.datasource.username=" + DB_USER,
                    "spring.datasource.password=" + DB_PASSWORD,
                    "flyway.url=" + JDBC_URL,
                    "flyway.user=" + DB_USER,
                    "flyway.password=" + DB_PASSWORD)
                    .applyTo(configurableApplicationContext.getEnvironment(), MAP, "testcontainers");

            if (postgres == null) {
                startPgServer();
                createDatabase();
            }
        }
    }

}