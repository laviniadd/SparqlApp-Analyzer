package com.project.sparqlApp.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "predicateStructure")
public class PredicateStructure {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@ManyToOne
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "class_id")
	private ClassStructure classStructure;
	
	private String predicateUri;
	private String status;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ClassStructure getClassStructure() {
		return classStructure;
	}

	public void setClassStructure(ClassStructure classStructure) {
		this.classStructure = classStructure;
	}

	public String getPredicateUri() {
		return predicateUri;
	}

	public void setPredicateUri(String predicateUri) {
		this.predicateUri = predicateUri;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
