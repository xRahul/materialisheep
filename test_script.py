import re
with open('app/src/main/java/io/github/sheepdestroyer/materialisheep/WebFragment.java', 'r') as f:
    content = f.read()

match = re.search(r'void setWebSettings\(boolean isRemote\) \{[^\}]*\}', content, re.DOTALL)
if match:
    print(match.group(0))
