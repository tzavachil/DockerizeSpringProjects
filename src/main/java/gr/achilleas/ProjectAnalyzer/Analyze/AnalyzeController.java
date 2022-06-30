package gr.achilleas.ProjectAnalyzer.Analyze;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path= "api/analyze")
public class AnalyzeController {
	
	@Autowired
	private AnalyzeService analyzeService;
	
	@GetMapping("/service")
	public String analyze() {
		return analyzeService.print();
	}
	
}
