package com.example.demo.form;

import java.sql.Date;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import lombok.Data;

@Data
public class TransactionForm {
	@NotNull
	private String flow;
	
	@NotNull
	private String name;
	
	@NotNull
	private Integer price;
	
	private String genre;
	
	@Past
	private Date date;
}
