package com.project.sparqlApp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.project.sparqlApp.model.ClassStructure;
import com.project.sparqlApp.model.Endpoint;
import com.project.sparqlApp.repository.ClassStructureRepository;

@Service
public class ClassFinderService {
	@Autowired
	private ClassStructureRepository classStructureRepository;

	public String findAndSaveAllClassesGivenAnEndpoint(Endpoint endpoint) {
		System.out.println("DENTRO trovare classi");
		Boolean success = false;
		int maxRepNumber = 2;
		ResultSet results = null;

		ParameterizedSparqlString qs = new ParameterizedSparqlString("" + "select distinct ?class where {"
				+ "?s a ?class." + "FILTER ( !strstarts(str(?class), \"http://www.w3.org/2002/07/owl\") ).\r\n"
				+ "FILTER ( !strstarts(str(?class), \"http://www.w3.org/2000/01/rdf-schema\") )." + "}");
		/*
		 * + "FILTER ( !strstarts(str(?class), \"http://dbpedia.org/\") )." +
		 * "FILTER ( !strstarts(str(?class), \"http://www.w3.org/\") )." +
		 * "FILTER ( !strstarts(str(?class), \"http://rdfs.org/ns/\") )." +
		 * "FILTER ( !strstarts(str(?class), \"http://schema.org/\") )." +
		 * "FILTER ( !strstarts(str(?class), \"http://xmlns.com/foaf/0.1/\") )."
		 */
		// System.out.println("\n=== Query find classes ===\n" + qs.asQuery());

		while (success == false && maxRepNumber > 0)

		{
			try (QueryEngineHTTP exec = QueryExecutionFactory.createServiceRequest(endpoint.getEndpointUri(),
					qs.asQuery())) {
				exec.setTimeout(300000, TimeUnit.MILLISECONDS, 300000, TimeUnit.MILLISECONDS);
				results = exec.execSelect();
				success = true;

				List<String> classes = new ArrayList<>();

				while (results.hasNext()) {
					classes.add(results.next().get("class").toString());
				}

				if (!classes.isEmpty()) {
					for (String classesUri : classes) {
						ClassStructure classStructure = new ClassStructure(classesUri, "DA ANALIZZARE", endpoint);
						classStructureRepository.save(classStructure);
					}
					return "Completed";
				} else {
					return "Endpoint non disponibile";
				}
			} catch (Exception e) {
				maxRepNumber--;
			}
		}
		if (maxRepNumber == 0) {
			return "Endpoint non disponibile";
		}

		return "Completed";
	}
}
