package bo.gotthardt.test;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;
import lombok.Delegate;

/**
 * In-memory EbeanServer for use in unit/integration tests.
 *
 * @author Bo Gotthardt
 */
public class InMemoryEbeanServer implements EbeanServer {
    @Delegate(types=EbeanServer.class)
    private final EbeanServer server;
    private final DdlGenerator ddl;

    public void clear() {
        ddl.runScript(false, ddl.generateDropDdl());
        ddl.runScript(false, ddl.generateCreateDdl());
    }

    public InMemoryEbeanServer() {
        // Load the in-memory database configuration.
        ServerConfig config = new ServerConfig();
        config.setName("h2");
        config.loadFromProperties();

        // Generate and run database creation queries before each test.
        config.setDdlGenerate(true);
        config.setDdlRun(true);

        // Set as default server in case anyone is using the Ebean singleton.
        config.setDefaultServer(true);

        server = EbeanServerFactory.create(config);
        ddl = ((SpiEbeanServer) server).getDdlGenerator();
    }
}