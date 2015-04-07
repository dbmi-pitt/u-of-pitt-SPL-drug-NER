import xmltodict
import json


#INPUT = "../processed-output/513a41d0-37d4-4355-8a6d-a2c643bce6fa-drugInteractions.txt-PROCESSED.xml"
#TEXT_FILE = "../textfiles/513a41d0-37d4-4355-8a6d-a2c643bce6fa-drugInteractions.txt"

dirpath = '../processed-output/'
inputProductLabels = '../textfiles/'

xl = file(INPUT) 
result = xmltodict.parse(xl)

drugL = result['root']['data']['annotatorResultBean']['annotations']['annotationBean']

drugList = []

for (jsondir, dirnames, filenames) in walk(dirpath):
    filesPddi.extend(filenames)
    break

with open(TEXT_FILE, 'r') as f:
    sectionText = str(f.read())
    print sectionText
    
    for drugMention in drugL:
        drugName = drugMention['context']['term']['name']
        fromIdx = drugMention['context']['from']
        toIdx = drugMention['context']['to']

        drugURI = drugMention['concept']['fullId']
        preferredName = drugMention['concept']['preferredName']

        #print sectionText[0:2]
        print sectionText[int(fromIdx) -1 :int(toIdx)]

        prefix = sectionText[int(fromIdx) - 30:int(fromIdx)]
        suffix = sectionText[int(toIdx):int(toIdx) + 30]

        print "%s, %s, %s, %s, %s, %s, %s" % (drugName, fromIdx, toIdx, drugURI, preferredName, prefix, suffix)

        jsonDict = {}
        jsonDict['name'] = drugName
        jsonDict['splId'] = 
