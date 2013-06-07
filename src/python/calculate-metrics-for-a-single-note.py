#!/usr/bin/python
#
# calculate-metrics-for-a-single-note.py
# 
# Author: Richard Boyce
# 
# This program calculates the recall, precision, and balanced F-measure
# for the NCBO Annotator service using a set of validation annotations
# created by a human annotator. The NCBO annotations and human
# annotations must be in XML files.

# Copyright (C) 2011 by Richard Boyce and the University of Pittsburgh
 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
# 
 
import getopt, sys
from xml.etree import ElementTree as ET

ontolLocalIdMap = {"SNP-Ontology":"39215",
                   "African Traditional Medicine":"40223",
                   "GeoSpecies Ontology":"39933",
                   "MeGO":"39976",
                   "Pseudogene":"38873",
                   "Family Health History Ontology":"38631",
                   "Ontology for Genetic Interval":"40117",
                   "Sample processing and separation techniques":"39509",
                   "MedlinePlus Health Topics":"40397",
                   "BRENDA tissue / enzyme source":"40643",
                   "Minimal anatomical terminology":"40245",
                   "BIRNLex":"29684",
                   "Hymenoptera Anatomy Ontology":"40660",
                   "Bleeding History Phenotype":"40546",
                   "Cell Cycle Ontology (A. thaliana)":"38550",
                   "Physician Data Query":"40399",
                   "Parasite Life Cycle":"39544",
                   "Cell Cycle Ontology (H. sapiens)":"36417",
                   "Proteomics Pipeline Infrastructure for CPTAC":"40009",
                   "National Drug File":"40402",
                   "Computer-based Patient Record Ontology":"39734",
                   "Protein Ontology":"3905",
                   "COSTART":"40390",
                   "International Classification of Primary Care":"40393",
                   "Suggested Ontology for Pharmacogenomics":"39343",
                   "DermLex: The Dermatology Lexicon":"39815",
                   "WHO Adverse Reaction Terminology":"40404",
                   "NCBI organismal classification":"38802",
                   "ABA Adult Mouse Brain":"40133",
                   "Galen":"4525",
                   "Basic Formal Ontology":"40358",
                   "Epoch Clinical Trial Ontologies":"8056",
                   "PhysicalFields":"40483",
                   "Ontology for disease genetic investigation":"39579",
                   "Cell Cycle Ontology (S. cerevisiae)":"36425",
                   "Cell Cycle Ontology (S. pombe)":"36430",
                   "Online Mendelian Inheritance in Man":"40398",
                   "Cardiac Electrophysiology Ontology":"39038",
                   "Comparative Data Analysis Ontology":"40648",
                   "MaHCO - An MHC Ontology":"39804",
                   "Cell line ontology":"39927",
                   "Proteomics data and process provenance":"13386",
                   "Protein-protein interaction":"39508",
                   "Subcellular Anatomy Ontology (SAO)":"14391",
                   "Ontology of Geographical Region":"39577",
                   "International Classification of Diseases":"35686",
                   "Cell Line Ontology":"40261",
                   "Microarray experimental conditions":"38801",
                   "Basic Vertebrate Anatomy":"4531",
                   "Ontology of Glucose Metabolism Disorder":"39580",
                   "Information Artifact Ontology":"40642",
                   "Syndromic Surveillance Ontology":"40646",
                   "MDSS Mo":"40649",
                   "Gazetteer Ontology":"40651",
                   "Ontology of Language Disorder in Autism":"40652",
                   "Pilot Ontology":"40653",
                   "Cell Behavior Ontology":"39336",
                   "Ontology for Biomedical Investigations":"40832",
                   "BioTop":"40937",
                   "Systems Biology":"40764",
                   "Amphibian taxonomy":"40791",
                   "ICNP":"40726",
                   "linkingkin2pep":"40802",
                   "Terminology for the Description of Dynamics":"40985",
                   "PKO_Re":"40917",
                   "Kinetic Simulation Algorithm Ontology":"40844",
                   "International Classification of Functioning, Disability and Health (ICF)":"40865",
                   "Common Terminology Criteria for Adverse Events":"40984",
                   "Parasite Experiment Ontology":"42093",
                   "Ontology of homology and related concepts in biology":"42117",
                   "MedDRA":"42280",
                   "RxNORM":"46395", #"44775",
                   "National Drug Data File":"42282",
                   "ICD-10-PCS":"42283",
                   "Master Drug Data Base":"42284",
                   "ICPC-2 PLUS":"42297",
                   "AIR":"42298",
                   "Protein modification":"42456",
                   "Bilateria anatomy":"42455",
                   "Software Ontology":"42367",
                   "General Formal Ontology":"42452",
                   "General Formal Ontology: Biology":"42453",
                   "Uber anatomy ontology":"40724",
                   "Cancer Research and Management ACGT Master Ontology":"42497",
                   "Health Level Seven":"42545",
                   "Body System":"42651",
                   "Plant structure":"42737",
                   "Plant growth and developmental stage":"42736",
                   "Situation-Based Access Control":"42728",
                   "Read Codes, Clinical Terms Version 3 (CTV3) ":"42295",
                   "Translational Medicine Ontology":"42758",
                   "International Classification of  External Causes of Injuries":"42765",
                   "IMGT-ONTOLOGY":"42685",
                   "Tissue Microarray Ontology":"42764",
                   "NCI Metathesaurus":"42788",
                   "NCI Thesaurus":"42838",
                   "TOK_Ontology":"42834",
                   "Neomark Oral Cancer-Centred Ontology":"42835",
                   "Current Procedural Terminology":"42853",
                   "Electrocardiography Ontology":"42932",
                   "BioPortal Metadata":"42948",
                   "Pathway ontology":"42912",
                   "Gene Ontology Extension ":"42925",
                   "Ontology of Physics for Biology":"44599",
                   "Rat Strain Ontology":"42955",
                   "Cell Cycle Ontology":"42983",
                   "Cereal plant development":"13404",
                   "Amino Acid":"44016",
                   "Breast tissue cell lines":"44178",
                   "SysMO-JERM":"44036",
                   "Platynereis stage ontology":"44006",
                   "RNA ontology":"44005",
                   "Ontology for MicroRNA Target Prediction":"42998",
                   "Metathesaurus CPT Hierarchical Terms":"42994",
                   "ICD10":"44103",
                   "Smoking Behavior Risk Ontology":"44448",
                   "Drosophila development":"44408",
                   "Mouse adult gross anatomy":"44393",
                   "Tick gross anatomy":"44426",
                   "C. elegans development":"44429",
                   "Common Anatomy Reference Ontology":"44397",
                   "Physico-chemical process":"44424",
                   "Fly taxonomy":"44409",
                   "Mosquito gross anatomy":"44418",
                   "Biomedical Resource Ontology":"44450",
                   "Dendritic cell":"44400",
                   "Human developmental anatomy, abstract version":"44413",
                   "Teleost taxonomy":"44356",
                   "Plant environmental conditions":"44421",
                   "Human developmental anatomy, timed version":"44415",
                   "Xenopus anatomy and development":"44431",
                   "OBO relationship types":"44423",
                   "eVOC (Expressed Sequence Annotation for Humans)":"44302",
                   "Medaka fish anatomy and development":"44417",
                   "Mouse gross anatomy and development":"44404",
                   "Amphibian gross anatomy":"44394",
                   "Fungal gross anatomy":"44411",
                   "Environment Ontology":"44405",
                   "Lipid Ontology":"44210",
                   "Multiple alignment":"44212",
                   "Spider Ontology":"44399",
                   "Mass spectrometry":"44255",
                   "Dictyostelium discoideum anatomy":"44402",
                   "Pathogen transmission":"44425",
                   "NIFSTD":"44268",
                   "Animal natural history and life history":"44451",
                   "Evidence codes":"44406",
                   "Yeast phenotypes":"44412",
                   "Mouse pathology":"44420",
                   "Infectious disease":"44333",
                   "Uber anatomy ontology":"44301",
                   "Taxonomic rank vocabulary":"44216",
                   "ICPS Network":"44304",
                   "Interaction Network Ontology":"44354",
                   "Human developmental anatomy, abstract version, v2":"44414",
                   "Neural Motor Recovery Ontology":"44245",
                   "BioPAX":"44167",
                   "OBOE SBC":"44257",
                   "OBOE":"44258",
                   "CRISP Thesaurus, 2006":"44432",
                   "VANDF":"44452",
                   "HUGO":"44453",
                   "HCPCS":"44454",
                   "EDAM":"44465",
                   "Medical Subject Headings":"46836",
                   "PRotein Ontology (PRO)":"44762",
                   "Spatial Ontology":"44580",
                   "Chemical entities of biological interest":"45026",
                   "Gene Ontology":"44806",
                   "Experimental Factor Ontology":"44757",
                   "Neural ElectroMagnetic Ontologies":"44800",
                   "Ontology of Clinical Research (OCRe)":"44778",
                   "Teleost Anatomy Ontology":"44786",
                   "Human Phenotype Ontology":"44801",
                   "Biological imaging methods":"44562",
                   "Zebrafish anatomy and development":"44609",
                   "Cereal plant trait":"44714",
                   "Logical Observation Identifier Names and Codes":"44774",
                   "Gene Regulation Ontology":"44629",
                   "Phenotypic quality":"44789",
                   "C. elegans gross anatomy":"44693",
                   "Sequence types and features":"44797",
                   "SNOMED Clinical Terms":"44777",
                   "Symptom Ontology":"44749",
                   "Malaria Ontology":"44686",
                   "Drosophila gross anatomy":"44765",
                   "C. elegans phenotype":"44798",
                   "Units of measurement":"44519",
                   "FlyBase Controlled Vocabulary":"44614",
                   "Skin Physiology Ontology":"38611",
                   "Vaccine Ontology":"44805",
                   "Ascomycete phenotype ontology":"44709",
                   "Human disease":"44764",
                   "Mammalian phenotype":"44803",
                   "Cell type":"44759",
                   "Mosquito insecticide resistance":"44807",
                   "RadLex":"44787",
                   "NanoParticle Ontology":"44737",
                   "Foundational Model of Anatomy":"44507",
                   "Ontology for General Medical Science":"44679",
                   "Adverse Event Ontology":"44773",
                   "PMA 2010":"44666",
                   "EDAM":"44600",
                   "SemanticScience Integrated Ontology":"44510",
                   "apollo-akesios":"44565",
                   "Brucellosis Ontology":"44723",
                   "Role Ontology":"44694",
                   "Neural-Immune Gene Ontology":"44724",
                   "Ontology for Drug Discovery Investigations":"44704",
                   "Cell line ontology":"44729",
                   "HOM Hospital Discharge Codes":"44730",
                   "TSI Hospital Discharge Codes":"44731",
                   "Wheat trait":"44779"}

def main():
    status = 0
    try:
        opts, args = getopt.getopt(sys.argv[1:],"vh:n:o:k:",["verbose","help","ncbo-annot-file=","ontology=","knowtator-file"])
    except getopt.GetoptError:
        usage()

    ncboAnnotFile = ""
    #ontology = "" 
    ontologies = []
    knowtatorFile = ""
    debug = False 

    for o, a in opts:
        if o in ("-h", "--help"):
            usage()
        elif o in ("-v", "--verbose"):
            debug = True
        elif o in ("-n", "--ncbo-annot-file"):
            ncboAnnotFile = str(a)
        elif o in ("-o", "--ontology"):
            ontologyS = str(a)
            ontologies = ontologyS.split("|")
        elif o in ("-k", "--knowtator-file"):
            knowtatorFile = str(a)

    if not knowtatorFile:
        print "Please specify the path to an XML file of annotations exported from the Knowtator Protege plug-in"
        sys.exit(' ')
    
    if not ncboAnnotFile:
        print "Please specify the path to an XML file of annotations produced by the NCBO Annotator"
        sys.exit(' ')

    ontols = ontolLocalIdMap.keys()
    ontols.sort()
    if ontologies == []: 
        print "Please specify the name of an ontology used by the NCBO Annotator. Here is a listing:\n\t%s" %  "\n\t".join(ontols)
        sys.exit(' ')

    # real work starts here
    ncboAnnots = []
    for ontology in ontologies:
        if ontology not in ontols:
            print "Ontology name '%s' not recognized. Please specify the name of an ontology used by the NCBO Annotator. Here is a listing:\n\t%s" %  (ontology, "\n\t".join(ontols))
            sys.exit(' ')

        ncboAnnots += parseNcboAnnotFile(ncboAnnotFile, ontolLocalIdMap[ontology], debug)

    knowtAnnots = parseKnowtatorFile(knowtatorFile, debug)
    
    #gag 071712: sorting annotations by length fixes rare mapping bug
    ncboTermsL = sorted([x["pname"].lower() for x in ncboAnnots], key=len)
    print "\n%d unique entities annotated by the NCBO Annotator\n\t\t%s" % (len(ncboTermsL), ncboTermsL)

    # map the shortest version of a string representing a drug entity
    # to all other entities that perfectly match the string from their
    # start
    ncboFnd = []
    ncboTermsDict = {}
    for t in ncboTermsL:
        if t in ncboFnd:
            continue

        l = filter(lambda x: x.find(t) == 0 or t.find(x) == 0, set(ncboTermsL).difference(set(ncboFnd)))

        if len(l) == 0:
            continue

        ncboFnd += l
        repTrm = l[0]
        repTrmLen = len(repTrm)
        for r in l:
            if len(r) < repTrmLen:
                repTrm = r
                repTrmLen = len(r)
        
        ncboTermsDict[repTrm] = l
    ncboRmng = set(ncboTermsL).difference(set(ncboFnd))
    for elt in ncboRmng:
        ncboTermsDict[elt] = []
    print "\nCollapsed version of unique entities annotated by the NCBO Annotator:\n\t\t%s" % ncboTermsDict

    knowtTermsL = sorted([x["pname"].lower() for x in knowtAnnots], key=len)
    print "\n%d unique entities in the reference annotation set\n\t\t%s" % (len(knowtTermsL), knowtTermsL)

    # map the shortest version of a string representing a drug entity
    # to all other entities that perfectly match the string from their
    # start
    knowtFnd = []
    knowtTermsDict = {}
    for t in knowtTermsL:
        if t in knowtFnd:
            continue

        l = filter(lambda x: x.find(t) == 0 or t.find(x) == 0, set(knowtTermsL).difference(set(knowtFnd)))

        if len(l) == 0:
            continue 

        knowtFnd += l
        repTrm = l[0]
        repTrmLen = len(repTrm)
        for r in l:
            if len(r) < repTrmLen:
                repTrm = r
                repTrmLen = len(r)
        
        knowtTermsDict[repTrm] = l
    knowtRmng = set(knowtTermsL).difference(set(knowtFnd))
    for elt in knowtRmng:
        knowtTermsDict[elt] = []
    print "\nCollapsed version of unique entities annotated by the KNOWT Annotator:\n\t\t%s" % knowtTermsDict

    ncboTermsS = set(ncboTermsDict.keys())
    knowtTermsS = set(knowtTermsDict.keys())

    perfectMtchs = ncboTermsS.intersection(knowtTermsS)
    print "\n%d perfect string matches between Knowtator and NCBO: %s" % (len(perfectMtchs), perfectMtchs)

    ncboTermsDiff = [x for x in ncboTermsS.difference(knowtTermsS)]
    knowtTermsDiff = [x for x in knowtTermsS.difference(ncboTermsS)]
    
    print "\n\nPrior to processing -- ncboTermsDiff: %s\nknowtTermsDiff: %s" % (ncboTermsDiff, knowtTermsDiff)
    
    # See if there is perfect match between any lower-cased Knowtator
    # annotation and the beginning string of an NCBO annotator or vice
    # versa (e.g. 'lotrel' <--> 'lotrel 5/20').  Many drug products
    # contain additional information besides the drug's brand or
    # active ingredient name.
    # diffFnd = []
    # diffL = ncboTermsDiff + knowtTermsDiff # set diff gaurantees uniqueness in both lists
    # diffTermsDict = {}
    # for t in diffL:
    #     if t in diffFnd:
    #         continue

    #     l = filter(lambda x: x.find(t) == 0 or t.find(x) == 0, set(diffL).difference(set(diffFnd)))

    #     if len(l) == 0:
    #         continue 
    #     diffFnd += l
    #     repTrm = l[0]
    #     repTrmLen = len(repTrm)
    #     for r in l:
    #         if len(r) < repTrmLen:
    #             repTrm = r
    #             repTrmLen = len(r)

    #     diffTermsDict[repTrm] = l # at least one element from both lists should be present 
    # print "\nCollapsed version of entities not matched by perfect string matches:\n\t\t%s" % diffTermsDict

    diffFnd = []
    diffTermsDict = {}
    for n in ncboTermsDiff:       

        l = filter(lambda x: n.find(x) == 0 or x.find(n) == 0, set(knowtTermsDiff).difference(set(diffFnd)))

        if len(l) == 0:
            continue 

        l.append(n)
        diffFnd += l
        repTrm = l[0]
        repTrmLen = len(repTrm)
        for r in l:
            if len(r) < repTrmLen:
                repTrm = r
                repTrmLen = len(r)

        diffTermsDict[repTrm] = l # at least one element from both lists should be present 
    print "\nCollapsed version of entities not matched by perfect string matches:\n\t\t%s" % diffTermsDict

    imperfectMtchs = []
    for e,v in diffTermsDict.iteritems():
        if len(v) == 1:
            continue

        imperfectMtchs.append(v)
        for elt in v:
            try:
                knowtTermsDiff.remove(elt)
            except ValueError:
                pass

            try:
                ncboTermsDiff.remove(elt)
            except ValueError:
                pass

    print "Post processing -- ncboTermsDiff: %s\nknowtTermsDiff: %s\ninperfectMtchs: %s" % (ncboTermsDiff, knowtTermsDiff, imperfectMtchs)

    allMatches = imperfectMtchs + [x for x in perfectMtchs]
    print "DEBUG: AllMatches = ", allMatches
    ncboUniq = ncboTermsDiff
    print "\n%d annotations unique to NCBO: %s" % (len(ncboUniq), ncboUniq)

    knowtUniq = knowtTermsDiff
    print "\n%d annotations unique to Knowtator: %s" % (len(knowtUniq), knowtUniq)
    
    # calculations are done using a contingency table as follows
    # +------------------+-------------+---------------+
    # |                  |knowt include| knowt exclude |
    # |                  |             |               |
    # +------------------+-------------+---------------+
    # | ncbo  include    |     A       |     B         |
    # +------------------+-------------+---------------+
    # | ncbo  exclude    |     C       |     D         |
    # +------------------+-------------+---------------+      
    a = float(len(allMatches))
    b = float(len(ncboUniq))
    c = float(len(knowtUniq))
    if a + b == 0:
        precision = -1
    else:
        precision = a / (a + b)
    if a + c == 0:
        recall = -1
    else:
        recall = a / (a + c)

    if (precision + recall) == 0:
        fM = -1
    else:
        fM = (2*precision*recall)/(precision + recall)
        
    print "------------------------------------------------------------"
    print "Recall: %.2f\nprecision: %.2f\nF-measure: %.2f" % (recall, precision, fM)
    print "------------------------------------------------------------"

def parseNcboAnnotFile(fname, ontolId, debug):
    print "\n************************************************************"
    print "Parsing %s for annotations from ontology ID %s" % (fname, ontolId)

    ncboAnnots = []

    # catalog the concepts returned by the NCBO
    # annotator for the clinical report
    etree = ET.parse(fname)
    root = etree.getroot()
    data = root.getchildren()[2]
    annotResult = data.getchildren()[0] 
    clinNote = annotResult.getchildren()[2].getchildren()[-2]
    annots = annotResult.getchildren()[3]
    ontols = annotResult.getchildren()[4]

    for annot in annots.getchildren():
        conceptTree = annot.getchildren()[1]
        id = conceptTree.getchildren()[0].text
        lcid = conceptTree.getchildren()[1].text
        loid = conceptTree.getchildren()[2].text
        fullid = conceptTree.getchildren()[4].text
        pname = annot.find("context").find("term").find("name").text #conceptTree.getchildren()[5].text
    
        if int(loid) != int(ontolId):
            if debug:
                print "Skipping annotation with preferred name '%s' because it is not from the selected ontology, local ontology id = %s" % (pname, loid)
            continue 

        contextTree = annot.getchildren()[2]
        spanFrom = contextTree.getchildren()[2].text
        spanTo = contextTree.getchildren()[3].text
        if debug:
            print "adding: %s, %s, %s, %s, %s, %s, %s" % (id, lcid, loid, fullid, pname, spanFrom, spanTo)
        ncboAnnots.append({'id':id, 
                           'lcid':lcid, 
                           'loid':loid, 
                           'fullid':fullid, 
                           'pname':pname.strip(), 
                           'spanFrom':spanFrom, 
                           'spanTo':spanTo})  
    return ncboAnnots

def parseKnowtatorFile(fname, debug):
    if debug:
        print "Parsing %s for annotations of any type" % (fname)

    knowtAnnots = []

    # catalog the concepts annotated in the clinical report
    etree = ET.parse(fname)
    root = etree.getroot()
    annots = filter(lambda x: x.tag == "annotation", root.getchildren())
    classMentions = filter(lambda x: x.tag == "classMention", root.getchildren())
    for annot in annots:
        conceptTree = annot.getchildren()
        mentionId = conceptTree[0].get("id")
        knowtatorEntity = filter(lambda x: x.get("id") == mentionId, classMentions)[0].getchildren()[0].get("id")
        spanFrom = conceptTree[2].get("start")
        spanTo = conceptTree[2].get("end")
        pname = conceptTree[3].text

        if debug:
            print "adding: %s, %s, %s, %s, %s" % (mentionId, knowtatorEntity, pname, spanFrom, spanTo)
        knowtAnnots.append({'mentionId':mentionId,
                            'knowtatorEntity':knowtatorEntity,
                            'pname':pname, 
                            'spanFrom':spanFrom, 
                            'spanTo':spanTo})
    return knowtAnnots


def usage():
    ontols = ontolLocalIdMap.keys()
    ontols.sort()
    print '''
-------------------------------------------------------------------------
This program calculates the recall, precision, and balanced F-measure
for the NCBO Annotator service using a set of validation annotations
created by a human annotator. The NCBO annotations and human
annotations must be in XML files.

NOTE: This program determines agreement between the NCBO Annotator and human using a rather simple approach:
1) A list is created containing the text of each Knowtator annotation text lower-cased
2) A similar list is created for each annotation provided by NCBO Annotator 
3) Perfect string matches between the Knowtator and NCBO Annotator annotations are considered agreements
4) Agreements are also thought present if there is perfect match between the lower-cased Knowtator annotation and 
   the beginning string of an NCBO annotator or vice versa (e.g. 'lotrel' <--> 'lotrel 5/20'). This is because many 
   drug products contain additional information besides the drug's brand or active ingredient name. 

Please check over the program's output to confirm that the semantics
of the "agreements" are in fact the same before trusting the IAA calculations. 

Input:
-h, --help : this help message
-v, verbose : debug printout
-n, --ncbo-annot-file : an XML file output by the NCBO Annotator containing annotations
-o, --ontology : annotations made using concepts from this ontology will be used to calculate IAA. To specify more than one ontology pass a list of ontology names in the following format "<ontology 1>|<ontology 2>|...|..."
-k, --knowtator-file : an XML file output by the Knowtator plug-in for Protege containing annotations

The NCBO Annotator ontologies:\n\t%s 
''' %  ("\n\t".join(ontols))
    sys.exit(' ')

#-------------------------------
if __name__ == "__main__":
    main()


