package ncbo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

public class AnnotatorClient { 
	
	public static final String annotatorUrl = "http://data.bioontology.org/annotator";
	private static final String API_KEY = "74028721-e60e-4ece-989b-1d2c17d14e9c";
	
    public static void main( String[] args ) {
    	String contents = annotate("water");
    	System.out.println(contents);
    }
    
    public static String annotate(String text) {
    	try {
            HttpClient client = new HttpClient();
            client.getParams().setParameter(
            HttpMethodParams.USER_AGENT,"Annotator Client Example - Annotator");  //Set this string for your application
            
            String stopwords = getStopWords(new File("data/stopwords.txt"));
            
            PostMethod method = new PostMethod(annotatorUrl);
            
            // Configure the form parameters
            method.addParameter("longest_only","true");
            method.addParameter("whole_word_only","true");
            method.addParameter("stop_words",stopwords);
            method.addParameter("minimum_match_length","3");
            method.addParameter("include_synonyms","true"); 
            method.addParameter("max_level", "0");
            method.addParameter("mapping_types", "false"); //null, Automatic, Manual 
            method.addParameter("text", text);  //"Melanoma is a malignant tumor of melanocytes which are found predominantly in skin but also in the bowel and the eye");
	    method.addParameter("ontologies", "MESH,RXNORM"); 
	    method.addParameter("include", "prefLabel"); 
            method.addParameter("format", "xml"); 
            method.addParameter("apikey", API_KEY);

            // Execute the POST method
            int statusCode = client.executeMethod(method);
            
            if( statusCode != -1 ) {
                try {
                String contents = method.getResponseBodyAsString();
                method.releaseConnection();
                return contents;
                }
                catch( Exception e ) {
                    e.printStackTrace();
                }
            }
        }
        catch( Exception e ){
            e.printStackTrace();
        }
    	return null;
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

