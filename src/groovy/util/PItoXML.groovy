package util

import abbreviation.ExtractAbbrev

import ncbo.AnnotatorClient

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.IndexWordSet
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.dictionary.Dictionary;
import net.sf.extjwnl.data.PointerUtils;
import net.sf.extjwnl.data.list.PointerTargetNode
import net.sf.extjwnl.data.list.PointerTargetTree;
import net.sf.extjwnl.data.list.PointerTargetNodeList;

class PItoXML {
	
        static final String PIPATH = "/home/rdb20/swat-4-med-safety/u-of-pitt-SPL-drug-NER/textfiles/"
	def BASEPATH = "/home/rdb20/swat-4-med-safety/u-of-pitt-SPL-drug-NER/"

	def OUTPATH = BASEPATH + "processed-output/"

	def RXNORM = "1423"  //virutalOntologyId constant for RXNORM
	def MESH = "1351" //virutalOntologyId constant for Medical Subject Headings (MeSH)
	def PUNCTUATION ='[\\Q!"#$%&()*+,./:;<=>?@[\\]^_`{|}~\\E]'
	def ALLPUNCTUATION= '[\\Q!"#$%&()*+,./:;<=>?@[\\]^_`{|}~-\'\\E]'
	def PROPSFILENAME = "etc/file_properties.xml"  //Wordnet api (extJWNL) properties file
	def BASERXNORMURI = "http://rxnav.nlm.nih.gov/REST/"
        //def BIOPORTALURL = "http://rest.bioontology.org/bioportal/" //"http://localhost:8080/bioportal/"
	def BIOPORTALURL = "http://data.bioontology.org/" 
	def APIKEY = "74028721-e60e-4ece-989b-1d2c17d14e9c"
	def STRINGWRITER = new StringWriter()
	def PRINTWRITER = new PrintWriter(STRINGWRITER)
	def XMLNODEPRINTER = new XmlNodePrinter(PRINTWRITER)
	/*Wordnet words that may be useful as hypernym checks:
	 * antiflatulent, antifungal, antacid, coagulant, emmenagogue, galactagogue, hypnagogue, hypoglycemic agent,
	 * lactifuge, lactogen, viricide, vermicide.
	 * 
	 * If no useful hyponyms, currently excluded.
	 */
	def HYPERNYMS = ["drug", "antifungal", "antacid", "hypoglycemic agent", "lactogen", "hormone", "steroid", "amino acid", "plant", "vitamin", "organic compound"]
	def RXNORMTERMTYPES = ["BN", "SBD", "BPCK", "SBDC", "SCD", "SCDC"] //Brand Names + Ingredients["BN", "IN", "SBD", "BPCK", "SBDC", "GPCK", "SCD", "SCDC", "MIN", "PIN"] BN = Brand Name, IN - Ingredient
	def METABOLITEFILE = new File("data/metabolites_070212.txt")
	def HERBALFILE = new File("data/herbals_070212.txt")
	def METABOLITES = new HashSet()
	def HERBALS = new HashSet()
	def XMLDECLARATION = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\r\n"
	
	def MESHBASEURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/"
	def MESHSEARCH = MESHBASEURL + "esearch.fcgi?db=mesh&term="
	def MESHSUMMARY = MESHBASEURL + "esummary.fcgi?db=mesh&version=2.0&id="
	static final List METABOLITEPROPERTIES = ["metabolite"]
	static final List HERBALPROPERTIES = ["herbal", "homeopathic"]
	
	def JOCHEMFILE = new File("data/jochem_terms_translated.txt")
	def JOCHEMTERMS = new HashSet()
	def SUBTRACTIONSFILE = new File("data/subtractions.txt")
	def ADDITIONSFILE = new File("data/additions.txt")
	def ABBREVIATIONSFILE = new File("data/abbreviations.txt")
	def SUBTRACTIONS = new HashSet()
	def ADDITIONS = new HashSet()
	
	def HYPHENSUFFIXES = ["-sensitive", "-based"]
	def CHEMICALGROUPS = ["-oh" : "-hydroxy",
						  "-hydroxy" : "-oh-",
						  "^hydroxy" : "oh-",
						  "-ch3" : "-methyl",
						  "^methyl" : "ch3-",
						  "-nh2" : "-amino",
						  "-amino" : "-nh2-",
						  "^amino" : "nh2-"]
	
	static final boolean DEBUG = false
	static final Dictionary dictionary = Dictionary.getInstance(new FileInputStream("etc/file_properties.xml"))
	
	static main(args) {
		def pitoXML = new PItoXML();
		pitoXML.setup()
		pitoXML.annotate()
	}
	
	def annotate() {
		println "Annotate function"
		def piFiles = getPIFiles()
		def xmlParser = new XmlParser()
		piFiles.each{
			println it.toString()
			def origText = it.text
			//def text = replaceHyphenSuffixes(replacePunctuation(origText.toLowerCase()))
			def text = origText.toLowerCase()
			println "INFO: Calling annotator client"
			def xml = AnnotatorClient.annotate(text)
			if (xml == null){
			    println "xml from annotator is null"
			}
			println "INFO: parsing the XML text for a root"
			def ncboRoot = xmlParser.parseText(xml)
			if (ncboRoot == null){
			    println "ncboRoot  is null"
			}
			println "INFO: postprocessing the XML"
			filter(ncboRoot)
			checkJochem(origText)
			add(text, origText, ncboRoot)
			writeXml(it.getName(), ncboRoot)
		}
	}
	
	def annotate(text) {
		println "Annotate function with text"
		def origText = text
		    //text = replaceHyphenSuffixes(replacePunctuation(text.toLowerCase()))
		text = text.toLowerCase()
		def xmlParser = new XmlParser()
		def xml = AnnotatorClient.annotate(text)
		def ncboRoot = xmlParser.parseText(xml)
		filter(ncboRoot)
		checkJochem(origText)
		add(text, origText, ncboRoot)
		return ncboRoot
	}
	
	def annotate_test(text) {
     	        println "annotate_test function"
		def origText = text 
		    //text = replaceHyphenSuffixes(replacePunctuation(text.toLowerCase()))
		text = text.toLowerCase()
		def xmlParser = new XmlParser()
		def xml = AnnotatorClient.annotate(text)
		def ncboRoot = xmlParser.parseText(xml)
		filter(ncboRoot)
		checkJochem(origText)
		add(text, origText, ncboRoot)
		XMLNODEPRINTER.print(ncboRoot)
		new File("test.xml").write(XMLDECLARATION + STRINGWRITER.toString())
	}
	
	//Add drugs missed by ncbo annotator
	def add(text, originalText, root) {
		def nameNodes = root.data.annotatorResultBean.annotations.annotationBean.context.term.name
		def terms = []
		nameNodes.each{
			terms << it.text().toLowerCase()
		}
		def textWords = text.split()
		ADDITIONS.each{
			def addition = it.toLowerCase()
			if (textWords.grep(addition) && !terms.grep(addition) && !SUBTRACTIONS.grep(addition) && !inWordnet(addition)) {
				def locations = checkLocations(root, getLocations(it, originalText))
				if (locations) {
					add_xml(root, it, locations)
				}
			}
		}
	}
	
	//Add nodes to the xml for Addition terms found in submitted text
	def add_xml(root, addTerm, locations) {
		def annotations = root.data.annotatorResultBean.annotations[0]
		locations.each{
			def annotationBean = new Node(annotations, "annotationBean")
			
			def concept = new Node(annotationBean, "concept")
			new Node(concept, "id", "None")
			new Node(concept, "localConceptId", "None")
			new Node(concept, "localOntologyId", "None")
			new Node(concept, "fullId", "Added locally")
			new Node(concept, "preferredName", addTerm)
			
			def context = new Node(annotationBean, "context")
			new Node(context, "from", it[0])
			new Node(context, "to", it[1])
			
			def term = new Node(context, "term")
			new Node(term, "name", addTerm)
		}
	}
	
	/*
	 * Check each word in text against the set of Jochem terms.
	 * If a match is found, add the term to the annotations if it
	 * is not already present. 
	 */
	def checkJochem(text) {
		def words = text.split();
		for (baseWord in words) {
			def word = baseWord.replaceAll(ALLPUNCTUATION, "").replaceAll(" ", "")
			if (JOCHEMTERMS.contains(word)) {
				ADDITIONS.add(baseWord)
				continue
			}
			def sub = substituteChemicalGroups(word)
			if (sub != null) {
				sub.replaceAll(ALLPUNCTUATION, "").replaceAll(" ", "")
				if (JOCHEMTERMS.contains(sub)) {
					ADDITIONS.add(baseWord)
				}
			}
		}
	}
	
	/*
	* Return only those locations that don't overlap with annotations
	* that are already present.
	*/
   def checkLocations(root, locations) {
	   def annotationBeans = root.data.annotatorResultBean.annotations.annotationBean
	   def toremove = []
	   for (annotationBean in annotationBeans) {
		   def from = Integer.parseInt(annotationBean.context.from.text())
		   def to = Integer.parseInt(annotationBean.context.to.text())
		   for (location in locations) {
			   if (location in toremove) {
				   continue
			   }
			   if (overlaps(from, to, location[0], location[1])) {
				   toremove << location
			   }
		   }
	   }
	   for (location in toremove) {
		   locations.remove(locations.indexOf(location))
	   }
	   return locations
   }
	
	//Filter out unwanted terms from ncbo annotations
	def filter(root) {
		def annotations = root.data.annotatorResultBean.annotations[0]
		if (annotations == null){
		    println "annotations is null"
		}
		def allNames = []
		def removeNames = []
		annotations.annotationBean.each{
			def name = it.context.term.name.text().toLowerCase()
			if (!(name in allNames)) {
				if (!isDrugName(name, it) || (name in SUBTRACTIONS)) {
					removeNames << name
				}
			}
			allNames << name
		}
		for (name in removeNames) {
			annotations.annotationBean.each {
				if (it.context.term.name.text().toLowerCase().equals(name)) {
					annotations.remove(it)
				}
			}
		}
	}
	
	def getAbbreviations(filename) {
		def abbrevSet = new HashSet()
		def extractAbbrev = new ExtractAbbrev()
		String[] args = {filename}
		List<String> abbrevs = extractAbbrev.extractAbbrPairs(filename)
		abbrevs.each{
			abbrevSet.add(it)
		}
		return abbrevSet
	}
	
	def getAllAbbreviations() {
		def piFiles = getPIFiles()
		def abbrevMap = new HashMap()
		for (File piFile : piFiles) {
			abbrevMap.put(piFile.name, getAbbreviations(piFile.absolutePath))
		}
		def outFile = new File("data/pi_abbreviations.csv")
		for (Map.Entry<String, Set> entry : abbrevMap) {
			for (String abbrev : entry.getValue()) {
				def abbrevPair = split(abbrev, "\t")
				outFile.append(entry.getKey() + "," + abbrevPair[0] + "," + abbrevPair[1] + "\n")
			}
		}
	}
	
	//From a list of virtualOntologyIds, get a corresponding list of localOntologyIds
	//based on the xml returned from NCBO Annotator
	def getLocalOntologyIds(Ids, root) {
		def ontologiesUsed = root.data.annotatorResultBean.ontologies.ontologyUsedBean
		def ontologyMap = [:]
		def localIds = []
		ontologiesUsed.each{
			ontologyMap[it.virtualOntologyId.text()] = it.localOntologyId.text()
		}
		Ids.each{
			localIds << ontologyMap[it]
		}
		return localIds
	}
	
	//Return a list of locations of a string within some text
	def getLocations(word, text) {
		def locations = []
		def starts = indexOfAll(word, text)
		starts.each{
			locations.add([it, it + word.size() - 1])
		}
		return locations
	}
	
	def getMeshUids(term) {
		/*Retrieve ids for term from MeSH E-utility
		 * 
		 *  Args:
		 *  term - a search string
		 */
		def url = MESHSEARCH + term.replaceAll(" ", "%20")
		def client = new HttpClient()
		def method = new GetMethod(url)
		def status = client.executeMethod(method)
		if( status != -1 ) {
			try {
				def root = new XmlParser().parseText(method.getResponseBodyAsString())
				method.releaseConnection();
				def uids = []
				root.IdList.Id.each{
					uids << it.text()
				}
				return uids
			} catch( ex ) {
				println ex
			}
		}
		
	}
	
	def getPIFiles() {
		File dir = new File(PIPATH)
		def files = []
		dir.listFiles().each{
			if (it.isFile()) {
				files << it
			}
		}
		return files
	}
	
	//Get indices of all occurrences of substring in string
	def indexOfAll(substring, string) {
		substring = substring.toLowerCase()
		string = string.toLowerCase()
		def indices = []
		def index = string.indexOf(substring)
		while (index >= 0) {
			indices.add(index)
			index = string.indexOf(substring, index + 1)
		}
		return indices
	}
	
	def writeXml(name, root) {
		name += "-PROCESSED.xml"
		STRINGWRITER.getBuffer().setLength(0)
		XMLNODEPRINTER.print(root)
//		def stringWriter = new StringWriter()
//		def printWriter = new PrintWriter(stringWriter)
//		def xmlNodePrinter = new XmlNodePrinter(printWriter).print(root)
		def writeFile = new File(OUTPATH + name)
		writeFile.write(XMLDECLARATION + STRINGWRITER.toString())
		writeFile = null
	}
	
	//See if a word is in WordNet
	//Wordnet apparently contains a fair number of drug names, so also
	//check if word has the hypernym (synonym ancestor) 'drug' or drug related hypernyms
	def inWordnet(testword) {
		def isWord = false
		def words = []
		testword = testword.toLowerCase()
		words << testword
		if (testword.endsWith("s")) {
			words << testword.substring(0, testword.length()-1)
		}
		//Dictionary dictionary = Dictionary.getInstance(new FileInputStream(PROPSFILENAME))
		for (word in words) {
			IndexWordSet result = dictionary.lookupAllIndexWords(word)
			if (result.size() > 0) {
				for (IndexWord indexword in result.getIndexWordCollection()) {
					if (indexword.lemma.toLowerCase().equals(word)) {
						isWord = true
						if (isWordnetDrug(indexword)) {
							return false
						}
					}
				}
			} else {
				return false
			}
		}
		return isWord
	}
	
	/*
	 * Determine if a word/annotation contains a valid drug name
	 */
	def isDrugName(word, annotationBean) {
	    // NOTE: isMeshMetabolite currently broken -- the API returns JSON but
	    //  this expects XML. Also, this is probably not necessary any
	    //   more because the ncbo call specifies semantic types.
	    //return !inWordnet(word) && (rxnormTradename(word) || isJochemTerm(word) || isMeshMetabolite(annotationBean))
	    return !inWordnet(word) && (rxnormTradename(word) || isJochemTerm(word))
	}
	
	def isHerbal(word) {
		if (word.toLowerCase() in HERBALS) {
			return true
		}
		return false
	}
	
	def hasMeshProperty(word, properties) {
		/*Search Mesh notes on word for string property
		 * 
		 * Args:
		 * word - a term to search for in MeSH
		 * properties - a list of strings to search for in notes of MeSH entry for word
		 * */
		def client = new HttpClient()
		def uids = getMeshUids(word)
		for (uid in uids){
			def url = MESHSUMMARY + uid
			def method = new GetMethod(url)
			def status = client.executeMethod(method)
			if( status != -1 ) {
				try {
					def root = new XmlParser().parseText(method.getResponseBodyAsString())
					method.releaseConnection();
					def note = root.DocumentSummarySet.DocumentSummary.DS_ScopeNote.text().toLowerCase()
					if (DEBUG) {
						println word
						println note
					}
					for (property in properties) {
						if (note.contains(property.toLowerCase())) {
							return true
						}
					}
				} catch( ex ) {
					println ex
				}
			}
		return false
		}
	}
	
	def isJochemTerm(word) {
		/*Search a list of Jochem terms for word*/
		if (word.toLowerCase() in JOCHEMTERMS) {
			return true
		}
		return false
	}
	
	/*
	 * Determine if an annotation is categorized as an organic chemical (TUI = T109)
	 * by MeSH, making it a good candidate for a metabolite.
	 *
	 * TODO: This is currently broken -- the API returns JSON but
	 * this expects XML. Also, this is probably not necessary any
	 * more because the ncbo call specifies semantic types.
	 */
	def isMeshMetabolite(annotationBean) {
		 //Work around for broken semantic types in ncbo annotator
		def localConceptId = annotationBean.concept.localConceptId.text()
    	        //def url = BIOPORTALURL + "virtual/ontology/$MESH?conceptid=$localConceptId&apikey=$APIKEY"
		def url = BIOPORTALURL + "search?q=$localConceptId&ontology=MESH&apikey=$APIKEY"
		def client = new HttpClient()
		def method = new GetMethod(url)
		def status = client.executeMethod(method)
		if( status != -1 ) {
			try {
				def root = new XmlParser().parseText(method.getResponseBodyAsString())
				method.releaseConnection();
				for (entry in root.data.classBean.relations.entry) {
					if (entry.string.text().equals("TUI")) {
						for (tui in entry.list.string) {
							if (tui.text().equals("T109")) {
								return true
							}
						}
					}
				}
				return false
			} catch( ex ) {
				println ex
			}
		}
	}
	
	def isMetabolite(word) {
		/*Search a list of metabolites for word*/
		if (word.toLowerCase() in METABOLITES) {
			return true
		}
		return false
	}
	
	//Check wordnet to see if indexword has the hypernym (synonym ancestor) 'drug'
	//or other drug related hypernyms
	def isWordnetDrug(indexword) {
		List senses = indexword.getSenses()
		for (Synset sense in indexword.getSenses()) {
			PointerTargetTree hypernymTree = PointerUtils.getHypernymTree(sense)
			for (PointerTargetNodeList hypernyms : hypernymTree.toList()) {
				for (PointerTargetNode hypernym : hypernyms) {
					def hypernymWords = hypernym.toString().split("--")[0].split("Words:")[1].trim()
					if (DEBUG) {
						println hypernymWords
					}
					for (hyp in HYPERNYMS) {
						if (hypernymWords.contains(hyp)) {
							return true
						}
					}
				}
			}
		}
		return false
	}
	
	def getWordnetHyponyms(word) {
	    //Dictionary dictionary = Dictionary.getInstance(new FileInputStream(PROPSFILENAME))
		IndexWordSet result = dictionary.lookupAllIndexWords(word)
		if (result.size() > 0) {
			for (IndexWord indexword in result.getIndexWordCollection()) {
				wordnetHyponyms(indexword)	
			}
		}
	}
	
	/*Return true if the linear space reperesented by range1 overlaps with that of range2*/
	def overlaps(a1, a2, b1, b2) {
		if (a1 <= b2 && a1 >= b1) {
			return true
		} else if (a2 <= b2 && a2 >= b1) {
			return true
		} else if (b1 <= a2 && b1 >= a1) {
			return true
		} else if (b2 <= a2 && b2 >= a1) {
			return true
		}
		return false
	}
	
	def wordnetHyponyms(indexword) {
		List senses = indexword.getSenses()
		for (Synset sense in indexword.getSenses()) {
			PointerTargetTree hypernymTree = PointerUtils.getHyponymTree(sense)
			for (PointerTargetNodeList hypernyms : hypernymTree.toList()) {
				for (PointerTargetNode hypernym : hypernyms) {
					def hypernymWords = hypernym.toString().split("--")[0].split("Words:")[1].trim()
					println hypernymWords
					if (hypernymWords.contains('drug')) {
						return true
					}
				}
			}
		}
		return false
	}
	
	//Restrict xml results to specific ontologies
	def restrictOntologies(root, ontologies) {
		def annotations = root.data.annotatorResultBean.annotations[0]
		def annotationBeans = annotations.annotationBean
		annotationBeans.each{
			if (!(it.concept.localOntologyId.text() in ontologies)){
				annotations.remove(it)
			}
		}
	}
	
	//Use the RxNorm REST service to determine
	//if an annotation returned by NCBO annotator has
	//a relationship linking it to an actual drug
	def rxnormTradename(term) {
		def client = new HttpClient()
		def rxcuis = getRxNormCuis(term)
		def relas = "tradename_of+has_tradename+form_of+precise_ingredient_of"
		for (cui in rxcuis){
			def url = BASERXNORMURI + "rxcui/$cui/related?rela=" + relas
			def method = new GetMethod(url)
			def status = client.executeMethod(method)
			if( status != -1 ) {
				try {
					def root = new XmlParser().parseText(method.getResponseBodyAsString())
					method.releaseConnection();
					if (DEBUG) {
						println root.relatedGroup.conceptGroup
					}
					if (!root.relatedGroup.conceptGroup.isEmpty()) {
						return true
					}
				} catch( ex ) {
					println ex
				}
			}
			url = BASERXNORMURI + "rxcui/$cui/properties"
			method = new GetMethod(url)
			status = client.executeMethod(method)
			if (status != -1) {
				try {
					def root = new XmlParser().parseText(method.getResponseBodyAsString())
					method.releaseConnection()
					if (DEBUG) {
						root.properties.tty.text()
					}
					if (root.properties.tty.text() in RXNORMTERMTYPES) {
						return true
					}
				} catch (ex) {
					println ex
				}
			}
		}
		return false
	}
	
	//Retrieve the rxcui from the RxNorm REST service for term
	def getRxNormCuis(term) {
		term = term.replaceAll(" ", "%20")
		def url = BASERXNORMURI + "rxcui?name=$term&srclist=RXNORM"
		def client = new HttpClient()
		def method = new GetMethod(url)
		def status = client.executeMethod(method)
		if( status != -1 ) {
			try {
				def root = new XmlParser().parseText(method.getResponseBodyAsString())
				method.releaseConnection();
				def cuis = []
				root.idGroup.rxnormId.each{
					cuis << it.text()
				}
				return cuis
			} catch( ex ) {
				println ex
			}
		}
	}
	
	def replaceHyphenSuffixes(text) {
		HYPHENSUFFIXES.each{
			text = text.replaceAll(it, " ")
		}
		return text
	}
	
	//Remove any punctuation from submitted text, except "'" and "-"
	def replacePunctuation(text) {
		return text.replaceAll(PUNCTUATION, " ")
	}
	
	/*
	 * Replace any sequence of characters from chars in text with the specified regular expression (regex)
	 */
	def replaceWithRegex(text, chars, regex) {
		return splitLazy(text, chars).join(regex)
	}
	
	/*
	 * Setup various string lists for later drug name searches
	 * 
	 * Note: The jochem file is particularly large (~60 Mb)
	 */
	def setup() {
//		METABOLITEFILE.readLines().each{
//			METABOLITES.add(it.toLowerCase().trim())
//		}
//		HERBALFILE.readLines().each{
//			HERBALS.add(it.toLowerCase().trim())
//		}
		JOCHEMFILE.readLines().each{
			JOCHEMTERMS.add(it.trim())
		}
		SUBTRACTIONSFILE.readLines().each{
			SUBTRACTIONS.add(it.toLowerCase().trim())
		}
		ADDITIONSFILE.readLines().each{
			ADDITIONS.add(it.toLowerCase().trim())
		}
		ABBREVIATIONSFILE.readLines().each{
			ADDITIONS.add(it.toLowerCase().trim())
		}
	}
	
	/*
	 * Emulates Python string.split.  Returns list of strings from str, sepearated on occurences
	 * of cmp.
	 */
	def split(String str, String cmp) {
		def splitStr = []
		def strbuild = ""
		for (ch in str) {
			if (ch.equals(cmp)) {
				splitStr << strbuild
				strbuild = ""
			} else {
				strbuild += ch
			}
		}
		splitStr << strbuild
		return splitStr
	}
	
	/*
	 * Split on any character or sequence of characters in chars
	 */
	def splitLazy(String text, String chars) {
		def newList = []
		def currentString = ""
		text = strip(text, chars).replaceAll(" ", "")
		def textIter = text.iterator()
		while (textIter.hasNext()) {
			def ch = textIter.next()
			if (chars.contains(ch)) {
				newList << currentString
				currentString = ""
			}
			while (chars.contains(ch)) {
				ch = textIter.next()
			}
			currentString = currentString + ch
		}
		newList << currentString
		return newList
	}
	
	/*
	 * Emulate python string.strip
	 */
	def strip(String str, String chars) {
		def start = null
		def end = null
		for (ch in str) {
			if (!chars.contains(ch)) {
				start = str.indexOf(ch)
				break
			}
		}
		for (int i=str.length()-1; i>=0; i--) {
			if (!chars.contains(str[i])) {
				end = i
				break
			}
		}
		return str.substring(start, end+1)
	}
	
	/*
	 * Find chemical group substitutions in text and replace them.
	 */
	def substituteChemicalGroups(String text) {
		text = text.toLowerCase()
		def substitutions = [:]
		Pattern regex
		Matcher matcher
		CHEMICALGROUPS.each{ k,v ->
			regex = Pattern.compile(k)
			matcher = regex.matcher(text)
			if (matcher.find()) {
				substitutions.put(k, v)
			}
		}
		if (!substitutions.isEmpty()) {
			substitutions.each { k,v ->
				regex = Pattern.compile(k)
				matcher = regex.matcher(text)
				while (matcher.find()) {
					text = matcher.replaceAll(v)
				}
			}
			return text
		}
		return null
	}
	
	// def test() {
	// 	def dikbFile = new File("C:/Work/package-insert-nlp/trunk/code/metabolite-extraction/dikb_metabolites.txt")
	// 	def dikbtext = dikbFile.text
	// 	checkJochem(dikbtext)
//		def piFiles = getPIFiles()
//		piFiles = [piFiles[0]]
//		piFiles.each{
//			def origText = it.text
//			def text = replacePunctuation(origText.toLowerCase())
//			checkJochem(text);
//			def xml = AnnotatorClient.annotate(text)
//			def ncboRoot = new XmlParser().parseText(xml)
//			filter(ncboRoot)
//			writeXml("test-" + it.getName(), ncboRoot)
//		}
//	}
	
	/*
	 * There are now a number of funtions to test if an annotation contains a drug name,
	 * this function can be used to show what they individually return.
	 */
	def testDrugFilterFunctions(word, annotationBean) {
		println word
		println "\tNot in wordnet: " + !inWordnet(word)
		println "\tRxNorm Tradename: " + rxnormTradename(word)
		println "\tis Jochem Term: " + isJochemTerm(word)
		println "\tis Mesh Metabolite: " + isMeshMetabolite(annotationBean)
		println "\tin Subtractions: " + (word.toLowerCase() in SUBTRACTIONS)
		print "\n"
	}
	
	/*
	 * Split text into a Set of words using regular expressions
	 */
	def tokenizeText(String text) {
		Pattern p = Pattern.compile("[\\w']+");
		Matcher m = p.matcher(text);
		Set<String> words = new HashSet<String>();
		while ( m.find() ) {
			words << text.substring(m.start(), m.end());
		}
		return words;
	}
}
