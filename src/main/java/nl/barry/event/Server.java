package nl.barry.event;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

public class Server {

	public static void main(String[] args) throws LifecycleException {

		Tomcat tomcat = new Tomcat();
		tomcat.setPort(8080);
//		tomcat.setHostname("0.0.0.0");
		tomcat.getConnector();
		Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());

		ResourceConfig packages = new ResourceConfig().packages("nl.barry.event");
		packages.property(ServerProperties.WADL_FEATURE_DISABLE, "true");
		Wrapper w = Tomcat.addServlet(ctx, "rest", new ServletContainer(packages));
		ctx.addServletMappingDecoded("/*", "rest");
		tomcat.start();
		tomcat.getServer().await();
	}
}
