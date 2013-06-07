package util

import util.PItoXML

/*
 * Determine from a list of abbreviations and their long forms whether they are drug name 
 * abbreviations.  If so, save to file.
 */
class DrugAbbreviation {
	
	def static UNIQUEABBREVFILE = new File("etc/spl_abbreviations_unique.csv")
	def static ABBREVFILE = new File("etc/spl_abbreviations.csv")
	def static OUTPUTFILE = new File("etc/spl_abbreviations_drugs.csv")

	static main(args) {
		def drugAbbrevs = []
		def piToXml = new PItoXML()
		for (line in UNIQUEABBREVFILE.readLines()) {
			try {
				println line
				def field = line.split(",")
				def abbrev = field[0].trim()
				def longForm = field[1].trim()
				def ncboRoot = piToXml.annotate(longForm)
				for (annotationBean in ncboRoot.data.annotatorResultBean.annotations.annotationBean) {
					if (longForm.toLowerCase().equals(annotationBean.context.term.name.text().toLowerCase())) {
						if (piToXml.isDrugName(longForm, annotationBean)) {
							drugAbbrevs << [abbrev, longForm]
							break
						}
					}
				}
			} catch (ex) {
			}
		}
		
		def output = ""
		for (line in ABBREVFILE.readLines()) {
			try {
				def field = line.split(",")
				def abbrev = field[4].trim()
				def longForm = field[5].trim()
				if ([abbrev, longForm] in drugAbbrevs) {
					output += line + "\n"
				}
			} catch (ex) {
			}
		}
		OUTPUTFILE.write(output)
	}

}
