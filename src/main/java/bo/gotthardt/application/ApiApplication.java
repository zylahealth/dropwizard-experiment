package bo.gotthardt.application;

import bo.gotthardt.PersonEndpoint;
import bo.gotthardt.configuration.ApiConfiguration;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

/**
 * @author Bo Gotthardt
 */
public class ApiApplication extends Service<ApiConfiguration> {
    public static void main(String... args) throws Exception {
        new ApiApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<ApiConfiguration> bootstrap) {

    }

    @Override
    public void run(ApiConfiguration configuration, Environment environment) throws Exception {
        environment.addResource(new PersonEndpoint());

        environment.addHealthCheck(new EbeanHealthCheck());
    }
}