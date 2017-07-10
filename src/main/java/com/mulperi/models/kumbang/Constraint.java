package com.mulperi.models.kumbang;

import javax.persistence.Entity;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
public class Constraint extends AbstractPersistable<Long> {
	
	private static final long serialVersionUID = -4884066864162001583L;
	
	String ifPresent;
	String thenRequired;
	
	public Constraint() {
		
	}
	
	public Constraint(String name1, String name2) {
		ifPresent = name1;
		thenRequired = name2;
	}

	public String getIfPresent() {
		return ifPresent;
	}

	public void setIfPresent(String ifPresent) {
		this.ifPresent = ifPresent;
	}

	public String getThenRequired() {
		return thenRequired;
	}

	public void setThenRequired(String thenRequired) {
		this.thenRequired = thenRequired;
	}
	
	@Override
	public String toString() {
		return "present(" + ifPresent + ") => present(" + thenRequired + ")";
	}
}