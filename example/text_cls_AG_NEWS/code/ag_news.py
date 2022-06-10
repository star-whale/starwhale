import re
import csv


def load_ag_data(data_source):
    """
    Load raw data from the source file into data variable.

    Returns: None

    """
    data = []
    with open(data_source, 'r', encoding='utf-8') as f:
        rdr = csv.reader(f, delimiter=',', quotechar='"')
        for row in rdr:
            txt = ""
            for s in row[1:]:
                txt = txt + " " + re.sub("^\s*(.-)\s*$", "%1", s).replace("\\n",
                                                                          "\n")
            data.append((row[0], txt))  # format: (label, text)
    return data
