package com.example.demo.controller;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Transaction;
import com.example.demo.form.TransactionForm;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class BudgetController {
	
	private final JdbcTemplate jdbcTemplate;
	
	@GetMapping("/index")
	public String index(Model model){
		//入出力リスト
		String sql="SELECT transactions.flow,"
				+ "transactions.name,"
				+ "transactions.price,"
				+ "transactions.genre,"
				+ "transactions.date,"
				+ "transactions.id"
				+ " FROM transactions"
				+ " ORDER BY transactions.id DESC;";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		List<Transaction> transactions=new ArrayList<Transaction>();
		for(Map<String, Object> map: list) {
			Transaction transaction=new Transaction();
			transaction.setFlow((String)(map.get("flow")));
			transaction.setName((String)(map.get("name")));
			transaction.setPrice((int)(map.get("price")));
			transaction.setGenre((String)(map.get("genre")));
			transaction.setDateString(map.get("date").toString());
			transaction.setId((int)(map.get("id")));
			transactions.add(transaction);
		}
		model.addAttribute("transactions", transactions);
		
		//今月分の円グラフ
		
		LocalDateTime nowDate = LocalDateTime.now();
		LocalDateTime nextMonth = nowDate.plusMonths(1);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-01");
		String nowDateString = formatter.format(nowDate);
		String nextMonthString = formatter.format(nextMonth);

		//ジャンルごとの今月の出費
		Map<String, String> sums = new HashMap<String, String>();		
		String[] genres= {"fixed","food", "daily_necessities", "beauty", "entertainment", "other"};
		for(String genre : genres) {
	        sql = "SELECT SUM(price) "
	        		+ "FROM transactions "
	        		+ "WHERE genre = ? "
	          		+ "AND `date` BETWEEN ? AND ? ";
	          Map<String, Object> map = jdbcTemplate.queryForMap(sql,genre,nowDateString,nextMonthString);
	
	        if(map.get("SUM(price)")==null) {
	        	sums.put(genre, "0");
	        }else {
	        	sums.put(genre, map.get("SUM(price)").toString());
	        }
		}
		
		//今月の出費合計額
		sql = "SELECT SUM(price) "
        		+ "FROM transactions "
          		+ "WHERE flow = 'expense' "
          		+ "AND `date` BETWEEN ? AND ? ";
          Map<String, Object> map = jdbcTemplate.queryForMap(sql,nowDateString,nextMonthString);

        if(map.get("SUM(price)")==null) {
        	sums.put("sum", "0");
        }else {
        	sums.put("sum", map.get("SUM(price)").toString());
        }

		model.addAttribute("sums", sums);
		
		//棒グラフ
		int[] incomes = new int[6];	
		int[] expenses = new int[6];		
		int[] month_labels = new int[6];		

		for(int i=5; i>=0; i--) {
			LocalDateTime startDate = nowDate.plusMonths(-1*i);
			LocalDateTime endDate = nowDate.plusMonths(-1*i+1);
			String startDateString = formatter.format(startDate);
			String endDateString = formatter.format(endDate);
			month_labels[5-i]=Integer.parseInt(startDateString.split("-")[1]);
			sql = "SELECT SUM(price) "
	        		+ "FROM transactions "
	          		+ "WHERE flow = ? "
	          		+ "AND `date` BETWEEN ? AND ? ";
	          Map<String, Object> expenseMap = jdbcTemplate.queryForMap(sql,"expense", startDateString, endDateString);
	          Map<String, Object> incomeMap = jdbcTemplate.queryForMap(sql,"income", startDateString, endDateString);

	          if(expenseMap.get("SUM(price)")==null) {
	        	  expenses[5-i]=0;
	          }else {
	        	  BigDecimal price = new BigDecimal(expenseMap.get("SUM(price)").toString());
	        	  expenses[5-i]=price.intValue();
	          }
	          
	          if(incomeMap.get("SUM(price)")==null) {
	        	  incomes[5-i]=0;
	          }else {
	        	  BigDecimal price = new BigDecimal(incomeMap.get("SUM(price)").toString());
	        	  incomes[5-i]=price.intValue();
	          	sums.put("sum", map.get("SUM(price)").toString());
	          }

		
		}
		
		model.addAttribute("incomes", incomes);
		model.addAttribute("expenses", expenses);
		model.addAttribute("month_labels", month_labels);

		
	
		return "index";
	}

	@GetMapping("/registration")
	public String registration(@ModelAttribute TransactionForm form, Model model){
		model.addAttribute("editId",-1);
		return "registration";
	}
	
	
	@GetMapping("/edit/{id}")
	public String edit(@PathVariable int id, @ModelAttribute TransactionForm form, Model model) {
        String sql = "SELECT flow, name, price, genre, date FROM transactions WHERE id = ?";
        Map<String, Object> map = jdbcTemplate.queryForMap(sql, id);
//        System.out.println(map.get("name"));
        form.setFlow((String)map.get("flow"));
        form.setName((String)map.get("name"));
        form.setPrice((int)map.get("price"));
        form.setGenre((String)map.get("genre"));
        form.setDate((Date)map.get("date"));
		model.addAttribute("editId",id);

        return "registration";
	}
	
		
	@PostMapping("/registration/submit/{id}")
	public String registration_submit(@Validated @ModelAttribute TransactionForm form,
										BindingResult result,
										@PathVariable int id){
		if (result.hasErrors()) {
			return "registration";
		}
		String sql ="";
		if(id==-1) {
			sql ="INSERT INTO transactions"
					+ "            (flow, name, price, genre, date)"
					+ "     VALUES (?, ?, ?, ?, ?)";
		}else {
			sql="UPDATE transactions"
					+ " SET flow = ?"
					+ ", name = ?"
					+ ", price = ?"
					+ ", genre = ?"
					+ ", date = ?"
					+ " WHERE id = "
					+ Integer.toString(id);
		}
		jdbcTemplate.update(sql, 
				form.getFlow(),
				form.getName(),
				form.getPrice(),
				form.getGenre(),
				form.getDate()
				);
		
		return "redirect:/index";
	}
	
	@GetMapping("/advice")
	public String advice(Model model,
			RedirectAttributes redirectAttributes){
		String sql="SELECT transactions.flow,"
				+ "transactions.name,"
				+ "transactions.price,"
				+ "transactions.genre,"
				+ "transactions.date"
				+ " FROM transactions"
				+ " ORDER BY transactions.date DESC";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		int count=0;
		StringBuilder promptBuilder=new StringBuilder("以下の家計簿について、やりくりに関するアドバイスを100~200文字程度でしてください。\n");
		for(Map<String, Object> map: list) {

			promptBuilder.append(map.get("date").toString()+"に");
			promptBuilder.append(map.get("name").toString()+"で");
			promptBuilder.append(Integer.toString( (int)map.get("price") )+"円の");
			
			String flow=(String)(map.get("flow"));
			if( flow.equals("income") ){
				promptBuilder.append("収入\n");
			}else{
				promptBuilder.append("支出\n");
			}
			if(++count > 20) break;
		}
		
	    Client client = Client.builder().apiKey("key").build();
	    GenerateContentResponse response =
	        client.models.generateContent(
	            "gemini-3-flash-preview",
	            promptBuilder.toString(),
	            null);
	
		redirectAttributes.addFlashAttribute("advice",response.text());
		return "redirect:/index";
	}
	
	@GetMapping("/delete/{id}")
	public String delete(@PathVariable int id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        jdbcTemplate.update(sql, id);
		return "redirect:/index";
	}
	
	
}
