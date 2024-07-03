package com.distribuida.books;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.consul.CheckOptions;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.consul.ConsulClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class BooksLifecycle {
    @ConfigProperty(name = "consul.host", defaultValue = "localhost")
    private String consulHost;

    @ConfigProperty(name = "consul.port", defaultValue = "8500")
    private int consulPort;

    @ConfigProperty(name = "quarkus.http.port")
    private int port;

    private String serviceId;

    // Para cuando la app inicie
    public void init(@Observes StartupEvent evt, Vertx vertx) throws UnknownHostException {
        System.out.println("*****BooksLifecyclle.init");

        ConsulClient client = ConsulClient.create(vertx,
                new ConsulClientOptions().setHost(consulHost).setPort(consulPort)
        );

        serviceId = UUID.randomUUID().toString();
        var ipAddress = InetAddress.getLocalHost();
        String httpCheckUrl = String.format("http://%s:%d/books", ipAddress.getHostAddress(), port);

        client.registerServiceAndAwait(
                new ServiceOptions()
                        .setName("app-books")
                        .setId(serviceId)
                        .setAddress(ipAddress.getHostAddress())
                        .setPort(port)
                        .setTags(
                                List.of(
                                        "traefik.enable=true",
                                        "traefik.http.routers.app-books.rule=PathPrefix(`/app-books`)",
                                        "traefik.http.routers.app-books.middlewares=app-books",
                                        "traefik.http.middlewares.app-books.stripPrefix.prefixes=/app-books")
                        )
                        .setCheckOptions(
                                new CheckOptions()
                                        .setHttp(httpCheckUrl)
                                        .setInterval("10s")
                                        .setDeregisterAfter("20s")
                        )
        );
    }

    // Para cuando la app termine
    public void stop (@Observes ShutdownEvent evt, Vertx vertx) {
        System.out.println("*****BooksLifecyclle.stop");

        ConsulClient client = ConsulClient.create(vertx,
                new ConsulClientOptions().setHost(consulHost).setPort(consulPort)
        );

        client.deregisterServiceAndAwait(serviceId);
    }
}
