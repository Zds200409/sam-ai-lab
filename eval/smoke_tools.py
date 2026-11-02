# -*- coding: utf-8 -*-
"""Step 5 冒烟测试：验证工具调用链路。"""
import json
import sys
import urllib.parse
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8081"

CASES = [
    ("无手机号问积分，应主动索号", "我的积分还有多少？"),
    ("带手机号查积分，应出真实数字", "查一下我的积分，手机号 18827578719"),
    ("查等级，应出会员信息", "我是什么等级？手机号 18827578719"),
    ("不存在的号码，应如实说查不到", "查一下 19900000000 的积分"),
    ("通用政策，应走知识库不调工具", "卓越会员几折？"),
]


def ask(question):
    q = urllib.parse.urlencode({"question": question})
    url = f"{BASE}/api/ai/chat?{q}"
    with urllib.request.urlopen(url, timeout=120) as resp:
        return json.loads(resp.read().decode("utf-8"))


for label, question in CASES:
    try:
        body = ask(question)
        data = body.get("data")
        text = data if isinstance(data, str) else json.dumps(data, ensure_ascii=False)
    except Exception as exc:  # noqa: BLE001
        text = f"[请求异常] {exc}"
    print("=" * 70)
    print(f"[{label}]")
    print(f"Q: {question}")
    print(f"A: {text}")
    print()
