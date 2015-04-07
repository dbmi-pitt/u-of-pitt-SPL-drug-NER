#!/usr/bin/python
#
# convertSPL_NER_2_ODA.py
#
# Convert NER output to Open Data Annotation serialized in JSON and
# output a CSV file containing the preferred name, exact string, and
# URIs of all drug mentions
#
# Author: Richard Boyce, Andres Hernandez, Yifan Ning

# Copyright (C) 2012-2015 by Richard Boyce and the University of Pittsburgh
 
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

import json, sys, re
from os import walk
import xmltodict
import codecs

############################################################
# Customizations

PREFIX_SUFFIX_SPAN = 60

inputNERDir = '../processed-output/'

inputProductLabelsDir = '../textfiles/'

############################################################
filesPddi = []


## read all NER outputs in XML

for (dirpath, dirnames, filenames) in walk(inputNERDir):
    for fname in filenames:
        if fname.endswith("PROCESSED.xml") and (not fname.startswith("TABLE-")):
            filesPddi.append(fname)
    break

#print filesPddi

nerList = []

for ner in filesPddi:
    with codecs.open(inputNERDir + ner, 'r', 'utf-8') as jsonInputFile:
        
        textFileName = ner.strip('-PROCESSED.xml')

        with codecs.open(inputProductLabelsDir + textFileName, 'r', 'utf-8') as textInputFile:
            sectionText = textInputFile.read()
            #print sectionText
            
            jsonResult = xmltodict.parse(jsonInputFile.read())
                

            drugL = jsonResult['root']['data']['annotatorResultBean']['annotations']['annotationBean']
            for drugMention in drugL:
                drugName = drugMention['context']['term']['name']
                fromIdx = drugMention['context']['from']
                toIdx = drugMention['context']['to']
                drugURI = drugMention['concept']['fullId']
                preferredName = drugMention['concept']['preferredName']

                setId = re.sub(r'-[A-Za-z]+\.txt$', "", textFileName)

                if len(range(0,int(fromIdx))) < PREFIX_SUFFIX_SPAN:
                    prefix = sectionText[0:int(fromIdx)-1]
                else:
                    prefix = sectionText[int(fromIdx)-PREFIX_SUFFIX_SPAN:int(fromIdx)-1]

                exact = sectionText[int(fromIdx)-1:int(toIdx)]

                if len(range(int(toIdx),len(sectionText))) < PREFIX_SUFFIX_SPAN:
                    suffix = sectionText[int(toIdx):]
                else:
                    suffix = sectionText[int(toIdx):int(toIdx)+PREFIX_SUFFIX_SPAN]

                nerDict = {"setId":setId ,"name":drugName, "fullId":drugURI, "prefix":prefix,"exact":exact, "suffix":suffix}

                nerList.append(nerDict)
                
                #print "%s, %s, %s, %s, %s, %s, %s, %s, %s" % (setId, drugName, fromIdx, toIdx, drugURI, preferredName, prefix, exact, suffix)
                print textFileName                
                
                
with open('NER-outputs.json', 'w') as nerOutput:
    json.dump(nerList, nerOutput)
                

# with open('drug_list.json', 'w') as nerOutput:
#     json.dump(drugInfoList, nerOutput)

# with open('NER-output.csv','w') as nerOutput:
#     for element in drugInfoList:
#         line = str(element['fileId'])+';'+str(element['name'])+';'+str(element['exact'])+';'+str(element['fullId'])
#         print >>  nerOutput, line
