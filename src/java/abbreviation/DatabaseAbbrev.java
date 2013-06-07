package abbreviation;

/*
 * Retrieve (using SPARQL) all sections from linkedSPLs database, process for abbreviations, and return abbreviations found,
 * as well as the setid of the spl in which the abbreviation was found, the section in which the abbreviation was found,
 * and the effectiveTime and version of the spl in which the abbreviation was found.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.query.*;

public class DatabaseAbbrev {
	private final String SERVICE = "http://dbmi-icode-01.dbmi.pitt.edu:8080/sparql";
	private final String DEFAULTGRAPH = "<http://dbmi-icode-01.dbmi.pitt.edu/linkedSPLs/>";
	private final String[] SECTIONPROPERTIES = {"adverseReaction", "boxedWarning", "clinicalPharmacology",
												"clinicalStudies", "contraindication", "description", "dosage",
												"drugInteractions", "supply", "inactiveIngredients", "indication",
												"overdosage", "precautions", "specificPopulations", "therapeuticApplication"};
	private final int INCREMENT = 50;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DatabaseAbbrev dbabb = new DatabaseAbbrev();
		dbabb.getAbbrevs();
//		dbabb.test();
	}
	
	private String buildQuery(String section, Integer offset, Integer limit) {
		String queryString = "PREFIX dailymed: <http://dbmi-icode-01.dbmi.pitt.edu/linkedSPLs/vocab/resource/>" +
							 " SELECT ?text ?setId ?time ?version" + 
							 " FROM " + DEFAULTGRAPH + 
							 " WHERE {?s dailymed:" + section +  " ?text ." +
							 "        ?s dailymed:setId ?setId ." +
							 "        ?s dailymed:effectiveTime ?time ." +
							 "        ?s dailymed:versionNumber ?version}";
		if (offset != null || limit != null) {
			queryString += " ORDER BY ?text";
		}
		if (offset != null) {
			queryString += " OFFSET " + Integer.toString(offset);
		}
		if (limit != null) {
			   queryString += " LIMIT " + Integer.toString(limit);
		}
		return queryString;
	}
	
	private ResultSet executeQuery(String queryString) {
		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.sparqlService(SERVICE, query);
		ResultSet results = qe.execSelect();
	
		// Important - free up resources used running the query
		qe.close();
		return results;
	}
	
	private void getAbbrevs() {
		try {
			File out = new File("spl_abbreviations.csv");
			BufferedWriter bw = new BufferedWriter(new FileWriter(out));
			for (String section : SECTIONPROPERTIES) {
				String queryString = buildQuery(section, null, null);
				System.out.println(queryString);
				Query query = QueryFactory.create(queryString);
				QueryExecution qe = QueryExecutionFactory.sparqlService(SERVICE, query);
				ResultSet results = qe.execSelect();
				Map<String, Set<String>> abbrevMap = parseResults(results);
				for (Map.Entry<String, Set<String>> entry : abbrevMap.entrySet()) {
					String[] data = entry.getKey().split("\\|");
					for (String abbrevPair : entry.getValue()) {
						String[] abbrevSplit = abbrevPair.split("\t"); 
						bw.write(section + "," + data[0] + "," + data[1] + "," + data[2] + "," + abbrevSplit[0].trim() + "," + abbrevSplit[1].trim() + "\n");
					}
				}
			
//			int total = 0;
//			while (true) {
//				String queryString = buildQuery(section, offset, INCREMENT);
//				System.out.println(queryString);
//				Query query = QueryFactory.create(queryString);
//				QueryExecution qe = QueryExecutionFactory.sparqlService(SERVICE, query);
//				ResultSet results = qe.execSelect();
//				int count = 0;
//				while (results.hasNext()) {
//					results.next();
//					count += 1;
//					total += 1;
//				}
//				qe.close();
//				if (count == 0) {
//					break;
//				} else {
//					offset += INCREMENT;
//				}
//			}
//			System.out.println(section + " has " + total + " results");
			}
			bw.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	private String getData(QuerySolution qs) {
		String time = qs.get("time").toString();
		String setId = qs.get("setId").toString();
		String version = qs.get("version").toString();
		return setId + "|" + time + "|" + version;
	}
	
	private Map<String, Set<String>> parseResults (ResultSet results) {
		Map<String, Set<String>> abbrevMap = new HashMap<String, Set<String>>();
		ExtractAbbrev extractAbbrev = new ExtractAbbrev();
		while (results.hasNext()) {
			Set<String> abbrevSet = new HashSet<String>();
			QuerySolution qs = results.next();
			String resultData = getData(qs);
			String text = qs.get("text").toString();
			File temp = null;
			try {
				temp = File.createTempFile("results", ".tmp");
				temp.deleteOnExit();
				BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
	    	    bw.write(text);
	    	    bw.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
			List<String> abbrevs = extractAbbrev.extractAbbrPairs(temp.getAbsolutePath());
			for (String abbrev : abbrevs) {
				abbrevSet.add(abbrev);
			}
			if (!abbrevs.isEmpty()) {
				if (abbrevMap.containsKey(resultData)) {
					abbrevMap.get(resultData).addAll(abbrevSet);
				} else {
					abbrevMap.put(resultData, abbrevSet);
				}
			}
		}
		return abbrevMap;
	}
	
	private List<String> split(String str, Character cmp) {
		List<String> splitStr = new ArrayList<String>();
		String strbuild = "";
		for (Character ch : str.toCharArray()) {
			if (ch.equals(cmp)) {
				splitStr.add(strbuild);
				strbuild = "";
			} else {
				strbuild += ch.toString();
			}
		}
		splitStr.add(strbuild);
		return splitStr;
	}
	
	private void test() {
		String queryString = buildQuery("adverseReaction", 0, 20);
		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.sparqlService(SERVICE, query);
		ResultSet results = qe.execSelect();
		Map<String, Set<String>> abbrevMap = parseResults(results);
		for (Map.Entry<String, Set<String>> entry : abbrevMap.entrySet()) {
			String[] data = entry.getKey().split("\\|");
			for (String abbrevPair : entry.getValue()) {
				String[] abbrevSplit = abbrevPair.split("\t");
			}
		}
		
	}

}
