package com.project.sparqlApp.service;

import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.sparqlApp.model.Endpoint;
import com.project.sparqlApp.repository.ClassStructureRepository;
import com.project.sparqlApp.repository.EndpointRepository;

@Service
public class PredicateFinderService {

	@Autowired
	ClassStructureRepository classStructureRepository;

	@Autowired
	EndpointRepository endRep;

	int numberOfThreads;

	@Resource
	AsyncServices services;

	Logger log = LoggerFactory.getLogger(this.getClass().getName());

	public String findAllPredicateGivenAClass(Endpoint endpoint) throws Exception {

		Future<String> process1 = services.processFindAllPredicateGivenAClass(endpoint, 0);
		Future<String> process2 = services.processFindAllPredicateGivenAClass(endpoint, 1);
		Future<String> process3 = services.processFindAllPredicateGivenAClass(endpoint, 2);

		// Wait until They are all Done
		// If all are not Done. Pause for next re-check

		while (!(process1.isDone() && process2.isDone() && process3.isDone())) {
			Thread.sleep(8000);
		}

		log.info("All Processes are DONE!");

		if (process1.get().equalsIgnoreCase("Ricerca predicati non completa")
				|| process2.get().equalsIgnoreCase("Ricerca predicati non completa")
				|| process3.get().equalsIgnoreCase("Ricerca predicati non completa")) {
			return "Ricerca predicati non completa";
		} else {
			return "Done";
		}

	}
}
