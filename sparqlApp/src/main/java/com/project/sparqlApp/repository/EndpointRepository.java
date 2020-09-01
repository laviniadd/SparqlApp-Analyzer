package com.project.sparqlApp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.sparqlApp.model.Endpoint;


@Repository
public interface EndpointRepository extends CrudRepository<Endpoint, Integer> {

	@Query(value = "SELECT * FROM Endpoint WHERE endpoint_uri = :endpoint_uri", nativeQuery = true)
	List<Endpoint> findEndpointByUri(@Param(value = "endpoint_uri") String endpoint_uri);
}