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
public class CardinalityAsyncServices {

	Logger log = LoggerFactory.getLogger(this.getClass().getName());
	String processInfo = "Done";
	Boolean success = false;
	ResultSet results = null;

	@Autowired
	private RelationsRepository relationsRepository;

	@Autowired
	private ClassStructureRepository classStructureRepository;

	@Autowired
	private PredicateStructureRepository predicateStructureRepository;

	@Async
	public Future<String> finderCardinality(Endpoint endpoint, int threadNumber) {
		log.info("###DENTRO ANALIZZARE cardinalita THREAD: " + Thread.currentThread().getId() + " - " + threadNumber);
		int numberOfThread = 3;

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
			if (clax.getStatus().equalsIgnoreCase("ANALIZZARE OGGETTI")) {

				List<PredicateStructure> allPredicateOfAClass = predicateStructureRepository
						.findAllPredicateGivenClassId(clax.getId());

				for (PredicateStructure pred : allPredicateOfAClass) {
					if (pred.getStatus().equalsIgnoreCase("ANALIZZARE OGGETTI")) {

						List<Relations> relationsOfClaxAndPredicate = relationsRepository
								.findAllObjectGivenClassAndPredicateAndEndpoint(endpoint.getId(), clax.getId(),
										pred.getId());

						for (Relations rel : relationsOfClaxAndPredicate) {

							if (rel.getResourceUri().equalsIgnoreCase("Integer")
									|| rel.getResourceUri().equalsIgnoreCase("String")) {

							} else {

								if (rel.getPredicateStructure().getPredicateUri()
										.equalsIgnoreCase("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
										&& rel.getClassStructure().getClassUri().equals(rel.getResourceUri())) {

									Optional<Relations> relationInDb = relationsRepository.findById(rel.getId());
									relationInDb.get().setCardinalityCO("uno");
									relationInDb.get().setCardinalityOC("uno");
									relationsRepository.save(relationInDb.get());

								} else {

									success = false;
									int maxRepNumber = 2;

									ParameterizedSparqlString qs = new ParameterizedSparqlString(
											"" + "SELECT ?s (count(*) as ?conto) WHERE { ?s  a  <"
													+ rel.getClassStructure().getClassUri() + ">. ?s  <"
													+ rel.getPredicateStructure().getPredicateUri() + ">  ?o. ?o a <"
													+ rel.getResourceUri() + "> } GROUP BY ?s");

									while (success == false && maxRepNumber > 0) {

										try (QueryEngineHTTP exec = QueryExecutionFactory
												.createServiceRequest(endpoint.getEndpointUri(), qs.asQuery())) {
											exec.setTimeout(180000, TimeUnit.MILLISECONDS, 180000,
													TimeUnit.MILLISECONDS);
											//System.out.println("\n=== Query co " + threadNumber + "  ===\n" + qs.asQuery());
											results = exec.execSelect();
											success = true;
											List<Integer> counts = new ArrayList<>();

											while (results.hasNext()) {
												String toSplit = results.next().get("conto").toString();
												String countNumber = toSplit
														.replace("^^http://www.w3.org/2001/XMLSchema#decimal", "");
												counts.add(Integer.parseInt(countNumber));
											}

											if (!counts.isEmpty()) {

												Optional<Relations> relationAddCardinality = relationsRepository
														.findById(rel.getId());

												if (counts.stream().anyMatch(i -> i > 1)) {

													relationAddCardinality.get().setCardinalityCO("molti");
													relationsRepository.save(relationAddCardinality.get());
													findCardinalityOC(rel, endpoint);

												} else {

													relationAddCardinality.get().setCardinalityCO("uno");
													relationsRepository.save(relationAddCardinality.get());
													findCardinalityOC(rel, endpoint);
												}

											}
										} catch (Exception e) {
											maxRepNumber--;
										}
									}
									if (maxRepNumber == 0) {
										processInfo = "Ricerca cardinalita non completa";
									}
								}
							}

						}
						if (!processInfo.equalsIgnoreCase("Ricerca cardinalita non completa")) {

							Optional<ClassStructure> classInDb = classStructureRepository.findById(clax.getId());

							classInDb.get().setStatus("ANALISI COMPLETATA");
							classStructureRepository.save(classInDb.get());

							Optional<PredicateStructure> predInDb = predicateStructureRepository.findById(pred.getId());
							predInDb.get().setStatus("ANALISI COMPLETATA");
							predicateStructureRepository.save(predInDb.get());
						}
					}
				}
			}
		}
		return new AsyncResult<>(processInfo);

	}

	private void findCardinalityOC(Relations rel, Endpoint endpoint) {

		success = false;
		int maxRepNumber = 2;

		ParameterizedSparqlString qs = new ParameterizedSparqlString(
				"" + "SELECT ?o (count(*) as ?conto) WHERE { ?s  a  <" + rel.getClassStructure().getClassUri()
						+ ">. ?s  <" + rel.getPredicateStructure().getPredicateUri() + ">  ?o. ?o a <"
						+ rel.getResourceUri() + "> } GROUP BY ?o");

		while (success == false && maxRepNumber > 0) {
			try (QueryEngineHTTP exec = QueryExecutionFactory.createServiceRequest(endpoint.getEndpointUri(),
					qs.asQuery())) {
				exec.setTimeout(180000, TimeUnit.MILLISECONDS, 180000, TimeUnit.MILLISECONDS);
				//System.out.println("\n=== Query card oc  ===\n" + qs.asQuery());
				results = exec.execSelect();
				success = true;

				List<Integer> counts = new ArrayList<>();

				while (results.hasNext()) {
					String toSplit = results.next().get("conto").toString();
					String countNumber = toSplit.replace("^^http://www.w3.org/2001/XMLSchema#decimal", "");
					counts.add(Integer.parseInt(countNumber));
				}

				if (!counts.isEmpty()) {

					Optional<Relations> relationAddCardinality = relationsRepository.findById(rel.getId());

					if (counts.stream().anyMatch(i -> i > 1)) {
						relationAddCardinality.get().setCardinalityOC("molti");
						relationsRepository.save(relationAddCardinality.get());
					} else {
						relationAddCardinality.get().setCardinalityOC("uno");
						relationsRepository.save(relationAddCardinality.get());
					}
				}

			} catch (Exception e) {
				maxRepNumber--;
			}
		}
		if (maxRepNumber == 0) {
			processInfo = "Ricerca cardinalita non completa";
		}
	}
}
