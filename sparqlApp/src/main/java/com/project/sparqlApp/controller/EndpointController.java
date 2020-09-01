package com.project.sparqlApp.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.project.sparqlApp.model.ClassStructure;
import com.project.sparqlApp.model.Endpoint;
import com.project.sparqlApp.model.Relations;
import com.project.sparqlApp.repository.ClassStructureRepository;
import com.project.sparqlApp.repository.EndpointRepository;
import com.project.sparqlApp.repository.RelationsRepository;
import com.project.sparqlApp.service.ClassFinderService;
import com.project.sparqlApp.service.ObjectFinderService;
import com.project.sparqlApp.service.PredicateFinderService;
import com.project.sparqlApp.service.CardinalityFinderService;

@Controller
@RequestMapping(path = "/app")
public class EndpointController {
	@Autowired
	private EndpointRepository endpointRepository;

	@Autowired
	private CardinalityFinderService cardinalityFinderService;

	@Autowired
	private ObjectFinderService objectFinderService;

	@Autowired
	private ClassFinderService classFinderService;

	@Autowired
	private PredicateFinderService predicateFinderService;

	@Autowired
	private RelationsRepository relationsRepository;

	@Autowired
	private ClassStructureRepository classRepository;

	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(path = "/add_endpoint")
	public @ResponseBody String addNewEndpoint(@ModelAttribute("endpointUri") String endpointUri) {

		final URL url;
	    try {
	    	url = new URL(endpointUri);
	    } catch (Exception e1) {
	        return "Endpoint not valid";
	    }
		// System.out.println("endpoint ricevuto: " + endpointUri);
		

		List<Endpoint> endpointAlreadySaveInDb = endpointRepository.findEndpointByUri(endpointUri.trim());

		Endpoint endpoint = new Endpoint();

		if (endpointAlreadySaveInDb.isEmpty()) {
			endpoint.setEndpointUri(endpointUri.trim());
			endpoint.setStatus("DA ANALIZZARE");
			endpointRepository.save(endpoint);
			return "Endpoint Salvato";
		} else {
			for (Endpoint endpoints : endpointAlreadySaveInDb) {
				endpoint = endpoints;
			}
			return "Endpoint gia salvato";
		}
	}

	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(path = "/start")
	public @ResponseBody String analyze(@ModelAttribute("endpointUri") String endpointUri) {

		final URL url;
	    try {
	    	url = new URL(endpointUri);
	    } catch (Exception e1) {
	        return "Endpoint not valid";
	    }
		// System.out.println("analisi endpoint iniziata: " + endpointUri);

		List<Endpoint> endpointAlreadySaveInDb = endpointRepository.findEndpointByUri(endpointUri.trim());

		Endpoint endpoint = new Endpoint();

		if (endpointAlreadySaveInDb.isEmpty()) {
			endpoint.setEndpointUri(endpointUri.trim());
			endpoint.setStatus("DA ANALIZZARE");
			endpointRepository.save(endpoint);
		} else {
			for (Endpoint endpoints : endpointAlreadySaveInDb) {
				endpoint = endpoints;
			}
		}

		if (endpoint.getStatus().equalsIgnoreCase("DA ANALIZZARE")) {
			String statusTrovaClassi = classFinderService.findAndSaveAllClassesGivenAnEndpoint(endpoint);
			if (statusTrovaClassi == "Endpoint non disponibile") {
				return statusTrovaClassi;
			} else {
				// AGGIORNO LO STATO DELL'ENDPOINT DOPO CHE HO TROVATO TUTTE LE CLASSI
				Optional<Endpoint> endpointToBeUpdated = endpointRepository.findById(endpoint.getId());
				endpointToBeUpdated.get().setStatus("ANALIZZARE CLASSI");
				endpointRepository.save(endpointToBeUpdated.get());
			}
		}

		if (endpoint.getStatus().equalsIgnoreCase("ANALIZZARE CLASSI")) {
			try {
				String statusPredicateRetrieving = predicateFinderService.findAllPredicateGivenAClass(endpoint);
				if (statusPredicateRetrieving.equalsIgnoreCase("Ricerca predicati non completa")) {
					return statusPredicateRetrieving;
				} else {
					Optional<Endpoint> endpointToBeUpdated = endpointRepository.findById(endpoint.getId());
					endpointToBeUpdated.get().setStatus("ANALIZZARE PREDICATI");
					endpointRepository.save(endpointToBeUpdated.get());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		if (endpoint.getStatus().equalsIgnoreCase("ANALIZZARE PREDICATI")) {
			try {
				String statusObjectRetrieving = objectFinderService.findAllObjectsGivenAPredicate(endpoint);
				if (statusObjectRetrieving.equalsIgnoreCase("Ricerca oggetti non completa")) {
					return statusObjectRetrieving;
				} else {

					Optional<Endpoint> endpointToBeUpdated = endpointRepository.findById(endpoint.getId());
					endpointToBeUpdated.get().setStatus("ANALIZZARE OGGETTI");
					endpointRepository.save(endpointToBeUpdated.get());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		if (endpoint.getStatus().equalsIgnoreCase("ANALIZZARE OGGETTI")) {
			try {
				String statusCardinalityRetrieving = cardinalityFinderService
						.findAllCardinalityBetweenClasses(endpoint);
				if (statusCardinalityRetrieving.equalsIgnoreCase("Ricerca cardinalita non completa")) {
					return statusCardinalityRetrieving;
				} else {
					Optional<Endpoint> endpointToBeUpdated = endpointRepository.findById(endpoint.getId());
					endpointToBeUpdated.get().setStatus("ANALISI COMPLETATA");
					endpointRepository.save(endpointToBeUpdated.get());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return "Done";

	}

	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(path = "/all_endpoints")
	public @ResponseBody Iterable<Endpoint> getAllEndpoints() {

		List<Endpoint> listEndpointCompleted = new ArrayList<Endpoint>();
		List<Endpoint> listEndpointNotCompleted = new ArrayList<Endpoint>();

		Iterable<Endpoint> allEndpoints = endpointRepository.findAll();
		for (Endpoint e : allEndpoints) {
			if (e.getStatus().equalsIgnoreCase("ANALISI COMPLETATA")) {
				listEndpointCompleted.add(e);
			} else {
				listEndpointNotCompleted.add(e);
			}
		}
		for (Endpoint e : listEndpointNotCompleted) {
			listEndpointCompleted.add(e);
		}
		return listEndpointCompleted;
	}

	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(path = "/endpoint", produces = MediaType.APPLICATION_JSON_VALUE)
	// public @ResponseBody String getGraphGivenEndpointId(@RequestParam(value =
	// "id") int id) {
	public @ResponseBody String getGraphGivenEndpointId(@RequestParam(value = "id") String id_string) {
		int id;
		try {
			id = Integer.parseInt(id_string);
		} catch (Exception e) {
			return "Not a valid id";
		}
		Optional<Endpoint> endpointExists = endpointRepository.findById(id);

		if (endpointExists.get().getStatus().equalsIgnoreCase("ANALISI COMPLETATA")) {

			List<String> classesUriInDb = relationsRepository.findAllDifferentObjects(id);

			Set<ClassStructure> classesObj = new HashSet<ClassStructure>();
			for (String clax : classesUriInDb) {
				if (!clax.equalsIgnoreCase("String") && !clax.equalsIgnoreCase("Integer")) {

					List<ClassStructure> cl = classRepository.findClassByObjectUriAndEndpoint(clax,
							endpointExists.get().getId());

					for (ClassStructure c : cl) {
						classesObj.add(c);
					}
				}
			}

			Set<Integer> classesId = relationsRepository.findAllClassesId(id);
			Set<ClassStructure> classesClass = new HashSet<ClassStructure>();
			for (Integer idclax : classesId) {
				classesClass.add(classRepository.findById(idclax).get());
			}

			Set<ClassStructure> allClasses = new HashSet<ClassStructure>();
			allClasses.addAll(classesObj);
			allClasses.addAll(classesClass);

			JSONArray nodes = new JSONArray();

			for (ClassStructure clax : allClasses) {

				JSONObject nodeDetails = new JSONObject();
				nodeDetails.put("id", clax.getId());

				String[] splitString = clax.getClassUri().split("/");
				String cleanIndex = splitString[splitString.length - 1];
				nodeDetails.put("name", cleanIndex);

				nodes.put(nodeDetails);
			}

			List<Relations> relations = relationsRepository.findAllRelationsGivenEndpointId(id);
			List<Relations> relationsForGraph = new ArrayList<Relations>();

			for (Relations rel : relations) {
				if (!rel.getResourceUri().equalsIgnoreCase("String")
						&& !rel.getResourceUri().equalsIgnoreCase("Integer")) {
					relationsForGraph.add(rel);
				}
			}

			JSONArray edges = new JSONArray();

			JSONObject graph = new JSONObject();

			for (Relations rel : relationsForGraph) {

				String[] splitString = rel.getPredicateStructure().getPredicateUri().split("/");
				String cleanedPredicate = splitString[splitString.length - 1];

				JSONObject edgeDetails = new JSONObject();

				List<ClassStructure> classObjs = classRepository.findClassByObjectUriAndEndpoint(rel.getResourceUri(),
						endpointExists.get().getId());
				for (ClassStructure classObj : classObjs) {
					edgeDetails.put("source", rel.getClassStructure().getId());
					edgeDetails.put("target", classObj.getId());
					edgeDetails.put("type", cleanedPredicate);
					edgeDetails.put("cardCO", rel.getCardinalityCO());
					edgeDetails.put("cardOC", rel.getCardinalityOC());
					edges.put(edgeDetails);
				}

			}

			graph.put("nodes", nodes);
			graph.put("links", edges);

			return graph.toString();
		} else {
			JSONObject graph = new JSONObject();
			return graph.toString();
		}
	}
}