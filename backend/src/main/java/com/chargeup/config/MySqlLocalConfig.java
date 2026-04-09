package com.chargeup.config;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.MariaDB4jService;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mysql-local")
@EnableConfigurationProperties(DataSourceProperties.class)
public class MySqlLocalConfig {

    private static final String DB_SERVICE = "mariaDb4jService";
    private static final String DB_NAME = "major";
    private static final String ROOT_USER = "root";
    private static final String ROOT_PASSWORD = "#Sujal200427";

    @Bean(name = DB_SERVICE, initMethod = "start", destroyMethod = "stop")
    public MariaDB4jService mariaDb4jService() {
        var service = new MariaDB4jService();
        var config = service.getConfiguration();
        var dataRoot = Path.of("data", "mysql-local").toAbsolutePath();

        config.setPort(3306);
        config.setBaseDir(dataRoot.resolve("base").toFile());
        config.setDataDir(dataRoot.resolve("db").toFile());
        config.setLibDir(dataRoot.resolve("libs").toFile());
        config.setDeletingTemporaryBaseAndDataDirsOnShutdown(false);
        config.setSecurityDisabled(false);
        config.addArg("--bind-address=127.0.0.1");
        return service;
    }

    @Bean
    @Primary
    @DependsOn(DB_SERVICE)
    public DataSource dataSource(MariaDB4jService mariaDb4j, DataSourceProperties properties) throws Exception {
        ensureDatabaseAndCredentials(mariaDb4j.getDB());

        return DataSourceBuilder.create()
            .driverClassName(properties.getDriverClassName())
            .url(properties.getUrl())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();
    }

    private void ensureDatabaseAndCredentials(DB db) throws Exception {
        db.createDB(DB_NAME);
        db.run("CREATE USER IF NOT EXISTS '" + ROOT_USER + "'@'localhost' IDENTIFIED BY '" + ROOT_PASSWORD + "';");
        db.run("CREATE USER IF NOT EXISTS '" + ROOT_USER + "'@'127.0.0.1' IDENTIFIED BY '" + ROOT_PASSWORD + "';");
        db.run("ALTER USER '" + ROOT_USER + "'@'localhost' IDENTIFIED BY '" + ROOT_PASSWORD + "';");
        db.run("ALTER USER '" + ROOT_USER + "'@'127.0.0.1' IDENTIFIED BY '" + ROOT_PASSWORD + "';");
        db.run("GRANT ALL PRIVILEGES ON *.* TO '" + ROOT_USER + "'@'localhost' WITH GRANT OPTION;");
        db.run("GRANT ALL PRIVILEGES ON *.* TO '" + ROOT_USER + "'@'127.0.0.1' WITH GRANT OPTION;");
        db.run("FLUSH PRIVILEGES;");
    }
}
