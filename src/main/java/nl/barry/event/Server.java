package nl.barry.event;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.filters.CorsFilter;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

public class Server {

	public static void main(String[] args) throws LifecycleException, IOException {

		Tomcat tomcat = new Tomcat();
		Path f = Files.createTempDirectory("tomcat");
		tomcat.setBaseDir(f.toFile().getAbsolutePath());
		tomcat.setPort(8080);
//		tomcat.setHostname("0.0.0.0");
		tomcat.getConnector();

		// root context
		Context ctx = tomcat.addContext("", new File("src/main/resources/static").getAbsolutePath());

		// add default servlet for static html and js files
		tomcat.addServlet("", "default", new DefaultServlet());
		ctx.addServletMappingDecoded("/", "default");
		ctx.addWelcomeFile("index.html");

		// add REST
		ResourceConfig packages = new ResourceConfig().packages("nl.barry.event");
		packages.property(ServerProperties.WADL_FEATURE_DISABLE, "true");
		Wrapper w = Tomcat.addServlet(ctx, "rest", new ServletContainer(packages));
		ctx.addServletMappingDecoded("/api/*", "rest");

		// add CORS
		CorsFilter corsFilter = new CorsFilter();
		FilterDef filter = new FilterDef();
		filter.setFilterName(CorsFilter.class.getSimpleName());
		filter.setFilter(corsFilter);
		ctx.addFilterDef(filter);
		FilterMap filterMap = new FilterMap();
		filterMap.setFilterName(CorsFilter.class.getSimpleName());
		filterMap.addURLPattern("/*");
		ctx.addFilterMap(filterMap);

		// start server and wait
		tomcat.start();
		tomcat.getServer().await();
	}
}
