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
def main():
    status = 0
    try:
        opts, args = getopt.getopt(sys.argv[1:],"vh:n:k:",["verbose","help","ncbo-annot-file=","knowtator-file"])
    except getopt.GetoptError:
        usage()

    ncboAnnotFile = ""
    knowtatorFile = ""
    debug = False 

    for o, a in opts:
        if o in ("-h", "--help"):
            usage()
        elif o in ("-v", "--verbose"):
            debug = True
        elif o in ("-n", "--ncbo-annot-file"):
            ncboAnnotFile = str(a)
        elif o in ("-k", "--knowtator-file"):
            knowtatorFile = str(a)

    if not knowtatorFile:
        print "Please specify the path to an XML file of annotations exported from the Knowtator Protege plug-in"
        sys.exit(' ')
    
    if not ncboAnnotFile:
        print "Please specify the path to an XML file of annotations produced by the NCBO Annotator"
        sys.exit(' ')

    # real work starts here
    ncboAnnots = []
    ncboAnnots += parseNcboAnnotFile(ncboAnnotFile, debug)
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

def parseNcboAnnotFile(fname, debug):
    print "\n************************************************************"
    print "Parsing %s for annotations" % (fname)

    ncboAnnots = []

    # catalog the concepts returned by the NCBO
    # annotator for the clinical report
    etree = ET.parse(fname)
    root = etree.getroot()
    data = root.getchildren()[0]
    annotResult = data.getchildren()[0] 
    annots = annotResult.getchildren()[0]

    for annot in annots.getchildren():
        conceptTree = annot.getchildren()[1]
        fullid = conceptTree.getchildren()[0].text
        pname = annot.find("context").find("term").find("name").text #conceptTree.getchildren()[5].text
    
        contextTree = annot.getchildren()[0]
        spanFrom = contextTree.getchildren()[1].text
        spanTo = contextTree.getchildren()[2].text
        if debug:
            print "adding: %s, %s, %s, %s, %s, %s, %s" % (id, "N/A", "N/A", fullid, pname, spanFrom, spanTo)
        ncboAnnots.append({'id':id, 
                           'lcid':"N/A", 
                           'loid':"N/A", 
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
-k, --knowtator-file : an XML file output by the Knowtator plug-in for Protege containing annotations

'''
    sys.exit(' ')

#-------------------------------
if __name__ == "__main__":
    main()


