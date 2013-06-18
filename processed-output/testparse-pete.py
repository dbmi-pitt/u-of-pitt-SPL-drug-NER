import xml.etree.ElementTree as ET

tree = ET.parse('package-insert-section-50.txt-PROCESSED.xml')
root = tree.getroot()


def findDrugs():
    drugs = []
    for term in root.iter('term'):
        dg = {}
        syn = []
        children = term.getchildren()
        for child in children:
            dg[child.tag] = child.text
            g1 = child.getchildren()
            for g in g1:
                dg[g.tag] = g.text
                g2 = g.getchildren()
                x = 0
                for u in g2:
                    syn.append(u.text)
                    dg[x] = u.text
                    x += 1                    
        drugs.append(dg)
    return drugs

if __name__=="__main__":
   table = findDrugs()
   print table
