package com.project.sparqlApp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.sparqlApp.model.ClassStructure;

@Repository
public interface ClassStructureRepository extends CrudRepository<ClassStructure, Integer> {
	@Query(value = "SELECT * FROM Class_structure WHERE endpoint_id = :endpoint_id", nativeQuery = true)
	List<ClassStructure> findAllClassGivenEndpoint(@Param(value = "endpoint_id") Integer endpoint_id);
	
	@Query(value = "SELECT * FROM Class_structure WHERE class_uri = :class_uri AND endpoint_id = :endpoint_id", nativeQuery = true)
	List<ClassStructure> findClassByObjectUriAndEndpoint(@Param(value = "class_uri") String class_uri, @Param(value = "endpoint_id") Integer endpoint_id);
}
