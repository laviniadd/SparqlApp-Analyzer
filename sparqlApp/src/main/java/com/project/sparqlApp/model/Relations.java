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
@Table(name = "relations")
public class Relations {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String cardinalityCO;
	private String cardinalityOC;
	private String resourceUri;
	
	@ManyToOne
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "endpoint_id")
	private Endpoint endpoint;
	
	@ManyToOne
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "class_id")
	private ClassStructure classStructure;

	@ManyToOne
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "predicate_id")
	private PredicateStructure predicateStructure;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getResourceUri() {
		return resourceUri;
	}

	public void setResourceUri(String resourceUri) {
		this.resourceUri = resourceUri;
	}

	public ClassStructure getClassStructure() {
		return classStructure;
	}

	public void setClassStructure(ClassStructure classStructure) {
		this.classStructure = classStructure;
	}

	public PredicateStructure getPredicateStructure() {
		return predicateStructure;
	}

	public void setPredicateStructure(PredicateStructure predicateStructure) {
		this.predicateStructure = predicateStructure;
	}

	public String getCardinalityCO() {
		return cardinalityCO;
	}

	public void setCardinalityCO(String cardinalityCO) {
		this.cardinalityCO = cardinalityCO;
	}

	public String getCardinalityOC() {
		return cardinalityOC;
	}

	public void setCardinalityOC(String cardinalityOC) {
		this.cardinalityOC = cardinalityOC;
	}

	public Endpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

	
}
