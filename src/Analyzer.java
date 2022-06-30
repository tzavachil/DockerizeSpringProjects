import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Analyzer {
	
	public Analyzer() {
		
		String path = "D:\\Programming\\Eclipse 2021 Projects\\smartclide-TD-Principal-main";
		
		File pomFile = new File(path + "\\pom.xml");
		File dockerFile = new File(path + "\\Dockerfile");
		
		// Checks if there is the pom.xml file in the project's root directory
		if(pomFile.exists() && !pomFile.isDirectory()) {
			
			System.out.println("pom.xml file found");
			
			// Checks if pom.xml file has dependencies of a Spring Project 
			boolean temp = this.checkDependencies(pomFile);
			if(temp) {
				System.out.println("pom.xml has Spring's dependencies");
				// Checks if there is the Docker file in the project's root directory
				if(dockerFile.exists() && !dockerFile.isDirectory()) {
					System.out.println("Dockerfile found");
				}
				else {
					System.out.println("Dockerfile doesn't exist");
				}
			}
			else {
				System.out.println("pom.xml hasn't Spring's dependencies");
			}		
		}
		else {
			System.out.println("pom.xml file doesn't exist");}
	}
	
	public boolean checkDependencies(File pomFile) {
		
		boolean flag = false;
		
		try {
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
			Document doc = dbBuilder.parse(pomFile);
			doc.getDocumentElement().normalize();
			NodeList dep = doc.getElementsByTagName("dependency");
			
			ArrayList<String> groupIds = new ArrayList<>();
			
			for(int i=0; i<dep.getLength(); i++) {
				
				Node tempNode = dep.item(i);
				if(tempNode.getNodeType() == Node.ELEMENT_NODE) {
				
					Element depGroupIdElement = (Element) tempNode;
					groupIds.add(depGroupIdElement.getElementsByTagName("groupId").item(0).getTextContent());
					
				}
				
			}
			
			flag = groupIds.contains("org.springframework.boot");

		} catch (ParserConfigurationException|SAXException|IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return flag;
	}
	
}
