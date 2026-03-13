package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.entity.Transaction;
import com.example.demo.form.TransactionForm;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BudgetController {
	
	private final JdbcTemplate jdbcTemplate;
	
	@GetMapping("/index")
	public String index(Model model){
		String sql="SELECT transactions.flow,"
				+ "transactions.name,"
				+ "transactions.price,"
				+ "transactions.genre,"
				+ "transactions.date"
				+ " FROM transactions;";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		List<Transaction> transactions=new ArrayList<Transaction>();
		for(Map<String, Object> map: list) {
			Transaction transaction=new Transaction();
			transaction.setFlow((String)(map.get("flow")));
			transaction.setName((String)(map.get("name")));
			transaction.setPrice((int)(map.get("price")));
			transaction.setGenre((String)(map.get("genre")));
			transaction.setDateString(map.get("date").toString());
			transactions.add(transaction);
		}
		model.addAttribute("transactions", transactions);

		return "index";
	}

	@GetMapping("/registration")
	public String registration(@ModelAttribute TransactionForm form){
		return "registration";
	}
	
	
	@PostMapping("/registration/submit")
	public String registration_submit(@Validated @ModelAttribute TransactionForm form,
										BindingResult result){
		if (result.hasErrors()) {
			return "registration";
		}
		
		String sql ="INSERT INTO transactions"
				+ "            (flow, name, price, genre, date)"
				+ "     VALUES (?, ?, ?, ?, ?)";

			jdbcTemplate.update(sql, form.getFlow(),
					form.getName(),
					form.getPrice(),
					form.getGenre(),
					form.getDate()
					);
			
		
//		System.out.println(form);
		return "redirect:/index";
	}

	
}
