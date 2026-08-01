import codecs
import re

path = r'd:\Songgka\src\client\java\com\songgka\client\color\NameColorManager.java'
with codecs.open(path, 'r', encoding='utf-8', errors='ignore') as f:
    content = f.read()

# Fix the broken unicode sequences
content = content.replace(u'\ufffd\\u00A7', '\\u00A7')
content = content.replace(u'\ufffd', '\\u00A7')
content = content.replace(u'\u00A7', '\\u00A7') # Ensure any raw ones are escaped

with codecs.open(path, 'w', encoding='utf-8') as f:
    f.write(content)
