package com.project.sparqlApp.service;

import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.project.sparqlApp.model.Endpoint;

@Service
public class CardinalityFinderService {
	@Resource
	CardinalityAsyncServices services;

	public String findAllCardinalityBetweenClasses(Endpoint endpoint) throws Exception {
		Future<String> process1 = services.finderCardinality(endpoint, 0);
		Future<String> process2 = services.finderCardinality(endpoint, 1);
		Future<String> process3 = services.finderCardinality(endpoint, 2);

		// Wait until They are all Done
		// If all are not Done. Pause for next re-check

		while (!(process1.isDone() && process2.isDone() && process3.isDone())) {
			Thread.sleep(8000);
		}

		if (process1.get().equalsIgnoreCase("Ricerca cardinalita non completa")
				|| process2.get().equalsIgnoreCase("Ricerca cardinalita non completa")
				|| process3.get().equalsIgnoreCase("Ricerca cardinalita non completa")) {
			return "Ricerca cardinalita non completa";
		} else {
			return "Done";
		}

	}

}
