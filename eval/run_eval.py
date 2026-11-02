#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RAG 评估跑批脚本。

读取 eval/questions.jsonl，逐条调用 /api/ai/chat，按预期片段判定通过与否，
输出逐条结果、分类统计和失败明细。

判分规则：
  - expect 是「组」的列表，每组内任一备选片段命中即算该组满足，所有组都必须满足
  - forbid 中的片段只要出现，本条直接判失败（用于检测幻觉与过期口径）
  - 多轮条目按 turns 顺序发送，只判定最后一轮的回答

用法：
  python eval/run_eval.py                  # 默认 http://localhost:8081
  python eval/run_eval.py --base http://localhost:8082
  python eval/run_eval.py --only C         # 只跑 C 类
  python eval/run_eval.py --tag baseline   # 结果文件加标记
"""

import argparse
import datetime
import json
import pathlib
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent
QUESTIONS = ROOT / "questions.jsonl"
RESULTS_DIR = ROOT / "results"


def call_chat(base, question, conversation_id, timeout=90, retries=1):
    params = urllib.parse.urlencode(
        {"question": question, "conversationId": conversation_id}, encoding="utf-8"
    )
    url = f"{base}/api/ai/chat?{params}"
    last_err = None
    for attempt in range(retries + 1):
        try:
            with urllib.request.urlopen(url, timeout=timeout) as resp:
                payload = json.loads(resp.read().decode("utf-8"))
            if payload.get("code") != 200:
                last_err = f"业务码 {payload.get('code')}: {payload.get('message')}"
            else:
                return payload.get("data") or "", None
        except urllib.error.URLError as e:
            last_err = f"请求失败: {e}"
        except Exception as e:  # noqa: BLE001
            last_err = f"解析失败: {e}"
        if attempt < retries:
            time.sleep(2)
    return "", last_err


def _alternatives(group):
    """expect / forbid 的元素允许写成字符串或字符串列表，统一成列表。"""
    return group if isinstance(group, list) else [group]


# 拒答话术。非「边界与拒答」类题目，回答以这些话术开头就说明没找到依据，
# 即便句子里偶然含有期望关键词也不算通过——这是关键词判分最大的假阳性来源。
REFUSAL_MARKERS = [
    "不确定",
    "没有准确信息",
    "没有准确",
    "不在我的服务范围",
    "帮不上",
    "查不到",
    "没有相关",
    "无法回答",
    "没有找到",
]
REFUSAL_WINDOW = 40


def is_refusal(answer):
    head = answer[:REFUSAL_WINDOW]
    return next((m for m in REFUSAL_MARKERS if m in head), None)


def grade(answer, expect, forbid, category=""):
    """返回 (是否通过, 失败原因)。"""
    if not answer.strip():
        return False, "回答为空"

    if not category.startswith("E"):
        marker = is_refusal(answer)
        if marker:
            return False, f"拒答（未找到依据）：{marker}"

    for group in expect:
        alts = _alternatives(group)
        if not any(alt in answer for alt in alts):
            return False, f"缺少关键信息，期望其一：{' / '.join(alts)}"
    for group in forbid:
        alts = _alternatives(group)
        hit = next((alt for alt in alts if alt in answer), None)
        if hit:
            return False, f"出现不应出现的内容：{hit}"
    return True, ""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://localhost:8081")
    parser.add_argument("--only", default="", help="只跑某个类别前缀，如 A、C、F")
    parser.add_argument("--tag", default="", help="结果标记，用于区分基线/改造后")
    args = parser.parse_args()

    if not QUESTIONS.exists():
        sys.exit(f"找不到题目文件：{QUESTIONS}")

    items = [json.loads(line) for line in QUESTIONS.read_text(encoding="utf-8").splitlines() if line.strip()]
    if args.only:
        items = [i for i in items if i["category"].startswith(args.only)]
    if not items:
        sys.exit("没有匹配的题目")

    RESULTS_DIR.mkdir(exist_ok=True)
    stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    tag = f"-{args.tag}" if args.tag else ""

    results = []
    started = time.time()
    for idx, item in enumerate(items, 1):
        cid = f"eval-{item['id']}-{stamp}"
        answer, err = "", None
        for turn in item["turns"]:
            answer, err = call_chat(args.base, turn, cid)
            if err:
                break
        if err:
            passed, reason = False, err
        else:
            passed, reason = grade(answer, item["expect"], item["forbid"], item["category"])
        results.append(
            {
                "id": item["id"],
                "category": item["category"],
                "turns": item["turns"],
                "passed": passed,
                "reason": reason,
                "answer": answer,
                "note": item.get("note", ""),
            }
        )
        mark = "PASS" if passed else "FAIL"
        print(f"[{idx:02d}/{len(items)}] {mark} {item['id']} {item['turns'][-1]}")
        if not passed:
            print(f"        -> {reason}")
            print(f"        -> {answer[:120].replace(chr(10), ' ')}")

    elapsed = time.time() - started

    # 汇总
    by_cat = {}
    for r in results:
        c = by_cat.setdefault(r["category"], {"total": 0, "passed": 0})
        c["total"] += 1
        c["passed"] += 1 if r["passed"] else 0
    total = len(results)
    passed_total = sum(1 for r in results if r["passed"])

    out = {
        "base": args.base,
        "tag": args.tag,
        "timestamp": stamp,
        "elapsed_seconds": round(elapsed, 1),
        "total": total,
        "passed": passed_total,
        "accuracy": round(passed_total / total, 4) if total else 0,
        "by_category": {
            k: {**v, "accuracy": round(v["passed"] / v["total"], 4)} for k, v in sorted(by_cat.items())
        },
        "results": results,
    }
    out_path = RESULTS_DIR / f"eval{tag}-{stamp}.json"
    out_path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")

    print("\n" + "=" * 60)
    print(f"总体：{passed_total}/{total}  准确率 {out['accuracy'] * 100:.1f}%  耗时 {elapsed:.0f}s")
    print("-" * 60)
    for cat, v in out["by_category"].items():
        print(f"  {cat:<16} {v['passed']:>2}/{v['total']:<2}  {v['accuracy'] * 100:>5.1f}%")
    print("-" * 60)
    failed = [r for r in results if not r["passed"]]
    if failed:
        print("失败明细：")
        for r in failed:
            print(f"  {r['id']}  {r['reason']}")
    print("=" * 60)
    print(f"结果已写入 {out_path}")


if __name__ == "__main__":
    main()
