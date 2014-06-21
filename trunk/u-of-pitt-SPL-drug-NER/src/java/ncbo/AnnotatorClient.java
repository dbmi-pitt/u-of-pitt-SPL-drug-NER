// AnnotateText.java
//
// Original Author: GitHub : palexander
//
// The original code was downloaded from
// https://github.com/ncbo/ncbo_rest_sample_code/ on 6/21/2014

package ncbo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class AnnotatorClient {

    static final String REST_URL = "http://data.bioontology.org";
    static final String API_KEY = "74028721-e60e-4ece-989b-1d2c17d14e9c";
    static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String urlParameters;
        JsonNode annotations;
        String textToAnnotate = URLEncoder.encode("Asenapine paroxetine PAXIL plavix", "ISO-8859-1");
        urlParameters = "text=" + textToAnnotate;

	String annotationsXml = annotate(textToAnnotate);
	System.out.println(annotationsXml);

	// UNCOMMENT THESE ENTRIES TO TEST GET AND POST CALLS
        // Get just annotations
        // annotations = jsonToNode(get(REST_URL + "/annotator?" + urlParameters));
        // printAnnotations(annotations);

        // // Annotations with hierarchy
        // urlParameters = "max_level=3&text=" + textToAnnotate;
        // annotations = jsonToNode(get(REST_URL + "/annotator?" + urlParameters));
        // printAnnotations(annotations);

        // // Annotations using POST (necessary for long text)
        // urlParameters = "text=" + textToAnnotate;
        // annotations = jsonToNode(post(REST_URL + "/annotator", urlParameters));
        // printAnnotations(annotations);

        // // Get labels, synonyms, and definitions with returned annotations
        // urlParameters = "include=prefLabel,synonym,definition&text=" + textToAnnotate;
        // annotations = jsonToNode(get(REST_URL + "/annotator?" + urlParameters));
        // for (JsonNode annotation : annotations) {
        //     System.out.println(annotation.get("annotatedClass").get("prefLabel").asText());
        // }
    }

    public static String annotate(String text) {
    	try {          
	    String textToAnnotate = URLEncoder.encode("text", "ISO-8859-1");
            String stopwords = getStopWords(new File("data/stopwords.txt"));
            JsonNode annotations;
	    String urlParameters;

            // Configure the form parameters
	    urlParameters = "longest_only=true&whole_word_only=true&stopwords=" + stopwords + "&minimum_match_length=3&include_synonyms=true&max_level=0&ontologies=MESH,RXNORM&include=prefLabel&text=" + textToAnnotate;

            // Execute the POST method
	    annotations = jsonToNode(post(REST_URL + "/annotator", urlParameters));            
	    String annotationsXml = convertAnnotationsToXml(annotations);
	    return annotationsXml;
        }
        catch( Exception e ){
            e.printStackTrace();
        }
    	return null;
    }


    private static void printAnnotations(JsonNode annotations) {
        for (JsonNode annotation : annotations) {
            // Get the details for the class that was found in the annotation and print
            JsonNode classDetails = jsonToNode(get(annotation.get("annotatedClass").get("links").get("self").asText()));
            System.out.println("Class details");
            System.out.println("\tid: " + classDetails.get("@id").asText());
            System.out.println("\tprefLabel: " + classDetails.get("prefLabel").asText());
            System.out.println("\tontology: " + classDetails.get("links").get("ontology").asText());
            System.out.println("\n");

            JsonNode hierarchy = annotation.get("hierarchy");
            // If we have hierarchy annotations, print the related class information as well
            if (hierarchy.isArray() && hierarchy.elements().hasNext()) {
                System.out.println("\tHierarchy annotations");
                for (JsonNode hierarchyAnnotation : hierarchy) {
                    classDetails = jsonToNode(get(hierarchyAnnotation.get("annotatedClass").get("links").get("self").asText()));
                    System.out.println("\t\tClass details");
                    System.out.println("\t\t\tid: " + classDetails.get("@id").asText());
                    System.out.println("\t\t\tprefLabel: " + classDetails.get("prefLabel").asText());
                    System.out.println("\t\t\tontology: " + classDetails.get("links").get("ontology").asText());
                }
            }
        }
    }

    private static JsonNode jsonToNode(String json) {
        JsonNode root = null;
        try {
            root = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }

    // The NER program that calls this client was written to work with
    // an older Bioportal API that returned XML. This methods shapes
    // the result to that XML format.
    private static String convertAnnotationsToXml(JsonNode annotations) {
	String xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	xmlStr += "<data><annotatorResultBean><annotations>";
	
        for (JsonNode annotation : annotations) {
            // Get the details for the class that was found in the annotation and print
            JsonNode classDetails = jsonToNode(get(annotation.get("annotatedClass").get("links").get("self").asText()));
	    JsonNode annotationNodeArray = annotation.get("annotations");
	    JsonNode annotationNode;
	    if (annotationNodeArray.isArray() && annotationNodeArray.elements().hasNext()) {
		annotationNode = annotationNodeArray.elements().next();
		// Debugging
		System.out.println(annotationNode.toString());
	    }
	    else {
		System.out.print("WARNING: no annotations!");
		continue;
	    }

	    xmlStr += "<annotationBean>";

	    xmlStr += "<context>";
	    xmlStr += "<term><name>";
	    xmlStr += classDetails.get("prefLabel").asText();
	    xmlStr += "</name></term>";
	    xmlStr += "<from>";
	    xmlStr += annotationNode.get("from").asText();
            xmlStr += "</from>";
	    xmlStr += "<to>";
	    xmlStr += annotationNode.get("to").asText();
            xmlStr += "</to>";
	    xmlStr += "</context>";
	    
	    xmlStr += "<concept>";
	    xmlStr += "<fullId>";
	    xmlStr += classDetails.get("@id").asText();
	    xmlStr += "</fullId>";
	    xmlStr += "<preferredName>";
	    xmlStr += classDetails.get("prefLabel").asText();
	    xmlStr += "</preferredName>";
	    xmlStr += "</concept>";
	    
	    xmlStr += "</annotationBean>";
	}	
	xmlStr += "</annotations></annotatorResultBean></data>";
	return xmlStr;
    }

    private static String get(String urlToGet) {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(urlToGet);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
            conn.setRequestProperty("Accept", "application/json");
            rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String post(String urlToGet, String urlParameters) {
        URL url;
        HttpURLConnection conn;

        String line;
        String result = "";
        try {
            url = new URL(urlToGet);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("charset", "utf-8");
            conn.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            conn.disconnect();

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static String getStopWords(File stopWordsFile) {
    	String contents = "";
    	try {
    		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(stopWordsFile)));
    		String line;
    		while ((line = reader.readLine()) != null) {
    			contents += line + ",";
    		}
    	} catch (IOException ioe) {
    		ioe.printStackTrace();
    		System.exit(1);
    	}

    	return contents;
    }



}
