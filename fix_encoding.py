import codecs

path = r'd:\Songgka\src\client\java\com\songgka\client\color\NameColorManager.java'
with codecs.open(path, 'r', encoding='utf-16le', errors='ignore') as f:
    content = f.read()

# Fallback to default encoding if it doesn't look like java code
if 'package com' not in content:
    with codecs.open(path, 'r', errors='ignore') as f:
        content = f.read()

content = content.replace('boolean isSorakkaa = "sorakkaa".equalsIgnoreCase(nameLower) || (mc.player != null && mc.player.getScoreboardName() != null && nameLower.equalsIgnoreCase(mc.player.getScoreboardName()));', 'boolean isSorakkaa = "sorakkaa".equalsIgnoreCase(nameLower);')

with codecs.open(path, 'w', encoding='utf-8') as f:
    f.write(content)
