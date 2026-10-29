"""从 Android 原始模型类提取真实模型参数，生成后端种子数据。

用法：
    python tools/extract_models.py --android-src <path> --out src/main/resources/seed/models.json

只读取源码中的 dx/dy、总行数、每行管数和每行行首偏移；
特例（隐藏孔/堵塞孔/隔断/中缝加宽）来自身份明确的 Fragment 子类，手工核对后写入下方 SPECIAL。
"""
import argparse
import json
import re
import sys
from pathlib import Path

MODELS = [
    ("e107", "E107", "类Model_E107.java"),
    ("e221", "E221", "类Model_E221.java"),
    ("e222", "E222", "类Model_E222.java"),
    ("e302", "E302", "类Model_E302.java"),
]

SPECIAL = {
    "e107": {
        "wideCenterGap": True,
        "hideCells": ["c_0_1", "c_0_7", "c_1_4", "c_1_5",
                      "c_26_4", "c_26_5", "c_27_1", "c_27_7"],
        "blockCells": ["c_21_1", "c_21_11"],
        "separators": [{"afterRow": 13, "heightDp": 22}],
    },
    "e221": {
        "wideCenterGap": False,
        "hideCells": [],
        "blockCells": [],
        "separators": [{"afterRow": 24, "heightDp": 16}],
    },
    "e222": {"wideCenterGap": False, "hideCells": [], "blockCells": [], "separators": []},
    "e302": {"wideCenterGap": False, "hideCells": [], "blockCells": [], "separators": []},
}

EXPECTED = {"e107": (28, 374), "e221": (50, 1762), "e222": (23, 371), "e302": (13, 127)}


def parse_model(path: Path):
    text = path.read_text(encoding="utf-8")

    dx = int(re.search(r"\bdx\s*=\s*(\d+)\s*;", text).group(1))
    dy = int(re.search(r"\bdy\s*=\s*(\d+)\s*;", text).group(1))
    total_row = int(re.search(r"本模型总行数\s*=\s*(\d+)\s*;", text).group(1))

    array_match = re.search(r"本model每行管数\s*=\s*new\s+int\[\]\s*\{(.*?)\}\s*;", text, re.S)
    if not array_match:
        raise SystemExit(f"未找到每行管数数组: {path}")
    tube_counts = [int(v) for v in re.findall(r"\d+", array_match.group(1))]

    offsets = [
        (float(x), float(y))
        for x, y in re.findall(
            r"new\s+MyCoord\(\s*(-?\d+(?:\.\d+)?)f?\s*,\s*(-?\d+(?:\.\d+)?)f?\s*,\s*this\s*\)", text)
    ]

    if len(tube_counts) != total_row:
        raise SystemExit(f"{path.name}: 管数数组长度 {len(tube_counts)} != 总行数 {total_row}")
    if len(offsets) != total_row:
        raise SystemExit(f"{path.name}: 偏移数组长度 {len(offsets)} != 总行数 {total_row}")

    return dx, dy, tube_counts, offsets


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--android-src", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    android_src = Path(args.android_src)
    result = []
    for code, name, filename in MODELS:
        path = android_src / filename
        if not path.exists():
            raise SystemExit(f"缺少源文件: {path}")
        dx, dy, tube_counts, offsets = parse_model(path)
        expected_rows, expected_tubes = EXPECTED[code]
        actual_tubes = sum(tube_counts)
        if len(tube_counts) != expected_rows or actual_tubes != expected_tubes:
            raise SystemExit(
                f"{code}: 校验失败，实际 {len(tube_counts)} 行 / {actual_tubes} 根，"
                f"期望 {expected_rows} 行 / {expected_tubes} 根")
        special = SPECIAL[code]
        result.append({
            "code": code,
            "name": name,
            "dx": dx,
            "dy": dy,
            "wideCenterGap": special["wideCenterGap"],
            "rows": [
                {"tubeCount": tube_counts[i], "offsetX": offsets[i][0], "offsetY": offsets[i][1]}
                for i in range(len(tube_counts))
            ],
            "hideCells": special["hideCells"],
            "blockCells": special["blockCells"],
            "separators": special["separators"],
            "imageFile": f"seed-images/model_{code}.png",
            "sourceFile": filename,
        })
        print(f"{code}: {len(tube_counts)} 行 / {actual_tubes} 根 / dx={dx} dy={dy}")

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"已写入 {out}")


if __name__ == "__main__":
    sys.exit(main())
