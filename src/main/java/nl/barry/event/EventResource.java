package nl.barry.event;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.exec.http.UpdateExecutionHTTP;
import org.apache.jena.sparql.graph.PrefixMappingMem;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

@Path("/")
public class EventResource {

	private static final String DEFAULT_TEMPLATES_FILE = "/defaultTemplates.json";

	private static Logger LOG = LoggerFactory.getLogger(EventResource.class);

	private Map<String, Element> templatePatterns = null;

	public EventResource() throws ParseException {

		this.templatePatterns = new HashMap<>();

		loadDefaultTemplates();

		LOG.debug("Constructed EventResource and loaded '{}' default templates.", this.templatePatterns.size());
	}

	private void loadDefaultTemplates() throws ParseException {
		InputStream is = EventResource.class.getResourceAsStream(DEFAULT_TEMPLATES_FILE);
		JsonReader builder = Json.createReader(new InputStreamReader(is));
		JsonObject templates = builder.readObject();

		for (String key : templates.keySet()) {

			String pattern = templates.getString(key);
			ElementPathBlock block = parseGraphPattern(new PrefixMappingMem(), pattern);
			this.templatePatterns.put(key, block);
		}
	}

	@GET
	@Path("/template")
	@Produces("application/json; charset=UTF-8")
	public Response getTemplate() {
		LOG.debug("getTemplate called");
		JsonObject o = createJson(this.templatePatterns);
		StringWriter sw = new StringWriter();
		JsonWriter jw = Json.createWriter(sw);
		jw.writeObject(o);
		return Response.ok().entity(sw.toString()).build();
	}

	@POST
	@Path("/event/{template}")
	public Response postEvent(@PathParam("template") String aTemplate, String someTriples) {
		LOG.debug("postEvent called with template '{}'", aTemplate);
		String sparqlEndpoint = ConfigProvider.getConfig().getValue(EventConfig.CONF_KEY_EVENT_SPARQL_ENDPOINT,
				String.class);

		ResponseBuilder rb;

		if (!aTemplate.isBlank() && this.templatePatterns.containsKey(aTemplate)) {
			UpdateRequest ur = UpdateFactory.create("INSERT DATA {" + someTriples + "}");
			UpdateExecutionHTTP ue = (UpdateExecutionHTTP) UpdateExecutionFactory.createRemote(ur, sparqlEndpoint);
			ue.execute();

			rb = Response.accepted();

		} else {

			String errorMessage = this.format(
					"Both an existing template {} and a set of triples should be supplied, but weren't. Received template '{}' and triples '{}'.",
					this.templatePatterns.keySet().toString(), aTemplate, someTriples);

			LOG.warn(errorMessage);
			rb = Response.status(Status.NOT_FOUND).entity(errorMessage);
		}

		return rb.build();
	}

	@GET
	@Path("/health")
	public Response getHealthWorld() {
		LOG.debug("getHelloWorld called");
		return Response.ok("Healty World!").build();
	}

	private ElementPathBlock parseGraphPattern(PrefixMapping prefixes, String pattern) throws ParseException {

		LOG.trace("prefixes: {}- pattern: {}", prefixes, pattern);

		ElementGroup eg;
		String queryString = "SELECT * {" + pattern + "}";

		Query query = new Query();
		query.setPrefixMapping(prefixes);
		QueryFactory.parse(query, queryString, null, Syntax.defaultQuerySyntax);

		Element e = query.getQueryPattern();
		LOG.trace("parsed knowledge: {}", e);
		eg = (ElementGroup) e;
		Element last = eg.getLast();

		if (!(last instanceof ElementPathBlock)) {
			LOG.error("This knowledge '{}' should be parseable to a ElementPathBlock", pattern);
			throw new ParseException(
					"The knowledge should be parseable to a ARQ ElementPathBlock (i.e. a BasicGraphPattern in the SPARQL syntax specification)");
		}
		return (ElementPathBlock) eg.getLast();

	}

	private JsonObject createJson(Map<String, Element> someTemplatePatterns) {

		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
		for (Map.Entry<String, Element> entry : someTemplatePatterns.entrySet()) {

			String patternString = entry.getValue().toString();
			objectBuilder.add(entry.getKey(), patternString);
		}

		return objectBuilder.build();
	}

	public String format(String aMsg, String... someParameters) {
		return MessageFormatter.arrayFormat(aMsg, someParameters).getMessage();
	}

}
