package com.project.sparqlApp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import com.project.sparqlApp.model.Relations;
import com.project.sparqlApp.repository.ClassStructureRepository;
import com.project.sparqlApp.repository.PredicateStructureRepository;
import com.project.sparqlApp.repository.RelationsRepository;

@Service
public class ObjectAsyncServices {
	Logger log = LoggerFactory.getLogger(this.getClass().getName());
	int numberOfThread = 3;
	Boolean success = false;
	ResultSet results = null;
	String processInfo = "Done";
	List<String> objects = new ArrayList<>();

	@Autowired
	private ClassStructureRepository classStructureRepository;
	@Autowired
	private PredicateStructureRepository predicateStructureRepository;
	@Autowired
	private RelationsRepository relationsRepository;

	@Async
	public Future<String> process(Endpoint endpoint, int threadNumber) throws InterruptedException {
		log.info("###DENTRO ANALIZZARE PREDICATI object service THREAD: " + Thread.currentThread().getId() + " - "
				+ threadNumber);

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

		for (ClassStructure clax : classesOfThreadToAnalyze) {
			if (clax.getStatus().equalsIgnoreCase("ANALIZZARE PREDICATI")) {

				List<PredicateStructure> allPredicateOfAClass = predicateStructureRepository
						.findAllPredicateToBeAnalyzeGivenClassIdAndStatus(clax.getId(), "DA ANALIZZARE");

				for (PredicateStructure predicateToBeAnalize : allPredicateOfAClass) {

					//System.out.println("classe e predicato di cui trovare gli oggetti: "+clax.getClassUri() + " - " + predicateToBeAnalize.getPredicateUri());

					success = false;
					int maxRepNumber = 2;

					ParameterizedSparqlString qs = new ParameterizedSparqlString(
							"" + "SELECT DISTINCT ?otype WHERE { ?s  a  <" + clax.getClassUri() + ">. ?s  <"
									+ predicateToBeAnalize.getPredicateUri() + ">  ?o. ?o a ?otype. }");

					//System.out.println("\n=== Query " + threadNumber + "  ===\n" + qs.asQuery());

					while (success == false && maxRepNumber > 0) {

						try (QueryEngineHTTP exec = QueryExecutionFactory
								.createServiceRequest(endpoint.getEndpointUri(), qs.asQuery())) {
							exec.setTimeout(180000, TimeUnit.MILLISECONDS, 180000, TimeUnit.MILLISECONDS);
							results = exec.execSelect();
							success = true;

							while (results.hasNext()) {
								objects.add(results.next().get("otype").toString());
							}
							if (objects.isEmpty()) {
								findAllIndividuals(clax, predicateToBeAnalize, endpoint);
							} else {

								for (String obj : objects) {

									List<ClassStructure> classesInDb = classStructureRepository
											.findClassByObjectUriAndEndpoint(obj, endpoint.getId());


									if (classesInDb.isEmpty()) {
										try {
											Integer.parseInt(obj);
											obj = "Integer";
											Relations relation = new Relations();
											relation.setResourceUri(obj);
											relation.setClassStructure(clax);
											relation.setPredicateStructure(predicateToBeAnalize);
											relation.setEndpoint(endpoint);
											relationsRepository.save(relation);

										} catch (NumberFormatException e) {
											obj = "String";
											Relations relation = new Relations();
											relation.setResourceUri(obj);
											relation.setClassStructure(clax);
											relation.setPredicateStructure(predicateToBeAnalize);
											relation.setEndpoint(endpoint);
											relationsRepository.save(relation);
										}
									} else {
										Relations rel = new Relations();
										rel.setResourceUri(obj);
										rel.setClassStructure(clax);
										rel.setPredicateStructure(predicateToBeAnalize);
										rel.setEndpoint(endpoint);
										relationsRepository.save(rel);
									}
								}

								Optional<ClassStructure> classStatusToBeUpdated = classStructureRepository
										.findById(clax.getId());
								classStatusToBeUpdated.get().setStatus("ANALIZZARE OGGETTI");
								classStructureRepository.save(classStatusToBeUpdated.get());

								Optional<PredicateStructure> predicateStatusToBeUpdated = predicateStructureRepository
										.findById(predicateToBeAnalize.getId());
								predicateStatusToBeUpdated.get().setStatus("ANALIZZARE OGGETTI");
								predicateStructureRepository.save(predicateStatusToBeUpdated.get());
							}
						} catch (Exception e) {
							maxRepNumber--;
							//System.out.println("catch 1: " + threadNumber + maxRepNumber);
						}
					}

					if (maxRepNumber == 0) {
						processInfo = "Ricerca oggetti non completa";
					}
				}
			}
		}
		return new AsyncResult<>(processInfo);
	}

	public void findAllIndividuals(ClassStructure clax, PredicateStructure predicateToBeAnalize, Endpoint endpoint) {
		List<String> objectsLit = new ArrayList<>();

		success = false;
		int maxRepNumber = 2;

		ParameterizedSparqlString qe = new ParameterizedSparqlString("" + "SELECT DISTINCT ?o WHERE { ?s  a  <"
				+ clax.getClassUri() + ">. ?s  <" + predicateToBeAnalize.getPredicateUri() + ">  ?o. }");
		
		//System.out.println("\n=== Query qe  ===\n" + qe.asQuery());

		while (success == false && maxRepNumber > 0) {
			try (QueryEngineHTTP execute = QueryExecutionFactory.createServiceRequest(endpoint.getEndpointUri(),
					qe.asQuery())) {
				execute.setTimeout(180000, TimeUnit.MILLISECONDS, 180000, TimeUnit.MILLISECONDS);
				results = execute.execSelect();
				success = true;

				while (results.hasNext()) {
					objectsLit.add(results.next().get("o").toString());
				}

				if (!objectsLit.isEmpty()) {

					for (String obj : objectsLit) {

						List<ClassStructure> classesInDb = classStructureRepository.findClassByObjectUriAndEndpoint(obj, endpoint.getId());

						if (classesInDb.isEmpty()) {
							try {
								Integer.parseInt(obj);
								obj = "Integer";
								Relations relation = new Relations();
								relation.setResourceUri(obj);
								relation.setClassStructure(clax);
								relation.setPredicateStructure(predicateToBeAnalize);
								relation.setEndpoint(endpoint);
								relationsRepository.save(relation);

							} catch (NumberFormatException e) {
								obj = "String";
								Relations relation = new Relations();
								relation.setResourceUri(obj);
								relation.setClassStructure(clax);
								relation.setPredicateStructure(predicateToBeAnalize);
								relation.setEndpoint(endpoint);
								relationsRepository.save(relation);
							}
						} else {
							Relations rel = new Relations();
							rel.setResourceUri(obj);
							rel.setClassStructure(clax);
							rel.setPredicateStructure(predicateToBeAnalize);
							rel.setEndpoint(endpoint);
							relationsRepository.save(rel);
						}
					}
					Optional<ClassStructure> classStatusToBeUpdated = classStructureRepository.findById(clax.getId());
					classStatusToBeUpdated.get().setStatus("ANALIZZARE OGGETTI");
					classStructureRepository.save(classStatusToBeUpdated.get());

					Optional<PredicateStructure> predicateStatusToBeUpdated = predicateStructureRepository
							.findById(predicateToBeAnalize.getId());
					predicateStatusToBeUpdated.get().setStatus("ANALIZZARE OGGETTI");
					predicateStructureRepository.save(predicateStatusToBeUpdated.get());
				}
			} catch (Exception e) {
				maxRepNumber--;
				//System.out.println("catch 2 ");
			}
		}

		if (maxRepNumber == 0) {
			processInfo = "Ricerca oggetti non completa";
		}
	}
}
