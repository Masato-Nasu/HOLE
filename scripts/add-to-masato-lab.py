#!/usr/bin/env python3
"""Append HOLE to an CURRENT editor JSON export without touching existing entries."""
import json,sys,pathlib
if len(sys.argv)!=2:
    sys.exit('Usage: python3 scripts/add-to-masato-lab.py /path/to/CURRENT-data.json')
p=pathlib.Path(sys.argv[1]).expanduser().resolve()
data=json.loads(p.read_text(encoding='utf-8-sig'))
if not isinstance(data,dict) or not isinstance(data.get('tools'),list):
    sys.exit('Expected data JSON object with tools[]; current site export is required.')
entry={
  'title':'HOLE — Transparent Phone',
  'description':'スマートフォンの画面を、向こう側が見える「穴」に見せるAndroidアプリ。背面カメラを半透明で重ね、50%ではほかのアプリも操作できます。',
  'image':'', 'imageRatio':'4:3','imageFit':'contain',
  'appUrl':'https://github.com/Masato-Nasu/HOLE/releases/latest',
  'repoUrl':'https://github.com/Masato-Nasu/HOLE',
  'category':'photo-camera','platform':'android',
  'tags':['Android','Camera','Overlay','Experimental','Optical Illusion']
}
for tool in data['tools']:
    if 'HOLE' in str(tool.get('title','')).upper():
        sys.exit('HOLE already exists. No output created (prevents duplicates).')
data['tools'].append(entry)
out=p.with_name(p.stem+'-with-HOLE.json')
out.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'Saved: {out} ({len(data["tools"])} tools total; all existing entries preserved).')
