package com.project.sparqlApp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.project.sparqlApp.model.ClassStructure;
import com.project.sparqlApp.model.Endpoint;
import com.project.sparqlApp.model.PredicateStructure;
import com.project.sparqlApp.repository.ClassStructureRepository;
import com.project.sparqlApp.repository.PredicateStructureRepository;

@Service
public class AsyncServices {
	Logger log = LoggerFactory.getLogger(this.getClass().getName());
	Boolean success = false;
	ResultSet results = null;
	int numberOfThread = 3;
	String processInfo = "Done";
	Timer timer = new Timer();
	@Autowired
	private ClassStructureRepository classStructureRepository;

	@Autowired
	private PredicateStructureRepository predicateStructureRepository;

	@Async
	public Future<String> processFindAllPredicateGivenAClass(Endpoint endpoint, int threadNumber)
			throws InterruptedException, TimeoutException {
		log.info("###DENTRO THREAD find all predicate: " + Thread.currentThread().getId() + " - " + threadNumber);

		// TROVO TUTTE LE CLASSI DATO UN ENDPOINT
		List<ClassStructure> classesGivenEndpoint = classStructureRepository
				.findAllClassGivenEndpoint(endpoint.getId());

		// SMISTO LE CLASSI A SECONDA DEL threadNumber
		List<ClassStructure> classesOfThreadToAnalyze = new ArrayList<ClassStructure>();
		for (int i = 0; i < classesGivenEndpoint.size(); i++) {
			if (threadNumber == i % numberOfThread) {
				classesOfThreadToAnalyze.add(classesGivenEndpoint.get(i));
			}
		}

		// ANALIZZO SOLO LE CLASSI CHE HANNO COME STATO "DA ANALIZZARE"
		for (ClassStructure clax : classesOfThreadToAnalyze) {

			success = false;
			int maxRepNumber = 2;

			if (clax.getStatus().equalsIgnoreCase("DA ANALIZZARE")) {

				ParameterizedSparqlString qs = new ParameterizedSparqlString(
						"" + "SELECT DISTINCT ?p WHERE { ?s  a  <" + clax.getClassUri() + ">. ?s  ?p  ?o.  }");
				
				//System.out.println("\n=== Query " + threadNumber + "  ===\n" + qs.asQuery());

				while (success == false && maxRepNumber > 0) {
					try (QueryEngineHTTP exec = QueryExecutionFactory.createServiceRequest(endpoint.getEndpointUri(),
							qs.asQuery())) {
						exec.setTimeout(180000, TimeUnit.MILLISECONDS, 180000, TimeUnit.MILLISECONDS);
						results = exec.execSelect();
						success = true;
				
						
						List<String> predicates = new ArrayList<>();
						while (results.hasNext()) {

							predicates.add(results.next().get("p").toString());

						}
						if (!predicates.isEmpty()) {
							for (String predicateUri : predicates) {
								PredicateStructure predicate = new PredicateStructure();
								predicate.setPredicateUri(predicateUri);
								predicate.setStatus("DA ANALIZZARE");
								predicate.setClassStructure(clax);
								predicateStructureRepository.save(predicate);
							}
							Optional<ClassStructure> classStatusToBeUpdated = classStructureRepository
									.findById(clax.getId());
							classStatusToBeUpdated.get().setStatus("ANALIZZARE PREDICATI");
							classStructureRepository.save(classStatusToBeUpdated.get());
						}
					} catch (Exception e) {
						maxRepNumber--;
					}
				}

				if (maxRepNumber == 0) {
					processInfo = "Ricerca predicati non completa";
				}
			}
		}
		return new AsyncResult<>(processInfo);
	}
}
