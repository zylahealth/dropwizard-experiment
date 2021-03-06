package bo.gotthardt.todolist.application;

import bo.gotthardt.application.BuildInfo;
import bo.gotthardt.application.VersionHealthCheck;
import bo.gotthardt.ebean.DbDiffCommand;
import bo.gotthardt.ebean.EbeanBundle;
import bo.gotthardt.jersey.filter.BasicAuthFilter;
import bo.gotthardt.jersey.parameters.ListFilteringFactory;
import bo.gotthardt.model.User;
import bo.gotthardt.oauth2.OAuth2Bundle;
import bo.gotthardt.queue.WorkersCommand;
import bo.gotthardt.queue.rabbitmq.RabbitMQBundle;
import bo.gotthardt.rest.CrossOriginResourceSharing;
import bo.gotthardt.todo.TodoClientBundle;
import bo.gotthardt.todolist.rest.TodoListResource;
import bo.gotthardt.user.EmailVerificationResource;
import bo.gotthardt.user.UserResource;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Stopwatch;
import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author Bo Gotthardt
 */
@Slf4j
public class TodoListApplication extends Application<TodoListConfiguration> {
    private static Timer startupTimeMetric;

    @Getter
    private EbeanBundle ebeanBundle;
    private RabbitMQBundle rabbitMqBundle;

    public static void main(String... args) throws Exception {
        Stopwatch startupTimer = Stopwatch.createStarted();
        new TodoListApplication().run(args);

        if (startupTimeMetric != null) {
            long elapsed = startupTimer.stop().elapsed(TimeUnit.MILLISECONDS);
            startupTimeMetric.update(elapsed, TimeUnit.MILLISECONDS);
            log.info("Startup took {} ms.", elapsed);
        }
    }

    @Override
    public void initialize(Bootstrap<TodoListConfiguration> bootstrap) {
        ebeanBundle = new EbeanBundle();
        //rabbitMqBundle = new RabbitMQBundle();

        // This outputs xDateTimes as ISO strings rather than an array of numbers in JSON.
        bootstrap.getObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        bootstrap.addBundle(new Java8Bundle());
        bootstrap.addBundle(ebeanBundle);
        //bootstrap.addBundle(rabbitMqBundle);
        bootstrap.addBundle(new OAuth2Bundle(ebeanBundle));
        bootstrap.addBundle(new TodoClientBundle());
        bootstrap.addBundle(new MigrationsBundle<TodoListConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(TodoListConfiguration configuration) {
                return configuration.getDatabaseConfig();
            }
        });

        // The anonymous subclass seems to be needed for the config type to be picked up correctly.
        bootstrap.addCommand(new WorkersCommand<TodoListConfiguration>(TodoListApplication.this) {});
        bootstrap.addCommand(new DbDiffCommand<TodoListConfiguration>() {});
    }

    @Override
    public void run(TodoListConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new TodoListHK2Binder(environment, configuration, ebeanBundle, rabbitMqBundle));

        environment.jersey().register(TodoListResource.class);
        environment.jersey().register(UserResource.class);
        environment.jersey().register(EmailVerificationResource.class);

        environment.jersey().register(ListFilteringFactory.getBinder());

        CrossOriginResourceSharing.enableFor(environment.servlets(), "/*");
        CrossOriginResourceSharing.enableFor(environment.admin(), "/healthcheck");

        BuildInfo buildInfo = BuildInfo.create(environment.getObjectMapper());
        log.info(buildInfo.printableInfo());
        environment.healthChecks().register("version", new VersionHealthCheck(buildInfo));

        BasicAuthFilter.addToAdmin(environment, "test", "test");

        startupTimeMetric = environment.metrics().timer(MetricRegistry.name(TodoListApplication.class, "startup"));

        User user = new User("test", "test", "Test Testsen");
        user.setEmail("example@example.com");
        ebeanBundle.getEbeanServer().save(user);



        //rabbitMqBundle.getQueue("username", User.class).publish(user);
    }
}
