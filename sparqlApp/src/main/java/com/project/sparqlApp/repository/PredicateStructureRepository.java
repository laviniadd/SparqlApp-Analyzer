package com.project.sparqlApp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.project.sparqlApp.model.PredicateStructure;

public interface PredicateStructureRepository extends CrudRepository<PredicateStructure, Integer> {
	@Query(value = "SELECT * FROM Predicate_structure WHERE class_id = :class_id AND status = :status", nativeQuery = true)
	List<PredicateStructure> findAllPredicateToBeAnalyzeGivenClassIdAndStatus(Integer class_id, String status);
	
	@Query(value = "SELECT * FROM Predicate_structure WHERE class_id = :class_id", nativeQuery = true)
	List<PredicateStructure> findAllPredicateGivenClassId(@Param(value = "class_id") Integer class_id);

}
