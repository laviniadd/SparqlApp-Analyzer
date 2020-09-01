package com.project.sparqlApp.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.project.sparqlApp.model.Relations;

public interface RelationsRepository extends CrudRepository<Relations, Integer> {
	@Query(value = "SELECT * FROM Relations WHERE resource_uri = :resource_uri", nativeQuery = true)
	Relations findByUri(@Param(value = "resource_uri") String resource_uri);

	@Query(value = "SELECT * FROM Relations WHERE endpoint_id = :endpoint_id AND NOT(resource_uri = 'String' OR resource_uri = 'Integer')", nativeQuery = true)
	List<Relations> findAllObjectNotIntOrString(@Param(value = "endpoint_id") int endpointId);

	@Query(value = "SELECT * FROM Relations WHERE endpoint_id = :endpoint_id AND class_id = :class_id AND predicate_id = :predicate_id", nativeQuery = true)
	List<Relations> findAllObjectGivenClassAndPredicateAndEndpoint(@Param(value = "endpoint_id") int endpointId, @Param(value = "class_id") int class_id, @Param(value = "predicate_id") int predicate_id);

	@Query(value = "SELECT * FROM Relations WHERE endpoint_id = :endpoint_id", nativeQuery = true)
	List<Relations> findAllRelationsGivenEndpointId(@Param(value = "endpoint_id") int endpointId);

	@Query(value = "SELECT DISTINCT resource_uri FROM Relations WHERE endpoint_id = :endpoint_id", nativeQuery = true)
	List<String> findAllDifferentObjects(@Param(value = "endpoint_id") int endpointId);
	
	@Query(value = "SELECT DISTINCT class_id FROM Relations WHERE endpoint_id = :endpoint_id", nativeQuery = true)
	Set<Integer> findAllClassesId(@Param(value = "endpoint_id") int endpointId);

	@Query(value = "SELECT * FROM Relations WHERE endpoint_id = :endpoint_id AND resource_uri= resource_uri AND class_id = class_id AND NOT(resource_uri = 'String' OR resource_uri = 'Integer')", nativeQuery = true)
	List<Relations> findAllRelationsWithSameSorceAndTargetGivenEndpointId(@Param(value = "endpoint_id") int endpointId);
}
