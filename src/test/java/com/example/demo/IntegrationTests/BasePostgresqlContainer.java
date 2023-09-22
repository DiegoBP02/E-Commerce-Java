package com.example.demo.IntegrationTests;

import org.testcontainers.containers.PostgreSQLContainer;

public class BasePostgresqlContainer extends PostgreSQLContainer<BasePostgresqlContainer> {
    private static final String IMAGE_VERSION = "postgres:latest";
    private static BasePostgresqlContainer container;

    private BasePostgresqlContainer() {
        super(IMAGE_VERSION);
    }

    public static BasePostgresqlContainer getInstance() {
        if (container == null) {
            container = new BasePostgresqlContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("DB_URL", container.getJdbcUrl());
        System.setProperty("DB_USERNAME", container.getUsername());
        System.setProperty("DB_PASSWORD", container.getPassword());
    }

    @Override
    public void stop() {
    }
}
